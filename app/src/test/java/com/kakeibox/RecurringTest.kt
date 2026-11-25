package com.kakeibox

import com.kakeibox.data.Category
import com.kakeibox.data.PaymentMethod
import com.kakeibox.data.Recurring
import com.kakeibox.data.TxType
import com.kakeibox.data.dueDates
import com.kakeibox.data.nextDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurringTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private fun rule(day: Int, start: LocalDate, processed: LocalDate? = null, enabled: Boolean = true) = Recurring(
        name = "家賃", type = TxType.EXPENSE, category = Category.HOUSING.name, amount = 80_000,
        payment = PaymentMethod.BANK, dayOfMonth = day, startDate = start.toEpochDay(),
        processedUntil = processed?.toEpochDay(), enabled = enabled,
    )

    @Test
    fun notDueBeforeDay() = assertEquals(emptyList<LocalDate>(), rule(27, d(2026, 11, 25)).dueDates(d(2026, 11, 26)))

    @Test
    fun dueOnTheDay() = assertEquals(listOf(d(2026, 11, 27)), rule(27, d(2026, 11, 25)).dueDates(d(2026, 11, 27)))

    @Test
    fun startDateExcludesEarlierDayInSameMonth() =
        assertEquals(emptyList<LocalDate>(), rule(10, d(2026, 11, 25)).dueDates(d(2026, 11, 30)))

    @Test
    fun catchesUpMissedMonths() {
        val r = rule(10, d(2026, 9, 1), processed = d(2026, 9, 30))
        assertEquals(listOf(d(2026, 10, 10), d(2026, 11, 10)), r.dueDates(d(2026, 11, 25)))
    }

    @Test
    fun alreadyProcessedIsNotRepeated() {
        val r = rule(10, d(2026, 9, 1), processed = d(2026, 11, 10))
        assertEquals(emptyList<LocalDate>(), r.dueDates(d(2026, 11, 25)))
    }

    @Test
    fun endOfMonthClampsToShortMonths() {
        val r = rule(31, d(2027, 1, 1))
        assertEquals(listOf(d(2027, 1, 31), d(2027, 2, 28), d(2027, 3, 31)), r.dueDates(d(2027, 3, 31)))
    }

    @Test
    fun disabledRuleProducesNothing() =
        assertEquals(emptyList<LocalDate>(), rule(1, d(2026, 1, 1), enabled = false).dueDates(d(2026, 11, 25)))

    @Test
    fun nextDateThisMonthOrNext() {
        assertEquals(d(2026, 11, 27), rule(27, d(2026, 1, 1)).nextDate(d(2026, 11, 25)))
        assertEquals(d(2026, 12, 10), rule(10, d(2026, 1, 1)).nextDate(d(2026, 11, 25)))
        assertEquals(d(2026, 11, 25), rule(25, d(2026, 1, 1)).nextDate(d(2026, 11, 25)))
        assertEquals(d(2027, 2, 28), rule(31, d(2026, 1, 1)).nextDate(d(2027, 2, 1)))
    }
}
