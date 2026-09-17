package com.medicontrol.app.data.model

/**
 * Formatos visuais disponíveis para representar um medicamento.
 * O desenho de cada um é feito via Canvas em [com.medicontrol.app.ui.components.MedicationIconView].
 */
enum class MedicationIcon(val label: String) {
    CAPSULE("Cápsula"),
    ROUND_PILL("Comprimido"),
    DROPS("Gotas"),
    INJECTION("Injeção")
}
