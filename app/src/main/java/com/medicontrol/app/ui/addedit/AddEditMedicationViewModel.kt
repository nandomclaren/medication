package com.medicontrol.app.ui.addedit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medicontrol.app.data.entity.Medication
import com.medicontrol.app.data.model.MedicationColor
import com.medicontrol.app.data.model.MedicationIcon
import com.medicontrol.app.data.repository.MedicationRepository
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
    var isLoading by mutableStateOf(medicationId != null)
        private set
    var isSaved by mutableStateOf(false)
        private set

    val isValid: Boolean
        get() = name.isNotBlank() &&
            dosage.isNotBlank() &&
            times.isNotEmpty() &&
            (isContinuous || endDate != null)

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
                }
                isLoading = false
            }
        }
    }

    fun addTime(time: LocalTime) {
        if (time !in times) times = (times + time).sorted()
    }

    fun removeTime(time: LocalTime) {
        times = times - time
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
                    colorHex = color.hex
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
