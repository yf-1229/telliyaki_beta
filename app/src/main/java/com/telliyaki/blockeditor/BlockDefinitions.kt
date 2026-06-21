package com.telliyaki.blockeditor

import androidx.compose.ui.graphics.Color

/**
 * ブロックパラメータの定義
 */
data class BlockParam(
    val name: String,           // "distance", "degrees" など
    val label: String,          // "cm", "°", "ms" など
    val defaultValue: Int,
    val min: Int,
    val max: Int
)

/**
 * ブロックの定義（宣言的）
 */
data class BlockDefinition(
    val type: String,           // "takeoff", "move_forward" など
    val label: String,          // 表示名 "前進"
    val color: Color,           // ブロックの色
    val category: String,       // "基本操作", "移動" など
    val params: List<BlockParam> = emptyList()
)

/**
 * ワークスペース上のブロックインスタンス
 */
data class WorkspaceBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    val definition: BlockDefinition,
    val paramValues: Map<String, Int> = emptyMap()
) {
    fun getDisplayText(): String {
        if (definition.params.isEmpty()) {
            return definition.label
        }
        val paramTexts = definition.params.map { param ->
            val value = paramValues[param.name] ?: param.defaultValue
            "$value${param.label}"
        }
        return "${definition.label} ${paramTexts.joinToString(", ")}"
    }
}

/**
 * 全ブロック定義
 */
object BlockDefinitions {
    val all: List<BlockDefinition> = listOf(
        // 基本操作
        BlockDefinition(
            type = "takeoff",
            label = "離陸する",
            color = Color(0xFF66BB6A),
            category = "基本操作"
        ),
        BlockDefinition(
            type = "land",
            label = "着陸する",
            color = Color(0xFF66BB6A),
            category = "基本操作"
        ),
        
        // 移動
        BlockDefinition(
            type = "move_forward",
            label = "前進",
            color = Color(0xFF42A5F5),
            category = "移動",
            params = listOf(BlockParam("distance", "cm", 20, 20, 500))
        ),
        BlockDefinition(
            type = "move_back",
            label = "後退",
            color = Color(0xFF42A5F5),
            category = "移動",
            params = listOf(BlockParam("distance", "cm", 20, 20, 500))
        ),
        BlockDefinition(
            type = "move_left",
            label = "左移動",
            color = Color(0xFF42A5F5),
            category = "移動",
            params = listOf(BlockParam("distance", "cm", 20, 20, 500))
        ),
        BlockDefinition(
            type = "move_right",
            label = "右移動",
            color = Color(0xFF42A5F5),
            category = "移動",
            params = listOf(BlockParam("distance", "cm", 20, 20, 500))
        ),
        BlockDefinition(
            type = "move_up",
            label = "上昇",
            color = Color(0xFF42A5F5),
            category = "移動",
            params = listOf(BlockParam("distance", "cm", 20, 20, 500))
        ),
        BlockDefinition(
            type = "move_down",
            label = "下降",
            color = Color(0xFF42A5F5),
            category = "移動",
            params = listOf(BlockParam("distance", "cm", 20, 20, 500))
        ),
        
        // 回転
        BlockDefinition(
            type = "rotate_cw",
            label = "時計回り回転",
            color = Color(0xFFAB47BC),
            category = "回転",
            params = listOf(BlockParam("degrees", "°", 90, 1, 360))
        ),
        BlockDefinition(
            type = "rotate_ccw",
            label = "反時計回り回転",
            color = Color(0xFFAB47BC),
            category = "回転",
            params = listOf(BlockParam("degrees", "°", 90, 1, 360))
        ),
        
        // 待機
        BlockDefinition(
            type = "wait",
            label = "待機",
            color = Color(0xFFFFCA28),
            category = "待機",
            params = listOf(BlockParam("milliseconds", "ms", 1000, 100, 30000))
        )
    )
    
    val categories: List<String> = listOf("基本操作", "移動", "回転", "待機")
    
    fun getByType(type: String): BlockDefinition? = all.find { it.type == type }
    
    fun getByCategory(category: String): List<BlockDefinition> = 
        all.filter { it.category == category }
}
