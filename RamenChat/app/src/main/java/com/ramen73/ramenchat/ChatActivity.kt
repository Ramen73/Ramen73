package com.ramen73.ramenchat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.firestore.Query
import com.ramen73.ramenchat.adapter.MessageAdapter
import com.ramen73.ramenchat.databinding.ActivityChatBinding
import com.ramen73.ramenchat.model.ChatRoom
import com.ramen73.ramenchat.model.Message
import com.ramen73.ramenchat.model.User
import com.ramen73.ramenchat.utils.AudioPlayer
import com.ramen73.ramenchat.utils.AudioRecorder
import com.ramen73.ramenchat.utils.FirebaseUtils
import com.ramen73.ramenchat.utils.gone
import com.ramen73.ramenchat.utils.toast
import com.ramen73.ramenchat.utils.visible
import kotlinx.coroutines.launch
import java.io.File

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var chatId: String
    private var otherUser: User? = null
    private var chatRoom: ChatRoom? = null

    private lateinit var audioRecorder: AudioRecorder
    private var audioFile: File? = null
    private var isRecording = false

    private val recordAudioLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else toast("Permesso microfono negato")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getStringExtra("CHAT_ID") ?: run { finish(); return }
        otherUser = intent.getParcelableExtra("OTHER_USER")

        audioRecorder = AudioRecorder(this)

        setupToolbar()
        setupRecyclerView()
        loadChatRoom()
        observeMessages()

        binding.btnSend.setOnClickListener { sendTextMessage() }
        setupAudioButton()

        binding.etMessage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                binding.btnSend.visibility = if (hasText) android.view.View.VISIBLE else android.view.View.GONE
                binding.btnRecord.visibility = if (hasText) android.view.View.GONE else android.view.View.VISIBLE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        binding.btnSend.visibility = android.view.View.GONE
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        otherUser?.let { user ->
            binding.tvRecipientName.text = user.displayName
            if (user.photoUrl.isNotEmpty()) {
                Glide.with(this).load(user.photoUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(binding.ivRecipientAvatar)
            }
            binding.tvStatus.text = if (user.online) "Online" else "Offline"
        }
    }

    private fun loadChatRoom() {
        FirebaseUtils.chatsCollection.document(chatId)
            .addSnapshotListener { snapshot, _ ->
                val room = snapshot?.toObject(ChatRoom::class.java) ?: return@addSnapshotListener
                chatRoom = room
                if (room.isGroup) {
                    binding.tvRecipientName.text = room.groupName
                    binding.tvStatus.text = "${room.participants.size} membri"
                    binding.ivRecipientAvatar.setImageResource(R.drawable.ic_default_avatar)
                }
                adapter.isGroup = room.isGroup
                adapter.notifyDataSetChanged()
            }
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
                adapter.submitList(messages) {
                    if (messages.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                    }
                }
            }
    }

    private fun sendTextMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return
        binding.etMessage.setText("")
        lifecycleScope.launch {
            try {
                FirebaseUtils.sendTextMessage(chatId, text, otherUser?.uid ?: "")
            } catch (e: Exception) {
                toast("Errore nell'invio")
            }
        }
    }

    private fun setupAudioButton() {
        binding.btnRecord.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (ContextCompat.checkSelfPermission(
                            this, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED) {
                        startRecording()
                    } else {
                        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isRecording) stopRecording()
                    true
                }
                else -> false
            }
        }
    }

    private fun startRecording() {
        try {
            audioFile = audioRecorder.start()
            isRecording = true
            binding.tvRecordingHint.visible()
            binding.btnRecord.setColorFilter(ContextCompat.getColor(this, R.color.error))
        } catch (e: Exception) {
            toast("Errore registrazione: ${e.localizedMessage}")
        }
    }

    private fun stopRecording() {
        val duration = audioRecorder.stop()
        isRecording = false
        binding.tvRecordingHint.gone()
        binding.btnRecord.clearColorFilter()

        val file = audioFile ?: return
        if (duration < 700) {
            file.delete()
            toast("Tieni premuto per registrare")
            return
        }
        lifecycleScope.launch {
            try {
                FirebaseUtils.sendAudioMessage(chatId, file, duration, otherUser?.uid ?: "")
                file.delete()
            } catch (e: Exception) {
                toast("Errore invio audio: ${e.localizedMessage}")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        AudioPlayer.stop()
        if (isRecording) audioRecorder.cancel()
    }
}
