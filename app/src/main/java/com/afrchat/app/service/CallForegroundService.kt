package com.afrchat.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.pm.ServiceInfo
import com.afrchat.app.MainActivity
import com.afrchat.app.R

/**
 * Service au premier plan maintenant l'appel WebRTC actif même si l'app passe en arrière-plan
 * (obligatoire sur Android 9+ pour garder micro/caméra actifs hors de l'activité au premier plan).
 */
class CallForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isVideo = intent?.getBooleanExtra("isVideo", false) ?: false
        val notification = buildNotification(isVideo)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(isVideo: Boolean): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, "afrchat_calls_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isVideo) "Appel vidéo AFR CHAT en cours" else "Appel audio AFR CHAT en cours")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object { private const val NOTIFICATION_ID = 42 }
}
