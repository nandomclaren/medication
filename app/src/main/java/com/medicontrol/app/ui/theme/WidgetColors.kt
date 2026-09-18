package com.medicontrol.app.ui.theme

import androidx.glance.material3.ColorProviders

/**
 * Paleta usada pelo widget (Jetpack Glance) em aparelhos anteriores ao
 * Android 12, onde não existe Material You — mesma paleta clara/escura do
 * app (ver Theme.kt), só reembalada no formato que o Glance espera. A
 * partir da API 31, o widget usa `GlanceTheme.colors` (dinâmico) em vez
 * desta paleta — ver MediControlWidget.kt.
 */
val WidgetColorFallback = ColorProviders(light = LightColors, dark = DarkColors)
