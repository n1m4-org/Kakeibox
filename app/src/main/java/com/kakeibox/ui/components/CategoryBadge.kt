package com.kakeibox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.Category
import com.kakeibox.ui.color
import com.kakeibox.ui.icon

/** 色の薄い円の中に線画アイコンを表示する */
@Composable
fun IconBadge(icon: ImageVector, color: Color, description: String?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier.size(size).background(color.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = color, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun CategoryBadge(category: Category, modifier: Modifier = Modifier, size: Dp = 40.dp) =
    IconBadge(category.icon, Color(category.color), category.label, modifier, size)

@Composable
fun TargetBadge(target: BudgetTarget, modifier: Modifier = Modifier, size: Dp = 40.dp) =
    IconBadge(target.icon, Color(target.color), target.label, modifier, size)
