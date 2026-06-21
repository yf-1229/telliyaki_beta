package com.telliyaki.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telliyaki.blockeditor.BlockDefinition
import com.telliyaki.blockeditor.BlockDefinitions
import com.telliyaki.blockeditor.BlockWorkspaceViewModel
import com.telliyaki.blockeditor.WorkspaceBlock
import com.telliyaki.data.BlocklyCommand
import com.telliyaki.network.TelloUdpClient
import com.telliyaki.storage.ProgramStorage
import com.telliyaki.validator.Hint
import com.telliyaki.validator.ProgramValidator
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorScreen(
    viewModel: BlockWorkspaceViewModel,
    storage: ProgramStorage,
    validator: ProgramValidator,
    telloClient: TelloUdpClient,
    programName: String,
    onBackClick: () -> Unit,
    onPreviewClick: (List<BlocklyCommand>) -> Unit,
    onExecuteClick: (List<BlocklyCommand>) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val blocks by viewModel.blocks.collectAsState()
    val maxBlocks by viewModel.maxBlocks.collectAsState()

    // 選択されたプログラム名が変わったら自動で読み込み
    LaunchedEffect(programName) {
        val json = storage.loadProgram(programName)
        if (json != null) {
            viewModel.loadFromJson(json)
        } else {
            viewModel.clear()
        }
    }
    
    var selectedCategory by remember { mutableStateOf("基本操作") }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showParamEditor by remember { mutableStateOf<String?>(null) }
    var showHintDialog by remember { mutableStateOf(false) }
    var hints by remember { mutableStateOf<List<Hint>>(emptyList()) }
    var showTelloDisconnectedDialog by remember { mutableStateOf(false) }
    var showTakeoffLandDialog by remember { mutableStateOf(false) }
    var pendingCommands by remember { mutableStateOf<List<BlocklyCommand>?>(null) }
    
    // ヒントダイアログ
    if (showHintDialog) {
        HintDialog(
            hints = hints,
            onDismiss = { showHintDialog = false }
        )
    }
    
    // 削除確認ダイアログ
    showDeleteDialog?.let { blockId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("ブロックを削除") },
            text = { Text("このブロックを削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeBlock(blockId)
                        showDeleteDialog = null
                    }
                ) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("キャンセル")
                }
            }
        )
    }
    
    // パラメータ編集ボトムシート
    showParamEditor?.let { blockId ->
        ParamEditorBottomSheet(
            viewModel = viewModel,
            blockId = blockId,
            onDismiss = { showParamEditor = null },
            onValueChange = { paramName, value ->
                viewModel.updateParam(blockId, paramName, value)
            }
        )
    }

    // Tello未接続ダイアログ
    if (showTelloDisconnectedDialog) {
        AlertDialog(
            onDismissRequest = { showTelloDisconnectedDialog = false },
            title = { Text("Tello未接続") },
            text = { Text("Telloが接続されていません。シミュレータで実行しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTelloDisconnectedDialog = false
                        pendingCommands?.let(onExecuteClick)
                        pendingCommands = null
                    }
                ) {
                    Text("シミュレータで実行")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTelloDisconnectedDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 離陸・着陸自動追加確認ダイアログ
    if (showTakeoffLandDialog) {
        AlertDialog(
            onDismissRequest = { showTakeoffLandDialog = false },
            title = { Text("離陸・着陸を追加") },
            text = { Text("離陸/着陸ブロックを追加して実行しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTakeoffLandDialog = false
                        viewModel.autoAddTakeoffAndLand()
                        val commands = viewModel.toBlocklyCommands()
                        if (!telloClient.isConnected()) {
                            pendingCommands = commands
                            showTelloDisconnectedDialog = true
                        } else {
                            onExecuteClick(commands)
                        }
                    }
                ) {
                    Text("追加して実行")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTakeoffLandDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(programName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    // ヒントボタン
                    IconButton(onClick = {
                        val commands = viewModel.toBlocklyCommands()
                        hints = validator.validate(commands)
                        showHintDialog = true
                    }) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "ヒント")
                    }
                    // 保存ボタン
                    IconButton(onClick = {
                        scope.launch {
                            val success = withContext(NonCancellable) {
                                runCatching {
                                    val json = viewModel.toJson()
                                    storage.saveProgram(programName, json)
                                }.isSuccess
                            }
                            snackbarHostState.showSnackbar(
                                if (success) "保存したよ ✅" else "保存に失敗したよ ❌"
                            )
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                    // プレビューボタン
                    IconButton(onClick = {
                        val commands = viewModel.toBlocklyCommands()
                        onPreviewClick(commands)
                    }) {
                        Icon(Icons.Default.Preview, contentDescription = "プレビュー")
                    }
                    // 実行ボタン
                    IconButton(onClick = {
                        val commands = viewModel.toBlocklyCommands()
                        if (commands.isEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("ブロックがないよ")
                            }
                            return@IconButton
                        }

                        // 1. 離陸・着陸チェック
                        val firstIsTakeoff = commands.firstOrNull() is BlocklyCommand.TakeOff
                        val lastIsLand = commands.lastOrNull() is BlocklyCommand.Land
                        if (!firstIsTakeoff || !lastIsLand) {
                            showTakeoffLandDialog = true
                            return@IconButton
                        }

                        // 2. Tello接続チェック
                        if (!telloClient.isConnected()) {
                            pendingCommands = commands
                            showTelloDisconnectedDialog = true
                            return@IconButton
                        }

                        onExecuteClick(commands)
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "実行")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ワークスペース
            WorkspaceArea(
                blocks = blocks,
                maxBlocks = maxBlocks,
                onBlockClick = { block ->
                    if (block.definition.params.isNotEmpty()) {
                        showParamEditor = block.id
                    }
                },
                onBlockLongClick = { block ->
                    showDeleteDialog = block.id
                },
                onMoveUp = { block ->
                    viewModel.moveBlockUp(block.id)
                },
                onMoveDown = { block ->
                    viewModel.moveBlockDown(block.id)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            
            // カテゴリタブ
            CategoryTabs(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            // ブロック一覧
            BlockPalette(
                category = selectedCategory,
                onBlockClick = { definition ->
                    viewModel.addBlock(definition)
                }
            )
        }
    }
}

@Composable
private fun WorkspaceArea(
    blocks: List<WorkspaceBlock>,
    maxBlocks: Int,
    onBlockClick: (WorkspaceBlock) -> Unit,
    onBlockLongClick: (WorkspaceBlock) -> Unit,
    onMoveUp: (WorkspaceBlock) -> Unit,
    onMoveDown: (WorkspaceBlock) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        if (blocks.isEmpty()) {
            // プレースホルダー
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📦",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "ここにブロックを置いてね",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "下のブロックをタップして追加",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
                    WorkspaceBlockItem(
                        block = block,
                        isFirst = index == 0,
                        isLast = index == blocks.size - 1,
                        onClick = { onBlockClick(block) },
                        onLongClick = { onBlockLongClick(block) },
                        onMoveUp = { onMoveUp(block) },
                        onMoveDown = { onMoveDown(block) }
                    )
                }
                
                // 残りブロック数表示
                item {
                    Text(
                        text = "残り: ${maxBlocks - blocks.size} / $maxBlocks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkspaceBlockItem(
    block: WorkspaceBlock,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = block.definition.color,
        label = "blockColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = animatedColor.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 色インジケータ
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(animatedColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // ブロックテキスト
            Text(
                text = block.getDisplayText(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            // 操作ボタン: 上 → 下 → 削除
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 上移動
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "上に移動",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 下移動
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "下に移動",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 削除
                IconButton(
                    onClick = onLongClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "削除",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = remember(selectedCategory) {
            BlockDefinitions.categories.indexOf(selectedCategory).takeIf { it >= 0 } ?: 0
        },
        modifier = modifier,
        edgePadding = 8.dp
    ) {
        BlockDefinitions.categories.forEach { category ->
            Tab(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                text = { Text(category) }
            )
        }
    }
}

@Composable
private fun BlockPalette(
    category: String,
    onBlockClick: (BlockDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val blocks = BlockDefinitions.getByCategory(category)
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(blocks) { definition ->
            PaletteBlockItem(
                definition = definition,
                onClick = { onBlockClick(definition) }
            )
        }
    }
}

@Composable
private fun PaletteBlockItem(
    definition: BlockDefinition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = definition.color,
        label = "paletteBlockColor"
    )
    
    Card(
        modifier = modifier
            .width(120.dp)
            .height(60.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = animatedColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = definition.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParamEditorBottomSheet(
    viewModel: BlockWorkspaceViewModel,
    blockId: String,
    onDismiss: () -> Unit,
    onValueChange: (String, Int) -> Unit
) {
    val blocks by viewModel.blocks.collectAsState()
    val block = blocks.find { it.id == blockId } ?: run {
        onDismiss()
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = block.definition.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            block.definition.params.forEach { param ->
                val currentValue = block.paramValues[param.name] ?: param.defaultValue
                var sliderPos by remember(currentValue) { mutableFloatStateOf(currentValue.toFloat()) }

                Column {
                    Text(
                        text = "${param.label}: ${sliderPos.roundToInt()}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Slider(
                        value = sliderPos,
                        onValueChange = { sliderPos = it },
                        onValueChangeFinished = {
                            onValueChange(param.name, sliderPos.roundToInt())
                        },
                        valueRange = param.min.toFloat()..param.max.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${param.min}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${param.max}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HintDialog(
    hints: List<Hint>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "💡 ヒント",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hints.isEmpty()) {
                    Text(
                        text = "ばっちり！エラーはないよ ✅",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    hints.forEach { hint ->
                        HintItem(hint = hint)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("わかった！")
            }
        }
    )
}

@Composable
private fun HintItem(hint: com.telliyaki.validator.Hint) {
    val icon = when (hint.type) {
        com.telliyaki.validator.HintType.ERROR -> "❌"
        com.telliyaki.validator.HintType.WARN -> "⚠️"
        com.telliyaki.validator.HintType.INFO -> "ℹ️"
    }
    val color = when (hint.type) {
        com.telliyaki.validator.HintType.ERROR -> MaterialTheme.colorScheme.error
        com.telliyaki.validator.HintType.WARN -> MaterialTheme.colorScheme.tertiary
        com.telliyaki.validator.HintType.INFO -> MaterialTheme.colorScheme.primary
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$icon ${hint.title}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = hint.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
