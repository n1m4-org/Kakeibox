package com.kakeibox.widget

import com.kakeibox.R
import com.kakeibox.data.BudgetTarget

/** カテゴリのアイコンのキー → ウィジェット用ベクター（ui/CategoryIcons.kt の表から生成） */
private val WIDGET_ICONS: Map<String, Int> = mapOf(
    "restaurant" to R.drawable.ic_cat_restaurant,
    "shopping_basket" to R.drawable.ic_cat_shopping_basket,
    "train" to R.drawable.ic_cat_train,
    "home" to R.drawable.ic_cat_home,
    "lightbulb" to R.drawable.ic_cat_lightbulb,
    "smartphone" to R.drawable.ic_cat_smartphone,
    "local_bar" to R.drawable.ic_cat_local_bar,
    "sports_esports" to R.drawable.ic_cat_sports_esports,
    "local_hospital" to R.drawable.ic_cat_local_hospital,
    "checkroom" to R.drawable.ic_cat_checkroom,
    "category" to R.drawable.ic_cat_category,
    "payments" to R.drawable.ic_cat_payments,
    "card_giftcard" to R.drawable.ic_cat_card_giftcard,
    "work_outline" to R.drawable.ic_cat_work_outline,
    "account_balance_wallet" to R.drawable.ic_cat_account_balance_wallet,
    "local_cafe" to R.drawable.ic_cat_local_cafe,
    "fastfood" to R.drawable.ic_cat_fastfood,
    "local_grocery_store" to R.drawable.ic_cat_local_grocery_store,
    "local_convenience_store" to R.drawable.ic_cat_local_convenience_store,
    "directions_car" to R.drawable.ic_cat_directions_car,
    "local_gas_station" to R.drawable.ic_cat_local_gas_station,
    "local_taxi" to R.drawable.ic_cat_local_taxi,
    "flight" to R.drawable.ic_cat_flight,
    "hotel" to R.drawable.ic_cat_hotel,
    "pets" to R.drawable.ic_cat_pets,
    "child_care" to R.drawable.ic_cat_child_care,
    "school" to R.drawable.ic_cat_school,
    "menu_book" to R.drawable.ic_cat_menu_book,
    "music_note" to R.drawable.ic_cat_music_note,
    "movie" to R.drawable.ic_cat_movie,
    "subscriptions" to R.drawable.ic_cat_subscriptions,
    "fitness_center" to R.drawable.ic_cat_fitness_center,
    "spa" to R.drawable.ic_cat_spa,
    "content_cut" to R.drawable.ic_cat_content_cut,
    "redeem" to R.drawable.ic_cat_redeem,
    "cake" to R.drawable.ic_cat_cake,
    "celebration" to R.drawable.ic_cat_celebration,
    "volunteer_activism" to R.drawable.ic_cat_volunteer_activism,
    "shopping_bag" to R.drawable.ic_cat_shopping_bag,
    "electric_bolt" to R.drawable.ic_cat_electric_bolt,
    "water_drop" to R.drawable.ic_cat_water_drop,
    "wifi" to R.drawable.ic_cat_wifi,
    "build" to R.drawable.ic_cat_build,
    "medication" to R.drawable.ic_cat_medication,
    "savings" to R.drawable.ic_cat_savings,
)

/** ウィジェット用のアイコン（アプリ内と同じ線画アイコンのベクター版） */
val BudgetTarget.widgetIcon: Int
    get() = when (this) {
        BudgetTarget.All -> R.drawable.ic_budget_all
        is BudgetTarget.Of -> WIDGET_ICONS[category.iconKey] ?: R.drawable.ic_cat_category
    }
