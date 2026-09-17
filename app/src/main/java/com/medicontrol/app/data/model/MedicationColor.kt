package com.medicontrol.app.data.model

import androidx.compose.ui.graphics.Color

/**
 * Paleta fixa de cores selecionáveis no cadastro do medicamento.
 * O valor persistido no banco é [hex], convertido de/para [Color] na UI.
 */
enum class MedicationColor(val hex: String) {
    BLUE("#1565C0"),
    RED("#C62828"),
    GREEN("#2E7D32"),
    YELLOW("#F9A825"),
    PURPLE("#6A1B9A"),
    ORANGE("#EF6C00");

    val composeColor: Color get() = Color(android.graphics.Color.parseColor(hex))

    companion object {
        fun fromHex(hex: String): MedicationColor =
            entries.firstOrNull { it.hex.equals(hex, ignoreCase = true) } ?: BLUE
    }
}
