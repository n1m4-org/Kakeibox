package com.kakeibox

import com.kakeibox.data.Categories
import com.kakeibox.data.Category
import com.kakeibox.data.CategoryConfig
import com.kakeibox.data.TxType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryConfigTest {
    @After fun reset() = Categories.set(CategoryConfig())

    private fun keys(list: List<Category>) = list.map { it.key }

    @Test
    fun defaultsAreBuiltInInOrder() {
        val c = CategoryConfig()
        assertEquals(11, c.visible(TxType.EXPENSE).size)
        assertEquals(Category.FOOD, c.visible(TxType.EXPENSE).first())
        assertEquals(Category.OTHER_INCOME, c.visible(TxType.INCOME).last())
    }

    @Test
    fun addedCategoryGetsSequentialKeyAndAppears() {
        val c = CategoryConfig().added(" ペット ", TxType.EXPENSE, 0xFF81C784, "pets").added("投資", TxType.INCOME, 0xFF51A696, "savings")
        assertEquals(listOf("CUSTOM_1", "CUSTOM_2"), c.custom.map { it.key })
        assertEquals("ペット", c.find("CUSTOM_1")!!.label)
        assertEquals("CUSTOM_1", keys(c.visible(TxType.EXPENSE)).last())
        assertEquals("CUSTOM_2", keys(c.visible(TxType.INCOME)).last())
        assertEquals(3, c.nextId)
    }

    @Test
    fun identityIsByKey() {
        val a = Category("CUSTOM_1", "ペット", TxType.EXPENSE, 1, "pets")
        val b = a.copy(label = "犬", color = 2)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, Category.FOOD)
    }

    @Test
    fun updateChangesOnlyCustom() {
        val c = CategoryConfig().added("ペット", TxType.EXPENSE, 1, "pets").updated("CUSTOM_1", "犬", 2, "pets")
        assertEquals("犬", c.find("CUSTOM_1")!!.label)
        assertEquals(2L, c.find("CUSTOM_1")!!.color)
        assertEquals("食費", CategoryConfig().updated("FOOD", "ごはん", 1, "cake").find("FOOD")!!.label)
    }

    @Test
    fun hideAndShowButFallbackCannotBeHidden() {
        val c = CategoryConfig().withHidden(Category.CLOTHES, true).withHidden(Category.OTHER_EXPENSE, true)
        assertFalse(Category.CLOTHES in c.visible(TxType.EXPENSE))
        assertTrue(Category.CLOTHES in c.ordered(TxType.EXPENSE))
        assertTrue(Category.OTHER_EXPENSE in c.visible(TxType.EXPENSE))
        assertTrue(Category.CLOTHES in c.withHidden(Category.CLOTHES, false).visible(TxType.EXPENSE))
    }

    @Test
    fun moveUpAndDownWithinBounds() {
        val c = CategoryConfig().moved(Category.DAILY, -1)
        assertEquals(listOf("DAILY", "FOOD", "TRANSPORT"), keys(c.ordered(TxType.EXPENSE)).take(3))
        assertEquals(c, c.moved(Category.DAILY, -1))
        val last = CategoryConfig().moved(Category.OTHER_INCOME, 1)
        assertEquals(CategoryConfig().ordered(TxType.INCOME), last.ordered(TxType.INCOME))
    }

    @Test
    fun removeClearsOrderAndHidden() {
        val c = CategoryConfig().added("ペット", TxType.EXPENSE, 1, "pets")
            .let { it.withHidden(it.find("CUSTOM_1")!!, true) }
            .let { it.moved(it.find("CUSTOM_1")!!, -3) }
            .removed("CUSTOM_1")
        assertEquals(null, c.find("CUSTOM_1"))
        assertFalse("CUSTOM_1" in c.hidden)
        assertFalse(c.order.values.any { "CUSTOM_1" in it })
        // 既定カテゴリは削除されない
        assertEquals(Category.FOOD, CategoryConfig().removed("FOOD").find("FOOD"))
    }

    @Test
    fun jsonRoundTrip() {
        val c = CategoryConfig().added("ペット", TxType.EXPENSE, 0xFF81C784, "pets")
            .moved(Category.HOBBY, -2).withHidden(Category.SOCIAL, true)
        val back = CategoryConfig.fromJson(c.toJson())
        assertEquals(c, back)
        assertEquals("pets", back.find("CUSTOM_1")!!.iconKey)
        assertEquals(CategoryConfig(), CategoryConfig.fromJson("broken"))
    }

    @Test
    fun fromNameFallsBackToOtherOfType() {
        assertEquals(Category.OTHER_INCOME, Category.fromName("CUSTOM_99", TxType.INCOME))
        assertEquals(Category.OTHER_EXPENSE, Category.fromName("CUSTOM_99"))
        Categories.set(CategoryConfig().added("ペット", TxType.EXPENSE, 1, "pets"))
        assertEquals("ペット", Category.fromName("CUSTOM_1").label)
        assertEquals(13, com.kakeibox.data.BudgetTarget.entries.size)
    }
}
