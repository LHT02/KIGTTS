# Electron Trainer: VoxCPM2 蒸馏模式（CUDA 运行时也在线下载）

## Summary
- 新增 `VoxCPM2 蒸馏` 模式，最终仍导出 KIGTTS `.kigvpk`。
- 软件本体不内置 VoxCPM2 权重，也不内置 CUDA 12 运行时。
- 软件内置轻量安装器与必要脚本；首次使用时在线创建 `voxcpm_env`，再从 ModelScope 下载 VoxCPM2 权重。
- 首版支持音色描述 + 参考音频克隆；默认启用 denoiser；默认 CUDA，CUDA 不可用时允许 CPU 回退但明确提示很慢。

## Key Changes
- 运行时管理新增 `VoxCPM2 运行时`：
  - 目录：`%APPDATA%/kgtts-trainer/runtimes/voxcpm_env`
  - 用内置 micromamba 在线创建 Python 3.10 环境
  - CUDA 12 / PyTorch 2.5+ 依赖在线安装
  - 国内镜像优先，失败回退官方源
  - 状态卡显示：未安装 / 安装中 / 已就绪 / CUDA 可用性 / Torch + CUDA 版本
- 模型权重管理新增 `VoxCPM2 模型`：
  - 主模型从 ModelScope 下载：`OpenBMB/VoxCPM2`
  - denoiser 从 ModelScope 下载：`iic/speech_zipenhancer_ans_multiloss_16k_base`
  - 目录：`%APPDATA%/kgtts-trainer/models/voxcpm2`
  - UI 提供“检查状态 / 下载模型 / 打开模型目录”
- 后端新增 IPC：
  - `get_voxcpm_runtime_status`
  - `install_voxcpm_runtime`
  - `get_voxcpm_model_status`
  - `download_voxcpm_models`
  - `start_voxcpm_distill_pipeline`
- 前端训练准备页：
  - 训练模式加入 `VoxCPM2 蒸馏`
  - 新增运行时状态、模型状态、音色描述、参考音频、文本来源、推理参数卡片
  - 复用现有文本来源拖入逻辑和 Piper 训练设置
- 流水线：
  - `collect -> synth -> train -> export`
  - VoxCPM2 helper 加载模型一次，逐条合成 wav 到 `work/voxcpm_corpus/wavs`
  - 生成 `work/metadata.csv`
  - 后续复用 Piper 训练与 `.kigvpk` 打包

## Runtime Defaults
- `voxcpm_env` 默认安装：
  - Python 3.10
  - `voxcpm==2.0.2`
  - `modelscope`
  - PyTorch 2.5+ CUDA 12 相关依赖
- 安装源策略：
  - pip/conda 国内镜像优先
  - 国内镜像失败后回退官方源
  - 不把 CUDA 12 wheel 打进软件包
- 开始训练前校验：
  - `voxcpm_env` 必须就绪
  - VoxCPM2 主模型必须下载
  - 默认 denoiser 开启时 denoiser 模型也必须下载
  - CUDA 不可用时允许 CPU 回退，但弹出/日志提示“可能非常慢”

## Test Plan
- 安装测试：
  - 空机器状态显示 `VoxCPM2 运行时未安装`
  - 点击安装后能创建 `voxcpm_env`
  - 国内源失败时能回退官方源
- 模型测试：
  - ModelScope 下载主模型成功
  - denoiser 默认下载成功
  - 中断后重试能继续或给出明确错误
- 推理测试：
  - 音色描述生成 1 条 wav
  - 参考音频克隆生成 1 条 wav
  - denoiser 开启/关闭各跑一次
- 完整链路：
  - 2-5 条文本跑通 `VoxCPM2 synth -> Piper train -> export .kigvpk`
  - CUDA 可用时确认用 CUDA
  - CUDA 不可用时确认 CPU 回退提示明确
- 打包：
  - `npm run build`
  - `npm run lint`
  - `npx electron-builder --win --dir`
  - `dist/win-unpacked` 不包含 VoxCPM2 权重，也不包含 CUDA 12 大型依赖

## Assumptions
- 允许首次使用时下载较大的 VoxCPM2 运行时和模型权重。
- 软件本体只内置 micromamba、运行时创建脚本、UI 和后端集成代码。
- VoxCPM2 模式只做蒸馏，不做 VoxCPM2 LoRA 微调或直接导出 VoxCPM2 模型。
