import { useEffect, useMemo, useRef, useState, type DragEvent as ReactDragEvent, type MouseEvent as ReactMouseEvent, type SyntheticEvent } from 'react'
import {
  Box,
  Button,
  ButtonBase,
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
  MenuItem,
  Popover,
  Paper,
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

type ProgressMap = Record<string, number>

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
const PIPELINE_STAGES = ['preprocess', 'vad', 'asr', 'train', 'export'] as const
type PipelineStage = (typeof PIPELINE_STAGES)[number]
type AppPage = 'prep' | 'settings' | 'preview' | 'logs'
const STAGE_LABEL: Record<PipelineStage, string> = {
  preprocess: '预处理',
  vad: '切分',
  asr: '识别',
  train: '训练',
  export: '导出',
}
const NAV_ITEMS: Array<{ key: AppPage; label: string; icon: string }> = [
  { key: 'prep', label: '训练准备', icon: 'folder' },
  { key: 'preview', label: '语音包试听', icon: 'record_voice_over' },
  { key: 'settings', label: '训练设置', icon: 'tune' },
  { key: 'logs', label: '日志', icon: 'article' },
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

const getDataTransfer = (event: ReactDragEvent | DragEvent) => {
  return (event as any).dataTransfer || (event as any).nativeEvent?.dataTransfer || null
}

const decodeFileUrl = (value: string) => {
  try {
    const cleaned = value.replace(/^file:\/*/i, '')
    return decodeURI(cleaned).replace(/\//g, '\\')
  } catch {
    return value
  }
}

const extractDroppedPaths = (event: ReactDragEvent | DragEvent) => {
  const dt = getDataTransfer(event)
  if (!dt) return []

  const fromFiles = Array.from(dt.files || [])
    .map((file) => (file as File & { path?: string }).path)
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
    .map((file) => (file as File & { path?: string }).path)
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

const readAllDirectoryEntries = (directory: any): Promise<any[]> =>
  new Promise((resolve) => {
    const reader = directory.createReader()
    const entries: any[] = []
    const readBatch = () => {
      reader.readEntries((batch: any[]) => {
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

const getFilesFromEntries = async (entry: any): Promise<File[]> => {
  if (!entry) return []
  if (entry.isFile) {
    return new Promise((resolve) => {
      entry.file((file: File) => resolve([file]), () => resolve([]))
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
    const entry = (item as any).webkitGetAsEntry?.()
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
  const [connected, setConnected] = useState(false)
  const [status, setStatus] = useState('待命')
  const [logs, setLogs] = useState<string[]>([])

  const [outputDir, setOutputDir] = useState('')
  const [audioFiles, setAudioFiles] = useState<string[]>([])
  const [quality, setQuality] = useState<'A' | 'B'>('A')
  const [denoise, setDenoise] = useState(false)
  const [sampleRate, setSampleRate] = useState('22050')
  const [asrModel, setAsrModel] = useState('')
  const [baseCkpt, setBaseCkpt] = useState('')
  const [useEspeak, setUseEspeak] = useState(false)
  const [piperConfig, setPiperConfig] = useState('')
  const [device, setDevice] = useState<'cpu' | 'cuda'>('cpu')
  const [voicepackName, setVoicepackName] = useState('未命名')
  const [voicepackRemark, setVoicepackRemark] = useState('')
  const [voicepackAvatar, setVoicepackAvatar] = useState('')
  const [voicepackAvatarPreview, setVoicepackAvatarPreview] = useState('')
  const [previewVoicepack, setPreviewVoicepack] = useState('')
  const [previewText, setPreviewText] = useState('你好，这是语音包试听。')
  const [previewAudioPath, setPreviewAudioPath] = useState('')
  const [previewAudioRev, setPreviewAudioRev] = useState(0)
  const [previewBusy, setPreviewBusy] = useState(false)
  const [previewPlaying, setPreviewPlaying] = useState(false)
  const [previewDuration, setPreviewDuration] = useState(0)
  const [previewCurrentTime, setPreviewCurrentTime] = useState(0)
  const [trainDonePromptOpen, setTrainDonePromptOpen] = useState(false)

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
  const [avatarDragActive, setAvatarDragActive] = useState(false)

  const [progress, setProgress] = useState<ProgressMap>({
    preprocess: 0,
    vad: 0,
    asr: 0,
    train: 0,
    export: 0,
  })
  const [pipelineRunning, setPipelineRunning] = useState(false)
  const [pipelineCardCollapsed, setPipelineCardCollapsed] = useState(false)
  const [pipelineCardMinimized, setPipelineCardMinimized] = useState(false)
  const [currentStage, setCurrentStage] = useState<PipelineStage | 'idle'>('idle')
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

  const overallProgress = useMemo(() => {
    const total = PIPELINE_STAGES.reduce((acc, stage) => acc + (progress[stage] ?? 0), 0)
    return total / PIPELINE_STAGES.length
  }, [progress])
  const previewAudioSrc = useMemo(() => {
    if (!previewAudioPath) return ''
    const base = toFileUrl(previewAudioPath)
    const sep = base.includes('?') ? '&' : '?'
    return `${base}${sep}v=${previewAudioRev}`
  }, [previewAudioPath, previewAudioRev])
  const hasPreviewAudio = Boolean(previewAudioSrc)
  const textContextCaps = useMemo(
    () => getTextContextCapabilities(textContextMenu.target),
    [textContextMenu.target],
  )

  const appendLog = (text: string) => {
    setLogs((prev) => [...prev, text].slice(-400))
  }

  const showToast = (message: string, severity: ToastState['severity'] = 'info') => {
    setToast({ open: true, message, severity })
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

  const requestBackendRestart = () => {
    window.backend?.restart?.()
    setConnected(false)
    appendLog('[SYS] 已请求后端重启')
    showToast('已请求后端重启', 'info')
    setTimeout(() => {
      send('ping')
    }, 1200)
  }

  const abortPipeline = () => {
    if (!pipelineRunning) {
      return
    }
    abortingPipelineRef.current = true
    setPipelineRunning(false)
    setPipelineCardMinimized(false)
    setCurrentStage('idle')
    setStatus('中止训练中...')
    appendLog('[SYS] 已请求中止训练，正在重启后端...')
    showToast('已请求中止训练', 'warning')
    window.backend?.restart?.()
    setConnected(false)
    setTimeout(() => {
      send('ping')
    }, 1200)
  }

  useEffect(() => {
    if (!window.backend) {
      appendLog('未检测到后端桥接。')
      return
    }
    const offBackendEvent = window.backend.onEvent((evt) => {
      if (evt.type === 'response' && evt.payload?.ok) {
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
        setProgress((prev) => ({
          ...prev,
          [evt.stage]: evt.value ?? 0,
        }))
        if (PIPELINE_STAGES.includes(evt.stage as PipelineStage)) {
          const stage = evt.stage as PipelineStage
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
            setStatus(`${STAGE_LABEL[stage]}中...`)
          }
        }
        if (evt.stage === 'preview' && evt.message) {
          setStatus(evt.message)
        }
        if (evt.message) {
          appendLog(`[${evt.stage}] ${evt.message}`)
        }
        return
      }
      if (evt.type === 'error') {
        const errMsg = String(evt.message ?? '')
        const backendFatal =
          errMsg.includes('Backend exited') ||
          errMsg.includes('Backend parse error') ||
          errMsg.includes('Python not found') ||
          errMsg.includes('JSON 解析失败')
        if (abortingPipelineRef.current && backendFatal) {
          setStatus('训练已中止')
          setPipelineRunning(false)
          setPipelineCardMinimized(false)
          setCurrentStage('idle')
          appendLog('[SYS] 训练已中止')
          showToast('训练已中止', 'info')
          return
        }
        setStatus('任务出错')
        setPipelineRunning(false)
        setPipelineCardMinimized(false)
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
    send('ping')
    return () => {
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
    media.addListener(legacyListener as unknown as (this: MediaQueryList, ev: MediaQueryListEvent) => any)
    return () =>
      media.removeListener(legacyListener as unknown as (this: MediaQueryList, ev: MediaQueryListEvent) => any)
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
    setProgress({
      preprocess: 0,
      vad: 0,
      asr: 0,
      train: 0,
      export: 0,
    })
    setStatus('工作缓存已清理')
    appendLog(`[SYS] 已清理工作缓存: ${result.path ?? `${outputDir}\\work`}`)
    showToast('工作缓存已清理', 'success')
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
          return `${file.name}|path=${anyFile.path ?? ''}|rel=${anyFile.webkitRelativePath ?? ''}`
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
      `kgtts-trainer-log-${now.getFullYear()}${pad2(now.getMonth() + 1)}${pad2(now.getDate())}-` +
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

  const startPipeline = async () => {
    if (!connected) {
      setStatus('后端未连接')
      showToast('后端未连接', 'error')
      return
    }
    if (previewBusy) {
      showToast('正在生成试听，请稍后再开始训练', 'warning')
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
    if (!audioFiles.length) {
      setStatus('请先添加音频文件')
      showToast('请先添加音频文件', 'warning')
      return
    }
    const parsedSampleRate = Number(sampleRate)
    const sampleRateValue =
      Number.isFinite(parsedSampleRate) && parsedSampleRate > 0 ? parsedSampleRate : 22050
    abortingPipelineRef.current = false
    setProgress({
      preprocess: 0,
      vad: 0,
      asr: 0,
      train: 0,
      export: 0,
    })
    setPipelineRunning(true)
    setPipelineCardCollapsed(false)
    setPipelineCardMinimized(false)
    setTrainDonePromptOpen(false)
    setCurrentStage('preprocess')
    setStatus('任务启动中...')
    showToast('任务已启动', 'info')
    send('start_pipeline', {
      input_audio: audioFiles,
      output_dir: effectiveOutputDir,
      opts: {
        quality,
        denoise,
        sample_rate: sampleRateValue,
        asr_model_zip: asrModel || null,
        piper_base_checkpoint: baseCkpt || null,
        use_espeak: useEspeak,
        piper_config: piperConfig || null,
        device,
        voicepack_name: voicepackName,
        voicepack_remark: voicepackRemark,
        voicepack_avatar: voicepackAvatar || null,
      },
    })
  }

  const prepContent = (
    <Stack spacing={2}>
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
            <InputLabel>训练设备</InputLabel>
            <Select value={device} label="训练设备" onChange={(e) => setDevice(e.target.value as 'cpu' | 'cuda')}>
              <MenuItem value="cpu">CPU</MenuItem>
              <MenuItem value="cuda">GPU/CUDA</MenuItem>
            </Select>
          </FormControl>
        </Box>
      </Paper>

      <Paper sx={cardPaperSx}>
        <Stack spacing={1}>
          <Typography variant="subtitle1" fontWeight={600}>
            进度
          </Typography>
          {['preprocess', 'vad', 'asr', 'train', 'export'].map((key) => (
            <Box key={key}>
              <Stack direction="row" justifyContent="space-between">
                <Typography variant="caption" sx={{ textTransform: 'uppercase', opacity: 0.7 }}>
                  {key}
                </Typography>
                <Typography variant="caption">{Math.round((progress[key] ?? 0) * 100)}%</Typography>
              </Stack>
              <LinearProgress variant="determinate" value={(progress[key] ?? 0) * 100} sx={{ height: 8, borderRadius: 6 }} />
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

  const renderContent = (currentPage: AppPage) => {
    if (currentPage === 'prep') return prepContent
    if (currentPage === 'settings') return settingsContent
    if (currentPage === 'preview') return previewContent
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
          <Typography variant="h6" sx={{ fontWeight: 500, flexGrow: 1, fontSize: 16 }}>
            KGTTS Trainer
          </Typography>
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
                            {currentStage === 'idle' ? status : `${STAGE_LABEL[currentStage]} · ${status}`}
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
                          gridTemplateColumns: 'repeat(5, minmax(0, 1fr))',
                        }}
                      >
                        {PIPELINE_STAGES.map((stage) => (
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

        <Tooltip title={pipelineRunning ? '中止训练' : '开始训练语音包'} placement="left" arrow>
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
            disabled={!pipelineRunning && previewBusy}
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
