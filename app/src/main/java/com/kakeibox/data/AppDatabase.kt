package com.kakeibox.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun fromType(t: TxType): String = t.name
    @TypeConverter fun toType(s: String): TxType = TxType.valueOf(s)
    @TypeConverter fun fromPayment(p: PaymentMethod): String = p.name
    @TypeConverter fun toPayment(s: String): PaymentMethod = PaymentMethod.fromName(s)
}

@Database(
    entities = [Transaction::class, Template::class, Recurring::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // 支払い方法の列を追加
        AutoMigration(from = 2, to = 3), // テンプレートのテーブルを追加
        AutoMigration(from = 3, to = 4), // 定期の収支のテーブルを追加
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun templateDao(): TemplateDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "kakeibox.db")
                    .build().also { instance = it }
            }
    }
}
