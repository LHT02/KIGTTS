import { useEffect, useMemo, useRef, useState, type DragEvent as ReactDragEvent, type MouseEvent as ReactMouseEvent, type SyntheticEvent } from 'react'
import {
  Box,
  Button,
  ButtonBase,
  Checkbox,
  Chip,
  Collapse,
  Container,
  CssBaseline,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  Drawer,
  Fab,
  IconButton,
  InputAdornment,
  InputLabel,
  LinearProgress,
  CircularProgress,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Link,
  MenuItem,
  Popover,
  Paper,
  Radio,
  Select,
  Slider,
  Snackbar,
  Slide,
  Stack,
  Switch,
  TextField,
  Tooltip,
  Typography,
  createTheme,
  ThemeProvider,
  Alert,
  Avatar,
} from '@mui/material'
import Cropper from 'react-easy-crop'
import type { Area } from 'react-easy-crop'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import logoBlack from '../../ARTS/LOGOBlack.svg'
import logoWhite from '../../ARTS/LOGOWhite.svg'
import avatarHuajiang from '../../ARTS/Avatar/huajiang.jpg'
import avatarLht from '../../ARTS/Avatar/LHT.jpg'
import avatarYuiLu from '../../ARTS/Avatar/YuiLu.jpg'
import openSourceLicensesText from './legal/open_source_licenses.md?raw'
import privacyPolicyText from './legal/privacy_policy.md?raw'

type ProgressStage = Exclude<PipelineStage, 'idle' | 'preview' | 'runtime'>
type ProgressMap = Record<ProgressStage, number>
type PendingRequest = {
  resolve: (payload: unknown) => void
  reject: (error: Error) => void
  timeout: number
}

type RuntimeStatusWithSources = PiperCudaRuntimeStatus | VoxCpmRuntimeStatus

type CommonPipelineOptions = {
  quality: 'A' | 'B'
  denoise: boolean
  sample_rate: number
  batch_size: number
  asr_model_zip: string | null
  piper_base_checkpoint: string | null
  use_espeak: boolean
  piper_config: string | null
  device: 'cpu' | 'cuda'
  voicepack_name: string
  voicepack_remark: string
  voicepack_avatar: string | null
}

const runtimeSourceLabels: Record<string, string> = {
  aliyun: '阿里云',
  bfsu: '北外',
  tencent: '腾讯云',
  sjtu: '上交',
  sustech: '南科大',
  tuna: '清华',
  ustc: '中科大',
  nju: '南大',
  huawei: '华为云',
  volces: '火山引擎',
  official: '官方源',
  conda: 'Conda',
  tuna_sustech_nvidia: '清华 + 南科大 nvidia',
  bfsu_sustech_nvidia: '北外 + 南科大 nvidia',
  ustc_sustech_nvidia: '中科大 + 南科大 nvidia',
  nju_sustech_nvidia: '南大 + 南科大 nvidia',
}

const getRuntimeSourceLabel = (source?: string | null) => {
  if (!source) return ''
  return runtimeSourceLabels[source] ?? source
}

const formatRuntimeSources = (status?: RuntimeStatusWithSources | null) => {
  if (!status) return ''
  const parts = [
    status.conda_source ? `Conda: ${getRuntimeSourceLabel(status.conda_source)}` : '',
    status.torch_source ? `Torch: ${getRuntimeSourceLabel(status.torch_source)}` : '',
    status.pip_toolchain_source ? `pip工具链: ${getRuntimeSourceLabel(status.pip_toolchain_source)}` : '',
    status.pip_dependency_source ? `pip依赖: ${getRuntimeSourceLabel(status.pip_dependency_source)}` : '',
  ].filter(Boolean)
  if (!parts.length && status.source) {
    parts.push(`来源: ${getRuntimeSourceLabel(status.source)}`)
  }
  return parts.join(' / ')
}

type PendingDistillStart = {
  outputDir: string
  commonOpts: CommonPipelineOptions
  distillPayload: DistillOptions
}

type AboutCreator = {
  name: string
  homepage: string
  avatar: string
}

type DistillTextPresetOption = {
  key: string
  title: string
  fileName: string
  charCountLabel: string
  description: string
  expectedEffect: string
  loadContent: () => Promise<string>
  recommended?: boolean
}

type GsviAttributionFields = {
  gsvAuthor: string
  gsvTrainer: string
  gsvTrainer2: string
  gsviPacker: string
}

const TITLEBAR_HEIGHT = 48
const DRAWER_WIDTH = 240
const MINI_DRAWER_WIDTH = 72
const FAB_SAFE_GUTTER = 120
const CONTENT_SIDE_GUTTER = 30
const PAGE_FADE_OUT_MS = 90
const PAGE_FADE_IN_MS = 210
const NAV_ICON_SLOT = 36
const NAV_EXPANDED_PADDING_X = 1.5
const NAV_COLLAPSED_PADDING_X = (MINI_DRAWER_WIDTH - NAV_ICON_SLOT) / 16
const DRAWER_EXPANDED_STORAGE_KEY = 'kgtts_drawer_expanded'
const DISTILL_SETTINGS_STORAGE_KEY = 'kigtts_gsv_distill_settings'
const VOXCPM_SETTINGS_STORAGE_KEY = 'kigtts_voxcpm_distill_settings'
const DISTILL_MODE_STORAGE_KEY = 'kigtts_training_mode'
const GSVI_CONFIRM_SKIP_STORAGE_KEY = 'kigtts_gsvi_confirm_skip'
const GSVI_MODE_INTRO_SKIP_STORAGE_KEY = 'kigtts_gsvi_mode_intro_skip'
const GSVI_GUIDE_URL = 'https://www.yuque.com/baicaigongchang1145haoyuangong/ib3g1e/gos50nrqrlipryqq'
const PIPER_PIPELINE_STAGES: ProgressStage[] = ['preprocess', 'vad', 'asr', 'train', 'export']
const DISTILL_PIPELINE_STAGES: ProgressStage[] = ['collect', 'distill', 'train', 'export']
const VOXCPM_PIPELINE_STAGES: ProgressStage[] = ['collect', 'synth', 'train', 'export']
const RESUME_PROJECT_PIPELINE_STAGES: ProgressStage[] = ['collect', 'distill', 'synth', 'train', 'export']
const PROGRESS_STAGES: ProgressStage[] = ['collect', 'distill', 'synth', 'preprocess', 'vad', 'asr', 'train', 'export']
const TRAINING_MODE_LABELS: Record<string, string> = {
  piper: 'Piper 标准',
  gsv_distill: 'GPT-SoVITS 蒸馏',
  voxcpm_distill: 'VoxCPM2 蒸馏',
  resume_project: '从旧项目继续训练',
}
type AppPage = 'prep' | 'settings' | 'preview' | 'logs' | 'about'
type AboutDialogKind = 'openSource' | 'privacy' | null
const STAGE_LABEL: Record<ProgressStage, string> = {
  collect: '收集',
  distill: '蒸馏',
  synth: '合成',
  preprocess: '预处理',
  vad: '切分',
  asr: '识别',
  train: '训练',
  export: '导出',
}
const DISTILL_TEXT_LANGS = ['中文', '英语', '日语', '粤语', '韩语', '中英混合', '日英混合', '粤英混合', '韩英混合', '多语种混合', '多语种混合(粤语)']
const DISTILL_SPLIT_METHODS = ['不切', '凑四句一切', '凑50字一切', '按中文句号。切', '按英文句号.切', '按标点符号切']
const VOXCPM_VOICE_MODES: Array<{ value: VoxCpmVoiceMode; label: string; description: string }> = [
  { value: 'description', label: '声音设定', description: '只用括号内音色描述生成新声音，不需要参考音频。' },
  { value: 'controlled_clone', label: '可控声音克隆', description: '用参考音频决定音色，可选音色描述控制情绪、语速和表达。' },
  { value: 'high_fidelity', label: '高保真克隆（需要调用 ASR）', description: '用参考音频和精确转写做 prompt，优先还原音色、节奏和细节。' },
]
const GSVI_REQUIRED_NAMES = {
  gsvAuthor: '花儿不哭',
  gsvTrainer: '红血球AE3803',
  gsvTrainer2: '白菜工厂1145号员工',
  gsviPacker: 'AI-Hobbyist',
} as const
const DISTILL_TEXT_SOURCE_EMPTY_HINT = '点击右上角 + 添加内置预设文本，或导入 / 拖入自定义 .txt、.csv、.jsonl 文本文件。'
const NAV_ITEMS: Array<{ key: AppPage; label: string; icon: string }> = [
  { key: 'prep', label: '训练准备', icon: 'folder' },
  { key: 'preview', label: '语音包试听', icon: 'record_voice_over' },
  { key: 'settings', label: '训练设置', icon: 'tune' },
  { key: 'logs', label: '日志', icon: 'article' },
  { key: 'about', label: '关于', icon: 'info' },
]
const APP_VERSION = __APP_VERSION__

const ABOUT_CREATORS: AboutCreator[] = [
  {
    name: 'LHT',
    homepage: 'https://space.bilibili.com/87244951',
    avatar: avatarLht,
  },
  {
    name: 'Yui Lu',
    homepage: 'https://space.bilibili.com/23208863',
    avatar: avatarYuiLu,
  },
  {
    name: '花酱',
    homepage: 'https://space.bilibili.com/573842321',
    avatar: avatarHuajiang,
  },
]

const DISTILL_TEXT_PRESET_OPTIONS: DistillTextPresetOption[] = [
  {
    key: '50k',
    title: '5 万字版本',
    fileName: '5万字文本库.txt',
    charCountLabel: '约 5 万字',
    description: '适合快速试跑流程、先验证蒸馏与训练链路。',
    expectedEffect: '训练耗时较短，能较快得到可用结果，但长句、复杂语气和泛化稳定性通常弱于更大版本。',
    loadContent: async () => (await import('../../SampleText/5万字文本库.txt?raw')).default,
  },
  {
    key: '100k',
    title: '10 万字版本',
    fileName: '10万字文本库.txt',
    charCountLabel: '约 10 万字',
    description: '推荐默认选择，兼顾训练成本、稳定性和日常可用性。',
    expectedEffect: '通常能在训练时长和效果之间取得较平衡表现，日常对话、常见台词和中等长度句子更稳。',
    loadContent: async () => (await import('../../SampleText/10万字文本库.txt?raw')).default,
    recommended: true,
  },
  {
    key: '150k',
    title: '15 万字版本',
    fileName: '15万字文本库.txt',
    charCountLabel: '约 15 万字',
    description: '适合追求更高覆盖度的训练，建议在时间和算力更充足时使用。',
    expectedEffect: '对长句、内容覆盖和整体稳定性更有帮助，但蒸馏与训练耗时更长，对整体流程耗时要求最高。',
    loadContent: async () => (await import('../../SampleText/15万字文本库.txt?raw')).default,
  },
]

const MsIcon = ({
  name,
  size = 20,
  fill = 0,
  wght = 500,
  grad = 0,
  opsz = 24,
}: {
  name: string
  size?: number
  fill?: 0 | 1
  wght?: number
  grad?: number
  opsz?: number
}) => (
  <Box
    component="span"
    className="material-symbols-sharp"
    sx={{
      fontSize: size,
      lineHeight: 1,
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      userSelect: 'none',
      fontVariationSettings: `'FILL' ${fill}, 'wght' ${wght}, 'GRAD' ${grad}, 'opsz' ${opsz}`,
    }}
    aria-hidden
  >
    {name}
  </Box>
)

const NumberField = ({
  label,
  value,
  onChangeValue,
  size = 'small',
  inputProps,
  helperText,
  fullWidth,
}: {
  label: string
  value: number | string
  onChangeValue: (value: number) => void
  size?: 'small' | 'medium'
  inputProps?: { step?: number; min?: number; max?: number }
  helperText?: string
  fullWidth?: boolean
}) => {
  const step = Number(inputProps?.step ?? 1)
  const min = inputProps?.min
  const max = inputProps?.max
  const decimals = String(inputProps?.step ?? '').includes('.')
    ? String(inputProps?.step).split('.')[1]?.length ?? 0
    : 0
  const clamp = (next: number) => {
    let nextValue = Number.isFinite(next) ? next : 0
    if (typeof min === 'number') nextValue = Math.max(min, nextValue)
    if (typeof max === 'number') nextValue = Math.min(max, nextValue)
    return decimals > 0 ? Number(nextValue.toFixed(decimals)) : nextValue
  }
  const adjust = (delta: number) => {
    const current = Number(value)
    onChangeValue(clamp((Number.isFinite(current) ? current : 0) + delta))
  }

  return (
    <TextField
      label={label}
      type="number"
      value={value}
      onChange={(event) => onChangeValue(Number(event.target.value))}
      size={size}
      fullWidth={fullWidth}
      helperText={helperText}
      inputProps={inputProps}
      InputProps={{
        endAdornment: (
          <InputAdornment position="end" sx={{ ml: 0.25 }}>
            <Stack spacing={0} sx={{ mr: -0.75 }}>
              <IconButton
                size="small"
                tabIndex={-1}
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => adjust(step)}
                sx={{ width: 18, height: 14, p: 0, color: 'text.secondary', '&:hover': { color: 'primary.main' } }}
              >
                <MsIcon name="arrow_drop_up" size={18} fill={1} />
              </IconButton>
              <IconButton
                size="small"
                tabIndex={-1}
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => adjust(-step)}
                sx={{ width: 18, height: 14, p: 0, mt: -0.25, color: 'text.secondary', '&:hover': { color: 'primary.main' } }}
              >
                <MsIcon name="arrow_drop_down" size={18} fill={1} />
              </IconButton>
            </Stack>
          </InputAdornment>
        ),
      }}
      sx={{
        '& input[type=number]': { MozAppearance: 'textfield' },
        '& input[type=number]::-webkit-outer-spin-button, & input[type=number]::-webkit-inner-spin-button': {
          WebkitAppearance: 'none',
          margin: 0,
        },
      }}
    />
  )
}

const buildTheme = (mode: 'light' | 'dark') =>
  createTheme({
    palette: {
      mode,
      primary: {
        main: '#038387',
      },
      secondary: {
        main: '#038387',
      },
      background: {
        default: mode === 'dark' ? '#0f1112' : '#f4f7f6',
        paper: mode === 'dark' ? '#1a1d1e' : '#ffffff',
      },
    },
    shape: {
      borderRadius: 4,
    },
    typography: {
      fontFamily: '"Roboto","Noto Sans SC","Segoe UI",sans-serif',
      button: {
        textTransform: 'uppercase',
        fontWeight: 500,
      },
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          '::selection': {
            backgroundColor: '#038387',
            color: '#ffffff',
          },
          '::-moz-selection': {
            backgroundColor: '#038387',
            color: '#ffffff',
          },
          body: {
            scrollbarColor: `${mode === 'dark' ? '#3b4b4a' : '#b9c5c4'} transparent`,
            scrollbarWidth: 'thin',
            transition: 'background-color 220ms cubic-bezier(0.4, 0, 0.2, 1), color 220ms cubic-bezier(0.4, 0, 0.2, 1)',
          },
          'body, body *': {
            userSelect: 'none',
            WebkitUserSelect: 'none',
          },
          'input, textarea, [contenteditable="true"], .allow-text-select, .allow-text-select *': {
            userSelect: 'text',
            WebkitUserSelect: 'text',
          },
          'html, #root': {
            transition: 'background-color 220ms cubic-bezier(0.4, 0, 0.2, 1), color 220ms cubic-bezier(0.4, 0, 0.2, 1)',
          },
          '*, *::before, *::after': {
            transitionProperty: 'background-color, border-color, color, fill, stroke, box-shadow',
            transitionDuration: '220ms',
            transitionTimingFunction: 'cubic-bezier(0.4, 0, 0.2, 1)',
          },
          '@media (prefers-reduced-motion: reduce)': {
            '*, *::before, *::after': {
              transition: 'none !important',
            },
          },
          '*::-webkit-scrollbar': {
            width: 8,
            height: 8,
          },
          '*::-webkit-scrollbar-track': {
            background: 'transparent',
          },
          '*::-webkit-scrollbar-thumb': {
            backgroundColor: mode === 'dark' ? '#3b4b4a' : '#b9c5c4',
            borderRadius: 8,
          },
          '*::-webkit-scrollbar-thumb:hover': {
            backgroundColor: mode === 'dark' ? '#4c5f5e' : '#9fb1b0',
          },
        },
      },
      MuiPaper: {
        defaultProps: {
          elevation: 2,
        },
        styleOverrides: {
          root: {
            borderRadius: 4,
            transitionProperty: 'background-color, border-color, color, box-shadow',
            transitionDuration: '220ms',
            transitionTimingFunction: 'cubic-bezier(0.4, 0, 0.2, 1)',
          },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: 4,
          },
        },
      },
      MuiOutlinedInput: {
        styleOverrides: {
          root: {
            borderRadius: 4,
          },
        },
      },
    },
  })

type ToastState = {
  open: boolean
  message: string
  severity: 'success' | 'info' | 'warning' | 'error'
}

type ContextMenuMode = 'custom' | 'native'

type EditableTarget = HTMLInputElement | HTMLTextAreaElement | HTMLElement

type TextContextMenuState = {
  open: boolean
  x: number
  y: number
  target: EditableTarget | null
  selectionStart: number | null
  selectionEnd: number | null
}

type TextContextMenuCapabilities = {
  canSelectAll: boolean
  canCut: boolean
  canCopy: boolean
  canPaste: boolean
}

const getEditableTarget = (target: EventTarget | null): EditableTarget | null => {
  if (!(target instanceof HTMLElement)) return null
  const element = target.closest('input, textarea, [contenteditable="true"]')
  if (!element || !(element instanceof HTMLElement)) return null
  if (element instanceof HTMLInputElement) {
    const textTypes = new Set(['text', 'search', 'url', 'tel', 'password', 'email', 'number'])
    if (!textTypes.has((element.type || 'text').toLowerCase())) {
      return null
    }
  }
  return element
}

const getTextContextCapabilities = (target: EditableTarget | null): TextContextMenuCapabilities => {
  if (!target) {
    return { canSelectAll: false, canCut: false, canCopy: false, canPaste: false }
  }

  if (target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement) {
    const valueLength = target.value?.length ?? 0
    const selStart = target.selectionStart ?? 0
    const selEnd = target.selectionEnd ?? 0
    const hasSelection = selEnd > selStart
    const canWrite = !target.readOnly && !target.disabled
    return {
      canSelectAll: valueLength > 0,
      canCut: canWrite && hasSelection,
      canCopy: hasSelection,
      canPaste: canWrite,
    }
  }

  const selection = window.getSelection()?.toString() ?? ''
  const hasText = (target.textContent || '').trim().length > 0
  const editable = target.getAttribute('contenteditable') !== 'false'
  return {
    canSelectAll: hasText,
    canCut: editable && selection.length > 0,
    canCopy: selection.length > 0,
    canPaste: editable,
  }
}

const emptyProgress = (): ProgressMap => ({
  collect: 0,
  distill: 0,
  synth: 0,
  preprocess: 0,
  vad: 0,
  asr: 0,
  train: 0,
  export: 0,
})

const defaultDistillOptions = (): DistillOptions => ({
  gsv_root: '',
  version: '',
  speaker: '',
  prompt_lang: '',
  emotion: '',
  device: 'cuda',
  text_lang: '中文',
  text_split_method: '按标点符号切',
  speed_factor: 1,
  temperature: 1,
  batch_size: 1,
  seed: -1,
  top_k: 10,
  top_p: 1,
  batch_threshold: 0.75,
  split_bucket: true,
  fragment_interval: 0.3,
  parallel_infer: true,
  repetition_penalty: 1.35,
  sample_steps: 16,
  if_sr: false,
  text_sources: [],
})

const defaultVoxcpmOptions = (): VoxCpmDistillOptions => ({
  device: 'cuda',
  allow_cpu_fallback: true,
  voice_mode: 'description',
  voice_description: '',
  reference_audio: '',
  prompt_text: '',
  cfg_value: 2,
  inference_timesteps: 10,
  min_len: 2,
  max_len: 4096,
  normalize: false,
  denoise: false,
  retry_badcase: true,
  retry_badcase_max_times: 3,
  retry_badcase_ratio_threshold: 6,
  text_sources: [],
})

const defaultGsviAttributionFields = (): GsviAttributionFields => ({
  gsvAuthor: '',
  gsvTrainer: '',
  gsvTrainer2: '',
  gsviPacker: '',
})

const normalizeDistillSelection = (catalog: GsvModelCatalog | null, current: DistillOptions): DistillOptions => {
  if (!catalog) {
    return {
      ...current,
      version: '',
      speaker: '',
      prompt_lang: '',
      emotion: '',
    }
  }

  const versionNames = Object.keys(catalog.versions)
  const version = versionNames.includes(current.version) ? current.version : (versionNames[0] ?? '')
  const versionNode = version ? catalog.versions[version] : undefined
  const speakerNames = Object.keys(versionNode?.speakers ?? {})
  const speaker = speakerNames.includes(current.speaker) ? current.speaker : (speakerNames[0] ?? '')
  const speakerNode = speaker ? versionNode?.speakers[speaker] : undefined
  const langNames = Object.keys(speakerNode?.languages ?? {})
  const promptLang = langNames.includes(current.prompt_lang) ? current.prompt_lang : (langNames[0] ?? '')
  const emotions = promptLang ? speakerNode?.languages[promptLang]?.emotions ?? [] : []
  const emotion = emotions.some((item) => item.name === current.emotion) ? current.emotion : (emotions[0]?.name ?? '')

  return {
    ...current,
    version,
    speaker,
    prompt_lang: promptLang,
    emotion,
  }
}

const isProgressStage = (stage: string): stage is ProgressStage =>
  PROGRESS_STAGES.includes(stage as ProgressStage)

const getStageLabel = (stage: PipelineStage) => {
  if (stage === 'idle') return '待命'
  if (stage === 'runtime') return '运行时'
  if (stage === 'preview') return '试听'
  return STAGE_LABEL[stage]
}

const mergeDistillSources = (current: DistillTextSource[], incoming: DistillTextSource[]) => {
  const seen = new Set(current.map((item) => `${item.kind}:${item.path.toLowerCase()}`))
  const merged = [...current]
  for (const item of incoming) {
    const key = `${item.kind}:${item.path.toLowerCase()}`
    if (seen.has(key)) continue
    seen.add(key)
    merged.push(item)
  }
  return merged
}

const isDistillTextFile = (path: string) => /\.(txt|csv|jsonl)$/i.test(path)

const getDistillSourcePrimaryText = (item: DistillTextSource) => item.label || item.path

const getDistillSourceSecondaryText = (item: DistillTextSource) => {
  if (item.description) return item.description
  return item.kind === 'project_dir' ? '旧训练项目目录' : '文本语料文件'
}

const getCudaRuntimeChipColor = (status: PiperCudaRuntimeStatus | null): 'default' | 'success' | 'warning' | 'error' => {
  if (!status) return 'default'
  if (status.status === 'error') return 'error'
  if (!status.available) return 'warning'
  if (status.cuda_available === false) return 'warning'
  return 'success'
}

const getCudaRuntimeChipLabel = (status: PiperCudaRuntimeStatus | null) => {
  if (!status) return '未检测'
  if (status.status === 'error') return '运行时异常'
  if (!status.available) return '未安装'
  if (status.cuda_available === false) return '已安装 / CUDA 不可用'
  return '已就绪'
}

const getRuntimeChipColor = (
  status: PiperCudaRuntimeStatus | VoxCpmRuntimeStatus | null,
): 'default' | 'success' | 'warning' | 'error' => {
  if (!status) return 'default'
  if (status.status === 'error') return 'error'
  if (!status.available) return 'warning'
  if (status.cuda_available === false) return 'warning'
  return 'success'
}

const getRuntimeChipLabel = (status: PiperCudaRuntimeStatus | VoxCpmRuntimeStatus | null) => {
  if (!status) return '未检测'
  if (status.status === 'error') return '运行时异常'
  if (!status.available) return '未安装'
  if (status.cuda_available === false) return '已安装 / CUDA 不可用'
  return '已就绪'
}

const getVoxcpmModelChipColor = (status: VoxCpmModelStatus | null): 'default' | 'success' | 'warning' | 'error' => {
  if (!status) return 'default'
  if (!status.main_available) return 'warning'
  if (!status.denoiser_available) return 'warning'
  return 'success'
}

const getVoxcpmModelChipLabel = (status: VoxCpmModelStatus | null) => {
  if (!status) return '未检测'
  if (!status.main_available) return '未下载'
  if (!status.denoiser_available) return '主模型已就绪 / denoiser 未下载'
  return '已就绪'
}

type PathFieldProps = {
  label: string
  value: string
  onChange: (value: string) => void
  onPick: () => void
  onDropPath?: (value: string) => void
  onDropFiles?: (files: File[]) => Promise<string | null>
  helperText?: string
  placeholder?: string
}

type FileSystemEntryLike = {
  isFile: boolean
  isDirectory: boolean
  file?: (success: (file: File) => void, error?: () => void) => void
  createReader?: () => FileSystemDirectoryReaderLike
}

type FileSystemDirectoryReaderLike = {
  readEntries: (success: (entries: FileSystemEntryLike[]) => void) => void
}

type DataTransferItemWithEntry = DataTransferItem & {
  webkitGetAsEntry?: () => FileSystemEntryLike | null
}

const getDataTransfer = (event: ReactDragEvent | DragEvent): DataTransfer | null => {
  if ('dataTransfer' in event && event.dataTransfer) {
    return event.dataTransfer
  }
  if ('nativeEvent' in event) {
    return event.nativeEvent.dataTransfer
  }
  return null
}

