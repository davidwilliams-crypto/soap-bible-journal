package com.soapjournal.app.data.ink

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb

data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timestamp: Long = 0L
)

data class InkStroke(
    val points: List<InkPoint>,
    val colorArgb: Int = Color(0xFF2C2416).toArgb(),
    val width: Float = 4f,
    val isEraser: Boolean = false
) {
    fun toPath(): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            path.quadraticBezierTo(prev.x, prev.y, midX, midY)
        }
        val last = points.last()
        path.lineTo(last.x, last.y)
        return path
    }
}

data class InkDocument(
    val strokes: List<InkStroke> = emptyList(),
    val canvasWidth: Float = 0f,
    val canvasHeight: Float = 1200f
)
