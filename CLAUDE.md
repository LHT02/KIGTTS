# CLAUDE.md — KIGTTS Flutter 重写规范

## 项目背景

将 Android 原生 App（Kotlin/Jetpack Compose，代码在 `android-app/`）用 Flutter 完全重写，实现前后端分离。Piper TTS 引擎、sherpa-onnx ASR、AEC3/eSpeak JNI 等原生引擎保持不变，通过 MethodChannel 桥接。UI 还原原始 App 设计。

原始代码参考：`android-app/app/src/main/java/com/kgtts/app/` 下的所有 `.kt` 文件及 `cpp/` 下的 JNI 代码。

---

## 架构要求

采用 **Clean Architecture** 四层分离：

- **Presentation**：Flutter Widget + Cubit/Bloc，纯 UI，不含业务逻辑
- **Domain**：实体、Repository 接口、UseCase，纯 Dart，零平台依赖
- **Data**：Repository 实现、DTO、本地存储
- **Services/Platform**：MethodChannel / FFI 桥接原生引擎

关键原则：
- UI 层禁止直接调用 MethodChannel，必须经过 Repository/Service 层
- Domain 层禁止导入 `dart:io`、`flutter/*` 等平台包
- 原始 `Engines.kt`（2235 行）必须拆分为独立的 Engine 类（AsrEngine、PiperTtsEngine、AudioPlayer、Aec3Processor 等），每个类对应一个 MethodChannel
- 原始 `RealtimeController` 保留在 Kotlin 原生侧（因为直接操作 AudioRecord/AudioTrack/JNI），通过 MethodChannel 暴露 start/stop/updateSettings/enqueueTts 接口，通过 EventChannel 推送识别结果/音量/进度/错误
- 原始 `MainViewModel`（12830 行的 God Object）必须拆分为多个 Cubit：RealtimeCubit、ModelManagerCubit、SettingsCubit、QuickSubtitleCubit、QuickCardCubit、DrawingCubit、OverlayCubit、SpeakerVerifyCubit
- FloatingOverlayService 保留 Android 原生实现，Flutter 通过 MethodChannel 控制

## 技术栈

- **Flutter** >= 3.22、**Dart** >= 3.4
- 状态管理：**flutter_bloc**（Cubit 优先）
- 路由：**go_router**
- DI：**get_it** + **injectable**
- 实体类：**freezed** + **json_serializable**
- 本地存储：**shared_preferences**（设置）+ 文件系统（模型）
- 原生桥接：**MethodChannel** + **EventChannel**

## 代码规范

- 遵循 Effective Dart，启用 flutter_lints 最严格规则
- **单文件不超过 500 行**，Widget 文件不超过 300 行，超过必须拆分
- 文件名 `snake_case`，类名 `PascalCase`，MethodChannel 命名 `com.kgtts.app/{module}`
- 禁止在 Widget 中执行 IO/计算，禁止 `setState` 管理跨组件状态
- 原生侧每个 Channel handler 方法不超过 30 行，所有异常必须 catch 并通过 `result.error()` 返回
- 所有 MethodChannel 调用必须有 try-catch，错误传递到 UI 层展示

## UI 要求

### 必须还原的页面（按优先级）

**P0**：实时转换页、模型管理页（含音色包详情 BottomSheet）、设置页
**P1**：快捷字幕、快捷卡片、画板、QR 扫描、悬浮窗控制、Push-to-Talk、说话人验证
**P2**：日志查看页

### 导航与布局

- **侧边导航抽屉 + 顶部工具栏**（不得改为底部导航栏）
- 侧边栏支持展开/折叠，横屏默认展开，竖屏默认迷你模式
- 必须支持竖屏和横屏自适应

### 主题

- 深色主题为默认，同时支持亮色跟随系统
- 使用 Material Symbols Sharp 图标字体
- 圆角、间距、颜色等视觉参数与原始 App 保持一致

### 组件

- 列表项、卡片、对话框等必须抽取为独立 Widget
- Slider 吸附行为必须还原（如播放增益在 100% 附近吸附）
- 页面切换需有 Fade/Slide 过渡动画
- Edge-to-Edge 适配

