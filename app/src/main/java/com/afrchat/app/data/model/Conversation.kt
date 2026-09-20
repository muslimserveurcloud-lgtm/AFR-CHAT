package com.afrchat.app.data.model

/** Discussion privée (1-à-1) ou entrée de liste pour un groupe. Collection "conversations/{id}". */
data class Conversation(
    val id: String = "",
    val type: String = "private", // "private" | "group"
    val participantIds: List<String> = emptyList(),
    val groupId: String? = null,
    val lastMessage: String = "",
    val lastMessageType: String = "text", // text | image | video | audio | file | contact
    val lastMessageSenderId: String = "",
    val lastMessageAt: Long = 0L,
    val unreadCount: Map<String, Int> = emptyMap(), // uid -> nombre de messages non lus
    val typingUserIds: List<String> = emptyList(),
    val isArchived: Map<String, Boolean> = emptyMap(),
    val isMuted: Map<String, Boolean> = emptyMap()
)
