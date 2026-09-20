package com.afrchat.app.data.model

/** Statut/story éphémère. Collection "stories/{id}", expire automatiquement via un champ TTL Firestore. */
data class Story(
    val id: String = "",
    val ownerUid: String = "",
    val type: String = "text", // text | image | video
    val content: String = "",
    val mediaUrl: String = "",
    val backgroundColor: String = "#5B4FE9",
    val viewerUids: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
)
