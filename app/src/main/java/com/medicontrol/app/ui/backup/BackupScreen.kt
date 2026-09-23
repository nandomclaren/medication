package com.medicontrol.app.ui.backup

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.medicontrol.app.MediControlApp
import com.medicontrol.app.data.backup.AutoBackupPrefs
import com.medicontrol.app.data.backup.AutoBackupScheduler
import com.medicontrol.app.util.toDisplayDateTime
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
    var autoBackupEnabled by remember { mutableStateOf(AutoBackupPrefs.isEnabled(context)) }
    var lastAutoBackup by remember { mutableStateOf(AutoBackupPrefs.getLastSuccess(context)) }

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

    val autoBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            autoBackupEnabled = false
            return@rememberLauncherForActivityResult
        }
        // Sem isso a permissão de escrita nesse Uri some assim que o app for encerrado —
        // e o job diário, rodando em background, precisa dela dias depois.
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        AutoBackupPrefs.enable(context, uri)
        AutoBackupScheduler.schedule(context)
        isWorking = true
        scope.launch {
            runCatching { app.backupManager.export(uri) }
                .onSuccess {
                    val now = System.currentTimeMillis()
                    AutoBackupPrefs.markSuccess(context, now)
                    lastAutoBackup = now
                    statusMessage = "Backup automático ativado."
                }
                .onFailure { statusMessage = "Backup automático ativado, mas o primeiro envio falhou: ${it.message}" }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(
                    checked = autoBackupEnabled,
                    enabled = !isWorking,
                    onCheckedChange = { checked ->
                        autoBackupEnabled = checked
                        if (checked) {
                            autoBackupLauncher.launch("medicontrol-backup-automatico.json")
                        } else {
                            AutoBackupPrefs.disable(context)
                            AutoBackupScheduler.cancel(context)
                        }
                    }
                )
                Text("Backup automático diário")
            }
            Text(
                "Sobrescreve o mesmo arquivo todo dia — escolha o local uma vez (pode ser no " +
                    "Google Drive) e o app cuida do resto sozinho, mesmo fechado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            lastAutoBackup?.let { timestamp ->
                Text(
                    "Última execução automática: ${timestamp.toDisplayDateTime()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
