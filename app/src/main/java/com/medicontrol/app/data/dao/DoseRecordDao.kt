package com.medicontrol.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medicontrol.app.data.entity.DoseRecord
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DoseRecord)

    @Query(
        "DELETE FROM dose_records WHERE medicationId = :medicationId " +
            "AND scheduledDate = :date AND scheduledTime = :time"
    )
    suspend fun deleteSlot(medicationId: Long, date: LocalDate, time: LocalTime)

    @Query(
        "SELECT * FROM dose_records WHERE scheduledDate = :date"
    )
    fun observeForDate(date: LocalDate): Flow<List<DoseRecord>>

    @Query(
        "SELECT * FROM dose_records WHERE scheduledDate BETWEEN :start AND :end"
    )
    suspend fun getBetween(start: LocalDate, end: LocalDate): List<DoseRecord>
}
