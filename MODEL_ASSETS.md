# KIGTTS 模型与素材下载索引

> 命名说明：项目当前主名称为 **KIGTTS**。`KGTTS` 为历史名称，后续会逐步弃用。

本文档集中维护项目相关模型与素材下载链接，避免链接分散在多个 README。

## 1. 运行必需素材（用户侧）

### 1.1 ASR 模型（Android 导入）
- SOSV 模型发布页（`sosv.zip` / `sosv-int8.zip`）：
  - https://github.com/HiMeditator/auto-caption/releases/tag/sosv-model

### 1.2 音色包（Android 导入）
- `voicepack.zip` 由 PC 训练器或 Electron 训练器导出。
- 最小结构：
  - `manifest.json`
  - `tts/model.onnx`
  - `tts/model.onnx.json`
  - `tts/phonemizer.dict`

## 2. 训练与推理相关上游项目

### 2.1 ASR 框架
- sherpa-onnx：
  - https://github.com/k2-fsa/sherpa-onnx

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
