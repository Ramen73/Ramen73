package com.ramen73.ramenchat.utils

import android.net.Uri
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.ktx.storage
import com.ramen73.ramenchat.AppConfig
import com.ramen73.ramenchat.model.ChatRoom
import com.ramen73.ramenchat.model.Message
import com.ramen73.ramenchat.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object FirebaseUtils {

    val auth = Firebase.auth
    val db = Firebase.firestore
    val storage = Firebase.storage

    val currentUserId get() = auth.currentUser?.uid ?: ""

    val usersCollection get() = db.collection("users")
    val chatsCollection get() = db.collection("chats")

    fun messagesCollection(chatId: String) =
        chatsCollection.document(chatId).collection("messages")

    // ── User helpers ─────────────────────────────────────────────────────────
    suspend fun saveUser(user: User) {
        val toSave = user.copy(displayNameLower = user.displayName.lowercase(Locale.getDefault()))
        usersCollection.document(user.uid).set(toSave).await()
    }

    suspend fun getUser(uid: String): User? =
        usersCollection.document(uid).get().await().toObject(User::class.java)

    suspend fun updateOnlineStatus(isOnline: Boolean) {
        if (currentUserId.isBlank()) return
        usersCollection.document(currentUserId).update(
            mapOf(
                "online" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun updateProfile(displayName: String, bio: String) {
        if (currentUserId.isBlank()) return
        usersCollection.document(currentUserId).update(
            mapOf(
                "displayName" to displayName,
                "displayNameLower" to displayName.lowercase(Locale.getDefault()),
                "bio" to bio
            )
        ).await()
    }

    suspend fun updateProfilePhoto(url: String) {
        if (currentUserId.isBlank()) return
        usersCollection.document(currentUserId)
            .update("photoUrl", url).await()
    }

    // ── FCM helpers ──────────────────────────────────────────────────────────
    suspend fun refreshAndSaveFcmToken() {
        if (currentUserId.isBlank()) return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            usersCollection.document(currentUserId).update("fcmToken", token).await()
        } catch (_: Exception) {}
    }

    suspend fun saveFcmToken(token: String) {
        if (currentUserId.isBlank()) return
        try {
            usersCollection.document(currentUserId).update("fcmToken", token).await()
        } catch (_: Exception) {}
    }

    suspend fun sendPushNotification(recipientId: String, senderName: String, messageBody: String) {
        if (AppConfig.FCM_SERVER_KEY == "INSERISCI_LA_TUA_CHIAVE_SERVER_FCM_QUI") return
        val recipient = getUser(recipientId) ?: return
        val token = recipient.fcmToken.ifEmpty { return }
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("to", token)
                    put("notification", JSONObject().apply {
                        put("title", "RamenChat – $senderName")
                        put("body", messageBody)
                        put("sound", "default")
                    })
                }
                val conn = URL("https://fcm.googleapis.com/fcm/send")
                    .openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "key=${AppConfig.FCM_SERVER_KEY}")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    outputStream.write(json.toString().toByteArray())
                    responseCode
                }
            } catch (_: Exception) {}
        }
    }

    // ── Search ───────────────────────────────────────────────────────────────
    /** Case-insensitive partial search: works with any name, also without displayNameLower field */
    suspend fun searchUsers(query: String): List<User> {
        val q = query.lowercase(Locale.getDefault()).trim()
        if (q.isEmpty()) return emptyList()
        val all = usersCollection.get().await().toObjects(User::class.java)
        return all.filter { user ->
            user.uid != currentUserId && (
                user.displayName.lowercase(Locale.getDefault()).contains(q) ||
                user.email.lowercase(Locale.getDefault()).contains(q)
            )
        }
    }

    // ── Upload helpers ───────────────────────────────────────────────────────
    suspend fun uploadImage(uri: Uri, path: String): String {
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadFile(file: File, path: String): String {
        val ref = storage.reference.child(path)
        ref.putFile(Uri.fromFile(file)).await()
        return ref.downloadUrl.await().toString()
    }

    // ── Chat helpers ─────────────────────────────────────────────────────────
    fun buildChatId(uid1: String, uid2: String): String =
        if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"

    suspend fun getOrCreateChatRoom(otherUserId: String): String {
        val chatId = buildChatId(currentUserId, otherUserId)
        val doc = chatsCollection.document(chatId).get().await()
        if (!doc.exists()) {
            val me = getUser(currentUserId)
            val other = getUser(otherUserId)
            val room = ChatRoom(
                chatId = chatId,
                participants = listOf(currentUserId, otherUserId),
                participantNames = mapOf(
                    currentUserId to (me?.displayName ?: ""),
                    otherUserId to (other?.displayName ?: "")
                ),
                lastMessageTime = System.currentTimeMillis()
            )
            chatsCollection.document(chatId).set(room).await()
        }
        return chatId
    }

    suspend fun createGroupChat(groupName: String, memberIds: List<String>): String {
        val allMembers = (memberIds + currentUserId).distinct()
        val chatRef = chatsCollection.document()
        val names = mutableMapOf<String, String>()
        for (uid in allMembers) {
            getUser(uid)?.let { names[uid] = it.displayName }
        }
        val room = ChatRoom(
            chatId = chatRef.id,
            participants = allMembers,
            participantNames = names,
            isGroup = true,
            groupName = groupName,
            createdBy = currentUserId,
            lastMessageTime = System.currentTimeMillis()
        )
        chatRef.set(room).await()
        return chatRef.id
    }

    suspend fun addMemberToGroup(chatId: String, userId: String) {
        val user = getUser(userId) ?: return
        chatsCollection.document(chatId).update(
            mapOf(
                "participants" to FieldValue.arrayUnion(userId),
                "participantNames.$userId" to user.displayName
            )
        ).await()
    }

    private suspend fun sendMessageInternal(chatId: String, message: Message, preview: String) {
        val msgRef = messagesCollection(chatId).document()
        val toSave = message.copy(messageId = msgRef.id)
        msgRef.set(toSave).await()
        chatsCollection.document(chatId).update(
            mapOf(
                "lastMessage" to preview,
                "lastMessageTime" to toSave.timestamp,
                "lastMessageSenderId" to currentUserId
            )
        ).await()
    }

    suspend fun sendTextMessage(chatId: String, text: String, receiverId: String = "") {
        val me = getUser(currentUserId)
        val message = Message(
            senderId = currentUserId,
            senderName = me?.displayName ?: "",
            receiverId = receiverId,
            text = text,
            timestamp = System.currentTimeMillis(),
            type = Message.TYPE_TEXT
        )
        sendMessageInternal(chatId, message, text)
        if (receiverId.isNotEmpty()) {
            sendPushNotification(receiverId, me?.displayName ?: "Qualcuno", text)
        }
    }

    suspend fun sendAudioMessage(chatId: String, audioFile: File, durationMs: Long, receiverId: String = "") {
        val me = getUser(currentUserId)
        val now = System.currentTimeMillis()
        val path = "chats/$chatId/audio/$now.m4a"
        val url = uploadFile(audioFile, path)
        val message = Message(
            senderId = currentUserId,
            senderName = me?.displayName ?: "",
            receiverId = receiverId,
            audioUrl = url,
            audioDuration = durationMs,
            timestamp = now,
            type = Message.TYPE_AUDIO
        )
        sendMessageInternal(chatId, message, "🎤 Messaggio vocale")
        if (receiverId.isNotEmpty()) {
            sendPushNotification(receiverId, me?.displayName ?: "Qualcuno", "🎤 Messaggio vocale")
        }
    }
}
