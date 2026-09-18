package com.medicontrol.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medicontrol.app.data.db.AppDatabase
import com.medicontrol.app.data.model.DoseStatus
import com.medicontrol.app.widget.WidgetRefresher
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Disparado pelo [AlarmManager] no horário exato de um "slot" de doses.
 *
 * Não sabe de antemão quais medicamentos estão nesse horário — consulta o
 * Room na hora (isso é o que permite agrupar vários remédios do mesmo
 * horário numa única notificação, mesmo que tenham sido cadastrados/editados
 * depois do alarme ter sido agendado). Doses já marcadas (tomada ou pulada)
 * não entram na notificação. Ao final, reagenda o mesmo horário para o dia
 * seguinte — exceto quando o disparo é de uma soneca, que é único.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_DOSE_ALARM) return

        val timeStr = intent.getStringExtra(AlarmScheduler.EXTRA_TIME) ?: return
        val time = LocalTime.parse(timeStr)
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)

        // onReceive precisa retornar rápido; goAsync() nos dá um tempo extra
        // para terminar a consulta ao Room + reagendamento em background.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now()
                val database = AppDatabase.getInstance(context)

                val dueMedications = database.medicationDao().getAllActive()
                    .filter { time in it.doseTimesOn(today) }

                if (dueMedications.isNotEmpty()) {
                    val recordsAtSlot = database.doseRecordDao().getBetween(today, today)
                        .filter { it.scheduledTime == time }
                        .associateBy { it.medicationId }

                    val pendingMedications = dueMedications.filter { medication ->
                        recordsAtSlot[medication.id]?.status !in
                            setOf(DoseStatus.TAKEN, DoseStatus.SKIPPED)
                    }

                    if (pendingMedications.isNotEmpty()) {
                        NotificationHelper.createChannel(context)
                        NotificationHelper.showDoseReminder(context, pendingMedications, today, time)
                    }
                }

                if (!isSnooze) {
                    AlarmScheduler(context).scheduleTime(time)
                }

                // Cada disparo também serve de "tick" pro widget — cobre a
                // virada do dia mesmo se o usuário não abrir o app nem
                // interagir com nenhuma dose.
                WidgetRefresher.refresh(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
