# KIGTTS Android App（原 KGTTS）

离线实时 ASR + TTS Android 客户端，同时集成便捷字幕、快捷名片、画板、悬浮窗、语音包与设置系统。

## 说明

- 技术栈：Kotlin + Jetpack Compose
- ASR：`sherpa-onnx`（AAR）
- TTS：系统 TTS（默认）+ Piper ONNX（导入兼容 `.kigvpk` / `.zip`）

> 命名变更：`KIGTTS` 为当前主名称，`KGTTS` 为旧称。

完整的安卓主软件功能、界面设计、导航结构与文件格式说明请看：
- [../ANDROID_APP_GUIDE.md](../ANDROID_APP_GUIDE.md)

## 构建

```bash
cd android-app
./gradlew assembleDebug
# 或
./gradlew assembleRelease
```

## 模型导入

模型与素材下载请看仓库根目录：
- [MODEL_ASSETS.md](../MODEL_ASSETS.md)

导入项：
- 语音识别资源包：`.7z` 或兼容 `.zip`，可统一包含 ASR、Silero VAD、GTCRN / DPDFNet 语音增强模型
- 音色包：`.kigvpk`（当前导出格式）或兼容 `.zip`

安卓 APK 当前内置：
- `speaker_verify/3dspeaker_speech_campplus_sv_zh-cn_16k-common.onnx`
- `espeak-ng-data.zip`

## 关键代码

- `app/src/main/java/com/kgtts/app/audio/Engines.kt`：ASR/TTS/播放链路
- `app/src/main/java/com/kgtts/app/ui/MainActivity.kt`：主 UI 与交互
- `app/src/main/java/com/kgtts/app/data/ModelRepository.kt`：模型与音色包管理
- `app/src/main/java/com/kgtts/app/overlay/`：悬浮窗模块

## 许可证

- 项目源码：`GNU GPL v3.0`
- 第三方许可证：[`../THIRD_PARTY_LICENSES.md`](../THIRD_PARTY_LICENSES.md)
