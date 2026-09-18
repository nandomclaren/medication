package com.medicontrol.app.data.backup

import com.medicontrol.app.data.entity.DoseRecord
import com.medicontrol.app.data.entity.Medication
import com.medicontrol.app.data.model.DoseStatus
import com.medicontrol.app.data.model.MedicationIcon
import com.medicontrol.app.data.model.RecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable

/**
 * Formato serializável do backup local. Usa apenas tipos primitivos/String
 * (datas e horas em ISO-8601) porque `java.time.*` não é suportado
 * diretamente pelo kotlinx.serialization — ver as funções `toBackup()`/`toEntity()`.
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: String,
    val medications: List<MedicationBackup>,
    val doseRecords: List<DoseRecordBackup>
)

@Serializable
data class MedicationBackup(
    val id: Long,
    val name: String,
    val dosage: String,
    val startDate: String,
    val endDate: String?,
    val isContinuous: Boolean,
    val times: List<String>,
    val icon: String,
    val colorHex: String,
    val recurrenceType: String,
    val recurrenceIntervalDays: Int,
    val recurrenceWeekdays: List<Int>,
    val recurrenceIntervalHours: Int,
    val stockQuantity: Int?,
    val stockThreshold: Int,
    val active: Boolean
)

@Serializable
data class DoseRecordBackup(
    val id: Long,
    val medicationId: Long,
    val scheduledDate: String,
    val scheduledTime: String,
    val status: String,
    val actionAt: Long?
)

fun Medication.toBackup() = MedicationBackup(
    id = id,
    name = name,
    dosage = dosage,
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
    isContinuous = isContinuous,
    times = times.map { it.toString() },
    icon = icon.name,
    colorHex = colorHex,
    recurrenceType = recurrenceType.name,
    recurrenceIntervalDays = recurrenceIntervalDays,
    recurrenceWeekdays = recurrenceWeekdays.map { it.value },
    recurrenceIntervalHours = recurrenceIntervalHours,
    stockQuantity = stockQuantity,
    stockThreshold = stockThreshold,
    active = active
)

fun MedicationBackup.toEntity() = Medication(
    id = id,
    name = name,
    dosage = dosage,
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let { LocalDate.parse(it) },
    isContinuous = isContinuous,
    times = times.map { LocalTime.parse(it) },
    icon = MedicationIcon.valueOf(icon),
    colorHex = colorHex,
    recurrenceType = RecurrenceType.valueOf(recurrenceType),
    recurrenceIntervalDays = recurrenceIntervalDays,
    recurrenceWeekdays = recurrenceWeekdays.map { DayOfWeek.of(it) }.toSet(),
    recurrenceIntervalHours = recurrenceIntervalHours,
    stockQuantity = stockQuantity,
    stockThreshold = stockThreshold,
    active = active
)

fun DoseRecord.toBackup() = DoseRecordBackup(
    id = id,
    medicationId = medicationId,
    scheduledDate = scheduledDate.toString(),
    scheduledTime = scheduledTime.toString(),
    status = status.name,
    actionAt = actionAt
)

fun DoseRecordBackup.toEntity() = DoseRecord(
    id = id,
    medicationId = medicationId,
    scheduledDate = LocalDate.parse(scheduledDate),
    scheduledTime = LocalTime.parse(scheduledTime),
    status = DoseStatus.valueOf(status),
    actionAt = actionAt
)
