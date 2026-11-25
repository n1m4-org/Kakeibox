package com.kakeibox

import com.kakeibox.data.Category
import com.kakeibox.data.CategoryTotal
import com.kakeibox.data.Change
import com.kakeibox.data.Period
import com.kakeibox.data.PaymentMethod
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import com.kakeibox.data.YearReport
import com.kakeibox.data.categoryChanges
import com.kakeibox.data.periodTotals
import com.kakeibox.data.recentPeriods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReportTest {
    private fun tx(type: TxType, amount: Long, date: LocalDate, c: Category = Category.FOOD) =
        Transaction(type = type, category = c.name, amount = amount, date = date.toEpochDay(), payment = PaymentMethod.CASH)

    @Test
    fun recentPeriodsOldestFirst() {
        val last = Period.of(LocalDate.of(2026, 2, 10), 1)
        val ps = recentPeriods(last, 3)
        assertEquals(listOf(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)), ps.map { it.start })
    }

    @Test
    fun periodTotalsSplitByPeriod() {
        val ps = recentPeriods(Period.of(LocalDate.of(2026, 11, 30), 25), 2) // 10/25〜11/24, 11/25〜12/24
        val txs = listOf(
            tx(TxType.EXPENSE, 1_000, LocalDate.of(2026, 11, 24)),
            tx(TxType.EXPENSE, 2_000, LocalDate.of(2026, 11, 25)),
            tx(TxType.INCOME, 300_000, LocalDate.of(2026, 11, 25), Category.SALARY),
        )
        val t = periodTotals(txs, ps)
        assertEquals(1_000L, t[0].expense)
        assertEquals(0L, t[0].income)
        assertEquals(2_000L, t[1].expense)
        assertEquals(298_000L, t[1].balance)
    }

    @Test
    fun changesIncludeCategoriesMissingInEitherPeriod() {
        val cur = listOf(CategoryTotal(Category.FOOD, 12_000, 1f))
        val prev = listOf(CategoryTotal(Category.FOOD, 10_000, 0.5f), CategoryTotal(Category.HOBBY, 10_000, 0.5f))
        val ch = categoryChanges(cur, prev)
        assertEquals(2_000L, ch[Category.FOOD]!!.diff)
        assertEquals(0.2f, ch[Category.FOOD]!!.rate!!, 1e-6f)
        assertEquals(-10_000L, ch[Category.HOBBY]!!.diff)
    }

    @Test
    fun rateIsNullWhenPreviousIsZero() = assertNull(Change(500, 0).rate)

    @Test
    fun yearReportAveragesOverElapsedMonths() {
        val txs = listOf(
            tx(TxType.EXPENSE, 30_000, LocalDate.of(2026, 1, 5)),
            tx(TxType.EXPENSE, 60_000, LocalDate.of(2026, 3, 5)),
            tx(TxType.EXPENSE, 99_999, LocalDate.of(2025, 12, 31)), // 対象外
        )
        val r = YearReport.build(2026, 1, txs, today = LocalDate.of(2026, 3, 20))
        assertEquals(90_000L, r.summary.expense)
        assertEquals(3, r.elapsedMonths)
        assertEquals(30_000L, r.averageExpense)
        assertEquals(12, r.months.size)
        assertEquals(60_000L, r.months[2].expense)
    }

    @Test
    fun yearReportWithStartDayUsesPeriodsStartingInYear() {
        // 25日始まり：2026年の1月度 = 2026/1/25〜2/24
        val txs = listOf(
            tx(TxType.EXPENSE, 1_000, LocalDate.of(2026, 1, 24)), // 2025年12月度 → 対象外
            tx(TxType.EXPENSE, 2_000, LocalDate.of(2026, 1, 25)),
        )
        val r = YearReport.build(2026, 25, txs, today = LocalDate.of(2027, 1, 1))
        assertEquals(2_000L, r.summary.expense)
        assertEquals(12, r.elapsedMonths)
    }
}
