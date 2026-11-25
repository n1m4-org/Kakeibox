package com.kakeibox.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kakeibox.AppGraph
import com.kakeibox.data.Category
import com.kakeibox.data.DailyBudgetSetting
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.TargetStatus
import com.kakeibox.data.computeTodayStatuses
import kotlinx.coroutines.flow.combine
import com.kakeibox.data.MonthSummary
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
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppGraph.repository(app)
    private val budgetStore = AppGraph.budgetStore(app)

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = _month
        .flatMapLatest { repo.observeMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<MonthSummary> = transactions
        .map { MonthSummary.from(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthSummary.EMPTY)

    val budget: StateFlow<Long> = budgetStore.budget
    val dailyBudget: StateFlow<Map<BudgetTarget, DailyBudgetSetting>> = budgetStore.daily

    /** 設定済みの対象ごとの今日の予算の状況 */
    val todayStatuses: StateFlow<List<TargetStatus>> =
        combine(repo.observeDailyExpense(YearMonth.now()), budgetStore.daily) { spent, settings ->
            computeTodayStatuses(settings, spent, LocalDate.now())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** クイック入力で保存した記録（取り消し用のSnackbar表示に使う） */
    private val _quickAdded = MutableSharedFlow<Transaction>(extraBufferCapacity = 1)
    val quickAdded: SharedFlow<Transaction> = _quickAdded.asSharedFlow()

    fun prevMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }
    fun thisMonth() { _month.value = YearMonth.now() }

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
    fun quickAdd(amount: Long, category: Category) = viewModelScope.launch {
        val tx = Transaction(
            type = TxType.EXPENSE,
            category = category.name,
            amount = amount,
            date = LocalDate.now().toEpochDay(),
        )
        val id = repo.save(tx)
        _quickAdded.emit(tx.copy(id = id))
    }
}
