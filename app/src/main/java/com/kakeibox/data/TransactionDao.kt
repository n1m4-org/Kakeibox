package com.kakeibox.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 日別・カテゴリ別の支出合計 */
data class DayCategoryTotal(val date: Long, val category: String, val total: Long)

private const val DAILY_EXPENSE =
    "SELECT date, category, SUM(amount) AS total FROM transactions " +
        "WHERE type = 'EXPENSE' AND date BETWEEN :startDay AND :endDay GROUP BY date, category"

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDay AND :endDay ORDER BY date DESC, id DESC")
    fun observeBetween(startDay: Long, endDay: Long): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDay AND :endDay")
    suspend fun sumExpenseBetween(startDay: Long, endDay: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDay AND :endDay")
    fun observeSumExpenseBetween(startDay: Long, endDay: Long): Flow<Long>

    @Query(DAILY_EXPENSE)
    fun observeDailyExpense(startDay: Long, endDay: Long): Flow<List<DayCategoryTotal>>

    @Query(DAILY_EXPENSE)
    suspend fun dailyExpense(startDay: Long, endDay: Long): List<DayCategoryTotal>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date, id")
    suspend fun all(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tx: Transaction): Long

    @Delete
    suspend fun delete(tx: Transaction)
}
