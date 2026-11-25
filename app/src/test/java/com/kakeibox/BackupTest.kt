package com.kakeibox

import com.kakeibox.data.BackupData
import com.kakeibox.data.Category
import com.kakeibox.data.PaymentMethod
import com.kakeibox.data.Recurring
import com.kakeibox.data.Template
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupTest {
    private val data = BackupData(
        transactions = listOf(
            Transaction(5, TxType.EXPENSE, Category.FOOD.name, 800, 20_000, "ランチ\n\"社食\"", PaymentMethod.QR),
            Transaction(9, TxType.INCOME, Category.SALARY.name, 250_000, 20_010, "", PaymentMethod.BANK),
        ),
        templates = listOf(Template(1, "コーヒー", TxType.EXPENSE, Category.FOOD.name, 0, PaymentMethod.EMONEY, "")),
        recurring = listOf(
            Recurring(2, "家賃", TxType.EXPENSE, Category.HOUSING.name, 80_000, PaymentMethod.BANK, "", 27, 19_000, null, true),
            Recurring(3, "サブスク", TxType.EXPENSE, Category.HOBBY.name, 990, PaymentMethod.CREDIT, "動画", 31, 19_000, 20_000, false),
        ),
        prefs = mapOf(
            "kakeibox" to mapOf("daily_budget" to 2_000L, "daily_budget_since" to 20_000L),
            "kakeibox_settings" to mapOf("theme" to "DARK", "period_start_day" to 25, "reminder" to true, "x" to 1.5f, "s" to setOf("a", "b")),
        ),
    )

    @Test
    fun roundTripPreservesEverythingIncludingPrefTypes() {
        val back = BackupData.fromJson(data.toJson())
        assertEquals(data, back)
        assertEquals(java.lang.Long::class.java, back.prefs["kakeibox"]!!["daily_budget"]!!::class.java)
        assertEquals(java.lang.Integer::class.java, back.prefs["kakeibox_settings"]!!["period_start_day"]!!::class.java)
    }

    @Test
    fun rejectsOtherFiles() {
        assertThrows(IllegalArgumentException::class.java) { BackupData.fromJson("{\"app\":\"Other\"}") }
        assertThrows(IllegalArgumentException::class.java) { BackupData.fromJson("not json") }
    }
}
