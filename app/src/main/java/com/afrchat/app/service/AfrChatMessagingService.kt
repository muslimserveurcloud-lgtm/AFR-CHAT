package com.afrchat.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.afrchat.app.MainActivity
import com.afrchat.app.R
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.data.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reçoit les notifications push envoyées par la Cloud Function onNewMessageCreated
 * (firebase/functions/index.js). Les messages FCM sont "data-only" : AUCUN contenu privé
 * n'est présent dans la charge utile visible sur l'écran verrouillé — seul le prénom de
 * l'expéditeur et un texte générique apparaissent, conformément à l'exigence de confidentialité.
 */
@AndroidEntryPoint
class AfrChatMessagingService : FirebaseMessagingService() {

    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var authRepository: AuthRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = authRepository.currentUserId ?: return
        CoroutineScope(Dispatchers.IO).launch { userRepository.registerFcmToken(uid) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        when (data["type"]) {
            "new_message" -> showMessageNotification(
                title = data["senderName"] ?: getString(R.string.new_message_generic),
                conversationId = data["conversationId"].orEmpty(),
                isGroup = data["isGroup"] == "true"
            )
            "incoming_call" -> showCallNotification(
                callerName = data["callerName"] ?: "Appel entrant",
                callId = data["callId"].orEmpty(),
                isVideo = data["isVideo"] == "true"
            )
        }
    }

    private fun showMessageNotification(title: String, conversationId: String, isGroup: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_conversation_id", conversationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, conversationId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Contenu volontairement générique sur l'écran verrouillé : pas d'aperçu du texte du message.
        val notification = NotificationCompat.Builder(this, "afrchat_messages_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isGroup) "Nouveau message de groupe" else title)
            .setContentText("Vous avez reçu un nouveau message")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pendingIntent)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(conversationId.hashCode(), notification)
    }

    private fun showCallNotification(callerName: String, callId: String, isVideo: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("incoming_call_id", callId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, callId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, "afrchat_calls_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isVideo) "Appel vidéo entrant" else "Appel audio entrant")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(callId.hashCode(), notification)
    }
}
