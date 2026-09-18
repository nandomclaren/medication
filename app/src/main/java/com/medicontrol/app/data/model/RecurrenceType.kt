package com.medicontrol.app.data.model

/** Como os dias de um medicamento se repetem — define quais dias [com.medicontrol.app.data.entity.Medication.isScheduledOn] considera ativos. */
enum class RecurrenceType(val label: String) {
    DAILY("Todos os dias"),
    WEEKDAYS("Dias da semana"),
    EVERY_N_DAYS("A cada X dias"),
    EVERY_N_HOURS("A cada X horas")
}
