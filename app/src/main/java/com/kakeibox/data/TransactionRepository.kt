package com.kakeibox.data

import kotlinx.coroutines.flow.Flow

/**
 * 収支データへの窓口。
 * @param onChanged 保存・削除のあとに呼ばれる（ウィジェット更新など）
 */
class TransactionRepository(
    private val dao: TransactionDao,
    private val onChanged: suspend () -> Unit = {},
) {
    fun observePeriod(period: Period): Flow<List<Transaction>> =
        dao.observeBetween(period.startEpochDay, period.endEpochDay)

    suspend fun periodExpense(period: Period): Long =
        dao.sumExpenseBetween(period.startEpochDay, period.endEpochDay)

    fun observePeriodExpense(period: Period): Flow<Long> =
        dao.observeSumExpenseBetween(period.startEpochDay, period.endEpochDay)

    /** 期間内の日別・カテゴリ別支出 */
    fun observeDailyExpense(period: Period): Flow<List<DayCategoryTotal>> =
        dao.observeDailyExpense(period.startEpochDay, period.endEpochDay)

    suspend fun dailyExpense(period: Period): List<DayCategoryTotal> =
        dao.dailyExpense(period.startEpochDay, period.endEpochDay)

    fun observeAll(): Flow<List<Transaction>> = dao.observeAll()
    suspend fun all(): List<Transaction> = dao.all()

    suspend fun findById(id: Long) = dao.findById(id)

    /** 保存して採番されたIDを返す */
    suspend fun save(tx: Transaction): Long = dao.upsert(tx).also { onChanged() }

    suspend fun delete(tx: Transaction) {
        dao.delete(tx)
        onChanged()
    }
}
