package com.ramen73.ramenchat.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "",
    val audioDuration: Long = 0L,
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val type: String = TYPE_TEXT
) {
    companion object {
        const val TYPE_TEXT = "TEXT"
        const val TYPE_IMAGE = "IMAGE"
        const val TYPE_AUDIO = "AUDIO"
    }
}
