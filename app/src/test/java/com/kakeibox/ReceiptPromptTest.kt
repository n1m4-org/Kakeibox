package com.kakeibox

import com.kakeibox.data.PaymentMethod
import com.kakeibox.receipt.CategoryOption
import com.kakeibox.receipt.ReceiptLine
import com.kakeibox.receipt.ReceiptPrompt
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReceiptPromptTest {
    private val cats = listOf(CategoryOption("FOOD", "食費"), CategoryOption("DAILY", "日用品"), CategoryOption("OTHER_EXPENSE", "その他"))
    private val keys = cats.map { it.key }.toSet()
    private val today = LocalDate.of(2026, 11, 28)
    private fun parse(json: String) = ReceiptPrompt.parse(json, today, keys, "OTHER_EXPENSE")

    @Test
    fun schema_isValidJsonWithCategoryEnum() {
        val s = JSONObject(ReceiptPrompt.schema(cats))
        assertEquals("object", s.getString("type"))
        assertFalse(s.getBoolean("additionalProperties"))
        val group = s.getJSONObject("properties").getJSONObject("groups").getJSONObject("items")
        assertEquals(listOf("FOOD", "DAILY", "OTHER_EXPENSE"), group.getJSONObject("properties").getJSONObject("category").getJSONArray("enum").toList())
        assertEquals(listOf("category", "amount", "items"), group.getJSONArray("required").toList())
        assertTrue(s.getJSONObject("properties").getJSONObject("payment").getJSONArray("enum").toList().contains("UNKNOWN"))
    }

    @Test
    fun system_listsCategoriesAndToday() {
        val text = ReceiptPrompt.system(cats, today)
        assertTrue(text.contains("2026-11-28"))
        assertTrue(text.contains("- DAILY: 日用品"))
    }

    @Test
    fun parse_normalReceipt() {
        val d = parse("""{"store":"スーパー山田","date":"2026-11-27","payment":"EMONEY","total":2780,
            "groups":[{"category":"FOOD","amount":2300,"items":["牛乳","パン"]},{"category":"DAILY","amount":480,"items":["洗剤"]}]}""")
        assertEquals("スーパー山田", d.store)
        assertEquals(LocalDate.of(2026, 11, 27), d.date)
        assertEquals(PaymentMethod.EMONEY, d.payment)
        assertEquals(listOf(ReceiptLine("FOOD", 2300, listOf("牛乳", "パン")), ReceiptLine("DAILY", 480, listOf("洗剤"))), d.lines)
        assertFalse(d.mismatch)
    }

    @Test
    fun parse_unknownCategoryGoesToOther_andMerges() {
        val d = parse("""{"store":"","date":"","payment":"UNKNOWN","total":0,
            "groups":[{"category":"PETS","amount":500,"items":[]},{"category":"OTHER_EXPENSE","amount":100,"items":["袋"]}]}""")
        assertEquals(listOf(ReceiptLine("OTHER_EXPENSE", 600, listOf("袋"))), d.lines)
        assertNull(d.payment)
        assertEquals(today, d.date)
    }

    @Test
    fun parse_dropsNonPositive_andFutureDateIsToday() {
        val d = parse("""{"store":"a","date":"2027-01-05","payment":"CASH","total":300,
            "groups":[{"category":"FOOD","amount":300,"items":[]},{"category":"DAILY","amount":0,"items":[]},{"category":"DAILY","amount":-50,"items":[]}]}""")
        assertEquals(listOf(ReceiptLine("FOOD", 300)), d.lines)
        assertEquals(today, d.date)
    }

    @Test
    fun parse_totalOnlyBecomesOtherLine() {
        val d = parse("""{"store":"a","date":"bad","payment":"CASH","total":1200,"groups":[]}""")
        assertEquals(listOf(ReceiptLine("OTHER_EXPENSE", 1200)), d.lines)
        assertEquals(today, d.date)
    }

    @Test
    fun parse_detectsMismatch() {
        val d = parse("""{"store":"a","date":"2026-11-01","payment":"CASH","total":1000,
            "groups":[{"category":"FOOD","amount":900,"items":[]}]}""")
        assertTrue(d.mismatch)
        assertEquals(900, d.linesSum)
    }

    @Test
    fun memo_storeAndItems() {
        assertEquals("スーパー山田（牛乳・パン・卵…）", ReceiptPrompt.memo("スーパー山田", listOf("牛乳", "パン", "卵", "米")))
        assertEquals("スーパー山田", ReceiptPrompt.memo("スーパー山田", emptyList()))
        assertEquals("牛乳", ReceiptPrompt.memo("", listOf("牛乳")))
        assertEquals(100, ReceiptPrompt.memo("あ".repeat(150), emptyList()).length)
    }

    private fun org.json.JSONArray.toList(): List<Any> = (0 until length()).map { get(it) }
}
