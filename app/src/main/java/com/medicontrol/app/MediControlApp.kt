package com.medicontrol.app

import android.app.Application
import com.medicontrol.app.alarm.AlarmScheduler
import com.medicontrol.app.alarm.NotificationHelper
import com.medicontrol.app.data.db.AppDatabase
import com.medicontrol.app.data.repository.MedicationRepository

/**
 * Application "manual" (sem framework de DI): cria o banco, o scheduler e o
 * repositório uma única vez e os expõe para as ViewModels via [ViewModelFactory].
 */
class MediControlApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val alarmScheduler: AlarmScheduler by lazy { AlarmScheduler(this) }
    val repository: MedicationRepository by lazy {
        MedicationRepository(database.medicationDao(), database.doseRecordDao(), alarmScheduler)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
