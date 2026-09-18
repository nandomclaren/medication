package com.medicontrol.app.data.db

import androidx.room.TypeConverter
import com.medicontrol.app.data.model.DoseStatus
import com.medicontrol.app.data.model.MedicationIcon
import com.medicontrol.app.data.model.RecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Conversores para os tipos usados nas entidades — Room só entende tipos primitivos/String. */
class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString() // ISO-8601 yyyy-MM-dd

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString() // ISO-8601 HH:mm

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromTimeList(times: List<LocalTime>): String =
        times.joinToString(separator = ";") { it.toString() }

    @TypeConverter
    fun toTimeList(value: String): List<LocalTime> =
        if (value.isBlank()) emptyList()
        else value.split(";").map { LocalTime.parse(it) }

    @TypeConverter
    fun fromMedicationIcon(icon: MedicationIcon): String = icon.name

    @TypeConverter
    fun toMedicationIcon(value: String): MedicationIcon = MedicationIcon.valueOf(value)

    @TypeConverter
    fun fromDoseStatus(status: DoseStatus): String = status.name

    @TypeConverter
    fun toDoseStatus(value: String): DoseStatus = DoseStatus.valueOf(value)

    @TypeConverter
    fun fromRecurrenceType(type: RecurrenceType): String = type.name

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType = RecurrenceType.valueOf(value)

    @TypeConverter
    fun fromWeekdaySet(days: Set<DayOfWeek>): String =
        days.joinToString(separator = ",") { it.value.toString() }

    @TypeConverter
    fun toWeekdaySet(value: String): Set<DayOfWeek> =
        if (value.isBlank()) emptySet()
        else value.split(",").map { DayOfWeek.of(it.trim().toInt()) }.toSet()
}
