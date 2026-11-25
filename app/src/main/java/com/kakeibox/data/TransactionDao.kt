package com.kakeibox.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDay AND :endDay ORDER BY date DESC, id DESC")
    fun observeBetween(startDay: Long, endDay: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tx: Transaction): Long

    @Delete
    suspend fun delete(tx: Transaction)
}
