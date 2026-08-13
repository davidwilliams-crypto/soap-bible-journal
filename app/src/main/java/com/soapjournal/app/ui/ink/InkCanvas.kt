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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.soapjournal.app.data.ink.shouldKeepInkPoint
import com.soapjournal.app.ui.theme.LocalJournalSurfaces

enum class InkTool {
    PEN,
    ERASER,
    PAN
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
    var viewportHeight by remember { mutableFloatStateOf(0f) }
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
                return if (scrollGate.drawing) available else Offset.Zero
            }
        }
    }

    val livePoints = remember { mutableListOf<InkPoint>() }
    var liveRevision by remember { mutableIntStateOf(0) }
    var liveIsEraser by remember { mutableStateOf(false) }
    var liveWidth by remember { mutableFloatStateOf(strokeWidth) }
    var liveColorArgb by remember { mutableIntStateOf(resolvedInk.toArgb()) }

    val pathCache = remember { LinkedHashMap<InkStroke, Path>() }
    val committedPaths = remember(document.strokes) {
        val keep = document.strokes.toHashSet()
        pathCache.keys.retainAll(keep)
        document.strokes.map { stroke ->
            pathCache.getOrPut(stroke) { stroke.toPath() }
        }
    }

    Box(
        modifier = modifier
            .background(paperColor)
            .onSizeChanged { size ->
                viewportHeight = size.height.toFloat()
                canvasWidth = size.width.toFloat()
            }
            .nestedScroll(blockScrollWhileDrawing)
            .verticalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeightDp)
                .pointerInput(tool, strokeWidth, resolvedInk, stylusOnly) {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        val pointerType = down.type
                        val isStylusLike = pointerType == PointerType.Stylus ||
                            pointerType == PointerType.Eraser ||
                            pointerType == PointerType.Mouse
                        val isTouch = pointerType == PointerType.Touch
                        if (stylusOnly && !isStylusLike) {
                            return@awaitEachGesture
                        }
                        if (!isStylusLike && !isTouch) {
                            return@awaitEachGesture
                        }
                        // Pan with finger / touch so the paper can move; stylus still writes.
                        if (tool == InkTool.PAN && !isStylusLike) {
                            return@awaitEachGesture
                        }

                        down.consume()
                        scrollGate.drawing = true

                        val first = InkPoint(
                            x = down.position.x,
                            y = down.position.y,
                            pressure = down.pressure.takeIf { it > 0f }?.coerceIn(0.05f, 1f) ?: 1f,
                            timestamp = System.currentTimeMillis()
                        )
                        val isEraser = tool == InkTool.ERASER || pointerType == PointerType.Eraser
                        livePoints.clear()
                        livePoints += first
                        liveIsEraser = isEraser
                        liveWidth = if (isEraser) strokeWidth * 3f else strokeWidth
                        liveColorArgb = resolvedInk.toArgb()
                        liveRevision++

                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull {
                                    it.id == down.id
                                } ?: event.changes.firstOrNull() ?: break

                                change.consume()
                                val next = InkPoint(
                                    x = change.position.x,
                                    y = change.position.y,
                                    pressure = change.pressure.takeIf { it > 0f }
                                        ?.coerceIn(0.05f, 1f) ?: 1f,
                                    timestamp = System.currentTimeMillis()
                                )
                                val prev = livePoints.lastOrNull()
                                val keep = prev == null ||
                                    !change.pressed ||
                                    shouldKeepInkPoint(prev, next)
                                if (keep) {
                                    livePoints += next
                                    liveRevision++
                                }

                                val viewH = viewportHeight
                                if (viewH > 0f && change.position.y > scrollState.value + viewH - 160f) {
                                    val dest = (change.position.y - viewH + 200f).toInt()
                                        .coerceIn(0, scrollState.maxValue)
                                    val delta = dest - scrollState.value
                                    if (delta > 0) {
                                        scrollState.dispatchRawDelta(delta.toFloat())
                                    }
                                }

                                if (!change.pressed) break
                            }
                        } finally {
                            scrollGate.drawing = false
                        }

                        val finishedPoints = livePoints.toList()
                        livePoints.clear()
                        liveRevision = 0
                        if (finishedPoints.isNotEmpty()) {
                            val points = if (finishedPoints.size == 1) {
                                val p = finishedPoints.first()
                                listOf(p, p.copy(x = p.x + 0.5f, y = p.y + 0.5f))
                            } else {
                                finishedPoints
                            }
                            val stroke = InkStroke(
                                points = points,
                                colorArgb = liveColorArgb,
                                width = liveWidth,
                                isEraser = liveIsEraser
                            )
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
            val revision = liveRevision
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

            committedPaths.forEachIndexed { index, path ->
                val stroke = document.strokes[index]
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

            if (revision > 0 && livePoints.size >= 2) {
                val active = InkStroke(
                    points = livePoints,
                    colorArgb = liveColorArgb,
                    width = liveWidth,
                    isEraser = liveIsEraser
                )
                drawPath(
                    path = active.toPath(),
                    color = if (active.isEraser) paperColor else Color(active.colorArgb),
                    style = Stroke(
                        width = active.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (revision > 0 && livePoints.size == 1) {
                val p = livePoints.first()
                drawCircle(
                    color = if (liveIsEraser) paperColor else Color(liveColorArgb),
                    radius = liveWidth / 2f,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}
