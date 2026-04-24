package com.ramen73.ramenchat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ramen73.ramenchat.databinding.ActivityRegisterBinding
import com.ramen73.ramenchat.model.User
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.gone
import com.ramen73.ramenchat.utils.toast
import com.ramen73.ramenchat.utils.visible
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.btnGoLogin.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirm = binding.etConfirmPassword.text.toString().trim()

        when {
            name.isEmpty() -> { toast("Inserisci il tuo nome"); return }
            email.isEmpty() -> { toast("Inserisci la tua email"); return }
            password.length < 6 -> { toast("La password deve avere almeno 6 caratteri"); return }
            password != confirm -> { toast("Le password non coincidono"); return }
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = FirebaseUtils.auth
                    .createUserWithEmailAndPassword(email, password).await()
                val uid = result.user!!.uid
                val user = User(
                    uid = uid,
                    displayName = name,
                    email = email,
                    online = true
                )
                FirebaseUtils.saveUser(user)
                startActivity(Intent(this@RegisterActivity, ChatListActivity::class.java)
                    .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
            } catch (e: Exception) {
                toast("Errore: ${e.localizedMessage}")
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            binding.progressBar.visible()
            binding.btnRegister.gone()
        } else {
            binding.progressBar.gone()
            binding.btnRegister.visible()
        }
    }
}
