# AGENTS.md — telliyaki

Compact reference for agents working in this Android repo.

## Project

Tello ドローンをブロックプログラミングで操作し、操作をシミュレータ表示する Android アプリ。

## Build

- **Build tool**: Gradle with Kotlin DSL (`*.gradle.kts`)
- **Wrapper**: `gradlew` is present but **not executable** — run `bash gradlew <task>` or `chmod +x gradlew` first.
- **Key versions**:
  - AGP `9.1.1` (preview / very new)
  - Kotlin `2.2.10`
  - Gradle `9.3.1`
  - Compile SDK `36.1`
  - Min SDK `28`, Target SDK `36`

## Setup

- `local.properties` is **required** (defines `sdk.dir`). It is already present and **gitignored** — do not commit it.
- No special environment variables or secrets needed.

## Common Commands

| Task | Command |
|------|---------|
| Build debug APK | `bash gradlew app:assembleDebug` |
| Install debug | `bash gradlew app:installDebug` |
| Unit tests | `bash gradlew app:testDebugUnitTest` |
| Instrumented tests | `bash gradlew app:connectedDebugAndroidTest` (needs device/emulator) |
| Lint | `bash gradlew app:lintDebug` |
| Lint + auto-fix | `bash gradlew app:lintFix` |
| Full check | `bash gradlew app:check` |

## Project Structure

- Single module `:app`
- Package: `com.telliyaki`
- `MainActivity.kt` is the single entry point.
- UI is 100 % Jetpack Compose.

## Architecture Notes

- **Navigation**: `MainActivity.kt` uses Compose Navigation via `AppNavHost` in `NavGraph.kt`.
- **Screen flow**: `StartScreen` → `ProgramListScreen` → `BlockEditorScreen` → `PreviewScreen` (optional) → `ExecutionScreen`.
- **Block programming**: `BlockEditorScreen` is a pure Compose implementation (no WebView). Blocks are defined declaratively in `BlockDefinitions.kt`.
- **Block editor**: Tap blocks in the bottom palette to add them to the workspace. Tap workspace blocks to edit parameters. Use up/down arrow buttons to reorder. Tap delete button to remove. Tello connection check and takeoff/land auto-add confirmation dialogs appear before execution.
- **Program validation**: `ProgramValidator` checks for common mistakes (missing takeoff/land, multiple takeoffs, etc.) and provides child-friendly hints in Japanese.
- **Simulator**: `PreviewScreen` renders a 2D top-down view using Compose `Canvas` (drone position + trajectory).
- **Execution**: `ExecutionScreen` runs commands sequentially with logging, emergency stop button, and battery monitoring (30s interval).
- **Data model**: `BlocklyCommand` sealed class in `data/BlocklyCommand.kt` (TakeOff, Land, Move*, Rotate*, Wait).
- **ViewModels**: `BlockWorkspaceViewModel` (workspace state), `ExecutionViewModel` (execution state + logs + battery).
- **Storage**: `ProgramStorage` uses DataStore Preferences for saving/loading named programs.
- **Tello communication**: `TelloUdpClient` is a stub — will need UDP/WiFi implementation for Tello EDU.
- **Max blocks**: Default 25, configurable via settings.
- **Code style**: `kotlin.code.style=official` is set in `gradle.properties`.

## Key Dependencies

- Compose BOM `2025.12.00`
- Navigation Compose `2.9.8`
- Kotlinx Serialization `1.7.3`
- Lifecycle ViewModel Compose `2.10.0`
- Material Icons Extended
- DataStore Preferences `1.1.1`

## Constraints

- ProGuard/R8 is **disabled** (`isMinifyEnabled = false`).
- No CI workflows or pre-commit hooks exist yet.

## TODO

### 高優先度
- [ ] **UDP 通信実装**: `TelloUdpClient` のスタブを実際の UDP 通信に置き換え（Tello EDU 接続）
- [ ] **Flip コマンド追加**: `flip l/r/f/b` ブロックを追加（Kotlin + シミュレータ対応）
- [ ] **保存ダイアログ**: プログラム保存時に名前入力ダイアログを表示（現状は固定名「プログラム」）

### 中優先度
- [ ] **サンプルプログラム**: 「おてほん」プログラムを ProgramListScreen に追加
- [ ] **速度設定コマンド**: `speed {cm/s}` ブロックを追加
- [ ] **ブロック順序変更**: ワークスペース内のブロックを上下に移動する機能

### 低優先度
- [ ] **ミッション機能**: 「四角く飛ぼう」などの課題クリアモード
- [ ] **曲線移動**: `curve` コマンドの追加
- [ ] **RC コントロール**: リアルタイム操縦モード
- [ ] **If/Repeat ブロック**: 条件分岐・繰り返しブロックの追加
