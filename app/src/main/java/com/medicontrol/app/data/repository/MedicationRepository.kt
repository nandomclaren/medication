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
 * [AlarmScheduler] (agendamento do sistema), para que a UI e os
 * BroadcastReceivers nunca precisem mexer em AlarmManager ou estoque na mão.
 *
 * [onDataChanged] é chamado depois de qualquer escrita que muda o que
 * aparece na tela — hoje só usado para atualizar o widget de tela inicial
 * (ver `MediControlApp`), mantendo o repositório sem depender diretamente
 * de APIs do Glance/AppWidget.
 */
class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val doseRecordDao: DoseRecordDao,
    private val alarmScheduler: AlarmScheduler,
    private val onDataChanged: suspend () -> Unit = {}
) {

    fun observeMedications(): Flow<List<Medication>> = medicationDao.observeActive()

    suspend fun getMedication(id: Long): Medication? = medicationDao.getById(id)

    suspend fun getAllActiveMedications(): List<Medication> = medicationDao.getAllActive()

    /** Insere ou atualiza um medicamento e reconcilia os alarmes de acordo com a nova cadência. */
    suspend fun saveMedication(medication: Medication): Long {
        val id = if (medication.id == 0L) {
            medicationDao.insert(medication)
        } else {
            medicationDao.update(medication)
            medication.id
        }
        reconcileAlarms()
        onDataChanged()
        return id
    }

    suspend fun deleteMedication(medication: Medication) {
        medicationDao.delete(medication)
        reconcileAlarms()
        onDataChanged()
    }

    /** Recalcula os alarmes do sistema a partir do conjunto atual de medicamentos ativos. Chamado após salvar/excluir e no boot. */
    suspend fun reconcileAlarms() {
        alarmScheduler.reconcile(medicationDao.getAllActive())
    }

    /** Doses do dia [date], já cruzadas com o histórico e ordenadas por horário. */
    suspend fun getDosesForDate(date: LocalDate): List<DoseUiModel> {
        val medications = medicationDao.getAllActive().filter { it.isActiveOn(date) }
        val records = doseRecordDao.getBetween(date, date)
        val recordByKey = records.associateBy { Triple(it.medicationId, it.scheduledDate, it.scheduledTime) }

        return medications
            .flatMap { medication ->
                medication.doseTimesOn(date).map { time ->
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

    /** Usado pelo checkbox da Home: alterna entre TOMADA e PENDENTE. */
    suspend fun toggleDoseTaken(medication: Medication, date: LocalDate, time: LocalTime, taken: Boolean) {
        setDoseStatus(medication, date, time, if (taken) DoseStatus.TAKEN else null)
    }

    /** Usado pelas ações "Marcar todos"/"Pular todos" da notificação agrupada. */
    suspend fun setGroupDoseStatus(medicationIds: List<Long>, date: LocalDate, time: LocalTime, status: DoseStatus) {
        medicationIds.forEach { id ->
            medicationDao.getById(id)?.let { setDoseStatus(it, date, time, status) }
        }
    }

    /**
     * Define o status de uma dose. [status] nulo volta a dose para "pendente"
     * (remove o registro). Ajusta o estoque do medicamento quando a dose entra
     * ou sai do status TOMADA — inclusive ao desfazer uma marcação.
     */
    suspend fun setDoseStatus(medication: Medication, date: LocalDate, time: LocalTime, status: DoseStatus?) {
        val previous = doseRecordDao.getBetween(date, date)
            .firstOrNull { it.medicationId == medication.id && it.scheduledTime == time }

        val wasTaken = previous?.status == DoseStatus.TAKEN
        val willBeTaken = status == DoseStatus.TAKEN
        if (wasTaken != willBeTaken) {
            adjustStock(medication, delta = if (willBeTaken) -1 else 1)
        }

        if (status == null) {
            doseRecordDao.deleteSlot(medication.id, date, time)
        } else {
            doseRecordDao.upsert(
                DoseRecord(
                    medicationId = medication.id,
                    scheduledDate = date,
                    scheduledTime = time,
                    status = status,
                    actionAt = System.currentTimeMillis()
                )
            )
        }
        onDataChanged()
    }

    private suspend fun adjustStock(medication: Medication, delta: Int) {
        val quantity = medication.stockQuantity ?: return
        medicationDao.update(medication.copy(stockQuantity = (quantity + delta).coerceAtLeast(0)))
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
                        medication.doseTimesOn(date).any { time ->
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
