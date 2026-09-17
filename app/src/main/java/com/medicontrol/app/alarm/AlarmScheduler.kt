package com.medicontrol.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.medicontrol.app.data.entity.Medication
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Agenda/cancela os alarmes exatos de um medicamento usando [AlarmManager].
 *
 * Estratégia: em vez de agendar TODAS as doses futuras (inviável para uso
 * contínuo/indefinido), agendamos apenas a PRÓXIMA ocorrência de cada horário.
 * Quando o [AlarmReceiver] dispara, ele reagenda o mesmo horário para o dia
 * seguinte (ver [scheduleNext]) — assim o alarme "se perpetua" sozinho,
 * respeitando a data de término quando houver.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Cancela e reagenda todos os horários do medicamento a partir de agora. */
    fun scheduleAll(medication: Medication, from: LocalDateTime = LocalDateTime.now()) {
        for (time in medication.times) {
            scheduleNext(medication, time, from)
        }
    }

    fun cancelAll(medication: Medication) {
        for (time in medication.times) cancel(medication.id, time)
    }

    /**
     * Agenda a próxima ocorrência de [time] para [medication] a partir de [from].
     * Não faz nada se o medicamento já tiver terminado o tratamento.
     */
    fun scheduleNext(medication: Medication, time: LocalTime, from: LocalDateTime = LocalDateTime.now()) {
        val nextTrigger = nextTriggerDateTime(medication, time, from) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Permissão de alarme exato não concedida; alarme não agendado.")
            return
        }

        val pendingIntent = buildPendingIntent(medication.id, medication.name, medication.dosage, time)
        val triggerMillis = nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            // setExactAndAllowWhileIdle dispara mesmo em Doze Mode / App Standby.
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } catch (e: SecurityException) {
            Log.e(TAG, "Sem permissão para agendar alarme exato", e)
        }
    }

    fun cancel(medicationId: Long, time: LocalTime) {
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_DOSE_ALARM }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(medicationId, time),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildPendingIntent(
        medicationId: Long,
        name: String,
        dosage: String,
        time: LocalTime
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DOSE_ALARM
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_MEDICATION_NAME, name)
            putExtra(EXTRA_MEDICATION_DOSAGE, dosage)
            putExtra(EXTRA_TIME, time.toString())
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(medicationId, time),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Próximo instante (hoje ou dias seguintes) em que [time] deve disparar, ou null se o tratamento já terminou. */
    private fun nextTriggerDateTime(
        medication: Medication,
        time: LocalTime,
        from: LocalDateTime
    ): LocalDateTime? {
        var candidate = LocalDateTime.of(from.toLocalDate(), time)
        if (!candidate.isAfter(from)) {
            candidate = candidate.plusDays(1)
        }
        while (candidate.toLocalDate().isBefore(medication.startDate)) {
            candidate = candidate.plusDays(1)
        }
        val endDate = medication.endDate
        if (!medication.isContinuous && endDate != null && candidate.toLocalDate().isAfter(endDate)) {
            return null
        }
        return candidate
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        const val ACTION_DOSE_ALARM = "com.medicontrol.app.action.DOSE_ALARM"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_MEDICATION_DOSAGE = "extra_medication_dosage"
        const val EXTRA_TIME = "extra_time"

        fun requestCodeFor(medicationId: Long, time: LocalTime): Int =
            "$medicationId-$time".hashCode()
    }
}
