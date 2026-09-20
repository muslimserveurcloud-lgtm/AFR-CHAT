package com.afrchat.app.data.model

/** Signalement pour modération. Collection "reports/{id}" — lu/traité uniquement par les admins (Cloud Functions). */
data class Report(
    val id: String = "",
    val reporterUid: String = "",
    val targetType: String = "user", // user | message | group
    val targetId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "open", // open | reviewed | dismissed
    val createdAt: Long = System.currentTimeMillis()
)
