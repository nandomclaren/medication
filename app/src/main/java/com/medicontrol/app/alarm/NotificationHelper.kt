package com.medicontrol.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.medicontrol.app.MainActivity
import com.medicontrol.app.R
import java.time.LocalDate
import java.time.LocalTime

object NotificationHelper {

    const val CHANNEL_ID = "dose_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            setBypassDnd(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun notificationIdFor(medicationId: Long, time: LocalTime): Int =
        AlarmScheduler.requestCodeFor(medicationId, time)

    fun showDoseReminder(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        date: LocalDate,
        time: LocalTime
    ) {
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationIdFor(medicationId, time),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenIntent = DoseActionReceiver.buildIntent(
            context, medicationId, date, time, DoseActionReceiver.ACTION_MARK_TAKEN
        )
        val skipIntent = DoseActionReceiver.buildIntent(
            context, medicationId, date, time, DoseActionReceiver.ACTION_MARK_SKIPPED
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setContentTitle(medicationName)
            .setContentText("Hora de tomar: $dosage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                context.getString(R.string.notification_action_taken),
                takenIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notification_action_skip),
                skipIntent
            )
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationIdFor(medicationId, time), notification)
    }

    fun cancel(context: Context, medicationId: Long, time: LocalTime) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(notificationIdFor(medicationId, time))
    }
}
