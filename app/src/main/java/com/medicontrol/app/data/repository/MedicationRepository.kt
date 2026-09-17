package com.medicontrol.app.data.repository

import com.medicontrol.app.alarm.AlarmScheduler
import com.medicontrol.app.data.dao.DoseRecordDao
import com.medicontrol.app.data.dao.MedicationDao
import com.medicontrol.app.data.entity.DoseRecord
import com.medicontrol.app.data.entity.Medication
import com.medicontrol.app.data.model.DoseStatus
import com.medicontrol.app.data.model.DoseUiModel
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

/**
 * Ponto único de acesso aos dados: combina Room (persistência) com o
 * [AlarmScheduler] (agendamento do sistema), para que a UI nunca precise
 * lidar diretamente com AlarmManager.
 */
class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val doseRecordDao: DoseRecordDao,
    private val alarmScheduler: AlarmScheduler
) {

    fun observeMedications(): Flow<List<Medication>> = medicationDao.observeActive()

    suspend fun getMedication(id: Long): Medication? = medicationDao.getById(id)

    /** Insere ou atualiza um medicamento e (re)agenda seus alarmes de acordo. */
    suspend fun saveMedication(medication: Medication): Long {
        val previous = if (medication.id != 0L) medicationDao.getById(medication.id) else null
        previous?.let { alarmScheduler.cancelAll(it) }

        val id = if (medication.id == 0L) {
            medicationDao.insert(medication)
        } else {
            medicationDao.update(medication)
            medication.id
        }

        val saved = medication.copy(id = id)
        alarmScheduler.scheduleAll(saved)
        return id
    }

    suspend fun deleteMedication(medication: Medication) {
        alarmScheduler.cancelAll(medication)
        medicationDao.delete(medication)
    }

    /** Doses do dia [date], já cruzadas com o histórico e ordenadas por horário. */
    suspend fun getDosesForDate(date: LocalDate): List<DoseUiModel> {
        val medications = medicationDao.getAllActive().filter { it.isActiveOn(date) }
        val records = doseRecordDao.getBetween(date, date)
        val recordByKey = records.associateBy { Triple(it.medicationId, it.scheduledDate, it.scheduledTime) }

        return medications
            .flatMap { medication ->
                medication.times.map { time ->
                    val record = recordByKey[Triple(medication.id, date, time)]
                    DoseUiModel(
                        medication = medication,
                        time = time,
                        status = record?.status ?: DoseStatus.PENDING,
                        isLastDoseDay = medication.isLastDoseDay(date)
                    )
                }
            }
            .sortedBy { it.time }
    }

    /** Alterna o status da dose entre TOMADA e PENDENTE (usado pelo checkbox da Home). */
    suspend fun toggleDoseTaken(medication: Medication, date: LocalDate, time: LocalTime, taken: Boolean) {
        if (taken) {
            doseRecordDao.upsert(
                DoseRecord(
                    medicationId = medication.id,
                    scheduledDate = date,
                    scheduledTime = time,
                    status = DoseStatus.TAKEN,
                    actionAt = System.currentTimeMillis()
                )
            )
        } else {
            doseRecordDao.deleteSlot(medication.id, date, time)
        }
    }

    /**
     * Dias, dentro de [start]..[end], em que existe ao menos uma dose que já
     * deveria ter sido tomada (dia anterior a hoje) e nunca foi marcada como
     * TOMADA. Usado para destacar em vermelho a barra de dias no topo da Home.
     */
    suspend fun getMissedDays(start: LocalDate, end: LocalDate): Set<LocalDate> {
        val today = LocalDate.now()
        val medications = medicationDao.getAllActive()
        val records = doseRecordDao.getBetween(start, end)
        val takenKeys = records
            .filter { it.status == DoseStatus.TAKEN }
            .map { Triple(it.medicationId, it.scheduledDate, it.scheduledTime) }
            .toSet()

        val missedDays = mutableSetOf<LocalDate>()
        var date = start
        while (!date.isAfter(end)) {
            if (date.isBefore(today)) {
                val hasMissedDose = medications
                    .filter { it.isActiveOn(date) }
                    .any { medication ->
                        medication.times.any { time ->
                            Triple(medication.id, date, time) !in takenKeys
                        }
                    }
                if (hasMissedDose) missedDays += date
            }
            date = date.plusDays(1)
        }
        return missedDays
    }
}
