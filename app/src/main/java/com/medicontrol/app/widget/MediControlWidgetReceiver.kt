package com.medicontrol.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Ponto de entrada do sistema para o widget — registrado no AndroidManifest. */
class MediControlWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediControlWidget()
}
