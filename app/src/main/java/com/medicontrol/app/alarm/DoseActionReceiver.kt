package com.medicontrol.app.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medicontrol.app.MediControlApp
import com.medicontrol.app.data.model.DoseStatus
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Recebe os cliques dos botões de ação direto na notificação: marcar/pular (um ou vários remédios) e soneca. */
class DoseActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val timeStr = intent.getStringExtra(EXTRA_TIME) ?: return
        val time = LocalTime.parse(timeStr)
        val repository = (context.applicationContext as MediControlApp).repository

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_MARK_TAKEN, ACTION_MARK_SKIPPED -> {
                        val medicationIds = intent.getLongArrayExtra(EXTRA_MEDICATION_IDS) ?: return@launch
                        val dateStr = intent.getStringExtra(EXTRA_DATE) ?: return@launch
                        val date = LocalDate.parse(dateStr)
                        val status = if (action == ACTION_MARK_TAKEN) DoseStatus.TAKEN else DoseStatus.SKIPPED
                        repository.setGroupDoseStatus(medicationIds.toList(), date, time, status)
                        NotificationHelper.cancel(context, time)
                    }
                    ACTION_SNOOZE -> {
                        NotificationHelper.cancel(context, time)
                        AlarmScheduler(context).scheduleSnooze(time)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_TAKEN = "com.medicontrol.app.action.MARK_TAKEN"
        const val ACTION_MARK_SKIPPED = "com.medicontrol.app.action.MARK_SKIPPED"
        const val ACTION_SNOOZE = "com.medicontrol.app.action.SNOOZE"
        private const val EXTRA_MEDICATION_IDS = "extra_medication_ids"
        private const val EXTRA_DATE = "extra_date"
        private const val EXTRA_TIME = "extra_time"

        fun buildIntent(
            context: Context,
            medicationIds: List<Long>,
            date: LocalDate,
            time: LocalTime,
            action: String
        ): PendingIntent {
            val intent = Intent(context, DoseActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_MEDICATION_IDS, medicationIds.toLongArray())
                putExtra(EXTRA_DATE, date.toString())
                putExtra(EXTRA_TIME, time.toString())
            }
            // requestCode único por (horário, ação) — evita que o PendingIntent do
            // botão "Tomei" sobrescreva o de "Pular" (ou o da soneca) no mesmo slot.
            val requestCode = "$time-$action".hashCode()
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun buildSnoozeIntent(context: Context, time: LocalTime): PendingIntent {
            val intent = Intent(context, DoseActionReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_TIME, time.toString())
            }
            val requestCode = "$time-$ACTION_SNOOZE".hashCode()
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
