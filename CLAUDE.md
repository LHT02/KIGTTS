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
| U08 | 设计 | 确保 UI 与原始 App 视觉上完全对应|

### 性能

| # | 验收项 | 通过标准 |
|---|--------|---------|
| P01 | 首包延迟 | < 1.2s（中端机） |
| P02 | UI 帧率 | >= 55 fps |
| P03 | 内存 | 实时运行 < 500MB |
| P04 | 冷启动 | < 3s |

---

## 待办改造计划（UI/交互还原）

> 以下内容基于原始 `android-app/` 代码（`MainActivity.kt` 13071 行）与当前 Flutter 实现的逐项对比，列出所有需要补齐的差距。

---

### 一、录音模式交互改造（高优先级）

#### 1.1 现状

当前 Flutter 实时转换页（`realtime_page.dart`）底部只有一个小 `FloatingActionButton`（toggle start/stop），没有三种录音模式切换。便捷字幕页（`quick_subtitle_page.dart`）的输入栏有键盘/PTT 模式切换，但行为与原始 App 不一致。

#### 1.2 原始 App 设计

原始 App **不在实时转换页使用 FAB**，而是将 FAB 放在**便捷字幕页**（即主页）。通过两个 boolean 组合实现三种模式：

| `pushToTalkMode` | `pushToTalkConfirmInputMode` | 模式名称 | 行为 |
|---|---|---|---|
| `false` | - | **持续监听** | 点击 FAB 切换 start/stop，VAD 自动检测+自动 TTS，识别文本自动上屏 |
| `true` | `false` | **简单 PTT** | 按住 FAB 开始录音，松手停止，自动 TTS 播报 |
| `true` | `true` | **确认 PTT** | 按住 FAB 录音+流式识别预览，松手根据拖动方向选择「上屏/输入文本框/取消」 |

模式切换在**设置页**（Switch 开关），不是通过手势或滑动切换。

#### 1.3 需改造的文件

| 文件 | 改造内容 |
|------|---------|
| `realtime_page.dart` + `running_controls.dart` | 移除当前的实时转换页底部 FAB；实时转换页仅作为"查看识别结果列表 + 状态栏"的辅助页面，不再包含启动/停止按钮 |
| `quick_subtitle_page.dart` | 主页底部添加**浮动 FAB**（`QuickSubtitleMicFab`），支持持续监听模式的 click 切换和 PTT 模式的按住手势 |
| `subtitle_input_bar.dart` | 保留键盘/PTT 切换逻辑，PTT 按钮改为 `GestureDetector` + `pointerInteropFilter` 实现真正的按住→拖动→松手三段手势 |
| `realtime_cubit.dart` + `realtime_state.dart` | 添加 `pushToTalkConfirmInputMode` 字段，添加 `beginPttSession()` / `commitPttSession(action)` 方法 |
| `settings_page.dart` → `recognition_section.dart` | 添加"按住说话"Switch + "确认输入模式"Switch（当 PTT 开启时显示） |
| `RealtimeChannel.kt`（原生侧） | 已有 `beginPttSession` / `commitPttSession` handler，无需改动 |

#### 1.4 FAB 交互规格（还原原始 `QuickSubtitleMicFab`）

**非 PTT 模式**：
- `onClick = onToggleMic`（切换 start/stop）
- 图标：运行中 `stop`，停止 `play_arrow`

**PTT 模式**：
- `onClick` 为空（不响应点击）
- 通过 `Listener` / `GestureDetector` 拦截触摸事件：
  - `ACTION_DOWN`：记录起始坐标，设 `pttPressed=true`，触发 `beginPttSession`
  - `ACTION_MOVE`：计算拖动方向，更新 `dragTarget`（三区域判定）
  - `ACTION_UP`：根据 `dragTarget` 决定 action → `SendToSubtitle` / `SendToInput` / `Cancel`
  - `ACTION_CANCEL`：自动 Cancel
- 图标：按住时 `settings_voice`，未按住 `mic`（Crossfade 动画）

**拖动三区域判定逻辑**（仅确认 PTT 模式生效）：
- 下方区域（`dy >= -56dp`）→ `DefaultSend`
- 上方左侧（`dx < -12dp`）→ `ToInput`
- 上方右侧（`dx >= -12dp`）→ `Cancel`
- 横屏紧凑模式（IME 可见时）：左拖 `ToInput`，右拖 `Cancel`

