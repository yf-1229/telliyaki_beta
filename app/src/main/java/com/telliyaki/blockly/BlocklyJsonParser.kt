package com.telliyaki.blockly

import com.telliyaki.data.BlocklyCommand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BlocklyJsonParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonString: String): List<BlocklyCommand> {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            parseSequence(root)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseSequence(obj: JsonObject): List<BlocklyCommand> {
        val type = obj["type"]?.jsonPrimitive?.content ?: return emptyList()
        if (type != "sequence") return emptyList()

        val children = obj["children"]?.jsonArray ?: return emptyList()
        return children.map { parseCommand(it) }.filterNotNull()
    }

    private fun parseCommand(element: JsonElement): BlocklyCommand? {
        val obj = element.jsonObject
        val type = obj["type"]?.jsonPrimitive?.content ?: return null

        return when (type) {
            "takeoff" -> BlocklyCommand.TakeOff
            "land" -> BlocklyCommand.Land
            "move_forward" -> BlocklyCommand.MoveForward(obj["distance"]?.jsonPrimitive?.int ?: 20)
            "move_back" -> BlocklyCommand.MoveBack(obj["distance"]?.jsonPrimitive?.int ?: 20)
            "move_left" -> BlocklyCommand.MoveLeft(obj["distance"]?.jsonPrimitive?.int ?: 20)
            "move_right" -> BlocklyCommand.MoveRight(obj["distance"]?.jsonPrimitive?.int ?: 20)
            "move_up" -> BlocklyCommand.MoveUp(obj["distance"]?.jsonPrimitive?.int ?: 20)
            "move_down" -> BlocklyCommand.MoveDown(obj["distance"]?.jsonPrimitive?.int ?: 20)
            "rotate_cw" -> BlocklyCommand.RotateCW(obj["degrees"]?.jsonPrimitive?.int ?: 90)
            "rotate_ccw" -> BlocklyCommand.RotateCCW(obj["degrees"]?.jsonPrimitive?.int ?: 90)
            "wait" -> BlocklyCommand.Wait(obj["milliseconds"]?.jsonPrimitive?.int ?: 1000)
            "if_altitude" -> parseIfAltitude(obj)
            "repeat" -> parseRepeat(obj)
            else -> null
        }
    }

    private fun parseIfAltitude(obj: JsonObject): BlocklyCommand.IfAltitude {
        val threshold = obj["threshold"]?.jsonPrimitive?.int ?: 100
        val operator = obj["operator"]?.jsonPrimitive?.content ?: "GT"
        val trueBranch = parseCommandList(obj["true_branch"])
        val falseBranch = parseCommandList(obj["false_branch"])

        return BlocklyCommand.IfAltitude(
            threshold = threshold,
            operator = operator,
            trueBranch = trueBranch,
            falseBranch = falseBranch
        )
    }

    private fun parseRepeat(obj: JsonObject): BlocklyCommand.Repeat {
        val times = obj["times"]?.jsonPrimitive?.int ?: 3
        val body = parseCommandList(obj["body"])

        return BlocklyCommand.Repeat(
            times = times,
            body = body
        )
    }

    private fun parseCommandList(element: JsonElement?): List<BlocklyCommand> {
        if (element == null || element !is JsonArray) return emptyList()
        return element.map { parseCommand(it) }.filterNotNull()
    }
}
