package com.kakeibox.data

import java.time.LocalDate

/** 明細の検索条件（空の項目は条件なし） */
data class SearchFilter(
    val keyword: String = "",
    val type: TxType? = null,
    val categories: Set<Category> = emptySet(),
    val payments: Set<PaymentMethod> = emptySet(),
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
) {
    val isEmpty: Boolean get() = this == SearchFilter()

    /** キーワードはメモ・カテゴリ名・支払い方法名に部分一致（スペース区切りはAND） */
    fun matches(tx: Transaction): Boolean {
        if (type != null && tx.type != type) return false
        if (categories.isNotEmpty() && tx.categoryEnum !in categories) return false
        if (payments.isNotEmpty() && tx.payment !in payments) return false
        if (minAmount != null && tx.amount < minAmount) return false
        if (maxAmount != null && tx.amount > maxAmount) return false
        if (from != null && tx.date < from.toEpochDay()) return false
        if (to != null && tx.date > to.toEpochDay()) return false
        val words = keyword.split(' ', '　').filter { it.isNotBlank() }
        if (words.isNotEmpty()) {
            val haystack = "${tx.memo} ${tx.categoryEnum.label} ${tx.payment.label}"
            if (!words.all { haystack.contains(it, ignoreCase = true) }) return false
        }
        return true
    }
}

fun List<Transaction>.search(filter: SearchFilter): List<Transaction> =
    if (filter.isEmpty) emptyList() else filter { filter.matches(it) }
