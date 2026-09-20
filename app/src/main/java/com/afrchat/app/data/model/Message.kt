package com.afrchat.app.data.model

/** Message individuel. Sous-collection "conversations/{id}/messages/{messageId}". */
data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val type: String = "text", // text | image | video | audio | file | contact | system
    val text: String = "",
    val mediaUrl: String = "",
    val mediaDurationMs: Long = 0L,
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val sharedContactUid: String? = null,
    val replyToMessageId: String? = null,
    val reactions: Map<String, String> = emptyMap(), // uid -> emoji
    val status: String = "sent", // sent | delivered | read
    val deletedFor: List<String> = emptyList(),
    val isDeletedForEveryone: Boolean = false,
    val sentAt: Long = System.currentTimeMillis(),
    val clientTempId: String = "" // permet la déduplication lors d'un envoi hors-ligne rejoué
)
