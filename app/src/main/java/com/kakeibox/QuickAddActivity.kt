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
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import com.kakeibox.ui.quickadd.QuickAddContent
import com.kakeibox.ui.theme.KakeiboxTheme
import com.kakeibox.ui.yen
import kotlinx.coroutines.launch
import java.time.LocalDate

/** ホーム画面ウィジェットから開く、クイック入力シートだけの透明Activity */
class QuickAddActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = AppGraph.repository(this)
        var saving = false

        setContent {
            KakeiboxTheme {
                ModalBottomSheet(
                    onDismissRequest = { finish() },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
                    QuickAddContent(
                        onSave = { amount, category ->
                            if (!saving) {
                                saving = true
                                lifecycleScope.launch {
                                    repo.save(
                                        Transaction(
                                            type = TxType.EXPENSE,
                                            category = category.name,
                                            amount = amount,
                                            date = LocalDate.now().toEpochDay(),
                                        )
                                    )
                                    Toast.makeText(
                                        applicationContext,
                                        "${category.label} ${yen(amount)} を記録しました",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    finish()
                                }
                            }
                        },
                        onDetail = {
                            startActivity(MainActivity.openEditIntent(this@QuickAddActivity))
                            finish()
                        },
                    )
                }
            }
        }
    }
}
