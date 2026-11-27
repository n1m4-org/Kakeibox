package com.kakeibox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakeibox.data.Category
import com.kakeibox.data.CategoryConfig
import com.kakeibox.data.TxType
import com.kakeibox.ui.CATEGORY_COLORS
import com.kakeibox.ui.CATEGORY_ICONS
import com.kakeibox.ui.MainViewModel
import com.kakeibox.ui.components.CategoryBadge
import com.kakeibox.ui.theme.AcrylicPanel
import com.kakeibox.ui.theme.panelPadding
import kotlinx.coroutines.launch

/** カテゴリの追加・編集・削除・並べ替え・非表示 */
@Composable
fun CategoriesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val config by vm.categoryConfig.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // 使われていて削除できなかったカテゴリと件数
    var blocked by remember { mutableStateOf<Pair<Category, Int>?>(null) }
    CategoriesContent(
        config = config,
        onBack = onBack,
        onAdd = vm::addCategory,
        onUpdate = vm::updateCategory,
        onHidden = vm::setCategoryHidden,
        onMove = vm::moveCategory,
        onDelete = { c ->
            scope.launch {
                if (!vm.deleteCategory(c)) blocked = c to vm.categoryUsage(c)
            }
        },
    )
    blocked?.let { (c, count) ->
        AlertDialog(
            onDismissRequest = { blocked = null },
            title = { Text("「${c.label}」は削除できません") },
            text = { Text("明細・テンプレート・定期の収支で${count}件使われています。非表示にすると、入力の候補には出なくなります（これまでの記録はそのまま）。") },
            confirmButton = { TextButton(onClick = { vm.setCategoryHidden(c, true); blocked = null }) { Text("非表示にする") } },
            dismissButton = { TextButton(onClick = { blocked = null }) { Text("閉じる") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoriesContent(
    config: CategoryConfig,
    onBack: () -> Unit,
    onAdd: (label: String, type: TxType, color: Long, icon: String) -> Unit,
    onUpdate: (Category, label: String, color: Long, icon: String) -> Unit,
    onHidden: (Category, Boolean) -> Unit,
    onMove: (Category, Int) -> Unit,
    onDelete: (Category) -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }
    val visible = config.visible(type)
    val hidden = config.ordered(type).filter { config.isHidden(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("カテゴリ") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("追加") })
        },
    ) { padding ->
        AcrylicPanel(Modifier.padding(padding).panelPadding()) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    TypeToggle(type, onChange = { type = it }, modifier = Modifier.padding(16.dp))
                }
                items(visible, key = { "v_" + it.key }) { c ->
                    val i = visible.indexOf(c)
                    ListItem(
                        modifier = Modifier.clickable(enabled = !c.builtIn) { editing = c },
                        leadingContent = { CategoryBadge(c) },
                        headlineContent = { Text(c.label) },
                        supportingContent = if (!c.builtIn) {{ Text("タップで編集") }} else null,
                        trailingContent = {
                            Row3 {
                                IconButton(onClick = { onMove(c, -1) }, enabled = i > 0) { Icon(Icons.Outlined.KeyboardArrowUp, "上へ") }
                                IconButton(onClick = { onMove(c, 1) }, enabled = i < visible.lastIndex) { Icon(Icons.Outlined.KeyboardArrowDown, "下へ") }
                                IconButton(onClick = { onHidden(c, true) }, enabled = !c.isFallback) {
                                    Icon(Icons.Outlined.VisibilityOff, "非表示にする")
                                }
                            }
                        },
                    )
                }
                if (hidden.isNotEmpty()) {
                    item {
                        Text(
                            "非表示（入力の候補に出ません。これまでの記録はそのまま）",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(hidden, key = { "h_" + it.key }) { c ->
                        ListItem(
                            modifier = Modifier.clickable(enabled = !c.builtIn) { editing = c },
                            leadingContent = { CategoryBadge(c) },
                            headlineContent = { Text(c.label, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            trailingContent = {
                                IconButton(onClick = { onHidden(c, false) }) { Icon(Icons.Outlined.Visibility, "表示する") }
                            },
                        )
                    }
                }
                item {
                    Text(
                        "「その他」は受け皿のため非表示にできません。既定のカテゴリは並べ替えと非表示のみできます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp).padding(bottom = 72.dp),
                    )
                }
            }
        }
    }

    if (adding) {
        CategoryDialog(
            title = "${if (type == TxType.EXPENSE) "支出" else "収入"}のカテゴリを追加",
            initial = null,
            onDismiss = { adding = false },
            onSave = { label, color, icon -> onAdd(label, type, color, icon); adding = false },
        )
    }
    editing?.let { c ->
        CategoryDialog(
            title = "カテゴリを編集",
            initial = c,
            onDismiss = { editing = null },
            onSave = { label, color, icon -> onUpdate(c, label, color, icon); editing = null },
            onDelete = { onDelete(c); editing = null },
        )
    }
}

@Composable
private fun Row3(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) { content() }
}

/** カテゴリの名前・アイコン・色の入力 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryDialog(
    title: String,
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (label: String, color: Long, icon: String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: CATEGORY_COLORS.first()) }
    var icon by remember { mutableStateOf(initial?.iconKey ?: "category") }
    // 見本（選んだ色・アイコン）
    val preview = Category("preview", label.ifBlank { "名前" }, TxType.EXPENSE, color, icon)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryBadge(preview, size = 44.dp)
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it.take(12) },
                        label = { Text("名前") },
                        singleLine = true,
                    )
                }
                Text("色", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATEGORY_COLORS.forEach { c ->
                        Box(
                            Modifier.size(32.dp).background(Color(c), CircleShape)
                                .then(if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { color = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (c == color) Icon(Icons.Outlined.Check, "選択中", Modifier.size(18.dp), tint = Color.White)
                        }
                    }
                }
                Text("アイコン", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    Modifier.heightIn(max = 260.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CATEGORY_ICONS.forEach { (key, vector) ->
                        val selected = key == icon
                        Box(
                            Modifier.size(40.dp)
                                .background(
                                    if (selected) Color(color).copy(alpha = 0.25f) else Color.Transparent,
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { icon = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(vector, null, tint = if (selected) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                        Text("このカテゴリを削除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = label.isNotBlank(), onClick = { onSave(label, color, icon) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}
