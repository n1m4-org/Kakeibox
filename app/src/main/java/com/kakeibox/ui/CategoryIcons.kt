package com.kakeibox.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.Category

/** カテゴリごとの線画（Outlined）アイコン */
val Category.icon: ImageVector
    get() = when (this) {
        Category.FOOD -> Icons.Outlined.Restaurant
        Category.DAILY -> Icons.Outlined.ShoppingBasket
        Category.TRANSPORT -> Icons.Outlined.Train
        Category.HOUSING -> Icons.Outlined.Home
        Category.UTILITY -> Icons.Outlined.Lightbulb
        Category.COMMUNICATION -> Icons.Outlined.Smartphone
        Category.SOCIAL -> Icons.Outlined.LocalBar
        Category.HOBBY -> Icons.Outlined.SportsEsports
        Category.MEDICAL -> Icons.Outlined.LocalHospital
        Category.CLOTHES -> Icons.Outlined.Checkroom
        Category.OTHER_EXPENSE, Category.OTHER_INCOME -> Icons.Outlined.Category
        Category.SALARY -> Icons.Outlined.Payments
        Category.BONUS -> Icons.Outlined.CardGiftcard
        Category.SIDE_JOB -> Icons.Outlined.WorkOutline
        Category.ALLOWANCE -> Icons.Outlined.AccountBalanceWallet
    }

/** 「全体」の表示色 */
const val ALL_TARGET_COLOR = 0xFF00897B

val BudgetTarget.icon: ImageVector
    get() = when (this) {
        BudgetTarget.All -> Icons.Outlined.Savings
        is BudgetTarget.Of -> category.icon
    }

val BudgetTarget.color: Long
    get() = when (this) {
        BudgetTarget.All -> ALL_TARGET_COLOR
        is BudgetTarget.Of -> category.color
    }
