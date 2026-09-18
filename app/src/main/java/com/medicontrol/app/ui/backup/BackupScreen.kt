package com.medicontrol.app.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.medicontrol.app.MediControlApp
import com.medicontrol.app.widget.WidgetRefresher
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MediControlApp
    val scope = rememberCoroutineScope()

    var isWorking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorking = true
        scope.launch {
            runCatching { app.backupManager.export(uri) }
                .onSuccess { statusMessage = "Backup salvo com sucesso." }
                .onFailure { statusMessage = "Não foi possível salvar o backup: ${it.message}" }
            isWorking = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorking = true
        scope.launch {
            runCatching { app.backupManager.import(uri) }
                .onSuccess { count ->
                    app.repository.reconcileAlarms()
                    WidgetRefresher.refresh(app)
                    statusMessage = "$count medicamentos restaurados."
                }
                .onFailure { statusMessage = "Não foi possível restaurar o backup: ${it.message}" }
            isWorking = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Exporte seus medicamentos e o histórico de doses para um arquivo, ou " +
                    "restaure a partir de um backup salvo antes. Ao escolher onde salvar ou " +
                    "de onde restaurar, o seletor do Android permite escolher o Google Drive " +
                    "como destino, se o app estiver instalado — sem precisar entrar com conta " +
                    "nenhuma aqui dentro.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { exportLauncher.launch("medicontrol-backup-${LocalDate.now()}.json") },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Exportar backup")
            }

            OutlinedButton(
                onClick = { showImportConfirm = true },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restaurar de um backup")
            }

            if (isWorking) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            statusMessage?.let { message ->
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Restaurar backup?") },
            text = { Text("Isso substitui todos os medicamentos e o histórico atuais pelos do arquivo escolhido. Não dá pra desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) { Text("Escolher arquivo") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}
