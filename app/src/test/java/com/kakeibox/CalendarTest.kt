package com.kakeibox

import com.kakeibox.data.Category
import com.kakeibox.data.DayTotals
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import com.kakeibox.data.calendarCells
import com.kakeibox.data.dailyTotals
import com.kakeibox.ui.compactYen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarTest {
    @Test
    fun cellsStartOnSundayAndPadToWeeks() {
        // 2026年11月1日は日曜
        val nov = calendarCells(YearMonth.of(2026, 11))
        assertEquals(LocalDate.of(2026, 11, 1), nov[0])
        assertEquals(35, nov.size)
        // 2026年12月1日は火曜 → 先頭2マス空き
        val dec = calendarCells(YearMonth.of(2026, 12))
        assertNull(dec[0]); assertNull(dec[1])
        assertEquals(LocalDate.of(2026, 12, 1), dec[2])
        assertEquals(0, dec.size % 7)
    }

    @Test
    fun dailyTotalsSplitIncomeAndExpense() {
        val d = LocalDate.of(2026, 11, 25)
        val t = dailyTotals(listOf(
            Transaction(type = TxType.EXPENSE, category = Category.FOOD.name, amount = 800, date = d.toEpochDay()),
            Transaction(type = TxType.EXPENSE, category = Category.FOOD.name, amount = 200, date = d.toEpochDay()),
            Transaction(type = TxType.INCOME, category = Category.SALARY.name, amount = 5_000, date = d.toEpochDay()),
        ))
        assertEquals(DayTotals(income = 5_000, expense = 1_000), t[d])
    }

    @Test
    fun compactYenFormats() {
        assertEquals("3,500", compactYen(3_500))
        assertEquals("1万", compactYen(10_000))
        assertEquals("1.2万", compactYen(12_345))
        assertEquals("25万", compactYen(250_000))
        assertEquals("125万", compactYen(1_250_000))
    }
}
