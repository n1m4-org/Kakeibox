package com.kakeibox.data

enum class TxType { EXPENSE, INCOME }

/** 収支カテゴリ。DBにはnameを保存する。 */
enum class Category(val label: String, val type: TxType, val color: Long) {
    FOOD("食費", TxType.EXPENSE, 0xFFE57373),
    DAILY("日用品", TxType.EXPENSE, 0xFF64B5F6),
    TRANSPORT("交通費", TxType.EXPENSE, 0xFF81C784),
    HOUSING("住居", TxType.EXPENSE, 0xFFFFB74D),
    UTILITY("水道光熱費", TxType.EXPENSE, 0xFFBA68C8),
    COMMUNICATION("通信費", TxType.EXPENSE, 0xFF4DB6AC),
    SOCIAL("交際費", TxType.EXPENSE, 0xFFF06292),
    HOBBY("趣味・娯楽", TxType.EXPENSE, 0xFF7986CB),
    MEDICAL("医療・保険", TxType.EXPENSE, 0xFFA1887F),
    CLOTHES("衣服・美容", TxType.EXPENSE, 0xFFFFD54F),
    OTHER_EXPENSE("その他", TxType.EXPENSE, 0xFF90A4AE),

    SALARY("給料", TxType.INCOME, 0xFF43A047),
    BONUS("賞与", TxType.INCOME, 0xFF1E88E5),
    SIDE_JOB("副業", TxType.INCOME, 0xFF8E24AA),
    ALLOWANCE("お小遣い", TxType.INCOME, 0xFFFB8C00),
    OTHER_INCOME("その他", TxType.INCOME, 0xFF757575);

    companion object {
        fun of(type: TxType): List<Category> = entries.filter { it.type == type }
        fun fromName(name: String): Category = entries.firstOrNull { it.name == name } ?: OTHER_EXPENSE
    }
}
