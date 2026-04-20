package com.ramen73.ramenchat

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ramen73.ramenchat.adapter.UserSearchAdapter
import com.ramen73.ramenchat.databinding.ActivityNewChatBinding
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.gone
import com.ramen73.ramenchat.utils.visible
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NewChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewChatBinding
    private lateinit var adapter: UserSearchAdapter
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = UserSearchAdapter { user ->
            lifecycleScope.launch {
                val chatId = FirebaseUtils.getOrCreateChatRoom(user.uid)
                val intent = Intent(this@NewChatActivity, ChatActivity::class.java).apply {
                    putExtra("CHAT_ID", chatId)
                    putExtra("OTHER_USER", user)
                }
                startActivity(intent)
                finish()
            }
        }
        binding.rvUsers.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(400)
                    val query = s.toString().trim()
                    if (query.length >= 2) {
                        binding.progressBar.visible()
                        val users = FirebaseUtils.searchUsers(query)
                        binding.progressBar.gone()
                        adapter.submitList(users)
                        if (users.isEmpty()) binding.tvEmpty.visible() else binding.tvEmpty.gone()
                    }
                }
            }
        })
    }
}
