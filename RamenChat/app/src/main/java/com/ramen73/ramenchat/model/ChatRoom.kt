package com.ramen73.ramenchat.model

data class ChatRoom(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSenderId: String = "",
    val unreadCount: Int = 0,
    // Populated locally, not stored in Firestore
    var otherUser: User = User()
)
