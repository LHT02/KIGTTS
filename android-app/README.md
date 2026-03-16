# Android App (离线实时 ASR + 并行 TTS)

- 架构：Kotlin + Jetpack Compose，ASR 基于 `sherpa-onnx` AAR（已放入 `app/libs/`），TTS 基于 Piper ONNX（从 `voicepack.zip` 读取 `tts/model.onnx`）。
- 模型导入：在“模型管理”页分别导入 `sosv.zip` 与 `voicepack.zip`，内部会解压到 `files/models/`。
- 实时页：点击“开始”请求麦克风权限，开始持续录音 → 简易端点检测 → sherpa-onnx 离线识别 → Piper ONNX 合成并播放，同时继续监听。

## 构建 APK
```bash
cd android-app
./gradlew assembleDebug
```
- 需要 Android SDK 34、JDK 17（Android Studio Giraffe+ 即可）。`app/libs/sherpa-onnx-1.12.20.aar` 已包含 JNI 与 onnxruntime。

## 目录要点
- `app/libs/sherpa-onnx-1.12.20.aar`：离线 ASR JNI + onnxruntime
- `app/src/main/java/com/kgtts/app/audio/Engines.kt`：ASR/TTS/音频播放与实时控制
- `app/src/main/java/com/kgtts/app/ui/MainActivity.kt`：UI + 模型导入 + 启停控制
- `app/src/main/java/com/kgtts/app/data/ModelRepository.kt`：模型/音色包解压与管理

## 回声/复读
- 采用 barge-in：播放时提高端点阈值，播放器独立线程，仍持续录音。
- UI 提示使用耳机；若设备支持，可在后续版本接入 AEC。

## 许可证
- 本目录代码随仓库整体按 **GNU GPL v3.0** 发布（见仓库根目录 `LICENSE`）。
- 第三方许可证见仓库根目录 `THIRD_PARTY_LICENSES.md`。
