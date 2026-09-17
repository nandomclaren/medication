package com.medicontrol.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medicontrol.app.data.entity.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: Medication): Long

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): Medication?

    @Query("SELECT * FROM medications WHERE active = 1 ORDER BY name")
    fun observeActive(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE active = 1")
    suspend fun getAllActive(): List<Medication>
}
