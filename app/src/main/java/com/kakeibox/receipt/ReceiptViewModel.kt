package com.kakeibox.receipt

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kakeibox.AppGraph
import com.kakeibox.data.Category
import com.kakeibox.data.PaymentMethod
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** 確認画面で修正できる1行 */
data class EditLine(val categoryKey: String, val amountText: String, val items: List<String> = emptyList()) {
    val amount: Long get() = amountText.toLongOrNull() ?: 0L
}

/** レシート読み取り画面の状態 */
sealed interface ReceiptState {
    /** 画像を選ぶ */
    data object Pick : ReceiptState

    /** 読み取り中（step は「文字を読み取り中」など） */
    data class Working(val step: String, val thumb: Bitmap?, val position: String) : ReceiptState

    data class Failed(val message: String, val thumb: Bitmap?, val position: String) : ReceiptState

    /** 読み取り結果の確認・修正 */
    data class Review(
        val thumb: Bitmap?,
        val position: String,
        val store: String,
        val date: LocalDate,
        val payment: PaymentMethod,
        val total: Long,
        val lines: List<EditLine>,
    ) : ReceiptState {
        val sum: Long get() = lines.sumOf { it.amount }
        val mismatch: Boolean get() = total > 0 && sum != total
        val savable: Boolean get() = lines.any { it.amount > 0 }
    }

    /** すべて保存した（件数） */
    data class Done(val saved: Int) : ReceiptState
}

class ReceiptViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<ReceiptState>(ReceiptState.Pick)
    val state: StateFlow<ReceiptState> = _state.asStateFlow()

    private var queue: List<Uri> = emptyList()
    private var index = 0
    private var savedCount = 0
    private var job: Job? = null

    private val position: String get() = if (queue.size > 1) "${index + 1}/${queue.size}" else ""

    /** 画像（複数可）の読み取りを始める */
    fun start(uris: List<Uri>) {
        if (uris.isEmpty()) return
        queue = uris
        index = 0
        savedCount = 0
        process()
    }

    /** 失敗した画像をもう一度読み取る */
    fun retry() = process()

    /** 今の画像を保存せずに次へ */
    fun skip() = next()

    fun reset() {
        job?.cancel()
        queue = emptyList()
        _state.value = ReceiptState.Pick
    }

    fun edit(transform: (ReceiptState.Review) -> ReceiptState.Review) {
        (_state.value as? ReceiptState.Review)?.let { _state.value = transform(it) }
    }

    /** 確認した内容を明細として保存し、次の画像へ */
    fun save() {
        val review = _state.value as? ReceiptState.Review ?: return
        val txs = review.lines.filter { it.amount > 0 }.map { line ->
            Transaction(
                type = TxType.EXPENSE,
                category = line.categoryKey,
                amount = line.amount,
                date = review.date.toEpochDay(),
                memo = ReceiptPrompt.memo(review.store, line.items),
                payment = review.payment,
            )
        }
        viewModelScope.launch {
            val repo = AppGraph.repository(getApplication())
            txs.forEach { repo.save(it) }
            savedCount += txs.size
            next()
        }
    }

    private fun next() {
        if (index + 1 < queue.size) {
            index++
            process()
        } else {
            _state.value = ReceiptState.Done(savedCount)
        }
    }

    private fun process() {
        val uri = queue.getOrNull(index) ?: return
        job?.cancel()
        job = viewModelScope.launch {
            val app = getApplication<Application>()
            var thumb: Bitmap? = null
            try {
                val apiKey = AppGraph.apiKeyStore(app).get()
                    ?: throw ReceiptException("API キーが登録されていません。設定から登録してください")
                _state.value = ReceiptState.Working("画像を読み込み中", null, position)
                val bitmap = withContext(Dispatchers.IO) { ReceiptImage.load(app, uri) }
                thumb = ReceiptImage.thumbnail(bitmap)
                _state.value = ReceiptState.Working("文字を読み取り中", thumb, position)
                val ocr = ReceiptOcr.recognize(bitmap)
                _state.value = ReceiptState.Working("AI で解析中", thumb, position)
                val jpeg = withContext(Dispatchers.Default) { ReceiptImage.toJpegBase64(bitmap) }
                val categories = Category.of(TxType.EXPENSE).map { CategoryOption(it.key, it.label) }
                val draft = ReceiptReader(apiKey).read(jpeg, ocr, categories, Category.OTHER_EXPENSE.key)
                val defaultPayment = AppGraph.settingsStore(app).settings.value.defaultPayment
                _state.value = ReceiptState.Review(
                    thumb = thumb,
                    position = position,
                    store = draft.store,
                    date = draft.date,
                    payment = draft.payment ?: defaultPayment,
                    total = draft.total,
                    lines = draft.lines.map { EditLine(it.categoryKey, it.amount.toString(), it.items) }
                        .ifEmpty { listOf(EditLine(Category.OTHER_EXPENSE.key, "")) },
                )
            } catch (e: ReceiptException) {
                _state.value = ReceiptState.Failed(e.message ?: "読み取れませんでした", thumb, position)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ReceiptState.Failed("読み取れませんでした（${e.javaClass.simpleName}）", thumb, position)
            }
        }
    }
}
