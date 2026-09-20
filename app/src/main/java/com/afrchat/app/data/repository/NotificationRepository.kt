package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * L'envoi effectif des notifications push (FCM) est déclenché côté serveur par une Cloud
 * Function (onNewMessageCreated dans firebase/functions/index.js) qui écoute les écritures
 * dans "conversations/{id}/messages/{id}" et envoie un message data-only aux fcmTokens des
 * destinataires. Le contenu du message n'est PAS inclus dans la notification système :
 * seul le nom de l'expéditeur et "Nouveau message" apparaissent sur l'écran verrouillé,
 * le contenu réel étant récupéré et affiché uniquement une fois l'app déverrouillée/ouverte.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun recordNotificationSeen(notificationId: String, uid: String): AfrResult<Unit> = try {
        firestore.collection("users").document(uid)
            .collection("notifications").document(notificationId)
            .update("seen", true).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Erreur notification.", e)
    }
}
