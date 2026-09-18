package com.medicontrol.app.ui.addedit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medicontrol.app.data.entity.Medication
import com.medicontrol.app.data.model.MedicationColor
import com.medicontrol.app.data.model.MedicationIcon
import com.medicontrol.app.data.model.RecurrenceType
import com.medicontrol.app.data.repository.MedicationRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

class AddEditMedicationViewModel(
    private val repository: MedicationRepository,
    private val medicationId: Long?
) : ViewModel() {

    val isEditing: Boolean = medicationId != null

    var name by mutableStateOf("")
    var dosage by mutableStateOf("")
    var startDate by mutableStateOf(LocalDate.now())
    var endDate by mutableStateOf<LocalDate?>(null)
    var isContinuous by mutableStateOf(true)
    var times by mutableStateOf(listOf<LocalTime>())
        private set
    var icon by mutableStateOf(MedicationIcon.ROUND_PILL)
    var color by mutableStateOf(MedicationColor.BLUE)

    var recurrenceType by mutableStateOf(RecurrenceType.DAILY)
    var recurrenceIntervalDays by mutableStateOf(1)
    var recurrenceWeekdays by mutableStateOf(setOf<DayOfWeek>())
        private set
    var recurrenceIntervalHours by mutableStateOf(8)

    var stockTrackingEnabled by mutableStateOf(false)
    var stockQuantity by mutableStateOf(0)
    var stockThreshold by mutableStateOf(5)

    var isLoading by mutableStateOf(medicationId != null)
        private set
    var isSaved by mutableStateOf(false)
        private set

    val isValid: Boolean
        get() {
            if (name.isBlank() || dosage.isBlank() || times.isEmpty()) return false
            if (!isContinuous && endDate == null) return false
            return when (recurrenceType) {
                RecurrenceType.WEEKDAYS -> recurrenceWeekdays.isNotEmpty()
                RecurrenceType.EVERY_N_DAYS -> recurrenceIntervalDays >= 1
                RecurrenceType.EVERY_N_HOURS -> recurrenceIntervalHours in 1..23
                RecurrenceType.DAILY -> true
            }
        }

    init {
        medicationId?.let { id ->
            viewModelScope.launch {
                repository.getMedication(id)?.let { medication ->
                    name = medication.name
                    dosage = medication.dosage
                    startDate = medication.startDate
                    endDate = medication.endDate
                    isContinuous = medication.isContinuous
                    times = medication.times
                    icon = medication.icon
                    color = medication.color
                    recurrenceType = medication.recurrenceType
                    recurrenceIntervalDays = medication.recurrenceIntervalDays
                    recurrenceWeekdays = medication.recurrenceWeekdays
                    recurrenceIntervalHours = medication.recurrenceIntervalHours
                    stockTrackingEnabled = medication.stockQuantity != null
                    stockQuantity = medication.stockQuantity ?: 0
                    stockThreshold = medication.stockThreshold
                }
                isLoading = false
            }
        }
    }

    /** Usado pelos modos com múltiplos horários por dia (diário / dias da semana / a cada X dias). */
    fun addTime(time: LocalTime) {
        if (time !in times) times = (times + time).sorted()
    }

    fun removeTime(time: LocalTime) {
        times = times - time
    }

    /** Usado pelo modo "a cada X horas": só existe UM horário-âncora, que este método substitui. */
    fun setAnchorTime(time: LocalTime) {
        times = listOf(time)
    }

    fun toggleWeekday(day: DayOfWeek) {
        recurrenceWeekdays = if (day in recurrenceWeekdays) recurrenceWeekdays - day else recurrenceWeekdays + day
    }

    fun onRecurrenceTypeChange(type: RecurrenceType) {
        recurrenceType = type
        if (type == RecurrenceType.EVERY_N_HOURS && times.size > 1) {
            times = times.take(1)
        }
    }

    fun save() {
        if (!isValid) return
        viewModelScope.launch {
            repository.saveMedication(
                Medication(
                    id = medicationId ?: 0L,
                    name = name.trim(),
                    dosage = dosage.trim(),
                    startDate = startDate,
                    endDate = if (isContinuous) null else endDate,
                    isContinuous = isContinuous,
                    times = times,
                    icon = icon,
                    colorHex = color.hex,
                    recurrenceType = recurrenceType,
                    recurrenceIntervalDays = recurrenceIntervalDays,
                    recurrenceWeekdays = recurrenceWeekdays,
                    recurrenceIntervalHours = recurrenceIntervalHours,
                    stockQuantity = if (stockTrackingEnabled) stockQuantity else null,
                    stockThreshold = stockThreshold
                )
            )
            isSaved = true
        }
    }

    fun delete() {
        val id = medicationId ?: return
        viewModelScope.launch {
            repository.getMedication(id)?.let { repository.deleteMedication(it) }
            isSaved = true
        }
    }
}
