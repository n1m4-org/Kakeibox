package com.kakeibox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kakeibox.data.Transaction
import com.kakeibox.data.TxType
import com.kakeibox.ui.dayLabel
import com.kakeibox.ui.icon
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.theme.IncomeColor
import com.kakeibox.ui.yen

/**
 * 明細の1行：カテゴリ＋支払い方法のアイコン／メモ（あるときだけ）／金額。
 * @param showDate 検索結果など、日付見出しがない一覧では日付も表示する
 */
@Composable
fun TransactionRow(tx: Transaction, onClick: () -> Unit, showDate: Boolean = false) {
    val cat = tx.categoryEnum
    val sub = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryBadge(cat)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cat.label, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(6.dp))
                Icon(tx.payment.icon, tx.payment.label, Modifier.size(14.dp), tint = sub)
            }
            val detail = listOfNotNull(
                tx.localDate.dayLabel().takeIf { showDate },
                tx.memo.takeIf { it.isNotBlank() },
            ).joinToString("・")
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val income = tx.type == TxType.INCOME
        Text(
            (if (income) "+" else "-") + yen(tx.amount),
            style = MaterialTheme.typography.titleMedium,
            color = if (income) IncomeColor else ExpenseColor,
        )
    }
}
