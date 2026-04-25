# KIGTTS Trainer（Electron）

KIGTTS 桌面训练器，基于 `Electron + React + Python 后端`，用于整理训练素材、执行本地训练或蒸馏流程，并最终导出 Android 端可导入的 `voicepack.kigvpk`。

> 命名说明：`KIGTTS` 为当前主名称，`KGTTS` 为旧称。

## 制作与署名

<table>
  <tr>
    <td align="center" width="33%">
      <a href="https://space.bilibili.com/87244951">
        <img src="../ARTS/Avatar/LHT.jpg" alt="LHT" width="88">
      </a>
      <br>
      <strong>LHT</strong>
      <br>
      <a href="https://space.bilibili.com/87244951">
        <img src="https://img.shields.io/badge/Bilibili-主页-00A1D6?style=flat-square&logo=bilibili&logoColor=white" alt="LHT Bilibili">
      </a>
    </td>
    <td align="center" width="33%">
      <a href="https://space.bilibili.com/573842321">
        <img src="../ARTS/Avatar/huajiang.jpg" alt="花酱" width="88">
      </a>
      <br>
      <strong>花酱</strong>
      <br>
      <a href="https://space.bilibili.com/573842321">
        <img src="https://img.shields.io/badge/Bilibili-主页-00A1D6?style=flat-square&logo=bilibili&logoColor=white" alt="花酱 Bilibili">
      </a>
    </td>
    <td align="center" width="33%">
      <a href="https://space.bilibili.com/23208863">
        <img src="../ARTS/Avatar/YuiLu.jpg" alt="Yui Lu" width="88">
      </a>
      <br>
      <strong>Yui Lu</strong>
      <br>
      <a href="https://space.bilibili.com/23208863">
        <img src="https://img.shields.io/badge/Bilibili-主页-00A1D6?style=flat-square&logo=bilibili&logoColor=white" alt="Yui Lu Bilibili">
      </a>
    </td>
  </tr>
</table>

## 文档入口

- 详细使用文档：[`../docs/trainer/ELECTRON_TRAINER_USAGE.md`](../docs/trainer/ELECTRON_TRAINER_USAGE.md)
- 模型与素材下载索引：[`../MODEL_ASSETS.md`](../MODEL_ASSETS.md)
- 隐私政策：[`../docs/legal/PRIVACY_POLICY.md`](../docs/legal/PRIVACY_POLICY.md)
- 开源许可证说明：[`../docs/legal/OPEN_SOURCE_LICENSES.md`](../docs/legal/OPEN_SOURCE_LICENSES.md)

## 当前支持的训练模式

- `Piper 标准`
- `GPT-SoVITS 蒸馏`
- `VoxCPM2 蒸馏`
- `从旧项目继续训练`

最终导出产物：

- 默认文件名：`voicepack.kigvpk`
- `.kigvpk` 本质上仍是 zip 结构
- 预览加载兼容旧 `.zip` 语音包

## 目录说明

- 前端入口：`electron/main.cjs`
- 预加载桥：`electron/preload.cjs`
- 后端入口：`backend/server.py`
- 训练流程入口：`backend/engine/pipeline.py`

## 开发

```bash
cd Electron_Trainer
npm install
npm run dev
```

## 打包

```bash
cd Electron_Trainer
npm run dist
```

Windows Inno 安装包：

```bash
cd Electron_Trainer
npm run dist:inno
```

## 资源说明

构建时会从 `../pc_trainer` 复制基础训练资源，例如：

- `piper_env`
- `resources_pack`
- `CKPT`
- 本地 piper 相关 wheels

额外在线资源：

- `Piper CUDA 运行时` 由训练器在用户机器上在线创建
- `VoxCPM2 运行时` 和模型由训练器在线安装 / 下载
- `GPT-SoVITS` 模式使用用户自己准备的外部整合包

## 许可证

- 项目源码：`GNU GPL v3.0`
- 第三方许可证：[`../THIRD_PARTY_LICENSES.md`](../THIRD_PARTY_LICENSES.md)
