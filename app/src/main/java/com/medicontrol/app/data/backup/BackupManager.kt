package com.medicontrol.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.medicontrol.app.data.db.AppDatabase
import java.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Exporta/importa todos os dados do app (medicamentos + histórico de doses)
 * como um único arquivo JSON, escolhido pelo usuário via o seletor de
 * arquivos do próprio Android (Storage Access Framework). Isso inclui
 * qualquer provedor de armazenamento instalado — Google Drive, por exemplo
 * — sem o app precisar de credenciais/OAuth ou de uma conta própria: quem
 * decide onde salvar é o sistema, o app só grava/lê o `Uri` retornado.
 *
 * Importar SUBSTITUI todos os dados atuais pelos do arquivo (é uma restauração,
 * não uma mesclagem) — a UI deve confirmar isso com o usuário antes de chamar [import].
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun export(uri: Uri) {
        val medications = database.medicationDao().getAllActive()
        val doseRecords = database.doseRecordDao().getAll()
        val backup = BackupData(
            exportedAt = Instant.now().toString(),
            medications = medications.map { it.toBackup() },
            doseRecords = doseRecords.map { it.toBackup() }
        )
        val text = json.encodeToString(backup)
        val stream = context.contentResolver.openOutputStream(uri)
            ?: error("Não foi possível abrir o arquivo para escrita")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    /** Retorna quantos medicamentos foram restaurados. */
    suspend fun import(uri: Uri): Int {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Não foi possível abrir o arquivo selecionado")
        val text = stream.use { it.readBytes().toString(Charsets.UTF_8) }
        val backup = json.decodeFromString(BackupData.serializer(), text)

        database.withTransaction {
            database.medicationDao().deleteAll()
            database.medicationDao().insertAll(backup.medications.map { it.toEntity() })
            database.doseRecordDao().deleteAll()
            database.doseRecordDao().insertAll(backup.doseRecords.map { it.toEntity() })
        }
        return backup.medications.size
    }
}
