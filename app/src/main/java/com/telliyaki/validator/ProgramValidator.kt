package com.telliyaki.validator

import com.telliyaki.data.BlocklyCommand

enum class HintType { INFO, WARN, ERROR }

data class Hint(
    val type: HintType,
    val title: String,
    val message: String
)

class ProgramValidator {
    fun validate(commands: List<BlocklyCommand>): List<Hint> {
        val hints = mutableListOf<Hint>()

        // トップレベルのコマンドを展開（Sequence の中身を取得）
        val topCommands = if (commands.size == 1 && commands[0] is BlocklyCommand.Sequence) {
            (commands[0] as BlocklyCommand.Sequence).children
        } else {
            commands
        }

        // 1. ブロックが空
        if (topCommands.isEmpty()) {
            hints.add(
                Hint(
                    HintType.ERROR,
                    "ブロックがないよ",
                    "左のメニューからブロックをえらんで、ワークスペースにおいてみよう！"
                )
            )
            return hints
        }

        // 2. 最初が離陸じゃない
        if (topCommands.first() !is BlocklyCommand.TakeOff) {
            hints.add(
                Hint(
                    HintType.WARN,
                    "離陸からはじめよう",
                    "さいしょに「離陸」ブロックをおくと、ドローンがそらにとぶよ"
                )
            )
        }

        // 3. 最後が着陸じゃない
        if (topCommands.last() !is BlocklyCommand.Land) {
            hints.add(
                Hint(
                    HintType.WARN,
                    "着陸でおわろう",
                    "さいごに「着陸」をおかないと、ドローンがもどってこないよ"
                )
            )
        }

        // 4. 離陸が2回以上
        val takeoffCount = topCommands.count { it is BlocklyCommand.TakeOff }
        if (takeoffCount > 1) {
            hints.add(
                Hint(
                    HintType.ERROR,
                    "離陸は1回だけ",
                    "2かいめの離陸はエラーになるよ。1かいだけにしよう"
                )
            )
        }

        // 5. 着陸より後にブロックがある
        val landIndex = topCommands.indexOfFirst { it is BlocklyCommand.Land }
        if (landIndex >= 0 && landIndex < topCommands.size - 1) {
            hints.add(
                Hint(
                    HintType.WARN,
                    "着陸したらおわり",
                    "着陸よりあとのブロックは実行されないよ。ぜんぶ着陸のまえにおこう"
                )
            )
        }

        // 6. 離陸だけで動かない
        val hasTakeoff = topCommands.any { it is BlocklyCommand.TakeOff }
        val hasMovement = topCommands.any {
            it is BlocklyCommand.MoveForward || it is BlocklyCommand.MoveBack ||
            it is BlocklyCommand.MoveLeft || it is BlocklyCommand.MoveRight ||
            it is BlocklyCommand.MoveUp || it is BlocklyCommand.MoveDown ||
            it is BlocklyCommand.RotateCW || it is BlocklyCommand.RotateCCW
        }
        if (hasTakeoff && !hasMovement) {
            hints.add(
                Hint(
                    HintType.INFO,
                    "うごかしてみよう",
                    "離陸したら、前進や回転でドローンをうごかしてみよう！"
                )
            )
        }

        // 7. 入れ子ブロックのチェック
        checkNestedCommands(topCommands, hints)

        return hints
    }

    private fun checkNestedCommands(commands: List<BlocklyCommand>, hints: MutableList<Hint>) {
        for (cmd in commands) {
            when (cmd) {
                is BlocklyCommand.Repeat -> {
                    // Repeatが1回
                    if (cmd.times == 1) {
                        hints.add(
                            Hint(
                                HintType.INFO,
                                "1回のくりかえし？",
                                "くりかえしは2かいいじょうにしよう。1かいだといみがないよ"
                            )
                        )
                    }
                    checkNestedCommands(cmd.body, hints)
                }
                is BlocklyCommand.IfAltitude -> {
                    checkNestedCommands(cmd.trueBranch, hints)
                    checkNestedCommands(cmd.falseBranch, hints)
                }
                is BlocklyCommand.Sequence -> {
                    checkNestedCommands(cmd.children, hints)
                }
                is BlocklyCommand.MoveForward,
                is BlocklyCommand.MoveBack,
                is BlocklyCommand.MoveLeft,
                is BlocklyCommand.MoveRight,
                is BlocklyCommand.MoveUp,
                is BlocklyCommand.MoveDown -> {
                    // 移動距離が大きすぎ
                    val distance = when (cmd) {
                        is BlocklyCommand.MoveForward -> cmd.distance
                        is BlocklyCommand.MoveBack -> cmd.distance
                        is BlocklyCommand.MoveLeft -> cmd.distance
                        is BlocklyCommand.MoveRight -> cmd.distance
                        is BlocklyCommand.MoveUp -> cmd.distance
                        is BlocklyCommand.MoveDown -> cmd.distance
                        else -> 0
                    }
                    if (distance > 500) {
                        hints.add(
                            Hint(
                                HintType.WARN,
                                "ちかくにしよう",
                                "500cmはとおすぎるよ。ドローンが見えなくなっちゃう"
                            )
                        )
                    }
                }
                else -> {}
            }
        }
    }
}
