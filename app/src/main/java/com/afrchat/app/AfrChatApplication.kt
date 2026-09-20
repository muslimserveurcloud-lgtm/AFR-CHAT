package com.afrchat.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Point d'entrée de l'application AFR CHAT.
 * Initialise Hilt, Firebase App Check (anti-abus), la persistance Firestore hors-ligne
 * et les canaux de notification.
 */
@HiltAndroidApp
class AfrChatApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Active la persistance locale Firestore : les messages restent lisibles hors-ligne
        // et les écritures faites sans réseau sont automatiquement rejouées à la reconnexion.
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build()
            )
            .build()

        // App Check : empêche des clients non authentifiés/modifiés d'appeler notre backend Firebase.
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        val messages = NotificationChannel(
            "afrchat_messages_channel",
            getString(R.string.notification_channel_messages),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Notifications de nouveaux messages AFR CHAT" }

        val calls = NotificationChannel(
            "afrchat_calls_channel",
            getString(R.string.notification_channel_calls),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Appels entrants AFR CHAT"
            setSound(
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE),
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
            )
        }

        manager.createNotificationChannel(messages)
        manager.createNotificationChannel(calls)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
