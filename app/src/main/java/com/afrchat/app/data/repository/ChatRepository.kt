package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.Conversation
import com.afrchat.app.data.model.Message
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cœur de la messagerie instantanée : conversations, messages en temps réel,
 * pagination de l'historique, accusés de réception, indicateur de frappe.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun conversationsRef() = firestore.collection("conversations")
    private fun messagesRef(conversationId: String) =
        conversationsRef().document(conversationId).collection("messages")

    /** Liste des conversations de l'utilisateur, triée par dernier message, en temps réel. */
    fun observeConversations(uid: String): Flow<List<Conversation>> = callbackFlow {
        val reg = conversationsRef()
            .whereArrayContains("participantIds", uid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Conversation::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    fun observeConversation(conversationId: String): Flow<Conversation?> = callbackFlow {
        val reg = conversationsRef().document(conversationId).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(Conversation::class.java))
        }
        awaitClose { reg.remove() }
    }

    /** Crée ou récupère la conversation privée existante entre deux utilisateurs. */
    suspend fun getOrCreatePrivateConversation(uidA: String, uidB: String): AfrResult<String> = try {
        val existing = conversationsRef()
            .whereEqualTo("type", "private")
            .whereArrayContains("participantIds", uidA)
            .get().await()
            .toObjects(Conversation::class.java)
            .firstOrNull { it.participantIds.toSet() == setOf(uidA, uidB) }

        if (existing != null) return AfrResult.Success(existing.id)

        val newDoc = conversationsRef().document()
        val conversation = Conversation(
            id = newDoc.id,
            type = "private",
            participantIds = listOf(uidA, uidB),
            lastMessageAt = System.currentTimeMillis()
        )
        newDoc.set(conversation).await()
        AfrResult.Success(newDoc.id)
    } catch (e: Exception) {
        AfrResult.Error("Impossible de démarrer la conversation.", e)
    }

    /** Charge une première page de messages (les plus récents en premier). */
    suspend fun loadRecentMessages(conversationId: String, pageSize: Long = 30): AfrResult<Pair<List<Message>, DocumentSnapshot?>> = try {
        val snap = messagesRef(conversationId).orderBy("sentAt", Query.Direction.DESCENDING).limit(pageSize).get().await()
        AfrResult.Success(snap.toObjects(Message::class.java) to snap.documents.lastOrNull())
    } catch (e: Exception) {
        AfrResult.Error("Impossible de charger les messages.", e)
    }

    /** Chargement progressif des messages plus anciens (scroll vers le haut). */
    suspend fun loadOlderMessages(conversationId: String, startAfter: DocumentSnapshot, pageSize: Long = 30): AfrResult<Pair<List<Message>, DocumentSnapshot?>> = try {
        val snap = messagesRef(conversationId).orderBy("sentAt", Query.Direction.DESCENDING)
            .startAfter(startAfter).limit(pageSize).get().await()
        AfrResult.Success(snap.toObjects(Message::class.java) to snap.documents.lastOrNull())
    } catch (e: Exception) {
        AfrResult.Error("Impossible de charger l'historique.", e)
    }

    /** Écoute les nouveaux messages en direct (Firestore renvoie automatiquement l'écriture locale hors-ligne, puis la confirmation serveur). */
    fun observeNewMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val reg = messagesRef(conversationId)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Message::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    suspend fun sendTextMessage(conversationId: String, senderId: String, text: String, replyToMessageId: String? = null): AfrResult<Unit> =
        sendMessage(conversationId, Message(
            conversationId = conversationId,
            senderId = senderId,
            type = "text",
            text = text,
            replyToMessageId = replyToMessageId,
            clientTempId = UUID.randomUUID().toString()
        ), previewText = text, previewType = "text")

    suspend fun sendMediaMessage(
        conversationId: String, senderId: String, type: String, mediaUrl: String,
        fileName: String = "", fileSizeBytes: Long = 0L, mediaDurationMs: Long = 0L
    ): AfrResult<Unit> {
        val preview = when (type) {
            "image" -> "📷 Photo"
            "video" -> "🎥 Vidéo"
            "audio" -> "🎤 Message vocal"
            else -> "📎 $fileName"
        }
        return sendMessage(conversationId, Message(
            conversationId = conversationId,
            senderId = senderId,
            type = type,
            mediaUrl = mediaUrl,
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            mediaDurationMs = mediaDurationMs,
            clientTempId = UUID.randomUUID().toString()
        ), previewText = preview, previewType = type)
    }

    suspend fun sendContactCard(conversationId: String, senderId: String, sharedContactUid: String, displayName: String): AfrResult<Unit> =
        sendMessage(conversationId, Message(
            conversationId = conversationId, senderId = senderId, type = "contact",
            sharedContactUid = sharedContactUid, text = displayName,
            clientTempId = UUID.randomUUID().toString()
        ), previewText = "👤 Contact : $displayName", previewType = "contact")

    private suspend fun sendMessage(conversationId: String, message: Message, previewText: String, previewType: String): AfrResult<Unit> = try {
        val doc = messagesRef(conversationId).document()
        val finalMessage = message.copy(id = doc.id, sentAt = System.currentTimeMillis())
        doc.set(finalMessage).await()

        val convo = conversationsRef().document(conversationId).get().await().toObject(Conversation::class.java)
        val unreadUpdates = mutableMapOf<String, Any>()
        convo?.participantIds?.filter { it != message.senderId }?.forEach { uid ->
            unreadUpdates["unreadCount.$uid"] = FieldValue.increment(1)
        }

        conversationsRef().document(conversationId).update(
            mapOf(
                "lastMessage" to previewText,
                "lastMessageType" to previewType,
                "lastMessageSenderId" to message.senderId,
                "lastMessageAt" to finalMessage.sentAt
            ) + unreadUpdates
        ).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("L'envoi du message a échoué. Il sera renvoyé automatiquement à la reconnexion.", e)
    }

    suspend fun markMessagesAsRead(conversationId: String, uid: String) {
        try {
            conversationsRef().document(conversationId).update("unreadCount.$uid", 0).await()
            val unread = messagesRef(conversationId)
                .whereNotEqualTo("senderId", uid)
                .whereEqualTo("status", "delivered")
                .get().await()
            val batch = firestore.batch()
            unread.documents.forEach { batch.update(it.reference, "status", "read") }
            batch.commit().await()
        } catch (_: Exception) { }
    }

    suspend fun setTyping(conversationId: String, uid: String, isTyping: Boolean) {
        try {
            val update = if (isTyping) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
            conversationsRef().document(conversationId).update("typingUserIds", update).await()
        } catch (_: Exception) { }
    }

    suspend fun deleteMessageForMe(conversationId: String, messageId: String, uid: String) {
        messagesRef(conversationId).document(messageId).update("deletedFor", FieldValue.arrayUnion(uid)).await()
    }

    suspend fun deleteMessageForEveryone(conversationId: String, messageId: String) {
        messagesRef(conversationId).document(messageId).update(
            mapOf("isDeletedForEveryone" to true, "text" to "", "mediaUrl" to "")
        ).await()
    }

    suspend fun addReaction(conversationId: String, messageId: String, uid: String, emoji: String) {
        messagesRef(conversationId).document(messageId).update("reactions.$uid", emoji).await()
    }

    suspend fun forwardMessage(fromConversationId: String, toConversationId: String, message: Message, senderId: String): AfrResult<Unit> {
        return when (message.type) {
            "text" -> sendTextMessage(toConversationId, senderId, message.text)
            else -> sendMediaMessage(toConversationId, senderId, message.type, message.mediaUrl, message.fileName, message.fileSizeBytes, message.mediaDurationMs)
        }
    }

    suspend fun setArchived(conversationId: String, uid: String, archived: Boolean) {
        conversationsRef().document(conversationId).update("isArchived.$uid", archived).await()
    }

    suspend fun setMuted(conversationId: String, uid: String, muted: Boolean) {
        conversationsRef().document(conversationId).update("isMuted.$uid", muted).await()
    }
}
