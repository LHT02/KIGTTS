# KIGTTS 安卓主软件待办与调试基线

> 更新时间：2026-04-14
>
> 本文件只记录 **当前安卓主软件 `android-app/` 主线** 的待办、已知风险和调试边界。
> 若会话上下文过长、实验改动过多或出现模型/分支记忆混杂，后续开发应优先回到本文件与 `github-main-clean` 最近几次提交核对真实状态。

## 1. 代码边界

- 当前安卓主软件：`android-app/`
- 当前主工作分支：`github-main-clean`
- 参考提交基线：
  - `fa589a449` `feat(android): unify realtime host and refresh model docs`
  - `1f8d81535` `fix(android): smooth overlay dock animation`
  - `b2342bf34` `feat(android): default to system tts and safer audio settings`
  - `0df1b6de1` `refactor(android): move realtime control to host service`

## 2. Flutter 边界说明

- `flutter_app/` 是**另一位开发者维护的并行 Flutter 分支版本**。
- 它不是当前安卓主软件的主线实现，也不是当前 Android 行为对齐、回归测试、音频链路排查的默认基线。
- 当前 Android 相关问题：
  - 优先检查 `android-app/`
  - 必要时仅将 Flutter 端作为“历史可用实现思路参考”，不要把其行为视为当前主线既定事实。

## 3. 当前安卓主线音频架构

- `RealtimeHostService`
  - 作为进程级音频宿主
  - 持有 ASR / TTS / 录音控制器
  - 主界面与悬浮窗都连接它
- `MainActivity`
  - 负责 UI 与页面导航
  - 不再作为音频主控宿主
- `FloatingOverlayService`
  - 负责悬浮窗 UI 与控制入口
  - 不再单独持有第二套音频引擎

## 4. 当前模型与默认策略

- 默认 TTS：系统 TTS
- 可选 TTS：Piper ONNX 音色包（导入 `.kigvpk` / `.zip`）
- ASR：SenseVoice（sherpa-onnx）
- VAD：
  - 原有阈值式 VAD
  - Silero VAD
- 语音增强：
  - Sherpa GTCRN（语句级）
  - Sherpa GTCRN（流式）
  - Sherpa DPDFNet2（流式）
  - Sherpa DPDFNet4（流式）
- 说话人验证：
  - sherpa-onnx 官方 speaker embedding 模型

## 5. 当前重点待办

### 5.1 Piper TTS / ORT 调试

现状：
- 用户反馈：加了语音增强之后，Piper TTS 无法正常启动；此前是正常的。
- 用户已确认：
  - 语音包本身没问题
  - 语音包已手动装好
  - 希望保留“原来的 Java ORT Piper 后端”

当前代码状态：
- 已回到 Java ORT 的 `PiperTtsEngine`
- 当前 ORT 依赖已回退为 `onnxruntime-android:1.17.1`
- 当前 sherpa AAR 使用 `sherpa-onnx-1.12.38-noort.aar`

仍需继续确认的点：
- Piper TTS 当前是否仍在特定设备上触发 `libonnxruntime4j_jni.so` / `OrtGetApiBase` 相关 native 组合问题
- ORT native 加载方式是否应完全回到旧版：
  - 不手动 `System.loadLibrary("onnxruntime")`
  - 仅通过 `OrtEnvironment.getEnvironment()` 触发 Java API 自己加载 JNI
- 需要继续确认：
  - APK 实际打入的 `libonnxruntime.so` / `libonnxruntime4j_jni.so` 是否与旧可用实现完全一致
  - Piper 启动失败是否还被错误映射成“麦克风启动失败”

### 5.2 语音增强与 TTS 联动回归

需要继续验证：
- 开启任一语音增强模式后：
  - Piper TTS 是否仍能正常初始化
  - 系统 TTS / Piper 切换是否稳定
  - 麦克风实时识别启动是否被 TTS 初始化失败拖死

### 5.3 悬浮窗 FAB 贴边

当前方向：
- 已切到“窗口留在屏内 + 视觉裁切半露”
- 仍需继续观察：
  - 左侧贴边动画
  - 半露裁切时的阴影过渡
  - 不同设备下是否仍有吸附距离异常

## 6. 当前调试原则

- 优先以 `android-app/` 当前文件状态为准，不沿用旧会话记忆推断“应该是什么”。
- 每轮改动前，先重新读取目标代码块，避免把已回退内容当成仍然存在。
- 遇到主线行为混乱时，先核对：
  - `git log --oneline github-main-clean -10`
  - 本文件
  - `ANDROID_APP_GUIDE.md`

## 7. 下一步推荐顺序

1. 先继续收口 Piper TTS / ORT native 组合问题
2. 再做语音增强模式下的 TTS / 录音联动回归
3. 最后再继续处理悬浮窗 FAB 贴边动画细节