const decodeFileUrl = (value: string) => {
  try {
    const cleaned = value.replace(/^file:\/*/i, '')
    return decodeURI(cleaned).replace(/\//g, '\\')
  } catch {
    return value
  }
}

const getNativeFilePath = (file: File | null | undefined) => {
  if (!file) return ''
  const maybePath = (file as File & { path?: string }).path
  if (typeof maybePath === 'string' && maybePath.trim()) {
    return maybePath
  }
  try {
    return window.fsBridge?.getPathForFile?.(file) || ''
  } catch {
    return ''
  }
}

const extractDroppedPaths = (event: ReactDragEvent | DragEvent) => {
  const dt = getDataTransfer(event)
  if (!dt) return []

  const fromFiles = Array.from(dt.files || [])
    .map((file) => getNativeFilePath(file))
    .filter(Boolean) as string[]
  if (fromFiles.length) return fromFiles

  const fromItems = Array.from(dt.items || [])
    .map((item) => {
      const entry = item as DataTransferItem
      if (entry.kind === 'file') {
        return entry.getAsFile()
      }
      return null
    })
    .filter(Boolean)
    .map((file) => getNativeFilePath(file as File))
    .filter(Boolean) as string[]
  if (fromItems.length) return fromItems

  const uri = dt.getData('text/uri-list')
  if (uri) {
    const lines = uri
      .split(/\r?\n/)
      .map((line: string) => line.trim())
      .filter((line: string) => line && !line.startsWith('#'))
    const decoded = lines.map((line: string) => (line.startsWith('file://') ? decodeFileUrl(line) : line))
    if (decoded.length) return decoded
  }

  const textPlain = dt.getData('text/plain') || ''
  if (textPlain) {
    const lines = textPlain
      .split(/\r?\n/)
      .map((line: string) => line.trim())
      .filter(Boolean)
    if (lines.length) {
      const normalized = lines.map((line: string) => (line.startsWith('file://') ? decodeFileUrl(line) : line))
      return normalized
    }
  }
  return []
}

const PathField = ({
  label,
  value,
  onChange,
  onPick,
  onDropPath,
  onDropFiles,
  helperText,
  placeholder,
}: PathFieldProps) => {
  const [dragging, setDragging] = useState(false)

  const handleDrop = async (event: ReactDragEvent<HTMLDivElement>) => {
    event.preventDefault()
    event.stopPropagation()
    setDragging(false)
    const paths = extractDroppedPaths(event)
    if (paths.length) {
      onDropPath?.(paths[0])
      return
    }
    if (onDropFiles) {
      const dt = getDataTransfer(event)
      const files = dt ? (Array.from(dt.files || []) as File[]) : []
      if (files.length) {
        const saved = await onDropFiles(files)
        if (saved) {
          onDropPath?.(saved)
        }
      }
    }
  }

  return (
    <Box
      onDragOver={(event) => {
        event.preventDefault()
        if (event.dataTransfer) {
          event.dataTransfer.dropEffect = 'copy'
        }
        setDragging(true)
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={handleDrop}
    >
      <TextField
        label={label}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        fullWidth
        size="small"
        helperText={helperText}
        placeholder={placeholder}
        InputProps={{
          endAdornment: (
            <InputAdornment position="end">
              <IconButton size="small" onClick={onPick}>
                <MsIcon name="folder_open" size={20} />
              </IconButton>
            </InputAdornment>
          ),
        }}
        sx={{
          '& .MuiOutlinedInput-root': {
            pr: 0.5,
          },
          ...(dragging && {
            '& .MuiOutlinedInput-notchedOutline': {
              borderColor: 'primary.main',
              borderWidth: 2,
            },
          }),
        }}
      />
    </Box>
  )
}

const createImage = (url: string) =>
  new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image()
    image.addEventListener('load', () => resolve(image))
    image.addEventListener('error', (err) => reject(err))
    image.setAttribute('crossOrigin', 'anonymous')
    image.src = url
  })

const getCroppedDataUrl = async (imageSrc: string, crop: Area, outputSize = 400): Promise<string> => {
  const image = await createImage(imageSrc)
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!ctx) return ''
  const size = Math.max(1, Math.floor(outputSize))
  canvas.width = size
  canvas.height = size
  ctx.drawImage(
    image,
    crop.x,
    crop.y,
    crop.width,
    crop.height,
    0,
    0,
    size,
    size,
  )
  return canvas.toDataURL('image/png')
}

const toFileUrl = (filePath: string) => {
  if (!filePath) return ''
  if (filePath.startsWith('file://')) return filePath
  const normalized = filePath.replace(/\\/g, '/')
  return `file:///${encodeURI(normalized)}`
}

const formatTime = (value: number) => {
  if (!Number.isFinite(value) || value < 0) return '00:00'
  const total = Math.floor(value)
  const min = Math.floor(total / 60)
  const sec = total % 60
  return `${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

const InlineAudioPlayer = ({
  src,
  audioPath,
  emptyText = '暂无试听音频',
}: {
  src: string
  audioPath?: string
  emptyText?: string
}) => {
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const [playing, setPlaying] = useState(false)
  const [duration, setDuration] = useState(0)
  const [currentTime, setCurrentTime] = useState(0)
  const hasAudio = Boolean(src)

  useEffect(() => {
    const audio = audioRef.current
    if (audio) {
      audio.pause()
      audio.currentTime = 0
      audio.load()
    }
    setPlaying(false)
    setDuration(0)
    setCurrentTime(0)
  }, [src])

  const togglePlay = () => {
    const audio = audioRef.current
    if (!audio || !hasAudio) return
    if (audio.paused) {
      void audio.play().catch(() => setPlaying(false))
    } else {
      audio.pause()
    }
  }

  const stopPlay = () => {
    const audio = audioRef.current
    if (!audio) return
    audio.pause()
    audio.currentTime = 0
    setCurrentTime(0)
    setPlaying(false)
  }

  const seekPlay = (_event: Event, value: number | number[]) => {
    const audio = audioRef.current
    if (!audio || !hasAudio) return
    const next = Array.isArray(value) ? value[0] : value
    audio.currentTime = next
    setCurrentTime(next)
  }

  return (
    <Stack spacing={1}>
      <Stack direction="row" spacing={1} alignItems="center">
        <Tooltip title={playing ? '暂停' : '播放'} arrow>
          <span>
            <IconButton color="primary" onClick={togglePlay} disabled={!hasAudio}>
              <MsIcon name={playing ? 'pause' : 'play_arrow'} size={20} fill={1} />
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title="停止" arrow>
          <span>
            <IconButton onClick={stopPlay} disabled={!hasAudio}>
              <MsIcon name="stop" size={20} fill={1} />
            </IconButton>
          </span>
        </Tooltip>
        <Box sx={{ flex: 1, px: 1 }}>
          <Slider
            size="small"
            min={0}
            max={duration > 0 ? duration : 1}
            step={0.01}
            value={hasAudio ? Math.min(currentTime, duration || 1) : 0}
            onChange={seekPlay}
            disabled={!hasAudio}
          />
        </Box>
        <Typography variant="caption" sx={{ minWidth: 96, textAlign: 'right', opacity: 0.75 }}>
          {formatTime(currentTime)} / {formatTime(duration)}
        </Typography>
      </Stack>
      <Typography variant="caption" sx={{ opacity: 0.75, wordBreak: 'break-all' }}>
        {audioPath || emptyText}
      </Typography>
      <Box
        component="audio"
        ref={audioRef}
        src={src}
        preload="metadata"
        onLoadedMetadata={(event: SyntheticEvent<HTMLAudioElement>) => {
          const audio = event.currentTarget
          setDuration(Number.isFinite(audio.duration) ? audio.duration : 0)
          setCurrentTime(audio.currentTime || 0)
        }}
        onDurationChange={(event: SyntheticEvent<HTMLAudioElement>) => {
          const audio = event.currentTarget
          setDuration(Number.isFinite(audio.duration) ? audio.duration : 0)
        }}
        onTimeUpdate={(event: SyntheticEvent<HTMLAudioElement>) => {
          setCurrentTime(event.currentTarget.currentTime || 0)
        }}
        onPlay={() => setPlaying(true)}
        onPause={() => setPlaying(false)}
        onEnded={(event: SyntheticEvent<HTMLAudioElement>) => {
          setPlaying(false)
          setCurrentTime(event.currentTarget.duration || 0)
        }}
        sx={{ display: 'none' }}
      />
    </Stack>
  )
}

const LegalDocumentDialog = ({
  open,
  title,
  content,
  onClose,
  monospace = false,
}: {
  open: boolean
  title: string
  content: string
  onClose: () => void
  monospace?: boolean
}) => (
  <Dialog
    open={open}
    onClose={(_event, reason) => {
      if (reason === 'backdropClick') return
      onClose()
    }}
    maxWidth="lg"
    fullWidth
  >
    <DialogTitle>{title}</DialogTitle>
    <DialogContent dividers className="allow-text-select">
      <Box
        sx={{
          fontSize: 13,
          lineHeight: 1.8,
          wordBreak: 'break-word',
          '& > *:first-of-type': { mt: 0 },
          '& > *:last-child': { mb: 0 },
          '& h1, & h2, & h3, & h4, & h5, & h6': {
            mt: 2.5,
            mb: 1,
            lineHeight: 1.45,
          },
          '& p': {
            my: 1,
          },
          '& ul, & ol': {
            my: 1,
            pl: 3,
          },
          '& li': {
            my: 0.5,
          },
          '& a': {
            color: 'primary.main',
          },
          '& blockquote': {
            m: 0,
            my: 1.5,
            px: 1.5,
            py: 1,
            borderLeft: '3px solid',
            borderColor: 'divider',
            bgcolor: 'action.hover',
          },
          '& pre': {
            my: 1.5,
            p: 1.5,
            overflowX: 'auto',
            borderRadius: 1,
            bgcolor: 'action.hover',
          },
          '& code': {
            fontSize: '0.92em',
            fontFamily: 'Menlo, Consolas, monospace',
          },
          ...(monospace
            ? {
                '& pre, & code': {
                  fontFamily: 'Menlo, Consolas, monospace',
                },
              }
            : {}),
        }}
      >
        <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
      </Box>
    </DialogContent>
    <DialogActions sx={{ px: 3, pb: 2 }}>
      <Button variant="contained" onClick={onClose}>
        关闭
      </Button>
    </DialogActions>
  </Dialog>
)

const safeSetSelectionRange = (target: HTMLInputElement | HTMLTextAreaElement, start: number, end: number) => {
  try {
    target.setSelectionRange(start, end)
  } catch {
    // Some input types (e.g. number) do not support setSelectionRange.
  }
}

const fileToDataUrl = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })

const readAllDirectoryEntries = (directory: FileSystemEntryLike): Promise<FileSystemEntryLike[]> =>
  new Promise((resolve) => {
    const reader = directory.createReader?.()
    const entries: FileSystemEntryLike[] = []
    if (!reader) {
      resolve(entries)
      return
    }
    const readBatch = () => {
      reader.readEntries((batch) => {
        if (!batch.length) {
          resolve(entries)
          return
        }
        entries.push(...batch)
        readBatch()
      })
    }
    readBatch()
  })

const getFilesFromEntries = async (entry: FileSystemEntryLike | null): Promise<File[]> => {
  if (!entry) return []
  if (entry.isFile) {
    return new Promise((resolve) => {
      entry.file?.((file: File) => resolve([file]), () => resolve([]))
    })
  }
  if (entry.isDirectory) {
    const entries = await readAllDirectoryEntries(entry)
    const results: File[] = []
    for (const child of entries) {
      const files = await getFilesFromEntries(child)
      results.push(...files)
    }
    return results
  }
  return []
}

const getFilesFromDataTransfer = async (event: ReactDragEvent | DragEvent): Promise<File[]> => {
  const dt = getDataTransfer(event)
  if (!dt) return []
  const items = Array.from(dt.items || [])
  const entryFiles: File[] = []
  for (const item of items) {
    const entry = (item as DataTransferItemWithEntry).webkitGetAsEntry?.() ?? null
    if (entry) {
      const files = await getFilesFromEntries(entry)
      entryFiles.push(...files)
    }
  }
  if (entryFiles.length) return entryFiles
  return Array.from(dt.files || []) as File[]
}

function App() {
  const requestId = useRef(1)
  const defaultsRequested = useRef(false)
  const previewAudioRef = useRef<HTMLAudioElement | null>(null)
  const abortingPipelineRef = useRef(false)
  const pingRequestIdRef = useRef<string | null>(null)
  const pendingRequestsRef = useRef<Map<string, PendingRequest>>(new Map())
  const pendingDistillStartRef = useRef<PendingDistillStart | null>(null)
  const runtimeInstallTailRef = useRef<Promise<void>>(Promise.resolve())
  const runtimeInstallQueueDepthRef = useRef(0)
  const cudaRuntimeRequestIdRef = useRef<string | null>(null)
  const voxcpmRuntimeRequestIdRef = useRef<string | null>(null)
  const voxcpmModelRequestIdRef = useRef<string | null>(null)
  const gsvDistillPreviewRequestIdRef = useRef<string | null>(null)
  const voxcpmDistillPreviewRequestIdRef = useRef<string | null>(null)
  const [connected, setConnected] = useState(false)
  const [status, setStatus] = useState('待命')
  const [logs, setLogs] = useState<string[]>([])

  const [outputDir, setOutputDir] = useState('')
  const [trainingMode, setTrainingMode] = useState<TrainingMode>(() => {
    try {
      const value = window.localStorage.getItem(DISTILL_MODE_STORAGE_KEY)
      if (value === 'gsv_distill' || value === 'voxcpm_distill' || value === 'resume_project') return value
      return 'piper'
    } catch {
      return 'piper'
    }
  })
  const [activeRunMode, setActiveRunMode] = useState<TrainingMode | null>(null)
  const [resumeProjectDir, setResumeProjectDir] = useState('')
  const [resumeProjectStatus, setResumeProjectStatus] = useState<TrainingProjectStatus | null>(null)
  const [resumeProjectBusy, setResumeProjectBusy] = useState(false)
  const [resumeRebuildConfirmOpen, setResumeRebuildConfirmOpen] = useState(false)
  const [pendingResumeProjectDir, setPendingResumeProjectDir] = useState('')
  const [pendingResumeProjectStatus, setPendingResumeProjectStatus] = useState<TrainingProjectStatus | null>(null)
  const [aboutDialog, setAboutDialog] = useState<AboutDialogKind>(null)
  const [audioFiles, setAudioFiles] = useState<string[]>([])
  const [quality, setQuality] = useState<'A' | 'B'>('A')
  const [denoise, setDenoise] = useState(false)
  const [sampleRate, setSampleRate] = useState('22050')
  const [trainBatchSize, setTrainBatchSize] = useState('24')
  const [asrModel, setAsrModel] = useState('')
  const [baseCkpt, setBaseCkpt] = useState('')
  const [useEspeak, setUseEspeak] = useState(false)
  const [piperConfig, setPiperConfig] = useState('')
  const [device, setDevice] = useState<'cpu' | 'cuda'>('cpu')
  const [cudaRuntimeStatus, setCudaRuntimeStatus] = useState<PiperCudaRuntimeStatus | null>(null)
  const [cudaRuntimeBusy, setCudaRuntimeBusy] = useState(false)
  const [cudaRuntimeProgressMessage, setCudaRuntimeProgressMessage] = useState('')
  const [cudaRuntimeProgressValue, setCudaRuntimeProgressValue] = useState(0)
  const [voxcpmRuntimeStatus, setVoxcpmRuntimeStatus] = useState<VoxCpmRuntimeStatus | null>(null)
  const [voxcpmRuntimeBusy, setVoxcpmRuntimeBusy] = useState(false)
  const [voxcpmRuntimeProgressMessage, setVoxcpmRuntimeProgressMessage] = useState('')
  const [voxcpmRuntimeProgressValue, setVoxcpmRuntimeProgressValue] = useState(0)
  const [voxcpmModelStatus, setVoxcpmModelStatus] = useState<VoxCpmModelStatus | null>(null)
  const [voxcpmModelBusy, setVoxcpmModelBusy] = useState(false)
  const [voxcpmModelProgressMessage, setVoxcpmModelProgressMessage] = useState('')
  const [voxcpmModelProgressValue, setVoxcpmModelProgressValue] = useState(0)
  const [distillOpts, setDistillOpts] = useState<DistillOptions>(() => {
    try {
      const raw = window.localStorage.getItem(DISTILL_SETTINGS_STORAGE_KEY)
      if (raw) {
        return { ...defaultDistillOptions(), ...JSON.parse(raw) } as DistillOptions
      }
    } catch {
      // ignore
    }
    return defaultDistillOptions()
  })
  const [voxcpmOpts, setVoxcpmOpts] = useState<VoxCpmDistillOptions>(() => {
    try {
      const raw = window.localStorage.getItem(VOXCPM_SETTINGS_STORAGE_KEY)
      if (raw) {
        return { ...defaultVoxcpmOptions(), ...JSON.parse(raw) } as VoxCpmDistillOptions
      }
    } catch {
      // ignore
    }
    return defaultVoxcpmOptions()
  })
  const [gsvCatalog, setGsvCatalog] = useState<GsvModelCatalog | null>(null)
  const [gsvRootStatus, setGsvRootStatus] = useState<{ ok: boolean; message: string } | null>(null)
  const [gsvRootBusy, setGsvRootBusy] = useState(false)
  const [distillAdvancedOpen, setDistillAdvancedOpen] = useState(false)
  const [voxcpmAdvancedOpen, setVoxcpmAdvancedOpen] = useState(false)
  const [voicepackName, setVoicepackName] = useState('未命名')
  const [voicepackRemark, setVoicepackRemark] = useState('')
  const [voicepackAvatar, setVoicepackAvatar] = useState('')
  const [voicepackAvatarPreview, setVoicepackAvatarPreview] = useState('')
  const [previewVoicepack, setPreviewVoicepack] = useState('')
  const [previewText, setPreviewText] = useState('你好，这是语音包试听。')
  const [previewAudioPath, setPreviewAudioPath] = useState('')
  const [previewAudioRev, setPreviewAudioRev] = useState(0)
  const [previewBusy, setPreviewBusy] = useState(false)
  const [gsvDistillPreviewText, setGsvDistillPreviewText] = useState('你好，这是 GPT-SoVITS 蒸馏试听。')
  const [gsvDistillPreviewAudioPath, setGsvDistillPreviewAudioPath] = useState('')
  const [gsvDistillPreviewAudioRev, setGsvDistillPreviewAudioRev] = useState(0)
  const [gsvDistillPreviewBusy, setGsvDistillPreviewBusy] = useState(false)
  const [voxcpmDistillPreviewText, setVoxcpmDistillPreviewText] = useState('你好，这是 VoxCPM2 蒸馏试听。')
  const [voxcpmDistillPreviewAudioPath, setVoxcpmDistillPreviewAudioPath] = useState('')
  const [voxcpmDistillPreviewAudioRev, setVoxcpmDistillPreviewAudioRev] = useState(0)
  const [voxcpmDistillPreviewBusy, setVoxcpmDistillPreviewBusy] = useState(false)
  const [previewPlaying, setPreviewPlaying] = useState(false)
  const [previewDuration, setPreviewDuration] = useState(0)
  const [previewCurrentTime, setPreviewCurrentTime] = useState(0)
  const [trainDonePromptOpen, setTrainDonePromptOpen] = useState(false)
  const [gsviModeIntroOpen, setGsviModeIntroOpen] = useState(false)
  const [gsviModeIntroSkipChecked, setGsviModeIntroSkipChecked] = useState(false)
  const [gsviDisclaimerOpen, setGsviDisclaimerOpen] = useState(false)
  const [gsviAttributionOpen, setGsviAttributionOpen] = useState(false)
  const [gsviAttribution, setGsviAttribution] = useState<GsviAttributionFields>(defaultGsviAttributionFields)
  const [gsviAttributionError, setGsviAttributionError] = useState('')
  const [gsviSkipPromptChecked, setGsviSkipPromptChecked] = useState(false)

  const [avatarDialogOpen, setAvatarDialogOpen] = useState(false)
  const [avatarSource, setAvatarSource] = useState('')
  const [avatarCrop, setAvatarCrop] = useState({ x: 0, y: 0 })
  const [avatarZoom, setAvatarZoom] = useState(1)
  const [avatarCropPixels, setAvatarCropPixels] = useState<Area | null>(null)

  const [toast, setToast] = useState<ToastState>({
    open: false,
    message: '',
    severity: 'info',
  })
  const [audioDragActive, setAudioDragActive] = useState(false)
  const [distillSourcesDragActive, setDistillSourcesDragActive] = useState(false)
  const [distillAddAnchorEl, setDistillAddAnchorEl] = useState<HTMLElement | null>(null)
  const [distillPresetDialogOpen, setDistillPresetDialogOpen] = useState(false)
  const [distillPresetBusy, setDistillPresetBusy] = useState(false)
  const [selectedDistillPresetKey, setSelectedDistillPresetKey] = useState<string>(
    DISTILL_TEXT_PRESET_OPTIONS.find((item) => item.recommended)?.key ?? DISTILL_TEXT_PRESET_OPTIONS[0]?.key ?? '',
  )
  const [avatarDragActive, setAvatarDragActive] = useState(false)

  const [progress, setProgress] = useState<ProgressMap>(emptyProgress)
  const [pipelineRunning, setPipelineRunning] = useState(false)
  const [pipelineCardCollapsed, setPipelineCardCollapsed] = useState(false)
  const [pipelineCardMinimized, setPipelineCardMinimized] = useState(false)
  const [currentStage, setCurrentStage] = useState<PipelineStage>('idle')
  const [isMaximized, setIsMaximized] = useState(false)
  const [drawerExpanded, setDrawerExpanded] = useState<boolean>(() => {
    try {
      const value = window.localStorage.getItem(DRAWER_EXPANDED_STORAGE_KEY)
      return value === 'true'
    } catch {
      // ignore
    }
    return false
  })
  const [page, setPage] = useState<AppPage>('prep')
  const [displayPage, setDisplayPage] = useState<AppPage>('prep')
  const [pageTransitionPhase, setPageTransitionPhase] = useState<'idle' | 'out' | 'in'>('idle')
  const [themeMode, setThemeMode] = useState<'system' | 'light' | 'dark'>(() => {
    try {
      const value = window.localStorage.getItem('kgtts_theme_mode')
      if (value === 'light' || value === 'dark' || value === 'system') {
        return value
      }
    } catch {
      // ignore
    }
    return 'system'
  })
  const [contextMenuMode, setContextMenuMode] = useState<ContextMenuMode>(() => {
    try {
      const value = window.localStorage.getItem('kgtts_text_context_menu_mode')
      if (value === 'native' || value === 'custom') {
        return value
      }
    } catch {
      // ignore
    }
    return 'custom'
  })
  const [textContextMenu, setTextContextMenu] = useState<TextContextMenuState>({
    open: false,
    x: 0,
    y: 0,
    target: null,
    selectionStart: null,
    selectionEnd: null,
  })
  const [systemDark, setSystemDark] = useState<boolean>(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return true
    return window.matchMedia('(prefers-color-scheme: dark)').matches
  })

  const resolvedThemeMode: 'light' | 'dark' = themeMode === 'system' ? (systemDark ? 'dark' : 'light') : themeMode
  const drawerWidth = drawerExpanded ? DRAWER_WIDTH : MINI_DRAWER_WIDTH
  const displayTrainingMode = activeRunMode ?? trainingMode
  const selectedDistillPreset = DISTILL_TEXT_PRESET_OPTIONS.find((item) => item.key === selectedDistillPresetKey) ?? DISTILL_TEXT_PRESET_OPTIONS[0]
  const activeProgressStages =
    displayTrainingMode === 'gsv_distill'
      ? DISTILL_PIPELINE_STAGES
      : displayTrainingMode === 'voxcpm_distill'
        ? VOXCPM_PIPELINE_STAGES
        : displayTrainingMode === 'resume_project'
          ? RESUME_PROJECT_PIPELINE_STAGES
          : PIPER_PIPELINE_STAGES
  const theme = useMemo(() => buildTheme(resolvedThemeMode), [resolvedThemeMode])
  const cardPaperSx = useMemo(
    () => ({
      p: 2,
      boxShadow: 2,
    }),
    [],
  )
  const titlebarButtonSx = {
    width: 32,
    height: 32,
    borderRadius: 6,
    color: resolvedThemeMode === 'dark' ? '#cfcfcf' : '#2f3f3f',
    '&:hover': {
      bgcolor: resolvedThemeMode === 'dark' ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.08)',
      borderRadius: '50%',
    },
  }
  const runtimeActionRowSx = {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 1,
    justifyContent: { xs: 'flex-start', sm: 'flex-end' },
    '& .MuiButton-root': {
      whiteSpace: 'nowrap',
      minWidth: 118,
    },
  }

  const overallProgress = useMemo(() => {
    const total = activeProgressStages.reduce((acc, stage) => acc + (progress[stage] ?? 0), 0)
    return activeProgressStages.length ? total / activeProgressStages.length : 0
  }, [activeProgressStages, progress])
  const previewAudioSrc = useMemo(() => {
    if (!previewAudioPath) return ''
    const base = toFileUrl(previewAudioPath)
    const sep = base.includes('?') ? '&' : '?'
    return `${base}${sep}v=${previewAudioRev}`
  }, [previewAudioPath, previewAudioRev])
  const activeDistillTextSources = trainingMode === 'voxcpm_distill' ? voxcpmOpts.text_sources : distillOpts.text_sources
  const gsvDistillPreviewAudioSrc = useMemo(() => {
    if (!gsvDistillPreviewAudioPath) return ''
    const base = toFileUrl(gsvDistillPreviewAudioPath)
    const sep = base.includes('?') ? '&' : '?'
    return `${base}${sep}v=${gsvDistillPreviewAudioRev}`
  }, [gsvDistillPreviewAudioPath, gsvDistillPreviewAudioRev])
  const voxcpmDistillPreviewAudioSrc = useMemo(() => {
    if (!voxcpmDistillPreviewAudioPath) return ''
    const base = toFileUrl(voxcpmDistillPreviewAudioPath)
    const sep = base.includes('?') ? '&' : '?'
    return `${base}${sep}v=${voxcpmDistillPreviewAudioRev}`
  }, [voxcpmDistillPreviewAudioPath, voxcpmDistillPreviewAudioRev])
  const hasPreviewAudio = Boolean(previewAudioSrc)
  const textContextCaps = useMemo(
    () => getTextContextCapabilities(textContextMenu.target),
    [textContextMenu.target],
  )
  const catalogVersions = Object.keys(gsvCatalog?.versions ?? {})
  const catalogSpeakers = Object.keys((distillOpts.version && gsvCatalog?.versions[distillOpts.version]?.speakers) ?? {})
  const catalogLanguages = Object.keys(
    (distillOpts.version && distillOpts.speaker && gsvCatalog?.versions[distillOpts.version]?.speakers[distillOpts.speaker]?.languages) ?? {},
  )
  const catalogEmotions =
    distillOpts.version && distillOpts.speaker && distillOpts.prompt_lang
      ? gsvCatalog?.versions[distillOpts.version]?.speakers[distillOpts.speaker]?.languages[distillOpts.prompt_lang]?.emotions ?? []
      : []
  const selectedEmotion = catalogEmotions.find((item) => item.name === distillOpts.emotion) ?? null
  const selectedVoxcpmVoiceMode = VOXCPM_VOICE_MODES.find((item) => item.value === voxcpmOpts.voice_mode) ?? VOXCPM_VOICE_MODES[0]
  const trainingFabBlockedByBackgroundTask = cudaRuntimeBusy || voxcpmRuntimeBusy || voxcpmModelBusy
  const gsviFieldMismatch = {
    gsvAuthor: Boolean(gsviAttributionError) && gsviAttribution.gsvAuthor.trim() !== GSVI_REQUIRED_NAMES.gsvAuthor,
    gsvTrainer: Boolean(gsviAttributionError) && gsviAttribution.gsvTrainer.trim() !== GSVI_REQUIRED_NAMES.gsvTrainer,
    gsvTrainer2: Boolean(gsviAttributionError) && gsviAttribution.gsvTrainer2.trim() !== GSVI_REQUIRED_NAMES.gsvTrainer2,
    gsviPacker: Boolean(gsviAttributionError) && gsviAttribution.gsviPacker.trim() !== GSVI_REQUIRED_NAMES.gsviPacker,
  }
  const shouldSkipGsviModeIntro = () => {
    try {
      return window.localStorage.getItem(GSVI_MODE_INTRO_SKIP_STORAGE_KEY) === 'true'
    } catch {
      return false
    }
  }
  const shouldSkipGsviPrompt = () => {
    try {
      return window.localStorage.getItem(GSVI_CONFIRM_SKIP_STORAGE_KEY) === 'true'
    } catch {
      return false
    }
  }

  const appendLog = (text: string) => {
    setLogs((prev) => [...prev, text].slice(-400))
  }

  const showToast = (message: string, severity: ToastState['severity'] = 'info') => {
    setToast({ open: true, message, severity })
  }

  const openExternalLink = async (targetUrl: string, label = '链接') => {
    if (!targetUrl) {
      showToast(`${label}为空`, 'warning')
      return
    }
    if (!window.paths?.openExternal) {
      showToast(`当前版本不支持打开${label}`, 'error')
      return
    }
    const result = await window.paths.openExternal(targetUrl)
    if (!result?.ok) {
      showToast(result?.message || `打开${label}失败`, 'error')
    }
  }

  const persistGsviModeIntroSkipPreference = (checked: boolean) => {
    try {
      if (checked) {
        window.localStorage.setItem(GSVI_MODE_INTRO_SKIP_STORAGE_KEY, 'true')
      } else {
        window.localStorage.removeItem(GSVI_MODE_INTRO_SKIP_STORAGE_KEY)
      }
    } catch {
      // ignore
    }
  }

  const persistGsviSkipPreference = (checked: boolean) => {
    try {
      if (checked) {
        window.localStorage.setItem(GSVI_CONFIRM_SKIP_STORAGE_KEY, 'true')
      } else {
        window.localStorage.removeItem(GSVI_CONFIRM_SKIP_STORAGE_KEY)
      }
    } catch {
      // ignore
    }
  }

  const resetGsviPromptState = () => {
    setGsviDisclaimerOpen(false)
    setGsviAttributionOpen(false)
    setGsviAttribution(defaultGsviAttributionFields())
    setGsviAttributionError('')
    setGsviSkipPromptChecked(false)
  }

  const cancelPendingDistillStart = (showFeedback = false) => {
    pendingDistillStartRef.current = null
    resetGsviPromptState()
    if (showFeedback) {
      setStatus('已取消 GPT-SoVITS 蒸馏启动')
      showToast('已取消 GPT-SoVITS 蒸馏启动', 'info')
    }
  }

  const openGsviGuide = async () => {
    try {
      const result = await window.paths?.openExternal?.(GSVI_GUIDE_URL)
      if (result?.ok) return
      window.open(GSVI_GUIDE_URL, '_blank', 'noopener,noreferrer')
    } catch {
      window.open(GSVI_GUIDE_URL, '_blank', 'noopener,noreferrer')
    }
  }

  const closeGsviModeIntro = () => {
    persistGsviModeIntroSkipPreference(gsviModeIntroSkipChecked)
    setGsviModeIntroSkipChecked(false)
    setGsviModeIntroOpen(false)
  }

  const handleTrainingModeChange = (nextMode: TrainingMode) => {
    setTrainingMode(nextMode)
    if (nextMode !== 'gsv_distill' || trainingMode === 'gsv_distill') {
      return
    }
    if (!shouldSkipGsviModeIntro()) {
      setGsviModeIntroSkipChecked(false)
      setGsviModeIntroOpen(true)
    }
  }

  const closeToast = () => {
    setToast((prev) => ({ ...prev, open: false }))
  }

  const closeTextContextMenu = () => {
    setTextContextMenu((prev) => ({
      ...prev,
      open: false,
      target: null,
      selectionStart: null,
      selectionEnd: null,
    }))
  }

  const insertTextAtSelection = (target: EditableTarget, text: string) => {
    if (target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement) {
      const current = target.value || ''
      const start = target.selectionStart ?? current.length
      const end = target.selectionEnd ?? start
      target.setRangeText(text, start, end, 'end')
      const caret = start + text.length
      safeSetSelectionRange(target, caret, caret)
      target.dispatchEvent(new Event('input', { bubbles: true }))
      target.dispatchEvent(new Event('change', { bubbles: true }))
      return
    }
    document.execCommand('insertText', false, text)
  }

  const runTextContextAction = async (action: 'selectAll' | 'cut' | 'copy' | 'paste') => {
    const target = textContextMenu.target
    if (!target) {
      closeTextContextMenu()
      return
    }
    if (target instanceof HTMLElement) {
      target.focus()
    }
    if (
      (target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement) &&
      textContextMenu.selectionStart !== null &&
      textContextMenu.selectionEnd !== null
    ) {
      safeSetSelectionRange(target, textContextMenu.selectionStart, textContextMenu.selectionEnd)
    }

    if (action === 'selectAll') {
      if (target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement) {
        target.select()
      } else {
        document.execCommand('selectAll')
      }
      closeTextContextMenu()
      return
    }

    if (action === 'cut') {
      document.execCommand('cut')
      closeTextContextMenu()
      return
    }

    if (action === 'copy') {
      document.execCommand('copy')
      closeTextContextMenu()
      return
    }

    let pasted = false
    const canWrite =
      !(target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement) ||
      (!target.readOnly && !target.disabled)
    if (canWrite) {
      let textFromClipboard: string | null = null
      try {
        if (window.clipboardBridge?.readText) {
          textFromClipboard = window.clipboardBridge.readText()
        }
      } catch {
        // ignore clipboard bridge errors
      }
      if (textFromClipboard === null) {
        try {
          textFromClipboard = await navigator.clipboard.readText()
        } catch {
          textFromClipboard = null
        }
      }
      if (typeof textFromClipboard === 'string') {
        insertTextAtSelection(target, textFromClipboard)
        pasted = true
      }
    }
    if (!pasted) {
      document.execCommand('paste')
    }
    closeTextContextMenu()
  }

  const togglePreviewPlay = async () => {
    const audio = previewAudioRef.current
    if (!audio || !hasPreviewAudio) return
    if (audio.paused) {
      try {
        await audio.play()
      } catch (err) {
        showToast(`播放失败: ${String(err)}`, 'error')
      }
      return
    }
    audio.pause()
  }

  const stopPreviewPlay = () => {
    const audio = previewAudioRef.current
    if (!audio) return
    audio.pause()
    audio.currentTime = 0
    setPreviewCurrentTime(0)
  }

  const seekPreviewPlay = (_event: unknown, value: number | number[]) => {
    const audio = previewAudioRef.current
    if (!audio || !hasPreviewAudio) return
    const target = Array.isArray(value) ? value[0] : value
    if (!Number.isFinite(target)) return
    audio.currentTime = Math.max(0, Number(target))
    setPreviewCurrentTime(audio.currentTime)
  }

  const saveDroppedFiles = async (files: File[]) => {
    if (!window.fsBridge?.saveDroppedFile) return []
    const saved: string[] = []
    for (const file of files) {
      if (!file || file.size <= 0) continue
      try {
        const data = await file.arrayBuffer()
        const outPath = await window.fsBridge.saveDroppedFile(file.name, data)
        if (outPath) {
          saved.push(outPath)
        }
      } catch {
        // ignore single file errors
      }
    }
    return saved
  }

  const saveDroppedFileSingle = async (files: File[]) => {
    const saved = await saveDroppedFiles(files)
    return saved[0] ?? null
  }

  const send = (type: string, payload?: Record<string, unknown>) => {
    const id = String(requestId.current++)
    window.backend?.send({ id, type, payload })
    return id
  }

  const rejectPendingRequests = (message: string) => {
    for (const [id, pending] of pendingRequestsRef.current.entries()) {
      window.clearTimeout(pending.timeout)
      pending.reject(new Error(message))
      pendingRequestsRef.current.delete(id)
    }
  }

  const requestBackend = <T = BackendResponsePayload>(
    type: string,
    payload?: Record<string, unknown>,
    timeoutMs = 20000,
    onId?: (id: string) => void,
  ) =>
    new Promise<T>((resolve, reject) => {
      const id = send(type, payload)
      onId?.(id)
      const timeout = window.setTimeout(() => {
        pendingRequestsRef.current.delete(id)
        reject(new Error(`${type} 请求超时`))
      }, timeoutMs)
      pendingRequestsRef.current.set(id, {
        resolve: (response) => resolve(response as T),
        reject,
        timeout,
      })
    })

  const enqueueRuntimeInstall = async <T,>(label: string, task: (queued: boolean) => Promise<T>) => {
    const queued = runtimeInstallQueueDepthRef.current > 0
    runtimeInstallQueueDepthRef.current += 1
    const previous = runtimeInstallTailRef.current.catch(() => undefined)
    const current = previous.then(async () => {
      if (queued) {
        appendLog(`[runtime] ${label} 排队完成，开始执行。`)
      }
      try {
        return await task(queued)
      } finally {
        runtimeInstallQueueDepthRef.current = Math.max(0, runtimeInstallQueueDepthRef.current - 1)
      }
    })
    runtimeInstallTailRef.current = current.then(() => undefined, () => undefined)
    return current
  }

  const refreshCudaRuntimeStatus = async (silent = false) => {
    try {
      if (!silent) {
        setCudaRuntimeBusy(true)
      }
      const status = await requestBackend<PiperCudaRuntimeStatus>('get_piper_cuda_runtime_status', {}, 30000)
      setCudaRuntimeStatus(status)
      if (!silent) {
        appendLog(`[runtime] ${status.message}`)
      }
      return status
    } catch (error) {
      const message = error instanceof Error ? error.message : '读取 Piper CUDA 运行时状态失败'
      if (!silent) {
        showToast(message, 'error')
      }
      throw error
    } finally {
      if (!silent) {
        setCudaRuntimeBusy(false)
        setCudaRuntimeProgressMessage('')
        setCudaRuntimeProgressValue(0)
      }
    }
  }

  const installCudaRuntime = async (force = false) => {
    const actionLabel = force ? '重建 Piper CUDA 运行时' : '安装 Piper CUDA 运行时'
    const queued = runtimeInstallQueueDepthRef.current > 0
    setCudaRuntimeBusy(true)
    setCudaRuntimeProgressValue(0)
    setCudaRuntimeProgressMessage(queued ? '已加入运行时安装队列，等待前一个 CUDA 运行时任务完成...' : `正在${actionLabel}...`)
    if (queued) {
      appendLog(`[runtime] ${actionLabel} 已加入队列。`)
    }
    try {
      return await enqueueRuntimeInstall<PiperCudaRuntimeStatus>(actionLabel, async () => {
        try {
          setCudaRuntimeProgressValue(0)
          setCudaRuntimeProgressMessage(`正在${actionLabel}...`)
          const status = await requestBackend<PiperCudaRuntimeStatus>(
            'install_piper_cuda_runtime',
            { force },
            60 * 60 * 1000,
            (id) => {
              cudaRuntimeRequestIdRef.current = id
            },
          )
          setCudaRuntimeStatus(status)
          setCudaRuntimeProgressValue(1)
          setCudaRuntimeProgressMessage(status.message)
          appendLog(`[runtime] ${status.message}`)
          showToast(status.message, status.cuda_available === false ? 'warning' : 'success')
          return status
        } catch (error) {
          const message = error instanceof Error ? error.message : '安装 Piper CUDA 运行时失败'
          setCudaRuntimeProgressMessage(message)
          appendLog(`[runtime] ${message}`)
          showToast(message, 'error')
          throw error
        } finally {
          cudaRuntimeRequestIdRef.current = null
        }
      })
    } finally {
      setCudaRuntimeBusy(false)
    }
  }

  const openCudaRuntimeDirectory = async () => {
    const target = cudaRuntimeStatus?.env_path || cudaRuntimeStatus?.runtime_root || ''
    if (!target || !window.paths?.openInExplorer) {
      showToast('当前没有可打开的运行时目录', 'warning')
      return
    }
    const result = await window.paths.openInExplorer(target)
    if (!result?.ok) {
      showToast(result?.message || '打开运行时目录失败', 'error')
    }
  }

  const refreshVoxcpmRuntimeStatus = async (silent = false) => {
    try {
      if (!silent) {
        setVoxcpmRuntimeBusy(true)
      }
      const status = await requestBackend<VoxCpmRuntimeStatus>('get_voxcpm_runtime_status', {}, 30000)
      setVoxcpmRuntimeStatus(status)
      if (!silent) {
        appendLog(`[runtime] ${status.message}`)
      }
      return status
    } catch (error) {
      const message = error instanceof Error ? error.message : '读取 VoxCPM2 运行时状态失败'
      if (!silent) {
        showToast(message, 'error')
      }
      throw error
    } finally {
      if (!silent) {
        setVoxcpmRuntimeBusy(false)
        setVoxcpmRuntimeProgressMessage('')
        setVoxcpmRuntimeProgressValue(0)
      }
    }
  }

  const installVoxcpmRuntime = async (force = false) => {
    const actionLabel = force ? '重建 VoxCPM2 运行时' : '安装 VoxCPM2 运行时'
    const queued = runtimeInstallQueueDepthRef.current > 0
    setVoxcpmRuntimeBusy(true)
    setVoxcpmRuntimeProgressValue(0)
    setVoxcpmRuntimeProgressMessage(queued ? '已加入运行时安装队列，等待前一个 CUDA 运行时任务完成...' : `正在${actionLabel}...`)
    if (queued) {
      appendLog(`[runtime] ${actionLabel} 已加入队列。`)
    }
    try {
      return await enqueueRuntimeInstall<VoxCpmRuntimeStatus>(actionLabel, async () => {
        try {
          setVoxcpmRuntimeProgressValue(0)
          setVoxcpmRuntimeProgressMessage(`正在${actionLabel}...`)
          const status = await requestBackend<VoxCpmRuntimeStatus>(
            'install_voxcpm_runtime',
            { force },
            60 * 60 * 1000,
            (id) => {
              voxcpmRuntimeRequestIdRef.current = id
            },
          )
          setVoxcpmRuntimeStatus(status)
          setVoxcpmRuntimeProgressValue(1)
          setVoxcpmRuntimeProgressMessage(status.message)
          appendLog(`[runtime] ${status.message}`)
          showToast(status.message, status.cuda_available === false ? 'warning' : 'success')
          return status
        } catch (error) {
          const message = error instanceof Error ? error.message : '安装 VoxCPM2 运行时失败'
          setVoxcpmRuntimeProgressMessage(message)
          appendLog(`[runtime] ${message}`)
          showToast(message, 'error')
          throw error
        } finally {
          voxcpmRuntimeRequestIdRef.current = null
        }
      })
    } finally {
      setVoxcpmRuntimeBusy(false)
    }
  }

  const refreshVoxcpmModelStatus = async (silent = false) => {
    try {
      if (!silent) {
        setVoxcpmModelBusy(true)
      }
      const status = await requestBackend<VoxCpmModelStatus>('get_voxcpm_model_status', {}, 30000)
      setVoxcpmModelStatus(status)
      if (!silent) {
        appendLog(`[runtime] ${status.message}`)
      }
      return status
    } catch (error) {
      const message = error instanceof Error ? error.message : '读取 VoxCPM2 模型状态失败'
      if (!silent) {
        showToast(message, 'error')
      }
      throw error
    } finally {
      if (!silent) {
        setVoxcpmModelBusy(false)
        setVoxcpmModelProgressMessage('')
        setVoxcpmModelProgressValue(0)
      }
    }
  }

  const downloadVoxcpmModels = async (force = false) => {
    try {
      setVoxcpmModelBusy(true)
      setVoxcpmModelProgressValue(0)
      setVoxcpmModelProgressMessage(force ? '正在重新下载 VoxCPM2 模型...' : '正在下载 VoxCPM2 模型...')
      const status = await requestBackend<VoxCpmModelStatus>(
        'download_voxcpm_models',
        { force, include_denoiser: true },
        24 * 60 * 60 * 1000,
        (id) => {
          voxcpmModelRequestIdRef.current = id
        },
      )
      setVoxcpmModelStatus(status)
      setVoxcpmModelProgressValue(1)
      setVoxcpmModelProgressMessage(status.message)
      appendLog(`[runtime] ${status.message}`)
      showToast(status.message, status.denoiser_available ? 'success' : 'warning')
      return status
    } catch (error) {
      const message = error instanceof Error ? error.message : '下载 VoxCPM2 模型失败'
      setVoxcpmModelProgressMessage(message)
      appendLog(`[runtime] ${message}`)
      showToast(message, 'error')
      throw error
    } finally {
      voxcpmModelRequestIdRef.current = null
      setVoxcpmModelBusy(false)
    }
  }

  const openVoxcpmRuntimeDirectory = async () => {
    const target = voxcpmRuntimeStatus?.env_path || voxcpmRuntimeStatus?.runtime_root || ''
    if (!target || !window.paths?.openInExplorer) {
      showToast('当前没有可打开的 VoxCPM2 运行时目录', 'warning')
      return
    }
    const result = await window.paths.openInExplorer(target)
    if (!result?.ok) {
      showToast(result?.message || '打开 VoxCPM2 运行时目录失败', 'error')
    }
  }

  const openVoxcpmModelDirectory = async () => {
    const target = voxcpmModelStatus?.model_root || ''
    if (!target || !window.paths?.openInExplorer) {
      showToast('当前没有可打开的 VoxCPM2 模型目录', 'warning')
      return
    }
    const result = await window.paths.openInExplorer(target)
    if (!result?.ok) {
      showToast(result?.message || '打开 VoxCPM2 模型目录失败', 'error')
    }
  }

  const requestBackendRestart = () => {
    rejectPendingRequests('后端已重启，请重试')
    window.backend?.restart?.()
    setConnected(false)
    appendLog('[SYS] 已请求后端重启')
    showToast('已请求后端重启', 'info')
    setTimeout(() => {
      pingRequestIdRef.current = send('ping')
    }, 1200)
  }

  const abortPipeline = () => {
    if (!pipelineRunning) {
      return
    }
    rejectPendingRequests('训练已中止')
    abortingPipelineRef.current = true
    setPipelineRunning(false)
    setPipelineCardMinimized(false)
    setActiveRunMode(null)
    setCurrentStage('idle')
    setStatus('中止训练中...')
    appendLog('[SYS] 已请求中止训练，正在重启后端...')
    showToast('已请求中止训练', 'warning')
    window.backend?.restart?.()
    setConnected(false)
    setTimeout(() => {
      pingRequestIdRef.current = send('ping')
    }, 1200)
  }

  useEffect(() => {
    if (!window.backend) {
      appendLog('未检测到后端桥接。')
      return
    }
    const offBackendEvent = window.backend.onEvent((evt) => {
      if (evt.type === 'response') {
        const pending = pendingRequestsRef.current.get(evt.id)
        if (pending) {
          window.clearTimeout(pending.timeout)
          pendingRequestsRef.current.delete(evt.id)
          pending.resolve(evt.payload)
        }
        if (evt.id === pingRequestIdRef.current && evt.payload?.ok) {
          pingRequestIdRef.current = null
          setConnected(true)
          if (abortingPipelineRef.current) {
            abortingPipelineRef.current = false
            setStatus('训练已中止')
          }
          showToast('后端已连接', 'success')
          if (!defaultsRequested.current) {
            defaultsRequested.current = true
            send('get_defaults')
          }
          void refreshCudaRuntimeStatus(true).catch(() => undefined)
          void refreshVoxcpmRuntimeStatus(true).catch(() => undefined)
          void refreshVoxcpmModelStatus(true).catch(() => undefined)
        }
        return
      }
      if (evt.type === 'defaults') {
        const defaults = (evt.payload || {}) as Record<string, string>
        const nextBase = baseCkpt || defaults.piper_base_checkpoint || ''
        const nextConfig = piperConfig || defaults.piper_config || ''
        if (!asrModel && defaults.asr_model_zip) {
          setAsrModel(defaults.asr_model_zip)
        }
        if (!baseCkpt && defaults.piper_base_checkpoint) {
          setBaseCkpt(defaults.piper_base_checkpoint)
        }
        if (!piperConfig && defaults.piper_config) {
          setPiperConfig(defaults.piper_config)
        }
        if (!useEspeak && nextBase && nextConfig) {
          setUseEspeak(true)
          appendLog('自动启用 espeak-ng（兼容基线）')
        }
        if (defaults.resources_root) {
          appendLog(`[INIT] resources: ${defaults.resources_root}`)
        }
      }
      if (evt.type === 'progress') {
        if (evt.stage === 'runtime') {
          const msg = String(evt.message ?? '')
          const value = typeof evt.value === 'number' ? evt.value : 0
          if (msg) {
            if (evt.id === cudaRuntimeRequestIdRef.current) {
              setCudaRuntimeProgressMessage(msg)
              setCudaRuntimeProgressValue(value)
            } else if (evt.id === voxcpmRuntimeRequestIdRef.current) {
              setVoxcpmRuntimeProgressMessage(msg)
              setVoxcpmRuntimeProgressValue(value)
            } else if (evt.id === voxcpmModelRequestIdRef.current) {
              setVoxcpmModelProgressMessage(msg)
              setVoxcpmModelProgressValue(value)
            }
            setStatus(msg)
          }
        }
        if (isProgressStage(evt.stage)) {
          setProgress((prev) => ({
            ...prev,
            [evt.stage]: evt.value ?? 0,
          }))
          const stage = evt.stage
          const msg = String(evt.message ?? '')
          setCurrentStage(stage)
          if (stage === 'asr') {
            if (msg.includes('转写完成')) {
              setStatus(msg)
            } else {
              setStatus('ASR 识别中...')
            }
          } else if (msg) {
            setStatus(msg)
          } else {
            setStatus(`${getStageLabel(stage)}中...`)
          }
        }
        if (evt.stage === 'preview' && evt.message) {
          setCurrentStage('preview')
          setStatus(evt.message)
        }
        if (evt.message) {
          appendLog(`[${evt.stage}] ${evt.message}`)
        }
        return
      }
      if (evt.type === 'error') {
        const errMsg = String(evt.message ?? '')
        const pending = pendingRequestsRef.current.get(evt.id)
        if (pending) {
          window.clearTimeout(pending.timeout)
          pendingRequestsRef.current.delete(evt.id)
          pending.reject(new Error(errMsg || '请求失败'))
          if (evt.traceback) {
            appendLog(evt.traceback)
          }
          return
        }
        if (evt.id === gsvDistillPreviewRequestIdRef.current) {
          gsvDistillPreviewRequestIdRef.current = null
          setGsvDistillPreviewBusy(false)
          setStatus('GPT-SoVITS 试听失败')
          appendLog(`[ERROR] ${errMsg}`)
          if (evt.traceback) {
            appendLog(evt.traceback)
          }
          showToast(errMsg || 'GPT-SoVITS 试听失败', 'error')
          return
        }
        if (evt.id === voxcpmDistillPreviewRequestIdRef.current) {
          voxcpmDistillPreviewRequestIdRef.current = null
          setVoxcpmDistillPreviewBusy(false)
          setStatus('VoxCPM2 试听失败')
          appendLog(`[ERROR] ${errMsg}`)
          if (evt.traceback) {
            appendLog(evt.traceback)
          }
          showToast(errMsg || 'VoxCPM2 试听失败', 'error')
          return
        }
        const backendFatal =
          errMsg.includes('Backend exited') ||
          errMsg.includes('Backend parse error') ||
          errMsg.includes('Python not found') ||
          errMsg.includes('JSON 解析失败')
        if (abortingPipelineRef.current && backendFatal) {
          setStatus('训练已中止')
          setPipelineRunning(false)
          setPipelineCardMinimized(false)
          setActiveRunMode(null)
          setCurrentStage('idle')
          appendLog('[SYS] 训练已中止')
          showToast('训练已中止', 'info')
          return
        }
        setStatus('任务出错')
        setPipelineRunning(false)
        setPipelineCardMinimized(false)
        setActiveRunMode(null)
        setCurrentStage('idle')
        setPreviewBusy(false)
        if (backendFatal) {
          setConnected(false)
        }
        appendLog(`[ERROR] ${errMsg}`)
        if (evt.traceback) {
          appendLog(evt.traceback)
        }
        showToast(errMsg || '任务出错', 'error')
        return
      }
      if (evt.type === 'preview_done') {
        const outPath = String(evt.payload?.audio_path ?? '')
        if (evt.id === gsvDistillPreviewRequestIdRef.current) {
          gsvDistillPreviewRequestIdRef.current = null
          setGsvDistillPreviewBusy(false)
          setStatus('GPT-SoVITS 试听完成')
          if (outPath) {
            setGsvDistillPreviewAudioPath(outPath)
            setGsvDistillPreviewAudioRev((prev) => prev + 1)
            appendLog(`[preview] GPT-SoVITS 试听生成: ${outPath}`)
            showToast('GPT-SoVITS 试听生成完成', 'success')
          } else {
            showToast('试听生成完成，但未返回音频路径', 'warning')
          }
          return
        }
        if (evt.id === voxcpmDistillPreviewRequestIdRef.current) {
          voxcpmDistillPreviewRequestIdRef.current = null
          setVoxcpmDistillPreviewBusy(false)
          setStatus('VoxCPM2 试听完成')
          if (outPath) {
            setVoxcpmDistillPreviewAudioPath(outPath)
            setVoxcpmDistillPreviewAudioRev((prev) => prev + 1)
            appendLog(`[preview] VoxCPM2 试听生成: ${outPath}`)
            showToast('VoxCPM2 试听生成完成', 'success')
          } else {
            showToast('试听生成完成，但未返回音频路径', 'warning')
          }
          return
        }
        setPreviewBusy(false)
        setStatus('试听完成')
        if (outPath) {
          setPreviewAudioPath(outPath)
          setPreviewAudioRev((prev) => prev + 1)
          appendLog(`[preview] 试听生成: ${outPath}`)
          showToast('试听生成完成', 'success')
        } else {
          showToast('试听生成完成，但未返回音频路径', 'warning')
        }
        return
      }
      if (evt.type === 'done') {
        setStatus('任务完成')
        setPipelineRunning(false)
        setPipelineCardMinimized(false)
        setActiveRunMode(null)
        setCurrentStage('idle')
        setTrainDonePromptOpen(true)
        const exportedVoicepack = String(evt.payload?.voicepack_path ?? '')
        appendLog(`导出完成: ${exportedVoicepack}`)
        if (exportedVoicepack) {
          setPreviewVoicepack(exportedVoicepack)
        }
        showToast('导出完成', 'success')
      }
      if (evt.type === 'log') {
        appendLog(evt.message ?? '')
      }
    })
    pingRequestIdRef.current = send('ping')
    return () => {
      rejectPendingRequests('界面已卸载')
      if (typeof offBackendEvent === 'function') {
        offBackendEvent()
      }
    }
  }, [])

  useEffect(() => {
    const audio = previewAudioRef.current
    if (!audio) return
    audio.pause()
    setPreviewPlaying(false)
    setPreviewCurrentTime(0)
    setPreviewDuration(0)
    if (previewAudioSrc) {
      audio.currentTime = 0
      audio.load()
    }
  }, [previewAudioSrc])

  useEffect(() => {
    const preventDefault = (event: Event) => {
      event.preventDefault()
    }
    window.addEventListener('dragover', preventDefault)
    window.addEventListener('drop', preventDefault)
    return () => {
      window.removeEventListener('dragover', preventDefault)
      window.removeEventListener('drop', preventDefault)
    }
  }, [])

  useEffect(() => {
    if (!window.windowControls) {
      return
    }
    window.windowControls.isMaximized().then((value) => setIsMaximized(value))
    window.windowControls.onState((state) => setIsMaximized(state.maximized))
  }, [])

  useEffect(() => {
    try {
      window.localStorage.setItem(DRAWER_EXPANDED_STORAGE_KEY, drawerExpanded ? 'true' : 'false')
    } catch {
      // ignore
    }
  }, [drawerExpanded])

  useEffect(() => {
    try {
      window.localStorage.setItem(DISTILL_MODE_STORAGE_KEY, trainingMode)
    } catch {
      // ignore
    }
  }, [trainingMode])

  useEffect(() => {
    try {
      window.localStorage.setItem(DISTILL_SETTINGS_STORAGE_KEY, JSON.stringify(distillOpts))
    } catch {
      // ignore
    }
  }, [distillOpts])

  useEffect(() => {
    try {
      window.localStorage.setItem(VOXCPM_SETTINGS_STORAGE_KEY, JSON.stringify(voxcpmOpts))
    } catch {
      // ignore
    }
  }, [voxcpmOpts])

  useEffect(() => {
    setDistillOpts((prev) => {
      const next = normalizeDistillSelection(gsvCatalog, prev)
      if (
        next.version === prev.version &&
        next.speaker === prev.speaker &&
        next.prompt_lang === prev.prompt_lang &&
        next.emotion === prev.emotion
      ) {
        return prev
      }
      return next
    })
  }, [gsvCatalog])

  useEffect(() => {
    const root = distillOpts.gsv_root.trim()
    if (!root) {
      setGsvRootBusy(false)
      setGsvRootStatus(null)
      setGsvCatalog(null)
      return
    }
    let cancelled = false
    const timer = window.setTimeout(async () => {
      setGsvRootBusy(true)
      try {
        const validation = await requestBackend<BackendResponsePayload>('validate_gsv_root', { gsv_root: root })
        if (cancelled) return
        const ok = Boolean(validation.ok)
        const normalizedRoot = String(validation.root || root)
        setGsvRootStatus({
          ok,
          message: String(validation.message || (ok ? '校验通过' : '校验失败')),
        })
        if (!ok) {
          setGsvCatalog(null)
          return
        }
        if (normalizedRoot !== distillOpts.gsv_root) {
          setDistillOpts((prev) => ({ ...prev, gsv_root: normalizedRoot }))
        }
        const catalog = await requestBackend<GsvModelCatalog>('scan_gsv_models', { gsv_root: normalizedRoot }, 30000)
        if (cancelled) return
        setGsvCatalog(catalog)
        const versionCount = Object.keys(catalog.versions || {}).length
        setGsvRootStatus({
          ok: true,
          message: versionCount > 0 ? `校验通过，已扫描 ${versionCount} 个版本` : '校验通过，但未发现可用说话人模型',
        })
      } catch (error) {
        if (cancelled) return
        setGsvCatalog(null)
        setGsvRootStatus({
          ok: false,
          message: error instanceof Error ? error.message : String(error),
        })
      } finally {
        if (!cancelled) {
          setGsvRootBusy(false)
        }
      }
    }, 360)
    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [distillOpts.gsv_root])

  useEffect(() => {
    if (page === displayPage) return
    setPageTransitionPhase('out')
    let inTimer: number | undefined
    const outTimer = window.setTimeout(() => {
      setDisplayPage(page)
      setPageTransitionPhase('in')
      inTimer = window.setTimeout(() => {
        setPageTransitionPhase('idle')
      }, PAGE_FADE_IN_MS)
    }, PAGE_FADE_OUT_MS)
    return () => {
      window.clearTimeout(outTimer)
      if (typeof inTimer === 'number') {
        window.clearTimeout(inTimer)
      }
    }
  }, [page, displayPage])

  useEffect(() => {
    if (!window.matchMedia) return
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    setSystemDark(media.matches)
    const onChange = (event: MediaQueryListEvent) => setSystemDark(event.matches)
    if (media.addEventListener) {
      media.addEventListener('change', onChange)
      return () => media.removeEventListener('change', onChange)
    }
    const legacyListener = (event: MediaQueryListEvent) => setSystemDark(event.matches)
    media.addListener(legacyListener)
    return () => media.removeListener(legacyListener)
  }, [])

  useEffect(() => {
    try {
      window.localStorage.setItem('kgtts_theme_mode', themeMode)
    } catch {
      // ignore
    }
  }, [themeMode])

  useEffect(() => {
    try {
      window.localStorage.setItem('kgtts_text_context_menu_mode', contextMenuMode)
    } catch {
      // ignore
    }
    window.windowControls?.setContextMenuMode?.(contextMenuMode)
    if (contextMenuMode !== 'custom') {
      setTextContextMenu((prev) => ({
        ...prev,
        open: false,
        target: null,
        selectionStart: null,
        selectionEnd: null,
      }))
    }
  }, [contextMenuMode])

  useEffect(() => {
    const onContextMenu = (event: MouseEvent) => {
      if (contextMenuMode !== 'custom') return
      const editableTarget = getEditableTarget(event.target)
      if (!editableTarget) return
      event.preventDefault()
      event.stopPropagation()
      editableTarget.focus()
      let selectionStart: number | null = null
      let selectionEnd: number | null = null
      if (editableTarget instanceof HTMLInputElement || editableTarget instanceof HTMLTextAreaElement) {
        selectionStart = editableTarget.selectionStart
        selectionEnd = editableTarget.selectionEnd
      }
      setTextContextMenu({
        open: true,
        x: event.clientX,
        y: event.clientY,
        target: editableTarget,
        selectionStart,
        selectionEnd,
      })
    }
    window.addEventListener('contextmenu', onContextMenu, true)
    return () => window.removeEventListener('contextmenu', onContextMenu, true)
  }, [contextMenuMode])

  const handleTitlebarThemeToggle = () => {
    setThemeMode(resolvedThemeMode === 'dark' ? 'light' : 'dark')
  }

  const pickOutputDir = async () => {
    const dir = await window.dialogs?.openDir()
    if (dir) {
      setOutputDir(dir)
    }
  }

  const handleOutputDrop = async (path: string) => {
    if (!path) return
    const looksLikeFile = /\.[^\\/]+$/.test(path)
    if (looksLikeFile && window.paths?.dirname) {
      const dir = await window.paths.dirname(path)
      setOutputDir(dir || path)
      return
    }
    setOutputDir(path)
  }

  const openOutputDirectory = async () => {
    if (!outputDir) {
      showToast('请先选择输出目录', 'warning')
      return
    }
    const result = await window.project?.openOutputDir(outputDir)
    if (result?.ok) {
      appendLog(`[SYS] 已打开目录: ${result.target ?? outputDir}`)
      showToast('已打开输出目录', 'success')
      return
    }
    showToast(result?.message || '打开输出目录失败', 'error')
  }

  const clearWorkCache = async () => {
    if (!outputDir) {
      showToast('请先选择输出目录', 'warning')
      return
    }
    const result = await window.project?.clearWorkCache(outputDir)
    if (!result?.ok) {
      showToast(result?.message || '清除工作缓存失败', 'error')
      return
    }
    setProgress(emptyProgress())
    setStatus('工作缓存已清理')
    appendLog(`[SYS] 已清理工作缓存: ${result.path ?? `${outputDir}\\work`}`)
    showToast('工作缓存已清理', 'success')
  }

  const pickGsvRoot = async () => {
    const dirs = await window.dialogs?.openFiles({
      title: '选择 GPT-SoVITS 根目录',
      properties: ['openDirectory'],
      filters: [{ name: 'All', extensions: ['*'] }],
    })
    if (dirs && dirs[0]) {
      setDistillOpts((prev) => ({ ...prev, gsv_root: dirs[0] }))
    }
  }

  const inspectResumeProject = async (projectDir = resumeProjectDir.trim()) => {
    if (!projectDir) {
      setResumeProjectStatus(null)
      return null
    }
    setResumeProjectBusy(true)
    try {
      const status = await requestBackend<TrainingProjectStatus>('inspect_training_project', { project_dir: projectDir }, 30000)
      setResumeProjectStatus(status)
      if (status.ok) {
        appendLog(`[project] ${status.message}`)
      }
      return status
    } catch (error) {
      const message = error instanceof Error ? error.message : '读取旧项目状态失败'
      setResumeProjectStatus({ ok: false, message, project_dir: projectDir })
      showToast(message, 'error')
      return null
    } finally {
      setResumeProjectBusy(false)
    }
  }

  const pickResumeProjectDir = async () => {
    const dirs = await window.dialogs?.openFiles({
      title: '选择旧训练项目目录',
      properties: ['openDirectory'],
      filters: [{ name: 'All', extensions: ['*'] }],
    })
    if (dirs && dirs[0]) {
      setResumeProjectDir(dirs[0])
      void inspectResumeProject(dirs[0])
    }
  }

  const handleGsvRootDrop = async (path: string) => {
    if (!path) return
    const looksLikeFile = /\.[^\\/]+$/.test(path)
    if (looksLikeFile && window.paths?.dirname) {
      const dir = await window.paths.dirname(path)
      setDistillOpts((prev) => ({ ...prev, gsv_root: dir || path }))
      return
    }
    setDistillOpts((prev) => ({ ...prev, gsv_root: path }))
  }

  const addDistillSources = (items: DistillTextSource[]) => {
    if (!items.length) return
    if (trainingMode === 'voxcpm_distill') {
      setVoxcpmOpts((prev) => ({
        ...prev,
        text_sources: mergeDistillSources(prev.text_sources, items),
      }))
      return
    }
    setDistillOpts((prev) => ({
      ...prev,
      text_sources: mergeDistillSources(prev.text_sources, items),
    }))
  }

  const mapDistillSourcePaths = (paths: string[]): DistillTextSource[] =>
    paths.reduce<DistillTextSource[]>((acc, path) => {
      if (isDistillTextFile(path)) {
        acc.push({ kind: 'text_file', path })
      }
      return acc
    }, [])

  const openDistillPresetDialog = () => {
    setDistillAddAnchorEl(null)
    setSelectedDistillPresetKey(DISTILL_TEXT_PRESET_OPTIONS.find((item) => item.recommended)?.key ?? DISTILL_TEXT_PRESET_OPTIONS[0]?.key ?? '')
    setDistillPresetDialogOpen(true)
  }

  const closeDistillPresetDialog = () => {
    if (distillPresetBusy) return
    setDistillPresetDialogOpen(false)
  }

  const addSelectedDistillPreset = async () => {
    if (!selectedDistillPreset) return
    setDistillPresetBusy(true)
    try {
      const presetContent = await selectedDistillPreset.loadContent()
      const presetPath =
        (await window.fsBridge?.ensureTextPresetFile?.(selectedDistillPreset.fileName, presetContent)) || ''
      if (!presetPath) {
        throw new Error('写入内置预设文本失败')
      }
      addDistillSources([
        {
          kind: 'text_file',
          path: presetPath,
          label: selectedDistillPreset.title,
          description: `${selectedDistillPreset.charCountLabel} · ${selectedDistillPreset.expectedEffect}`,
        },
      ])
      setDistillPresetDialogOpen(false)
      showToast(`已添加内置预设：${selectedDistillPreset.title}`, 'success')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '添加内置预设失败', 'error')
    } finally {
      setDistillPresetBusy(false)
    }
  }

  const pickDistillTextFiles = async () => {
    const files = await window.dialogs?.openFiles({
      title: '添加蒸馏文本来源',
      properties: ['openFile', 'multiSelections'],
      filters: [
        { name: 'Text Corpus', extensions: ['txt', 'csv', 'jsonl'] },
        { name: 'All', extensions: ['*'] },
      ],
    })
    if (files?.length) {
      addDistillSources(files.map((path) => ({ kind: 'text_file', path })))
    }
  }

  const pickVoxcpmReferenceAudio = async () => {
    const file = await window.dialogs?.openFile({
      filters: [
        { name: 'Audio', extensions: ['wav', 'mp3', 'm4a', 'flac', 'ogg'] },
        { name: 'All', extensions: ['*'] },
      ],
    })
    if (file) {
      setVoxcpmOpts((prev) => ({ ...prev, reference_audio: file }))
    }
  }

  const removeDistillSource = (target: DistillTextSource) => {
    if (trainingMode === 'voxcpm_distill') {
      setVoxcpmOpts((prev) => ({
        ...prev,
        text_sources: prev.text_sources.filter((item) => !(item.kind === target.kind && item.path === target.path)),
      }))
      return
    }
    setDistillOpts((prev) => ({
      ...prev,
      text_sources: prev.text_sources.filter((item) => !(item.kind === target.kind && item.path === target.path)),
    }))
  }

  const clearDistillSources = () => {
    if (trainingMode === 'voxcpm_distill') {
      setVoxcpmOpts((prev) => ({ ...prev, text_sources: [] }))
      showToast('已清空文本来源', 'info')
      return
    }
    setDistillOpts((prev) => ({ ...prev, text_sources: [] }))
    showToast('已清空文本来源', 'info')
  }

  const handleDistillSourcesDrop = (event: ReactDragEvent<HTMLDivElement>) => {
    event.preventDefault()
    event.stopPropagation()
    setDistillSourcesDragActive(false)
    const itemsFromPaths = mapDistillSourcePaths(extractDroppedPaths(event))
    if (itemsFromPaths.length) {
      addDistillSources(itemsFromPaths)
      return
    }

    getFilesFromDataTransfer(event).then((files) => {
      if (!files.length) {
        showToast('未检测到可用的文本文件', 'warning')
        return
      }
      const filePaths = files
        .map((file) => getNativeFilePath(file))
        .filter(Boolean) as string[]
      const resolvedFromPaths = mapDistillSourcePaths(filePaths)
      if (resolvedFromPaths.length) {
        addDistillSources(resolvedFromPaths)
        return
      }
      const dt = getDataTransfer(event)
      const types = dt ? Array.from(dt.types || []) : []
      const items = dt ? Array.from(dt.items || []) : []
      const fileSummary = files.slice(0, 3).map((file: File) => {
        const anyFile = file as File & { path?: string; webkitRelativePath?: string }
        return `${file.name}|path=${getNativeFilePath(file) || (anyFile.path ?? '')}|rel=${anyFile.webkitRelativePath ?? ''}`
      })
      const uri = dt ? dt.getData('text/uri-list') : ''
      const textPlain = dt ? dt.getData('text/plain') : ''
      appendLog(
        `[DROP] 文本来源拖入未提供原始路径 types=${types.join(',')} files=${files.length} items=${items.length} ` +
          `names=${fileSummary.join(';')} uri=${uri.slice(0, 120)} text=${textPlain.slice(0, 120)}`
      )
      showToast('当前拖入来源未提供原始文件路径，请从资源管理器直接拖入文本文件或使用添加按钮', 'warning')
    })
  }

  const pickAsrModel = async () => {
    const file = await window.dialogs?.openFile({
      filters: [{ name: 'ASR Zip', extensions: ['zip'] }],
    })
    if (file) {
      setAsrModel(file)
    }
  }

  const pickBaseCkpt = async () => {
    const file = await window.dialogs?.openFile({
      filters: [{ name: 'Checkpoint', extensions: ['ckpt'] }],
    })
    if (file) {
      setBaseCkpt(file)
    }
  }

  const pickPiperConfig = async () => {
    const file = await window.dialogs?.openFile({
      filters: [{ name: 'Piper Config', extensions: ['json'] }],
    })
    if (file) {
      setPiperConfig(file)
    }
  }

  const pickPreviewVoicepack = async () => {
    const file = await window.dialogs?.openFile({
      filters: [
        { name: 'KIGTTS Voicepack', extensions: ['kigvpk', 'zip'] },
        { name: 'All', extensions: ['*'] },
      ],
    })
    if (file) {
      setPreviewVoicepack(file)
    }
  }

  const pickAudioFiles = async () => {
    const files = await window.dialogs?.openFiles()
    if (files && files.length) {
      setAudioFiles((prev) => [...prev, ...files])
    }
  }

  const handleAudioDrop = (event: ReactDragEvent<HTMLDivElement>) => {
    event.preventDefault()
    event.stopPropagation()
    setAudioDragActive(false)
    const paths = extractDroppedPaths(event)
    if (paths.length) {
      setAudioFiles((prev) => [...prev, ...paths])
      return
    }
    getFilesFromDataTransfer(event).then((files) => {
      if (!files.length) {
        const dt = getDataTransfer(event)
        const types = dt ? Array.from(dt.types || []) : []
        const items = dt ? Array.from(dt.items || []) : []
        const fileSummary = files.slice(0, 3).map((file: File) => {
          const anyFile = file as File & { path?: string; webkitRelativePath?: string }
          return `${file.name}|path=${getNativeFilePath(file) || (anyFile.path ?? '')}|rel=${anyFile.webkitRelativePath ?? ''}`
        })
        const uri = dt ? dt.getData('text/uri-list') : ''
        const textPlain = dt ? dt.getData('text/plain') : ''
        appendLog(
          `[DROP] 未检测到可用路径 types=${types.join(',')} files=${files.length} items=${items.length} ` +
            `names=${fileSummary.join(';')} uri=${uri.slice(0, 120)} text=${textPlain.slice(0, 120)}`
        )
        showToast('未检测到可用的文件路径', 'warning')
        return
      }
      saveDroppedFiles(files).then((saved) => {
        if (saved.length) {
          setAudioFiles((prev) => [...prev, ...saved])
        } else {
          showToast('未检测到可用的文件路径', 'warning')
        }
      })
    })
  }

  const clearAudioFiles = () => {
    setAudioFiles([])
    showToast('已清空音频列表', 'info')
  }

  const clearAvatar = () => {
    setVoicepackAvatar('')
    setVoicepackAvatarPreview('')
  }

  const removeAudioFile = (index: number) => {
    setAudioFiles((prev) => prev.filter((_, idx) => idx !== index))
  }

  const openAvatarDialog = async () => {
    const file = await window.dialogs?.openFile({
      filters: [{ name: 'Images', extensions: ['png', 'jpg', 'jpeg', 'webp', 'bmp'] }],
    })
    if (!file) return
    const dataUrl = await window.fsBridge?.readImage(file)
    if (!dataUrl) {
      showToast('无法读取图像文件', 'error')
      return
    }
    setAvatarSource(dataUrl)
    setAvatarCrop({ x: 0, y: 0 })
    setAvatarZoom(1)
    setAvatarCropPixels(null)
    setAvatarDialogOpen(true)
  }

  const handleAvatarDrop = async (event: ReactDragEvent<HTMLElement>) => {
    event.preventDefault()
    event.stopPropagation()
    const paths = extractDroppedPaths(event)
    if (paths.length) {
      const dataUrl = await window.fsBridge?.readImage(paths[0])
      if (!dataUrl) {
        showToast('无法读取图像文件', 'error')
        return
      }
      setAvatarSource(dataUrl)
      setAvatarCrop({ x: 0, y: 0 })
      setAvatarZoom(1)
      setAvatarCropPixels(null)
      setAvatarDialogOpen(true)
      return
    }
    const files = await getFilesFromDataTransfer(event)
    if (!files.length) return
    try {
      const dataUrl = await fileToDataUrl(files[0])
      setAvatarSource(dataUrl)
      setAvatarCrop({ x: 0, y: 0 })
      setAvatarZoom(1)
      setAvatarCropPixels(null)
      setAvatarDialogOpen(true)
    } catch {
      showToast('无法读取图像文件', 'error')
    }
  }

  const applyAvatarCrop = async () => {
    if (!avatarSource || !avatarCropPixels) {
      showToast('请先裁剪头像', 'warning')
      return
    }
    const cropped = await getCroppedDataUrl(avatarSource, avatarCropPixels)
    if (!cropped) {
      showToast('头像裁剪失败', 'error')
      return
    }
    const savedPath = await window.fsBridge?.saveImage(cropped)
    if (!savedPath) {
      showToast('保存头像失败', 'error')
      return
    }
    setVoicepackAvatar(savedPath)
    setVoicepackAvatarPreview(cropped)
    setAvatarDialogOpen(false)
    showToast('头像已更新', 'success')
  }

  const startPreview = () => {
    if (!connected) {
      setStatus('后端未连接')
      showToast('后端未连接', 'error')
      return
    }
    if (pipelineRunning) {
      showToast('训练进行中，无法生成试听', 'warning')
      return
    }
    if (!previewVoicepack.trim()) {
      showToast('请先选择语音包', 'warning')
      return
    }
    if (!previewText.trim()) {
      showToast('请输入试听文本', 'warning')
      return
    }
    setPreviewBusy(true)
    setStatus('试听生成中...')
    send('preview_voicepack', {
      voicepack_path: previewVoicepack.trim(),
      text: previewText.trim(),
      output_dir: outputDir || null,
    })
  }

  const buildCommonOptsPayload = (): CommonPipelineOptions => {
    const parsedSampleRate = Number(sampleRate)
    const sampleRateValue =
      Number.isFinite(parsedSampleRate) && parsedSampleRate > 0 ? parsedSampleRate : 22050
    const parsedTrainBatchSize = Number(trainBatchSize)
    const trainBatchSizeValue =
      Number.isFinite(parsedTrainBatchSize) && parsedTrainBatchSize > 0 ? Math.max(1, Math.floor(parsedTrainBatchSize)) : 24
    return {
      quality,
      denoise,
      sample_rate: sampleRateValue,
      batch_size: trainBatchSizeValue,
      asr_model_zip: asrModel || null,
      piper_base_checkpoint: baseCkpt || null,
      use_espeak: useEspeak,
      piper_config: piperConfig || null,
      device,
      voicepack_name: voicepackName,
      voicepack_remark: voicepackRemark,
      voicepack_avatar: voicepackAvatar || null,
    }
  }

  const buildGsvPreviewPayload = (): DistillOptions => ({
    ...distillOpts,
    gsv_root: distillOpts.gsv_root.trim(),
    device: distillOpts.device === 'cpu' ? 'cpu' : 'cuda',
    batch_size: Math.max(1, Number(distillOpts.batch_size) || 1),
    seed: Number.isFinite(Number(distillOpts.seed)) ? Math.trunc(Number(distillOpts.seed)) : -1,
    top_k: Math.max(1, Number(distillOpts.top_k) || 10),
    top_p: Math.max(0, Number(distillOpts.top_p) || 1),
    batch_threshold: Math.max(0, Number(distillOpts.batch_threshold) || 0.75),
    fragment_interval: Math.max(0, Number(distillOpts.fragment_interval) || 0.3),
    repetition_penalty: Math.max(0, Number(distillOpts.repetition_penalty) || 1.35),
    sample_steps: Math.max(1, Number(distillOpts.sample_steps) || 16),
    speed_factor: Math.max(0.1, Number(distillOpts.speed_factor) || 1),
    temperature: Math.max(0, Number(distillOpts.temperature) || 1),
  })

  const buildVoxcpmPreviewPayload = (): VoxCpmDistillOptions => ({
    ...voxcpmOpts,
    device: voxcpmOpts.device === 'cpu' ? 'cpu' : 'cuda',
    voice_mode: voxcpmOpts.voice_mode,
    voice_description: voxcpmOpts.voice_description.trim(),
    reference_audio: voxcpmOpts.reference_audio.trim(),
    prompt_text: voxcpmOpts.prompt_text.trim(),
    cfg_value: Math.max(0.1, Number(voxcpmOpts.cfg_value) || 2),
    inference_timesteps: Math.max(1, Math.floor(Number(voxcpmOpts.inference_timesteps) || 10)),
    min_len: Math.max(1, Math.floor(Number(voxcpmOpts.min_len) || 2)),
    max_len: Math.max(1, Math.floor(Number(voxcpmOpts.max_len) || 4096)),
    retry_badcase_max_times: Math.max(0, Math.floor(Number(voxcpmOpts.retry_badcase_max_times) || 3)),
    retry_badcase_ratio_threshold: Math.max(0.1, Number(voxcpmOpts.retry_badcase_ratio_threshold) || 6),
  })

  const startGsvDistillPreview = () => {
    if (!connected) {
      showToast('后端未连接', 'error')
      return
    }
    if (pipelineRunning || previewBusy || gsvDistillPreviewBusy || voxcpmDistillPreviewBusy) {
      showToast('当前有任务在运行，请稍后再生成试听', 'warning')
      return
    }
    if (!distillOpts.gsv_root.trim() || !gsvRootStatus?.ok) {
      showToast('请先选择并校验 GPT-SoVITS 根目录', 'warning')
      return
    }
    if (!distillOpts.version || !distillOpts.speaker || !distillOpts.prompt_lang || !distillOpts.emotion) {
      showToast('请先完成 GPT-SoVITS 模型选择', 'warning')
      return
    }
    if (!gsvDistillPreviewText.trim()) {
      showToast('请输入试听文本', 'warning')
      return
    }
    setGsvDistillPreviewBusy(true)
    setStatus('GPT-SoVITS 试听生成中...')
    const id = send('preview_gsv_distill', {
      output_dir: outputDir || null,
      text: gsvDistillPreviewText.trim(),
      distill: buildGsvPreviewPayload(),
    })
    gsvDistillPreviewRequestIdRef.current = id
  }

  const startVoxcpmDistillPreview = () => {
    if (!connected) {
      showToast('后端未连接', 'error')
      return
    }
    if (pipelineRunning || previewBusy || gsvDistillPreviewBusy || voxcpmDistillPreviewBusy) {
      showToast('当前有任务在运行，请稍后再生成试听', 'warning')
      return
    }
    if (!voxcpmRuntimeStatus?.available || !voxcpmModelStatus?.main_available) {
      showToast('请先准备 VoxCPM2 运行时和主模型', 'warning')
      return
    }
    if (voxcpmOpts.denoise && !voxcpmModelStatus?.denoiser_available) {
      showToast('当前启用了 denoiser，请先下载 denoiser 模型', 'warning')
      return
    }
    if (!voxcpmDistillPreviewText.trim()) {
      showToast('请输入试听文本', 'warning')
      return
    }
    if (voxcpmOpts.voice_mode === 'description' && !voxcpmOpts.voice_description.trim()) {
      showToast('声音设定模式需要填写音色描述', 'warning')
      return
    }
    if (voxcpmOpts.voice_mode !== 'description' && !voxcpmOpts.reference_audio.trim()) {
      showToast('当前声音生成模式需要参考音频', 'warning')
      return
    }
    if (voxcpmOpts.voice_mode === 'high_fidelity' && !voxcpmOpts.prompt_text.trim() && !asrModel) {
      showToast('高保真克隆需要参考文本；如留空，请先配置 ASR 模型用于自动转写', 'warning')
      return
    }
    setVoxcpmDistillPreviewBusy(true)
    setStatus('VoxCPM2 试听生成中...')
    const id = send('preview_voxcpm_distill', {
      output_dir: outputDir || null,
      text: voxcpmDistillPreviewText.trim(),
      opts: buildCommonOptsPayload(),
      voxcpm: buildVoxcpmPreviewPayload(),
    })
    voxcpmDistillPreviewRequestIdRef.current = id
  }

  const exportDistillPreviewAudio = async (audioPath: string, defaultName: string) => {
    if (!audioPath) {
      showToast('暂无可导出的试听音频', 'warning')
      return
    }
    if (!window.dialogs?.saveFile || !window.fsBridge?.copyFile) {
      showToast('当前版本不支持导出音频', 'error')
      return
    }
    const defaultPath = outputDir ? `${outputDir}\\${defaultName}` : defaultName
    const target = await window.dialogs.saveFile({
      title: '导出试听音频',
      defaultPath,
      filters: [{ name: 'WAV Audio', extensions: ['wav'] }],
    })
    if (!target) return
    const ok = await window.fsBridge.copyFile(audioPath, target)
    if (!ok) {
      showToast('导出失败', 'error')
      return
    }
    showToast('导出成功', 'success')
  }

  const exportPreviewAudio = async () => {
    if (!previewAudioPath) {
      showToast('暂无可导出的试听音频', 'warning')
      return
    }
    if (!window.dialogs?.saveFile || !window.fsBridge?.copyFile) {
      showToast('当前版本不支持导出音频', 'error')
      return
    }
    const defaultPath = outputDir
      ? `${outputDir}\\voicepack_preview.wav`
      : 'voicepack_preview.wav'
    const target = await window.dialogs.saveFile({
      title: '导出试听音频',
      defaultPath,
      filters: [{ name: 'WAV Audio', extensions: ['wav'] }],
    })
    if (!target) {
      return
    }
    const ok = await window.fsBridge.copyFile(previewAudioPath, target)
    if (!ok) {
      showToast('导出失败', 'error')
      return
    }
    showToast('导出成功', 'success')
  }

  const exportLogs = async () => {
    if (!window.dialogs?.saveFile || !window.fsBridge?.writeTextFile) {
      showToast('当前版本不支持导出日志', 'error')
      return
    }
    const now = new Date()
    const pad2 = (value: number) => String(value).padStart(2, '0')
    const defaultName =
      `kigtts-trainer-log-${now.getFullYear()}${pad2(now.getMonth() + 1)}${pad2(now.getDate())}-` +
      `${pad2(now.getHours())}${pad2(now.getMinutes())}${pad2(now.getSeconds())}.txt`
    const defaultPath = outputDir ? `${outputDir}\\${defaultName}` : defaultName
    const target = await window.dialogs.saveFile({
      title: '导出日志',
      defaultPath,
      filters: [{ name: 'Text', extensions: ['txt', 'log'] }],
    })
    if (!target) {
      return
    }
    const logText = logs.join('\n')
    const ok = await window.fsBridge.writeTextFile(target, logText)
    if (!ok) {
      showToast('导出日志失败', 'error')
      return
    }
    showToast('日志已导出', 'success')
  }

  const openCurrentVoicepackDirectory = async () => {
    const target = previewVoicepack.trim()
    if (!target) {
      showToast('请先选择语音包', 'warning')
      return
    }
    if (!window.paths?.openInExplorer) {
      showToast('当前版本不支持打开目录', 'error')
      return
    }
    const result = await window.paths.openInExplorer(target)
    if (result?.ok) {
      showToast('已打开语音包目录', 'success')
      return
    }
    showToast(result?.message || '打开语音包目录失败', 'error')
  }

  const beginPipelineRun = (mode: TrainingMode, stage: PipelineStage, nextStatus: string, toastMessage: string) => {
    abortingPipelineRef.current = false
    setProgress(emptyProgress())
    setPipelineRunning(true)
    setPipelineCardCollapsed(false)
    setPipelineCardMinimized(false)
    setTrainDonePromptOpen(false)
    setActiveRunMode(mode)
    setCurrentStage(stage)
    setStatus(nextStatus)
    showToast(toastMessage, 'info')
  }

  const launchDistillPipeline = (outputDir: string, commonOpts: CommonPipelineOptions, distillPayload: DistillOptions) => {
    beginPipelineRun('gsv_distill', 'collect', '蒸馏任务启动中...', 'GPT-SoVITS 蒸馏任务已启动')
    send('start_distill_pipeline', {
      output_dir: outputDir,
      opts: commonOpts,
      distill: distillPayload,
    })
  }

  const launchVoxcpmPipeline = (outputDir: string, commonOpts: CommonPipelineOptions, voxcpmPayload: VoxCpmDistillOptions) => {
    beginPipelineRun('voxcpm_distill', 'collect', 'VoxCPM2 蒸馏任务启动中...', 'VoxCPM2 蒸馏任务已启动')
    send('start_voxcpm_distill_pipeline', {
      output_dir: outputDir,
      opts: commonOpts,
      voxcpm: voxcpmPayload,
    })
  }

  const launchResumeProjectPipeline = (projectDir: string) => {
    beginPipelineRun('resume_project', 'collect', '旧项目继续训练任务启动中...', '旧项目继续训练任务已启动')
    send('start_resume_project_pipeline', {
      project_dir: projectDir,
    })
  }

  const openResumeRebuildConfirm = (projectDir: string, projectStatus: TrainingProjectStatus) => {
    setPendingResumeProjectDir(projectDir)
    setPendingResumeProjectStatus(projectStatus)
    setResumeRebuildConfirmOpen(true)
  }

  const cancelResumeRebuildConfirm = () => {
    setResumeRebuildConfirmOpen(false)
    setPendingResumeProjectDir('')
    setPendingResumeProjectStatus(null)
  }

  const confirmResumeRebuild = () => {
    const projectDir = pendingResumeProjectDir
    if (!projectDir) {
      cancelResumeRebuildConfirm()
      showToast('旧项目启动上下文已失效，请重新点击开始训练。', 'warning')
      return
    }
    cancelResumeRebuildConfirm()
    launchResumeProjectPipeline(projectDir)
  }

  const openGsviConfirmationFlow = (pending: PendingDistillStart) => {
    pendingDistillStartRef.current = pending
    setGsviAttribution(defaultGsviAttributionFields())
    setGsviAttributionError('')
    setGsviSkipPromptChecked(false)
    setGsviAttributionOpen(false)
    setGsviDisclaimerOpen(true)
  }

  const confirmGsviDisclaimer = () => {
    setGsviDisclaimerOpen(false)
    setGsviAttributionOpen(true)
    setGsviAttributionError('')
  }

  const submitGsviAttribution = () => {
    const trimmed = {
      gsvAuthor: gsviAttribution.gsvAuthor.trim(),
      gsvTrainer: gsviAttribution.gsvTrainer.trim(),
      gsvTrainer2: gsviAttribution.gsvTrainer2.trim(),
      gsviPacker: gsviAttribution.gsviPacker.trim(),
    }
    const mismatch = Object.entries(GSVI_REQUIRED_NAMES).find(([key, expected]) => trimmed[key as keyof GsviAttributionFields] !== expected)
    if (mismatch) {
      setGsviAttributionError('请输入并完整确认所有署名字段后再继续。')
      return
    }
    const pending = pendingDistillStartRef.current
    if (!pending) {
      cancelPendingDistillStart()
      showToast('蒸馏启动上下文已失效，请重新点击开始训练。', 'warning')
      return
    }
    persistGsviSkipPreference(gsviSkipPromptChecked)
    resetGsviPromptState()
    pendingDistillStartRef.current = null
    launchDistillPipeline(pending.outputDir, pending.commonOpts, pending.distillPayload)
  }

  const startPipeline = async () => {
    if (!connected) {
      setStatus('后端未连接')
      showToast('后端未连接', 'error')
      return
    }
    if (trainingFabBlockedByBackgroundTask) {
      setStatus('环境安装或模型下载中')
      showToast('当前正在安装运行时或下载模型，请等待后台任务完成后再开始训练', 'warning')
      return
    }
    if (previewBusy) {
      showToast('正在生成试听，请稍后再开始训练', 'warning')
      return
    }
    if (trainingMode === 'resume_project') {
      const projectDir = resumeProjectDir.trim()
      if (!projectDir) {
        setStatus('请先选择旧训练项目目录')
        showToast('请先选择旧训练项目目录', 'warning')
        return
      }
      const status = resumeProjectStatus?.project_dir === projectDir ? resumeProjectStatus : await inspectResumeProject(projectDir)
      if (!status?.ok) {
        setStatus('旧项目不可用')
        showToast(status?.message || '旧项目不可用', 'error')
        return
      }
      if (status.needs_material_rebuild) {
        openResumeRebuildConfirm(projectDir, status)
        return
      }
      launchResumeProjectPipeline(projectDir)
      return
    }
    let effectiveOutputDir = outputDir.trim()
    if (!effectiveOutputDir) {
      const created = await window.project?.ensureDefaultOutputDir?.()
      if (!created?.ok || !created.path) {
        setStatus('请先选择输出目录')
        showToast(created?.message || '输出目录为空，且默认目录创建失败', 'error')
        return
      }
      effectiveOutputDir = created.path
      setOutputDir(created.path)
      appendLog(`[SYS] 输出目录为空，已自动创建: ${created.path}`)
      showToast('已自动创建默认输出目录', 'info')
    }
    if (device === 'cuda') {
      if (!cudaRuntimeStatus?.available) {
        setStatus('请先安装 Piper CUDA 运行时')
        showToast('当前未安装 Piper CUDA 运行时，请先在训练设置里完成配置', 'warning')
        return
      }
      if (cudaRuntimeStatus.cuda_available === false) {
        setStatus('当前机器未检测到可用 CUDA')
        showToast('Piper CUDA 运行时已安装，但当前机器未检测到可用 CUDA', 'warning')
        return
      }
    }

    const commonOpts = buildCommonOptsPayload()

    if (trainingMode === 'gsv_distill') {
      if (!distillOpts.gsv_root.trim()) {
        setStatus('请先选择 GPT-SoVITS 根目录')
        showToast('请先选择 GPT-SoVITS 根目录', 'warning')
        return
      }
      if (!gsvRootStatus?.ok) {
        setStatus('GPT-SoVITS 根目录校验失败')
        showToast(gsvRootStatus?.message || 'GPT-SoVITS 根目录校验失败', 'error')
        return
      }
      if (!distillOpts.version || !distillOpts.speaker || !distillOpts.prompt_lang || !distillOpts.emotion) {
        setStatus('请先完成 GPT-SoVITS 模型选择')
        showToast('请先完成 GPT-SoVITS 模型选择', 'warning')
        return
      }
      if (!distillOpts.text_sources.length) {
        setStatus('请先添加蒸馏文本来源')
        showToast('请先添加蒸馏文本来源', 'warning')
        return
      }
      const distillPayload: DistillOptions = {
        ...distillOpts,
        gsv_root: distillOpts.gsv_root.trim(),
        device: distillOpts.device === 'cpu' ? 'cpu' : 'cuda',
        batch_size: Math.max(1, Number(distillOpts.batch_size) || 1),
        seed: Number.isFinite(Number(distillOpts.seed)) ? Math.trunc(Number(distillOpts.seed)) : -1,
        top_k: Math.max(1, Number(distillOpts.top_k) || 10),
        top_p: Math.max(0, Number(distillOpts.top_p) || 1),
        batch_threshold: Math.max(0, Number(distillOpts.batch_threshold) || 0.75),
        fragment_interval: Math.max(0, Number(distillOpts.fragment_interval) || 0.3),
        repetition_penalty: Math.max(0, Number(distillOpts.repetition_penalty) || 1.35),
        sample_steps: Math.max(1, Number(distillOpts.sample_steps) || 16),
        speed_factor: Math.max(0.1, Number(distillOpts.speed_factor) || 1),
        temperature: Math.max(0, Number(distillOpts.temperature) || 1),
      }
      const pendingStart = {
        outputDir: effectiveOutputDir,
        commonOpts,
        distillPayload,
      }
      if (shouldSkipGsviPrompt()) {
        launchDistillPipeline(pendingStart.outputDir, pendingStart.commonOpts, pendingStart.distillPayload)
        return
      }
      openGsviConfirmationFlow(pendingStart)
      return
    }

    if (trainingMode === 'voxcpm_distill') {
      if (!voxcpmRuntimeStatus?.available) {
        setStatus('请先安装 VoxCPM2 运行时')
        showToast('请先安装 VoxCPM2 运行时', 'warning')
        return
      }
      if (!voxcpmModelStatus?.main_available) {
        setStatus('请先下载 VoxCPM2 主模型')
        showToast('请先下载 VoxCPM2 主模型', 'warning')
        return
      }
      if (voxcpmOpts.denoise && !voxcpmModelStatus?.denoiser_available) {
        setStatus('请先下载 VoxCPM2 denoiser 模型')
        showToast('当前启用了 denoiser，请先下载 denoiser 模型', 'warning')
        return
      }
      if (voxcpmOpts.voice_mode === 'description' && !voxcpmOpts.voice_description.trim()) {
        setStatus('请先填写音色描述')
        showToast('声音设定模式需要填写音色描述', 'warning')
        return
      }
      if (voxcpmOpts.voice_mode !== 'description' && !voxcpmOpts.reference_audio.trim()) {
        setStatus('请先选择参考音频')
        showToast('当前声音生成模式需要参考音频', 'warning')
        return
      }
      if (voxcpmOpts.voice_mode === 'high_fidelity' && !voxcpmOpts.prompt_text.trim() && !asrModel) {
        setStatus('请先配置 ASR 模型或填写参考文本')
        showToast('高保真克隆需要参考文本；如留空，请先配置 ASR 模型用于自动转写', 'warning')
        return
      }
      if (!voxcpmOpts.text_sources.length) {
        setStatus('请先添加蒸馏文本来源')
        showToast('请先添加蒸馏文本来源', 'warning')
        return
      }
      if (voxcpmOpts.device === 'cuda' && voxcpmRuntimeStatus.cuda_available === false && !voxcpmOpts.allow_cpu_fallback) {
        setStatus('当前机器未检测到可用 CUDA')
        showToast('VoxCPM2 请求使用 CUDA，但当前机器未检测到可用 CUDA', 'warning')
        return
      }
      if (voxcpmOpts.device === 'cuda' && voxcpmRuntimeStatus.cuda_available === false && voxcpmOpts.allow_cpu_fallback) {
        appendLog('[runtime] VoxCPM2 未检测到 CUDA，将回退 CPU；CPU 推理可能非常慢。')
        showToast('VoxCPM2 未检测到 CUDA，将回退 CPU，速度可能非常慢', 'warning')
      }
      const voxcpmPayload: VoxCpmDistillOptions = {
        ...voxcpmOpts,
        device: voxcpmOpts.device === 'cpu' ? 'cpu' : 'cuda',
        voice_mode: voxcpmOpts.voice_mode,
        voice_description: voxcpmOpts.voice_description.trim(),
        reference_audio: voxcpmOpts.reference_audio.trim(),
        prompt_text: voxcpmOpts.prompt_text.trim(),
        cfg_value: Math.max(0.1, Number(voxcpmOpts.cfg_value) || 2),
        inference_timesteps: Math.max(1, Math.floor(Number(voxcpmOpts.inference_timesteps) || 10)),
        min_len: Math.max(1, Math.floor(Number(voxcpmOpts.min_len) || 2)),
        max_len: Math.max(1, Math.floor(Number(voxcpmOpts.max_len) || 4096)),
        retry_badcase_max_times: Math.max(0, Math.floor(Number(voxcpmOpts.retry_badcase_max_times) || 3)),
        retry_badcase_ratio_threshold: Math.max(0.1, Number(voxcpmOpts.retry_badcase_ratio_threshold) || 6),
      }
      launchVoxcpmPipeline(effectiveOutputDir, commonOpts, voxcpmPayload)
      return
    }

    if (!audioFiles.length) {
      setStatus('请先添加音频文件')
      showToast('请先添加音频文件', 'warning')
      return
    }

    beginPipelineRun('piper', 'preprocess', '任务启动中...', '任务已启动')
    send('start_pipeline', {
      input_audio: audioFiles,
      output_dir: effectiveOutputDir,
      opts: commonOpts,
    })
  }

  const prepContent = (
    <Stack spacing={2}>
      <Paper sx={cardPaperSx}>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
          训练模式
        </Typography>
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          spacing={2}
          alignItems={{ xs: 'stretch', md: 'center' }}
          justifyContent="space-between"
        >
          <FormControl size="small" sx={{ minWidth: { xs: '100%', md: 280 } }}>
            <InputLabel>模式</InputLabel>
            <Select
              value={trainingMode}
              label="模式"
              onChange={(event) => handleTrainingModeChange(event.target.value as TrainingMode)}
              disabled={pipelineRunning}
            >
              <MenuItem value="piper">Piper 标准</MenuItem>
              <MenuItem value="gsv_distill">GPT-SoVITS 蒸馏</MenuItem>
              <MenuItem value="voxcpm_distill">VoxCPM2 蒸馏</MenuItem>
              <MenuItem value="resume_project">从旧项目继续训练</MenuItem>
            </Select>
          </FormControl>
          <Typography variant="body2" sx={{ opacity: 0.78 }}>
            {trainingMode === 'piper'
              ? '标准模式会重新做裁剪、VAD 和 ASR。'
              : trainingMode === 'gsv_distill'
                ? '蒸馏模式会直接从 GPT-SoVITS 说话人模型生成语料，再继续训练并导出 KIGTTS 语音包。'
                : trainingMode === 'voxcpm_distill'
                  ? 'VoxCPM2 蒸馏会用音色描述或参考音频生成语料，再继续训练并导出 KIGTTS 语音包。'
                  : '从旧项目读取已保存的训练模式和参数；音频完整时直接训练，缺失时按项目配置尝试补生成。'}
          </Typography>
        </Stack>
      </Paper>

      <Paper sx={cardPaperSx}>
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <Typography variant="subtitle1" fontWeight={600}>
            项目与输出
          </Typography>
        </Stack>
        <Box sx={{ mt: 2 }}>
          <PathField
            label="输出目录"
            value={outputDir}
            onChange={setOutputDir}
            onPick={pickOutputDir}
            onDropPath={handleOutputDrop}
          />
        </Box>
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          spacing={1}
          alignItems={{ xs: 'flex-start', md: 'center' }}
          sx={{ mt: 1 }}
        >
          <Button
            variant="contained"
            startIcon={<MsIcon name="folder_open" size={18} />}
            onClick={openOutputDirectory}
          >
            打开输出目录
          </Button>
          <Tooltip title="缓存目录为 <输出目录>/work" arrow>
            <Button
              variant="contained"
              startIcon={<MsIcon name="delete" size={18} />}
              onClick={clearWorkCache}
            >
              清除工作缓存
            </Button>
          </Tooltip>
        </Stack>
      </Paper>

      {trainingMode === 'resume_project' ? (
        <Paper sx={cardPaperSx}>
          <Stack spacing={1.5}>
            <Typography variant="subtitle1" fontWeight={600}>
              旧项目继续训练
            </Typography>
            <PathField
              label="旧训练项目目录"
              value={resumeProjectDir}
              onChange={(value) => {
                setResumeProjectDir(value)
                setResumeProjectStatus(null)
              }}
              onPick={pickResumeProjectDir}
              onDropPath={(value) => {
                setResumeProjectDir(value)
                setResumeProjectStatus(null)
                void inspectResumeProject(value)
              }}
              helperText="选择包含 work/metadata.csv 和 work/kigtts_project.json 的项目目录"
              placeholder="选择旧训练项目根目录"
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <Button
                variant="outlined"
                startIcon={resumeProjectBusy ? <CircularProgress size={16} color="inherit" /> : <MsIcon name="refresh" size={18} />}
                onClick={() => {
                  void inspectResumeProject()
                }}
                disabled={resumeProjectBusy || !resumeProjectDir.trim()}
              >
                检查项目
              </Button>
              <Typography variant="caption" sx={{ alignSelf: 'center', opacity: 0.72 }}>
                该模式会使用项目内保存的训练参数和蒸馏配置，不再使用当前页面的文本来源配置。
              </Typography>
            </Stack>
            {resumeProjectStatus && (
              <Alert severity={resumeProjectStatus.ok ? (resumeProjectStatus.needs_material_rebuild ? 'warning' : 'success') : 'error'}>
                <Stack spacing={1}>
                  <Typography variant="body2">{resumeProjectStatus.message}</Typography>
                  {resumeProjectStatus.material_status && (
                    <Typography variant="body2" sx={{ opacity: 0.86 }}>
                      {resumeProjectStatus.material_status}
                    </Typography>
                  )}
                  <Stack direction="row" spacing={0.75} useFlexGap flexWrap="wrap">
                    <Chip
                      size="small"
                      label={`模式：${TRAINING_MODE_LABELS[String(resumeProjectStatus.mode || '')] || resumeProjectStatus.mode || '未知'}`}
                    />
                    <Chip size="small" label={`metadata：${resumeProjectStatus.metadata_count ?? 0} 条`} />
                    <Chip size="small" label={`可用音频：${resumeProjectStatus.existing_count ?? 0} 条`} />
                    <Chip
                      size="small"
                      color={resumeProjectStatus.missing_count ? 'warning' : 'default'}
                      label={`缺失音频：${resumeProjectStatus.missing_count ?? 0} 条`}
                    />
                    <Chip
                      size="small"
                      color={resumeProjectStatus.direct_train_ready ? 'success' : 'warning'}
                      label={resumeProjectStatus.direct_train_ready ? '素材完整：直接训练' : '需要准备素材'}
                    />
                    {resumeProjectStatus.input_audio_count !== undefined && resumeProjectStatus.input_audio_count > 0 && (
                      <Chip
                        size="small"
                        color={resumeProjectStatus.input_audio_missing_count ? 'warning' : 'default'}
                        label={`原始音频：${resumeProjectStatus.input_audio_available_count ?? 0}/${resumeProjectStatus.input_audio_count}`}
                      />
                    )}
                    {resumeProjectStatus.metadata_inconsistent && (
                      <Chip size="small" color="warning" label="文本记录不一致" />
                    )}
                  </Stack>
                  {resumeProjectStatus.config_summary && (
                    <Typography variant="caption" sx={{ opacity: 0.78, wordBreak: 'break-all' }}>
                      配置摘要：{resumeProjectStatus.config_summary}
                    </Typography>
                  )}
                  {resumeProjectStatus.config_path && (
                    <Typography variant="caption" sx={{ opacity: 0.78, wordBreak: 'break-all' }}>
                      配置：{resumeProjectStatus.config_path}
                    </Typography>
                  )}
                </Stack>
              </Alert>
            )}
            <Alert severity="info">
              音频完整时会直接进入 Piper 训练；VoxCPM2 项目缺失音频会用项目内配置继续生成；GPT-SoVITS 项目缺失音频但找不到对应模型时，会移除缺失文本后继续训练，若音频完全缺失则无法开始。
            </Alert>
          </Stack>
        </Paper>
      ) : trainingMode === 'piper' ? (
        <Paper sx={cardPaperSx}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography variant="subtitle1" fontWeight={600}>
              音频导入
            </Typography>
            <Stack direction="row" spacing={1}>
              <Tooltip title="添加音频" arrow>
                <IconButton size="small" onClick={pickAudioFiles}>
                  <MsIcon name="add" size={20} />
                </IconButton>
              </Tooltip>
              <Tooltip title="清空音频" arrow>
                <IconButton size="small" onClick={clearAudioFiles}>
                  <MsIcon name="delete" size={20} />
                </IconButton>
              </Tooltip>
            </Stack>
          </Stack>
          <Box
            sx={{
              mt: 1,
              border: '1px dashed',
              borderColor: audioDragActive ? 'primary.main' : 'transparent',
              borderRadius: 1,
              p: 1,
              transition: 'border-color 0.15s ease',
            }}
            onDragOver={(event) => {
              event.preventDefault()
              if (event.dataTransfer) {
                event.dataTransfer.dropEffect = 'copy'
              }
              setAudioDragActive(true)
            }}
            onDragLeave={() => setAudioDragActive(false)}
            onDrop={handleAudioDrop}
          >
            <List dense sx={{ maxHeight: 240, overflow: 'auto' }}>
              {audioFiles.length === 0 && (
                <ListItem>
                  <ListItemText primary="暂无音频文件" secondary="点击添加或拖拽导入" />
                </ListItem>
              )}
              {audioFiles.map((file, index) => (
                <ListItem
                  key={`${file}-${index}`}
                  divider
                  secondaryAction={
                    <IconButton edge="end" size="small" onClick={() => removeAudioFile(index)}>
                      <MsIcon name="close" size={18} />
                    </IconButton>
                  }
                >
                  <ListItemText primary={file} />
                </ListItem>
              ))}
            </List>
          </Box>
        </Paper>
      ) : trainingMode === 'gsv_distill' ? (
        <>
          <Paper sx={cardPaperSx}>
            <Stack spacing={1.25}>
              <Stack spacing={1.25}>
                <Box sx={{ minWidth: 0 }}>
                  <Typography variant="subtitle1" fontWeight={600}>
                    GSVI 整合包获取索引
                  </Typography>
                  <Typography variant="body2" sx={{ mt: 0.5, opacity: 0.78 }}>
                    GSVI 是 GPT-SoVITS 推理特化整合包。蒸馏模式需要先准备兼容的整合包根目录，索引页里有整合包获取入口、模型资源和相关说明。
                  </Typography>
                </Box>
                <Button
                  variant="outlined"
                  startIcon={<MsIcon name="open_in_new" size={18} />}
                  onClick={() => {
                    void openGsviGuide()
                  }}
                >
                  打开索引页
                </Button>
              </Stack>
              <Typography
                variant="caption"
                sx={{
                  display: 'block',
                  wordBreak: 'break-all',
                  opacity: 0.7,
                  cursor: 'pointer',
                  '&:hover': { color: 'primary.main', opacity: 1 },
                }}
                onClick={() => {
                  void openGsviGuide()
                }}
              >
                {GSVI_GUIDE_URL}
              </Typography>
            </Stack>
          </Paper>

          <Paper sx={cardPaperSx}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              GPT-SoVITS 根目录
            </Typography>
            <Stack spacing={1}>
              <PathField
                label="GPT-SoVITS 根目录"
                value={distillOpts.gsv_root}
                onChange={(value) => setDistillOpts((prev) => ({ ...prev, gsv_root: value }))}
                onPick={pickGsvRoot}
                onDropPath={handleGsvRootDrop}
                helperText="例如 D:\\GPT-SoVITS-1007-cu124"
                placeholder="选择包含 runtime、models、GPT_SoVITS 的整合包目录"
              />
              {gsvRootBusy && (
                <Stack direction="row" spacing={1} alignItems="center">
                  <CircularProgress size={16} />
                  <Typography variant="caption" sx={{ opacity: 0.75 }}>
                    正在校验并扫描模型...
                  </Typography>
                </Stack>
              )}
              {gsvRootStatus && (
                <Alert severity={gsvRootStatus.ok ? 'success' : 'error'}>
                  {gsvRootStatus.message}
                </Alert>
              )}
            </Stack>
          </Paper>

          <Paper sx={cardPaperSx}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              模型选择
            </Typography>
            <Box
              sx={{
                display: 'grid',
                gap: 2,
                gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
              }}
            >
              <FormControl fullWidth size="small" disabled={!catalogVersions.length}>
                <InputLabel>版本</InputLabel>
                <Select
                  value={distillOpts.version}
                  label="版本"
                  onChange={(event) => setDistillOpts((prev) => ({ ...prev, version: event.target.value, speaker: '', prompt_lang: '', emotion: '' }))}
                >
                  {catalogVersions.map((version) => (
                    <MenuItem key={version} value={version}>
                      {version}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControl fullWidth size="small" disabled={!catalogSpeakers.length}>
                <InputLabel>说话人</InputLabel>
                <Select
                  value={distillOpts.speaker}
                  label="说话人"
                  onChange={(event) => setDistillOpts((prev) => ({ ...prev, speaker: event.target.value, prompt_lang: '', emotion: '' }))}
                >
                  {catalogSpeakers.map((speaker) => (
                    <MenuItem key={speaker} value={speaker}>
                      {speaker}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControl fullWidth size="small" disabled={!catalogLanguages.length}>
                <InputLabel>参考语言</InputLabel>
                <Select
                  value={distillOpts.prompt_lang}
                  label="参考语言"
                  onChange={(event) => setDistillOpts((prev) => ({ ...prev, prompt_lang: event.target.value, emotion: '' }))}
                >
                  {catalogLanguages.map((lang) => (
                    <MenuItem key={lang} value={lang}>
                      {lang}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControl fullWidth size="small" disabled={!catalogEmotions.length}>
                <InputLabel>情感</InputLabel>
                <Select
                  value={distillOpts.emotion}
                  label="情感"
                  onChange={(event) => setDistillOpts((prev) => ({ ...prev, emotion: event.target.value }))}
                >
                  {catalogEmotions.map((emotion) => (
                    <MenuItem key={`${emotion.name}-${emotion.ref_audio_path}`} value={emotion.name}>
                      {emotion.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>
            <Box sx={{ mt: 1.5 }}>
              {selectedEmotion ? (
                <Alert severity="info">
                  <Box sx={{ wordBreak: 'break-all' }}>
                    <Typography variant="body2" fontWeight={600}>
                      参考文本
                    </Typography>
                    <Typography variant="body2" sx={{ mt: 0.25 }}>
                      {selectedEmotion.prompt_text}
                    </Typography>
                    <Typography variant="caption" sx={{ display: 'block', mt: 0.75, opacity: 0.72 }}>
                      {selectedEmotion.ref_audio_path}
                    </Typography>
                  </Box>
                </Alert>
              ) : (
                <Typography variant="caption" sx={{ opacity: 0.7 }}>
                  选择版本、说话人、参考语言和情感后，这里会显示整合包里的参考文本与音频路径。
                </Typography>
              )}
            </Box>
          </Paper>

          <Paper sx={cardPaperSx}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Typography variant="subtitle1" fontWeight={600}>
                文本来源
              </Typography>
              <Stack direction="row" spacing={1}>
                <Tooltip title="添加文本来源" arrow>
                  <IconButton size="small" onClick={(event) => setDistillAddAnchorEl(event.currentTarget)}>
                    <MsIcon name="add" size={20} />
                  </IconButton>
                </Tooltip>
                <Tooltip title="清空文本来源" arrow>
                  <IconButton size="small" onClick={clearDistillSources}>
                    <MsIcon name="delete" size={20} />
                  </IconButton>
                </Tooltip>
              </Stack>
            </Stack>
            <Box
              sx={{
                mt: 1,
                border: '1px dashed',
                borderColor: distillSourcesDragActive ? 'primary.main' : 'transparent',
                borderRadius: 1,
                p: 1,
                transition: 'border-color 0.15s ease',
              }}
              onDragOver={(event) => {
                event.preventDefault()
                if (event.dataTransfer) {
                  event.dataTransfer.dropEffect = 'copy'
                }
                setDistillSourcesDragActive(true)
              }}
              onDragLeave={() => setDistillSourcesDragActive(false)}
              onDrop={handleDistillSourcesDrop}
            >
              <List dense sx={{ maxHeight: 240, overflow: 'auto' }}>
                {activeDistillTextSources.length === 0 && (
                  <ListItem>
                    <ListItemText primary="暂无文本来源" secondary={DISTILL_TEXT_SOURCE_EMPTY_HINT} />
                  </ListItem>
                )}
                {activeDistillTextSources.map((item) => (
                  <ListItem
                    key={`${item.kind}:${item.path}`}
                    divider
                    secondaryAction={
                      <IconButton edge="end" size="small" onClick={() => removeDistillSource(item)}>
                        <MsIcon name="close" size={18} />
                      </IconButton>
                    }
                  >
                    <ListItemIcon sx={{ minWidth: 28 }}>
                      <MsIcon name={item.kind === 'project_dir' ? 'folder' : 'article'} size={18} />
                    </ListItemIcon>
                    <ListItemText primary={getDistillSourcePrimaryText(item)} secondary={getDistillSourceSecondaryText(item)} />
                  </ListItem>
                ))}
              </List>
            </Box>
            <Popover
              open={Boolean(distillAddAnchorEl)}
              anchorEl={distillAddAnchorEl}
              onClose={() => setDistillAddAnchorEl(null)}
              anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
              transformOrigin={{ vertical: 'top', horizontal: 'right' }}
            >
              <List dense sx={{ py: 0.5, minWidth: 180 }}>
                <ListItemButton
                  onClick={() => {
                    openDistillPresetDialog()
                  }}
                >
                  <ListItemIcon sx={{ minWidth: 32 }}>
                    <MsIcon name="library_books" size={18} />
                  </ListItemIcon>
                  <ListItemText primary="添加预设文件" secondary="内置 5 万 / 10 万 / 15 万字版本" />
                </ListItemButton>
                <ListItemButton
                  onClick={() => {
                    setDistillAddAnchorEl(null)
                    void pickDistillTextFiles()
                  }}
                >
                  <ListItemIcon sx={{ minWidth: 32 }}>
                    <MsIcon name="article" size={18} />
                  </ListItemIcon>
                  <ListItemText primary="导入自定义文本文件" secondary=".txt / .csv / .jsonl" />
                </ListItemButton>
              </List>
            </Popover>
          </Paper>

          <Paper sx={cardPaperSx}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Typography variant="subtitle1" fontWeight={600}>
                推理参数
              </Typography>
              <Button
                size="small"
                variant="text"
                startIcon={<MsIcon name={distillAdvancedOpen ? 'expand_less' : 'expand_more'} size={18} />}
                onClick={() => setDistillAdvancedOpen((prev) => !prev)}
              >
                {distillAdvancedOpen ? '收起高级参数' : '展开高级参数'}
              </Button>
            </Stack>
            <Box
              sx={{
                mt: 1,
                display: 'grid',
                gap: 2,
                gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
              }}
            >
              <FormControl fullWidth size="small">
                <InputLabel>推理设备</InputLabel>
                <Select
                  value={distillOpts.device}
                  label="推理设备"
                  onChange={(event) => setDistillOpts((prev) => ({ ...prev, device: event.target.value as 'cpu' | 'cuda' }))}
                >
                  <MenuItem value="cuda">GPU/CUDA</MenuItem>
                  <MenuItem value="cpu">CPU</MenuItem>
                </Select>
              </FormControl>

              <FormControl fullWidth size="small">
                <InputLabel>合成文本语言</InputLabel>
                <Select
                  value={distillOpts.text_lang}
                  label="合成文本语言"
                  onChange={(event) => setDistillOpts((prev) => ({ ...prev, text_lang: event.target.value }))}
                >
                  {DISTILL_TEXT_LANGS.map((lang) => (
                    <MenuItem key={lang} value={lang}>
                      {lang}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControl fullWidth size="small">
                <InputLabel>切句方式</InputLabel>
                <Select
                  value={distillOpts.text_split_method}
                  label="切句方式"
                  onChange={(event) => setDistillOpts((prev) => ({ ...prev, text_split_method: event.target.value }))}
                >
                  {DISTILL_SPLIT_METHODS.map((method) => (
                    <MenuItem key={method} value={method}>
                      {method}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <NumberField
                label="语速"
                value={distillOpts.speed_factor}
                onChangeValue={(value) => setDistillOpts((prev) => ({ ...prev, speed_factor: value }))}
                inputProps={{ step: 0.05, min: 0.1 }}
              />

              <NumberField
                label="Temperature"
                value={distillOpts.temperature}
                onChangeValue={(value) => setDistillOpts((prev) => ({ ...prev, temperature: value }))}
                inputProps={{ step: 0.05, min: 0 }}
              />

              <NumberField
                label="蒸馏 batch size"
                value={distillOpts.batch_size}
                onChangeValue={(value) => setDistillOpts((prev) => ({ ...prev, batch_size: Math.max(1, value || 1) }))}
                inputProps={{ step: 1, min: 1 }}
              />

              <NumberField
                label="随机种子"
                value={distillOpts.seed}
                onChangeValue={(value) => setDistillOpts((prev) => ({ ...prev, seed: Math.trunc(value || -1) }))}
                inputProps={{ step: 1 }}
                helperText="-1 表示每条文本随机种子"
              />
            </Box>

            <Collapse in={distillAdvancedOpen} timeout={220}>
              <Box
                sx={{
                  mt: 2,
                  display: 'grid',
                  gap: 2,
                  gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
                }}
              >
                <NumberField
                  label="top_k"
                  value={distillOpts.top_k}
                  onChangeValue={(value) => setDistillOpts((prev) => ({ ...prev, top_k: Math.max(1, value || 1) }))}
                  inputProps={{ step: 1, min: 1 }}
                />
                <NumberField
                  label="top_p"
                  value={distillOpts.top_p}
                  onChangeValue={(value) => setDistillOpts((prev) => ({ ...prev, top_p: value }))}
                  inputProps={{ step: 0.05, min: 0, max: 1 }}
                />
                <NumberField
                  label="batch_threshold"
                  value={distillOpts.batch_threshold}
                  onChangeValue={(value) => setDistillOpts((prev) => ({ ...prev, batch_threshold: value }))}
                  inputProps={{ step: 0.05, min: 0 }}
                />
                <NumberField
                  label="fragment_interval"
                  value={distillOpts.fragment_interval}
                  onChangeValue={(value) => setDistillOpts((prev) => ({ ...prev, fragment_interval: value }))}
                  inputProps={{ step: 0.05, min: 0 }}
                />
                <NumberField
                  label="repetition_penalty"
                  value={distillOpts.repetition_penalty}
                  onChangeValue={(value) => setDistillOpts((prev) => ({ ...prev, repetition_penalty: value }))}
                  inputProps={{ step: 0.05, min: 0 }}
                />
                <NumberField
                  label="sample_steps"
                  value={distillOpts.sample_steps}
                  onChangeValue={(value) => setDistillOpts((prev) => ({ ...prev, sample_steps: Math.max(1, value || 1) }))}
                  inputProps={{ step: 1, min: 1 }}
                />
                <FormControlLabel
                  control={
                    <Switch
                      checked={distillOpts.split_bucket}
                      onChange={(event) => setDistillOpts((prev) => ({ ...prev, split_bucket: event.target.checked }))}
                    />
                  }
                  label="split_bucket"
                />
                <FormControlLabel
                  control={
                    <Switch
                      checked={distillOpts.parallel_infer}
                      onChange={(event) => setDistillOpts((prev) => ({ ...prev, parallel_infer: event.target.checked }))}
                    />
                  }
                  label="parallel_infer"
                />
                <FormControlLabel
                  control={
                    <Switch
                      checked={distillOpts.if_sr}
                      onChange={(event) => setDistillOpts((prev) => ({ ...prev, if_sr: event.target.checked }))}
                    />
                  }
                  label="if_sr"
                />
              </Box>
            </Collapse>
          </Paper>

          <Paper sx={cardPaperSx}>
            <Stack spacing={1.5}>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} alignItems={{ xs: 'stretch', md: 'center' }} justifyContent="space-between">
                <Box>
                  <Typography variant="subtitle1" fontWeight={600}>
                    语音合成预览
                  </Typography>
                  <Typography variant="body2" sx={{ mt: 0.5, opacity: 0.72 }}>
                    使用当前 GPT-SoVITS 模型和推理参数生成单条试听，不写入训练语料。
                  </Typography>
                </Box>
                <Stack direction="row" spacing={1}>
                  <Button
                    variant="contained"
                    startIcon={gsvDistillPreviewBusy ? <CircularProgress size={16} color="inherit" /> : <MsIcon name="play_arrow" size={18} />}
                    onClick={startGsvDistillPreview}
                    disabled={gsvDistillPreviewBusy || pipelineRunning}
                  >
                    生成试听
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<MsIcon name="download" size={18} />}
                    onClick={() => {
                      void exportDistillPreviewAudio(gsvDistillPreviewAudioPath, 'gsv_distill_preview.wav')
                    }}
                    disabled={!gsvDistillPreviewAudioPath}
                  >
                    导出音频
                  </Button>
                </Stack>
              </Stack>
              <TextField
                label="试听文本"
                value={gsvDistillPreviewText}
                onChange={(event) => setGsvDistillPreviewText(event.target.value)}
                multiline
                minRows={2}
                fullWidth
              />
              {gsvDistillPreviewBusy && <LinearProgress />}
              {gsvDistillPreviewAudioSrc && (
                <InlineAudioPlayer src={gsvDistillPreviewAudioSrc} audioPath={gsvDistillPreviewAudioPath} />
              )}
            </Stack>
          </Paper>
        </>
      ) : (
        <>
          <Paper sx={cardPaperSx}>
            <Stack spacing={1.5}>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.25} alignItems={{ xs: 'flex-start', md: 'center' }} justifyContent="space-between">
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                  <Typography variant="subtitle1" fontWeight={600}>
                    VoxCPM2 运行时
                  </Typography>
                  <Chip
                    size="small"
                    color={getRuntimeChipColor(voxcpmRuntimeStatus)}
                    label={getRuntimeChipLabel(voxcpmRuntimeStatus)}
                  />
                </Stack>
                <Box sx={runtimeActionRowSx}>
                  <Button
                    variant="outlined"
                    startIcon={<MsIcon name="refresh" size={18} />}
                    onClick={() => {
                      void refreshVoxcpmRuntimeStatus()
                    }}
                    disabled={voxcpmRuntimeBusy || pipelineRunning}
                  >
                    刷新状态
                  </Button>
                  <Button
                    variant="contained"
                    startIcon={<MsIcon name={voxcpmRuntimeStatus?.available ? 'build' : 'download'} size={18} />}
                    onClick={() => {
                      void installVoxcpmRuntime(Boolean(voxcpmRuntimeStatus?.available))
                    }}
                    disabled={voxcpmRuntimeBusy || pipelineRunning}
                  >
                    {voxcpmRuntimeStatus?.available ? '重建运行时' : '安装运行时'}
                  </Button>
                </Box>
              </Stack>
              <Box sx={{ minWidth: 0 }}>
                <Typography variant="body2" sx={{ opacity: 0.78 }}>
                  首次使用会在线创建 voxcpm_env，并安装 PyTorch 2.5+ / CUDA 12 相关依赖。安装前会自动测速 Conda / PyPI / PyTorch CUDA wheel 镜像，优先使用当前最快可达源并自动换源。软件本体不内置 CUDA 运行时。
                </Typography>
                <Typography variant="caption" sx={{ mt: 0.5, display: 'block', opacity: 0.62 }}>
                  候选源：Conda 含南科大/上交/清华/北外/中科大/南大/官方；PyPI 含阿里/北外/腾讯/上交/南科大等；PyTorch wheel 含阿里/上交/官方。
                </Typography>
              </Box>

              {(voxcpmRuntimeBusy || voxcpmRuntimeProgressMessage) && (
                <Stack spacing={0.75}>
                  {voxcpmRuntimeBusy && (
                    <LinearProgress
                      variant={voxcpmRuntimeProgressValue > 0 ? 'determinate' : 'indeterminate'}
                      value={Math.min(100, Math.max(0, voxcpmRuntimeProgressValue * 100))}
                    />
                  )}
                  {voxcpmRuntimeBusy && voxcpmRuntimeProgressValue > 0 && (
                    <Typography variant="caption" sx={{ opacity: 0.68 }}>
                      安装进度：{Math.round(voxcpmRuntimeProgressValue * 100)}%
                    </Typography>
                  )}
                  <Typography variant="caption" sx={{ opacity: 0.78 }}>
                    {voxcpmRuntimeProgressMessage || '正在处理 VoxCPM2 运行时...'}
                  </Typography>
                </Stack>
              )}

              {voxcpmRuntimeStatus && (
                <Alert severity={voxcpmRuntimeStatus.status === 'error' ? 'error' : voxcpmRuntimeStatus.cuda_available === false ? 'warning' : voxcpmRuntimeStatus.available ? 'success' : 'info'}>
                  <Stack spacing={0.5}>
                    <Typography variant="body2">{voxcpmRuntimeStatus.message}</Typography>
                    <Typography variant="caption" sx={{ opacity: 0.8 }}>
                      运行时目录：{voxcpmRuntimeStatus.env_path}
                    </Typography>
                    {voxcpmRuntimeStatus.driver_version && (
                      <Typography variant="caption" sx={{ opacity: 0.8 }}>
                        NVIDIA 驱动：{voxcpmRuntimeStatus.driver_version}
                        {voxcpmRuntimeStatus.gpu_name ? ` / ${voxcpmRuntimeStatus.gpu_name}` : ''}
                        {voxcpmRuntimeStatus.gpu_memory ? ` / ${voxcpmRuntimeStatus.gpu_memory}` : ''}
                      </Typography>
                    )}
                    {voxcpmRuntimeStatus.torch_version && (
                      <Typography variant="caption" sx={{ opacity: 0.8 }}>
                        Torch：{voxcpmRuntimeStatus.torch_version}
                        {voxcpmRuntimeStatus.torch_cuda_version ? ` / CUDA Runtime ${voxcpmRuntimeStatus.torch_cuda_version}` : ''}
                        {voxcpmRuntimeStatus.voxcpm_version ? ` / voxcpm ${voxcpmRuntimeStatus.voxcpm_version}` : ''}
                      </Typography>
                    )}
                    {formatRuntimeSources(voxcpmRuntimeStatus) && (
                      <Typography variant="caption" sx={{ opacity: 0.8 }}>
                        安装来源：{formatRuntimeSources(voxcpmRuntimeStatus)}
                      </Typography>
                    )}
                  </Stack>
                </Alert>
              )}

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                <Button
                  variant="outlined"
                  startIcon={<MsIcon name="folder_open" size={18} />}
                  onClick={() => {
                    void openVoxcpmRuntimeDirectory()
                  }}
                  disabled={voxcpmRuntimeBusy || !voxcpmRuntimeStatus}
                >
                  打开运行时目录
                </Button>
                <Typography variant="caption" sx={{ alignSelf: 'center', opacity: 0.72 }}>
                  默认使用 CUDA；如不可用，可允许回退 CPU，但速度可能非常慢。
                </Typography>
              </Stack>
            </Stack>
          </Paper>

          <Paper sx={cardPaperSx}>
            <Stack spacing={1.5}>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.25} alignItems={{ xs: 'flex-start', md: 'center' }} justifyContent="space-between">
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                  <Typography variant="subtitle1" fontWeight={600}>
                    VoxCPM2 模型
                  </Typography>
                  <Chip
                    size="small"
                    color={getVoxcpmModelChipColor(voxcpmModelStatus)}
                    label={getVoxcpmModelChipLabel(voxcpmModelStatus)}
                  />
                </Stack>
                <Box sx={runtimeActionRowSx}>
                  <Button
                    variant="outlined"
                    startIcon={<MsIcon name="refresh" size={18} />}
                    onClick={() => {
                      void refreshVoxcpmModelStatus()
                    }}
                    disabled={voxcpmModelBusy || pipelineRunning}
                  >
                    检查状态
                  </Button>
                  <Button
                    variant="contained"
                    startIcon={<MsIcon name={voxcpmModelStatus?.main_available ? 'sync' : 'download'} size={18} />}
                    onClick={() => {
                      void downloadVoxcpmModels(Boolean(voxcpmModelStatus?.main_available))
                    }}
                    disabled={voxcpmModelBusy || pipelineRunning || !voxcpmRuntimeStatus?.available}
                  >
                    {voxcpmModelStatus?.main_available ? '重新下载' : '下载模型'}
                  </Button>
                </Box>
              </Stack>
              <Typography variant="body2" sx={{ opacity: 0.78 }}>
                主模型与 denoiser 从 ModelScope 下载到用户数据目录，打包产物不会包含这些权重。
              </Typography>

              {(voxcpmModelBusy || voxcpmModelProgressMessage) && (
                <Stack spacing={0.75}>
                  {voxcpmModelBusy && (
                    <LinearProgress
                      variant={voxcpmModelProgressValue > 0 ? 'determinate' : 'indeterminate'}
                      value={Math.min(100, Math.max(0, voxcpmModelProgressValue * 100))}
                    />
                  )}
                  {voxcpmModelBusy && voxcpmModelProgressValue > 0 && (
                    <Typography variant="caption" sx={{ opacity: 0.68 }}>
                      下载进度：{Math.round(voxcpmModelProgressValue * 100)}%
                    </Typography>
                  )}
                  <Typography variant="caption" sx={{ opacity: 0.78 }}>
                    {voxcpmModelProgressMessage || '正在处理 VoxCPM2 模型...'}
                  </Typography>
                </Stack>
              )}

              {voxcpmModelStatus && (
                <Alert severity={!voxcpmModelStatus.main_available ? 'warning' : voxcpmModelStatus.denoiser_available ? 'success' : 'warning'}>
                  <Stack spacing={0.5}>
                    <Typography variant="body2">{voxcpmModelStatus.message}</Typography>
                    <Typography variant="caption" sx={{ opacity: 0.8 }}>
                      主模型：{voxcpmModelStatus.main_repo} {'->'} {voxcpmModelStatus.main_model_dir}
                    </Typography>
                    <Typography variant="caption" sx={{ opacity: 0.8 }}>
                      denoiser：{voxcpmModelStatus.denoiser_repo} {'->'} {voxcpmModelStatus.denoiser_model_dir}
                    </Typography>
                  </Stack>
                </Alert>
              )}

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                <Button
                  variant="outlined"
                  startIcon={<MsIcon name="folder_open" size={18} />}
                  onClick={() => {
                    void openVoxcpmModelDirectory()
                  }}
                  disabled={voxcpmModelBusy || !voxcpmModelStatus}
                >
                  打开模型目录
                </Button>
                <Typography variant="caption" sx={{ alignSelf: 'center', opacity: 0.72 }}>
                  默认会同时下载主模型和 denoiser；关闭 denoiser 后可只依赖主模型。
                </Typography>
              </Stack>
            </Stack>
          </Paper>

          <Paper sx={cardPaperSx}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              声音生成模式
            </Typography>
            <Stack spacing={2}>
              <FormControl fullWidth size="small">
                <InputLabel>模式</InputLabel>
                <Select
                  value={voxcpmOpts.voice_mode}
                  label="模式"
                  onChange={(event) => setVoxcpmOpts((prev) => ({ ...prev, voice_mode: event.target.value as VoxCpmVoiceMode }))}
                >
                  {VOXCPM_VOICE_MODES.map((mode) => (
                    <MenuItem key={mode.value} value={mode.value}>
                      {mode.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <Alert severity="info">{selectedVoxcpmVoiceMode.description}</Alert>
              {voxcpmOpts.voice_mode !== 'high_fidelity' && (
                <TextField
                  label={voxcpmOpts.voice_mode === 'description' ? '音色描述' : '风格控制描述（可选）'}
                  value={voxcpmOpts.voice_description}
                  onChange={(event) => setVoxcpmOpts((prev) => ({ ...prev, voice_description: event.target.value }))}
                  multiline
                  minRows={2}
                  fullWidth
                  helperText={
                    voxcpmOpts.voice_mode === 'description'
                      ? '官方格式会把描述包在括号中放到文本前，例如：年轻女性，温柔甜美，语速自然。'
                      : '可选。用于控制情绪、语速、语气；不填则按参考音频音色直接克隆。'
                  }
                />
              )}
              {voxcpmOpts.voice_mode !== 'description' && (
                <PathField
                  label="参考音频"
                  value={voxcpmOpts.reference_audio}
                  onChange={(value) => setVoxcpmOpts((prev) => ({ ...prev, reference_audio: value }))}
                  onPick={pickVoxcpmReferenceAudio}
                  onDropPath={(value) => setVoxcpmOpts((prev) => ({ ...prev, reference_audio: value }))}
                  onDropFiles={saveDroppedFileSingle}
                  helperText="建议使用清晰、短时长、单人声参考音频。"
                />
              )}
              {voxcpmOpts.voice_mode === 'high_fidelity' && (
                <TextField
                  label="参考音频转写文本"
                  value={voxcpmOpts.prompt_text}
                  onChange={(event) => setVoxcpmOpts((prev) => ({ ...prev, prompt_text: event.target.value }))}
                  multiline
                  minRows={2}
                  fullWidth
                  helperText="可留空自动调用训练设置里的 ASR 模型；手动填写越准确，高保真克隆越稳定。"
                />
              )}
              <Alert severity="info">
                VoxCPM2 试听和蒸馏都会按当前模式合成 wav 语料，再进入 Piper 训练和 KIGTTS 语音包导出。
              </Alert>
            </Stack>
          </Paper>

          <Paper sx={cardPaperSx}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Typography variant="subtitle1" fontWeight={600}>
                文本来源
              </Typography>
              <Stack direction="row" spacing={1}>
                <Tooltip title="添加文本来源" arrow>
                  <IconButton size="small" onClick={(event) => setDistillAddAnchorEl(event.currentTarget)}>
                    <MsIcon name="add" size={20} />
                  </IconButton>
                </Tooltip>
                <Tooltip title="清空文本来源" arrow>
                  <IconButton size="small" onClick={clearDistillSources}>
                    <MsIcon name="delete" size={20} />
                  </IconButton>
                </Tooltip>
              </Stack>
            </Stack>
            <Box
              sx={{
                mt: 1,
                border: '1px dashed',
                borderColor: distillSourcesDragActive ? 'primary.main' : 'transparent',
                borderRadius: 1,
                p: 1,
                transition: 'border-color 0.15s ease',
              }}
              onDragOver={(event) => {
                event.preventDefault()
                if (event.dataTransfer) {
                  event.dataTransfer.dropEffect = 'copy'
                }
                setDistillSourcesDragActive(true)
              }}
              onDragLeave={() => setDistillSourcesDragActive(false)}
              onDrop={handleDistillSourcesDrop}
            >
              <List dense sx={{ maxHeight: 240, overflow: 'auto' }}>
                {activeDistillTextSources.length === 0 && (
                  <ListItem>
                    <ListItemText primary="暂无文本来源" secondary={DISTILL_TEXT_SOURCE_EMPTY_HINT} />
                  </ListItem>
                )}
                {activeDistillTextSources.map((item) => (
                  <ListItem
                    key={`${item.kind}:${item.path}`}
                    divider
                    secondaryAction={
                      <IconButton edge="end" size="small" onClick={() => removeDistillSource(item)}>
                        <MsIcon name="close" size={18} />
                      </IconButton>
                    }
                  >
                    <ListItemIcon sx={{ minWidth: 28 }}>
                      <MsIcon name={item.kind === 'project_dir' ? 'folder' : 'article'} size={18} />
                    </ListItemIcon>
                    <ListItemText primary={getDistillSourcePrimaryText(item)} secondary={getDistillSourceSecondaryText(item)} />
                  </ListItem>
                ))}
              </List>
            </Box>
            <Popover
              open={Boolean(distillAddAnchorEl)}
              anchorEl={distillAddAnchorEl}
              onClose={() => setDistillAddAnchorEl(null)}
              anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
              transformOrigin={{ vertical: 'top', horizontal: 'right' }}
            >
              <List dense sx={{ py: 0.5, minWidth: 180 }}>
                <ListItemButton
                  onClick={() => {
                    openDistillPresetDialog()
                  }}
                >
                  <ListItemIcon sx={{ minWidth: 32 }}>
                    <MsIcon name="library_books" size={18} />
                  </ListItemIcon>
                  <ListItemText primary="添加预设文件" secondary="内置 5 万 / 10 万 / 15 万字版本" />
                </ListItemButton>
                <ListItemButton
                  onClick={() => {
                    setDistillAddAnchorEl(null)
                    void pickDistillTextFiles()
                  }}
                >
                  <ListItemIcon sx={{ minWidth: 32 }}>
                    <MsIcon name="article" size={18} />
                  </ListItemIcon>
                  <ListItemText primary="导入自定义文本文件" secondary=".txt / .csv / .jsonl" />
                </ListItemButton>
              </List>
            </Popover>
          </Paper>

          <Paper sx={cardPaperSx}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Typography variant="subtitle1" fontWeight={600}>
                VoxCPM2 推理参数
              </Typography>
              <Button
                size="small"
                variant="text"
                startIcon={<MsIcon name={voxcpmAdvancedOpen ? 'expand_less' : 'expand_more'} size={18} />}
                onClick={() => setVoxcpmAdvancedOpen((prev) => !prev)}
              >
                {voxcpmAdvancedOpen ? '收起高级参数' : '展开高级参数'}
              </Button>
            </Stack>
            <Box
              sx={{
                mt: 1,
                display: 'grid',
                gap: 2,
                gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
              }}
            >
              <FormControl fullWidth size="small">
                <InputLabel>推理设备</InputLabel>
                <Select
                  value={voxcpmOpts.device}
                  label="推理设备"
                  onChange={(event) => setVoxcpmOpts((prev) => ({ ...prev, device: event.target.value as 'cpu' | 'cuda' }))}
                >
                  <MenuItem value="cuda">GPU/CUDA</MenuItem>
                  <MenuItem value="cpu">CPU</MenuItem>
                </Select>
              </FormControl>
              <NumberField
                label="CFG 强度"
                value={voxcpmOpts.cfg_value}
                onChangeValue={(value) => setVoxcpmOpts((prev) => ({ ...prev, cfg_value: value }))}
                inputProps={{ step: 0.1, min: 0.1 }}
              />
              <NumberField
                label="推理步数"
                value={voxcpmOpts.inference_timesteps}
                onChangeValue={(value) => setVoxcpmOpts((prev) => ({ ...prev, inference_timesteps: Math.max(1, value || 1) }))}
                inputProps={{ step: 1, min: 1 }}
              />
              <NumberField
                label="max_len"
                value={voxcpmOpts.max_len}
                onChangeValue={(value) => setVoxcpmOpts((prev) => ({ ...prev, max_len: Math.max(1, value || 1) }))}
                inputProps={{ step: 64, min: 1 }}
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={voxcpmOpts.denoise}
                    onChange={(event) => setVoxcpmOpts((prev) => ({ ...prev, denoise: event.target.checked }))}
                  />
                }
                label="启用 denoiser"
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={voxcpmOpts.allow_cpu_fallback}
                    onChange={(event) => setVoxcpmOpts((prev) => ({ ...prev, allow_cpu_fallback: event.target.checked }))}
                  />
                }
                label="CUDA 不可用时回退 CPU"
              />
            </Box>
            <Collapse in={voxcpmAdvancedOpen} timeout={220}>
              <Box
                sx={{
                  mt: 2,
                  display: 'grid',
                  gap: 2,
                  gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
                }}
              >
                <NumberField
                  label="min_len"
                  value={voxcpmOpts.min_len}
                  onChangeValue={(value) => setVoxcpmOpts((prev) => ({ ...prev, min_len: Math.max(1, value || 1) }))}
                  inputProps={{ step: 1, min: 1 }}
                />
                <NumberField
                  label="坏例重试次数"
                  value={voxcpmOpts.retry_badcase_max_times}
                  onChangeValue={(value) => setVoxcpmOpts((prev) => ({ ...prev, retry_badcase_max_times: Math.max(0, value || 0) }))}
                  inputProps={{ step: 1, min: 0 }}
                />
                <NumberField
                  label="坏例时长比阈值"
                  value={voxcpmOpts.retry_badcase_ratio_threshold}
                  onChangeValue={(value) => setVoxcpmOpts((prev) => ({ ...prev, retry_badcase_ratio_threshold: value }))}
                  inputProps={{ step: 0.1, min: 0.1 }}
                />
                <FormControlLabel
                  control={
                    <Switch
                      checked={voxcpmOpts.normalize}
                      onChange={(event) => setVoxcpmOpts((prev) => ({ ...prev, normalize: event.target.checked }))}
                    />
                  }
                  label="文本规范化"
                />
                <FormControlLabel
                  control={
                    <Switch
                      checked={voxcpmOpts.retry_badcase}
                      onChange={(event) => setVoxcpmOpts((prev) => ({ ...prev, retry_badcase: event.target.checked }))}
                    />
                  }
                  label="坏例自动重试"
                />
              </Box>
            </Collapse>
          </Paper>

          <Paper sx={cardPaperSx}>
            <Stack spacing={1.5}>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} alignItems={{ xs: 'stretch', md: 'center' }} justifyContent="space-between">
                <Box>
                  <Typography variant="subtitle1" fontWeight={600}>
                    语音合成预览
                  </Typography>
                  <Typography variant="body2" sx={{ mt: 0.5, opacity: 0.72 }}>
                    使用当前 VoxCPM2 声音模式和推理参数生成单条试听，不写入训练语料。
                  </Typography>
                </Box>
                <Stack direction="row" spacing={1}>
                  <Button
                    variant="contained"
                    startIcon={voxcpmDistillPreviewBusy ? <CircularProgress size={16} color="inherit" /> : <MsIcon name="play_arrow" size={18} />}
                    onClick={startVoxcpmDistillPreview}
                    disabled={voxcpmDistillPreviewBusy || pipelineRunning}
                  >
                    生成试听
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<MsIcon name="download" size={18} />}
                    onClick={() => {
                      void exportDistillPreviewAudio(voxcpmDistillPreviewAudioPath, 'voxcpm_distill_preview.wav')
                    }}
                    disabled={!voxcpmDistillPreviewAudioPath}
                  >
                    导出音频
                  </Button>
                </Stack>
              </Stack>
              <TextField
                label="试听文本"
                value={voxcpmDistillPreviewText}
                onChange={(event) => setVoxcpmDistillPreviewText(event.target.value)}
                multiline
                minRows={2}
                fullWidth
              />
              {voxcpmDistillPreviewBusy && <LinearProgress />}
              {voxcpmDistillPreviewAudioSrc && (
                <InlineAudioPlayer src={voxcpmDistillPreviewAudioSrc} audioPath={voxcpmDistillPreviewAudioPath} />
              )}
            </Stack>
          </Paper>
        </>
      )}

      <Paper sx={cardPaperSx}>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
          语音包信息
        </Typography>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ xs: 'flex-start', md: 'center' }}>
          <Box sx={{ width: 96, textAlign: 'center', position: 'relative' }}>
            {(voicepackAvatarPreview || voicepackAvatar) && (
              <IconButton
                size="small"
                onClick={(event) => {
                  event.stopPropagation()
                  clearAvatar()
                }}
                sx={{
                  position: 'absolute',
                  top: 6,
                  right: 6,
                  width: 18,
                  height: 18,
                  bgcolor: 'rgba(0,0,0,0.3)',
                  color: '#fff',
                  zIndex: 2,
                  '&:hover': { bgcolor: 'rgba(0,0,0,0.45)' },
                }}
              >
                <MsIcon name="close" size={12} />
              </IconButton>
            )}
            <ButtonBase
              focusRipple
              onClick={openAvatarDialog}
              onDragOver={(event) => {
                event.preventDefault()
                if (event.dataTransfer) {
                  event.dataTransfer.dropEffect = 'copy'
                }
                setAvatarDragActive(true)
              }}
              onDragLeave={() => setAvatarDragActive(false)}
              onDrop={(event) => {
                setAvatarDragActive(false)
                handleAvatarDrop(event)
              }}
              sx={{
                width: 96,
                height: 96,
                borderRadius: 1,
                overflow: 'hidden',
                display: 'block',
                bgcolor: avatarDragActive
                  ? resolvedThemeMode === 'dark'
                    ? '#3a474a'
                    : '#eaf4f3'
                  : resolvedThemeMode === 'dark'
                    ? '#2d3537'
                    : '#e9f1f0',
                boxShadow: 2,
                transition: 'box-shadow 120ms cubic-bezier(0.4, 0, 0.2, 1), background-color 120ms cubic-bezier(0.4, 0, 0.2, 1)',
                '&:hover': {
                  boxShadow: 4,
                  bgcolor: resolvedThemeMode === 'dark' ? '#3a474a' : '#edf5f4',
                },
                '&:active': {
                  boxShadow: 1,
                  bgcolor: resolvedThemeMode === 'dark' ? '#2d3537' : '#e7efee',
                },
              }}
            >
              <Avatar
                variant="rounded"
                src={voicepackAvatarPreview || toFileUrl(voicepackAvatar)}
                sx={{
                  width: 96,
                  height: 96,
                  bgcolor: 'transparent',
                  color: resolvedThemeMode === 'dark' ? 'rgba(255,255,255,0.7)' : 'rgba(0,0,0,0.45)',
                }}
              >
                <MsIcon name="image" size={26} />
              </Avatar>
            </ButtonBase>
            <Typography variant="caption" sx={{ display: 'block', mt: 0.5, opacity: 0.7 }}>
              点击或拖入图片
            </Typography>
          </Box>
          <Stack spacing={2} sx={{ flex: 1, width: '100%' }}>
            <TextField
              label="名称"
              value={voicepackName}
              onChange={(e) => setVoicepackName(e.target.value)}
              fullWidth
              size="small"
              InputProps={{
                endAdornment: voicepackName ? (
                  <InputAdornment position="end">
                    <IconButton size="small" onClick={() => setVoicepackName('')}>
                      <MsIcon name="close" size={18} />
                    </IconButton>
                  </InputAdornment>
                ) : undefined,
              }}
            />
            <TextField
              label="备注"
              value={voicepackRemark}
              onChange={(e) => setVoicepackRemark(e.target.value)}
              fullWidth
              size="small"
              InputProps={{
                endAdornment: voicepackRemark ? (
                  <InputAdornment position="end">
                    <IconButton size="small" onClick={() => setVoicepackRemark('')}>
                      <MsIcon name="close" size={18} />
                    </IconButton>
                  </InputAdornment>
                ) : undefined,
              }}
            />
          </Stack>
        </Stack>
      </Paper>
    </Stack>
  )

  const settingsContent = (
    <Stack spacing={2}>
      <Paper sx={cardPaperSx}>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
          训练参数
        </Typography>
        <Box
          sx={{
            display: 'grid',
            gap: 2,
            gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' },
          }}
        >
          <FormControl fullWidth size="small">
            <InputLabel>音频等级</InputLabel>
            <Select value={quality} label="音频等级" onChange={(e) => setQuality(e.target.value as 'A' | 'B')}>
              <MenuItem value="A">A 档</MenuItem>
              <MenuItem value="B">B 档(降噪)</MenuItem>
            </Select>
          </FormControl>
          <TextField
            label="采样率"
            value={sampleRate}
            onChange={(e) => setSampleRate(e.target.value)}
            fullWidth
            size="small"
            InputProps={{
              endAdornment: sampleRate ? (
                <InputAdornment position="end">
                  <IconButton size="small" onClick={() => setSampleRate('')}>
                    <MsIcon name="close" size={18} />
                  </IconButton>
                </InputAdornment>
              ) : undefined,
            }}
          />
          <TextField
            label="训练 batch_size"
            value={trainBatchSize}
            onChange={(e) => setTrainBatchSize(e.target.value)}
            fullWidth
            size="small"
            helperText="显存不足时会自动降级重试"
            InputProps={{
              endAdornment: trainBatchSize ? (
                <InputAdornment position="end">
                  <IconButton size="small" onClick={() => setTrainBatchSize('')}>
                    <MsIcon name="close" size={18} />
                  </IconButton>
                </InputAdornment>
              ) : undefined,
            }}
          />
          <Box sx={{ gridColumn: '1 / -1' }}>
            <FormControlLabel
              control={<Switch checked={denoise} onChange={(e) => setDenoise(e.target.checked)} />}
              label="开启降噪/筛选"
            />
          </Box>
          <PathField
            label="ASR 模型 zip"
            value={asrModel}
            onChange={setAsrModel}
            onPick={pickAsrModel}
            onDropPath={setAsrModel}
            onDropFiles={saveDroppedFileSingle}
          />
          <PathField
            label="Piper 基线 ckpt (可选)"
            value={baseCkpt}
            onChange={setBaseCkpt}
            onPick={pickBaseCkpt}
            onDropPath={setBaseCkpt}
            onDropFiles={saveDroppedFileSingle}
          />
          <FormControlLabel
            control={<Switch checked={useEspeak} onChange={(e) => setUseEspeak(e.target.checked)} />}
            label="使用 espeak-ng (兼容基线)"
          />
          <PathField
            label="Piper config.json"
            value={piperConfig}
            onChange={setPiperConfig}
            onPick={pickPiperConfig}
            onDropPath={setPiperConfig}
            onDropFiles={saveDroppedFileSingle}
          />
          <FormControl fullWidth size="small">
            <InputLabel>Piper 训练设备</InputLabel>
            <Select value={device} label="Piper 训练设备" onChange={(e) => setDevice(e.target.value as 'cpu' | 'cuda')}>
              <MenuItem value="cpu">CPU</MenuItem>
              <MenuItem value="cuda">GPU/CUDA</MenuItem>
            </Select>
          </FormControl>
        </Box>
      </Paper>

      <Paper sx={cardPaperSx}>
        <Stack spacing={1.5}>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.25} alignItems={{ xs: 'flex-start', md: 'center' }} justifyContent="space-between">
            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
              <Typography variant="subtitle1" fontWeight={600}>
                Piper CUDA 运行时
              </Typography>
              <Chip
                size="small"
                color={getCudaRuntimeChipColor(cudaRuntimeStatus)}
                label={getCudaRuntimeChipLabel(cudaRuntimeStatus)}
              />
            </Stack>
            <Box sx={runtimeActionRowSx}>
              <Button
                variant="outlined"
                startIcon={<MsIcon name="refresh" size={18} />}
                onClick={() => {
                  void refreshCudaRuntimeStatus()
                }}
                disabled={cudaRuntimeBusy || pipelineRunning}
              >
                刷新状态
              </Button>
              <Button
                variant="contained"
                startIcon={<MsIcon name={cudaRuntimeStatus?.available ? 'build' : 'download'} size={18} />}
                onClick={() => {
                  void installCudaRuntime(Boolean(cudaRuntimeStatus?.available))
                }}
                disabled={cudaRuntimeBusy || pipelineRunning}
              >
                {cudaRuntimeStatus?.available ? '重建运行时' : '安装运行时'}
              </Button>
            </Box>
          </Stack>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="body2" sx={{ opacity: 0.78 }}>
              使用内置 micromamba 在线创建 `piper_env_cuda`。安装前会自动测速 Conda / PyPI / PyTorch CUDA wheel 镜像，优先使用当前最快可达源并自动换源。
            </Typography>
            <Typography variant="caption" sx={{ mt: 0.5, display: 'block', opacity: 0.62 }}>
              候选源：Conda 含南科大/上交/清华/北外/中科大/南大/官方；PyPI 含阿里/北外/腾讯/上交/南科大等；PyTorch wheel 含阿里/上交/官方。
            </Typography>
          </Box>

          {(cudaRuntimeBusy || cudaRuntimeProgressMessage) && (
            <Stack spacing={0.75}>
              {cudaRuntimeBusy && (
                <LinearProgress
                  variant={cudaRuntimeProgressValue > 0 ? 'determinate' : 'indeterminate'}
                  value={Math.min(100, Math.max(0, cudaRuntimeProgressValue * 100))}
                />
              )}
              {cudaRuntimeBusy && cudaRuntimeProgressValue > 0 && (
                <Typography variant="caption" sx={{ opacity: 0.68 }}>
                  安装进度：{Math.round(cudaRuntimeProgressValue * 100)}%
                </Typography>
              )}
              <Typography variant="caption" sx={{ opacity: 0.78 }}>
                {cudaRuntimeProgressMessage || '正在处理 Piper CUDA 运行时...'}
              </Typography>
            </Stack>
          )}

          {cudaRuntimeStatus && (
            <Alert severity={cudaRuntimeStatus.status === 'error' ? 'error' : cudaRuntimeStatus.cuda_available === false ? 'warning' : cudaRuntimeStatus.available ? 'success' : 'info'}>
              <Stack spacing={0.5}>
                <Typography variant="body2">{cudaRuntimeStatus.message}</Typography>
                <Typography variant="caption" sx={{ opacity: 0.8 }}>
                  运行时目录：{cudaRuntimeStatus.env_path}
                </Typography>
                <Typography variant="caption" sx={{ opacity: 0.8 }}>
                  micromamba：{cudaRuntimeStatus.micromamba_path || cudaRuntimeStatus.bundled_micromamba_path || '未找到'}
                </Typography>
                {cudaRuntimeStatus.driver_version && (
                  <Typography variant="caption" sx={{ opacity: 0.8 }}>
                    NVIDIA 驱动：{cudaRuntimeStatus.driver_version}
                    {cudaRuntimeStatus.gpu_name ? ` / ${cudaRuntimeStatus.gpu_name}` : ''}
                    {cudaRuntimeStatus.gpu_memory ? ` / ${cudaRuntimeStatus.gpu_memory}` : ''}
                  </Typography>
                )}
                {cudaRuntimeStatus.torch_version && (
                  <Typography variant="caption" sx={{ opacity: 0.8 }}>
                    Torch：{cudaRuntimeStatus.torch_version}
                    {cudaRuntimeStatus.torch_cuda_version ? ` / CUDA Runtime ${cudaRuntimeStatus.torch_cuda_version}` : ''}
                  </Typography>
                )}
                {formatRuntimeSources(cudaRuntimeStatus) && (
                  <Typography variant="caption" sx={{ opacity: 0.8 }}>
                    安装来源：{formatRuntimeSources(cudaRuntimeStatus)}
                  </Typography>
                )}
              </Stack>
            </Alert>
          )}

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
            <Button
              variant="outlined"
              startIcon={<MsIcon name="folder_open" size={18} />}
              onClick={() => {
                void openCudaRuntimeDirectory()
              }}
              disabled={cudaRuntimeBusy || !cudaRuntimeStatus}
            >
              打开运行时目录
            </Button>
            <Typography variant="caption" sx={{ alignSelf: 'center', opacity: 0.72 }}>
              建议仅在 NVIDIA 驱动正常、系统已能识别 GPU 的机器上安装。
            </Typography>
          </Stack>
        </Stack>
      </Paper>

      <Paper sx={cardPaperSx}>
        <Stack spacing={1}>
          <Typography variant="subtitle1" fontWeight={600}>
            进度
          </Typography>
          {activeProgressStages.map((stage) => (
            <Box key={stage}>
              <Stack direction="row" justifyContent="space-between">
                <Typography variant="caption" sx={{ opacity: 0.7 }}>
                  {STAGE_LABEL[stage]}
                </Typography>
                <Typography variant="caption">{Math.round((progress[stage] ?? 0) * 100)}%</Typography>
              </Stack>
              <LinearProgress variant="determinate" value={(progress[stage] ?? 0) * 100} sx={{ height: 8, borderRadius: 6 }} />
            </Box>
          ))}
        </Stack>
      </Paper>

      <Paper sx={cardPaperSx}>
        <Stack spacing={1}>
          <Typography variant="subtitle1" fontWeight={600}>
            系统
          </Typography>
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={1}
            alignItems={{ sm: 'center' }}
            sx={{ width: '100%' }}
          >
            <FormControl size="small" sx={{ width: { xs: '100%', sm: 220 } }}>
              <InputLabel>主题模式</InputLabel>
              <Select
                value={themeMode}
                label="主题模式"
                onChange={(e) => setThemeMode(e.target.value as 'system' | 'light' | 'dark')}
              >
                <MenuItem value="system">跟随系统</MenuItem>
                <MenuItem value="dark">暗色</MenuItem>
                <MenuItem value="light">明亮</MenuItem>
              </Select>
            </FormControl>
            <FormControlLabel
              control={
                <Switch
                  checked={contextMenuMode === 'custom'}
                  onChange={(e) => setContextMenuMode(e.target.checked ? 'custom' : 'native')}
                />
              }
              label="自绘右键菜单"
            />
            <Box sx={{ flexGrow: 1, display: { xs: 'none', sm: 'block' } }} />
            <Stack
              direction="row"
              spacing={1}
              sx={{
                width: { xs: '100%', sm: 'auto' },
                justifyContent: 'flex-end',
              }}
            >
              <Button
                variant="contained"
                color="primary"
                startIcon={<MsIcon name="restart_alt" size={18} />}
                onClick={requestBackendRestart}
              >
                重启后端
              </Button>
            </Stack>
          </Stack>
        </Stack>
      </Paper>
    </Stack>
  )

  const previewContent = (
    <Stack spacing={2}>
      <Paper sx={cardPaperSx}>
        <Typography variant="subtitle1" fontWeight={600} gutterBottom>
          语音包试听
        </Typography>
        <Stack spacing={2}>
          <PathField
            label="语音包 .kigvpk/.zip/目录"
            value={previewVoicepack}
            onChange={setPreviewVoicepack}
            onPick={pickPreviewVoicepack}
            onDropPath={setPreviewVoicepack}
            onDropFiles={saveDroppedFileSingle}
          />
          <TextField
            label="试听文本"
            value={previewText}
            onChange={(e) => setPreviewText(e.target.value)}
            multiline
            minRows={3}
            fullWidth
          />
          <Stack direction="row" spacing={1}>
            <Button
              variant="contained"
              startIcon={<MsIcon name="play_arrow" size={18} />}
              onClick={startPreview}
              disabled={previewBusy || pipelineRunning}
            >
              {previewBusy ? '生成中...' : '生成试听'}
            </Button>
            <Button
              variant="outlined"
              startIcon={<MsIcon name="download" size={18} />}
              onClick={exportPreviewAudio}
              disabled={!previewAudioPath}
            >
              导出音频
            </Button>
            <Button
              variant="outlined"
              startIcon={<MsIcon name="folder_open" size={18} />}
              onClick={openCurrentVoicepackDirectory}
              disabled={!previewVoicepack.trim()}
            >
              打开当前语音包目录
            </Button>
          </Stack>
          {previewBusy && <LinearProgress />}
        </Stack>
      </Paper>

      <Paper sx={cardPaperSx}>
        <Typography variant="subtitle1" fontWeight={600}>
          播放器
        </Typography>
        <Stack spacing={1} sx={{ mt: 1 }}>
          <Stack direction="row" spacing={1} alignItems="center">
            <Tooltip title={previewPlaying ? '暂停' : '播放'} arrow>
              <span>
                <IconButton color="primary" onClick={togglePreviewPlay} disabled={!hasPreviewAudio}>
                  <MsIcon name={previewPlaying ? 'pause' : 'play_arrow'} size={20} fill={1} />
                </IconButton>
              </span>
            </Tooltip>
            <Tooltip title="停止" arrow>
              <span>
                <IconButton onClick={stopPreviewPlay} disabled={!hasPreviewAudio}>
                  <MsIcon name="stop" size={20} fill={1} />
                </IconButton>
              </span>
            </Tooltip>
            <Box sx={{ flex: 1, px: 1 }}>
              <Slider
                size="small"
                min={0}
                max={previewDuration > 0 ? previewDuration : 1}
                step={0.01}
                value={hasPreviewAudio ? Math.min(previewCurrentTime, previewDuration || 1) : 0}
                onChange={seekPreviewPlay}
                disabled={!hasPreviewAudio}
              />
            </Box>
            <Typography variant="caption" sx={{ minWidth: 96, textAlign: 'right', opacity: 0.75 }}>
              {formatTime(previewCurrentTime)} / {formatTime(previewDuration)}
            </Typography>
          </Stack>
          <Typography variant="caption" sx={{ opacity: 0.75, wordBreak: 'break-all' }}>
            {previewAudioPath || '暂无试听音频'}
          </Typography>
        </Stack>
      </Paper>
    </Stack>
  )

  const logsContent = (
    <Paper sx={{ ...cardPaperSx, flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between">
        <Typography variant="subtitle1" fontWeight={600}>
          日志
        </Typography>
        <Stack direction="row" alignItems="center" spacing={0.5}>
          <Chip
            label={connected ? 'Backend Ready' : 'Backend Offline'}
            color={connected ? 'success' : 'default'}
            variant="outlined"
            size="small"
          />
          <Tooltip title="导出日志" arrow>
            <IconButton size="small" onClick={exportLogs}>
              <MsIcon name="download" size={18} />
            </IconButton>
          </Tooltip>
        </Stack>
      </Stack>
      <Box
        className="allow-text-select"
        sx={{
          mt: 1,
          flex: 1,
          minHeight: 0,
          overflow: 'auto',
          fontFamily: 'Menlo, Consolas, monospace',
          fontSize: 12,
          whiteSpace: 'pre-wrap',
        }}
      >
        {logs.length === 0 ? (
          <Typography variant="caption" sx={{ opacity: 0.6 }}>
            暂无日志
          </Typography>
        ) : (
          logs.map((line, idx) => (
            <Box key={`${idx}-${line}`}>{line}</Box>
          ))
        )}
      </Box>
    </Paper>
  )

  const aboutContent = (
    <Stack spacing={2}>
      <Paper
        sx={{
          ...cardPaperSx,
          p: { xs: 3, md: 4 },
          background:
            resolvedThemeMode === 'dark'
              ? 'linear-gradient(135deg, rgba(3,131,135,0.18), rgba(11,15,20,0.92))'
              : 'linear-gradient(135deg, rgba(3,131,135,0.12), rgba(255,255,255,0.98))',
        }}
      >
        <Stack spacing={2} alignItems="center" textAlign="center">
          <Box
            component="span"
            role="img"
            aria-label="KIGTTS"
            sx={{
              position: 'relative',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              height: { xs: 48, md: 62 },
              width: { xs: 220, md: 280 },
              maxWidth: '100%',
            }}
          >
            <Box
              component="img"
              src={logoBlack}
              alt=""
              aria-hidden
              sx={{
                position: 'absolute',
                inset: 0,
                width: '100%',
                height: '100%',
                objectFit: 'contain',
                opacity: resolvedThemeMode === 'dark' ? 0 : 1,
                filter: resolvedThemeMode === 'dark' ? 'blur(1px)' : 'blur(0px)',
                transition: 'opacity 220ms ease, filter 220ms ease',
              }}
            />
            <Box
              component="img"
              src={logoWhite}
              alt=""
              aria-hidden
              sx={{
                position: 'absolute',
                inset: 0,
                width: '100%',
                height: '100%',
                objectFit: 'contain',
                opacity: resolvedThemeMode === 'dark' ? 1 : 0,
                filter: resolvedThemeMode === 'dark' ? 'blur(0px)' : 'blur(1px)',
                transition: 'opacity 220ms ease, filter 220ms ease',
              }}
            />
          </Box>
          <Typography variant="body2" sx={{ opacity: 0.72, letterSpacing: 0.5 }}>
            Version {APP_VERSION}
          </Typography>
        </Stack>
      </Paper>

      <Paper sx={cardPaperSx}>
        <Stack spacing={1.5}>
          <Typography variant="subtitle1" fontWeight={600}>
            软件制作
          </Typography>
          <Box
            sx={{
              display: 'grid',
              gap: 1.5,
              gridTemplateColumns: { xs: '1fr', md: 'repeat(3, minmax(0, 1fr))' },
            }}
          >
            {ABOUT_CREATORS.map((creator) => (
              <Box
                key={creator.name}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1.5,
                  minWidth: 0,
                  py: 0.5,
                }}
              >
                <Avatar src={creator.avatar} alt={creator.name} sx={{ width: 64, height: 64 }} />
                <Stack spacing={0.35} sx={{ minWidth: 0, flex: 1 }}>
                  <Typography variant="body1" fontWeight={600} noWrap>
                    {creator.name}
                  </Typography>
                  <Stack direction="row" spacing={0.5} alignItems="center" sx={{ minWidth: 0 }}>
                    <Link
                      href={creator.homepage}
                      underline="none"
                      color="primary"
                      sx={{
                        flex: 1,
                        minWidth: 0,
                        px: 0.5,
                        py: 0.2,
                        borderRadius: 1,
                        fontSize: 12,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        transition: 'background-color 120ms ease',
                        '&:hover': {
                          bgcolor: 'action.hover',
                          textDecoration: 'none',
                        },
                      }}
                      onClick={(event) => {
                        event.preventDefault()
                        void openExternalLink(creator.homepage, `${creator.name} 主页`)
                      }}
                    >
                      {creator.homepage}
                    </Link>
                    <Tooltip title={`打开 ${creator.name} 主页`} arrow>
                      <IconButton
                        size="small"
                        sx={{ flexShrink: 0 }}
                        onClick={() => {
                          void openExternalLink(creator.homepage, `${creator.name} 主页`)
                        }}
                      >
                        <MsIcon name="open_in_new" size={18} />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                </Stack>
              </Box>
            ))}
          </Box>
        </Stack>
      </Paper>

      <Paper sx={cardPaperSx}>
        <Stack spacing={1.5}>
          <Typography variant="subtitle1" fontWeight={600}>
            关于
          </Typography>
          <Stack spacing={0.25} alignItems="flex-start">
            <Button
              variant="text"
              sx={{
                px: 0.5,
                py: 0.25,
                minWidth: 0,
                justifyContent: 'flex-start',
                borderRadius: 1,
                '&:hover': { bgcolor: 'action.hover' },
              }}
              onClick={() => setAboutDialog('openSource')}
            >
              开源许可证
            </Button>
            <Button
              variant="text"
              sx={{
                px: 0.5,
                py: 0.25,
                minWidth: 0,
                justifyContent: 'flex-start',
                borderRadius: 1,
                '&:hover': { bgcolor: 'action.hover' },
              }}
              onClick={() => setAboutDialog('privacy')}
            >
              隐私政策
            </Button>
          </Stack>
        </Stack>
      </Paper>
    </Stack>
  )

  const renderContent = (currentPage: AppPage) => {
    if (currentPage === 'prep') return prepContent
    if (currentPage === 'settings') return settingsContent
    if (currentPage === 'preview') return previewContent
    if (currentPage === 'about') return aboutContent
    return logsContent
  }

  const pageBody =
    displayPage === 'logs' ? (
      <Box sx={{ flex: 1, minHeight: 0, px: 0.75, pb: 2, display: 'flex', overflow: 'hidden' }}>{logsContent}</Box>
    ) : (
      <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto', px: 0.75, pb: 2 }}>
        {renderContent(displayPage)}
      </Box>
    )

  const mainContent = (
    <Box sx={{ flex: 1, minHeight: 0, overflow: 'hidden' }}>
      <Box
        sx={{
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          ...(pageTransitionPhase === 'out'
            ? {
                animation: `kgttsSharedAxisYOut ${PAGE_FADE_OUT_MS}ms cubic-bezier(0.4, 0, 1, 1) both`,
              }
            : pageTransitionPhase === 'in'
              ? {
                  animation: `kgttsSharedAxisYIn ${PAGE_FADE_IN_MS}ms cubic-bezier(0, 0, 0.2, 1) both`,
                }
              : {}),
          '@keyframes kgttsSharedAxisYOut': {
            from: { opacity: 1, transform: 'translateY(0px)' },
            to: { opacity: 0, transform: 'translateY(-8px)' },
          },
          '@keyframes kgttsSharedAxisYIn': {
            from: { opacity: 0, transform: 'translateY(12px)' },
            to: { opacity: 1, transform: 'translateY(0px)' },
          },
        }}
      >
        {pageBody}
      </Box>
    </Box>
  )

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ height: '100vh', overflow: 'hidden', bgcolor: 'background.default' }}>
        <Box
          sx={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            height: TITLEBAR_HEIGHT,
            display: 'flex',
            alignItems: 'center',
            px: 2,
            gap: 2,
            bgcolor: 'background.paper',
            borderBottom: '1px solid',
            borderColor: 'divider',
            WebkitAppRegion: 'drag',
            userSelect: 'none',
            zIndex: 1200,
            boxShadow: theme.shadows[2],
          }}
          onDoubleClick={() => window.windowControls?.toggleMaximize()}
        >
          <Box sx={{ WebkitAppRegion: 'no-drag' }}>
            <Tooltip title={drawerExpanded ? '收纳导航' : '展开导航'} arrow>
              <IconButton
                size="small"
                onClick={() => setDrawerExpanded((prev) => !prev)}
                sx={titlebarButtonSx}
              >
                <MsIcon name="menu" size={20} />
              </IconButton>
            </Tooltip>
          </Box>
          <Box sx={{ flexGrow: 1, display: 'flex', alignItems: 'center' }}>
            <Box
              sx={{
                position: 'relative',
                width: { xs: 156, md: 188 },
                height: 22,
                overflow: 'hidden',
              }}
              aria-label="KIGTTS"
            >
              <Box
                component="img"
                src={logoBlack}
                alt=""
                sx={{
                  position: 'absolute',
                  inset: 0,
                  display: 'block',
                  height: 22,
                  width: 'auto',
                  maxWidth: '100%',
                  objectFit: 'contain',
                  opacity: resolvedThemeMode === 'dark' ? 0 : 1,
                  transform: resolvedThemeMode === 'dark' ? 'translateY(1px)' : 'translateY(0)',
                  transition: theme.transitions.create(['opacity', 'transform'], {
                    duration: theme.transitions.duration.short,
                    easing: theme.transitions.easing.easeInOut,
                  }),
                }}
              />
              <Box
                component="img"
                src={logoWhite}
                alt=""
                sx={{
                  position: 'absolute',
                  inset: 0,
                  display: 'block',
                  height: 22,
                  width: 'auto',
                  maxWidth: '100%',
                  objectFit: 'contain',
                  opacity: resolvedThemeMode === 'dark' ? 1 : 0,
                  transform: resolvedThemeMode === 'dark' ? 'translateY(0)' : 'translateY(-1px)',
                  transition: theme.transitions.create(['opacity', 'transform'], {
                    duration: theme.transitions.duration.short,
                    easing: theme.transitions.easing.easeInOut,
                  }),
                }}
              />
            </Box>
          </Box>
          <Box sx={{ display: 'flex', gap: 1, WebkitAppRegion: 'no-drag', alignItems: 'center' }}>
            <Chip label={status} variant="outlined" size="small" />
            <Tooltip title="切换亮/暗主题" arrow>
              <IconButton
                size="small"
                onClick={handleTitlebarThemeToggle}
                sx={titlebarButtonSx}
              >
                {resolvedThemeMode === 'dark' ? <MsIcon name="light_mode" size={20} /> : <MsIcon name="dark_mode" size={20} />}
              </IconButton>
            </Tooltip>
          </Box>
          <Box sx={{ display: 'flex', gap: 0.5, ml: 1.5, WebkitAppRegion: 'no-drag' }}>
            <IconButton
              size="small"
              onClick={() => window.windowControls?.minimize()}
              sx={titlebarButtonSx}
            >
              <MsIcon name="remove" size={20} />
            </IconButton>
            <IconButton
              size="small"
              onClick={() => window.windowControls?.toggleMaximize()}
              sx={titlebarButtonSx}
            >
              {isMaximized ? <MsIcon name="filter_none" size={20} /> : <MsIcon name="crop_square" size={20} />}
            </IconButton>
            <IconButton
              size="small"
              onClick={() => window.windowControls?.close()}
              sx={{
                width: 32,
                height: 32,
                borderRadius: 6,
                color: resolvedThemeMode === 'dark' ? '#cfcfcf' : '#2f3f3f',
                '&:hover': {
                  bgcolor: 'rgba(244, 67, 54, 0.25)',
                  color: resolvedThemeMode === 'dark' ? '#ff8a80' : '#b71c1c',
                  borderRadius: '50%',
                },
              }}
            >
              <MsIcon name="close" size={20} />
            </IconButton>
          </Box>
        </Box>

        <Box
          sx={{
            height: `calc(100vh - ${TITLEBAR_HEIGHT}px)`,
            mt: `${TITLEBAR_HEIGHT}px`,
            overflow: 'hidden',
            display: 'grid',
            gridTemplateColumns: `${drawerWidth}px minmax(0, 1fr)`,
            transition: theme.transitions.create(['grid-template-columns'], {
              duration: theme.transitions.duration.shorter,
              easing: theme.transitions.easing.sharp,
            }),
          }}
        >
          <Drawer
            variant="permanent"
            PaperProps={{
              sx: {
                position: 'relative',
                width: '100%',
                overflowX: 'hidden',
                bgcolor: 'background.paper',
                color: 'text.primary',
                borderRadius: 0,
                borderRight: '1px solid',
                borderColor: 'divider',
                boxShadow: 'none',
                transition: theme.transitions.create(['width', 'background-color', 'border-color', 'color', 'box-shadow'], {
                  duration: theme.transitions.duration.shorter,
                  easing: theme.transitions.easing.sharp,
                }),
              },
            }}
            sx={{
              width: '100%',
              '& .MuiDrawer-paper': {
                width: '100%',
                boxSizing: 'border-box',
              },
            }}
          >
            <List sx={{ pt: 1 }}>
              {NAV_ITEMS.map((item) => {
                const button = (
                  <ListItemButton
                    selected={page === item.key}
                    onClick={() => setPage(item.key)}
                    sx={{
                      minHeight: 48,
                      borderRadius: 0,
                      px: drawerExpanded ? NAV_EXPANDED_PADDING_X : NAV_COLLAPSED_PADDING_X,
                      justifyContent: 'flex-start',
                      transition: theme.transitions.create(
                        ['padding-left', 'padding-right', 'background-color', 'border-color', 'color'],
                        {
                          duration: theme.transitions.duration.shorter,
                          easing: theme.transitions.easing.sharp,
                        },
                      ),
                    }}
                  >
                    <ListItemIcon
                      sx={{
                        minWidth: NAV_ICON_SLOT,
                        mr: drawerExpanded ? 1 : 0,
                        justifyContent: 'center',
                        color: 'inherit',
                        transition: theme.transitions.create(['margin-right', 'color'], {
                          duration: theme.transitions.duration.shorter,
                          easing: theme.transitions.easing.sharp,
                        }),
                      }}
                    >
                      <MsIcon name={item.icon} size={22} />
                    </ListItemIcon>
                    <Box
                      sx={{
                        minWidth: 0,
                        overflow: 'hidden',
                        maxWidth: drawerExpanded ? 180 : 0,
                        opacity: drawerExpanded ? 1 : 0,
                        transform: drawerExpanded ? 'translateX(0)' : 'translateX(-6px)',
                        transition: theme.transitions.create(['max-width', 'opacity', 'transform'], {
                          duration: theme.transitions.duration.shorter,
                          easing: theme.transitions.easing.sharp,
                        }),
                        transitionDelay: drawerExpanded ? '70ms' : '0ms',
                        pointerEvents: 'none',
                      }}
                    >
                      <ListItemText
                        primary={item.label}
                        primaryTypographyProps={{ noWrap: true }}
                        sx={{
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          minWidth: 0,
                        }}
                      />
                    </Box>
                  </ListItemButton>
                )

                return (
                  <ListItem key={item.key} disablePadding>
                    <Tooltip title={item.label} placement="right" arrow disableHoverListener={drawerExpanded}>
                      {button}
                    </Tooltip>
                  </ListItem>
                )
              })}
            </List>
            <List sx={{ mt: 'auto', pb: 0.75 }}>
              <Collapse in={pipelineRunning && pipelineCardMinimized} timeout={220} unmountOnExit>
                <ListItem disablePadding>
                  <Tooltip title="恢复总进度条" placement="right" arrow disableHoverListener={drawerExpanded}>
                    <ListItemButton
                      onClick={() => setPipelineCardMinimized(false)}
                      sx={{
                        minHeight: 48,
                        borderRadius: 0,
                        px: drawerExpanded ? NAV_EXPANDED_PADDING_X : NAV_COLLAPSED_PADDING_X,
                        justifyContent: 'flex-start',
                        transition: theme.transitions.create(
                          ['padding-left', 'padding-right', 'background-color', 'border-color', 'color'],
                          {
                            duration: theme.transitions.duration.shorter,
                            easing: theme.transitions.easing.sharp,
                          },
                        ),
                      }}
                    >
                      <ListItemIcon
                        sx={{
                          minWidth: NAV_ICON_SLOT,
                          mr: drawerExpanded ? 1 : 0,
                          justifyContent: 'center',
                          color: 'inherit',
                          transition: theme.transitions.create(['margin-right', 'color'], {
                            duration: theme.transitions.duration.shorter,
                            easing: theme.transitions.easing.sharp,
                          }),
                        }}
                      >
                        <CircularProgress
                          variant="determinate"
                          value={Math.round(overallProgress * 100)}
                          size={22}
                          thickness={5}
                        />
                      </ListItemIcon>
                      <Box
                        sx={{
                          minWidth: 0,
                          overflow: 'hidden',
                          maxWidth: drawerExpanded ? 180 : 0,
                          opacity: drawerExpanded ? 1 : 0,
                          transform: drawerExpanded ? 'translateX(0)' : 'translateX(-6px)',
                          transition: theme.transitions.create(['max-width', 'opacity', 'transform'], {
                            duration: theme.transitions.duration.shorter,
                            easing: theme.transitions.easing.sharp,
                          }),
                          transitionDelay: drawerExpanded ? '70ms' : '0ms',
                          pointerEvents: 'none',
                        }}
                      >
                        <ListItemText
                          primary={`总进度 ${Math.round(overallProgress * 100)}%`}
                          primaryTypographyProps={{ noWrap: true, variant: 'body2' }}
                          sx={{
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            minWidth: 0,
                          }}
                        />
                      </Box>
                    </ListItemButton>
                  </Tooltip>
                </ListItem>
              </Collapse>
            </List>
          </Drawer>
          <Box
            sx={{
              flex: 1,
              minWidth: 0,
              overflow: 'hidden',
              position: 'relative',
            }}
          >
            <Container maxWidth="lg" sx={{ height: '100%', display: 'flex', flexDirection: 'column', pt: 2, pb: 2 }}>
              {mainContent}
            </Container>
            <Slide
              direction="up"
              in={pipelineRunning && !pipelineCardMinimized}
              mountOnEnter
              unmountOnExit
              timeout={{ enter: 260, exit: 220 }}
            >
              <Box
                sx={{
                  position: 'absolute',
                  left: CONTENT_SIDE_GUTTER,
                  right: FAB_SAFE_GUTTER,
                  bottom: 20,
                  zIndex: 1250,
                  pointerEvents: 'none',
                }}
              >
                <Box
                  sx={{
                    width: 'min(980px, 100%)',
                    mx: 'auto',
                  }}
                >
                  <Paper
                    sx={{
                      width: '100%',
                      maxWidth: '100%',
                      p: 1.5,
                      boxShadow: 8,
                      pointerEvents: 'auto',
                    }}
                  >
                    <Stack spacing={1}>
                      <Stack direction="row" alignItems="flex-start" justifyContent="space-between">
                        <Box>
                          <Typography variant="caption" sx={{ opacity: 0.7 }}>
                            当前工作状态
                          </Typography>
                          <Typography variant="body2" fontWeight={600}>
                            {currentStage === 'idle' ? status : `${getStageLabel(currentStage)} · ${status}`}
                          </Typography>
                        </Box>
                        <Stack direction="row" spacing={1} alignItems="center">
                          <Collapse in={!pipelineCardCollapsed} orientation="horizontal" timeout={180}>
                            <Typography variant="caption" sx={{ opacity: 0.75, whiteSpace: 'nowrap' }}>
                              总进度 {Math.round(overallProgress * 100)}%
                            </Typography>
                          </Collapse>
                          <IconButton
                            size="small"
                            onClick={() => setPipelineCardMinimized(true)}
                            sx={{ mt: -0.25 }}
                          >
                            <MsIcon name="remove" size={18} />
                          </IconButton>
                          <IconButton
                            size="small"
                            onClick={() => setPipelineCardCollapsed((prev) => !prev)}
                            sx={{ mt: -0.25 }}
                          >
                            {pipelineCardCollapsed ? <MsIcon name="expand_less" size={18} /> : <MsIcon name="expand_more" size={18} />}
                          </IconButton>
                        </Stack>
                      </Stack>

                      <Box
                        sx={{
                          display: 'grid',
                          gap: 1,
                          gridTemplateColumns: `repeat(${activeProgressStages.length}, minmax(0, 1fr))`,
                        }}
                      >
                        {activeProgressStages.map((stage) => (
                          <Box key={stage}>
                            <Collapse in={!pipelineCardCollapsed} timeout={180}>
                              <Stack direction="row" justifyContent="space-between">
                                <Typography variant="caption" sx={{ opacity: 0.7 }}>
                                  {STAGE_LABEL[stage]}
                                </Typography>
                                <Typography variant="caption" sx={{ opacity: 0.7 }}>
                                  {Math.round((progress[stage] ?? 0) * 100)}%
                                </Typography>
                              </Stack>
                            </Collapse>
                            <LinearProgress
                              variant="determinate"
                              value={(progress[stage] ?? 0) * 100}
                              sx={{
                                height: pipelineCardCollapsed ? 6 : 8,
                                borderRadius: 6,
                                mt: pipelineCardCollapsed ? 0 : 0.25,
                              }}
                            />
                          </Box>
                        ))}
                      </Box>
                    </Stack>
                  </Paper>
                </Box>
              </Box>
            </Slide>
            <Slide
              direction="up"
              in={trainDonePromptOpen}
              mountOnEnter
              unmountOnExit
              timeout={{ enter: 260, exit: 220 }}
            >
              <Box
                sx={{
                  position: 'absolute',
                  left: CONTENT_SIDE_GUTTER,
                  right: FAB_SAFE_GUTTER,
                  bottom: 20,
                  zIndex: 1255,
                  pointerEvents: 'none',
                }}
              >
                <Box sx={{ width: 'min(520px, 100%)', mx: 'auto' }}>
                  <Paper
                    sx={{
                      p: 1.5,
                      boxShadow: 8,
                      pointerEvents: 'auto',
                    }}
                  >
                    <Stack spacing={1.25}>
                      <Stack direction="row" alignItems="flex-start" justifyContent="space-between">
                        <Box>
                          <Typography variant="caption" sx={{ opacity: 0.7 }}>
                            任务提示
                          </Typography>
                          <Typography variant="body2" fontWeight={600}>
                            训练完成，是否前往试听页面？
                          </Typography>
                        </Box>
                        <IconButton size="small" onClick={() => setTrainDonePromptOpen(false)} sx={{ mt: -0.25 }}>
                          <MsIcon name="close" size={18} />
                        </IconButton>
                      </Stack>
                      <Stack direction="row" spacing={1}>
                        <Button
                          variant="contained"
                          startIcon={<MsIcon name="record_voice_over" size={18} />}
                          onClick={() => {
                            setPage('preview')
                            setTrainDonePromptOpen(false)
                          }}
                        >
                          前往试听
                        </Button>
                        <Button variant="outlined" onClick={() => setTrainDonePromptOpen(false)}>
                          关闭
                        </Button>
                      </Stack>
                    </Stack>
                  </Paper>
                </Box>
              </Box>
            </Slide>
          </Box>
        </Box>

        <Tooltip
          title={
            pipelineRunning
              ? '中止训练'
              : trainingFabBlockedByBackgroundTask
                ? '环境安装或模型下载中'
                : '开始训练语音包'
          }
          placement="left"
          arrow
        >
          <Fab
            color="secondary"
            sx={{
              position: 'fixed',
              right: 24,
              bottom: 24,
              boxShadow: theme.shadows[6],
              '&:hover': {
                boxShadow: theme.shadows[8],
              },
              '&:active': {
                boxShadow: theme.shadows[12],
              },
            }}
            onClick={pipelineRunning ? abortPipeline : startPipeline}
            disabled={!pipelineRunning && (previewBusy || trainingFabBlockedByBackgroundTask)}
          >
            <MsIcon name={pipelineRunning ? 'stop' : 'play_arrow'} size={24} fill={1} />
          </Fab>
        </Tooltip>

        <Box
          component="audio"
          ref={previewAudioRef}
          src={previewAudioSrc}
          preload="metadata"
          onLoadedMetadata={(event: SyntheticEvent<HTMLAudioElement>) => {
            const audio = event.currentTarget
            setPreviewDuration(Number.isFinite(audio.duration) ? audio.duration : 0)
            setPreviewCurrentTime(audio.currentTime || 0)
          }}
          onDurationChange={(event: SyntheticEvent<HTMLAudioElement>) => {
            const audio = event.currentTarget
            setPreviewDuration(Number.isFinite(audio.duration) ? audio.duration : 0)
          }}
          onTimeUpdate={(event: SyntheticEvent<HTMLAudioElement>) => {
            setPreviewCurrentTime(event.currentTarget.currentTime || 0)
          }}
          onPlay={() => setPreviewPlaying(true)}
          onPause={() => setPreviewPlaying(false)}
          onEnded={(event: SyntheticEvent<HTMLAudioElement>) => {
            setPreviewPlaying(false)
            setPreviewCurrentTime(event.currentTarget.duration || 0)
          }}
          sx={{ display: 'none' }}
        />

        <Popover
          open={textContextMenu.open && contextMenuMode === 'custom'}
          onClose={closeTextContextMenu}
          disableAutoFocus
          disableEnforceFocus
          disableRestoreFocus
          anchorReference="anchorPosition"
          anchorPosition={{
            top: textContextMenu.y,
            left: textContextMenu.x,
          }}
          transformOrigin={{ vertical: 'top', horizontal: 'left' }}
          PaperProps={{
            onMouseDown: (event: ReactMouseEvent<HTMLDivElement>) => {
              event.preventDefault()
            },
            sx: {
              p: 0.5,
              borderRadius: 1,
              boxShadow: 8,
              bgcolor: 'background.paper',
            },
          }}
        >
          <Stack direction="row" spacing={0.25}>
            <IconButton
              size="small"
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => void runTextContextAction('selectAll')}
              disabled={!textContextCaps.canSelectAll}
              sx={{
                width: 28,
                height: 28,
                color: 'text.secondary',
                '&:hover': {
                  bgcolor: 'action.hover',
                  color: 'text.primary',
                },
              }}
            >
              <MsIcon name="select_all" size={18} />
            </IconButton>
            <IconButton
              size="small"
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => void runTextContextAction('cut')}
              disabled={!textContextCaps.canCut}
              sx={{
                width: 28,
                height: 28,
                color: 'text.secondary',
                '&:hover': {
                  bgcolor: 'action.hover',
                  color: 'text.primary',
                },
              }}
            >
              <MsIcon name="content_cut" size={18} />
            </IconButton>
            <IconButton
              size="small"
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => void runTextContextAction('copy')}
              disabled={!textContextCaps.canCopy}
              sx={{
                width: 28,
                height: 28,
                color: 'text.secondary',
                '&:hover': {
                  bgcolor: 'action.hover',
                  color: 'text.primary',
                },
              }}
            >
              <MsIcon name="content_copy" size={18} />
            </IconButton>
            <IconButton
              size="small"
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => void runTextContextAction('paste')}
              disabled={!textContextCaps.canPaste}
              sx={{
                width: 28,
                height: 28,
                color: 'text.secondary',
                '&:hover': {
                  bgcolor: 'action.hover',
                  color: 'text.primary',
                },
              }}
            >
              <MsIcon name="content_paste" size={18} />
            </IconButton>
          </Stack>
        </Popover>

        <Dialog
          open={distillPresetDialogOpen}
          onClose={(_event, reason) => {
            if ((reason === 'backdropClick' || reason === 'escapeKeyDown') && distillPresetBusy) return
            closeDistillPresetDialog()
          }}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle>添加预设文件</DialogTitle>
          <DialogContent dividers>
            <Stack spacing={1.5}>
              <Typography variant="body2" sx={{ opacity: 0.78 }}>
                选择要添加的内置文本预设版本。推荐先使用 10 万字版本，通常更适合作为蒸馏训练的默认起点。
              </Typography>
              <List disablePadding>
                {DISTILL_TEXT_PRESET_OPTIONS.map((option, index) => {
                  const selected = option.key === selectedDistillPreset?.key
                  return (
                    <Box key={option.key}>
                      {index > 0 && <Box sx={{ height: 12 }} />}
                      <ListItemButton
                        onClick={() => setSelectedDistillPresetKey(option.key)}
                        alignItems="flex-start"
                        sx={{
                          gap: 1,
                          px: 0,
                          py: 0,
                          borderRadius: 1,
                          '&:hover': {
                            bgcolor: 'action.hover',
                          },
                          '&:active': {
                            bgcolor: 'action.selected',
                          },
                          '&.Mui-focusVisible': {
                            bgcolor: 'action.hover',
                          },
                        }}
                      >
                        <Radio
                          checked={selected}
                          tabIndex={-1}
                          disableRipple
                          sx={{
                            p: 0.5,
                            mt: 0.15,
                            mr: 0.25,
                          }}
                        />
                        <Stack spacing={0.6} sx={{ minWidth: 0, flex: 1 }}>
                          <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap">
                            <Typography variant="subtitle2" fontWeight={600}>
                              {option.title}
                            </Typography>
                            <Chip size="small" label={option.charCountLabel} />
                            {option.recommended && <Chip size="small" color="primary" label="推荐" />}
                          </Stack>
                          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                            {option.description}
                          </Typography>
                          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                            预期训练效果：{option.expectedEffect}
                          </Typography>
                        </Stack>
                      </ListItemButton>
                    </Box>
                  )
                })}
              </List>
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2 }}>
            <Button onClick={closeDistillPresetDialog} disabled={distillPresetBusy}>
              取消
            </Button>
            <Button
              variant="contained"
              onClick={() => {
                void addSelectedDistillPreset()
              }}
              disabled={distillPresetBusy || !selectedDistillPreset}
              startIcon={distillPresetBusy ? <CircularProgress size={16} color="inherit" /> : <MsIcon name="library_add" size={18} />}
            >
              添加到文本来源
            </Button>
          </DialogActions>
        </Dialog>

        <LegalDocumentDialog
          open={aboutDialog === 'openSource'}
          title="开源许可证"
          content={openSourceLicensesText}
          onClose={() => setAboutDialog(null)}
          monospace
        />

        <LegalDocumentDialog
          open={aboutDialog === 'privacy'}
          title="隐私政策"
          content={privacyPolicyText}
          onClose={() => setAboutDialog(null)}
        />

        <Dialog
          open={resumeRebuildConfirmOpen}
          onClose={(_event, reason) => {
            if (reason === 'backdropClick' || reason === 'escapeKeyDown') return
            cancelResumeRebuildConfirm()
          }}
          maxWidth="sm"
          fullWidth
          disableEscapeKeyDown
        >
          <DialogTitle>旧项目需要重新准备训练素材</DialogTitle>
          <DialogContent dividers>
            <Stack spacing={2}>
              <Typography variant="body2" sx={{ opacity: 0.86 }}>
                当前旧项目的训练素材不完整或与项目记录不一致，继续开始训练前需要先按项目配置重新生成或补齐素材。
              </Typography>
              {pendingResumeProjectStatus && (
                <Alert severity="warning">
                  <Stack spacing={1}>
                    <Typography variant="body2">
                      {pendingResumeProjectStatus.material_status || pendingResumeProjectStatus.message}
                    </Typography>
                    <Stack direction="row" spacing={0.75} useFlexGap flexWrap="wrap">
                      <Chip
                        size="small"
                        label={`模式：${TRAINING_MODE_LABELS[String(pendingResumeProjectStatus.mode || '')] || pendingResumeProjectStatus.mode || '未知'}`}
                      />
                      <Chip size="small" label={`metadata：${pendingResumeProjectStatus.metadata_count ?? 0} 条`} />
                      <Chip size="small" label={`可用音频：${pendingResumeProjectStatus.existing_count ?? 0} 条`} />
                      <Chip size="small" color="warning" label={`缺失音频：${pendingResumeProjectStatus.missing_count ?? 0} 条`} />
                      {pendingResumeProjectStatus.input_audio_count !== undefined && pendingResumeProjectStatus.input_audio_count > 0 && (
                        <Chip
                          size="small"
                          color={pendingResumeProjectStatus.input_audio_missing_count ? 'warning' : 'default'}
                          label={`原始音频：${pendingResumeProjectStatus.input_audio_available_count ?? 0}/${pendingResumeProjectStatus.input_audio_count}`}
                        />
                      )}
                      {pendingResumeProjectStatus.metadata_inconsistent && (
                        <Chip size="small" color="warning" label="文本记录不一致" />
                      )}
                    </Stack>
                  </Stack>
                </Alert>
              )}
              <Typography variant="caption" sx={{ opacity: 0.72, wordBreak: 'break-all' }}>
                项目目录：{pendingResumeProjectDir || resumeProjectDir || '未选择'}
              </Typography>
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
            <Button variant="outlined" onClick={cancelResumeRebuildConfirm}>
              取消
            </Button>
            <Button variant="contained" onClick={confirmResumeRebuild}>
              继续并重新准备素材
            </Button>
          </DialogActions>
        </Dialog>

        <Dialog
          open={gsviModeIntroOpen}
          onClose={(_event, reason) => {
            if (reason === 'backdropClick' || reason === 'escapeKeyDown') return
            closeGsviModeIntro()
          }}
          maxWidth="md"
          fullWidth
          disableEscapeKeyDown
        >
          <DialogTitle>GPT-SoVITS 蒸馏模式说明</DialogTitle>
          <DialogContent dividers>
            <Stack spacing={2}>
              <Typography variant="body2" sx={{ opacity: 0.85 }}>
                该模式会调用你本机上的 GSVI / GPT-SoVITS 整合包生成蒸馏语料，然后继续在训练器里完成 Piper 训练与 KIGTTS 语音包导出。
                它不会替你训练或导出 GPT-SoVITS 模型本体。
              </Typography>

              <Box>
                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  获取索引
                </Typography>
                <Typography variant="body2" sx={{ opacity: 0.82 }}>
                  使用前请先准备兼容的 GSVI / GPT-SoVITS 整合包。索引页中包含整合包获取入口、模型资源和相关说明。
                </Typography>
                <Button
                  variant="text"
                  size="small"
                  sx={{ mt: 0.75, px: 0, justifyContent: 'flex-start' }}
                  startIcon={<MsIcon name="open_in_new" size={18} />}
                  onClick={() => {
                    void openGsviGuide()
                  }}
                >
                  {GSVI_GUIDE_URL}
                </Button>
              </Box>

              <Alert severity="info">
                了解完索引页和使用说明后，再继续配置根目录、模型与文本来源即可。若你已经熟悉这一流程，可以勾选下方选项，后续切换到该模式时不再提醒。
              </Alert>

              <FormControlLabel
                control={
                  <Checkbox
                    checked={gsviModeIntroSkipChecked}
                    onChange={(event) => setGsviModeIntroSkipChecked(event.target.checked)}
                  />
                }
                label="下次切换到该模式时不再提醒"
              />
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
            <Button
              variant="outlined"
              onClick={() => {
                void openGsviGuide()
              }}
            >
              打开索引页
            </Button>
            <Button variant="contained" onClick={closeGsviModeIntro}>
              我知道了
            </Button>
          </DialogActions>
        </Dialog>

        <Dialog
          open={gsviDisclaimerOpen}
          onClose={(_event, reason) => {
            if (reason === 'backdropClick' || reason === 'escapeKeyDown') return
            cancelPendingDistillStart(true)
          }}
          maxWidth="md"
          fullWidth
          disableEscapeKeyDown
        >
          <DialogTitle>GSVI / GPT-SoVITS 蒸馏模式使用声明</DialogTitle>
          <DialogContent dividers>
            <Stack spacing={2}>
              <Typography variant="body2" sx={{ opacity: 0.85 }}>
                当前模式会调用你本机上的 GSVI / GPT-SoVITS 整合包作为教师模型生成蒸馏语料，然后继续在本训练器里完成 Piper 训练和语音包导出。
                它不会替你导出 GPT-SoVITS 模型，也不会替你处理作品发布时的署名与使用责任。
              </Typography>

              <Box>
                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  获取索引
                </Typography>
                <Typography variant="body2" sx={{ opacity: 0.82 }}>
                  如果你还没有准备好兼容的整合包，可以先查看 GSVI 获取索引页。页面内包含整合包入口、模型资源和相关说明。
                </Typography>
                <Button
                  variant="text"
                  size="small"
                  sx={{ mt: 0.75, px: 0, justifyContent: 'flex-start' }}
                  startIcon={<MsIcon name="open_in_new" size={18} />}
                  onClick={() => {
                    void openGsviGuide()
                  }}
                >
                  {GSVI_GUIDE_URL}
                </Button>
              </Box>

              <Box>
                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  署名提醒
                </Typography>
                <Typography variant="body2" sx={{ opacity: 0.82 }}>
                  如果你后续公开发布基于该蒸馏流程生成的音频、作品或演示内容，需要根据相关项目与模型要求完整署名贡献者。下一步会要求你手动输入关键署名信息进行确认。
                </Typography>
              </Box>

              <Alert severity="warning">
                <Typography variant="body2">
                  你需要自行确认外部整合包、模型文件和生成内容的来源、授权范围与用途合规性。训练器作者无法控制你导入的模型、文本和导出的声音内容。
                </Typography>
              </Alert>
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
            <Button variant="outlined" onClick={() => cancelPendingDistillStart(true)}>
              取消
            </Button>
            <Button variant="contained" onClick={confirmGsviDisclaimer}>
              继续确认
            </Button>
          </DialogActions>
        </Dialog>

        <Dialog
          open={gsviAttributionOpen}
          onClose={(_event, reason) => {
            if (reason === 'backdropClick' || reason === 'escapeKeyDown') return
            cancelPendingDistillStart(true)
          }}
          maxWidth="sm"
          fullWidth
          disableEscapeKeyDown
        >
          <DialogTitle>开始蒸馏前确认署名</DialogTitle>
          <DialogContent dividers>
            <Stack spacing={2}>
              <Typography variant="body2" sx={{ opacity: 0.82 }}>
                请输入下列名字，确认你知道在对外发布相关内容时需要完整注明这些贡献者。训练器不会自动把这些内容写进作品简介。
              </Typography>

              <TextField
                label="GPT-SoVITS 开发者"
                value={gsviAttribution.gsvAuthor}
                onChange={(event) => {
                  const value = event.target.value
                  setGsviAttribution((prev) => ({ ...prev, gsvAuthor: value }))
                  if (gsviAttributionError) setGsviAttributionError('')
                }}
                placeholder={`请输入：${GSVI_REQUIRED_NAMES.gsvAuthor}`}
                size="small"
                fullWidth
                error={gsviFieldMismatch.gsvAuthor}
                helperText={gsviFieldMismatch.gsvAuthor ? `需输入：${GSVI_REQUIRED_NAMES.gsvAuthor}` : undefined}
              />
              <TextField
                label="模型训练者"
                value={gsviAttribution.gsvTrainer}
                onChange={(event) => {
                  const value = event.target.value
                  setGsviAttribution((prev) => ({ ...prev, gsvTrainer: value }))
                  if (gsviAttributionError) setGsviAttributionError('')
                }}
                placeholder={`请输入：${GSVI_REQUIRED_NAMES.gsvTrainer}`}
                size="small"
                fullWidth
                error={gsviFieldMismatch.gsvTrainer}
                helperText={gsviFieldMismatch.gsvTrainer ? `需输入：${GSVI_REQUIRED_NAMES.gsvTrainer}` : undefined}
              />
              <TextField
                label="模型训练者补充"
                value={gsviAttribution.gsvTrainer2}
                onChange={(event) => {
                  const value = event.target.value
                  setGsviAttribution((prev) => ({ ...prev, gsvTrainer2: value }))
                  if (gsviAttributionError) setGsviAttributionError('')
                }}
                placeholder={`请输入：${GSVI_REQUIRED_NAMES.gsvTrainer2}`}
                size="small"
                fullWidth
                error={gsviFieldMismatch.gsvTrainer2}
                helperText={gsviFieldMismatch.gsvTrainer2 ? `需输入：${GSVI_REQUIRED_NAMES.gsvTrainer2}` : undefined}
              />
              <TextField
                label="GSVI 推理特化包适配 / 整理"
                value={gsviAttribution.gsviPacker}
                onChange={(event) => {
                  const value = event.target.value
                  setGsviAttribution((prev) => ({ ...prev, gsviPacker: value }))
                  if (gsviAttributionError) setGsviAttributionError('')
                }}
                placeholder={`请输入：${GSVI_REQUIRED_NAMES.gsviPacker}`}
                size="small"
                fullWidth
                error={gsviFieldMismatch.gsviPacker}
                helperText={gsviFieldMismatch.gsviPacker ? `需输入：${GSVI_REQUIRED_NAMES.gsviPacker}` : undefined}
              />

              {gsviAttributionError && <Alert severity="error">{gsviAttributionError}</Alert>}

              <FormControlLabel
                control={
                  <Checkbox
                    checked={gsviSkipPromptChecked}
                    onChange={(event) => setGsviSkipPromptChecked(event.target.checked)}
                  />
                }
                label="我已确认，下次不再提示"
              />
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
            <Button variant="outlined" onClick={() => cancelPendingDistillStart(true)}>
              取消
            </Button>
            <Button variant="contained" onClick={submitGsviAttribution}>
              我保证完整注明
            </Button>
          </DialogActions>
        </Dialog>

        <Dialog open={avatarDialogOpen} onClose={() => setAvatarDialogOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>裁剪头像</DialogTitle>
          <DialogContent>
            <Box
              sx={{
                position: 'relative',
                width: '100%',
                height: 320,
                bgcolor: '#121212',
                borderRadius: 2,
                overflow: 'hidden',
              }}
            >
              {avatarSource ? (
                <Cropper
                  image={avatarSource}
                  crop={avatarCrop}
                  zoom={avatarZoom}
                  aspect={1}
                  onCropChange={setAvatarCrop}
                  onZoomChange={setAvatarZoom}
                  onCropComplete={(_, croppedPixels) => setAvatarCropPixels(croppedPixels)}
                />
              ) : (
                <Stack alignItems="center" justifyContent="center" sx={{ height: '100%' }}>
                  <Typography variant="body2" sx={{ opacity: 0.7 }}>
                    未选择图像
                  </Typography>
                </Stack>
              )}
            </Box>
            <Stack direction="row" spacing={2} alignItems="center" sx={{ mt: 2 }}>
              <Tooltip title="缩放" arrow>
                <Box sx={{ minWidth: 20, display: 'flex', justifyContent: 'center', opacity: 0.8 }}>
                  <MsIcon name="search" size={20} fill={0} />
                </Box>
              </Tooltip>
              <Slider
                value={avatarZoom}
                min={1}
                max={3}
                step={0.05}
                onChange={(_, value) => setAvatarZoom(value as number)}
              />
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
            <Tooltip title="取消" arrow>
              <IconButton
                size="small"
                onClick={() => setAvatarDialogOpen(false)}
                sx={{
                  width: 28,
                  height: 28,
                  color: 'text.secondary',
                  '&:hover': {
                    bgcolor: 'action.hover',
                    color: 'text.primary',
                  },
                }}
              >
                <MsIcon name="close" size={18} fill={0} />
              </IconButton>
            </Tooltip>
            <Tooltip title="应用" arrow>
              <IconButton
                size="small"
                onClick={applyAvatarCrop}
                sx={{
                  width: 28,
                  height: 28,
                  color: 'text.secondary',
                  '&:hover': {
                    bgcolor: 'action.hover',
                    color: 'text.primary',
                  },
                }}
              >
                <MsIcon name="check" size={18} fill={0} />
              </IconButton>
            </Tooltip>
          </DialogActions>
        </Dialog>

        <Snackbar
          open={toast.open}
          autoHideDuration={4000}
          onClose={closeToast}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert
            onClose={closeToast}
            severity={toast.severity}
            variant="filled"
            sx={{
              width: '100%',
              boxShadow: theme.shadows[6],
              color: '#fff',
              ...(toast.severity === 'info' || toast.severity === 'success'
                ? { bgcolor: theme.palette.primary.main }
                : {}),
            }}
          >
            {toast.message}
          </Alert>
        </Snackbar>
      </Box>
    </ThemeProvider>
  )
}

export default App
