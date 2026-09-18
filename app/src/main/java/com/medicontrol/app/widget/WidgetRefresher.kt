package com.medicontrol.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Pequeno ponto de indireção pra quem precisa pedir "atualiza o widget" sem depender direto da API do Glance. */
object WidgetRefresher {
    suspend fun refresh(context: Context) {
        MediControlWidget().updateAll(context)
    }
}
