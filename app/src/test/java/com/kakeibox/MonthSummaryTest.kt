package com.kakeibox

import com.kakeibox.data.Category
import com.kakeibox.data.MonthSummary
import com.kakeibox.data.PaymentMethod
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthSummaryTest {
    private fun tx(type: TxType, c: Category, amount: Long) =
        Transaction(type = type, category = c.name, amount = amount, date = 0)

    @Test
    fun totalsAndBalance() {
        val s = MonthSummary.from(listOf(
            tx(TxType.INCOME, Category.SALARY, 250_000),
            tx(TxType.EXPENSE, Category.FOOD, 30_000),
            tx(TxType.EXPENSE, Category.FOOD, 10_000),
            tx(TxType.EXPENSE, Category.HOUSING, 60_000),
        ))
        assertEquals(250_000L, s.income)
        assertEquals(100_000L, s.expense)
        assertEquals(150_000L, s.balance)
    }

    @Test
    fun categoryBreakdownSortedWithRatio() {
        val s = MonthSummary.from(listOf(
            tx(TxType.EXPENSE, Category.FOOD, 40_000),
            tx(TxType.EXPENSE, Category.HOUSING, 60_000),
        ))
        assertEquals(listOf(Category.HOUSING, Category.FOOD), s.expenseByCategory.map { it.category })
        assertEquals(0.6f, s.expenseByCategory[0].ratio, 1e-6f)
        assertEquals(0.4f, s.expenseByCategory[1].ratio, 1e-6f)
    }

    @Test
    fun expenseByPaymentSortedDescending() {
        val s = MonthSummary.from(listOf(
            tx(TxType.EXPENSE, Category.FOOD, 1_000).copy(payment = PaymentMethod.CASH),
            tx(TxType.EXPENSE, Category.FOOD, 3_000).copy(payment = PaymentMethod.CREDIT),
            tx(TxType.EXPENSE, Category.DAILY, 500).copy(payment = PaymentMethod.CASH),
            tx(TxType.INCOME, Category.SALARY, 9_000).copy(payment = PaymentMethod.BANK),
        ))
        assertEquals(listOf(PaymentMethod.CREDIT to 3_000L, PaymentMethod.CASH to 1_500L), s.expenseByPayment)
    }

    @Test
    fun emptyListYieldsZero() {
        val s = MonthSummary.from(emptyList())
        assertEquals(0L, s.balance)
        assertEquals(0, s.expenseByCategory.size)
    }
}
