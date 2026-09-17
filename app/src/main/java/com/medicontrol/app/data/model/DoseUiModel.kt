package com.medicontrol.app.data.model

import com.medicontrol.app.data.entity.Medication
import java.time.LocalTime

/** Representa uma dose (medicamento + horário) já resolvida para exibição na tela do dia. */
data class DoseUiModel(
    val medication: Medication,
    val time: LocalTime,
    val status: DoseStatus,
    val isLastDoseDay: Boolean
)
