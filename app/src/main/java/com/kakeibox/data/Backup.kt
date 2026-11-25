package com.kakeibox.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 全データ（明細・テンプレート・定期の収支）と設定（SharedPreferences）のバックアップ。
 */
data class BackupData(
    val transactions: List<Transaction>,
    val templates: List<Template>,
    val recurring: List<Recurring>,
    /** ファイル名 → (キー → 値)。値は String / Int / Long / Boolean / Float / Set<String> */
    val prefs: Map<String, Map<String, Any?>>,
) {
    fun toJson(): String = JSONObject().apply {
        put("app", "Kakeibox")
        put("version", VERSION)
        put("transactions", JSONArray(transactions.map { it.toJson() }))
        put("templates", JSONArray(templates.map { it.toJson() }))
        put("recurring", JSONArray(recurring.map { it.toJson() }))
        put("prefs", JSONObject().apply {
            prefs.forEach { (file, map) -> put(file, JSONObject().apply { map.forEach { (k, v) -> put(k, prefValue(v)) } }) }
        })
    }.toString(2)

    companion object {
        const val VERSION = 1

        /** @throws IllegalArgumentException Kakeibox のバックアップでない場合 */
        fun fromJson(text: String): BackupData {
            val o = try { JSONObject(text) } catch (e: Exception) { throw IllegalArgumentException("JSONとして読み込めません", e) }
            require(o.optString("app") == "Kakeibox") { "Kakeibox のバックアップファイルではありません" }
            require(o.optInt("version") <= VERSION) { "新しいバージョンのアプリで作成されたバックアップです" }
            return BackupData(
                transactions = o.getJSONArray("transactions").objects().map { it.toTransaction() },
                templates = o.optJSONArray("templates")?.objects()?.map { it.toTemplate() }.orEmpty(),
                recurring = o.optJSONArray("recurring")?.objects()?.map { it.toRecurring() }.orEmpty(),
                prefs = o.optJSONObject("prefs")?.let { p ->
                    p.keys().asSequence().associateWith { file ->
                        val m = p.getJSONObject(file)
                        m.keys().asSequence().associateWith { k -> readPrefValue(m.getJSONObject(k)) }
                    }
                }.orEmpty(),
            )
        }
    }
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

private fun Transaction.toJson() = JSONObject().apply {
    put("id", id); put("type", type.name); put("category", category); put("amount", amount)
    put("date", date); put("memo", memo); put("payment", payment.name)
}

private fun JSONObject.toTransaction() = Transaction(
    id = getLong("id"), type = TxType.valueOf(getString("type")), category = getString("category"),
    amount = getLong("amount"), date = getLong("date"), memo = optString("memo"),
    payment = PaymentMethod.fromName(optString("payment")),
)

private fun Template.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("type", type.name); put("category", category)
    put("amount", amount); put("payment", payment.name); put("memo", memo)
}

private fun JSONObject.toTemplate() = Template(
    id = getLong("id"), name = getString("name"), type = TxType.valueOf(getString("type")),
    category = getString("category"), amount = getLong("amount"),
    payment = PaymentMethod.fromName(optString("payment")), memo = optString("memo"),
)

private fun Recurring.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("type", type.name); put("category", category); put("amount", amount)
    put("payment", payment.name); put("memo", memo); put("dayOfMonth", dayOfMonth); put("startDate", startDate)
    put("processedUntil", processedUntil ?: JSONObject.NULL); put("enabled", enabled)
}

private fun JSONObject.toRecurring() = Recurring(
    id = getLong("id"), name = getString("name"), type = TxType.valueOf(getString("type")),
    category = getString("category"), amount = getLong("amount"), payment = PaymentMethod.fromName(optString("payment")),
    memo = optString("memo"), dayOfMonth = getInt("dayOfMonth"), startDate = getLong("startDate"),
    processedUntil = if (isNull("processedUntil")) null else getLong("processedUntil"),
    enabled = optBoolean("enabled", true),
)

/** SharedPreferences の値を型つきで保存する（Int と Long を区別するため） */
private fun prefValue(v: Any?): JSONObject = JSONObject().apply {
    when (v) {
        is Boolean -> { put("t", "bool"); put("v", v) }
        is Int -> { put("t", "int"); put("v", v) }
        is Long -> { put("t", "long"); put("v", v) }
        is Float -> { put("t", "float"); put("v", v.toDouble()) }
        is Set<*> -> { put("t", "set"); put("v", JSONArray(v.map { it.toString() })) }
        else -> { put("t", "string"); put("v", v?.toString() ?: JSONObject.NULL) }
    }
}

private fun readPrefValue(o: JSONObject): Any? = when (o.getString("t")) {
    "bool" -> o.getBoolean("v")
    "int" -> o.getInt("v")
    "long" -> o.getLong("v")
    "float" -> o.getDouble("v").toFloat()
    "set" -> o.getJSONArray("v").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
    else -> if (o.isNull("v")) null else o.getString("v")
}
