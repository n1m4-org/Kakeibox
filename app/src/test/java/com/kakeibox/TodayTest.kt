package com.kakeibox

import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.Category
import com.kakeibox.data.DailyBudgetSetting
import com.kakeibox.data.DayCategoryTotal
import com.kakeibox.data.Period
import com.kakeibox.data.Week
import com.kakeibox.data.millisUntilNextMidnight
import com.kakeibox.data.todayStatusesFlow
import com.kakeibox.data.weeklyStatusesFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TodayTest {
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)

    @Test
    fun millisUntilMidnight() {
        assertEquals(60_000L, millisUntilNextMidnight(LocalDateTime.of(2026, 11, 25, 23, 59)))
        assertEquals(86_400_000L, millisUntilNextMidnight(LocalDateTime.of(2026, 11, 26, 0, 0)))
        assertEquals(1_000L, millisUntilNextMidnight(LocalDateTime.of(2026, 12, 31, 23, 59, 59)))
        assertEquals(43_200_000L, millisUntilNextMidnight(LocalDateTime.of(2026, 11, 30, 12, 0)))
    }

    @Test
    fun todayStatusesCarryOverWhenDateChanges() = runTest(UnconfinedTestDispatcher()) {
        val today = MutableStateFlow(d(11, 25))
        val settings = flowOf(mapOf<BudgetTarget, DailyBudgetSetting>(BudgetTarget.All to DailyBudgetSetting(2_000, d(11, 25).toEpochDay())))
        // 11/25 に 1,500円使った
        val spent = listOf(DayCategoryTotal(d(11, 25).toEpochDay(), Category.FOOD.name, 1_500))
        val results = mutableListOf<Pair<Long, Long>>()
        val job = launch {
            todayStatusesFlow(today, flowOf(1), settings) { flowOf(spent) }
                .collect { list -> results += list.single().status.let { it.carryover to it.todayBudget } }
        }
        // 0:00 を過ぎて 11/26 に → 前日の余り 500円が繰り越される
        today.value = d(11, 26)
        job.cancel()
        assertEquals(listOf(0L to 2_000L, 500L to 2_500L), results)
    }

    @Test
    fun todayStatusesSwitchPeriodAtStartDay() = runTest(UnconfinedTestDispatcher()) {
        val today = MutableStateFlow(d(11, 24))
        val periods = mutableListOf<Period>()
        val job = launch {
            todayStatusesFlow(today, flowOf(25), flowOf(emptyMap())) { p -> periods += p; flowOf(emptyList()) }.collect {}
        }
        today.value = d(11, 24) // 同じ日付では再購読しない
        today.value = d(11, 25) // 締め日の翌日（開始日）で新しい期間に
        job.cancel()
        assertEquals(listOf(d(10, 25), d(11, 25)), periods.map { it.start })
    }

    @Test
    fun weeklyStatusesSwitchWeekOnMonday() = runTest(UnconfinedTestDispatcher()) {
        val today = MutableStateFlow(d(11, 29)) // 日曜
        val weeks = mutableListOf<Week>()
        val job = launch {
            weeklyStatusesFlow(today, flowOf(emptyMap())) { w -> weeks += w; flowOf(emptyList()) }.collect {}
        }
        today.value = d(11, 30) // 月曜 → 新しい週
        job.cancel()
        assertEquals(listOf(d(11, 23), d(11, 30)), weeks.map { it.start })
    }
}
