package com.afrchat.app.data.model

/** Groupe de discussion. Collection "groups/{id}". */
data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val photoUrl: String = "",
    val ownerUid: String = "",
    val adminUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val onlyAdminsCanPost: Boolean = false,
    val onlyAdminsCanEditInfo: Boolean = true
)

data class GroupMember(
    val uid: String = "",
    val role: String = "member", // "owner" | "admin" | "member"
    val joinedAt: Long = System.currentTimeMillis()
)
