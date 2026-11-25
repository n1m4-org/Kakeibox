package com.kakeibox

import com.kakeibox.data.Category
import com.kakeibox.data.PaymentMethod
import com.kakeibox.data.SearchFilter
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import com.kakeibox.data.search
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SearchTest {
    private fun d(day: Int) = LocalDate.of(2026, 11, day)
    private val list = listOf(
        Transaction(1, TxType.EXPENSE, Category.FOOD.name, 800, d(1).toEpochDay(), "ランチ 社食", PaymentMethod.EMONEY),
        Transaction(2, TxType.EXPENSE, Category.FOOD.name, 3_500, d(5).toEpochDay(), "飲み会", PaymentMethod.CREDIT),
        Transaction(3, TxType.EXPENSE, Category.DAILY.name, 1_200, d(10).toEpochDay(), "ドラッグストア", PaymentMethod.CASH),
        Transaction(4, TxType.INCOME, Category.SALARY.name, 250_000, d(25).toEpochDay(), "", PaymentMethod.BANK),
    )
    private fun ids(f: SearchFilter) = list.search(f).map { it.id }

    @Test fun emptyFilterReturnsNothing() = assertEquals(emptyList<Long>(), ids(SearchFilter()))
    @Test fun keywordInMemo() = assertEquals(listOf(1L), ids(SearchFilter(keyword = "ランチ")))
    @Test fun keywordMatchesCategoryLabel() = assertEquals(listOf(1L, 2L), ids(SearchFilter(keyword = "食費")))
    @Test fun keywordsAreAnded() = assertEquals(listOf(1L), ids(SearchFilter(keyword = "食費　社食")))
    @Test fun byType() = assertEquals(listOf(4L), ids(SearchFilter(type = TxType.INCOME)))
    @Test fun byCategories() = assertEquals(listOf(3L), ids(SearchFilter(categories = setOf(Category.DAILY))))
    @Test fun byPayments() = assertEquals(listOf(2L, 4L), ids(SearchFilter(payments = setOf(PaymentMethod.CREDIT, PaymentMethod.BANK))))
    @Test fun byAmountRange() = assertEquals(listOf(2L, 3L), ids(SearchFilter(minAmount = 1_000, maxAmount = 5_000)))
    @Test fun byDateRangeInclusive() = assertEquals(listOf(2L, 3L), ids(SearchFilter(from = d(5), to = d(10))))
}
