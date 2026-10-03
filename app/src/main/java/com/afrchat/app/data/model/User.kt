package com.afrchat.app.data.model

/** Représente un utilisateur AFR CHAT. Table Supabase "profiles" (id = uid Supabase Auth). */
data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val statusMessage: String = "Salut, j'utilise AFR CHAT !",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val privacy: PrivacySettings = PrivacySettings(),
    val isAdmin: Boolean = false,
    val isBanned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

data class PrivacySettings(
    val showLastSeen: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val showReadReceipts: Boolean = true,
    val whoCanAddToGroups: String = "everyone" // "everyone" | "contacts" | "nobody"
)
