package com.kakeibox.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalConvenienceStore
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.kakeibox.data.BudgetTarget
import com.kakeibox.data.Category
import com.kakeibox.data.PaymentMethod

/**
 * カテゴリに使える線画（Outlined）アイコン。キーはカテゴリの設定に保存される。
 * ウィジェット用のベクター（res/drawable/ic_cat_<キー>.xml）はこの表から生成している。
 */
val CATEGORY_ICONS: Map<String, ImageVector> = linkedMapOf(
    "restaurant" to Icons.Outlined.Restaurant,
    "shopping_basket" to Icons.Outlined.ShoppingBasket,
    "train" to Icons.Outlined.Train,
    "home" to Icons.Outlined.Home,
    "lightbulb" to Icons.Outlined.Lightbulb,
    "smartphone" to Icons.Outlined.Smartphone,
    "local_bar" to Icons.Outlined.LocalBar,
    "sports_esports" to Icons.Outlined.SportsEsports,
    "local_hospital" to Icons.Outlined.LocalHospital,
    "checkroom" to Icons.Outlined.Checkroom,
    "category" to Icons.Outlined.Category,
    "payments" to Icons.Outlined.Payments,
    "card_giftcard" to Icons.Outlined.CardGiftcard,
    "work_outline" to Icons.Outlined.WorkOutline,
    "account_balance_wallet" to Icons.Outlined.AccountBalanceWallet,
    "local_cafe" to Icons.Outlined.LocalCafe,
    "fastfood" to Icons.Outlined.Fastfood,
    "local_grocery_store" to Icons.Outlined.LocalGroceryStore,
    "local_convenience_store" to Icons.Outlined.LocalConvenienceStore,
    "directions_car" to Icons.Outlined.DirectionsCar,
    "local_gas_station" to Icons.Outlined.LocalGasStation,
    "local_taxi" to Icons.Outlined.LocalTaxi,
    "flight" to Icons.Outlined.Flight,
    "hotel" to Icons.Outlined.Hotel,
    "pets" to Icons.Outlined.Pets,
    "child_care" to Icons.Outlined.ChildCare,
    "school" to Icons.Outlined.School,
    "menu_book" to Icons.AutoMirrored.Outlined.MenuBook,
    "music_note" to Icons.Outlined.MusicNote,
    "movie" to Icons.Outlined.Movie,
    "subscriptions" to Icons.Outlined.Subscriptions,
    "fitness_center" to Icons.Outlined.FitnessCenter,
    "spa" to Icons.Outlined.Spa,
    "content_cut" to Icons.Outlined.ContentCut,
    "redeem" to Icons.Outlined.Redeem,
    "cake" to Icons.Outlined.Cake,
    "celebration" to Icons.Outlined.Celebration,
    "volunteer_activism" to Icons.Outlined.VolunteerActivism,
    "shopping_bag" to Icons.Outlined.ShoppingBag,
    "electric_bolt" to Icons.Outlined.ElectricBolt,
    "water_drop" to Icons.Outlined.WaterDrop,
    "wifi" to Icons.Outlined.Wifi,
    "build" to Icons.Outlined.Build,
    "medication" to Icons.Outlined.Medication,
    "savings" to Icons.Outlined.Savings,
)

/** カテゴリの線画アイコン（不明なキーは汎用アイコン） */
val Category.icon: ImageVector
    get() = CATEGORY_ICONS[iconKey] ?: Icons.Outlined.Category

/** 追加するカテゴリに選べる色 */
val CATEGORY_COLORS: List<Long> = listOf(
    0xFFE57373, 0xFFF06292, 0xFFBA68C8, 0xFF7986CB, 0xFF64B5F6, 0xFF4DB6AC,
    0xFF81C784, 0xFFFFD54F, 0xFFFFB74D, 0xFFA1887F, 0xFF26A69A, 0xFFFF8A65,
)

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

val PaymentMethod.icon: ImageVector
    get() = when (this) {
        PaymentMethod.CASH -> Icons.Outlined.Payments
        PaymentMethod.CREDIT -> Icons.Outlined.CreditCard
        PaymentMethod.EMONEY -> Icons.Outlined.Contactless
        PaymentMethod.QR -> Icons.Outlined.QrCode2
        PaymentMethod.BANK -> Icons.Outlined.AccountBalance
        PaymentMethod.OTHER -> Icons.Outlined.MoreHoriz
    }
