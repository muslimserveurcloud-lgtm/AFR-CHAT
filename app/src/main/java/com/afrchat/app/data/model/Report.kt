package com.afrchat.app.data.model

/** Signalement pour modération. Table "reports" — lue/traitée uniquement par les admins (RPC Supabase). */
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
