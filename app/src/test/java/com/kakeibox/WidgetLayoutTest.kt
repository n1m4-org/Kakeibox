package com.kakeibox

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.kakeibox.widget.BudgetWidgetLayout
import com.kakeibox.widget.StackedBars
import com.kakeibox.widget.stackedBars
import com.kakeibox.widget.budgetWidgetLayout
import com.kakeibox.widget.rowShowsLabel
import com.kakeibox.widget.RowFit
import com.kakeibox.widget.fitRow
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutTest {
    private fun layout(w: Int, h: Int) = budgetWidgetLayout(DpSize(w.dp, h.dp))

    @Test
    fun oneRow_narrowIsRow_wideIsRowWide() {
        assertEquals(BudgetWidgetLayout.ROW, layout(110, 40))   // 2×1
        assertEquals(BudgetWidgetLayout.ROW, layout(235, 70))   // 3×1
        assertEquals(BudgetWidgetLayout.ROW_WIDE, layout(250, 40))
        assertEquals(BudgetWidgetLayout.ROW_WIDE, layout(320, 70)) // 4×1
    }

    @Test
    fun oneRow_labelFromThreeCells() {
        assertEquals(false, DpSize(110.dp, 40.dp).rowShowsLabel()) // 2×1（最小）
        assertEquals(false, DpSize(150.dp, 70.dp).rowShowsLabel()) // 2×1
        assertEquals(true, DpSize(180.dp, 40.dp).rowShowsLabel())  // 3×1（最小）
        assertEquals(true, DpSize(235.dp, 70.dp).rowShowsLabel())  // 3×1
        assertEquals(true, DpSize(320.dp, 70.dp).rowShowsLabel())  // 4×1
    }

    @Test
    fun squareIsDonut_tallIsLabeledDonut() {
        assertEquals(BudgetWidgetLayout.DONUT, layout(110, 110))   // 2×2（最小）
        assertEquals(BudgetWidgetLayout.DONUT, layout(150, 150))   // 2×2
        assertEquals(BudgetWidgetLayout.DONUT, layout(235, 230))   // 3×3
        assertEquals(BudgetWidgetLayout.DONUT_LABELED, layout(110, 180)) // 2×3（最小）
        assertEquals(BudgetWidgetLayout.DONUT_LABELED, layout(150, 230)) // 2×3
    }

    @Test
    fun wideTwoRowsIsCard() {
        assertEquals(BudgetWidgetLayout.CARD, layout(180, 110))  // 3×2
        assertEquals(BudgetWidgetLayout.CARD, layout(235, 150))  // 3×2
        assertEquals(BudgetWidgetLayout.CARD, layout(150, 95))   // 低めの2段
    }

    @Test
    fun fourWideTwoOrMoreTallIsStack() {
        assertEquals(BudgetWidgetLayout.STACK, layout(250, 110)) // 4×2（最小）
        assertEquals(BudgetWidgetLayout.STACK, layout(320, 150)) // 4×2
        assertEquals(BudgetWidgetLayout.STACK, layout(250, 180)) // 4×3（最小）
        assertEquals(BudgetWidgetLayout.STACK, layout(320, 230)) // 4×3
        assertEquals(BudgetWidgetLayout.STACK, layout(400, 400)) // 5×5
        assertEquals(BudgetWidgetLayout.DONUT, layout(235, 230)) // 3×3 はドーナツのまま
    }

    @Test
    fun stackedBars_withinBudgetIsOneBar() {
        assertEquals(StackedBars(listOf(0f), 0), stackedBars(0, 1_000, 5))
        assertEquals(StackedBars(listOf(0.4f), 0), stackedBars(400, 1_000, 5))
        assertEquals(StackedBars(listOf(1f), 0), stackedBars(1_000, 1_000, 5))
    }

    @Test
    fun stackedBars_overspendAddsBars() {
        assertEquals(StackedBars(listOf(1f, 0.5f), 0), stackedBars(1_500, 1_000, 5))
        assertEquals(StackedBars(listOf(1f, 1f, 1f), 0), stackedBars(3_000, 1_000, 5))
        assertEquals(StackedBars(listOf(1f, 1f, 1f, 1f, 1f), 0), stackedBars(5_000, 1_000, 5))
    }

    @Test
    fun stackedBars_tooManyShowsMoreOnLastBar() {
        // 12.95本分・5本まで → 4本＋「×9」（5〜13本目）
        assertEquals(StackedBars(listOf(1f, 1f, 1f, 1f, 1f), 9), stackedBars(12_950, 1_000, 5))
        assertEquals(StackedBars(listOf(1f), 3), stackedBars(3_000, 1_000, 1))
    }

    @Test
    fun stackedBars_noBudgetIsFull() {
        assertEquals(StackedBars(listOf(1f), 0), stackedBars(500, 0, 5))
    }

    @Test
    fun fitRow_prefersLabel() {
        // 4×1（320dp）：全部入る
        assertEquals(RowFit("食費", true), fitRow(320f - 94f, "食費", "¥800", " / 1,200"))
        // 3×1（180dp）：「/ 予算」を省いて名前を残す
        assertEquals(RowFit("食費", false), fitRow(180f - 94f, "食費", "¥800", " / 1,200"))
        // 3×1（180dp）で名前が長い：名前を詰める
        assertEquals(RowFit("日用…", false), fitRow(180f - 94f, "日用品・消耗品", "¥800", " / 1,200"))
        // 3×1（235dp）：マイナスの金額でも全部入る
        assertEquals(RowFit("食費", true), fitRow(235f - 94f, "食費", "-¥12,150", " / 800"))
        // 2文字の名前は詰めても短くならないのでそのまま
        assertEquals("食費", fitRow(10f, "食費", "-¥123,456", " / 800").label)
        // とても狭い長い名前は1文字＋…
        assertEquals("日…", fitRow(10f, "日用品・消耗品", "-¥123,456", " / 800").label)
    }
}
