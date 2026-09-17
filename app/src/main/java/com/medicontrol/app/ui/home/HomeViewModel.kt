package com.medicontrol.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medicontrol.app.data.model.DoseUiModel
import com.medicontrol.app.data.repository.MedicationRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: MedicationRepository) : ViewModel() {

    /** Janela de dias exibida na barra superior: 15 dias no passado, 60 no futuro. */
    val visibleDays: List<LocalDate> = run {
        val today = LocalDate.now()
        (-15..60).map { today.plusDays(it.toLong()) }
    }

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _doses = MutableStateFlow<List<DoseUiModel>>(emptyList())
    val doses: StateFlow<List<DoseUiModel>> = _doses

    private val _missedDays = MutableStateFlow<Set<LocalDate>>(emptySet())
    val missedDays: StateFlow<Set<LocalDate>> = _missedDays

    init {
        refreshDoses()
        refreshMissedDays()
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        refreshDoses()
    }

    fun onDoseCheckedChange(dose: DoseUiModel, taken: Boolean) {
        viewModelScope.launch {
            repository.toggleDoseTaken(dose.medication, _selectedDate.value, dose.time, taken)
            refreshDoses()
            refreshMissedDays()
        }
    }

    fun refreshAll() {
        refreshDoses()
        refreshMissedDays()
    }

    private fun refreshDoses() {
        viewModelScope.launch {
            _doses.value = repository.getDosesForDate(_selectedDate.value)
        }
    }

    private fun refreshMissedDays() {
        viewModelScope.launch {
            _missedDays.value = repository.getMissedDays(visibleDays.first(), visibleDays.last())
        }
    }
}
