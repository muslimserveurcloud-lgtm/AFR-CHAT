package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.Conversation
import com.afrchat.app.data.model.Message
import com.afrchat.app.data.remote.ConversationRow
import com.afrchat.app.data.remote.MessageInsert
import com.afrchat.app.data.remote.MessageRow
import com.afrchat.app.data.remote.RealtimeHub
import com.afrchat.app.data.remote.TableSpec
import com.afrchat.app.data.remote.jsonOf
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cœur de la messagerie : conversations, messages en temps réel (Supabase Realtime),
 * pagination, accusés de lecture, indicateur de frappe.
 * Les effets de bord d'un envoi (aperçu de conversation, compteurs non-lus) sont gérés par
 * le trigger SQL on_message_insert ; les actions sensibles passent par des fonctions RPC.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val client: SupabaseClient,
    private val hub: RealtimeHub
) {
    private val db get() = client.postgrest
    private val convoColumns = Columns.raw("*, conversation_members(*)")

    private suspend fun fetchConversations(): List<Conversation> =
        db.from("conversations").select(convoColumns) { order("last_message_at", Order.DESCENDING) }
            .decodeList<ConversationRow>().map { it.toModel() }

    private suspend fun fetchConversation(id: String): Conversation? =
        db.from("conversations").select(convoColumns) { filter { eq("id", id) } }
            .decodeList<ConversationRow>().firstOrNull()?.toModel()

    private suspend fun fetchMessages(conversationId: String, limit: Long, before: Long? = null): List<Message> =
        db.from("messages").select {
            filter {
                eq("conversation_id", conversationId)
                if (before != null) lt("sent_at", before)
            }
            order("sent_at", Order.DESCENDING)
            limit(limit)
        }.decodeList<MessageRow>().map { it.toModel() }

    suspend fun getConversation(id: String): Conversation? = fetchConversation(id)

    /** Liste des conversations de l'utilisateur (la RLS ne renvoie que les siennes), en temps réel. */
    fun observeConversations(uid: String): Flow<List<Conversation>> =
        hub.observe("conversations-$uid", TableSpec("conversations"), TableSpec("conversation_members", "user_id", uid)) {
            fetchConversations()
        }

    fun observeConversation(conversationId: String): Flow<Conversation?> =
        hub.observe(
            "conversation-$conversationId",
            TableSpec("conversations", "id", conversationId),
            TableSpec("conversation_members", "conversation_id", conversationId)
        ) { fetchConversation(conversationId) }

    /** Crée ou récupère la conversation privée existante entre l'utilisateur courant et [uidB]. */
    suspend fun getOrCreatePrivateConversation(uidA: String, uidB: String): AfrResult<String> = try {
        val id = db.rpc("get_or_create_private_conversation", jsonOf("other_id" to uidB)).decodeAs<String>()
        AfrResult.Success(id)
    } catch (e: Exception) {
        AfrResult.Error("Impossible de démarrer la conversation.", e)
    }

    /** Charge une première page de messages (les plus récents d'abord). Curseur = sentAt du plus ancien. */
    suspend fun loadRecentMessages(conversationId: String, pageSize: Long = 30): AfrResult<Pair<List<Message>, Long?>> = try {
        val msgs = fetchMessages(conversationId, pageSize)
        AfrResult.Success(msgs to msgs.lastOrNull()?.sentAt)
    } catch (e: Exception) {
        AfrResult.Error("Impossible de charger les messages.", e)
    }

    /** Chargement progressif des messages plus anciens que [beforeSentAt] (scroll vers le haut). */
    suspend fun loadOlderMessages(conversationId: String, beforeSentAt: Long, pageSize: Long = 30): AfrResult<Pair<List<Message>, Long?>> = try {
        val msgs = fetchMessages(conversationId, pageSize, before = beforeSentAt)
        AfrResult.Success(msgs to msgs.lastOrNull()?.sentAt)
    } catch (e: Exception) {
        AfrResult.Error("Impossible de charger l'historique.", e)
    }

    /** Les 50 derniers messages, rafraîchis en direct à chaque insertion / modification. */
    fun observeNewMessages(conversationId: String): Flow<List<Message>> =
        hub.observe("messages-$conversationId", TableSpec("messages", "conversation_id", conversationId)) {
            fetchMessages(conversationId, 50)
        }

    suspend fun sendTextMessage(conversationId: String, senderId: String, text: String, replyToMessageId: String? = null): AfrResult<Unit> =
        insertMessage(MessageInsert(
            conversationId = conversationId, senderId = senderId, type = "text", text = text,
            replyToMessageId = replyToMessageId, clientTempId = UUID.randomUUID().toString()
        ))

    suspend fun sendMediaMessage(
        conversationId: String, senderId: String, type: String, mediaUrl: String,
        fileName: String = "", fileSizeBytes: Long = 0L, mediaDurationMs: Long = 0L
    ): AfrResult<Unit> = insertMessage(MessageInsert(
        conversationId = conversationId, senderId = senderId, type = type, mediaUrl = mediaUrl,
        fileName = fileName, fileSizeBytes = fileSizeBytes, mediaDurationMs = mediaDurationMs,
        clientTempId = UUID.randomUUID().toString()
    ))

    suspend fun sendContactCard(conversationId: String, senderId: String, sharedContactUid: String, displayName: String): AfrResult<Unit> =
        insertMessage(MessageInsert(
            conversationId = conversationId, senderId = senderId, type = "contact",
            text = displayName, sharedContactUid = sharedContactUid, clientTempId = UUID.randomUUID().toString()
        ))

    private suspend fun insertMessage(message: MessageInsert): AfrResult<Unit> = try {
        db.from("messages").insert(message)
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("L'envoi du message a échoué. Vérifiez votre connexion et réessayez.", e)
    }

    suspend fun markMessagesAsRead(conversationId: String, uid: String) {
        try { db.rpc("mark_conversation_read", jsonOf("conv" to conversationId)) } catch (_: Exception) { }
    }

    suspend fun setTyping(conversationId: String, uid: String, isTyping: Boolean) {
        try {
            db.from("conversation_members").update(jsonOf("is_typing" to isTyping)) {
                filter { eq("conversation_id", conversationId); eq("user_id", uid) }
            }
        } catch (_: Exception) { }
    }

    suspend fun deleteMessageForMe(conversationId: String, messageId: String, uid: String) {
        try { db.rpc("delete_message_for_me", jsonOf("msg" to messageId)) } catch (_: Exception) { }
    }

    suspend fun deleteMessageForEveryone(conversationId: String, messageId: String) {
        try { db.rpc("delete_message_for_everyone", jsonOf("msg" to messageId)) } catch (_: Exception) { }
    }

    suspend fun addReaction(conversationId: String, messageId: String, uid: String, emoji: String) {
        try { db.rpc("react_to_message", jsonOf("msg" to messageId, "emoji" to emoji)) } catch (_: Exception) { }
    }

    suspend fun forwardMessage(fromConversationId: String, toConversationId: String, message: Message, senderId: String): AfrResult<Unit> {
        return when (message.type) {
            "text" -> sendTextMessage(toConversationId, senderId, message.text)
            else -> sendMediaMessage(toConversationId, senderId, message.type, message.mediaUrl, message.fileName, message.fileSizeBytes, message.mediaDurationMs)
        }
    }

    suspend fun setArchived(conversationId: String, uid: String, archived: Boolean) {
        db.from("conversation_members").update(jsonOf("is_archived" to archived)) {
            filter { eq("conversation_id", conversationId); eq("user_id", uid) }
        }
    }

    suspend fun setMuted(conversationId: String, uid: String, muted: Boolean) {
        db.from("conversation_members").update(jsonOf("is_muted" to muted)) {
            filter { eq("conversation_id", conversationId); eq("user_id", uid) }
        }
    }

    /** Nouveaux messages reçus (insertions), pour les notifications locales. La RLS filtre déjà les conversations. */
    fun observeIncomingMessageSignals(): Flow<Unit> = hub.changes("incoming-messages", TableSpec("messages"))

    /** Derniers messages reçus par l'utilisateur (hors les siens), du plus récent au plus ancien. */
    suspend fun latestIncomingMessages(uid: String, limit: Long = 5): List<Message> =
        db.from("messages").select {
            filter { neq("sender_id", uid) }
            order("sent_at", Order.DESCENDING)
            limit(limit)
        }.decodeList<MessageRow>().map { it.toModel() }
}