---

### 二、顶栏 Actions 还原（中优先级）

#### 2.1 现状

`app_scaffold.dart` → `_actions` 只对两个页面配置了 actions：
- `AppRoutes.home` → `FullscreenAction`（全屏，未实现）
- `AppRoutes.settings` → `LogAction`（跳转日志页）

其他页面的顶栏右侧**完全为空**。

#### 2.2 原始 App 各页面的顶栏按钮

| 页面 | 顶栏右侧按钮 | 图标 |
|------|-------------|------|
| **便捷字幕**（主页面） | 全屏预览 | `fullscreen` |
| **快捷名片 - 主页面** | 新建 + 扫码 + 排序 | `add` + `qr_code_scanner` + `sort` |
| **快捷名片 - 编辑** | 复制 + 删除 | `content_copy` + `delete` |
| **快捷名片 - 排序** | 确认 | `check` |
| **快捷名片 - 扫码/结果/网页** | 网页模式有 reload/back/forward | `refresh` + `arrow_back` + `arrow_forward` |
| **画板** | 保存 + 恢复 | `save` + `settings_backup_restore` |
| **语音包** | 导入语音包 | `folder_open` |
| **设置 - 主页面** | 打开日志 | `article` |
| **设置 - 日志** | 刷新 + 复制 + 分享 | `refresh` + `content_copy` + `share` |

#### 2.3 需改造的文件

| 文件 | 改造内容 |
|------|---------|
| `top_bar_actions.dart` | 新增 Action Widget：`VoicePackImportAction`、`DrawingSaveAction`、`QuickCardAddAction`、`QuickCardScanAction`、`QuickCardSortAction`、`LogRefreshAction`、`LogCopyAction`、`LogShareAction` |
| `app_scaffold.dart` → `_actions` | 按 `currentPath` 返回正确的 actions 列表 |
| `log_viewer_page.dart` | 添加刷新/复制/分享回调，通过 `InheritedWidget` 或 callback 传递给顶栏 |
| `model_manager_page.dart` | 导入按钮移到顶栏（或同时保留 FAB + 顶栏入口） |
| `drawing_page.dart` | 保存功能绑定到顶栏按钮 |

#### 2.4 动态标题

原始 App 在子页面（编辑字幕、编辑名片、日志等）时会动态切换标题和导航图标（`←` 返回箭头替代 `☰` 菜单）。当前 Flutter 的 `TopBar` 不支持子页面标题切换。

需在 `app_scaffold.dart` 中实现：
- 各页面通过 `InheritedWidget` 或 `Provider` 向 `AppScaffold` 上报子页面标题和 actions
- 导航图标在子页面内时变为返回箭头

---

### 三、便捷字幕页补全（中优先级）

#### 3.1 现状 vs 原始差距

| 功能 | 原始 App | Flutter 当前 | 状态 |
|------|---------|-------------|------|
| 字幕显示卡片 | ✅ | ✅ | OK |
| 预设短语行 | ✅ | ✅ | OK |
| 分组 Tab | ✅ | ✅ | OK |
| 键盘输入 + 发送 | ✅ | ✅ | OK |
| PTT 按住说话 | ✅ | ⚠️ 有基本框架但行为不完整 | 需完善 |
| 浮动 FAB（录音/停止） | ✅ 在便捷字幕页右下角 | ❌ 不存在 | **需新增** |
| 历史记录页 | ✅ 子页面 | ❌ 无路由 | **需新增** |
| 编辑字幕页 | ✅ 子页面 | ❌ 无路由 | **需新增** |
| 全屏字幕预览 Dialog | ✅ | ❌ | **需新增** |
| 粗体/居中/清屏/历史 按钮行 | ✅ | ❌ | **需新增** |
| 复制字幕（长按） | ✅ | ❌ | **需新增** |

#### 3.2 需新增的文件

- `quick_subtitle_history_page.dart` — 历史记录子页面
- `quick_subtitle_editor_page.dart` — 编辑字幕子页面
- `subtitle_fullscreen_dialog.dart` — 全屏字幕预览弹窗
- `quick_subtitle_fab.dart` — 浮动录音 FAB（支持持续监听 + PTT 两种模式）

---

### 四、各页面其他缺失功能

#### 4.1 语音包页

