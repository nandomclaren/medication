package com.medicontrol.app.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medicontrol.app.data.db.AppDatabase
import com.medicontrol.app.data.entity.DoseRecord
import com.medicontrol.app.data.model.DoseStatus
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Recebe os cliques dos botões "Tomei" / "Pular" direto na notificação. */
class DoseActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val dateStr = intent.getStringExtra(EXTRA_DATE) ?: return
        val timeStr = intent.getStringExtra(EXTRA_TIME) ?: return
        val action = intent.action
        if (medicationId == -1L || action == null) return

        val date = LocalDate.parse(dateStr)
        val time = LocalTime.parse(timeStr)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).doseRecordDao()
                val status = when (action) {
                    ACTION_MARK_TAKEN -> DoseStatus.TAKEN
                    ACTION_MARK_SKIPPED -> DoseStatus.SKIPPED
                    else -> null
                }
                if (status != null) {
                    dao.upsert(
                        DoseRecord(
                            medicationId = medicationId,
                            scheduledDate = date,
                            scheduledTime = time,
                            status = status,
                            actionAt = System.currentTimeMillis()
                        )
                    )
                }
                NotificationHelper.cancel(context, medicationId, time)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_TAKEN = "com.medicontrol.app.action.MARK_TAKEN"
        const val ACTION_MARK_SKIPPED = "com.medicontrol.app.action.MARK_SKIPPED"
        private const val EXTRA_MEDICATION_ID = "extra_medication_id"
        private const val EXTRA_DATE = "extra_date"
        private const val EXTRA_TIME = "extra_time"

        fun buildIntent(
            context: Context,
            medicationId: Long,
            date: LocalDate,
            time: LocalTime,
            action: String
        ): PendingIntent {
            val intent = Intent(context, DoseActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_MEDICATION_ID, medicationId)
                putExtra(EXTRA_DATE, date.toString())
                putExtra(EXTRA_TIME, time.toString())
            }
            // requestCode precisa ser único por (medicamento, horário, ação) para não
            // sobrescrever o PendingIntent do botão "Tomei" com o de "Pular".
            val requestCode = "$medicationId-$time-$action".hashCode()
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
