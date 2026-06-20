package com.telliyaki.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.telliyaki.storage.ProgramStorage
import com.telliyaki.storage.SavedProgram
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramListScreen(
    storage: ProgramStorage,
    onBackClick: () -> Unit,
    onProgramSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var programs by remember { mutableStateOf<List<SavedProgram>>(emptyList()) }
    var showNewProgramDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<SavedProgram?>(null) }
    var newProgramName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            programs = storage.getAllPrograms()
        }
    }

    // 削除確認ダイアログ
    showDeleteDialog?.let { program ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("プログラムを削除") },
            text = { Text("「${program.name}」を削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val success = runCatching {
                                storage.deleteProgram(program.name)
                            }.isSuccess
                            if (success) {
                                programs = programs.filter { it.name != program.name }
                                snackbarHostState.showSnackbar("削除したよ ✅")
                            } else {
                                snackbarHostState.showSnackbar("削除に失敗したよ ❌")
                            }
                        }
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

    if (showNewProgramDialog) {
        AlertDialog(
            onDismissRequest = { showNewProgramDialog = false },
            title = { Text("新しいプログラム") },
            text = {
                OutlinedTextField(
                    value = newProgramName,
                    onValueChange = { newProgramName = it },
                    label = { Text("プログラム名") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmedName = newProgramName.trim()
                        if (trimmedName.isNotBlank()) {
                            onProgramSelect(trimmedName)
                            showNewProgramDialog = false
                            newProgramName = ""
                        }
                    }
                ) {
                    Text("作成")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProgramDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("プログラム一覧") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewProgramDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新規作成")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (programs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "保存されたプログラムはありません\n「+」ボタンで新規作成",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(programs) { program ->
                    ProgramItem(
                        program = program,
                        onClick = { onProgramSelect(program.name) },
                        onDelete = { showDeleteDialog = program }
                    )
                }
            }
        }
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

@Composable
private fun ProgramItem(
    program: SavedProgram,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val updatedAtText = remember(program.updatedAt) {
                    val dateTime = Instant.ofEpochMilli(program.updatedAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                    "更新: ${dateFormatter.format(dateTime)}"
                }
                Text(
                    text = updatedAtText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
