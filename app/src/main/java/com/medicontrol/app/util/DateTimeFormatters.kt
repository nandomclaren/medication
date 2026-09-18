package com.medicontrol.app.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ptBr = Locale("pt", "BR")

fun LocalTime.toDisplayString(): String = format(DateTimeFormatter.ofPattern("HH:mm"))

fun LocalDate.toDisplayString(): String =
    format(DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBr))

fun LocalDate.monthLabel(): String =
    month.getDisplayName(TextStyle.FULL, ptBr).replaceFirstChar { it.uppercase() } + " de $year"

/** Formato compacto usado no cabeçalho do widget, ex: "Qui, 18 set". */
fun LocalDate.toShortWeekdayString(): String {
    val weekday = dayOfWeek.getDisplayName(TextStyle.SHORT, ptBr).replaceFirstChar { it.uppercase() }
    val month = format(DateTimeFormatter.ofPattern("MMM", ptBr)).replaceFirstChar { it.uppercase() }
    return "$weekday, $dayOfMonth $month".replace(".", "")
}
