package com.telliyaki.blockly

import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BlocklyBridge {
    private val _workspaceJson = MutableStateFlow("{}")
    val workspaceJson: StateFlow<String> = _workspaceJson

    private val _autoSaveJson = MutableStateFlow("")
    val autoSaveJson: StateFlow<String> = _autoSaveJson

    private val _executionLog = MutableStateFlow("")
    val executionLog: StateFlow<String> = _executionLog

    private val _executionComplete = MutableStateFlow(false)
    val executionComplete: StateFlow<Boolean> = _executionComplete

    @JavascriptInterface
    fun getWorkspaceJson(): String {
        return _workspaceJson.value
    }

    @JavascriptInterface
    fun setWorkspaceJson(json: String) {
        _workspaceJson.value = json
    }

    @JavascriptInterface
    fun autoSave(json: String) {
        _autoSaveJson.value = json
    }

    @JavascriptInterface
    fun getAutoSavedJson(): String {
        return _autoSaveJson.value
    }

    @JavascriptInterface
    fun logExecution(message: String) {
        _executionLog.value = message
    }

    @JavascriptInterface
    fun executionComplete() {
        _executionComplete.value = true
    }

    fun resetExecutionState() {
        _executionComplete.value = false
        _executionLog.value = ""
    }
}
