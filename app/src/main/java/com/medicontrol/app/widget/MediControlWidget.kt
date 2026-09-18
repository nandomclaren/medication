package com.medicontrol.app.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
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

/**
 * Widget "Próximas doses". Usa Material You (`GlanceTheme.colors`, dinâmico
 * a partir da paleta do papel de parede) no Android 12+; em versões
 * anteriores cai para a mesma paleta clara/escura do app ([WidgetColorFallback]),
 * já que o Glance só ganhou suporte a cor dinâmica na API 31.
 */
class MediControlWidget : GlanceAppWidget() {

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
                WidgetContent(today, doses)
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
private fun DoseRow(dose: DoseUiModel) {
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
                text = dose.medication.name,
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
