package com.medicontrol.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.medicontrol.app.data.model.DoseStatus
import java.time.LocalDate
import java.time.LocalTime

/**
 * Registro histórico do estado de UMA dose (medicamento + dia + horário).
 *
 * Só existe uma linha aqui quando o usuário efetivamente interage com a dose
 * (marca como tomada ou pula). Doses que ainda não passaram, ou que passaram mas
 * nunca foram tocadas, não têm linha correspondente e são tratadas como
 * [DoseStatus.PENDING] "virtuais" pelo repositório — isso evita ter que
 * pré-gerar milhares de linhas para medicamentos de uso contínuo.
 */
@Entity(
    tableName = "dose_records",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medicationId", "scheduledDate", "scheduledTime"], unique = true),
        Index(value = ["scheduledDate"])
    ]
)
data class DoseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val medicationId: Long,
    val scheduledDate: LocalDate,
    val scheduledTime: LocalTime,
    val status: DoseStatus,
    val actionAt: Long? = null
)
