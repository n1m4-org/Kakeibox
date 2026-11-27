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

/**
 * よく使う記録の定型。amount=0 は「金額はその都度入力」。
 */
@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType,
    val category: String,
    val amount: Long,
    val payment: PaymentMethod,
    val memo: String = "",
) {
    val categoryEnum: Category get() = Category.fromName(category, type)

    /** テンプレートから記録を作る。amount=0 のテンプレートは [enteredAmount] を使う */
    fun toTransaction(date: LocalDate = LocalDate.now(), enteredAmount: Long = 0): Transaction = Transaction(
        type = type,
        category = category,
        amount = if (amount > 0) amount else enteredAmount,
        date = date.toEpochDay(),
        memo = memo,
        payment = payment,
    )

    companion object {
        /** 入力中の記録からテンプレートを作る */
        fun from(tx: Transaction, name: String) = Template(
            name = name,
            type = tx.type,
            category = tx.category,
            amount = tx.amount,
            payment = tx.payment,
            memo = tx.memo,
        )
    }
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY id")
    fun observeAll(): Flow<List<Template>>

    @Query("SELECT * FROM templates ORDER BY id")
    suspend fun all(): List<Template>

    @Query("SELECT COUNT(*) FROM templates WHERE category = :key")
    suspend fun countByCategory(key: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(t: Template): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Template>)

    @Query("DELETE FROM templates")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(t: Template)
}
