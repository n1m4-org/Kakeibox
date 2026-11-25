package com.kakeibox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kakeibox.data.Category
import com.kakeibox.data.PaymentMethod
import com.kakeibox.data.TxType
import com.kakeibox.ui.icon

/** カテゴリの選択チップ（1つ選択） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(type: TxType, selected: Category, onSelect: (Category) -> Unit) {
    Text("カテゴリ", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Category.of(type).forEach { c ->
            FilterChip(
                selected = c == selected,
                onClick = { onSelect(c) },
                label = { Text(c.label) },
                leadingIcon = {
                    Icon(c.icon, null, tint = Color(c.color), modifier = Modifier.size(FilterChipDefaults.IconSize))
                },
            )
        }
    }
}

/** 支払い方法の選択チップ（1つ選択） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentChips(type: TxType, selected: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    Text(if (type == TxType.EXPENSE) "支払い方法" else "受け取り方法", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PaymentMethod.entries.forEach { p ->
            FilterChip(
                selected = p == selected,
                onClick = { onSelect(p) },
                label = { Text(p.label) },
                leadingIcon = { Icon(p.icon, null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
            )
        }
    }
}
