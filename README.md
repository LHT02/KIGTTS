# 离线实时 ASR → 指定音色 TTS 项目

根据 `要求.md` 实现的双端方案：PC 端训练器（GUI+流水线）负责从用户录音生成 `voicepack.zip`，Android 端 App 负责离线实时 ASR 并并行调用 TTS。

## 仓库结构
- `要求.md`：原始需求文档
- `pc_trainer/`：Python 训练器（GUI + 引擎），可打包为 EXE
- `android-app/`：Android 工程（Kotlin/Compose），可产出 APK

## 快速开始（PC 训练器）
1. 准备 Python 3.10+ 环境（Windows / Linux）并安装依赖：
   ```bash
   cd pc_trainer
   python -m venv .venv
   .venv\Scripts\activate   # Linux/macOS 下改用 source .venv/bin/activate
   pip install -r requirements.txt
   ```
2. 运行 GUI 训练器：
   ```bash
   python app.py
   ```
   - 导入音频（支持多文件），选择 A/B 档与输出目录，点击“一键训练”。
   - GUI 会依次完成预处理 → VAD 切分 → sherpa-onnx ASR 转写 → Piper 训练/导出 → 打包 `voicepack.zip`。
3. 打包 EXE（Windows）：
   ```powershell
   cd pc_trainer
   ./build_exe.ps1
   ```
   产物位于 `pc_trainer/dist/kgt_trainer.exe`。

## 快速开始（Android App）
1. 安装 Android Studio（Giraffe+），JDK 17。
2. 打开 `android-app`，首次同步会下载依赖（Compose、Room、OnnxRuntime）。
3. 将 `sosv.zip`（或 `sosv-int8.zip`）与 `voicepack.zip` 拷入手机存储，或在 App 内导入。
4. 运行/构建：
   ```bash
   cd android-app
   ./gradlew assembleDebug   # 产出 APK 位于 android-app/app/build/outputs/apk/debug/
   ```
   运行后在“模型管理”导入 ASR 模型与音色包，在“实时转换”点击开始即可。

## 主要特性对照需求
- **全离线链路**：PC 端训练和 Android 端推理均基于本地模型（sherpa-onnx + Piper ONNX）。
- **A/B 档支持**：预处理参数可切换，B 档开启更严格降噪与筛选。
- **并行 ASR/TTS**：Android 端使用协程 + 流式 ASR，endpoint 确认后立即送入 TTS 播放，播放期间继续监听。
- **模型导入/管理**：支持导入/切换/删除 `sosv.zip` 与 `voicepack.zip`，UI 有状态提示。
- **voicepack 规范**：导出包内含 `manifest.json` + `tts/model.onnx(.json)` + `tts/phonemizer.dict`。

更多细节与扩展请见各子目录内说明。

## 许可证
- 本项目源码采用 **GNU GPL v3.0** 进行发布（见根目录 `LICENSE` 与 `COPYING`）。
- 第三方依赖许可证清单见 `THIRD_PARTY_LICENSES.md`。
- 发布产物会尽量附带许可证文件；若你二次分发，请同时保留许可证与版权声明。
