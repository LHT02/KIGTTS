# KIGTTS Trainer 使用文档

本文件面向训练器实际使用者，说明 `Electron_Trainer/` 当前支持的训练模式、准备项、常见工作流、项目目录结构和恢复逻辑。

训练器的目标不是导出 GPT-SoVITS 或 VoxCPM2 模型本体，而是把语料整理、蒸馏、Piper 训练和语音包打包串成一条桌面流程，最终输出可给 KIGTTS Android 端导入的 `.kigvpk` 语音包。

## 下载入口

<table>
  <tr>
    <td align="center" width="50%">
      <strong>ModelScope</strong>
      <br>
      <span>国内下载镜像</span>
      <br><br>
      <a href="https://modelscope.cn/models/LHTSTUDIO/KIGTTS_TRAINER/files">
        <img src="https://img.shields.io/badge/ModelScope-下载-624AFF?style=for-the-badge" alt="ModelScope Download">
      </a>
    </td>
    <td align="center" width="50%">
      <strong>Hugging Face</strong>
      <br>
      <span>海外下载镜像</span>
      <br><br>
      <a href="https://huggingface.co/LHT02/kigtts-trainer/tree/main">
        <img src="https://img.shields.io/badge/Hugging%20Face-下载-FFD21E?style=for-the-badge&logo=huggingface&logoColor=black" alt="Hugging Face Download">
      </a>
    </td>
  </tr>
</table>

配套 Android 软件发行版：

