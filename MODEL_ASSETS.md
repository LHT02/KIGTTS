# KIGTTS 模型与素材下载索引

> 命名说明：项目当前主名称为 **KIGTTS**。`KGTTS` 为历史名称，后续会逐步弃用。

本文档集中维护项目相关模型与素材下载链接，避免链接分散在多个 README。

> 说明：本文件描述的是当前安卓主软件 `android-app/` 与训练端相关的资源基线；仓库中的 `flutter_app/` 为并行 Flutter 分支版本，不作为当前安卓主软件资源加载行为的默认参考实现。

当前安卓端的默认模型/后端策略：
- 默认 TTS 后端为 **系统 TTS**，不再内置默认 Piper 语音包
- 自定义离线 Piper 音色包仍可导入，格式为 `.kigvpk` 或兼容 `.zip`
- ASR、Silero VAD、GTCRN / DPDFNet 语音增强模型不再随 APK 内置，改为通过 `语音识别资源包` 外部安装
- APK 内已集成 sherpa-onnx 说话人验证模型，无需用户单独下载

## 1. 运行必需素材（用户侧）

### 1.1 一键配置

克隆仓库后运行根目录的 `setup_assets.ps1`，自动下载 ASR 模型并分发到旧版兼容资源目录；当前 Android 主软件推荐改为打包并外部安装 `语音识别资源包`：

```powershell
powershell -ExecutionPolicy Bypass -File setup_assets.ps1
```

脚本会处理：
- **sosv-int8.zip**（ASR, ~209MB）— 从 GitHub Release 下载
- 如项目根目录存在本地 Piper 音色包 zip，也会按旧兼容逻辑分发到 assets 目录，便于开发测试

> 安卓主软件当前默认使用系统 TTS，不依赖内置 `firefly.zip`。

> 如果网络不通，脚本会给出手动下载链接。

### 1.2 ASR 模型（Android 导入）
- SOSV 模型发布页（`sosv.zip` / `sosv-int8.zip`）：
  - https://github.com/HiMeditator/auto-caption/releases/tag/sosv-model

### 1.2.1 语音识别资源包（Android 设置页安装）

安卓主软件支持把语音识别与降噪相关模型整合为一个 `.7z` 或兼容 `.zip` 包，用于放到魔搭 / Hugging Face 等下载源，或从本地文件安装。安装后资源会解压到软件内部目录，后续 ASR、Silero VAD、GTCRN / DPDFNet 语音增强都会使用该资源包。

推荐包内结构：

```text
kigtts-recognition-resources/
  recognition_resources.json
  asr/
    sensevoice/model.int8.onnx
    sensevoice/tokens.txt
    silero_vad.onnx
    punct/model.int8.onnx
    punct-en/model.int8.onnx
  speech_enhancement/
    gtcrn_simple.onnx
    dpdfnet2.onnx
    dpdfnet4.onnx
```

`recognition_resources.json` 示例：

```json
{
  "type": "kigtts-recognition-resources",
  "name": "KIGTTS 语音识别资源包",
  "version": "2026.05",
  "files": {
    "asrDir": "asr",
    "sileroVad": "asr/silero_vad.onnx",
    "speechEnhancement": {
      "gtcrn": "speech_enhancement/gtcrn_simple.onnx",
      "dpdfnet2": "speech_enhancement/dpdfnet2.onnx",
      "dpdfnet4": "speech_enhancement/dpdfnet4.onnx"
    }
  }
}
```

兼容说明：
- manifest 文件名可使用 `recognition_resources.json`、`kigtts_recognition_resources.json` 或 `manifest.json`
- 如果 manifest 未显式写路径，软件会按文件名递归查找 `silero_vad.onnx`、`gtcrn_simple.onnx`、`dpdfnet2.onnx`、`dpdfnet4.onnx`
- 通过下载源安装的临时 `.7z` 包会在安装完成后清理；本地安装不会删除用户选择的源文件

### 1.3 音色包（Android 导入）
- 当前推荐导出格式：`voicepack.kigvpk`
- 兼容导入：`.kigvpk` / `.zip`
- 最小结构：
  - `manifest.json`
  - `tts/model.onnx`
  - `tts/model.onnx.json`
  - `tts/phonemizer.dict`

### 1.4 APK 内置模型与资源

安卓主软件当前随 APK 一起打包的模型/资源：

- `speaker_verify/3dspeaker_speech_campplus_sv_zh-cn_16k-common.onnx`
  - sherpa-onnx 官方说话人验证模型
  - 约 `26.97 MiB`
- `espeak-ng-data.zip`
  - Piper 语音包所需的 eSpeak 数据

说明：
- 系统 TTS 为默认内置朗读后端，不需要单独模型文件
- ASR、Silero VAD、GTCRN / DPDFNet 语音增强模型需要通过设置页安装 `语音识别资源包`
- 自定义 Piper 音色包仅在用户导入后才会出现在语音包列表中
- 设置页里的 `AI 语音增强` 当前支持：
  - 关闭
  - Sherpa GTCRN（语句级）
  - Sherpa GTCRN（流式）
  - Sherpa DPDFNet2（流式）
  - Sherpa DPDFNet4（流式）

## 2. 训练与推理相关上游项目

### 2.1 ASR 框架
- sherpa-onnx：
  - https://github.com/k2-fsa/sherpa-onnx
- sherpa-onnx Speaker Identification / Verification：
  - https://k2-fsa.github.io/sherpa/onnx/speaker-identification/index.html
- sherpa-onnx 官方 speaker models：
  - https://github.com/k2-fsa/sherpa-onnx/releases/tag/speaker-recongition-models

### 2.2 TTS 训练/导出（Piper）
- 当前活跃上游（推荐）：
  - https://github.com/OHF-Voice/piper1-gpl
- 历史归档仓库（仅兼容参考）：
  - https://github.com/rhasspy/piper
- 训练参考文档（历史仓库）：
  - https://github.com/rhasspy/piper/blob/master/TRAINING.md
- 公开 checkpoints（历史）：
  - https://huggingface.co/datasets/rhasspy/piper-checkpoints

### 2.3 参考项目
- Auto Caption（架构与引擎通信参考）：
  - https://github.com/HiMeditator/auto-caption
  - https://github.com/HiMeditator/auto-caption/blob/main/docs/engine-manual/zh.md
- Android 端 TTS 性能参考（可选）：
  - https://github.com/nihui/ncnn-android-piper

## 3. 开发素材建议

- 录音素材：单人、干净录音优先（A 档）；轻噪声场景作为 B 档。
- 测试语料：短句/长句/数字/中英混读。
- 设备：中端机 + 高端机至少各 1 台。

## 4. 链接维护规则

- 新增/替换模型来源时，先更新本文件，再同步到各 README。
- 若链接失效，优先更新为官方仓库/发布页，不使用第三方镜像直链。
