package com.kakeibox.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * カテゴリの設定：追加したカテゴリ・並び順・非表示。
 * すべて純粋な値で、変更は新しい値を返す。
 */
data class CategoryConfig(
    val custom: List<Category> = emptyList(),
    /** 種別ごとの並び順（key のリスト）。載っていないカテゴリは既定の順で後ろに付く */
    val order: Map<TxType, List<String>> = emptyMap(),
    val hidden: Set<String> = emptySet(),
    /** 次に追加するカテゴリの番号 */
    val nextId: Int = 1,
) {
    fun find(key: String): Category? = Category.BUILT_IN.firstOrNull { it.key == key } ?: custom.firstOrNull { it.key == key }

    /** 非表示も含めて並び順どおり */
    fun ordered(type: TxType): List<Category> {
        val pool = (Category.BUILT_IN + custom).filter { it.type == type }
        val order = order[type].orEmpty()
        val listed = order.mapNotNull { k -> pool.firstOrNull { it.key == k } }
        return listed + pool.filter { it !in listed }
    }

    fun visible(type: TxType): List<Category> = ordered(type).filter { it.key !in hidden }

    fun isHidden(c: Category): Boolean = c.key in hidden

    fun added(label: String, type: TxType, color: Long, icon: String): CategoryConfig {
        val c = Category("CUSTOM_$nextId", label.trim(), type, color, icon)
        return copy(custom = custom + c, nextId = nextId + 1)
    }

    /** 追加したカテゴリの名前・色・アイコンを変える（既定カテゴリは変えない） */
    fun updated(key: String, label: String, color: Long, icon: String): CategoryConfig =
        copy(custom = custom.map { if (it.key == key) it.copy(label = label.trim(), color = color, iconKey = icon) else it })

    /** 追加したカテゴリを消す（既定カテゴリは消さない） */
    fun removed(key: String): CategoryConfig = copy(
        custom = custom.filter { it.key != key },
        order = order.mapValues { (_, keys) -> keys - key },
        hidden = hidden - key,
    )

    /** 非表示にする／戻す（「その他」は非表示にできない） */
    fun withHidden(c: Category, hide: Boolean): CategoryConfig = when {
        c.isFallback -> this
        hide -> copy(hidden = hidden + c.key)
        else -> copy(hidden = hidden - c.key)
    }

    /** 並び順を delta（-1=上、+1=下）だけ動かす */
    fun moved(c: Category, delta: Int): CategoryConfig {
        val keys = ordered(c.type).map { it.key }.toMutableList()
        val i = keys.indexOf(c.key)
        val j = i + delta
        if (i < 0 || j !in keys.indices) return this
        keys.removeAt(i)
        keys.add(j, c.key)
        return copy(order = order + (c.type to keys))
    }

    fun toJson(): String = JSONObject().apply {
        put("custom", JSONArray(custom.map { c ->
            JSONObject().put("key", c.key).put("label", c.label).put("type", c.type.name).put("color", c.color).put("icon", c.iconKey)
        }))
        put("order", JSONObject().apply { order.forEach { (t, keys) -> put(t.name, JSONArray(keys)) } })
        put("hidden", JSONArray(hidden.toList()))
        put("nextId", nextId)
    }.toString()

    companion object {
        fun fromJson(text: String?): CategoryConfig {
            if (text.isNullOrBlank()) return CategoryConfig()
            return runCatching {
                val o = JSONObject(text)
                val customArr = o.optJSONArray("custom") ?: JSONArray()
                val custom = (0 until customArr.length()).map { i ->
                    val c = customArr.getJSONObject(i)
                    Category(c.getString("key"), c.getString("label"), TxType.valueOf(c.getString("type")), c.getLong("color"), c.getString("icon"))
                }
                val orderObj = o.optJSONObject("order") ?: JSONObject()
                val order = TxType.entries.mapNotNull { t ->
                    orderObj.optJSONArray(t.name)?.let { a -> t to (0 until a.length()).map { a.getString(it) } }
                }.toMap()
                val hiddenArr = o.optJSONArray("hidden") ?: JSONArray()
                CategoryConfig(
                    custom = custom,
                    order = order,
                    hidden = (0 until hiddenArr.length()).map { hiddenArr.getString(it) }.toSet(),
                    nextId = o.optInt("nextId", (custom.maxOfOrNull { it.key.removePrefix("CUSTOM_").toIntOrNull() ?: 0 } ?: 0) + 1),
                )
            }.getOrDefault(CategoryConfig())
        }
    }
}

/** アプリ全体で共有するカテゴリの設定（起動時に CategoryStore が読み込む） */
object Categories {
    private val _config = MutableStateFlow(CategoryConfig())
    val config: StateFlow<CategoryConfig> = _config.asStateFlow()

    fun set(config: CategoryConfig) {
        _config.value = config
    }
}
