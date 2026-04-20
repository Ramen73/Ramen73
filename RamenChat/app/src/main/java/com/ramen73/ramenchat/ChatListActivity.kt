package com.ramen73.ramenchat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.Query
import com.ramen73.ramenchat.adapter.ChatListAdapter
import com.ramen73.ramenchat.databinding.ActivityChatListBinding
import com.ramen73.ramenchat.model.ChatRoom
import com.ramen73.ramenchat.model.User
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.gone
import com.ramen73.ramenchat.utils.visible
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

        binding.fabNewChat.setOnClickListener {
            startActivity(Intent(this, NewChatActivity::class.java))
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatListAdapter { chatRoom ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("CHAT_ID", chatRoom.chatId)
                putExtra("OTHER_USER", chatRoom.otherUser)
            }
            startActivity(intent)
        }
        binding.rvChats.adapter = adapter
    }

    private fun observeChats() {
        FirebaseUtils.chatsCollection
            .whereArrayContains("participants", FirebaseUtils.currentUserId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
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
                        val otherId = room.participants.first { it != FirebaseUtils.currentUserId }
                        val other = FirebaseUtils.getUser(otherId) ?: User(uid = otherId)
                        room.copy(otherUser = other)
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
