package com.medicontrol.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.medicontrol.app.MainActivity
import com.medicontrol.app.R
import com.medicontrol.app.data.entity.Medication
import com.medicontrol.app.util.toDisplayString
import java.time.LocalDate
import java.time.LocalTime

object NotificationHelper {

    const val CHANNEL_ID = "dose_reminders"
    private const val SNOOZE_MINUTES = 10

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

    fun notificationIdFor(time: LocalTime): Int = AlarmScheduler.requestCodeFor(time)

    /**
     * Mostra UMA notificação para todos os [medications] agendados em [time].
     * Com um único remédio, os botões agem só sobre ele ("Tomei"/"Pular").
     * Com dois ou mais, viram "Marcar todos"/"Pular todos" e a notificação
     * lista cada remédio (estilo caixa de entrada) — é a mesma ideia do
     * agrupamento por horário usada no widget da tela inicial.
     */
    fun showDoseReminder(
        context: Context,
        medications: List<Medication>,
        date: LocalDate,
        time: LocalTime
    ) {
        if (medications.isEmpty()) return

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationIdFor(time),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val medicationIds = medications.map { it.id }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        if (medications.size == 1) {
            val medication = medications.first()
            builder
                .setContentTitle(medication.name)
                .setContentText("Hora de tomar: ${medication.dosage} · ${time.toDisplayString()}")
                .addAction(
                    android.R.drawable.checkbox_on_background,
                    context.getString(R.string.notification_action_taken),
                    DoseActionReceiver.buildIntent(context, medicationIds, date, time, DoseActionReceiver.ACTION_MARK_TAKEN)
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.notification_action_skip),
                    DoseActionReceiver.buildIntent(context, medicationIds, date, time, DoseActionReceiver.ACTION_MARK_SKIPPED)
                )
        } else {
            val style = NotificationCompat.InboxStyle()
            medications.forEach { style.addLine("${it.name} · ${it.dosage}") }

            builder
                .setContentTitle("${medications.size} remédios às ${time.toDisplayString()}")
                .setContentText(medications.joinToString(", ") { it.name })
                .setStyle(style)
                .addAction(
                    android.R.drawable.checkbox_on_background,
                    context.getString(R.string.notification_action_taken_all),
                    DoseActionReceiver.buildIntent(context, medicationIds, date, time, DoseActionReceiver.ACTION_MARK_TAKEN)
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.notification_action_skip_all),
                    DoseActionReceiver.buildIntent(context, medicationIds, date, time, DoseActionReceiver.ACTION_MARK_SKIPPED)
                )
        }

        builder.addAction(
            android.R.drawable.ic_menu_recent_history,
            context.getString(R.string.notification_action_snooze, SNOOZE_MINUTES),
            DoseActionReceiver.buildSnoozeIntent(context, time)
        )

        NotificationManagerCompat.from(context).notify(notificationIdFor(time), builder.build())
    }

    fun cancel(context: Context, time: LocalTime) {
        NotificationManagerCompat.from(context).cancel(notificationIdFor(time))
    }
}
