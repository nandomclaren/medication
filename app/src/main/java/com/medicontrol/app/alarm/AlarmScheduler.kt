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
 * Agenda/cancela os alarmes exatos do app usando [AlarmManager].
 *
 * Os alarmes são chaveados só pelo HORÁRIO (não por medicamento): se dois
 * remédios caem às 8h, existe UM alarme do sistema para as 8h, não dois. Quando
 * ele dispara, o [AlarmReceiver] consulta o banco e resolve quais medicamentos
 * estão de fato programados para aquele instante — é isso que permite agrupar
 * tudo numa única notificação.
 *
 * Como o AlarmManager não tem "alarme exato recorrente", cada disparo se
 * reagenda para o dia seguinte (ver [AlarmReceiver]) — [reconcile] só entra em
 * ação quando o CONJUNTO de horários muda (medicamento criado/editado/excluído),
 * pra ligar horários novos e desligar os que ninguém mais usa.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    /** Recalcula o conjunto de horários necessários a partir dos medicamentos ativos e ajusta os alarmes do sistema de acordo. */
    fun reconcile(activeMedications: List<Medication>, from: LocalDateTime = LocalDateTime.now()) {
        val needed = activeMedications.flatMap { it.scheduleTimes }.toSet()
        val previous = readScheduledTimes()

        (previous - needed).forEach { cancelTime(it) }
        needed.forEach { scheduleTime(it, from) }

        prefs.edit().putStringSet(KEY_SCHEDULED_TIMES, needed.map { it.toString() }.toSet()).apply()
    }

    /** Agenda a próxima ocorrência (hoje ou amanhã) de [time]. Idempotente — pode ser chamado de novo sem duplicar alarmes. */
    fun scheduleTime(time: LocalTime, from: LocalDateTime = LocalDateTime.now()) {
        if (!hasExactAlarmPermission()) {
            Log.w(TAG, "Permissão de alarme exato não concedida; horário $time não agendado.")
            return
        }
        var next = LocalDateTime.of(from.toLocalDate(), time)
        if (!next.isAfter(from)) next = next.plusDays(1)
        setAlarm(next, buildPendingIntent(time, isSnooze = false))
    }

    /** Agenda um disparo único (não recorrente) da soneca, [minutes] a partir de agora, para o mesmo [time] original. */
    fun scheduleSnooze(time: LocalTime, minutes: Int = 10) {
        if (!hasExactAlarmPermission()) return
        val trigger = LocalDateTime.now().plusMinutes(minutes.toLong())
        setAlarm(trigger, buildPendingIntent(time, isSnooze = true))
    }

    fun cancelTime(time: LocalTime) {
        val pendingIntent = buildPendingIntent(time, isSnooze = false)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun setAlarm(dateTime: LocalDateTime, pendingIntent: PendingIntent) {
        val triggerMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            // setExactAndAllowWhileIdle dispara mesmo em Doze Mode / App Standby.
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } catch (e: SecurityException) {
            Log.e(TAG, "Sem permissão para agendar alarme exato", e)
        }
    }

    private fun hasExactAlarmPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun readScheduledTimes(): Set<LocalTime> =
        (prefs.getStringSet(KEY_SCHEDULED_TIMES, emptySet()) ?: emptySet())
            .mapNotNull { runCatching { LocalTime.parse(it) }.getOrNull() }
            .toSet()

    private fun buildPendingIntent(time: LocalTime, isSnooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DOSE_ALARM
            putExtra(EXTRA_TIME, time.toString())
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        val requestCode = if (isSnooze) requestCodeForSnooze(time) else requestCodeFor(time)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        private const val PREFS_NAME = "alarm_scheduler"
        private const val KEY_SCHEDULED_TIMES = "scheduled_times"

        const val ACTION_DOSE_ALARM = "com.medicontrol.app.action.DOSE_ALARM"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"

        fun requestCodeFor(time: LocalTime): Int = "slot-$time".hashCode()
        fun requestCodeForSnooze(time: LocalTime): Int = "snooze-$time".hashCode()
    }
}
