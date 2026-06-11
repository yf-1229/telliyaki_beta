package com.telliyaki.ui.screen

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.telliyaki.blockly.BlocklyBridge
import com.telliyaki.blockly.BlocklyJsonParser
import com.telliyaki.storage.ProgramStorage
import com.telliyaki.validator.Hint
import com.telliyaki.validator.HintType
import com.telliyaki.validator.ProgramValidator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklyScreen(
    bridge: BlocklyBridge,
    storage: ProgramStorage,
    validator: ProgramValidator,
    parser: BlocklyJsonParser,
    onBackClick: () -> Unit,
    onPreviewClick: (String) -> Unit,
    onExecuteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val autoSaveJson by bridge.autoSaveJson.collectAsState()
    var showHintDialog by remember { mutableStateOf(false) }
    var hints by remember { mutableStateOf<List<Hint>>(emptyList()) }

    LaunchedEffect(autoSaveJson) {
        if (autoSaveJson.isNotEmpty()) {
            storage.autoSave(autoSaveJson)
        }
    }

    // ヒントダイアログ
    if (showHintDialog) {
        HintDialog(
            hints = hints,
            onDismiss = { showHintDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tello Block") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    // ヒントボタン
                    IconButton(onClick = {
                        scope.launch {
                            val json = bridge.getWorkspaceJson()
                            val commands = parser.parse(json)
                            hints = validator.validate(commands)
                            showHintDialog = true
                        }
                    }) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "ヒント")
                    }
                    // 保存ボタン
                    IconButton(onClick = {
                        scope.launch {
                            val json = bridge.getWorkspaceJson()
                            // TODO: 保存ダイアログを表示して名前を入力
                            storage.saveProgram("プログラム", json)
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                    // プレビューボタン
                    IconButton(onClick = {
                        scope.launch {
                            val json = bridge.getWorkspaceJson()
                            onPreviewClick(json)
                        }
                    }) {
                        Icon(Icons.Default.Preview, contentDescription = "プレビュー")
                    }
                    // 実行ボタン
                    IconButton(onClick = {
                        scope.launch {
                            val json = bridge.getWorkspaceJson()
                            onExecuteClick(json)
                        }
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "実行")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        BlocklyWebView(
            bridge = bridge,
            storage = storage,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
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
private fun HintItem(hint: Hint) {
    val icon = when (hint.type) {
        HintType.ERROR -> "❌"
        HintType.WARN -> "⚠️"
        HintType.INFO -> "ℹ️"
    }
    val color = when (hint.type) {
        HintType.ERROR -> MaterialTheme.colorScheme.error
        HintType.WARN -> MaterialTheme.colorScheme.tertiary
        HintType.INFO -> MaterialTheme.colorScheme.primary
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BlocklyWebView(
    bridge: BlocklyBridge,
    storage: ProgramStorage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                // デバッグを有効化
                WebView.setWebContentsDebuggingEnabled(true)
                
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true

                // JavaScript コンソールログを捕捉
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Log.d(
                                "BlocklyConsole",
                                "${it.messageLevel()}: ${it.message()} at ${it.sourceId()}:${it.lineNumber()}"
                            )
                        }
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // 保存済みプログラムがあれば読み込む
                        scope.launch {
                            val savedJson = storage.getAutoSavedJson()
                            if (!savedJson.isNullOrEmpty()) {
                                view?.evaluateJavascript("loadWorkspaceJson('$savedJson')", null)
                            }
                        }
                    }
                }

                addJavascriptInterface(bridge, "AndroidBridge")
                loadUrl("file:///android_asset/blockly/index.html")
            }
        },
        modifier = modifier
    )
}
