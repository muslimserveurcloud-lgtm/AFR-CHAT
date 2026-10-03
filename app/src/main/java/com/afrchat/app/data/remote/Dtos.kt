package com.afrchat.app.data.remote

import com.afrchat.app.data.model.CallSession
import com.afrchat.app.data.model.Conversation
import com.afrchat.app.data.model.Group
import com.afrchat.app.data.model.IceCandidateData
import com.afrchat.app.data.model.Message
import com.afrchat.app.data.model.PrivacySettings
import com.afrchat.app.data.model.Report
import com.afrchat.app.data.model.Story
import com.afrchat.app.data.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lignes Postgres (snake_case) <-> modèles de l'app (camelCase).
 * Le reste de l'application ne manipule jamais ces classes : seuls les repositories les utilisent.
 */

// ---------------------------------------------------------------- profils
@Serializable
data class PrivacyDto(
    val showLastSeen: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val showReadReceipts: Boolean = true,
    val whoCanAddToGroups: String = "everyone"
)

@Serializable
data class ProfileRow(
    val id: String,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    @SerialName("photo_url") val photoUrl: String = "",
    @SerialName("status_message") val statusMessage: String = "",
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("last_seen") val lastSeen: Long = 0L,
    val privacy: PrivacyDto = PrivacyDto(),
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_banned") val isBanned: Boolean = false,
    @SerialName("created_at") val createdAt: Long = 0L
) {
    fun toModel() = User(
        uid = id, firstName = firstName, lastName = lastName, email = email, phone = phone,
        photoUrl = photoUrl, statusMessage = statusMessage, isOnline = isOnline, lastSeen = lastSeen,
        privacy = PrivacySettings(privacy.showLastSeen, privacy.showOnlineStatus, privacy.showReadReceipts, privacy.whoCanAddToGroups),
        isAdmin = isAdmin, isBanned = isBanned, createdAt = createdAt
    )
}

// ---------------------------------------------------------------- conversations
@Serializable
data class MemberRow(
    @SerialName("conversation_id") val conversationId: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_muted") val isMuted: Boolean = false,
    @SerialName("is_typing") val isTyping: Boolean = false
)

@Serializable
data class ConversationRow(
    val id: String,
    val type: String = "private",
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("last_message") val lastMessage: String = "",
    @SerialName("last_message_type") val lastMessageType: String = "text",
    @SerialName("last_message_sender_id") val lastMessageSenderId: String? = null,
    @SerialName("last_message_at") val lastMessageAt: Long = 0L,
    @SerialName("conversation_members") val members: List<MemberRow> = emptyList()
) {
    fun toModel() = Conversation(
        id = id, type = type,
        participantIds = members.map { it.userId },
        groupId = groupId,
        lastMessage = lastMessage, lastMessageType = lastMessageType,
        lastMessageSenderId = lastMessageSenderId.orEmpty(), lastMessageAt = lastMessageAt,
        unreadCount = members.associate { it.userId to it.unreadCount },
        typingUserIds = members.filter { it.isTyping }.map { it.userId },
        isArchived = members.associate { it.userId to it.isArchived },
        isMuted = members.associate { it.userId to it.isMuted }
    )
}

// ---------------------------------------------------------------- messages
@Serializable
data class MessageRow(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val type: String = "text",
    val text: String = "",
    @SerialName("media_url") val mediaUrl: String = "",
    @SerialName("media_duration_ms") val mediaDurationMs: Long = 0L,
    @SerialName("file_name") val fileName: String = "",
    @SerialName("file_size_bytes") val fileSizeBytes: Long = 0L,
    @SerialName("shared_contact_uid") val sharedContactUid: String? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val status: String = "sent",
    @SerialName("deleted_for") val deletedFor: List<String> = emptyList(),
    @SerialName("is_deleted_for_everyone") val isDeletedForEveryone: Boolean = false,
    @SerialName("sent_at") val sentAt: Long = 0L,
    @SerialName("client_temp_id") val clientTempId: String = ""
) {
    fun toModel() = Message(
        id = id, conversationId = conversationId, senderId = senderId, type = type, text = text,
        mediaUrl = mediaUrl, mediaDurationMs = mediaDurationMs, fileName = fileName, fileSizeBytes = fileSizeBytes,
        sharedContactUid = sharedContactUid, replyToMessageId = replyToMessageId, reactions = reactions,
        status = status, deletedFor = deletedFor, isDeletedForEveryone = isDeletedForEveryone,
        sentAt = sentAt, clientTempId = clientTempId
    )
}

