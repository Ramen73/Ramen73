package com.ramen73.ramenchat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.firestore.Query
import com.ramen73.ramenchat.adapter.MessageAdapter
import com.ramen73.ramenchat.databinding.ActivityChatBinding
import com.ramen73.ramenchat.model.ChatRoom
import com.ramen73.ramenchat.model.Message
import com.ramen73.ramenchat.model.User
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.NotificationHelper
import com.ramen73.ramenchat.utils.toast
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var chatId: String
    private var otherUser: User? = null
    private var lastMessageCount = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getStringExtra("CHAT_ID") ?: run { finish(); return }
        otherUser = intent.getParcelableExtra("OTHER_USER")

        setupToolbar()
        setupRecyclerView()
        loadChatRoom()
        observeMessages()
        observeOtherUserStatus()

        binding.btnSend.setOnClickListener { sendTextMessage() }
        binding.btnRecord.visibility = android.view.View.GONE
        binding.tvRecordingHint.visibility = android.view.View.GONE
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        otherUser?.let { user ->
            binding.tvRecipientName.text = user.displayName
            if (user.photoUrl.isNotEmpty()) {
                Glide.with(this).load(user.photoUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(binding.ivRecipientAvatar)
            }
            binding.tvStatus.text = if (user.online) "Online" else "Offline"
        }
    }

    private fun observeOtherUserStatus() {
        val uid = otherUser?.uid ?: return
        FirebaseUtils.usersCollection.document(uid)
            .addSnapshotListener { snapshot, _ ->
                val user = snapshot?.toObject(User::class.java) ?: return@addSnapshotListener
                binding.tvStatus.text = if (user.online) "Online" else "Offline"
            }
    }

    private fun loadChatRoom() {
        FirebaseUtils.chatsCollection.document(chatId)
            .addSnapshotListener { snapshot, _ ->
                val room = snapshot?.toObject(ChatRoom::class.java) ?: return@addSnapshotListener
                if (room.isGroup) {
                    binding.tvRecipientName.text = room.groupName
                    binding.tvStatus.text = "${room.participants.size} membri"
                    binding.ivRecipientAvatar.setImageResource(R.drawable.ic_default_avatar)
                }
                adapter.isGroup = room.isGroup
                adapter.notifyDataSetChanged()
            }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(FirebaseUtils.currentUserId)
        binding.rvMessages.adapter = adapter
    }

    private fun observeMessages() {
        FirebaseUtils.messagesCollection(chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val messages = snapshot.toObjects(Message::class.java)
                val isNewIncomingMessage = lastMessageCount >= 0 &&
                    messages.size > lastMessageCount &&
                    messages.last().senderId != FirebaseUtils.currentUserId
                adapter.submitList(messages) {
                    if (messages.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                    }
                }
                if (isNewIncomingMessage) {
                    NotificationHelper.playInAppSound(this)
                }
                lastMessageCount = messages.size
            }
    }

    private fun sendTextMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return
        binding.etMessage.setText("")
        lifecycleScope.launch {
            try {
                FirebaseUtils.sendTextMessage(chatId, text, otherUser?.uid ?: "")
            } catch (e: Exception) {
                toast("Errore nell'invio")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { FirebaseUtils.updateOnlineStatus(true) }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch { FirebaseUtils.updateOnlineStatus(false) }
    }
}
