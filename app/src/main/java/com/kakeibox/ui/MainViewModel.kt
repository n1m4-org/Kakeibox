package com.kakeibox.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kakeibox.AppGraph
import com.kakeibox.data.Category
import com.kakeibox.data.DailyBudgetSetting
import com.kakeibox.data.AppSettings
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.TargetStatus
import com.kakeibox.data.computeTodayStatuses
import kotlinx.coroutines.flow.combine
import com.kakeibox.data.MonthSummary
import com.kakeibox.data.PaymentMethod
import com.kakeibox.data.Period
import com.kakeibox.data.Template
import com.kakeibox.data.quickExpense
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppGraph.repository(app)
    private val budgetStore = AppGraph.budgetStore(app)
    private val settingsStore = AppGraph.settingsStore(app)

    val settings: StateFlow<AppSettings> = settingsStore.settings
    fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsStore.update(transform)

    /** 今日を含む期間（締め日の設定に追従） */
    val currentPeriod: StateFlow<Period> = settingsStore.settings
        .map { Period.of(LocalDate.now(), it.periodStartDay) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsStore.currentPeriod())

    /** 表示中の期間（今の期間からのずれ） */
    private val _offset = MutableStateFlow(0L)
    val period: StateFlow<Period> = combine(currentPeriod, _offset) { p, off -> p.shift(off) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsStore.currentPeriod())

    val transactions: StateFlow<List<Transaction>> = period
        .flatMapLatest { repo.observePeriod(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<MonthSummary> = transactions
        .map { MonthSummary.from(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthSummary.EMPTY)

    val budget: StateFlow<Long> = budgetStore.budget
    val dailyBudget: StateFlow<Map<BudgetTarget, DailyBudgetSetting>> = budgetStore.daily

    /** 設定済みの対象ごとの今日の予算の状況 */
    val todayStatuses: StateFlow<List<TargetStatus>> =
        currentPeriod.flatMapLatest { p ->
            combine(repo.observeDailyExpense(p), budgetStore.daily) { spent, settings ->
                computeTodayStatuses(settings, spent, LocalDate.now(), p.start)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** クイック入力で保存した記録（取り消し用のSnackbar表示に使う） */
    private val _quickAdded = MutableSharedFlow<Transaction>(extraBufferCapacity = 1)
    val quickAdded: SharedFlow<Transaction> = _quickAdded.asSharedFlow()

    fun prevMonth() { _offset.value -= 1 }
    fun nextMonth() { _offset.value += 1 }
    fun thisMonth() { _offset.value = 0 }

    suspend fun find(id: Long): Transaction? = repo.findById(id)

    fun save(tx: Transaction) = viewModelScope.launch { repo.save(tx) }
    fun delete(tx: Transaction) = viewModelScope.launch { repo.delete(tx) }
    fun setBudget(value: Long) {
        budgetStore.set(value)
        refreshWidgets()
    }

    fun setDailyBudget(target: BudgetTarget, value: Long) {
        budgetStore.setDaily(target, value)
        refreshWidgets()
    }

    private fun refreshWidgets() = viewModelScope.launch { AppGraph.refreshWidgets(getApplication()) }

    /** 今日の支出として保存する */
    private val templateDao = AppGraph.templateDao(app)
    val templates: StateFlow<List<Template>> = templateDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveTemplate(t: Template) = viewModelScope.launch { templateDao.upsert(t) }
    fun deleteTemplate(t: Template) = viewModelScope.launch { templateDao.delete(t) }

    /** テンプレートで今日の記録をつける */
    fun addFromTemplate(t: Template, enteredAmount: Long) = viewModelScope.launch {
        addAndNotify(t.toTransaction(enteredAmount = enteredAmount))
    }

    fun quickAdd(amount: Long, category: Category, payment: PaymentMethod) = viewModelScope.launch {
        addAndNotify(quickExpense(amount, category, payment))
    }

    /** 保存して、取り消し用のSnackbarに通知する */
    private suspend fun addAndNotify(tx: Transaction) {
        val id = repo.save(tx)
        _quickAdded.emit(tx.copy(id = id))
    }
}