- [APP0.1.0](https://github.com/LHT02/KIGTTS/releases/tag/APP0.1.0)

## 制作与署名

<table>
  <tr>
    <td align="center" width="33%">
      <a href="https://space.bilibili.com/87244951">
        <img src="../../ARTS/Avatar/LHT.jpg" alt="LHT" width="88">
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
        <img src="../../ARTS/Avatar/huajiang.jpg" alt="花酱" width="88">
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
        <img src="../../ARTS/Avatar/YuiLu.jpg" alt="Yui Lu" width="88">
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

## 1. 训练器能做什么

- 从原始录音直接走 `预处理 -> VAD -> ASR -> Piper 训练 -> 导出`
- 调用外部 `GSVI / GPT-SoVITS` 模型批量生成蒸馏语料，再继续 Piper 训练
- 调用 `VoxCPM2` 批量生成蒸馏语料，再继续 Piper 训练
- 读取旧项目的配置和中间文件，直接继续训练或补生成缺失素材
- 生成语音包试听音频，并导出最终的 `voicepack.kigvpk`

## 2. 支持的训练模式

| 模式 | 适合场景 | 主要输入 | 主要流程 |
| --- | --- | --- | --- |
| `Piper 标准` | 你有自己的原始录音，想从头训练 | 音频文件 | `preprocess -> vad -> asr -> train -> export` |
| `GPT-SoVITS 蒸馏` | 你已有 GSVI / GPT-SoVITS 说话人模型，想快速生成大量教师语料 | 外部整合包根目录 + 文本来源 | `collect -> distill -> train -> export` |
| `VoxCPM2 蒸馏` | 你想用音色描述或参考音频快速生成训练语料 | VoxCPM2 运行时/模型 + 文本来源 | `collect -> synth -> train -> export` |
| `从旧项目继续训练` | 你之前已经跑过项目，想直接续训或补齐缺失素材 | 旧项目目录 | 根据项目模式决定直接训练或补生成 |

## 3. 使用前准备

### 3.1 基本建议

- 每次训练尽量使用一个新的输出目录，避免旧文件和新任务混在一起。
- 输出目录建议放在本地磁盘，而不是网络盘、同步盘或移动设备目录。
- 如果你要做长时间训练或大语料蒸馏，优先保证磁盘空间、显卡驱动和电源策略稳定。

### 3.2 训练器的几个关键输入

- `输出目录`
  - 训练器会在这里创建项目目录、`work/` 中间文件和 `export/` 导出文件。
- `ASR 模型 zip`
  - `Piper 标准` 模式需要。
  - `VoxCPM2 高保真克隆` 在未手填参考文本时也会用它自动转写参考音频。
- `Piper 基线 ckpt`
  - 可选，用于继续靠近某个 Piper 基线音色。
- `Piper config.json`
  - 可选，通常只在你有特殊导出配置时使用。

### 3.3 GPU / CUDA 相关

- 训练器默认内置 CPU 基线训练环境。
- 如果 `Piper 训练设备` 选择 `GPU/CUDA`，需要先在训练设置页安装 `Piper CUDA 运行时`。
- `GPT-SoVITS 蒸馏` 的教师推理设备是单独设置的，默认可选 `cuda`。
- `VoxCPM2 蒸馏` 的推理设备也是单独设置的，默认可选 `cuda`，并可勾选允许回退 CPU。
- 运行时安装时会自动测速镜像并择优换源；安装和下载过程中，`开始训练语音包` 会被禁用。

### 3.4 在线下载位置

当前 Windows 下，训练器把在线安装的运行时和模型放到：

```text
%LOCALAPPDATA%\kgtts-trainer\
```

主要目录：

```text
%LOCALAPPDATA%\kgtts-trainer\runtimes\piper_env_cuda
%LOCALAPPDATA%\kgtts-trainer\runtimes\voxcpm_env
%LOCALAPPDATA%\kgtts-trainer\models\voxcpm2
```

说明：

- `Piper CUDA 运行时` 由内置 micromamba 在线创建。
- `VoxCPM2 运行时` 也由内置 micromamba 在线创建。
- `VoxCPM2` 主模型和 denoiser 会从 ModelScope 下载到用户数据目录。
- 打包产物不会把这些在线下载的运行时和权重一起打进去。

## 4. 项目目录结构

训练器每次都会围绕一个项目目录工作。一个典型项目大致如下：

```text
你的项目目录/
  work/
    metadata.csv
    kigtts_project.json
    training.log
    preview.txt
    onnx/
    processed/
    segments/
  export/
    voicepack.kigvpk
```

不同模式下还会出现额外目录：

### 4.1 Piper 标准

```text
work/
  input_audio/
  processed/
  segments/
  transcripts.jsonl
  metadata.csv
```

- 原始音频会归档到 `work/input_audio/`
- `metadata.csv` 格式固定为：

```text
audio_path|text
```

### 4.2 GPT-SoVITS 蒸馏

```text
work/
  distill_corpus/
    texts.jsonl
    wavs/
  gsv_distill.log
  metadata.csv
```

### 4.3 VoxCPM2 蒸馏

```text
work/
  voxcpm_corpus/
    texts.jsonl
    wavs/
  references/
    voxcpm_reference.wav
  voxcpm_distill.log
  metadata.csv
```

- 如果你提供了参考音频，训练器会把它归档到 `work/references/`，方便旧项目恢复。

### 4.4 项目配置文件

每次训练完成准备阶段后，训练器都会写入：

```text
work/kigtts_project.json
```

里面会保存：

- 当前训练模式
- 训练参数
- 蒸馏参数
- 文本来源记录
- 原始音频归档路径
- 项目当时的 `metadata` 文本记录

它是“从旧项目继续训练”的核心依据。

## 5. 文本来源规则

`GPT-SoVITS 蒸馏` 和 `VoxCPM2 蒸馏` 共用同一套文本来源逻辑。

支持来源：

- 内置预设文本
  - `5 万字`
  - `10 万字`
  - `15 万字`
- 自定义文本文件
  - `.txt`
  - `.csv`
  - `.jsonl`
- 旧训练项目目录

解析规则：

- `.txt`
  - 按非空行读取
- `.csv`
  - 必须包含 `text` 列
- `.jsonl`
  - 每行必须有 `text` 字段
- 旧项目目录
  - 读取 `<project>/work/metadata.csv`
  - 取每行首个 `|` 后的文本

额外说明：

- 多来源会按添加顺序合并。
- 当前不会自动去重。
- 空文本会被过滤。
- 默认更推荐先用内置 `10 万字` 版本作为蒸馏起点。

## 6. 标准流程：Piper 标准

适合你手里已经有一批自己的录音文件，想从头训练语音包。

### 6.1 操作步骤

1. 选择训练模式 `Piper 标准`
2. 选择输出目录
3. 导入原始音频
4. 选择 `ASR 模型 zip`
5. 按需设置：
   - `音频等级`
   - `采样率`
   - `训练 batch_size`
   - `Piper 基线 ckpt`
   - `Piper 训练设备`
6. 需要 GPU 时，先安装 `Piper CUDA 运行时`
7. 填好语音包信息
8. 点击 `开始训练语音包`

### 6.2 流程说明

标准流程会按顺序执行：

- 预处理音频
- VAD 切分
- ASR 转写
- 写入 `metadata.csv`
- Piper 训练
- 导出 ONNX
- 生成试听
- 打包 `voicepack.kigvpk`

### 6.3 旧项目恢复逻辑

如果该项目之后以 `从旧项目继续训练` 打开：

- 当 `metadata.csv` 和音频都完整时，会直接跳过切分与 ASR，进入训练。
- 当音频条目不对、`metadata.csv` 异常、或者音频缺失时：
  - 只要项目里还保留了 `work/input_audio/` 原始音频
  - 就会重新走标准流程
- 如果需要重跑，但项目配置里已经没有可用原始音频，就无法继续。

## 7. 蒸馏流程：GPT-SoVITS

适合你已经有 GSVI / GPT-SoVITS 兼容整合包和说话人模型，想跳过重新录音、切分和 ASR，直接生成教师语料再训练 Piper。

### 7.1 需要准备什么

- 一个兼容的 `GSVI / GPT-SoVITS` 整合包根目录
- 根目录至少需要有：

```text
runtime/python.exe
models/
GPT_SoVITS/configs/tts_infer.yaml
```

- 训练器当前按 `models/<version>/<speaker>` 扫描说话人模型

应用内也内置了索引页入口：

- https://www.yuque.com/baicaigongchang1145haoyuangong/ib3g1e/gos50nrqrlipryqq

### 7.2 操作步骤

1. 切换到 `GPT-SoVITS 蒸馏`
2. 选择并校验 `GPT-SoVITS 根目录`
3. 选择：
   - 版本
   - 说话人
   - 参考语言
   - 情感
4. 添加文本来源
5. 调整推理参数
6. 可先用 `语音合成预览` 试听单条效果
7. 点击 `开始训练语音包`

### 7.3 训练器会做什么

训练器不会启动长期 HTTP 服务，而是直接调用整合包自带 `runtime/python.exe` 去跑 helper：

- 读取文本来源
- 调用 GPT-SoVITS 教师模型批量合成 `wav`
- 写入 `work/distill_corpus/wavs/`
- 生成训练器自己的 `work/metadata.csv`
- 继续走 Piper 训练与导出

也就是说，这个模式的重点是：

- 不重新录音
- 不重新做标准流程 ASR
- 直接把教师模型生成的语料喂给 Piper

### 7.4 使用声明与署名确认

在该模式下点击开始训练时，训练器会弹出软件内声明：

- 说明当前模式只负责“教师语料生成 + Piper 训练 + 语音包导出”
- 不负责 GPT-SoVITS 模型本体导出
- 不替用户处理外部模型、整合包和生成内容的授权问题

随后还会要求你完成署名确认。你可以勾选“下次不再提示”，但前提是你已经清楚相关署名责任。

### 7.5 旧项目恢复逻辑

如果以 `从旧项目继续训练` 打开 GPT-SoVITS 项目：

- 音频完整时，直接进入训练
- 部分音频缺失时，训练器会尝试按项目里保存的 GPT-SoVITS 配置补生成
- 如果缺失音频对应模型已经不可用：
  - 但项目里仍有一部分音频存在
  - 训练器会移除缺失条目，继续训练剩余语料
- 如果音频完全缺失且无法按项目配置补生成，则无法开始训练

## 8. 蒸馏流程：VoxCPM2

适合你想通过音色描述、参考音频克隆或高保真克隆快速生成大量训练语料。

### 8.1 需要准备什么

- 先在应用内安装 `VoxCPM2 运行时`
- 再下载 `VoxCPM2 模型`
- 如果启用了 `denoiser`，也需要把 denoiser 一起下载好

默认下载位置：

```text
%LOCALAPPDATA%\kgtts-trainer\runtimes\voxcpm_env
%LOCALAPPDATA%\kgtts-trainer\models\voxcpm2
```

### 8.2 三种声音生成模式

#### `声音设定`

- 只用文字描述声音
- 不需要参考音频
- 适合先快速试风格

#### `可控声音克隆`

- 使用参考音频决定音色
- 可再额外填写风格控制描述
- 适合“以某条参考音为基础，再调节语气/情绪”

#### `高保真克隆（需要调用 ASR）`

- 使用参考音频和精确的参考文本做 prompt
- 音色还原优先级最高
- 如果不手填参考文本，就会调用训练设置里的 ASR 模型自动转写

### 8.3 操作步骤

1. 切换到 `VoxCPM2 蒸馏`
2. 安装 `VoxCPM2 运行时`
3. 下载 `VoxCPM2 模型`
4. 选择声音生成模式
5. 视模式填写：
   - 音色描述
   - 参考音频
   - 参考音频转写文本
6. 添加文本来源
7. 调整推理参数
8. 可先试听单条音频
9. 点击 `开始训练语音包`

### 8.4 训练器会做什么

- 先汇总文本
- 用 VoxCPM2 合成 `wav`
- 输出到 `work/voxcpm_corpus/wavs/`
- 生成 `work/metadata.csv`
- 继续走 Piper 训练和 `.kigvpk` 导出

### 8.5 CPU 回退与 denoiser 重试

- 如果你请求使用 `CUDA`，但当前机器没有可用 CUDA：
  - 且开启了允许回退 CPU
  - 训练器会提示并回退到 CPU
- 如果 `denoiser` 初始化失败：
  - 训练器会自动关闭 denoiser 再重试一次

### 8.6 旧项目恢复逻辑

如果以 `从旧项目继续训练` 打开 VoxCPM2 项目：

- 音频完整时，直接进入训练
- 缺失音频时，会按项目保存的 VoxCPM2 配置继续补生成
- 如果补生成后仍然缺失，则不会继续训练

## 9. 从旧项目继续训练

这个模式适合恢复以前的训练结果，不需要你重新手填一遍参数。

### 9.1 旧项目目录要求

至少需要包含：

```text
work/metadata.csv
work/kigtts_project.json
```

### 9.2 打开旧项目后会显示什么

界面会显示：

- 项目模式
- `metadata` 条数
- 现有音频数 / 缺失音频数
- 原始音频归档状态
- 当前素材准备状态
- 已保存的配置摘要

### 9.3 开始训练前的判断

训练器会先判断当前项目能否直接训练：

- 能直接训练：直接进入 Piper 训练
- 需要补素材：会弹出软件内确认框，再按项目配置补生成或重跑
- 无法补素材：直接报错并阻止开始

## 10. 试听、导出与日志

### 10.1 试听

训练器现在有三类试听：

- `语音包试听`
  - 针对已经导出的 Piper / ONNX 语音包
- `GPT-SoVITS 蒸馏试听`
  - 用当前 GPT-SoVITS 模型和参数生成单条音频
- `VoxCPM2 蒸馏试听`
  - 用当前 VoxCPM2 模式和参数生成单条音频

蒸馏试听只用于验证当前参数，不会写入正式训练语料。

### 10.2 导出结果

主导出文件固定为：

```text
export/voicepack.kigvpk
```

兼容说明：

- `.kigvpk` 本质上仍是 zip 结构
- Android 端和其它 KIGTTS 组件可以直接识别

### 10.3 常见日志文件

- 标准训练日志：

```text
work/training.log
```

- GPT-SoVITS 蒸馏日志：

```text
work/gsv_distill.log
```

- VoxCPM2 蒸馏日志：

```text
work/voxcpm_distill.log
```

当你需要排查失败原因时，优先看这些文件。

## 11. 显存不足时会发生什么

训练器对一部分 GPU OOM 做了自动降级重试。

### 11.1 GPT-SoVITS 蒸馏

出现显存不足时，训练器会按顺序尝试：

1. 关闭 `parallel_infer`
2. 将蒸馏 `batch_size` 对半降低，最低到 `1`
3. 仍失败则切到 `CPU`

### 11.2 Piper 训练

出现显存不足时，训练器会按顺序尝试：

1. 将训练 `batch_size` 对半降低，最低到 `1`
2. 仍失败则切到 `CPU`

这些动作都会写入日志，并在进度区提示当前设备和批大小。

## 12. 常见问题

### 12.1 `请先安装 Piper CUDA 运行时`

原因：

- 你把 `Piper 训练设备` 设成了 `GPU/CUDA`
- 但还没有安装 `piper_env_cuda`

处理：

- 先到训练设置页安装 `Piper CUDA 运行时`
- 如果只想先跑通流程，可先切回 `CPU`

### 12.2 `请先选择并校验 GPT-SoVITS 根目录`

原因：

- 根目录未选
- 目录结构不完整
- 缺少 `runtime/python.exe`、`models/` 或 `GPT_SoVITS/configs/tts_infer.yaml`

处理：

- 重新选择兼容的 GSVI / GPT-SoVITS 整合包根目录

### 12.3 `蒸馏文本为空`

原因：

- 文本来源虽然添加了，但实际没有解析出有效文本
- 常见于空文件、编码异常或 `csv/jsonl` 字段不符合规则

处理：

- 检查 `.txt/.csv/.jsonl`
- 确认 `csv` 包含 `text` 列
- 确认 `jsonl` 每行有 `text`

### 12.4 `高保真克隆需要参考文本`

原因：

- 你选择了 `VoxCPM2 高保真克隆`
- 但既没有手填参考文本，也没有配置可用的 ASR 模型

处理：

- 手动填写 `参考音频转写文本`
- 或者在训练设置中补上 `ASR 模型 zip`

### 12.5 `旧项目无法继续训练`

常见原因：

- `work/kigtts_project.json` 丢失
- `work/metadata.csv` 丢失
- 原始音频或蒸馏音频已经被清理
- GPT-SoVITS 项目的外部模型路径已不可用

处理：

- 优先保留完整项目目录，不要只保留导出文件
- 需要长期归档时，至少保留 `work/` 与 `export/`

## 13. 文档入口

与训练器相关的其它文档：

- [Electron_Trainer/README](../../Electron_Trainer/README.md)
- [模型与素材下载索引](../../MODEL_ASSETS.md)
- [隐私政策](../../docs/legal/PRIVACY_POLICY.md)
- [开源许可证说明](../../docs/legal/OPEN_SOURCE_LICENSES.md)
