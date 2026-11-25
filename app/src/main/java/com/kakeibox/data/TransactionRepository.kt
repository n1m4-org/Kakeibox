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

    suspend fun findById(id: Long) = dao.findById(id)

    /** 保存して採番されたIDを返す */
    suspend fun save(tx: Transaction): Long = dao.upsert(tx).also { onChanged() }

    suspend fun delete(tx: Transaction) {
        dao.delete(tx)
        onChanged()
    }
}
