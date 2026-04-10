# KIGTTS PC 训练器（原 KGTTS）

基于 Python 的离线训练器：导入音频 -> 预处理/VAD -> sherpa-onnx 转写 -> Piper 训练/导出 -> 打包 `voicepack.zip`。

## 环境准备

```bash
cd pc_trainer
python -m venv .venv
.venv\Scripts\activate   # Linux/macOS: source .venv/bin/activate
pip install -r requirements.txt
```

## 运行

```bash
python app.py
```

- 在 GUI 中选择输出目录、导入音频、设置 A/B 档后执行一键训练。
- 日志：`logs/trainer.log`
- 工作目录：`project_root/work/`
- 导出目录：`project_root/export/voicepack.zip`

## 打包 EXE（Windows）

```powershell
./build_exe.ps1
```

## 模型/素材下载

请统一参考仓库根目录：
- [../MODEL_ASSETS.md](../MODEL_ASSETS.md)

## 许可证

- 项目源码：`GNU GPL v3.0`
- 第三方许可证：[`../THIRD_PARTY_LICENSES.md`](../THIRD_PARTY_LICENSES.md)
