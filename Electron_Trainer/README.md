# KIGTTS Trainer（Electron）

Electron 训练端（前端 + 内置 Python 后端），用于从录音训练并导出 `voicepack.kigvpk`。

> 命名变更：`KIGTTS` 为当前主名称，`KGTTS` 为旧称。

## 开发

```bash
cd Electron_Trainer
npm install
npm run dev
```

## 打包

```bash
npm run dist
```

## 导出产物

- 训练导出的语音包默认文件名为 `voicepack.kigvpk`
- `.kigvpk` 本质上仍是 zip 结构，供安卓端和其它 KIGTTS 组件直接识别安装
- 预览加载同时兼容旧的 `.zip` 语音包

## 目录说明

- 前端入口：`electron/main.cjs`
- 后端入口：`backend/server.py`
- 构建时会从 `../pc_trainer` 复制训练运行时资源（`piper_env` / `resources_pack`）

## 模型与素材

统一下载与准备说明：
- [../MODEL_ASSETS.md](../MODEL_ASSETS.md)

## 许可证

- 项目源码：`GNU GPL v3.0`
- 第三方许可证：[`../THIRD_PARTY_LICENSES.md`](../THIRD_PARTY_LICENSES.md)
