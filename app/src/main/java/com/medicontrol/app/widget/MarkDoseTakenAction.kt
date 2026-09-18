package com.medicontrol.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.medicontrol.app.MediControlApp
import com.medicontrol.app.data.model.DoseStatus
import java.time.LocalDate
import java.time.LocalTime

/**
 * Ação do "checkbox" de cada dose no widget. Passa pelo mesmo
 * [com.medicontrol.app.data.repository.MedicationRepository.setDoseStatus]
 * usado pela Home e pela notificação — mesmo ajuste de estoque, mesma fonte
 * de verdade — e o próprio repositório já dispara a atualização do widget
 * ao final (via `onDataChanged`), então não precisamos chamar `updateAll` aqui.
 */
class MarkDoseTakenAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val medicationId = parameters[KEY_MEDICATION_ID] ?: return
        val time = parameters[KEY_TIME]?.let { LocalTime.parse(it) } ?: return

        val repository = (context.applicationContext as MediControlApp).repository
        val medication = repository.getMedication(medicationId) ?: return
        repository.setDoseStatus(medication, LocalDate.now(), time, DoseStatus.TAKEN)
    }

    companion object {
        val KEY_MEDICATION_ID = ActionParameters.Key<Long>("medication_id")
        val KEY_TIME = ActionParameters.Key<String>("time")
    }
}
