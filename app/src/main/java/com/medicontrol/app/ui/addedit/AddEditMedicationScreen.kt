package com.medicontrol.app.ui.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.medicontrol.app.MediControlApp
import com.medicontrol.app.data.model.RecurrenceType
import com.medicontrol.app.ui.components.ColorPickerRow
import com.medicontrol.app.ui.components.IconPickerRow
import com.medicontrol.app.util.toDisplayString
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationScreen(
    medicationId: Long?,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MediControlApp
    val viewModel: AddEditMedicationViewModel = viewModel(
        factory = viewModelFactory { initializer { AddEditMedicationViewModel(app.repository, medicationId) } }
    )

    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) onDone()
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAnchorTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Editar medicamento" else "Novo medicamento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (viewModel.isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text("Nome do remédio") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.dosage,
                onValueChange = { viewModel.dosage = it },
                label = { Text("Dosagem (ex: 50mg, 1 comprimido)") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            Text("Ícone", style = MaterialTheme.typography.titleMedium)
            IconPickerRow(
                selected = viewModel.icon,
                color = viewModel.color,
                onSelected = { viewModel.icon = it }
            )

            Text("Cor", style = MaterialTheme.typography.titleMedium)
            ColorPickerRow(
                selected = viewModel.color,
                onSelected = { viewModel.color = it }
            )

            HorizontalDivider()

            Text("Período de tratamento", style = MaterialTheme.typography.titleMedium)

            OutlinedButton(onClick = { showStartDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Início: ${viewModel.startDate.toDisplayString()}")
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = viewModel.isContinuous, onCheckedChange = { viewModel.isContinuous = it })
                Text("Uso contínuo / Indefinido")
            }

            if (!viewModel.isContinuous) {
                OutlinedButton(onClick = { showEndDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        viewModel.endDate?.let { "Término: ${it.toDisplayString()}" } ?: "Selecionar data de término"
                    )
                }
            }

            HorizontalDivider()

            Text("Cadência", style = MaterialTheme.typography.titleMedium)
            RecurrenceTypeRow(
                selected = viewModel.recurrenceType,
                onSelected = viewModel::onRecurrenceTypeChange
            )

            when (viewModel.recurrenceType) {
                RecurrenceType.WEEKDAYS -> WeekdayPickerRow(
                    selected = viewModel.recurrenceWeekdays,
                    onToggle = viewModel::toggleWeekday
                )
                RecurrenceType.EVERY_N_DAYS -> NumberField(
                    label = "A cada quantos dias",
                    value = viewModel.recurrenceIntervalDays,
                    onValueChange = { viewModel.recurrenceIntervalDays = it.coerceAtLeast(1) }
                )
                RecurrenceType.EVERY_N_HOURS -> NumberField(
                    label = "A cada quantas horas",
                    value = viewModel.recurrenceIntervalHours,
                    onValueChange = { viewModel.recurrenceIntervalHours = it.coerceIn(1, 23) }
                )
                RecurrenceType.DAILY -> Unit
            }

            HorizontalDivider()

            if (viewModel.recurrenceType == RecurrenceType.EVERY_N_HOURS) {
                Text("Horário inicial", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { showAnchorTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(viewModel.times.firstOrNull()?.toDisplayString() ?: "Selecionar horário")
                }
            } else {
                Text("Horários do dia", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.times) { time ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.removeTime(time) },
                            label = { Text(time.toDisplayString()) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remover horário",
                                    modifier = Modifier.size(InputChipDefaults.IconSize)
                                )
                            }
                        )
                    }
                }
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text("+ Adicionar horário")
                }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = viewModel.stockTrackingEnabled, onCheckedChange = { viewModel.stockTrackingEnabled = it })
                Text("Controlar estoque")
            }
            if (viewModel.stockTrackingEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        label = "Quantidade em estoque",
                        value = viewModel.stockQuantity,
                        onValueChange = { viewModel.stockQuantity = it.coerceAtLeast(0) },
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        label = "Avisar quando restar",
                        value = viewModel.stockThreshold,
                        onValueChange = { viewModel.stockThreshold = it.coerceAtLeast(0) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Button(
                onClick = viewModel::save,
                enabled = viewModel.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar")
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerModal(
            initialDate = viewModel.startDate,
            onDismiss = { showStartDatePicker = false },
            onConfirm = { viewModel.startDate = it; showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        DatePickerModal(
            initialDate = viewModel.endDate ?: viewModel.startDate,
            onDismiss = { showEndDatePicker = false },
            onConfirm = { viewModel.endDate = it; showEndDatePicker = false }
        )
    }

    if (showTimePicker) {
        TimePickerModal(
            onDismiss = { showTimePicker = false },
            onConfirm = { viewModel.addTime(it); showTimePicker = false }
        )
    }

    if (showAnchorTimePicker) {
        TimePickerModal(
            onDismiss = { showAnchorTimePicker = false },
            onConfirm = { viewModel.setAnchorTime(it); showAnchorTimePicker = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir medicamento?") },
            text = { Text("Isso também apaga o histórico de doses e cancela os lembretes agendados.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.delete() }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun RecurrenceTypeRow(selected: RecurrenceType, onSelected: (RecurrenceType) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(RecurrenceType.entries) { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelected(type) },
                label = { Text(type.label) }
            )
        }
    }
}

@Composable
private fun WeekdayPickerRow(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    val locale = Locale("pt", "BR")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(DayOfWeek.values().toList()) { day ->
            val label = day.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.uppercase() }
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.width(180.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(initialDate: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    onConfirm(date)
                } else onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerModal(onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar horário") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
