package com.ramen73.ramenchat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ramen73.ramenchat.databinding.ActivityProfileBinding
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.toast
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.fabEditPhoto.visibility = android.view.View.GONE
        binding.ivAvatar.setOnClickListener {
            toast("Abilita Firebase Storage per le foto profilo")
        }

        loadCurrentUser()

        binding.btnSave.setOnClickListener { saveProfile() }
        binding.btnLogout.setOnClickListener { logout() }
    }

    private fun loadCurrentUser() {
        lifecycleScope.launch {
            val user = FirebaseUtils.getUser(FirebaseUtils.currentUserId) ?: return@launch
            binding.etName.setText(user.displayName)
            binding.etBio.setText(user.bio)
            binding.tvEmail.text = user.email
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()
        if (name.isEmpty()) { toast("Il nome non può essere vuoto"); return }
        lifecycleScope.launch {
            try {
                FirebaseUtils.updateProfile(name, bio)
                toast("Profilo aggiornato!")
            } catch (e: Exception) {
                toast("Errore nel salvataggio")
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            FirebaseUtils.updateOnlineStatus(false)
            FirebaseUtils.auth.signOut()
            startActivity(Intent(this@ProfileActivity, LoginActivity::class.java)
                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
        }
    }
}
