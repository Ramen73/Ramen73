package com.ramen73.ramenchat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ramen73.ramenchat.databinding.ActivityLoginBinding
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.gone
import com.ramen73.ramenchat.utils.toast
import com.ramen73.ramenchat.utils.visible
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            toast("Inserisci email e password")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                FirebaseUtils.auth.signInWithEmailAndPassword(email, password).await()
                FirebaseUtils.updateOnlineStatus(true)
                startActivity(Intent(this@LoginActivity, ChatListActivity::class.java))
                finish()
            } catch (e: Exception) {
                toast("Errore: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            binding.progressBar.visible()
            binding.btnLogin.gone()
        } else {
            binding.progressBar.gone()
            binding.btnLogin.visible()
        }
    }
}
