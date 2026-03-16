# KGTTS Trainer (Electron_Trainer)

Electron + React + MUI 的训练器前端，后端由内置 Python 服务进程驱动。

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

## 说明
- 构建时会从 `../pc_trainer` 复制 `piper_env` 与 `resources_pack` 到安装包资源目录。
- 后端入口：`backend/server.py`
- 前端入口：`electron/main.cjs` + `dist-renderer/`

## 许可证
- 本目录代码随仓库整体按 **GNU GPL v3.0** 发布（见仓库根目录 `LICENSE`）。
- 第三方许可证见仓库根目录 `THIRD_PARTY_LICENSES.md`。
