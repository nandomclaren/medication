package com.medicontrol.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medicontrol.app.data.model.MedicationColor
import com.medicontrol.app.data.model.MedicationIcon
import java.time.LocalDate
import java.time.LocalTime

/**
 * Um medicamento cadastrado pelo usuário.
 *
 * [endDate] nulo (junto de [isContinuous] = true) representa uso contínuo/indefinido:
 * o medicamento fica ativo em qualquer data >= [startDate], sem data de término.
 *
 * [times] é a lista de horários do dia em que a dose deve ser tomada, persistida
 * como texto "HH:mm,HH:mm" via [com.medicontrol.app.data.db.Converters].
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
    val active: Boolean = true
) {
    val color: MedicationColor get() = MedicationColor.fromHex(colorHex)

    /** Um medicamento está ativo em [date] se [date] estiver dentro do período de tratamento. */
    fun isActiveOn(date: LocalDate): Boolean {
        if (!active || date.isBefore(startDate)) return false
        if (isContinuous || endDate == null) return true
        return !date.isAfter(endDate)
    }

    /** true quando [date] é o último dia de tratamento (não se aplica a uso contínuo). */
    fun isLastDoseDay(date: LocalDate): Boolean =
        !isContinuous && endDate != null && date == endDate
}
