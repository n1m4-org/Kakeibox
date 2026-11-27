package com.kakeibox.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** 明細のCSV（Excelで開けるようUTF-8 BOM付きで書き出す） */
object Csv {
    val HEADER = listOf("日付", "種別", "カテゴリ", "金額", "支払い方法", "メモ")
    const val BOM = "﻿"

    fun export(txs: List<Transaction>): String = buildString {
        append(BOM)
        appendLine(HEADER.joinToString(","))
        txs.sortedWith(compareBy({ it.date }, { it.id })).forEach { tx ->
            appendLine(
                listOf(
                    tx.localDate.toString(),
                    typeLabel(tx.type),
                    tx.categoryEnum.label,
                    tx.amount.toString(),
                    tx.payment.label,
                    tx.memo,
                ).joinToString(",") { escape(it) }
            )
        }
    }

    /** 取り込み結果 */
    data class ImportResult(val transactions: List<Transaction>, val skippedLines: List<Int>)

    /**
     * 自アプリの形式（列：日付,種別,カテゴリ,金額,支払い方法,メモ）を読み込む。
     * 日付は yyyy-MM-dd / yyyy/M/d、種別・カテゴリ・支払い方法は表示名か内部名。
     * 支払い方法・メモは省略可。読めない行は skippedLines（1始まりの行番号）に入れる。
     */
    fun import(text: String): ImportResult {
        val rows = parse(text.removePrefix(BOM))
        val txs = mutableListOf<Transaction>()
        val skipped = mutableListOf<Int>()
        rows.forEachIndexed { i, row ->
            val lineNo = i + 1
            if (row.all { it.isBlank() }) return@forEachIndexed
            if (i == 0 && row.firstOrNull()?.trim() == HEADER[0]) return@forEachIndexed
            val tx = parseRow(row)
            if (tx == null) skipped += lineNo else txs += tx
        }
        return ImportResult(txs, skipped)
    }

    private fun parseRow(row: List<String>): Transaction? {
        if (row.size < 4) return null
        val date = parseDate(row[0].trim()) ?: return null
        val type = parseType(row[1].trim()) ?: return null
        val category = parseCategory(row[2].trim(), type) ?: return null
        val amount = row[3].trim().replace(",", "").replace("¥", "").replace("円", "").toLongOrNull()?.takeIf { it > 0 } ?: return null
        val payment = row.getOrNull(4)?.trim()?.takeIf { it.isNotEmpty() }?.let { parsePayment(it) ?: return null } ?: PaymentMethod.CASH
        val memo = row.getOrNull(5)?.trim().orEmpty()
        return Transaction(type = type, category = category.name, amount = amount, date = date.toEpochDay(), memo = memo, payment = payment)
    }

    private val SLASH_DATE = DateTimeFormatter.ofPattern("yyyy/M/d")

    private fun parseDate(s: String): LocalDate? =
        try { LocalDate.parse(s) } catch (_: DateTimeParseException) {
            try { LocalDate.parse(s, SLASH_DATE) } catch (_: DateTimeParseException) { null }
        }

    private fun typeLabel(t: TxType) = if (t == TxType.EXPENSE) "支出" else "収入"

    private fun parseType(s: String): TxType? = when (s) {
        "支出", "EXPENSE" -> TxType.EXPENSE
        "収入", "INCOME" -> TxType.INCOME
        else -> null
    }

    private fun parseCategory(s: String, type: TxType): Category? =
        Category.all(type).firstOrNull { it.label == s || it.name == s }

    private fun parsePayment(s: String): PaymentMethod? =
        PaymentMethod.entries.firstOrNull { it.label == s || it.name == s }

    private fun escape(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    /** RFC 4180 形式のCSVを行×列に分解する（引用符内の改行・カンマに対応） */
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ } else inQuotes = false
                } else field.append(c)
            } else when (c) {
                '"' -> inQuotes = true
                ',' -> { row += field.toString(); field.clear() }
                '\r' -> {}
                '\n' -> { row += field.toString(); field.clear(); rows += row; row = mutableListOf() }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row += field.toString(); rows += row }
        return rows
    }
}
