package com.soapjournal.app.ui.ink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.soapjournal.app.data.ink.InkDocument
import com.soapjournal.app.data.ink.InkPoint
import com.soapjournal.app.data.ink.InkStroke
import com.soapjournal.app.ui.theme.InkBrown
import com.soapjournal.app.ui.theme.Paper
import com.soapjournal.app.ui.theme.PaperDark

enum class InkTool {
    PEN,
    ERASER
}

@Composable
fun InkCanvas(
    document: InkDocument,
    tool: InkTool,
    strokeWidth: Float,
    inkColor: Color = InkBrown,
    onDocumentChange: (InkDocument) -> Unit,
    modifier: Modifier = Modifier,
    stylusOnly: Boolean = false
) {
    var canvasWidth by remember { mutableFloatStateOf(document.canvasWidth) }
    var activeStroke by remember { mutableStateOf<InkStroke?>(null) }
    val currentDocument by rememberUpdatedState(document)
    val currentOnChange by rememberUpdatedState(onDocumentChange)
    val density = LocalDensity.current
    val minHeightPx = with(density) { 1200.dp.toPx() }
    val contentHeight = maxOf(document.canvasHeight, minHeightPx)

    Box(
        modifier = modifier
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .heightIn(min = with(density) { contentHeight.toDp() })
            .onSizeChanged { size ->
                canvasWidth = size.width.toFloat()
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = with(density) { contentHeight.toDp() })
                .pointerInput(tool, strokeWidth, inkColor, stylusOnly) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val isStylusLike = down.type == PointerType.Stylus ||
                            down.type == PointerType.Eraser ||
                            down.type == PointerType.Mouse
                        if (stylusOnly && !isStylusLike) {
                            return@awaitEachGesture
                        }

                        val points = mutableListOf(
                            InkPoint(
                                x = down.position.x,
                                y = down.position.y,
                                pressure = down.pressure.coerceIn(0.1f, 1f),
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        val isEraser = tool == InkTool.ERASER || down.type == PointerType.Eraser
                        activeStroke = InkStroke(
                            points = points.toList(),
                            colorArgb = inkColor.toArgb(),
                            width = if (isEraser) strokeWidth * 3f else strokeWidth,
                            isEraser = isEraser
                        )

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.positionChange() != Offset.Zero || change.pressed) {
                                points += InkPoint(
                                    x = change.position.x,
                                    y = change.position.y,
                                    pressure = change.pressure.coerceIn(0.1f, 1f),
                                    timestamp = System.currentTimeMillis()
                                )
                                activeStroke = activeStroke?.copy(points = points.toList())
                                change.consume()
                            }
                            if (!change.pressed) break
                        } while (true)

                        val finished = activeStroke
                        activeStroke = null
                        if (finished != null && finished.points.size >= 2) {
                            val latest = currentDocument
                            val maxY = finished.points.maxOf { it.y } + 120f
                            val newHeight = maxOf(latest.canvasHeight, maxY, minHeightPx)
                            currentOnChange(
                                latest.copy(
                                    strokes = latest.strokes + finished,
                                    canvasWidth = canvasWidth,
                                    canvasHeight = newHeight
                                )
                            )
                        }
                    }
                }
        ) {
            val lineSpacing = 48.dp.toPx()
            var y = lineSpacing
            while (y < size.height) {
                drawLine(
                    color = PaperDark,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += lineSpacing
            }

            val strokesToDraw = activeStroke?.let { document.strokes + it } ?: document.strokes
            strokesToDraw.forEach { stroke ->
                val path = stroke.toPath()
                drawPath(
                    path = path,
                    color = if (stroke.isEraser) Paper else Color(stroke.colorArgb),
                    style = Stroke(
                        width = stroke.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
