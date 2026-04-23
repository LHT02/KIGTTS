export {}

declare global {
  type TrainingMode = 'piper' | 'gsv_distill' | 'voxcpm_distill'

  type PipelineStage =
    | 'idle'
    | 'runtime'
    | 'collect'
    | 'distill'
    | 'synth'
    | 'preprocess'
    | 'vad'
    | 'asr'
    | 'train'
    | 'export'
    | 'preview'

  type DialogFilter = {
    name: string
    extensions: string[]
  }

  type OpenFilesOptions = {
    title?: string
    filters?: DialogFilter[]
    properties?: string[]
  }

  type DistillTextSource = {
    kind: 'text_file' | 'project_dir'
    path: string
  }

  type GsvEmotion = {
    name: string
    prompt_text: string
    ref_audio_path: string
  }

  type GsvLanguage = {
    emotions: GsvEmotion[]
  }

  type GsvSpeaker = {
    languages: Record<string, GsvLanguage>
  }

  type GsvVersion = {
    speakers: Record<string, GsvSpeaker>
  }

  type GsvModelCatalog = {
    root: string
    versions: Record<string, GsvVersion>
  }

  type DistillOptions = {
    gsv_root: string
    version: string
    speaker: string
    prompt_lang: string
    emotion: string
    device: 'cpu' | 'cuda'
    text_lang: string
    text_split_method: string
    speed_factor: number
    temperature: number
    batch_size: number
    seed: number
    top_k: number
    top_p: number
    batch_threshold: number
    split_bucket: boolean
    fragment_interval: number
    parallel_infer: boolean
    repetition_penalty: number
    sample_steps: number
    if_sr: boolean
    text_sources: DistillTextSource[]
  }

  type VoxCpmDistillOptions = {
    device: 'cpu' | 'cuda'
    allow_cpu_fallback: boolean
    voice_description: string
    reference_audio: string
    cfg_value: number
    inference_timesteps: number
    min_len: number
    max_len: number
    normalize: boolean
    denoise: boolean
    retry_badcase: boolean
    retry_badcase_max_times: number
    retry_badcase_ratio_threshold: number
    text_sources: DistillTextSource[]
  }

  type PiperCudaRuntimeStatus = {
    ok: boolean
    available: boolean
    status: 'missing' | 'ready' | 'error'
    message: string
    runtime_root: string
    env_path: string
    python_path: string
    micromamba_path: string
    bundled_micromamba_path?: string
    requirements_path?: string
    piper_train_wheel?: string
    local_wheel_dirs?: string[]
    source?: string
    installed_with?: string
    torch_version?: string
    torch_cuda_version?: string | null
    torchaudio_version?: string
    pytorch_lightning_version?: string
    piper_train_path?: string
    cuda_available?: boolean
    nvidia_smi_path?: string
    driver_version?: string
    gpu_name?: string
    gpu_memory?: string
  }

  type VoxCpmRuntimeStatus = {
    ok: boolean
    available: boolean
    status: 'missing' | 'ready' | 'error'
    message: string
    runtime_root: string
    env_path: string
    python_path: string
    micromamba_path: string
    bundled_micromamba_path?: string
    source?: string
    installed_with?: string
    torch_version?: string
    torch_cuda_version?: string | null
    voxcpm_version?: string
    modelscope_version?: string
    cuda_available?: boolean
    nvidia_smi_path?: string
    driver_version?: string
    gpu_name?: string
    gpu_memory?: string
  }

  type VoxCpmModelStatus = {
    ok: boolean
    main_available: boolean
    denoiser_available: boolean
    message: string
    model_root: string
    main_model_dir: string
    denoiser_model_dir: string
    main_repo: string
    denoiser_repo: string
  }

  type BackendResponsePayload = {
    ok?: boolean
    started?: boolean
    message?: string
    [key: string]: unknown
  }

  type BackendResponseEvent = {
    type: 'response'
    id: string
    payload: BackendResponsePayload
  }

  type BackendDefaultsEvent = {
    type: 'defaults'
    id: string
    payload: Record<string, string>
  }

  type BackendProgressEvent = {
    type: 'progress'
    id: string
    stage: PipelineStage
    value: number
    message: string
  }

  type BackendDoneEvent = {
    type: 'done'
    id: string
    payload: {
      manifest_path?: string | null
      voicepack_path?: string | null
      preview_path?: string | null
      training_log?: string | null
    }
  }

  type BackendPreviewDoneEvent = {
    type: 'preview_done'
    id: string
    payload: {
      audio_path?: string | null
    }
  }

  type BackendErrorEvent = {
    type: 'error'
    id: string
    message: string
    traceback?: string
    raw?: string
  }

  type BackendLogEvent = {
    type: 'log'
    id: string
    message: string
  }

  type BackendEvent =
    | BackendResponseEvent
    | BackendDefaultsEvent
    | BackendProgressEvent
    | BackendDoneEvent
    | BackendPreviewDoneEvent
    | BackendErrorEvent
    | BackendLogEvent

  interface Window {
    backend?: {
      send: (msg: unknown) => void
      restart?: () => void
      onEvent: (callback: (evt: BackendEvent) => void) => (() => void) | void
    }
    dialogs?: {
      openFiles: (opts?: OpenFilesOptions) => Promise<string[]>
      openDir: () => Promise<string>
      openFile: (opts?: { filters?: DialogFilter[] }) => Promise<string>
      saveFile: (opts?: {
        title?: string
        defaultPath?: string
        filters?: DialogFilter[]
      }) => Promise<string>
    }
    windowControls?: {
      minimize: () => void
      toggleMaximize: () => void
      close: () => void
      setContextMenuMode?: (mode: 'custom' | 'native') => void
      isMaximized: () => Promise<boolean>
      onState: (callback: (state: { maximized: boolean }) => void) => (() => void) | void
    }
    paths?: {
      dirname: (filePath: string) => Promise<string>
      openInExplorer?: (targetPath: string) => Promise<{ ok: boolean; target?: string; message?: string }>
      openExternal?: (targetUrl: string) => Promise<{ ok: boolean; target?: string; message?: string }>
    }
    project?: {
      openOutputDir: (outputDir: string) => Promise<{ ok: boolean; target?: string; message?: string }>
      clearWorkCache: (outputDir: string) => Promise<{ ok: boolean; cleared?: boolean; path?: string; message?: string }>
      ensureDefaultOutputDir: () => Promise<{ ok: boolean; path?: string; message?: string }>
    }
    fsBridge?: {
      saveImage: (dataUrl: string) => Promise<string>
      readImage: (filePath: string) => Promise<string>
      saveDroppedFile: (name: string, data: ArrayBuffer) => Promise<string>
      copyFile: (src: string, dst: string) => Promise<boolean>
      writeTextFile: (filePath: string, text: string) => Promise<boolean>
      getPathForFile?: (file: File) => string
    }
    clipboardBridge?: {
      readText: () => string
      writeText: (text: string) => void
    }
  }
}
