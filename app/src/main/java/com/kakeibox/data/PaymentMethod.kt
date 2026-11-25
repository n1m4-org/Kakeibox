package com.kakeibox.data

/** 支払い方法（収入の場合は受け取り方法） */
enum class PaymentMethod(val label: String) {
    CASH("現金"),
    CREDIT("クレジットカード"),
    EMONEY("電子マネー"),
    QR("QRコード決済"),
    BANK("銀行・口座振替"),
    OTHER("その他");

    companion object {
        fun fromName(name: String?): PaymentMethod = entries.firstOrNull { it.name == name } ?: CASH
    }
}
