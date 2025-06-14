package org.carstenf.wordfinder

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.widget.TableLayout
import androidx.appcompat.widget.AppCompatButton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun drawConnectionsBetweenButtons(tableLayout: TableLayout, buttons: List<AppCompatButton>) {
    val overlay = tableLayout.overlay
    overlay.clear()

    if(buttons.size<2) return

    val buttonWidth = buttons.firstOrNull()?.width?.toFloat() ?: 0f
    val width = buttonWidth * 0.2f
    val arrowSize = width * 2f
    val transparency = 230

    val linePaint = Paint().apply {
        color = Color.argb(transparency, 173, 230, 216)
        strokeWidth = width
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        isAntiAlias = true
    }

    val arrowPaint = Paint(linePaint).apply {
        style = Paint.Style.FILL
        alpha = transparency
    }

    for (i in 0 until buttons.size - 1) {
        val startButton = buttons[i]
        val endButton = buttons[i + 1]

        val startLocation = IntArray(2)
        val endLocation = IntArray(2)
        startButton.getLocationOnScreen(startLocation)
        endButton.getLocationOnScreen(endLocation)

        val tableLocation = IntArray(2)
        tableLayout.getLocationOnScreen(tableLocation)

        val startX = startLocation[0] - tableLocation[0] + startButton.width / 2f
        val startY = startLocation[1] - tableLocation[1] + startButton.height / 2f
        val endX = endLocation[0] - tableLocation[0] + endButton.width / 2f
        val endY = endLocation[1] - tableLocation[1] + endButton.height / 2f

        val angle = atan2(endY - startY, endX - startX)

        // Calculate arrow base point (where line should end)
        val arrowBaseX = endX - arrowSize * cos(angle).toFloat()
        val arrowBaseY = endY - arrowSize * sin(angle).toFloat()

        // Draw line ending exactly at arrow base
        val line = Path().apply {
            moveTo(startX, startY)
            lineTo(arrowBaseX, arrowBaseY)
        }
        overlay.add(DrawableCanvas(linePaint, line))

        // Draw arrowhead
        val arrowPath = Path().apply {
            moveTo(endX, endY)
            lineTo(
                arrowBaseX - width * sin(angle).toFloat(), // Perpendicular offset
                arrowBaseY + width * cos(angle).toFloat()
            )
            lineTo(
                arrowBaseX + width * sin(angle).toFloat(),
                arrowBaseY - width * cos(angle).toFloat()
            )
            close()
        }
        overlay.add(DrawableCanvas(arrowPaint, arrowPath))
    }
}

private class DrawableCanvas(private val paint: Paint, private val path: Path) : Drawable() {
    override fun draw(canvas: Canvas) { canvas.drawPath(path, paint) }
    override fun setAlpha(alpha: Int) {} // Not needed as transparency is set in paint
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}