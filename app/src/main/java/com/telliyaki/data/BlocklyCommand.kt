package com.telliyaki.data

import kotlinx.serialization.Serializable

@Serializable
sealed class BlocklyCommand {
    @Serializable
    data object TakeOff : BlocklyCommand()

    @Serializable
    data object Land : BlocklyCommand()

    @Serializable
    data class MoveForward(val distance: Int) : BlocklyCommand()

    @Serializable
    data class MoveBack(val distance: Int) : BlocklyCommand()

    @Serializable
    data class MoveLeft(val distance: Int) : BlocklyCommand()

    @Serializable
    data class MoveRight(val distance: Int) : BlocklyCommand()

    @Serializable
    data class MoveUp(val distance: Int) : BlocklyCommand()

    @Serializable
    data class MoveDown(val distance: Int) : BlocklyCommand()

    @Serializable
    data class RotateCW(val degrees: Int) : BlocklyCommand()

    @Serializable
    data class RotateCCW(val degrees: Int) : BlocklyCommand()

    @Serializable
    data class Wait(val milliseconds: Int) : BlocklyCommand()

    @Serializable
    data class IfAltitude(
        val threshold: Int,
        val operator: String,
        val trueBranch: List<BlocklyCommand> = emptyList(),
        val falseBranch: List<BlocklyCommand> = emptyList()
    ) : BlocklyCommand()

    @Serializable
    data class Repeat(
        val times: Int,
        val body: List<BlocklyCommand> = emptyList()
    ) : BlocklyCommand()

    @Serializable
    data class Sequence(
        val children: List<BlocklyCommand> = emptyList()
    ) : BlocklyCommand()

    fun toDisplayString(): String = when (this) {
        is TakeOff -> "離陸"
        is Land -> "着陸"
        is MoveForward -> "前進 ${distance}cm"
        is MoveBack -> "後退 ${distance}cm"
        is MoveLeft -> "左移動 ${distance}cm"
        is MoveRight -> "右移動 ${distance}cm"
        is MoveUp -> "上昇 ${distance}cm"
        is MoveDown -> "下降 ${distance}cm"
        is RotateCW -> "時計回り回転 ${degrees}°"
        is RotateCCW -> "反時計回り回転 ${degrees}°"
        is Wait -> "待機 ${milliseconds}ms"
        is IfAltitude -> "もし高度が${threshold}cm${operatorToSymbol(operator)}なら"
        is Repeat -> "${times}回繰り返す"
        is Sequence -> "シーケンス"
    }

    private fun operatorToSymbol(op: String): String = when (op) {
        "GT" -> "より大きい"
        "LT" -> "より小さい"
        "GE" -> "以上"
        "LE" -> "以下"
        else -> op
    }
}
