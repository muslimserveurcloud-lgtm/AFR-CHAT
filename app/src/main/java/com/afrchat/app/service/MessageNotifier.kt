package com.afrchat.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import com.afrchat.app.MainActivity
import com.afrchat.app.R
import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.data.repository.ChatRepository
import com.afrchat.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Conversation actuellement ouverte à l'écran (pas de notification pour elle). */
object ActiveChat {
    @Volatile var conversationId: String? = null
}

/**
 * Notifications LOCALES de nouveaux messages, alimentées par Supabase Realtime.
 *
 * Limite importante : Supabase n'a pas de service de push intégré. Ces notifications n'apparaissent
 * donc que tant que l'application tourne (premier plan ou arrière-plan récent). Pour des notifications
 * quand l'app est complètement fermée, il faut ajouter un service de push (FCM, UnifiedPush, ntfy…) :
 * voir README, section "Limitations".
 *
 * Confidentialité : le contenu du message n'est jamais affiché, seulement le nom de l'expéditeur.
 */
@Singleton
class MessageNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val seen = HashSet<String>()

    fun start() {
        if (job?.isActive == true) return
        val uid = authRepository.currentUserId ?: return
        seen.clear()
        job = scope.launch {
            var first = true
            chatRepository.observeIncomingMessageSignals().conflate().collect {
                val latest = try { chatRepository.latestIncomingMessages(uid) } catch (_: Exception) { return@collect }
                if (first) {
                    // Premier passage : on mémorise l'existant sans notifier l'historique.
                    first = false
                    latest.forEach { seen.add(it.id) }
                    return@collect
                }
                latest.filter { seen.add(it.id) }.reversed().forEach { msg ->
                    if (msg.conversationId == ActiveChat.conversationId) return@forEach
                    val convo = try { chatRepository.getConversation(msg.conversationId) } catch (_: Exception) { null }
                    if (convo?.isMuted?.get(uid) == true) return@forEach
                    val senderName = (userRepository.getUser(msg.senderId) as? AfrResult.Success)?.data?.firstName
                        ?: context.getString(R.string.new_message_generic)
                    show(senderName, msg.conversationId, convo?.type == "group")
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun show(senderName: String, conversationId: String, isGroup: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_conversation_id", conversationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, conversationId.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, "afrchat_messages_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isGroup) "Nouveau message de groupe" else senderName)
            .setContentText("Vous avez reçu un nouveau message")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pendingIntent)
            .build()
        try {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(conversationId.hashCode(), notification)
        } catch (_: SecurityException) { /* permission POST_NOTIFICATIONS refusée */ }
    }
}

@HiltViewModel
class MessageNotifierViewModel @Inject constructor(
    private val notifier: MessageNotifier
) : ViewModel() {
    fun start() = notifier.start()
    fun stop() = notifier.stop()
}
