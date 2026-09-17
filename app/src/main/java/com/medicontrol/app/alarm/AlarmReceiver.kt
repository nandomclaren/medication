package com.medicontrol.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medicontrol.app.data.db.AppDatabase
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Disparado pelo [AlarmManager] no horário exato de uma dose.
 *
 * Mostra a notificação e, em seguida, reagenda o MESMO horário para o dia
 * seguinte — é assim que o app continua lembrando o usuário todos os dias
 * sem precisar manter um serviço rodando em background.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_DOSE_ALARM) return

        val medicationId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        val name = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICATION_NAME) ?: return
        val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICATION_DOSAGE) ?: ""
        val timeStr = intent.getStringExtra(AlarmScheduler.EXTRA_TIME) ?: return
        val time = LocalTime.parse(timeStr)
        if (medicationId == -1L) return

        // onReceive precisa retornar rápido; goAsync() nos dá um tempo extra
        // para terminar a consulta ao Room + reagendamento em background.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationHelper.createChannel(context)
                NotificationHelper.showDoseReminder(
                    context = context,
                    medicationId = medicationId,
                    medicationName = name,
                    dosage = dosage,
                    date = LocalDate.now(),
                    time = time
                )

                val database = AppDatabase.getInstance(context)
                val medication = database.medicationDao().getById(medicationId)
                if (medication != null && medication.active) {
                    AlarmScheduler(context).scheduleNext(medication, time)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
