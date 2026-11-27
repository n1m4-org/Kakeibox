package com.kakeibox.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** 1件の収支記録。dateはLocalDate.toEpochDay()、amountは円(正の値)。 */
@Entity(tableName = "transactions", indices = [Index("date")])
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val category: String,
    val amount: Long,
    val date: Long,
    val memo: String = "",
    @ColumnInfo(defaultValue = "CASH") val payment: PaymentMethod = PaymentMethod.CASH,
) {
    val localDate: LocalDate get() = LocalDate.ofEpochDay(date)
    val categoryEnum: Category get() = Category.fromName(category, type)
}

/** クイック入力（今日・支出・メモなし）の記録 */
fun quickExpense(
    amount: Long,
    category: Category,
    payment: PaymentMethod,
    date: LocalDate = LocalDate.now(),
    memo: String = "",
) = Transaction(
    type = TxType.EXPENSE,
    category = category.name,
    amount = amount,
    date = date.toEpochDay(),
    memo = memo,
    payment = payment,
)
