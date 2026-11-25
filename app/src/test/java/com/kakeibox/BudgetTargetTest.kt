package com.kakeibox

import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.Category
import com.kakeibox.data.DailyBudgetSetting
import com.kakeibox.data.DayCategoryTotal
import com.kakeibox.data.computeTodayStatuses
import com.kakeibox.data.monthlyBudget
import com.kakeibox.data.spentByDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetTargetTest {
    private fun day(d: Int) = LocalDate.of(2026, 11, d).toEpochDay()
    private val spent = listOf(
        DayCategoryTotal(day(1), Category.FOOD.name, 1_200),
        DayCategoryTotal(day(1), Category.DAILY.name, 300),
        DayCategoryTotal(day(2), Category.FOOD.name, 800),
        DayCategoryTotal(day(2), Category.HOBBY.name, 5_000),
    )

    @Test
    fun keyRoundTrip() {
        BudgetTarget.entries.forEach { assertEquals(it, BudgetTarget.fromKey(it.key)) }
        assertEquals(BudgetTarget.All, BudgetTarget.fromKey(null))
        assertEquals(BudgetTarget.All, BudgetTarget.fromKey("UNKNOWN"))
    }

    @Test
    fun entriesAreAllPlusExpenseCategories() {
        assertEquals(BudgetTarget.All, BudgetTarget.entries.first())
        assertEquals(12, BudgetTarget.entries.size)
    }

    @Test
    fun spentByDayForAllSumsEveryCategory() =
        assertEquals(mapOf(day(1) to 1_500L, day(2) to 5_800L), spent.spentByDay(BudgetTarget.All))

    @Test
    fun spentByDayForCategoryFilters() =
        assertEquals(mapOf(day(1) to 1_200L, day(2) to 800L), spent.spentByDay(BudgetTarget.Of(Category.FOOD)))

    @Test
    fun monthlyBudgetUsesDaysInMonth() {
        assertEquals(60_000L, monthlyBudget(2_000, YearMonth.of(2026, 11)))
        assertEquals(62_000L, monthlyBudget(2_000, YearMonth.of(2026, 12)))
        assertEquals(56_000L, monthlyBudget(2_000, YearMonth.of(2026, 2)))
        assertEquals(58_000L, monthlyBudget(2_000, YearMonth.of(2028, 2)))
    }

    @Test
    fun todayStatusesOnlyForConfiguredTargetsInOrder() {
        val since = day(1)
        val settings = mapOf(
            BudgetTarget.Of(Category.HOBBY) to DailyBudgetSetting(1_000, since),
            BudgetTarget.Of(Category.FOOD) to DailyBudgetSetting(1_000, since),
            BudgetTarget.All to DailyBudgetSetting(3_000, since),
        )
        val result = computeTodayStatuses(settings, spent, LocalDate.of(2026, 11, 3))
        assertEquals(
            listOf(BudgetTarget.All, BudgetTarget.Of(Category.FOOD), BudgetTarget.Of(Category.HOBBY)),
            result.map { it.target },
        )
        // 食費: (1000-1200)+(1000-800) = 0、趣味: (1000-0)+(1000-5000) = -3000
        assertEquals(0L, result[1].status.carryover)
        assertEquals(-3_000L, result[2].status.carryover)
    }
}
