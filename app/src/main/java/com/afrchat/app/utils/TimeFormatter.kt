package com.afrchat.app.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeFormatter {
    fun formatMessageTime(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.FRENCH).format(Date(millis))

    fun formatConversationTime(millis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        return when {
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) &&
                now.get(Calendar.YEAR) == target.get(Calendar.YEAR) ->
                SimpleDateFormat("HH:mm", Locale.FRENCH).format(Date(millis))
            now.get(Calendar.YEAR) == target.get(Calendar.YEAR) ->
                SimpleDateFormat("d MMM", Locale.FRENCH).format(Date(millis))
            else -> SimpleDateFormat("d MMM yyyy", Locale.FRENCH).format(Date(millis))
        }
    }

    fun formatLastSeen(millis: Long): String {
        if (millis == 0L) return "hors ligne"
        val diff = System.currentTimeMillis() - millis
        val minutes = diff / 60000
        return when {
            minutes < 1 -> "à l'instant"
            minutes < 60 -> "il y a ${minutes} min"
            minutes < 24 * 60 -> "il y a ${minutes / 60} h"
            else -> "vu le " + SimpleDateFormat("d MMM à HH:mm", Locale.FRENCH).format(Date(millis))
        }
    }
}
