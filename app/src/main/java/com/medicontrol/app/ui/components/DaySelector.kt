package com.medicontrol.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medicontrol.app.ui.theme.AdherenceMissedColor
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Barra horizontal com os dias do período visível. O dia selecionado fica
 * destacado com a cor primária; dias passados com alguma dose não marcada
 * como tomada ficam com destaque vermelho (indicador de adesão).
 */
@Composable
fun DaySelector(
    days: List<LocalDate>,
    selectedDate: LocalDate,
    missedDays: Set<LocalDate>,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val selectedIndex = days.indexOf(selectedDate).coerceAtLeast(0)

    LaunchedEffect(selectedDate) {
        listState.animateScrollToItem((selectedIndex - 2).coerceAtLeast(0))
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { day ->
            DayChip(
                day = day,
                isSelected = day == selectedDate,
                isMissed = day in missedDays,
                onClick = { onDaySelected(day) }
            )
        }
    }
}

@Composable
private fun DayChip(day: LocalDate, isSelected: Boolean, isMissed: Boolean, onClick: () -> Unit) {
    val weekDayLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))
        .replaceFirstChar { it.uppercase() }

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isMissed -> AdherenceMissedColor
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (isMissed && !isSelected) AdherenceMissedColor else backgroundColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(width = 52.dp, height = 68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(width = if (isMissed && !isSelected) 2.dp else 0.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(top = 10.dp)
    ) {
        Text(
            text = weekDayLabel,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isMissed) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = day.dayOfMonth.toString(),
            color = contentColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isMissed || isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
