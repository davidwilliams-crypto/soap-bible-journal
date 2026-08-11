package com.soapjournal.app.data.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReadingPlanTest {
    @Test
    fun planHasSevenHundredThirtyDays() {
        assertEquals(730, ReadingPlan.TOTAL_DAYS)
        val first = ReadingPlan.day(0)
        val last = ReadingPlan.day(729)
        assertTrue(first.label.isNotBlank())
        assertTrue(last.label.isNotBlank())
    }

    @Test
    fun dayForUsesPlanStart() {
        val start = LocalDate.of(2026, 1, 1)
        val day0 = ReadingPlan.dayFor(start, start)
        val day1 = ReadingPlan.dayFor(start, start.plusDays(1))
        assertEquals(0, day0.dayIndex)
        assertEquals(1, day1.dayIndex)
    }
}
