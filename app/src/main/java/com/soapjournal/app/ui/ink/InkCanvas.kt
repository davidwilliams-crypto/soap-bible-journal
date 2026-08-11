package com.soapjournal.app.ui.ink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.soapjournal.app.data.ink.InkDocument
import com.soapjournal.app.data.ink.InkPoint
import com.soapjournal.app.data.ink.InkStroke
import com.soapjournal.app.ui.theme.LocalJournalSurfaces

enum class InkTool {
    PEN,
    ERASER
}

/**
 * Mutable gate so drawing can block nested scroll without recomposing mid-stroke.
 */
private class DrawingScrollGate {
    @Volatile
    var drawing: Boolean = false
}

@Composable
fun InkCanvas(
    document: InkDocument,
    tool: InkTool,
    strokeWidth: Float,
    inkColor: Color? = null,
    onDocumentChange: (InkDocument) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * When true, only stylus / mouse / eraser pointers can draw.
     * Default false so Samsung S Pen fallback-as-touch still works.
     */
    stylusOnly: Boolean = false
) {
    val surfaces = LocalJournalSurfaces.current
    val resolvedInk = inkColor ?: surfaces.ink
    val paperColor = surfaces.paper
    val ruleColor = surfaces.rule
    var canvasWidth by remember { mutableFloatStateOf(document.canvasWidth) }
    var activeStroke by remember { mutableStateOf<InkStroke?>(null) }
    val currentDocument by rememberUpdatedState(document)
    val currentOnChange by rememberUpdatedState(onDocumentChange)
    val density = LocalDensity.current
    val minHeightPx = with(density) { InkDefaults.MinCanvasHeightDp.dp.toPx() }
    val contentHeight = maxOf(document.canvasHeight, minHeightPx)
    val contentHeightDp = with(density) { contentHeight.toDp() }
    val scrollState = rememberScrollState()
    val scrollGate = remember { DrawingScrollGate() }
    val blockScrollWhileDrawing = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Consume scroll deltas while a stroke is active so Samsung S Pen
                // writing is not stolen by the paper scroll container.
                return if (scrollGate.drawing) available else Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .background(paperColor)
            .nestedScroll(blockScrollWhileDrawing)
            .verticalScroll(scrollState)
            .onSizeChanged { size ->
                canvasWidth = size.width.toFloat()
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeightDp)
                .pointerInput(tool, strokeWidth, resolvedInk, stylusOnly) {
                    awaitEachGesture {
                        // Initial pass so we win against scrollable / pager parents.
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        val pointerType = down.type
                        val isStylusLike = pointerType == PointerType.Stylus ||
                            pointerType == PointerType.Eraser ||
                            pointerType == PointerType.Mouse
                        // Samsung One UI sometimes delivers S Pen as Touch.
                        val isTouch = pointerType == PointerType.Touch
                        if (stylusOnly && !isStylusLike) {
                            return@awaitEachGesture
                        }
                        if (!isStylusLike && !isTouch) {
                            return@awaitEachGesture
                        }

                        down.consume()
                        scrollGate.drawing = true

                        val points = mutableListOf(
                            InkPoint(
                                x = down.position.x,
                                y = down.position.y,
                                pressure = down.pressure.takeIf { it > 0f }?.coerceIn(0.05f, 1f) ?: 1f,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        val isEraser = tool == InkTool.ERASER || pointerType == PointerType.Eraser
                        activeStroke = InkStroke(
                            points = points.toList(),
                            colorArgb = resolvedInk.toArgb(),
                            width = if (isEraser) strokeWidth * 3f else strokeWidth,
                            isEraser = isEraser
                        )

                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull {
                                    it.id == down.id
                                } ?: event.changes.firstOrNull() ?: break

                                change.consume()
                                points += InkPoint(
                                    x = change.position.x,
                                    y = change.position.y,
                                    pressure = change.pressure.takeIf { it > 0f }
                                        ?.coerceIn(0.05f, 1f) ?: 1f,
                                    timestamp = System.currentTimeMillis()
                                )
                                activeStroke = activeStroke?.copy(points = points.toList())

                                if (!change.pressed) break
                            }
                        } finally {
                            scrollGate.drawing = false
                        }

                        val finished = activeStroke
                        activeStroke = null
                        // Accept single-point taps as tiny strokes (stylus tip taps).
                        if (finished != null && finished.points.isNotEmpty()) {
                            val stroke = if (finished.points.size == 1) {
                                val p = finished.points.first()
                                finished.copy(
                                    points = listOf(
                                        p,
                                        p.copy(x = p.x + 0.5f, y = p.y + 0.5f)
                                    )
                                )
                            } else {
                                finished
                            }
                            val latest = currentDocument
                            val maxY = stroke.points.maxOf { it.y } + InkDefaults.AutoGrowPaddingPx
                            val newHeight = maxOf(latest.canvasHeight, maxY, minHeightPx)
                            currentOnChange(
                                latest.copy(
                                    strokes = latest.strokes + stroke,
                                    canvasWidth = canvasWidth.takeIf { it > 0f }
                                        ?: latest.canvasWidth,
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
                    color = ruleColor,
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
                    color = if (stroke.isEraser) paperColor else Color(stroke.colorArgb),
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
