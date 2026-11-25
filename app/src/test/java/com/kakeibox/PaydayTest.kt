package com.kakeibox

import com.kakeibox.data.MonthSummary
import com.kakeibox.data.nextPayday
import com.kakeibox.data.paydayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PaydayTest {
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)

    @Test
    fun thisMonthIfNotPassed() = assertEquals(d(11, 25), nextPayday(d(11, 20), 25, true))

    @Test
    fun todayIsPayday() = assertEquals(d(11, 25), nextPayday(d(11, 25), 25, true))

    @Test
    fun nextMonthAfterPayday() = assertEquals(d(12, 25), nextPayday(d(11, 26), 25, true))

    @Test
    fun weekendShiftsToFriday() {
        // 2026/10/25 は日曜 → 10/23(金)
        assertEquals(d(10, 23), nextPayday(d(10, 20), 25, true))
        assertEquals(d(10, 25), nextPayday(d(10, 20), 25, false))
        // 10/24(土) は 10/23 を過ぎているので翌月
        assertEquals(d(11, 25), nextPayday(d(10, 24), 25, true))
    }

    @Test
    fun endOfMonth() = assertEquals(d(2, 27, 2026), nextPayday(d(2, 10, 2026), 31, true)) // 2/28は土曜

    @Test
    fun perDayFromMonthlyBudget() {
        val s = paydayStatus(d(11, 20), 25, true, 100_000, MonthSummary(0, 90_000, emptyList(), emptyList()))
        assertEquals(5, s.daysLeft)
        assertEquals(10_000L, s.remaining)
        assertEquals(2_000L, s.perDay)
        assertEquals("今月の予算の残り", s.basis)
    }

    @Test
    fun perDayFromIncomeWhenNoBudget() {
        val s = paydayStatus(d(11, 20), 25, true, null, MonthSummary(250_000, 200_000, emptyList(), emptyList()))
        assertEquals(10_000L, s.perDay)
    }

    @Test
    fun noBasisWithoutBudgetOrIncome() =
        assertNull(paydayStatus(d(11, 20), 25, true, null, MonthSummary.EMPTY).perDay)
}
