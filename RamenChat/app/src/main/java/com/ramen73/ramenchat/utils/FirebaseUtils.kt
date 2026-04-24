package com.ramen73.ramenchat.utils

import android.net.Uri
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.ramen73.ramenchat.model.ChatRoom
import com.ramen73.ramenchat.model.Message
import com.ramen73.ramenchat.model.User
import kotlinx.coroutines.tasks.await
import java.io.File
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

    /** Case-insensitive user search by name and email */
    suspend fun searchUsers(query: String): List<User> {
        val q = query.lowercase(Locale.getDefault())
        val end = q + ""

        val byName = try {
            usersCollection
                .orderBy("displayNameLower")
                .startAt(q).endAt(end)
                .limit(20).get().await()
                .toObjects(User::class.java)
        } catch (e: Exception) { emptyList() }

        val byEmail = usersCollection
            .orderBy("email")
            .startAt(q).endAt(end)
            .limit(20).get().await()
            .toObjects(User::class.java)

        return (byName + byEmail)
            .distinctBy { it.uid }
            .filter { it.uid != currentUserId }
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

    suspend fun createGroupChat(
        groupName: String,
        memberIds: List<String>
    ): String {
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
    }

    suspend fun sendImageMessage(chatId: String, imageUri: Uri, receiverId: String = "") {
        val me = getUser(currentUserId)
        val now = System.currentTimeMillis()
        val path = "chats/$chatId/images/$now.jpg"
        val url = uploadImage(imageUri, path)
        val message = Message(
            senderId = currentUserId,
            senderName = me?.displayName ?: "",
            receiverId = receiverId,
            imageUrl = url,
            timestamp = now,
            type = Message.TYPE_IMAGE
        )
        sendMessageInternal(chatId, message, "📷 Foto")
    }

    suspend fun sendAudioMessage(
        chatId: String,
        audioFile: File,
        durationMs: Long,
        receiverId: String = ""
    ) {
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
    }
}
