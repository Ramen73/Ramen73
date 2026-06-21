package com.ramen73.ramenchat

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ramen73.ramenchat.adapter.UserMultiSelectAdapter
import com.ramen73.ramenchat.databinding.ActivityNewGroupBinding
import com.ramen73.ramenchat.model.User
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.gone
import com.ramen73.ramenchat.utils.toast
import com.ramen73.ramenchat.utils.visible
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NewGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewGroupBinding
    private lateinit var adapter: UserMultiSelectAdapter
    private val selected = mutableSetOf<User>()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = UserMultiSelectAdapter(selected) { user, isSelected ->
            if (isSelected) selected.add(user) else selected.remove(user)
            binding.tvCount.text = "${selected.size} selezionati"
            binding.btnCreate.isEnabled = selected.isNotEmpty() &&
                binding.etGroupName.text.toString().isNotBlank()
        }
        binding.rvUsers.adapter = adapter

        binding.etSearch.addTextChangedListener(textChanged { query ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(400)
                if (query.length >= 2) {
                    binding.progressBar.visible()
                    val users = FirebaseUtils.searchUsers(query)
                    binding.progressBar.gone()
                    adapter.submitList(users)
                }
            }
        })

        binding.etGroupName.addTextChangedListener(textChanged {
            binding.btnCreate.isEnabled = selected.isNotEmpty() && it.isNotBlank()
        })

        binding.btnCreate.setOnClickListener { createGroup() }
    }

    private fun createGroup() {
        val name = binding.etGroupName.text.toString().trim()
        if (name.isEmpty() || selected.isEmpty()) return

        binding.progressBar.visible()
        binding.btnCreate.isEnabled = false
        lifecycleScope.launch {
            try {
                val chatId = FirebaseUtils.createGroupChat(name, selected.map { it.uid })
                startActivity(Intent(this@NewGroupActivity, ChatActivity::class.java).apply {
                    putExtra("CHAT_ID", chatId)
                })
                finish()
            } catch (e: Exception) {
                binding.progressBar.gone()
                binding.btnCreate.isEnabled = true
                toast("Errore: ${e.localizedMessage}")
            }
        }
    }

    private fun textChanged(onChange: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { onChange(s.toString().trim()) }
    }
}
