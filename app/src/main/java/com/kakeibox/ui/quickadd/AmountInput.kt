package com.kakeibox.ui.quickadd

/** 数字キーパッドのキー */
sealed interface AmountKey {
    data class Digit(val value: Int) : AmountKey {
        init { require(value in 0..9) }
    }
    data object DoubleZero : AmountKey
    data object Backspace : AmountKey
}

/** 入力できる最大桁数（¥9,999,999,999 まで） */
const val MAX_AMOUNT_DIGITS = 10

/**
 * キー入力を反映した金額文字列を返す。
 * - 先頭の0は入力しない（空のまま）
 * - 桁数上限を超える入力は無視
 */
fun applyKey(current: String, key: AmountKey): String {
    val append = when (key) {
        AmountKey.Backspace -> return current.dropLast(1)
        AmountKey.DoubleZero -> "00"
        is AmountKey.Digit -> key.value.toString()
    }
    if (current.isEmpty() && append.startsWith("0")) return current
    val next = current + append
    return if (next.length > MAX_AMOUNT_DIGITS) current else next
}
