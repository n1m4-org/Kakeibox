package com.kakeibox.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/**
 * 毎月決まった日に自動で記録する定期の収支（家賃・サブスク・給料など）。
 * dayOfMonth が月の日数より大きい場合は月末に記録する（31 = 毎月末）。
 */
@Entity(tableName = "recurring")
data class Recurring(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType,
    val category: String,
    val amount: Long,
    val payment: PaymentMethod,
    val memo: String = "",
    val dayOfMonth: Int,
    /** この日以降の分だけ記録する（epochDay） */
    val startDate: Long,
    /** ここまで処理済み（epochDay）。null は未処理 */
    val processedUntil: Long? = null,
    val enabled: Boolean = true,
) {
    val categoryEnum: Category get() = Category.fromName(category, type)

    fun toTransaction(date: LocalDate) = Transaction(
        type = type,
        category = category,
        amount = amount,
        date = date.toEpochDay(),
        memo = memo.ifBlank { name },
        payment = payment,
    )

    val dayLabel: String get() = if (dayOfMonth >= 31) "毎月末" else "毎月${dayOfMonth}日"
}

/** その月の記録日（月の日数を超える場合は月末） */
fun Recurring.dateIn(month: YearMonth): LocalDate = month.atDay(minOf(dayOfMonth, month.lengthOfMonth()))

/** まだ記録していない、today までに到来した記録日 */
fun Recurring.dueDates(today: LocalDate): List<LocalDate> {
    if (!enabled) return emptyList()
    val from = maxOf(startDate, (processedUntil ?: Long.MIN_VALUE) + 1)
    if (from > today.toEpochDay()) return emptyList()
    val result = mutableListOf<LocalDate>()
    var month = YearMonth.from(LocalDate.ofEpochDay(from))
    val last = YearMonth.from(today)
    while (month <= last) {
        val d = dateIn(month)
        if (d.toEpochDay() in from..today.toEpochDay()) result += d
        month = month.plusMonths(1)
    }
    return result
}

/** 次回の記録日（today を含む） */
fun Recurring.nextDate(today: LocalDate): LocalDate {
    val base = maxOf(today, LocalDate.ofEpochDay(startDate))
    val thisMonth = dateIn(YearMonth.from(base))
    return if (thisMonth >= base) thisMonth else dateIn(YearMonth.from(base).plusMonths(1))
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY dayOfMonth, id")
    fun observeAll(): Flow<List<Recurring>>

    @Query("SELECT * FROM recurring")
    suspend fun all(): List<Recurring>

    @Query("SELECT COUNT(*) FROM recurring WHERE category = :key")
    suspend fun countByCategory(key: String): Int

    @Query("SELECT * FROM recurring WHERE id = :id")
    suspend fun find(id: Long): Recurring?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(r: Recurring): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Recurring>)

    @Query("DELETE FROM recurring")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(r: Recurring)
}
