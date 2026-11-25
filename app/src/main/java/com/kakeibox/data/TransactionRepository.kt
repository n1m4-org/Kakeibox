package com.kakeibox.data

import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

class TransactionRepository(private val dao: TransactionDao) {
    fun observeMonth(month: YearMonth): Flow<List<Transaction>> =
        dao.observeBetween(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())

    suspend fun findById(id: Long) = dao.findById(id)
    suspend fun save(tx: Transaction) = dao.upsert(tx)
    suspend fun delete(tx: Transaction) = dao.delete(tx)
}
