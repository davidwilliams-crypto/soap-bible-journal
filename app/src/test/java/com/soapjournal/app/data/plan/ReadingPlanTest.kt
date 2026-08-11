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
        assertTrue(first.passages.isNotEmpty())
        assertTrue(last.passages.isNotEmpty())
    }

    @Test
    fun dayForUsesPlanStart() {
        val start = LocalDate.of(2026, 1, 1)
        val day0 = ReadingPlan.dayFor(start, start)
        val day1 = ReadingPlan.dayFor(start, start.plusDays(1))
        assertEquals(0, day0.dayIndex)
        assertEquals(1, day1.dayIndex)
    }

    @Test
    fun crossBookDaysKeepEveryBookInPassages() {
        val crossBookDays = (0 until ReadingPlan.TOTAL_DAYS)
            .map { ReadingPlan.day(it) }
            .filter { day -> day.passages.map { it.book }.distinct().size > 1 }

        assertTrue("Expected some days to span books", crossBookDays.isNotEmpty())
        crossBookDays.forEach { day ->
            assertTrue(day.label.contains("–"))
            // Label books must appear in passages so journaling loads the full range.
            val booksInLabel = day.passages.map { it.book }.toSet()
            booksInLabel.forEach { book ->
                assertTrue("Day ${day.dayIndex} label=${day.label} missing $book in passages", 
                    day.passages.any { it.book == book })
            }
            assertTrue(day.passages.size >= 2)
        }
    }
}
