const { contextBridge, ipcRenderer, clipboard, webUtils } = require('electron');

contextBridge.exposeInMainWorld('backend', {
  send: (msg) => ipcRenderer.send('backend:send', msg),
  restart: () => ipcRenderer.send('backend:restart'),
  onEvent: (callback) => {
    const handler = (_, data) => callback(data);
    ipcRenderer.on('backend:event', handler);
    return () => ipcRenderer.removeListener('backend:event', handler);
  },
});

contextBridge.exposeInMainWorld('dialogs', {
  openFiles: (opts) => ipcRenderer.invoke('dialog:openFiles', opts),
  openDir: () => ipcRenderer.invoke('dialog:openDir'),
  openFile: (opts) => ipcRenderer.invoke('dialog:openFile', opts),
  saveFile: (opts) => ipcRenderer.invoke('dialog:saveFile', opts),
});

contextBridge.exposeInMainWorld('windowControls', {
  minimize: () => ipcRenderer.send('window:minimize'),
  toggleMaximize: () => ipcRenderer.send('window:toggleMaximize'),
  close: () => ipcRenderer.send('window:close'),
  setContextMenuMode: (mode) => ipcRenderer.send('context-menu:set-mode', mode),
  isMaximized: () => ipcRenderer.invoke('window:isMaximized'),
  onState: (callback) => {
    const handler = (_, data) => callback(data);
    ipcRenderer.on('window:state', handler);
    return () => ipcRenderer.removeListener('window:state', handler);
  },
});

contextBridge.exposeInMainWorld('paths', {
  dirname: (filePath) => ipcRenderer.invoke('path:dirname', filePath),
  openInExplorer: (targetPath) => ipcRenderer.invoke('path:openInExplorer', targetPath),
  openExternal: (targetUrl) => ipcRenderer.invoke('path:openExternal', targetUrl),
});

contextBridge.exposeInMainWorld('project', {
  openOutputDir: (outputDir) => ipcRenderer.invoke('project:openOutputDir', outputDir),
  clearWorkCache: (outputDir) => ipcRenderer.invoke('project:clearWorkCache', outputDir),
  ensureDefaultOutputDir: () => ipcRenderer.invoke('project:ensureDefaultOutputDir'),
});

contextBridge.exposeInMainWorld('fsBridge', {
  saveImage: (dataUrl) => ipcRenderer.invoke('fs:saveImage', { dataUrl }),
  readImage: (filePath) => ipcRenderer.invoke('fs:readImage', filePath),
  saveDroppedFile: (name, data) => ipcRenderer.invoke('fs:saveDroppedFile', { name, data }),
  copyFile: (src, dst) => ipcRenderer.invoke('fs:copyFile', { src, dst }),
  writeTextFile: (filePath, text) => ipcRenderer.invoke('fs:writeTextFile', { path: filePath, text }),
  ensureTextPresetFile: (name, text) => ipcRenderer.invoke('fs:ensureTextPresetFile', { name, text }),
  getPathForFile: (file) => {
    try {
      return webUtils.getPathForFile(file) || '';
    } catch {
      return '';
    }
  },
});

contextBridge.exposeInMainWorld('clipboardBridge', {
  readText: () => clipboard.readText(),
  writeText: (text) => clipboard.writeText(text || ''),
});
