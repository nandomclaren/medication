package com.medicontrol.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.medicontrol.app.data.dao.DoseRecordDao
import com.medicontrol.app.data.dao.MedicationDao
import com.medicontrol.app.data.entity.DoseRecord
import com.medicontrol.app.data.entity.Medication

@Database(
    entities = [Medication::class, DoseRecord::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao
    abstract fun doseRecordDao(): DoseRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medicontrol.db"
                )
                    // App pré-lançamento, sem base instalada em produção ainda: em vez de
                    // escrever Migrations manuais a cada mudança de schema, recriamos o banco.
                    // Trocar por Migrations reais antes do primeiro release público.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
