package com.kakeibox.data

enum class TxType { EXPENSE, INCOME }

/**
 * 収支カテゴリ。DBには [key] を保存する。
 * 既定のカテゴリ（[builtIn]）と、ユーザーが追加したカテゴリ（key = "CUSTOM_<番号>"）がある。
 * 同一性は key だけで決まる（名前や色を変えても、記録・予算との対応は変わらない）。
 */
class Category(
    val key: String,
    val label: String,
    val type: TxType,
    val color: Long,
    /** アイコンのキー（ui/CategoryIcons.kt の表） */
    val iconKey: String,
    val builtIn: Boolean = false,
) {
    /** 以前の enum との互換（= key） */
    val name: String get() = key

    /** 「その他」は受け皿のため、非表示・削除できない */
    val isFallback: Boolean get() = key == OTHER_EXPENSE.key || key == OTHER_INCOME.key

    fun copy(label: String = this.label, color: Long = this.color, iconKey: String = this.iconKey) =
        Category(key, label, type, color, iconKey, builtIn)

    override fun equals(other: Any?): Boolean = other is Category && other.key == key
    override fun hashCode(): Int = key.hashCode()
    override fun toString(): String = "Category($key, $label)"

    companion object {
        val FOOD = Category("FOOD", "食費", TxType.EXPENSE, 0xFFE57373, "restaurant", true)
        val DAILY = Category("DAILY", "日用品", TxType.EXPENSE, 0xFF64B5F6, "shopping_basket", true)
        val TRANSPORT = Category("TRANSPORT", "交通費", TxType.EXPENSE, 0xFF81C784, "train", true)
        val HOUSING = Category("HOUSING", "住居", TxType.EXPENSE, 0xFFFFB74D, "home", true)
        val UTILITY = Category("UTILITY", "水道光熱費", TxType.EXPENSE, 0xFFBA68C8, "lightbulb", true)
        val COMMUNICATION = Category("COMMUNICATION", "通信費", TxType.EXPENSE, 0xFF4DB6AC, "smartphone", true)
        val SOCIAL = Category("SOCIAL", "交際費", TxType.EXPENSE, 0xFFF06292, "local_bar", true)
        val HOBBY = Category("HOBBY", "趣味・娯楽", TxType.EXPENSE, 0xFF7986CB, "sports_esports", true)
        val MEDICAL = Category("MEDICAL", "医療・保険", TxType.EXPENSE, 0xFFA1887F, "local_hospital", true)
        val CLOTHES = Category("CLOTHES", "衣服・美容", TxType.EXPENSE, 0xFFFFD54F, "checkroom", true)
        val OTHER_EXPENSE = Category("OTHER_EXPENSE", "その他", TxType.EXPENSE, 0xFF90A4AE, "category", true)

        val SALARY = Category("SALARY", "給料", TxType.INCOME, 0xFF43A047, "payments", true)
        val BONUS = Category("BONUS", "賞与", TxType.INCOME, 0xFF1E88E5, "card_giftcard", true)
        val SIDE_JOB = Category("SIDE_JOB", "副業", TxType.INCOME, 0xFF8E24AA, "work_outline", true)
        val ALLOWANCE = Category("ALLOWANCE", "お小遣い", TxType.INCOME, 0xFFFB8C00, "account_balance_wallet", true)
        val OTHER_INCOME = Category("OTHER_INCOME", "その他", TxType.INCOME, 0xFF757575, "category", true)

        /** 既定のカテゴリ（この順が既定の並び） */
        val BUILT_IN: List<Category> = listOf(
            FOOD, DAILY, TRANSPORT, HOUSING, UTILITY, COMMUNICATION, SOCIAL, HOBBY, MEDICAL, CLOTHES, OTHER_EXPENSE,
            SALARY, BONUS, SIDE_JOB, ALLOWANCE, OTHER_INCOME,
        )

        /** 表示中のカテゴリ（並び順どおり。非表示は含まない） */
        fun of(type: TxType): List<Category> = Categories.config.value.visible(type)

        /** 非表示も含むすべてのカテゴリ（並び順どおり） */
        fun all(type: TxType): List<Category> = Categories.config.value.ordered(type)

        /** すべてのカテゴリ（支出 → 収入） */
        val entries: List<Category> get() = all(TxType.EXPENSE) + all(TxType.INCOME)

        /** key からカテゴリを探す。見つからなければ（削除済みなど）その種別の「その他」 */
        fun fromName(name: String, type: TxType? = null): Category =
            find(name) ?: if (type == TxType.INCOME) OTHER_INCOME else OTHER_EXPENSE

        fun find(key: String): Category? = Categories.config.value.find(key)
    }
}