| 缺失功能 | 说明 |
|---------|------|
| 顶栏导入按钮 | 原始 App 在顶栏有 `folder_open` 导入按钮（当前只有底部 FAB） |
| 语音包排序/置顶 | 原始 App 支持拖拽排序和置顶，Flutter 有 `ReorderableListView` 框架但 `onReorder` 是 TODO |

#### 4.2 画板页

| 缺失功能 | 说明 |
|---------|------|
| 保存按钮 | 原始 App 顶栏有保存按钮，Flutter 的保存是 TODO |
| 全屏模式 | 原始 App 支持画板全屏（隐藏 TopBar + 侧栏），Flutter 未实现 |

#### 4.3 日志页

| 缺失功能 | 说明 |
|---------|------|
| 顶栏：刷新/复制/分享 | 原始 App 在日志页顶栏有三个按钮 |
| 自动滚动到底部 | 原始 App 日志实时滚动 |

#### 4.4 悬浮窗控制页

| 缺失功能 | 说明 |
|---------|------|
| 自动吸附开关 | `onChanged` 是空（TODO），需连接 `SettingsCubit` |

---

### 五、构建配置注意事项

以下修改已应用但**不在 git 中**，每次 clean/checkout 后需确认：

| 文件 | 修改 | 原因 |
|------|------|------|
| `flutter_app/android/app/build.gradle.kts` | `ndkVersion = "27.0.12077973"` | 插件依赖 NDK 27 |
| 同上 | `cmake { version = "3.31.6" }` | CMake 3.22 不兼容 NDK 27 |
| 同上 | `cmake { abiFilters("arm64-v8a") }` | 避免 x86_64 的 SSE2 链接错误 |
| 同上 | `aaptOptions { noCompress += listOf("zip") }` | assets 中的大 zip 不被压缩 |
| `flutter_app/android/gradle.properties` | `kotlin.incremental=false` | Pub Cache (C:) vs 项目 (D:) 跨盘导致 Kotlin daemon 崩溃 |
| `patch_aec3.cmake` | 注入 `cmake_policy(SET CMP0057 NEW)` | NDK 27 的 `flags.cmake` 依赖 `IN_LIST` |
| `patch_espeak.cmake` | 同上 | 同上 |
| `espeak_jni.cpp` | `Java_com_kgtts_kgtts_1app_audio_*` | Flutter 包名 `com.kgtts.kgtts_app` 需要 `_1` 转义 |
| `aec3_jni.cpp` | 同上 | 同上 |

---

### 六、改造优先级与工作量估算

| 优先级 | 任务 | 涉及文件数 | 估算工作量 |
|--------|------|-----------|-----------|
| **P0** | 便捷字幕页浮动 FAB + 持续监听模式 | ~5 | 大 |
| **P0** | PTT 按住说话完整手势（三区域拖动） | ~4 | 大 |
| **P0** | 设置页录音模式开关（PTT / 确认 PTT） | ~2 | 小 |
| **P1** | 顶栏 actions 还原（所有页面） | ~3 | 中 |
| **P1** | 顶栏动态标题 + 子页面返回箭头 | ~2 | 中 |
| **P1** | 便捷字幕：历史记录子页面 | ~2 新增 | 中 |
| **P1** | 便捷字幕：编辑子页面 | ~2 新增 | 中 |
| **P1** | 便捷字幕：全屏预览 + 粗体/居中/清屏工具栏 | ~2 新增 | 小 |
| **P1** | 便捷字幕：复制（长按） | ~1 | 小 |
| **P2** | 语音包排序/置顶持久化 | ~2 | 小 |
| **P2** | 画板保存 + 全屏 | ~2 | 中 |
| **P2** | 日志页顶栏按钮（刷新/复制/分享） | ~2 | 小 |
| **P2** | 悬浮窗自动吸附开关连通 | ~1 | 小 |

---

### 七、建议改造顺序

1. **先让核心功能跑通**：在便捷字幕页实现浮动 FAB + 持续监听模式，验证 ASR→TTS 全链路
2. **补全 PTT 手势**：实现按住说话 + 确认拖动手势
3. **补全设置联动**：在设置页添加 PTT / 确认 PTT 开关
4. **还原顶栏 actions**：逐页面补齐
5. **补全子页面**：历史记录、编辑字幕、全屏预览等
6. **收尾**：画板保存、日志按钮、悬浮窗开关等
