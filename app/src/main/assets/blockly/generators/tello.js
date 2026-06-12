// Tello ブロック用 JSON ジェネレータ (Blockly v11.2.0 API)
'use strict';

Blockly.Tello = new Blockly.CodeGenerator('Tello');

Blockly.Tello.scrub_ = function(block, code, opt_thisOnly) {
  var nextBlock = block.nextConnection && block.nextConnection.targetBlock();
  var nextCode = opt_thisOnly ? '' : this.blockToCode(nextBlock);
  return code + nextCode;
};

// 基本操作
Blockly.Tello.forBlock['tello_takeoff'] = function(block, generator) {
  return '{"type":"takeoff"}\n';
};

Blockly.Tello.forBlock['tello_land'] = function(block, generator) {
  return '{"type":"land"}\n';
};

// 移動
Blockly.Tello.forBlock['tello_move_forward'] = function(block, generator) {
  var distance = generator.getFieldValue(block, 'DISTANCE');
  return '{"type":"move_forward","distance":' + distance + '}\n';
};

Blockly.Tello.forBlock['tello_move_back'] = function(block, generator) {
  var distance = generator.getFieldValue(block, 'DISTANCE');
  return '{"type":"move_back","distance":' + distance + '}\n';
};

Blockly.Tello.forBlock['tello_move_left'] = function(block, generator) {
  var distance = generator.getFieldValue(block, 'DISTANCE');
  return '{"type":"move_left","distance":' + distance + '}\n';
};

Blockly.Tello.forBlock['tello_move_right'] = function(block, generator) {
  var distance = generator.getFieldValue(block, 'DISTANCE');
  return '{"type":"move_right","distance":' + distance + '}\n';
};

Blockly.Tello.forBlock['tello_move_up'] = function(block, generator) {
  var distance = generator.getFieldValue(block, 'DISTANCE');
  return '{"type":"move_up","distance":' + distance + '}\n';
};

Blockly.Tello.forBlock['tello_move_down'] = function(block, generator) {
  var distance = generator.getFieldValue(block, 'DISTANCE');
  return '{"type":"move_down","distance":' + distance + '}\n';
};

// 回転
Blockly.Tello.forBlock['tello_rotate_cw'] = function(block, generator) {
  var degrees = generator.getFieldValue(block, 'DEGREES');
  return '{"type":"rotate_cw","degrees":' + degrees + '}\n';
};

Blockly.Tello.forBlock['tello_rotate_ccw'] = function(block, generator) {
  var degrees = generator.getFieldValue(block, 'DEGREES');
  return '{"type":"rotate_ccw","degrees":' + degrees + '}\n';
};

// 待機
Blockly.Tello.forBlock['tello_wait'] = function(block, generator) {
  var milliseconds = generator.getFieldValue(block, 'MILLISECONDS');
  return '{"type":"wait","milliseconds":' + milliseconds + '}\n';
};

// 条件分岐
Blockly.Tello.forBlock['tello_if_altitude'] = function(block, generator) {
  var threshold = generator.getFieldValue(block, 'THRESHOLD');
  var operator = generator.getFieldValue(block, 'OPERATOR');
  var trueBranch = generator.statementToCode(block, 'TRUE_BRANCH');
  var falseBranch = generator.statementToCode(block, 'FALSE_BRANCH');
  
  var trueCommands = trueBranch ? parseStatements(trueBranch) : [];
  var falseCommands = falseBranch ? parseStatements(falseBranch) : [];
  
  var result = {
    type: 'if_altitude',
    threshold: threshold,
    operator: operator,
    true_branch: trueCommands,
    false_branch: falseCommands
  };
  
  return JSON.stringify(result) + '\n';
};

// 繰り返し
Blockly.Tello.forBlock['tello_repeat'] = function(block, generator) {
  var times = generator.getFieldValue(block, 'TIMES');
  var body = generator.statementToCode(block, 'BODY');
  
  var bodyCommands = body ? parseStatements(body) : [];
  
  var result = {
    type: 'repeat',
    times: times,
    body: bodyCommands
  };
  
  return JSON.stringify(result) + '\n';
};

// ステートメントをパースするヘルパー関数
function parseStatements(code) {
  var lines = code.trim().split('\n').filter(function(line) { return line.trim(); });
  var commands = [];
  for (var i = 0; i < lines.length; i++) {
    try {
      var cmd = JSON.parse(lines[i].trim());
      commands.push(cmd);
    } catch (e) {
      console.error('Failed to parse command:', lines[i], e);
    }
  }
  return commands;
}

// ワークスペース全体をJSONに変換
Blockly.Tello.workspaceToJson = function(workspace) {
  var topBlocks = workspace.getTopBlocks(true);
  var commands = [];
  
  for (var i = 0; i < topBlocks.length; i++) {
    var code = Blockly.Tello.blockToCode(topBlocks[i]);
    var lines = code.trim().split('\n').filter(function(line) { return line.trim(); });
    for (var j = 0; j < lines.length; j++) {
      try {
        commands.push(JSON.parse(lines[j].trim()));
      } catch (e) {
        console.error('Failed to parse:', lines[j], e);
      }
    }
  }
  
  return JSON.stringify({
    type: 'sequence',
    children: commands
  }, null, 2);
};
