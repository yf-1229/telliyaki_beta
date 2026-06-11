// Tello カスタムブロック定義
'use strict';

Blockly.defineBlocksWithJsonArray([
  // === 基本操作 ===
  {
    "type": "tello_takeoff",
    "message0": "離陸する",
    "previousStatement": null,
    "nextStatement": null,
    "colour": 160,
    "tooltip": "Telloを離陸させます",
    "helpUrl": ""
  },
  {
    "type": "tello_land",
    "message0": "着陸する",
    "previousStatement": null,
    "nextStatement": null,
    "colour": 160,
    "tooltip": "Telloを着陸させます",
    "helpUrl": ""
  },

  // === 移動 ===
  {
    "type": "tello_move_forward",
    "message0": "前進 %1 cm",
    "args0": [
      {
        "type": "field_number",
        "name": "DISTANCE",
        "value": 20,
        "min": 20,
        "max": 500
      }
    ],
    "previousStatement": null,
    "nextStatement": null,
    "colour": 210,
    "tooltip": "指定距離だけ前進します",
    "helpUrl": ""
  },
  {
    "type": "tello_move_back",
    "message0": "後退 %1 cm",
    "args0": [
      {
        "type": "field_number",
        "name": "DISTANCE",
        "value": 20,
        "min": 20,
        "max": 500
      }
    ],
    "previousStatement": null,
    "nextStatement": null,
    "colour": 210,
    "tooltip": "指定距離だけ後退します",
    "helpUrl": ""
  },
  {
    "type": "tello_move_left",
    "message0": "左移動 %1 cm",
    "args0": [
      {
        "type": "field_number",
        "name": "DISTANCE",
        "value": 20,
        "min": 20,
        "max": 500
      }
    ],
    "previousStatement": null,
    "nextStatement": null,
    "colour": 210,
    "tooltip": "指定距離だけ左に移動します",
    "helpUrl": ""
  },
  {
    "type": "tello_move_right",
    "message0": "右移動 %1 cm",
    "args0": [
      {
        "type": "field_number",
        "name": "DISTANCE",
        "value": 20,
        "min": 20,
        "max": 500
      }
    ],
    "previousStatement": null,
    "nextStatement": null,
    "colour": 210,
    "tooltip": "指定距離だけ右に移動します",
    "helpUrl": ""
  },
  {
    "type": "tello_move_up",
    "message0": "上昇 %1 cm",
    "args0": [
      {
        "type": "field_number",
        "name": "DISTANCE",
        "value": 20,
        "min": 20,
        "max": 500
      }
    ],
    "previousStatement": null,
    "nextStatement": null,
    "colour": 210,
    "tooltip": "指定距離だけ上昇します",
    "helpUrl": ""
  },
  {
    "type": "tello_move_down",
    "message0": "下降 %1 cm",
    "args0": [
      {
        "type": "field_number",
        "name": "DISTANCE",
        "value": 20,
        "min": 20,
        "max": 500
      }
    ],
    "previousStatement": null,
    "nextStatement": null,
    "colour": 210,
    "tooltip": "指定距離だけ下降します",
    "helpUrl": ""
  },

  // === 回転 ===
  {
    "type": "tello_rotate_cw",
    "message0": "時計回りに %1 度回転",
    "args0": [
      {
        "type": "field_number",
        "name": "DEGREES",
        "value": 90,
        "min": 1,
        "max": 360
      }
    ],
    "previousStatement": null,
    "nextStatement": null,
    "colour": 330,
    "tooltip": "時計回りに指定角度だけ回転します",
    "helpUrl": ""
  },
  {
    "type": "tello_rotate_ccw",
    "message0": "反時計回りに %1 度回転",
    "args0": [
      {
        "type": "field_number",
        "name": "DEGREES",
        "value": 90,
        "min": 1,
        "max": 360
      }
    ],
    "previousStatement": null,
    "nextStatement": null,
    "colour": 330,
    "tooltip": "反時計回りに指定角度だけ回転します",
    "helpUrl": ""
  },

  // === 待機 ===
  {
    "type": "tello_wait",
    "message0": "待機 %1 ミリ秒",
    "args0": [
      {
        "type": "field_number",
        "name": "MILLISECONDS",
        "value": 1000,
        "min": 100,
        "max": 30000
      }
    ],
    "previousStatement": null,
    "nextStatement": null,
    "colour": 60,
    "tooltip": "指定時間だけ待機します",
    "helpUrl": ""
  },

  // === 条件分岐 ===
  {
    "type": "tello_if_altitude",
    "message0": "もし高度が %1 cmより %2 なら %3 %4",
    "args0": [
      {
        "type": "field_number",
        "name": "THRESHOLD",
        "value": 100,
        "min": 0,
        "max": 200
      },
      {
        "type": "field_dropdown",
        "name": "OPERATOR",
        "options": [
          ["大きい", "GT"],
          ["小さい", "LT"],
          ["以上", "GE"],
          ["以下", "LE"]
        ]
      },
      {
        "type": "input_dummy"
      },
      {
        "type": "input_statement",
        "name": "TRUE_BRANCH"
      }
    ],
    "message1": "そうでなければ %1",
    "args1": [
      {
        "type": "input_statement",
        "name": "FALSE_BRANCH"
      }
    ],
    "previousStatement": null,
    "nextStatement": null,
    "colour": 0,
    "tooltip": "現在の高度に基づいて条件分岐します（最大200cm）",
    "helpUrl": ""
  },

  // === 繰り返し ===
  {
    "type": "tello_repeat",
    "message0": "%1 回繰り返す %2 %3",
    "args0": [
      {
        "type": "field_number",
        "name": "TIMES",
        "value": 3,
        "min": 1,
        "max": 10
      },
      {
        "type": "input_dummy"
      },
      {
        "type": "input_statement",
        "name": "BODY"
      }
    ],
    "previousStatement": null,
    "nextStatement": null,
    "colour": 120,
    "tooltip": "指定回数だけ中のブロックを繰り返します（最大5回まで入れ子可能）",
    "helpUrl": ""
  }
]);
