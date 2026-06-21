package com.ramen73.ramenchat.model

data class ChatRoom(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupPhoto: String = "",
    val createdBy: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSenderId: String = "",
    val unreadCount: Int = 0,
    // Populated locally, not stored in Firestore
    var otherUser: User = User()
)
