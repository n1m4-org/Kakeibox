package com.kakeibox.data

import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/**
 * 収支データへの窓口。
 * @param onChanged 保存・削除のあとに呼ばれる（ウィジェット更新など）
 */
class TransactionRepository(
    private val dao: TransactionDao,
    private val onChanged: suspend () -> Unit = {},
) {
    fun observeMonth(month: YearMonth): Flow<List<Transaction>> =
        dao.observeBetween(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())

    suspend fun monthExpense(month: YearMonth): Long =
        dao.sumExpenseBetween(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())

    fun observeMonthExpense(month: YearMonth): Flow<Long> =
        dao.observeSumExpenseBetween(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())

    /** 月内の日別・カテゴリ別支出 */
    fun observeDailyExpense(month: YearMonth): Flow<List<DayCategoryTotal>> =
        dao.observeDailyExpense(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())

    suspend fun dailyExpense(month: YearMonth): List<DayCategoryTotal> =
        dao.dailyExpense(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())

    suspend fun findById(id: Long) = dao.findById(id)

    /** 保存して採番されたIDを返す */
    suspend fun save(tx: Transaction): Long = dao.upsert(tx).also { onChanged() }

    suspend fun delete(tx: Transaction) {
        dao.delete(tx)
        onChanged()
    }
}
