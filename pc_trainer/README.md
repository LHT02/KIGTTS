# PC 训练器

基于 Python 的离线训练器，流程：导入音频 → 预处理/VAD → sherpa-onnx 转写 → Piper 训练/导出 → 打包 `voicepack.zip`。

## 依赖安装
```bash
cd pc_trainer
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

需额外安装 Piper 训练/导出工具（参考 https://github.com/rhasspy/piper/blob/master/TRAINING.md），确保有 `piper_train` 与 `piper_export` 命令。

## 运行 GUI
```bash
python app.py
```
- 选择输出目录、拖入音频、设置 A/B 档与模型路径后点击“一键训练并导出”。
- 训练日志存于 `logs/trainer.log`，中间产物在 `project_root/work/`。

## 命令行打包 EXE
```powershell
./build_exe.ps1
```

## 产出
- `project_root/export/voicepack.zip`：包含 `manifest.json`、`tts/model.onnx`、`tts/model.onnx.json`、`tts/phonemizer.dict`、可选 `preview.wav`。

## 许可证
- 本目录代码随仓库整体按 **GNU GPL v3.0** 发布（见仓库根目录 `LICENSE`）。
- 第三方许可证见仓库根目录 `THIRD_PARTY_LICENSES.md`。
