package com.kakeibox.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    /** 月内の日別支出（epochDay → 合計） */
    fun observeDailyExpense(month: YearMonth): Flow<Map<Long, Long>> =
        dao.observeDailyExpense(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())
            .map { list -> list.associate { it.date to it.total } }

    suspend fun dailyExpense(month: YearMonth): Map<Long, Long> =
        dao.dailyExpense(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())
            .associate { it.date to it.total }

    suspend fun findById(id: Long) = dao.findById(id)

    /** 保存して採番されたIDを返す */
    suspend fun save(tx: Transaction): Long = dao.upsert(tx).also { onChanged() }

    suspend fun delete(tx: Transaction) {
        dao.delete(tx)
        onChanged()
    }
}
