package com.kakeibox

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.lifecycle.lifecycleScope
import com.kakeibox.data.Category
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import com.kakeibox.data.quickExpense
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.ui.quickadd.QuickAddContent
import com.kakeibox.ui.theme.KakeiboxTheme
import com.kakeibox.ui.yen
import kotlinx.coroutines.launch

/** ホーム画面ウィジェットから開く、クイック入力シートだけの透明Activity */
class QuickAddActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = AppGraph.repository(this)
        var saving = false

        val templateDao = AppGraph.templateDao(this)

        fun saveAndFinish(tx: Transaction) {
            if (saving) return
            saving = true
            lifecycleScope.launch {
                repo.save(tx)
                Toast.makeText(
                    applicationContext,
                    "${tx.categoryEnum.label} ${yen(tx.amount)} を記録しました",
                    Toast.LENGTH_SHORT,
                ).show()
                finish()
            }
        }

        setContent {
            val templates by templateDao.observeAll().collectAsStateWithLifecycle(emptyList())
            KakeiboxTheme {
                ModalBottomSheet(
                    onDismissRequest = { finish() },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
                    QuickAddContent(
                        defaultPayment = AppGraph.settingsStore(this@QuickAddActivity).settings.value.defaultPayment,
                        onSave = { amount, category, payment -> saveAndFinish(quickExpense(amount, category, payment)) },
                        templates = templates,
                        onTemplate = { t, entered -> saveAndFinish(t.toTransaction(enteredAmount = entered)) },
                        presetCategory = intent.getStringExtra(EXTRA_CATEGORY)
                            ?.let { name -> Category.find(name)?.takeIf { it.type == TxType.EXPENSE } },
                        onDetail = {
                            startActivity(MainActivity.openEditIntent(this@QuickAddActivity))
                            finish()
                        },
                    )
                }
            }
        }
    }

    companion object {
        /** 選択済みにするカテゴリ（Category.name）。カテゴリの「今日の予算」ウィジェットから渡す */
        const val EXTRA_CATEGORY = "category"
    }
}
