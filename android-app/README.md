# KIGTTS Android App（原 KGTTS）

离线实时 ASR + TTS Android 客户端。

## 说明

- 技术栈：Kotlin + Jetpack Compose
- ASR：`sherpa-onnx`（AAR）
- TTS：Piper ONNX（从 `voicepack.zip` 读取）

> 命名变更：`KIGTTS` 为当前主名称，`KGTTS` 为旧称。

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
- ASR：`sosv.zip` 或 `sosv-int8.zip`
- 音色包：`voicepack.zip`

## 关键代码

- `app/src/main/java/com/kgtts/app/audio/Engines.kt`：ASR/TTS/播放链路
- `app/src/main/java/com/kgtts/app/ui/MainActivity.kt`：主 UI 与交互
- `app/src/main/java/com/kgtts/app/data/ModelRepository.kt`：模型与音色包管理
- `app/src/main/java/com/kgtts/app/overlay/`：悬浮窗模块

## 许可证

- 项目源码：`GNU GPL v3.0`
- 第三方许可证：[`../THIRD_PARTY_LICENSES.md`](../THIRD_PARTY_LICENSES.md)
