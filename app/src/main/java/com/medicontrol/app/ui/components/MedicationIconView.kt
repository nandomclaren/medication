package com.medicontrol.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.medicontrol.app.data.model.MedicationIcon

/**
 * Desenha o ícone do medicamento inteiramente via Canvas (sem depender de
 * assets/imagens externas), na cor escolhida pelo usuário no cadastro.
 */
@Composable
fun MedicationIconView(
    icon: MedicationIcon,
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp
) {
    Canvas(modifier = modifier.size(iconSize)) {
        when (icon) {
            MedicationIcon.CAPSULE -> drawCapsule(color)
            MedicationIcon.ROUND_PILL -> drawRoundPill(color)
            MedicationIcon.DROPS -> drawDrop(color)
            MedicationIcon.INJECTION -> drawInjection(color)
        }
    }
}

private fun DrawScope.drawCapsule(color: Color) {
    rotate(45f) {
        val h = size.height * 0.5f
        val top = (size.height - h) / 2f
        val rect = Rect(left = size.width * 0.05f, top = top, right = size.width * 0.95f, bottom = top + h)
        val corner = CornerRadius(h / 2f, h / 2f)

        clipRect(left = rect.left, top = rect.top, right = rect.center.x, bottom = rect.bottom) {
            drawRoundRect(color = color, topLeft = rect.topLeft, size = Size(rect.width, rect.height), cornerRadius = corner)
        }
        clipRect(left = rect.center.x, top = rect.top, right = rect.right, bottom = rect.bottom) {
            drawRoundRect(color = color.copy(alpha = 0.3f), topLeft = rect.topLeft, size = Size(rect.width, rect.height), cornerRadius = corner)
        }
        drawRoundRect(
            color = color,
            topLeft = rect.topLeft,
            size = Size(rect.width, rect.height),
            cornerRadius = corner,
            style = Stroke(width = size.minDimension * 0.04f)
        )
    }
}

private fun DrawScope.drawRoundPill(color: Color) {
    val radius = size.minDimension * 0.42f
    drawCircle(color = color, radius = radius, center = center)
    drawLine(
        color = Color.White,
        start = Offset(center.x - radius * 0.75f, center.y),
        end = Offset(center.x + radius * 0.75f, center.y),
        strokeWidth = size.minDimension * 0.06f
    )
}

private fun DrawScope.drawDrop(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w / 2f, h * 0.08f)
        cubicTo(w * 0.88f, h * 0.5f, w * 0.8f, h * 0.92f, w / 2f, h * 0.92f)
        cubicTo(w * 0.2f, h * 0.92f, w * 0.12f, h * 0.5f, w / 2f, h * 0.08f)
        close()
    }
    drawPath(path, color = color)
    drawCircle(color = Color.White.copy(alpha = 0.55f), radius = w * 0.08f, center = Offset(w * 0.42f, h * 0.55f))
}

private fun DrawScope.drawInjection(color: Color) {
    val w = size.width
    val h = size.height
    val strokeWidth = size.minDimension * 0.05f

    val barrel = Rect(left = w * 0.18f, top = h * 0.38f, right = w * 0.72f, bottom = h * 0.62f)
    drawRect(color = color.copy(alpha = 0.25f), topLeft = barrel.topLeft, size = Size(barrel.width, barrel.height))
    drawRect(color = color, topLeft = barrel.topLeft, size = Size(barrel.width, barrel.height), style = Stroke(width = strokeWidth))

    // êmbolo
    drawRect(color = color, topLeft = Offset(w * 0.04f, h * 0.34f), size = Size(w * 0.14f, h * 0.32f))

    // agulha
    drawLine(color = color, start = Offset(barrel.right, h * 0.5f), end = Offset(w * 0.96f, h * 0.5f), strokeWidth = strokeWidth * 0.6f)

    // marcações de graduação
    for (i in 1..3) {
        val x = barrel.left + barrel.width * i / 4f
        drawLine(color = Color.White, start = Offset(x, barrel.top), end = Offset(x, barrel.bottom), strokeWidth = strokeWidth * 0.3f)
    }
}
