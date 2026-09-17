package com.medicontrol.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.medicontrol.app.data.model.MedicationColor
import com.medicontrol.app.data.model.MedicationIcon

/** Seletor visual dos 4 formatos de medicação disponíveis. */
@Composable
fun IconPickerRow(
    selected: MedicationIcon,
    color: MedicationColor,
    onSelected: (MedicationIcon) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MedicationIcon.entries.forEach { icon ->
            val isSelected = icon == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .selectable(selected = isSelected, onClick = { onSelected(icon) })
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .padding(8.dp)
            ) {
                MedicationIconView(icon = icon, color = color.composeColor, iconSize = 40.dp)
                Text(icon.label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Seletor de cor: círculos coloridos com destaque no selecionado. */
@Composable
fun ColorPickerRow(
    selected: MedicationColor,
    onSelected: (MedicationColor) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MedicationColor.entries.forEach { medicationColor ->
            val isSelected = medicationColor == selected
            Box(
                modifier = Modifier
                    .size(if (isSelected) 44.dp else 36.dp)
                    .clip(CircleShape)
                    .background(medicationColor.composeColor)
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    )
                    .selectable(selected = isSelected, onClick = { onSelected(medicationColor) })
            )
        }
    }
}
