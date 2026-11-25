package com.kakeibox.data

/** カテゴリ別合計 */
data class CategoryTotal(val category: Category, val total: Long, val ratio: Float)

/** 月次集計 */
data class MonthSummary(
    val income: Long,
    val expense: Long,
    val expenseByCategory: List<CategoryTotal>,
    val incomeByCategory: List<CategoryTotal>,
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
