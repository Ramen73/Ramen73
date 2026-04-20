package com.ramen73.ramenchat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.firestore.Query
import com.ramen73.ramenchat.adapter.MessageAdapter
import com.ramen73.ramenchat.databinding.ActivityChatBinding
import com.ramen73.ramenchat.model.Message
import com.ramen73.ramenchat.model.User
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.toast
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var chatId: String
    private lateinit var otherUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getStringExtra("CHAT_ID") ?: run { finish(); return }
        otherUser = intent.getParcelableExtra("OTHER_USER") ?: run { finish(); return }

        setupToolbar()
        setupRecyclerView()
        observeMessages()

        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.tvRecipientName.text = otherUser.displayName
        if (otherUser.photoUrl.isNotEmpty()) {
            Glide.with(this).load(otherUser.photoUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.ivRecipientAvatar)
        }
        binding.tvStatus.text = if (otherUser.isOnline) "Online" else "Offline"
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
                adapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return
        binding.etMessage.setText("")
        lifecycleScope.launch {
            try {
                FirebaseUtils.sendMessage(chatId, text, otherUser.uid)
            } catch (e: Exception) {
                toast("Errore nell'invio del messaggio")
            }
        }
    }
}