## 兼容性要求

- voicepack.zip 和 sosv.zip 格式必须与原始完全兼容
- `files/models/` 下已导入模型的目录结构必须兼容，用户升级不丢数据
- UserPrefs 的所有 key 和默认值与原始 `UserPrefs.kt` 保持一致

## 禁止事项

1. 不要重新实现 Piper 推理 / sherpa-onnx ASR / AEC3 / eSpeak — 保留原生侧
2. 不要用 Flutter 实现系统悬浮窗 — 保留 Android 原生 FloatingOverlayService
3. 不要在 Flutter UI 层直接调用 MethodChannel
4. 不要把所有状态塞进一个 Cubit（避免重蹈 MainViewModel 的覆辙）

## 验收标准

### 功能（P0 — 必须通过）

| # | 验收项 | 通过标准 |
|---|--------|---------|
| F01 | 离线 ASR | 断网下正确识别中文并显示 |
| F02 | 离线 TTS | 识别完一句后自动用加载的音色播报 |
| F03 | ASR/TTS 并行 | 播放不中断识别，识别不中断播放 |
| F04 | 模型导入 | sosv.zip 和 voicepack.zip 导入后状态正常 |
| F05 | 音色包切换 | 切换后音色明显变化 |
| F06 | 音色包管理 | 编辑名称/备注/头像、删除、导出均正常 |
| F07 | 设置持久化 | 杀进程重启后设置不丢失 |
| F08 | Piper 调参 | noiseScale/lengthScale/noiseW 调节实时生效 |
| F09 | 稳定性 | 连续运行 30 分钟无崩溃无明显内存泄漏 |
| F10 | 回声控制 | 外放播报时不出现明显复读 |

### 功能（P1）

| # | 验收项 | 通过标准 |
|---|--------|---------|
| F11 | 快捷字幕 | 点击后 TTS 播报 |
| F12 | 快捷卡片 | 图片/QR/文本三种类型正常 |
| F13 | Push-to-Talk | 按住说话→松手→播报 |
| F14 | 画板 | 画/选色/擦除/保存 |
| F15 | QR 扫描 | 扫码导入模型 |
| F16 | 悬浮窗 | 正常显示、控制、拖拽 |
| F17 | 保活 | 锁屏后 ASR/TTS 持续运行 |
| F18 | 说话人验证 | 非目标说话人不触发 TTS |
| F19 | AEC3 | 开启后复读减少 |
| F20 | 横竖屏 | 切换后 UI 正常 |

### 架构

| # | 验收项 | 通过标准 |
|---|--------|---------|
| A01 | 前后端分离 | UI 层无 MethodChannel 直接调用 |
| A02 | 分层清晰 | Domain 层无平台依赖 |
| A03 | 文件大小 | 无 Dart 文件超过 500 行 |
| A04 | 状态管理 | 跨组件状态均通过 Cubit/Bloc |
| A05 | DI | Service/Repository 均通过 get_it 注入 |
| A06 | 原生拆分 | Engines.kt 拆分为独立 Engine 类 + Channel |
| A07 | 错误处理 | Channel 调用有 try-catch，错误传到 UI |

### UI

| # | 验收项 | 通过标准 |
|---|--------|---------|
| U01 | 导航 | 侧边抽屉 + 顶栏，支持展开/折叠 |
| U02 | 深色主题 | 默认深色，视觉与原始一致 |
| U03 | 亮色主题 | 跟随系统切换 |
| U04 | 音色包卡片 | 布局/头像/置顶标记一致 |
| U05 | 设置页 | Slider 范围和吸附行为一致 |
| U06 | 动画 | 页面切换有过渡动画 |
| U07 | Edge-to-Edge | 适配状态栏和导航栏 |

### 性能

| # | 验收项 | 通过标准 |
|---|--------|---------|
| P01 | 首包延迟 | < 1.2s（中端机） |
| P02 | UI 帧率 | >= 55 fps |
| P03 | 内存 | 实时运行 < 500MB |
| P04 | 冷启动 | < 3s |
