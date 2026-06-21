package com.telliyaki.blockeditor

import android.util.Log
import androidx.lifecycle.ViewModel
import com.telliyaki.data.BlocklyCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ブロックエディタのワークスペース状態管理
 */
class BlockWorkspaceViewModel : ViewModel() {
    
    private val _blocks = MutableStateFlow<List<WorkspaceBlock>>(emptyList())
    val blocks: StateFlow<List<WorkspaceBlock>> = _blocks
    
    private val _maxBlocks = MutableStateFlow(25)
    val maxBlocks: StateFlow<Int> = _maxBlocks
    
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    /**
     * ブロックを末尾に追加
     */
    fun addBlock(definition: BlockDefinition): Boolean {
        if (_blocks.value.size >= _maxBlocks.value) {
            return false
        }
        val defaultParams = definition.params.associate { it.name to it.defaultValue }
        val newBlock = WorkspaceBlock(
            definition = definition,
            paramValues = defaultParams
        )
        _blocks.value = _blocks.value + newBlock
        return true
    }
    
    /**
     * ブロックを削除
     */
    fun removeBlock(blockId: String) {
        _blocks.value = _blocks.value.filter { it.id != blockId }
    }

    /**
     * ブロックを上に移動
     */
    fun moveBlockUp(blockId: String): Boolean {
        val list = _blocks.value.toMutableList()
        val index = list.indexOfFirst { it.id == blockId }
        if (index <= 0) return false
        java.util.Collections.swap(list, index, index - 1)
        _blocks.value = list
        return true
    }

    /**
     * ブロックを下に移動
     */
    fun moveBlockDown(blockId: String): Boolean {
        val list = _blocks.value.toMutableList()
        val index = list.indexOfFirst { it.id == blockId }
        if (index < 0 || index >= list.size - 1) return false
        java.util.Collections.swap(list, index, index + 1)
        _blocks.value = list
        return true
    }

    /**
     * 離陸・着陸を自動補完
     * @return 補完したかどうか（true = 変更あり）
     */
    fun autoAddTakeoffAndLand(): Boolean {
        val current = _blocks.value.toMutableList()
        var changed = false

        // 先頭に離陸がなければ追加
        if (current.isEmpty() || current.first().definition.type != "takeoff") {
            val takeoffDef = BlockDefinitions.getByType("takeoff") ?: return false
            current.add(0, WorkspaceBlock(definition = takeoffDef))
            changed = true
        }

        // 末尾に着陸がなければ追加
        if (current.last().definition.type != "land") {
            val landDef = BlockDefinitions.getByType("land") ?: return changed
            current.add(WorkspaceBlock(definition = landDef))
            changed = true
        }

        if (changed) {
            _blocks.value = current
        }
        return changed
    }

    /**
     * パラメータを更新
     */
    fun updateParam(blockId: String, paramName: String, value: Int) {
        _blocks.value = _blocks.value.map { block ->
            if (block.id == blockId) {
                block.copy(paramValues = block.paramValues + (paramName to value))
            } else {
                block
            }
        }
    }
    
    /**
     * 最大ブロック数を設定
     */
    fun setMaxBlocks(max: Int) {
        _maxBlocks.value = max.coerceIn(1, 100)
    }
    
    /**
     * ワークスペースをクリア
     */
    fun clear() {
        _blocks.value = emptyList()
    }
    
    /**
     * BlocklyCommandのリストに変換
     */
    fun toBlocklyCommands(): List<BlocklyCommand> {
        return _blocks.value.map { block ->
            convertToCommand(block)
        }
    }
    
    private fun convertToCommand(block: WorkspaceBlock): BlocklyCommand {
        return when (block.definition.type) {
            "takeoff" -> BlocklyCommand.TakeOff
            "land" -> BlocklyCommand.Land
            "move_forward" -> BlocklyCommand.MoveForward(
                block.paramValue("distance")
            )
            "move_back" -> BlocklyCommand.MoveBack(
                block.paramValue("distance")
            )
            "move_left" -> BlocklyCommand.MoveLeft(
                block.paramValue("distance")
            )
            "move_right" -> BlocklyCommand.MoveRight(
                block.paramValue("distance")
            )
            "move_up" -> BlocklyCommand.MoveUp(
                block.paramValue("distance")
            )
            "move_down" -> BlocklyCommand.MoveDown(
                block.paramValue("distance")
            )
            "rotate_cw" -> BlocklyCommand.RotateCW(
                block.paramValue("degrees")
            )
            "rotate_ccw" -> BlocklyCommand.RotateCCW(
                block.paramValue("degrees")
            )
            "wait" -> BlocklyCommand.Wait(
                block.paramValue("milliseconds")
            )
            else -> {
                Log.w("BlockWorkspaceViewModel", "Unknown block type: ${block.definition.type}")
                BlocklyCommand.Wait(0) // fallback
            }
        }
    }

    private fun WorkspaceBlock.paramValue(name: String): Int {
        val param = definition.params.find { it.name == name }
        return paramValues[name] ?: param?.defaultValue ?: 0
    }
    
    /**
     * ワークスペースをJSON文字列にシリアライズ（保存用）
     */
    fun toJson(): String {
        val data = _blocks.value.map { block ->
            BlockData(
                type = block.definition.type,
                params = block.paramValues
            )
        }
        return json.encodeToString(data)
    }
    
    /**
     * JSON文字列からワークスペースを復元
     */
    fun loadFromJson(jsonString: String) {
        try {
            val data = json.decodeFromString<List<BlockData>>(jsonString)
            _blocks.value = data.mapNotNull { blockData ->
                val definition = BlockDefinitions.getByType(blockData.type) ?: return@mapNotNull null
                WorkspaceBlock(
                    definition = definition,
                    paramValues = blockData.params
                )
            }
        } catch (e: Exception) {
            _blocks.value = emptyList()
        }
    }
    
    /**
     * 保存用のデータクラス
     */
    @kotlinx.serialization.Serializable
    private data class BlockData(
        val type: String,
        val params: Map<String, Int>
    )
}
