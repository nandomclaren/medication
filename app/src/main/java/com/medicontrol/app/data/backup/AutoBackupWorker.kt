package com.medicontrol.app.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medicontrol.app.MediControlApp

/** Roda uma vez por dia (ver [AutoBackupScheduler]) e sobrescreve o mesmo arquivo escolhido pelo usuário. */
class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uri = AutoBackupPrefs.getUri(applicationContext) ?: return Result.failure()
        val app = applicationContext as MediControlApp

        return try {
            app.backupManager.export(uri)
            AutoBackupPrefs.markSuccess(applicationContext, System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            // Uri revogada (arquivo apagado, permissão perdida) ou erro de I/O — tenta de novo no próximo ciclo.
            Result.retry()
        }
    }
}
