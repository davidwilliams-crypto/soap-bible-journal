package com.soapjournal.app.data.ink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class InkStrokeTest {
    @Test
    fun strokeStoresPointsAndWidth() {
        val stroke = InkStroke(
            points = listOf(
                InkPoint(0f, 0f),
                InkPoint(10f, 10f),
                InkPoint(20f, 5f)
            ),
            width = 6f
        )
        assertEquals(3, stroke.points.size)
        assertEquals(6f, stroke.width)
        assertFalse(stroke.isEraser)
    }

    @Test
    fun documentDefaultsAreEmpty() {
        val doc = InkDocument()
        assertEquals(0, doc.strokes.size)
        assertEquals(1200f, doc.canvasHeight)
    }
}
