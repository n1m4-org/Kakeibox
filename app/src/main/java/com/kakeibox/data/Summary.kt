package com.kakeibox.data

/** カテゴリ別合計 */
data class CategoryTotal(val category: Category, val total: Long, val ratio: Float)

/** 月次集計 */
data class MonthSummary(
    val income: Long,
    val expense: Long,
    val expenseByCategory: List<CategoryTotal>,
    val incomeByCategory: List<CategoryTotal>,
    /** 支出の支払い方法別合計（多い順） */
    val expenseByPayment: List<Pair<PaymentMethod, Long>> = emptyList(),
) {
    val balance: Long get() = income - expense

    companion object {
        val EMPTY = MonthSummary(0, 0, emptyList(), emptyList())

        fun from(list: List<Transaction>): MonthSummary {
            val income = list.filter { it.type == TxType.INCOME }.sumOf { it.amount }
            val expense = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
            return MonthSummary(
                income = income,
                expense = expense,
                expenseByCategory = byCategory(list, TxType.EXPENSE, expense),
                incomeByCategory = byCategory(list, TxType.INCOME, income),
                expenseByPayment = list.filter { it.type == TxType.EXPENSE }
                    .groupBy { it.payment }
                    .map { (p, txs) -> p to txs.sumOf { it.amount } }
                    .sortedByDescending { it.second },
            )
        }

        private fun byCategory(list: List<Transaction>, type: TxType, total: Long): List<CategoryTotal> =
            list.filter { it.type == type }
                .groupBy { it.categoryEnum }
                .map { (c, txs) ->
                    val sum = txs.sumOf { it.amount }
                    CategoryTotal(c, sum, if (total > 0) sum.toFloat() / total else 0f)
                }
                .sortedByDescending { it.total }
    }
}
