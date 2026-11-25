package com.kakeibox

import com.kakeibox.data.computeDailyBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DailyBudgetTest {
    private val today = LocalDate.of(2026, 11, 5)
    private val monthStart = LocalDate.of(2026, 11, 1)
    private fun day(d: Int) = LocalDate.of(2026, 11, d).toEpochDay()

    @Test
    fun unsetReturnsNull() = assertNull(computeDailyBudget(0, monthStart, today, emptyMap()))

    @Test
    fun unspentDaysCarryOver() {
        // 1〜4日は支出なし → 2,000×4 が繰り越し
        val s = computeDailyBudget(2_000, monthStart, today, emptyMap())!!
        assertEquals(8_000L, s.carryover)
        assertEquals(10_000L, s.todayBudget)
    }

    @Test
    fun overspendReducesToday() {
        val spent = mapOf(day(1) to 2_000L, day(2) to 2_000L, day(3) to 2_000L, day(4) to 3_500L)
        val s = computeDailyBudget(2_000, monthStart, today, spent)!!
        assertEquals(-1_500L, s.carryover)
        assertEquals(500L, s.todayBudget)
    }

    @Test
    fun todaySpendingAffectsRemainingNotCarryover() {
        val spent = mapOf(day(1) to 2_000L, day(2) to 2_000L, day(3) to 2_000L, day(4) to 2_000L, day(5) to 800L)
        val s = computeDailyBudget(2_000, monthStart, today, spent)!!
        assertEquals(0L, s.carryover)
        assertEquals(800L, s.spentToday)
        assertEquals(1_200L, s.remaining)
        assertEquals(0.4f, s.ratio, 1e-6f)
    }

    @Test
    fun resetsAtMonthStart() {
        // 先月から設定していても、繰り越しは今月1日から
        val s = computeDailyBudget(1_000, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 1), emptyMap())!!
        assertEquals(0L, s.carryover)
        assertEquals(1_000L, s.todayBudget)
    }

    @Test
    fun countsOnlyFromSettingDay() {
        // 4日に設定 → 4日分の余りだけ繰り越し
        val s = computeDailyBudget(2_000, LocalDate.of(2026, 11, 4), today, emptyMap())!!
        assertEquals(2_000L, s.carryover)
    }

    @Test
    fun nonPositiveBudgetIsFullyUsed() {
        val spent = mapOf(day(4) to 5_000L)
        val s = computeDailyBudget(2_000, LocalDate.of(2026, 11, 4), today, spent)!!
        assertEquals(-1_000L, s.todayBudget)
        assertEquals(1f, s.ratio, 1e-6f)
    }
}
