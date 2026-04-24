package com.ramen73.ramenchat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ramen73.ramenchat.databinding.ActivityProfileBinding
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.gone
import com.ramen73.ramenchat.utils.toast
import com.ramen73.ramenchat.utils.visible
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadPhoto(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadCurrentUser()

        binding.ivAvatar.setOnClickListener { pickImage.launch("image/*") }
        binding.fabEditPhoto.setOnClickListener { pickImage.launch("image/*") }
        binding.btnSave.setOnClickListener { saveProfile() }
        binding.btnLogout.setOnClickListener { logout() }
    }

    private fun loadCurrentUser() {
        lifecycleScope.launch {
            val user = FirebaseUtils.getUser(FirebaseUtils.currentUserId) ?: return@launch
            binding.etName.setText(user.displayName)
            binding.etBio.setText(user.bio)
            binding.tvEmail.text = user.email
            if (user.photoUrl.isNotEmpty()) {
                Glide.with(this@ProfileActivity)
                    .load(user.photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(binding.ivAvatar)
            }
        }
    }

    private fun uploadPhoto(uri: Uri) {
        binding.progressBar.visible()
        lifecycleScope.launch {
            try {
                val path = "users/${FirebaseUtils.currentUserId}/profile.jpg"
                val url = FirebaseUtils.uploadImage(uri, path)
                FirebaseUtils.updateProfilePhoto(url)
                Glide.with(this@ProfileActivity).load(url).circleCrop().into(binding.ivAvatar)
                toast("Foto profilo aggiornata!")
            } catch (e: Exception) {
                toast("Errore nel caricamento: ${e.localizedMessage}")
            } finally {
                binding.progressBar.gone()
            }
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
