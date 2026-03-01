export {}

declare global {
  interface Window {
    backend?: {
      send: (msg: unknown) => void
      restart?: () => void
      onEvent: (callback: (evt: any) => void) => (() => void) | void
    }
    dialogs?: {
      openFiles: () => Promise<string[]>
      openDir: () => Promise<string>
      openFile: (opts?: { filters?: { name: string; extensions: string[] }[] }) => Promise<string>
      saveFile: (opts?: {
        title?: string
        defaultPath?: string
        filters?: { name: string; extensions: string[] }[]
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
    }
    clipboardBridge?: {
      readText: () => string
      writeText: (text: string) => void
    }
  }
}
