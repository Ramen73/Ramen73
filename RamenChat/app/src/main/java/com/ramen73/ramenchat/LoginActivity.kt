package com.ramen73.ramenchat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
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
        binding.tvForgotPassword.setOnClickListener { showForgotPasswordDialog() }
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
                FirebaseUtils.refreshAndSaveFcmToken()
                startActivity(Intent(this@LoginActivity, ChatListActivity::class.java))
                finish()
            } catch (e: Exception) {
                toast("Errore: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val input = TextInputEditText(this).apply {
            hint = "La tua email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(binding.etEmail.text)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Recupera password")
            .setMessage("Inserisci la tua email e ti invieremo un link per reimpostare la password.")
            .setView(input)
            .setPositiveButton("Invia link") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isEmpty()) {
                    toast("Inserisci la tua email")
                    return@setPositiveButton
                }
                sendPasswordReset(email)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun sendPasswordReset(email: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                FirebaseUtils.auth.sendPasswordResetEmail(email).await()
                toast("Email di recupero inviata a $email — controlla la casella di posta")
            } catch (e: Exception) {
                toast("Errore: ${e.localizedMessage}")
            } finally {
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
