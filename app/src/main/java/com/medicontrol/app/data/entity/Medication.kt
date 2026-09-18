package com.medicontrol.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medicontrol.app.data.model.MedicationColor
import com.medicontrol.app.data.model.MedicationIcon
import com.medicontrol.app.data.model.RecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Um medicamento cadastrado pelo usuário.
 *
 * [endDate] nulo (junto de [isContinuous] = true) representa uso contínuo/indefinido:
 * o medicamento fica ativo em qualquer data >= [startDate], sem data de término.
 *
 * [times] é a lista de horários do dia em que a dose deve ser tomada, persistida
 * como texto "HH:mm;HH:mm" via [com.medicontrol.app.data.db.Converters]. Para
 * [RecurrenceType.EVERY_N_HOURS], [times] guarda só o horário-âncora (a primeira
 * dose do dia) — os demais são derivados em [scheduleTimes].
 *
 * [stockQuantity] nulo significa "não controlar estoque" para este medicamento.
 */
@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val dosage: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isContinuous: Boolean,
    val times: List<LocalTime>,
    val icon: MedicationIcon,
    val colorHex: String,
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val recurrenceIntervalDays: Int = 1,
    val recurrenceWeekdays: Set<DayOfWeek> = emptySet(),
    val recurrenceIntervalHours: Int = 8,
    val stockQuantity: Int? = null,
    val stockThreshold: Int = 5,
    val active: Boolean = true
) {
    val color: MedicationColor get() = MedicationColor.fromHex(colorHex)

    /** Um medicamento está ativo em [date] se estiver dentro do período de tratamento E for um dia previsto pela cadência. */
    fun isActiveOn(date: LocalDate): Boolean {
        if (!active || date.isBefore(startDate)) return false
        if (!isContinuous && endDate != null && date.isAfter(endDate)) return false
        return isScheduledOn(date)
    }

    /** Padrão de dias da cadência, ignorando o período de tratamento (ver [isActiveOn]). */
    fun isScheduledOn(date: LocalDate): Boolean = when (recurrenceType) {
        RecurrenceType.DAILY, RecurrenceType.EVERY_N_HOURS -> true
        RecurrenceType.WEEKDAYS -> date.dayOfWeek in recurrenceWeekdays
        RecurrenceType.EVERY_N_DAYS -> {
            val interval = recurrenceIntervalDays.coerceAtLeast(1)
            ChronoUnit.DAYS.between(startDate, date) % interval == 0L
        }
    }

    /**
     * Horários do dia derivados da cadência. Para [RecurrenceType.EVERY_N_HOURS],
     * gera a lista a partir do horário-âncora ([times].first()) repetindo a cada
     * [recurrenceIntervalHours] horas até completar 24h. Para os demais tipos,
     * é simplesmente [times].
     */
    val scheduleTimes: List<LocalTime>
        get() {
            if (recurrenceType != RecurrenceType.EVERY_N_HOURS) return times
            val anchor = times.firstOrNull() ?: return emptyList()
            val intervalMinutes = recurrenceIntervalHours.coerceAtLeast(1) * 60
            val anchorMinutes = anchor.hour * 60 + anchor.minute
            return generateSequence(anchorMinutes) { it + intervalMinutes }
                .takeWhile { it < anchorMinutes + 24 * 60 }
                .map { LocalTime.of((it / 60) % 24, it % 60) }
                .toList()
        }

    /** Horários efetivamente agendados em [date] — vazio se o medicamento não estiver ativo nesse dia. */
    fun doseTimesOn(date: LocalDate): List<LocalTime> = if (isActiveOn(date)) scheduleTimes else emptyList()

    /** true quando [date] é o último dia de tratamento (não se aplica a uso contínuo). */
    fun isLastDoseDay(date: LocalDate): Boolean =
        !isContinuous && endDate != null && date == endDate

    val isLowStock: Boolean
        get() = stockQuantity != null && stockQuantity <= stockThreshold
}
