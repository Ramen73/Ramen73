package com.ramen73.ramenchat.utils

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.ramen73.ramenchat.model.ChatRoom
import com.ramen73.ramenchat.model.Message
import com.ramen73.ramenchat.model.User
import kotlinx.coroutines.tasks.await

object FirebaseUtils {

    val auth = Firebase.auth
    val db = Firebase.firestore
    val storage = Firebase.storage

    val currentUserId get() = auth.currentUser?.uid ?: ""

    // ── Collections ──────────────────────────────────────────────────────────
    val usersCollection get() = db.collection("users")
    val chatsCollection get() = db.collection("chats")

    fun messagesCollection(chatId: String) =
        chatsCollection.document(chatId).collection("messages")

    // ── User helpers ─────────────────────────────────────────────────────────
    suspend fun saveUser(user: User) {
        usersCollection.document(user.uid).set(user).await()
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

    suspend fun searchUsers(query: String): List<User> {
        val snap = usersCollection
            .whereGreaterThanOrEqualTo("displayName", query)
            .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
            .get().await()
        return snap.toObjects(User::class.java).filter { it.uid != currentUserId }
    }

    // ── Chat helpers ─────────────────────────────────────────────────────────
    fun buildChatId(uid1: String, uid2: String): String =
        if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"

    suspend fun getOrCreateChatRoom(otherUserId: String): String {
        val chatId = buildChatId(currentUserId, otherUserId)
        val doc = chatsCollection.document(chatId).get().await()
        if (!doc.exists()) {
            val room = ChatRoom(
                chatId = chatId,
                participants = listOf(currentUserId, otherUserId)
            )
            chatsCollection.document(chatId).set(room).await()
        }
        return chatId
    }

    suspend fun sendMessage(chatId: String, text: String, receiverId: String) {
        val msgRef = messagesCollection(chatId).document()
        val message = Message(
            messageId = msgRef.id,
            senderId = currentUserId,
            receiverId = receiverId,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        msgRef.set(message).await()
        chatsCollection.document(chatId).update(
            mapOf(
                "lastMessage" to text,
                "lastMessageTime" to message.timestamp,
                "lastMessageSenderId" to currentUserId
            )
        ).await()
    }
}
