package ru.finney.pet.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.finney.pet.MainActivity
import ru.finney.pet.R

/** Канал и показ уведомлений. Разрешения нет или уведомления выключены — молча ничего. */
object Notifier {

    private const val CHANNEL = "pet"

    const val ID_REMINDER = 1
    const val ID_WOKE = 2

    /** Канал «Питомец». Создать можно сколько угодно раз — система оставит один. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Питомец", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Питомец проснулся или зовёт в гости"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Показать уведомление. Нажатие открывает приложение — на главном, если питомец уже есть. */
    fun post(context: Context, id: Int, text: NotificationText) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text.title)
            .setContentText(text.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Разрешение отозвали между проверкой и показом — игра от этого не зависит.
        }
    }
}
