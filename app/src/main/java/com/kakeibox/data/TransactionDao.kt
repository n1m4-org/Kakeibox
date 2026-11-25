package com.kakeibox.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 日別の支出合計 */
data class DayTotal(val date: Long, val total: Long)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDay AND :endDay ORDER BY date DESC, id DESC")
    fun observeBetween(startDay: Long, endDay: Long): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDay AND :endDay")
    suspend fun sumExpenseBetween(startDay: Long, endDay: Long): Long

    @Query("SELECT date, SUM(amount) AS total FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDay AND :endDay GROUP BY date")
    fun observeDailyExpense(startDay: Long, endDay: Long): Flow<List<DayTotal>>

    @Query("SELECT date, SUM(amount) AS total FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDay AND :endDay GROUP BY date")
    suspend fun dailyExpense(startDay: Long, endDay: Long): List<DayTotal>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tx: Transaction): Long

    @Delete
    suspend fun delete(tx: Transaction)
}