/** Insertion d'un message : id, statut et horodatage sont attribués par le serveur. */
@Serializable
data class MessageInsert(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val type: String,
    val text: String = "",
    @SerialName("media_url") val mediaUrl: String = "",
    @SerialName("media_duration_ms") val mediaDurationMs: Long = 0L,
    @SerialName("file_name") val fileName: String = "",
    @SerialName("file_size_bytes") val fileSizeBytes: Long = 0L,
    @SerialName("shared_contact_uid") val sharedContactUid: String? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerialName("client_temp_id") val clientTempId: String = ""
)

// ---------------------------------------------------------------- groupes
@Serializable
data class GroupMemberRow(
    @SerialName("user_id") val userId: String,
    val role: String = "member"
)

@Serializable
data class GroupRow(
    val id: String,
    val name: String = "",
    val description: String = "",
    @SerialName("photo_url") val photoUrl: String = "",
    @SerialName("owner_id") val ownerId: String = "",
    @SerialName("only_admins_can_post") val onlyAdminsCanPost: Boolean = false,
    @SerialName("only_admins_can_edit_info") val onlyAdminsCanEditInfo: Boolean = true,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("group_members") val members: List<GroupMemberRow> = emptyList()
) {
    fun toModel() = Group(
        id = id, name = name, description = description, photoUrl = photoUrl, ownerUid = ownerId,
        adminUids = members.filter { it.role == "owner" || it.role == "admin" }.map { it.userId },
        memberUids = members.map { it.userId },
        createdAt = createdAt, onlyAdminsCanPost = onlyAdminsCanPost, onlyAdminsCanEditInfo = onlyAdminsCanEditInfo
    )
}

// ---------------------------------------------------------------- statuts
@Serializable
data class StoryRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val type: String = "text",
    val content: String = "",
    @SerialName("media_url") val mediaUrl: String = "",
    @SerialName("background_color") val backgroundColor: String = "#5B4FE9",
    @SerialName("viewer_ids") val viewerIds: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("expires_at") val expiresAt: Long = 0L
) {
    fun toModel() = Story(
        id = id, ownerUid = ownerId, type = type, content = content, mediaUrl = mediaUrl,
        backgroundColor = backgroundColor, viewerUids = viewerIds, createdAt = createdAt, expiresAt = expiresAt
    )
}

@Serializable
data class StoryInsert(
    @SerialName("owner_id") val ownerId: String,
    val type: String,
    val content: String = "",
    @SerialName("media_url") val mediaUrl: String = "",
    @SerialName("background_color") val backgroundColor: String = "#5B4FE9"
)

// ---------------------------------------------------------------- appels
@Serializable
data class CallRow(
    val id: String,
    @SerialName("caller_id") val callerId: String,
    @SerialName("callee_id") val calleeId: String,
    @SerialName("is_video") val isVideo: Boolean = false,
    val status: String = "ringing",
    @SerialName("offer_sdp") val offerSdp: String? = null,
    @SerialName("answer_sdp") val answerSdp: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("ended_at") val endedAt: Long? = null
) {
    fun toModel() = CallSession(
        id = id, callerUid = callerId, calleeUid = calleeId, isVideo = isVideo, status = status,
        offerSdp = offerSdp, answerSdp = answerSdp, createdAt = createdAt, endedAt = endedAt
    )
}

@Serializable
data class CallInsert(
    val id: String,
    @SerialName("caller_id") val callerId: String,
    @SerialName("callee_id") val calleeId: String,
    @SerialName("is_video") val isVideo: Boolean,
    val status: String = "ringing",
    @SerialName("offer_sdp") val offerSdp: String
)

@Serializable
data class CandidateRow(
    val id: Long = 0L,
    @SerialName("from_role") val fromRole: String = "",
    @SerialName("sdp_mid") val sdpMid: String = "",
    @SerialName("sdp_m_line_index") val sdpMLineIndex: Int = 0,
    val candidate: String = ""
) {
    fun toModel() = IceCandidateData(sdpMid, sdpMLineIndex, candidate)
}

@Serializable
data class CandidateInsert(
    @SerialName("call_id") val callId: String,
    @SerialName("from_role") val fromRole: String,
    @SerialName("sdp_mid") val sdpMid: String,
    @SerialName("sdp_m_line_index") val sdpMLineIndex: Int,
    val candidate: String
)

// ---------------------------------------------------------------- signalements
@Serializable
data class ReportRow(
    val id: String,
    @SerialName("reporter_id") val reporterId: String = "",
    @SerialName("target_type") val targetType: String = "user",
    @SerialName("target_id") val targetId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "open",
    @SerialName("created_at") val createdAt: Long = 0L
) {
    fun toModel() = Report(id, reporterId, targetType, targetId, reason, details, status, createdAt)
}

@Serializable
data class ReportInsert(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String,
    val reason: String,
    val details: String
)
