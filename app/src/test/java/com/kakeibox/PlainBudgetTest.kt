package com.kakeibox

import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.Category
import com.kakeibox.data.DayCategoryTotal
import com.kakeibox.data.Week
import com.kakeibox.data.plainBudgetStatuses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlainBudgetTest {
    private fun d(day: Int) = LocalDate.of(2026, 11, day)

    @Test
    fun statusesForConfiguredTargetsOnly() {
        val spent = listOf(
            DayCategoryTotal(d(23).toEpochDay(), Category.FOOD.name, 4_000),
            DayCategoryTotal(d(24).toEpochDay(), Category.FOOD.name, 2_000),
            DayCategoryTotal(d(24).toEpochDay(), Category.HOBBY.name, 9_000),
        )
        val result = plainBudgetStatuses(
            mapOf(BudgetTarget.Of(Category.FOOD) to 10_000L, BudgetTarget.All to 20_000L),
            spent,
        )
        assertEquals(listOf(BudgetTarget.All, BudgetTarget.Of(Category.FOOD)), result.map { it.target })
        assertEquals(15_000L, result[0].spent)
        assertEquals(5_000L, result[0].remaining)
        assertEquals(6_000L, result[1].spent)
        assertEquals(0.6f, result[1].ratio, 1e-6f)
    }

    @Test
    fun weekStartsOnMonday() {
        // 2026/11/25 は水曜
        val w = Week.of(d(25))
        assertEquals(d(23), w.start)
        assertEquals(d(29), w.endInclusive)
        assertTrue(d(29) in w)
        assertFalse(d(30) in w)
        assertEquals(5, w.daysLeft(d(25)))
        assertEquals(d(23), Week.of(d(23)).start)
        assertEquals(d(23), Week.of(d(29)).start)
    }
}
