package com.kakeibox

import com.kakeibox.data.Category
import com.kakeibox.data.PaymentMethod
import com.kakeibox.data.Template
import com.kakeibox.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TemplateTest {
    private val lunch = Template(name = "ランチ", type = TxType.EXPENSE, category = Category.FOOD.name,
        amount = 800, payment = PaymentMethod.EMONEY, memo = "社食")

    @Test
    fun fixedAmountTemplateIgnoresEnteredAmount() {
        val tx = lunch.toTransaction(LocalDate.of(2026, 11, 25), enteredAmount = 1234)
        assertEquals(800L, tx.amount)
        assertEquals(PaymentMethod.EMONEY, tx.payment)
        assertEquals("社食", tx.memo)
        assertEquals(LocalDate.of(2026, 11, 25).toEpochDay(), tx.date)
        assertEquals(0L, tx.id)
    }

    @Test
    fun openAmountTemplateUsesEnteredAmount() {
        val tx = lunch.copy(amount = 0).toTransaction(enteredAmount = 1234)
        assertEquals(1234L, tx.amount)
    }

    @Test
    fun fromTransactionRoundTrip() {
        val tx = lunch.toTransaction(LocalDate.of(2026, 11, 25))
        val t = Template.from(tx, "ランチ")
        assertEquals(lunch, t)
    }
}
