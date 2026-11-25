package com.kakeibox.ui.quickadd

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kakeibox.data.Category
import com.kakeibox.data.PaymentMethod
import com.kakeibox.ui.icon
import com.kakeibox.data.TxType
import com.kakeibox.ui.components.CategoryBadge
import com.kakeibox.ui.theme.ExpenseColor
import com.kakeibox.ui.yen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 支出のクイック入力。金額をキーパッドで入力し、カテゴリをタップした時点で保存する。
 * 日付は今日、種別は支出固定。日付・メモ・収入は [onDetail] から詳細入力へ。
 */
@Composable
fun QuickAddContent(
    defaultPayment: PaymentMethod,
    onSave: (amount: Long, category: Category, payment: PaymentMethod) -> Unit,
    onDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var payment by rememberSaveable(defaultPayment) { mutableStateOf(defaultPayment) }
    var error by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("今日の支出", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            PaymentSelector(payment, onChange = { payment = it })
            TextButton(onClick = onDetail) {
                Text("詳細入力")
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
            }
        }

        // 金額表示
        Text(
            text = yen(amount.toLongOrNull() ?: 0),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            color = if (error) ExpenseColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().offset { IntOffset(shake.value.roundToInt(), 0) },
        )
        Text(
            if (error) "金額を入力してください" else "金額を入力して、カテゴリをタップすると記録されます",
            style = MaterialTheme.typography.bodySmall,
            color = if (error) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // カテゴリ（タップで即保存）
        Category.of(TxType.EXPENSE).chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { c ->
                    CategoryCell(c, Modifier.weight(1f)) {
                        val value = amount.toLongOrNull() ?: 0
                        if (value <= 0) {
                            error = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                shake.snapTo(32f)
                                shake.animateTo(0f, spring(dampingRatio = 0.2f, stiffness = 1200f))
                            }
                        } else {
                            onSave(value, c, payment)
                            amount = ""
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Keypad(
            onKey = { key ->
                amount = applyKey(amount, key)
                error = false
            },
            onClear = { amount = "" },
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CategoryCell(category: Category, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Column(
            Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CategoryBadge(category, size = 44.dp)
            Text(category.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Keypad(onKey: (AmountKey) -> Unit, onClear: () -> Unit) {
    val rows = listOf(
        listOf(AmountKey.Digit(7), AmountKey.Digit(8), AmountKey.Digit(9)),
        listOf(AmountKey.Digit(4), AmountKey.Digit(5), AmountKey.Digit(6)),
        listOf(AmountKey.Digit(1), AmountKey.Digit(2), AmountKey.Digit(3)),
        listOf(AmountKey.DoubleZero, AmountKey.Digit(0), AmountKey.Backspace),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Box(
                            Modifier.fillMaxSize().combinedClickable(
                                role = Role.Button,
                                onClick = { onKey(key) },
                                // ⌫ 長押しで全消去
                                onLongClick = if (key == AmountKey.Backspace) onClear else null,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (key) {
                                AmountKey.Backspace -> Icon(Icons.AutoMirrored.Outlined.Backspace, "1文字消す（長押しで全消去）")
                                AmountKey.DoubleZero -> Text("00", style = MaterialTheme.typography.titleLarge)
                                is AmountKey.Digit -> Text(key.value.toString(), style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 支払い方法の選択（タップでメニュー） */
@Composable
fun PaymentSelector(payment: PaymentMethod, onChange: (PaymentMethod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(payment.label) },
            leadingIcon = { Icon(payment.icon, null, Modifier.size(AssistChipDefaults.IconSize)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PaymentMethod.entries.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.label) },
                    leadingIcon = { Icon(p.icon, null) },
                    onClick = { onChange(p); expanded = false },
                )
            }
        }
    }
}
