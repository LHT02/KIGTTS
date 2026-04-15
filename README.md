# KIGTTS（原 KGTTS）

离线语音项目：PC/Electron 训练器负责生成 `voicepack.kigvpk` / `voicepack.zip`，Android App 负责离线实时 ASR + TTS，并集成便捷字幕、快捷名片、画板、悬浮窗、语音包管理与系统级文件/分享接管。

> 命名变更：项目主名称已切换为 **KIGTTS**。文档中出现的 `KGTTS` 均视为旧称，后续逐步弃用。

## 仓库结构

- `android-app/`：Android 客户端（Kotlin + Compose）
- `pc_trainer/`：Python 训练器（GUI + pipeline）
- `Electron_Trainer/`：Electron 训练端（前端 + Python 后端）
- `flutter_app/`：Flutter 试验/并行开发分支版本，由另一位开发者维护，不等同于当前安卓主软件实现
- `ANDROID_APP_GUIDE.md`：安卓主软件功能与界面设计总览
- `MODEL_ASSETS.md`：模型与素材下载索引（统一入口）
- `docs/android/ANDROID_MAIN_TODO.md`：当前安卓主软件待办与调试基线
- `要求.md`：需求与验收基线

## 分支与实现边界

- 当前主安卓软件以 `android-app/` 为准，这是正在维护和发版的 Kotlin/Compose 版本。
- `flutter_app/` 是**另一位开发者维护的并行 Flutter 分支实现**，不属于当前安卓主软件主线，也不应作为当前 Android 问题排查的行为基线。
- 若当前会话或文档上下文发生混乱，优先以：
  - `github-main-clean` 最近几次提交
  - `android-app/`
  - [docs/android/ANDROID_MAIN_TODO.md](./docs/android/ANDROID_MAIN_TODO.md)
  作为当前安卓主软件的真实基线。

## 快速开始

### 1) PC 训练器（Python）

```bash
cd pc_trainer
python -m venv .venv
.venv\Scripts\activate   # Linux/macOS: source .venv/bin/activate
pip install -r requirements.txt
python app.py
```

### 2) Electron 训练端

```bash
cd Electron_Trainer
npm install
npm run dev
```

### 3) Android App

```bash
cd android-app
./gradlew assembleDebug
```

APK 输出目录：`android-app/app/build/outputs/apk/`

安卓端功能、界面结构、文件格式与系统集成说明：
- [ANDROID_APP_GUIDE.md](./ANDROID_APP_GUIDE.md)

## 运行所需模型

请统一从 [MODEL_ASSETS.md](./MODEL_ASSETS.md) 获取下载链接与素材说明：

- ASR：`sosv.zip` / `sosv-int8.zip`
- TTS 音色包：`.kigvpk`（当前导出格式）或兼容导入 `.zip`
- 安卓端默认朗读后端：`系统 TTS`
- 安卓 APK 内置资源：`sosv-int8.zip`、Sherpa speech enhancement models（GTCRN / DPDFNet2 / DPDFNet4）、sherpa speaker verify model、`espeak-ng-data.zip`

## 许可证

- 项目源码：`GNU GPL v3.0`
- 第三方许可证清单：`THIRD_PARTY_LICENSES.md`
