package com.medicontrol.app.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.medicontrol.app.MainActivity
import com.medicontrol.app.MediControlApp
import com.medicontrol.app.data.model.DoseStatus
import com.medicontrol.app.data.model.DoseUiModel
import com.medicontrol.app.ui.theme.WidgetColorFallback
import com.medicontrol.app.util.toDisplayString
import com.medicontrol.app.util.toShortWeekdayString
import java.time.LocalDate

private const val MAX_VISIBLE_ROWS = 4

// As duas paragens que o widget pode assumir — o Android "encaixa" o
// tamanho real (o usuário arrasta livremente) na mais próxima destas duas.
private val COMPACT_SIZE = DpSize(110.dp, 56.dp)
private val MEDIUM_SIZE = DpSize(200.dp, 110.dp)

// Abaixo desta largura já não cabe a lista — mostramos só a próxima dose.
private val COMPACT_WIDTH_THRESHOLD = 160.dp

/**
 * Widget "Próximas doses". Usa Material You (`GlanceTheme.colors`, dinâmico
 * a partir da paleta do papel de parede) no Android 12+; em versões
 * anteriores cai para a mesma paleta clara/escura do app ([WidgetColorFallback]),
 * já que o Glance só ganhou suporte a cor dinâmica na API 31.
 */
class MediControlWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(COMPACT_SIZE, MEDIUM_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as MediControlApp
        val today = LocalDate.now()
        val doses = app.repository.getDosesForDate(today)

        provideContent {
            val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                GlanceTheme.colors
            } else {
                WidgetColorFallback
            }
            GlanceTheme(colors = colors) {
                if (LocalSize.current.width < COMPACT_WIDTH_THRESHOLD) {
                    CompactWidgetContent(doses)
                } else {
                    WidgetContent(today, doses)
                }
            }
        }
    }
}

@Composable
private fun WidgetContent(today: LocalDate, doses: List<DoseUiModel>) {
    val allDone = doses.isEmpty() || doses.all { it.status == DoseStatus.TAKEN }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        WidgetHeader(today)
        Spacer(modifier = GlanceModifier.height(6.dp))

        if (allDone) {
            EmptyState(doses)
        } else {
            val visible = doses.take(MAX_VISIBLE_ROWS)
            visible.forEach { dose -> DoseRow(dose) }
            val remaining = doses.size - visible.size
            if (remaining > 0) {
                Text(
                    text = "+ $remaining mais tarde",
                    style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Layout compacto (~2×1): só a próxima dose pendente. Quando mais de um
 * remédio cai no mesmo horário, mostra o primeiro + "+N" — o checkbox marca
 * só esse; os demais ficam pro toque no resto do card, que abre o app.
 */
@Composable
private fun CompactWidgetContent(doses: List<DoseUiModel>) {
    val pending = doses.filter { it.status != DoseStatus.TAKEN }.sortedBy { it.time }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(18.dp)
            .padding(10.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Text(
            text = "Próxima dose",
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))

        if (pending.isEmpty()) {
            Text(
                text = "Tudo em dia",
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
        } else {
            val earliestTime = pending.first().time
            val atSameTime = pending.filter { it.time == earliestTime }
            val label = if (atSameTime.size > 1) {
                "${atSameTime.first().medication.name} +${atSameTime.size - 1}"
            } else {
                atSameTime.first().medication.name
            }
            DoseRow(dose = atSameTime.first(), nameOverride = label)
        }
    }
}

@Composable
private fun WidgetHeader(today: LocalDate) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = "Próximas doses",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.primary
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = today.toShortWeekdayString(),
            style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant)
        )
    }
}

@Composable
private fun DoseRow(dose: DoseUiModel, nameOverride: String? = null) {
    val taken = dose.status == DoseStatus.TAKEN

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .size(20.dp)
                .cornerRadius(6.dp)
                .background(ColorProvider(dose.medication.color.composeColor))
        ) {}

        Spacer(modifier = GlanceModifier.width(8.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = nameOverride ?: dose.medication.name,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface,
                    textDecoration = if (taken) TextDecoration.LineThrough else TextDecoration.None
                )
            )
            Text(
                text = dose.medication.dosage,
                maxLines = 1,
                style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
        }

        Text(
            text = dose.time.toDisplayString(),
            style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant)
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        var checkModifier = GlanceModifier
            .size(18.dp)
            .cornerRadius(9.dp)
            .background(if (taken) GlanceTheme.colors.primary else GlanceTheme.colors.surfaceVariant)
        if (!taken) {
            checkModifier = checkModifier.clickable(
                actionRunCallback<MarkDoseTakenAction>(
                    actionParametersOf(
                        MarkDoseTakenAction.KEY_MEDICATION_ID to dose.medication.id,
                        MarkDoseTakenAction.KEY_TIME to dose.time.toString()
                    )
                )
            )
        }
        Box(modifier = checkModifier, contentAlignment = Alignment.Center) {
            if (taken) {
                Image(
                    provider = ImageProvider(android.R.drawable.checkbox_on_background),
                    contentDescription = null,
                    modifier = GlanceModifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(doses: List<DoseUiModel>) {
    val nextTime = doses.filter { it.status != DoseStatus.TAKEN }.minByOrNull { it.time }?.time

    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Tudo em dia",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onSurface
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = nextTime?.let { "Próxima dose às ${it.toDisplayString()}" } ?: "Nenhuma dose hoje",
            style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant)
        )
    }
}
