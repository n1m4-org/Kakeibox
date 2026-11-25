package com.kakeibox

import com.kakeibox.data.Period
import com.kakeibox.data.computeDailyBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PeriodTest {
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)

    @Test
    fun calendarMonthWhenStartDayIsOne() {
        val p = Period.of(d(11, 25), 1)
        assertEquals(d(11, 1), p.start)
        assertEquals(d(11, 30), p.endInclusive)
        assertEquals(30, p.days)
        assertEquals("2026年11月", p.label)
    }

    @Test
    fun dateOnOrAfterStartDayBelongsToThisMonthsPeriod() {
        val p = Period.of(d(11, 25), 25)
        assertEquals(d(11, 25), p.start)
        assertEquals(d(12, 24), p.endInclusive)
        assertEquals("2026/11/25〜12/24", p.label)
    }

    @Test
    fun dateBeforeStartDayBelongsToPreviousMonthsPeriod() {
        val p = Period.of(d(11, 24), 25)
        assertEquals(d(10, 25), p.start)
        assertEquals(d(11, 24), p.endInclusive)
    }

    @Test
    fun crossesYearBoundary() {
        val p = Period.of(d(1, 10), 25)
        assertEquals(LocalDate.of(2025, 12, 25), p.start)
        assertEquals(d(1, 24), p.endInclusive)
    }

    @Test
    fun shiftAndContains() {
        val p = Period.of(d(11, 30), 25)
        assertEquals(d(12, 25), p.next().start)
        assertEquals(d(10, 25), p.previous().start)
        assertTrue(d(12, 24) in p)
        assertFalse(d(12, 25) in p)
    }

    @Test
    fun februaryPeriodLength() {
        // 2/28始まり → 3/27 まで（28日）
        assertEquals(28, Period.of(d(3, 1), 28).days)
    }

    @Test
    fun dailyBudgetCarryoverResetsAtPeriodStart() {
        val today = d(11, 27)
        val p = Period.of(today, 25)
        // 11/25, 11/26 の2日分だけ繰り越し（11/1からではない）
        val s = computeDailyBudget(1_000, d(9, 1), today, emptyMap(), p.start)!!
        assertEquals(2_000L, s.carryover)
    }
}
