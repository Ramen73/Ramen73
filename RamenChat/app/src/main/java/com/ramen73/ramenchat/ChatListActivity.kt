package com.ramen73.ramenchat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.firestore.Query
import com.ramen73.ramenchat.adapter.ChatListAdapter
import com.ramen73.ramenchat.databinding.ActivityChatListBinding
import com.ramen73.ramenchat.model.ChatRoom
import com.ramen73.ramenchat.model.User
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.gone
import com.ramen73.ramenchat.utils.toast
import com.ramen73.ramenchat.utils.visible
import kotlinx.coroutines.launch

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private lateinit var adapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeChats()

        binding.fabNewChat.setOnClickListener { showNewChatMenu(it) }
    }

    private fun showNewChatMenu(anchor: View) {
        val menu = PopupMenu(this, anchor)
        menu.menu.add("Nuova chat")
        menu.menu.add("Nuovo gruppo")
        menu.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Nuova chat" -> startActivity(Intent(this, NewChatActivity::class.java))
                "Nuovo gruppo" -> startActivity(Intent(this, NewGroupActivity::class.java))
            }
            true
        }
        menu.show()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        // Load own avatar in the toolbar
        lifecycleScope.launch {
            val me = FirebaseUtils.getUser(FirebaseUtils.currentUserId)
            if (!me?.photoUrl.isNullOrEmpty()) {
                Glide.with(this@ChatListActivity)
                    .load(me?.photoUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(binding.ivProfile)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatListAdapter { chatRoom ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("CHAT_ID", chatRoom.chatId)
                if (!chatRoom.isGroup) putExtra("OTHER_USER", chatRoom.otherUser)
            }
            startActivity(intent)
        }
        binding.rvChats.adapter = adapter
    }

    private fun observeChats() {
        FirebaseUtils.chatsCollection
            .whereArrayContains("participants", FirebaseUtils.currentUserId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ChatList", "Firestore error", error)
                    toast("Errore: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val rooms = snapshot.toObjects(ChatRoom::class.java)
                if (rooms.isEmpty()) {
                    binding.tvEmpty.visible()
                    binding.rvChats.gone()
                    adapter.submitList(emptyList())
                    return@addSnapshotListener
                }
                binding.tvEmpty.gone()
                binding.rvChats.visible()
                lifecycleScope.launch {
                    val enriched = rooms.map { room ->
                        if (room.isGroup) {
                            room
                        } else {
                            val otherId = room.participants.firstOrNull { it != FirebaseUtils.currentUserId }
                                ?: return@map room
                            val other = FirebaseUtils.getUser(otherId) ?: User(uid = otherId)
                            room.copy(otherUser = other)
                        }
                    }
                    adapter.submitList(enriched)
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
