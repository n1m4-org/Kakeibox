package com.kakeibox

import com.kakeibox.data.Category
import com.kakeibox.data.Csv
import com.kakeibox.data.PaymentMethod
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvTest {
    private val d = LocalDate.of(2026, 11, 25)
    private val txs = listOf(
        Transaction(id = 1, type = TxType.EXPENSE, category = Category.FOOD.name, amount = 800, date = d.toEpochDay(), memo = "ランチ, \"社食\"", payment = PaymentMethod.EMONEY),
        Transaction(id = 2, type = TxType.INCOME, category = Category.OTHER_INCOME.name, amount = 5_000, date = d.toEpochDay(), memo = "2行\nメモ", payment = PaymentMethod.BANK),
        Transaction(id = 3, type = TxType.EXPENSE, category = Category.OTHER_EXPENSE.name, amount = 300, date = d.minusDays(1).toEpochDay()),
    )

    @Test
    fun exportHasBomHeaderAndEscapes() {
        val csv = Csv.export(txs)
        assertTrue(csv.startsWith("﻿日付,種別,カテゴリ,金額,支払い方法,メモ\n"))
        assertTrue(csv.contains("2026-11-25,支出,食費,800,電子マネー,\"ランチ, \"\"社食\"\"\""))
        // 日付順
        assertTrue(csv.indexOf("2026-11-24") < csv.indexOf("2026-11-25"))
    }

    @Test
    fun roundTrip() {
        val result = Csv.import(Csv.export(txs))
        assertEquals(emptyList<Int>(), result.skippedLines)
        val expected = txs.sortedBy { it.date }.map { it.copy(id = 0) }
        assertEquals(expected, result.transactions)
        // 「その他」は種別で収入／支出を区別
        assertEquals(Category.OTHER_INCOME.name, result.transactions.first { it.type == TxType.INCOME }.category)
    }

    @Test
    fun lenientFormatsAndSkippedLines() {
        val text = """
            日付,種別,カテゴリ,金額
            2026/11/3,支出,交通費,"1,200"
            2026-11-04,EXPENSE,FOOD,¥500,QR,コンビニ
            2026-13-01,支出,食費,100
            2026-11-05,支出,存在しない,100
            2026-11-06,支出,食費,0
        """.trimIndent()
        val r = Csv.import(text)
        assertEquals(2, r.transactions.size)
        assertEquals(1_200L, r.transactions[0].amount)
        assertEquals(PaymentMethod.CASH, r.transactions[0].payment)
        assertEquals(PaymentMethod.QR, r.transactions[1].payment)
        assertEquals(listOf(4, 5, 6), r.skippedLines)
    }
}
