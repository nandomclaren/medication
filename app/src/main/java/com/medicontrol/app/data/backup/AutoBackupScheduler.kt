package com.medicontrol.app.data.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Liga/desliga o job diário de backup automático (ver [AutoBackupWorker]). */
object AutoBackupScheduler {
    private const val WORK_NAME = "auto_backup_daily"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
