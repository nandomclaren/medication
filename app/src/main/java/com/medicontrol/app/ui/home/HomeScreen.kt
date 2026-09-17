package com.medicontrol.app.ui.home

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.medicontrol.app.MediControlApp
import com.medicontrol.app.data.model.DoseStatus
import com.medicontrol.app.data.model.DoseUiModel
import com.medicontrol.app.ui.components.DaySelector
import com.medicontrol.app.ui.components.MedicationIconView
import com.medicontrol.app.util.monthLabel
import com.medicontrol.app.util.toDisplayString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddMedication: () -> Unit,
    onEditMedication: (Long) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MediControlApp
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(app.repository) } }
    )

    val selectedDate by viewModel.selectedDate.collectAsState()
    val doses by viewModel.doses.collectAsState()
    val missedDays by viewModel.missedDays.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshAll() }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(selectedDate.monthLabel()) })
                DaySelector(
                    days = viewModel.visibleDays,
                    selectedDate = selectedDate,
                    missedDays = missedDays,
                    onDaySelected = viewModel::selectDate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                ExactAlarmPermissionBanner(context)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMedication) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar medicamento")
            }
        }
    ) { padding ->
        if (doses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Nenhum medicamento programado para este dia.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(doses, key = { "${it.medication.id}-${it.time}" }) { dose ->
                    DoseCard(
                        dose = dose,
                        onToggle = { taken -> viewModel.onDoseCheckedChange(dose, taken) },
                        onClick = { onEditMedication(dose.medication.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DoseCard(dose: DoseUiModel, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    val taken = dose.status == DoseStatus.TAKEN
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MedicationIconView(icon = dose.medication.icon, color = dose.medication.color.composeColor)

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = dose.medication.name,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (taken) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (dose.isLastDoseDay) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Última dose!",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${dose.medication.dosage} · ${dose.time.toDisplayString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Checkbox(checked = taken, onCheckedChange = onToggle)
        }
    }
}

/**
 * Em Android 12+ o usuário precisa autorizar alarmes exatos manualmente nas
 * configurações do sistema. Sem essa permissão, os lembretes não disparam no
 * horário certo — por isso exibimos um aviso até que ela seja concedida.
 */
@Composable
private fun ExactAlarmPermissionBanner(context: Context) {
    var granted by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    LaunchedEffect(Unit) { granted = canScheduleExactAlarms(context) }

    if (!granted) {
        Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickableOpenAlarmSettings(context),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    "Permita alarmes exatos para que os lembretes toquem na hora certa. Toque para ajustar.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    return alarmManager?.canScheduleExactAlarms() ?: true
}

private fun Modifier.clickableOpenAlarmSettings(context: Context): Modifier =
    this.clickable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }
