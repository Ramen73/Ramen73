package com.ramen73.ramenchat.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val type: MessageType = MessageType.TEXT
) {
    enum class MessageType { TEXT, IMAGE }
}
