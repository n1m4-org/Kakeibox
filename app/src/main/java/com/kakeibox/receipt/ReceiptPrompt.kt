package com.kakeibox.receipt

import com.kakeibox.data.PaymentMethod
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** 読み取ったレシートの1行（カテゴリごとの合計） */
data class ReceiptLine(val categoryKey: String, val amount: Long, val items: List<String> = emptyList())

/** 読み取ったレシート（確認画面で修正してから明細にする） */
data class ReceiptDraft(
    val store: String,
    val date: LocalDate,
    /** 読み取れなければ null（既定の支払い方法を使う） */
    val payment: PaymentMethod?,
    /** レシートに書かれた合計（読めなければ 0） */
    val total: Long,
    val lines: List<ReceiptLine>,
) {
    val linesSum: Long get() = lines.sumOf { it.amount }

    /** レシートの合計と、カテゴリごとの金額の和が合わない */
    val mismatch: Boolean get() = total > 0 && linesSum != total
}

/** カテゴリの選択肢（key と名前） */
data class CategoryOption(val key: String, val label: String)

/** 支払い方法が読み取れないときの値 */
private const val PAYMENT_UNKNOWN = "UNKNOWN"

/**
 * Claude に渡す指示と、応答（JSON）の読み方。
 * Android に依存しない純粋な処理だけを置き、ユニットテストで確かめる。
 */
object ReceiptPrompt {

    /** システムプロンプト（カテゴリの一覧と今日の日付を入れる） */
    fun system(categories: List<CategoryOption>, today: LocalDate): String = buildString {
        appendLine("あなたは家計簿アプリで、日本のレシートを読み取って支出を記録する係です。")
        appendLine("レシートの画像と、端末の文字認識で読み取った文字（誤りを含むことがあります）が渡されます。")
        appendLine("画像を正とし、文字認識の結果は読みにくい箇所の手がかりとして使ってください。")
        appendLine()
        appendLine("今日の日付: $today")
        appendLine()
        appendLine("出力のきまり:")
        appendLine("- store: 店名（読めなければ空文字）")
        appendLine("- date: 購入日を yyyy-MM-dd で。年が省略されていれば今日に近い年。読めなければ空文字")
        appendLine("- payment: 支払い方法。現金=CASH、クレジットカード=CREDIT、電子マネー（交通系IC・iD・QUICPay など）=EMONEY、QRコード決済（PayPay など）=QR、口座振替=BANK、それ以外=OTHER、分からなければ UNKNOWN")
        appendLine("- total: レシートの支払い合計（税込・円・整数）。読めなければ 0")
        appendLine("- groups: 品目をカテゴリごとにまとめた金額。category は下の一覧の key から選ぶ")
        appendLine("  - amount は税込・値引き後の円（整数）。外税のレシートは税をカテゴリの金額に按分して含める")
        appendLine("  - groups の amount の合計が total と一致するようにする（端数は一番大きいカテゴリで調整）")
        appendLine("  - items は品名の短い一覧（最大5件、長い名前は短く）")
        appendLine("  - レシートでない画像や、金額が読めない場合は groups を空にする")
        appendLine()
        appendLine("カテゴリの一覧（key: 名前）:")
        categories.forEach { appendLine("- ${it.key}: ${it.label}") }
    }

    /** ユーザーメッセージのテキスト部分（画像の後に付ける） */
    fun userText(ocrText: String?): String =
        if (ocrText.isNullOrBlank()) {
            "このレシートを読み取ってください。（端末の文字認識は使えませんでした）"
        } else {
            "このレシートを読み取ってください。\n\n端末の文字認識の結果:\n$ocrText"
        }

    /** 出力の JSON スキーマ（構造化出力）。カテゴリと支払い方法は列挙で縛る */
    fun schema(categories: List<CategoryOption>): Map<String, Any> = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf("store", "date", "payment", "total", "groups"),
        "properties" to mapOf(
            "store" to mapOf("type" to "string"),
            "date" to mapOf("type" to "string"),
            "payment" to mapOf(
                "type" to "string",
                "enum" to PaymentMethod.entries.map { it.name } + PAYMENT_UNKNOWN,
            ),
            "total" to mapOf("type" to "integer"),
            "groups" to mapOf(
                "type" to "array",
                "items" to mapOf(
                    "type" to "object",
                    "additionalProperties" to false,
                    "required" to listOf("category", "amount", "items"),
                    "properties" to mapOf(
                        "category" to mapOf("type" to "string", "enum" to categories.map { it.key }),
                        "amount" to mapOf("type" to "integer"),
                        "items" to mapOf("type" to "array", "items" to mapOf("type" to "string")),
                    ),
                ),
            ),
        ),
    )

    /**
     * 応答の JSON を [ReceiptDraft] にする。
     * - 一覧にないカテゴリは [fallbackKey]（その他）へ
     * - 同じカテゴリはまとめ、0円以下の行は除く
     * - 日付が読めなければ今日。未来の日付も今日にする
     * - 行が無く合計だけ読めたときは、合計を「その他」の1行にする
     */
    fun parse(json: String, today: LocalDate, validKeys: Set<String>, fallbackKey: String): ReceiptDraft {
        val obj = JSONObject(json)
        val total = obj.optLong("total", 0L).coerceAtLeast(0L)
        val date = obj.optString("date").trim().takeIf { it.isNotEmpty() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.takeIf { !it.isAfter(today) }
            ?: today
        val payment = obj.optString("payment").let { p -> PaymentMethod.entries.firstOrNull { it.name == p } }

        val merged = LinkedHashMap<String, Pair<Long, MutableList<String>>>()
        val groups = obj.optJSONArray("groups") ?: JSONArray()
        for (i in 0 until groups.length()) {
            val g = groups.optJSONObject(i) ?: continue
            val amount = g.optLong("amount", 0L)
            if (amount <= 0) continue
            val key = g.optString("category").takeIf { it in validKeys } ?: fallbackKey
            val items = g.optJSONArray("items")?.let { arr ->
                (0 until arr.length()).mapNotNull { arr.optString(it).trim().takeIf(String::isNotEmpty) }
            }.orEmpty()
            val (sum, list) = merged[key] ?: (0L to mutableListOf())
            list += items
            merged[key] = (sum + amount) to list
        }
        var lines = merged.map { (key, v) -> ReceiptLine(key, v.first, v.second.distinct()) }
        if (lines.isEmpty() && total > 0) lines = listOf(ReceiptLine(fallbackKey, total))

        return ReceiptDraft(
            store = obj.optString("store").trim(),
            date = date,
            payment = payment,
            total = total,
            lines = lines,
        )
    }

    /** 明細のメモ：店名（品名が分かれば「店名（品名・品名…）」）。最大100文字 */
    fun memo(store: String, items: List<String>): String {
        val head = store.trim()
        val list = items.take(3).joinToString("・")
        val text = when {
            head.isEmpty() -> list
            list.isEmpty() -> head
            else -> "$head（$list${if (items.size > 3) "…" else ""}）"
        }
        return text.take(100)
    }
}

/** レシート読み取りの失敗（画面にそのまま出す文言） */
class ReceiptException(message: String, cause: Throwable? = null) : Exception(message, cause)
