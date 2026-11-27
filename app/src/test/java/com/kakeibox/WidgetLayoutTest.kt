package com.kakeibox

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.kakeibox.widget.BudgetWidgetLayout
import com.kakeibox.widget.budgetWidgetLayout
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
        assertEquals(BudgetWidgetLayout.CARD, layout(320, 150))  // 4×2
        assertEquals(BudgetWidgetLayout.CARD, layout(150, 95))   // 低めの2段
    }
}
