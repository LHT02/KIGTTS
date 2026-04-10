const { app, BrowserWindow, ipcMain, dialog, shell, Menu } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const fs = require('fs');

let mainWindow = null;
let backendProc = null;
let rendererReady = false;
let suppressBackendExit = false;
let textContextMenuMode = 'custom';
const pendingBackendEvents = [];

const isDev = !app.isPackaged;
const isMac = process.platform === 'darwin';

function resolvePython() {
  if (!isDev) {
    if (process.platform === 'win32') {
      return path.join(process.resourcesPath, 'piper_env', 'python.exe');
    }
    return path.join(process.resourcesPath, 'piper_env', 'bin', 'python3');
  }
  const repoRoot = path.join(__dirname, '..', '..');
  if (process.platform === 'win32') {
    return path.join(repoRoot, 'pc_trainer', 'piper_env', 'python.exe');
  }
  return path.join(repoRoot, 'pc_trainer', 'piper_env', 'bin', 'python3');
}

function resolveResources() {
  if (!isDev) {
    return path.join(process.resourcesPath, 'resources_pack');
  }
  const repoRoot = path.join(__dirname, '..', '..');
  return path.join(repoRoot, 'pc_trainer', 'resources_pack');
}

function resolveBaseDir() {
  if (!isDev) {
    return process.resourcesPath;
  }
  const repoRoot = path.join(__dirname, '..', '..');
  return path.join(repoRoot, 'pc_trainer');
}

function sendBackendEvent(payload) {
  if (mainWindow && mainWindow.webContents && rendererReady) {
    mainWindow.webContents.send('backend:event', payload);
    return;
  }
  pendingBackendEvents.push(payload);
}

function flushBackendEvents() {
  if (!mainWindow || !mainWindow.webContents || !rendererReady) return;
  while (pendingBackendEvents.length) {
    mainWindow.webContents.send('backend:event', pendingBackendEvents.shift());
  }
}

function startBackend() {
  const pythonPath = resolvePython();
  let backendScript = path.join(__dirname, '..', 'backend', 'server.py');
  if (!isDev) {
    const unpacked = path.join(process.resourcesPath, 'app.asar.unpacked', 'backend', 'server.py');
    if (fs.existsSync(unpacked)) {
      backendScript = unpacked;
    }
  }
  const env = {
    ...process.env,
    PYTHONIOENCODING: 'utf-8',
    KGTTS_RESOURCES: resolveResources(),
    KGTTS_BASE_DIR: resolveBaseDir(),
  };

  if (!fs.existsSync(pythonPath)) {
    sendBackendEvent({
      type: 'error',
      id: '',
      message: `Python not found: ${pythonPath}`,
    });
    return;
  }

  backendProc = spawn(pythonPath, ['-u', backendScript], {
    stdio: ['pipe', 'pipe', 'pipe'],
    env,
  });

  backendProc.stdout.setEncoding('utf8');
  backendProc.stdout.on('data', (chunk) => {
    const lines = chunk.split(/\r?\n/).filter(Boolean);
    for (const line of lines) {
      try {
        const msg = JSON.parse(line);
        sendBackendEvent(msg);
      } catch (err) {
        sendBackendEvent({
          type: 'error',
          id: '',
          message: `Backend parse error: ${err}`,
          raw: line,
        });
      }
    }
  });

  backendProc.stderr.setEncoding('utf8');
  backendProc.stderr.on('data', (chunk) => {
    sendBackendEvent({
      type: 'log',
      id: '',
      message: chunk.toString(),
    });
  });

  backendProc.on('exit', (code) => {
    if (suppressBackendExit) {
      suppressBackendExit = false;
      return;
    }
    sendBackendEvent({
      type: 'error',
      id: '',
      message: `Backend exited with code ${code}`,
    });
  });
}

function stopBackend() {
  return new Promise((resolve) => {
    if (!backendProc) {
      resolve();
      return;
    }
    const proc = backendProc;
    let done = false;
    suppressBackendExit = true;
    const finalize = () => {
      if (done) return;
      done = true;
      if (backendProc === proc) {
        backendProc = null;
      }
      resolve();
    };
    proc.once('exit', finalize);
    try {
      proc.kill();
    } catch (err) {
      finalize();
      return;
    }
    setTimeout(() => {
      if (done) return;
      try {
        proc.kill('SIGKILL');
      } catch (err) {
        // ignore
      }
      finalize();
    }, 2000);
  });
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 860,
    backgroundColor: '#0f1115',
    frame: false,
    titleBarStyle: isMac ? 'hiddenInset' : undefined,
    trafficLightPosition: isMac ? { x: 12, y: 12 } : undefined,
    minWidth: 960,
    minHeight: 640,
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
    },
  });

  mainWindow.setMenu(null);

  if (isDev) {
    mainWindow.loadURL('http://localhost:5173');
    mainWindow.webContents.openDevTools({ mode: 'detach' });
  } else {
    mainWindow.loadFile(path.join(__dirname, '..', 'dist-renderer', 'index.html'));
  }

  mainWindow.webContents.on(
    'did-fail-load',
    (_event, errorCode, errorDescription, validatedURL, isMainFrame) => {
      console.error(
        `[renderer] did-fail-load code=${errorCode} mainFrame=${isMainFrame} url=${validatedURL} desc=${errorDescription}`,
      );
    },
  );

  mainWindow.webContents.on('console-message', (_event, level, message, line, sourceId) => {
    const lvl = ['verbose', 'info', 'warn', 'error'][level] || String(level);
    console.log(`[renderer:${lvl}] ${message} (${sourceId}:${line})`);
  });

  mainWindow.webContents.on('render-process-gone', (_event, details) => {
    console.error(`[renderer] process-gone reason=${details.reason} exitCode=${details.exitCode}`);
  });

  mainWindow.webContents.on('unresponsive', () => {
    console.error('[renderer] window unresponsive');
  });

  mainWindow.webContents.on('did-finish-load', () => {
    rendererReady = true;
    flushBackendEvents();
  });

  mainWindow.webContents.on('context-menu', (event, params) => {
    if (!mainWindow || !params?.isEditable) {
      return;
    }
    event.preventDefault();
    if (textContextMenuMode !== 'native') {
      return;
    }
    const editFlags = params.editFlags || {};
    const menu = Menu.buildFromTemplate([
      {
        label: '全选',
        role: 'selectAll',
        enabled: editFlags.canSelectAll !== false,
      },
      {
        label: '剪切',
        role: 'cut',
        enabled: !!editFlags.canCut,
      },
      {
        label: '复制',
        role: 'copy',
        enabled: !!editFlags.canCopy,
      },
      {
        label: '粘贴',
        role: 'paste',
        enabled: !!editFlags.canPaste,
      },
    ]);
    menu.popup({ window: mainWindow });
  });
}

app.whenReady().then(() => {
  createWindow();
  startBackend();

  const sendWindowState = () => {
    if (mainWindow && mainWindow.webContents) {
      mainWindow.webContents.send('window:state', {
        maximized: mainWindow.isMaximized(),
      });
    }
  };

  if (mainWindow) {
    mainWindow.on('maximize', sendWindowState);
    mainWindow.on('unmaximize', sendWindowState);
    mainWindow.on('restore', sendWindowState);
  }

  ipcMain.on('backend:send', (_, msg) => {
    if (
      !backendProc ||
      backendProc.killed ||
      backendProc.exitCode !== null ||
      !backendProc.stdin ||
      backendProc.stdin.destroyed ||
      !backendProc.stdin.writable
    ) {
      return;
    }
    try {
      backendProc.stdin.write(`${JSON.stringify(msg)}\n`);
    } catch (err) {
      sendBackendEvent({
        type: 'error',
        id: '',
        message: `Backend write failed: ${String(err)}`,
      });
    }
  });

  ipcMain.on('backend:restart', () => {
    sendBackendEvent({ type: 'log', id: '', message: '[SYS] 后端重启中...' });
    stopBackend().then(() => {
      startBackend();
      sendBackendEvent({ type: 'log', id: '', message: '[SYS] 后端已重启' });
    });
  });

  ipcMain.on('context-menu:set-mode', (_, mode) => {
    if (mode === 'native' || mode === 'custom') {
      textContextMenuMode = mode;
    }
  });

  ipcMain.on('window:minimize', () => {
    if (mainWindow) {
      mainWindow.minimize();
    }
  });
  ipcMain.on('window:toggleMaximize', () => {
    if (!mainWindow) return;
    if (mainWindow.isMaximized()) {
      mainWindow.unmaximize();
    } else {
      mainWindow.maximize();
    }
    sendWindowState();
  });
  ipcMain.on('window:close', () => {
    if (mainWindow) {
      mainWindow.close();
    }
  });
  ipcMain.handle('window:isMaximized', () => {
    return mainWindow ? mainWindow.isMaximized() : false;
  });

  ipcMain.handle('dialog:openFiles', async () => {
    const result = await dialog.showOpenDialog(mainWindow, {
      properties: ['openFile', 'multiSelections'],
      filters: [
        { name: 'Audio', extensions: ['wav', 'mp3', 'm4a', 'flac'] },
        { name: 'All', extensions: ['*'] },
      ],
    });
    return result.canceled ? [] : result.filePaths;
  });

  ipcMain.handle('dialog:openDir', async () => {
    const result = await dialog.showOpenDialog(mainWindow, {
      properties: ['openDirectory'],
    });
    return result.canceled ? '' : result.filePaths[0];
  });

  ipcMain.handle('dialog:openFile', async (_, opts = {}) => {
    const result = await dialog.showOpenDialog(mainWindow, {
      properties: ['openFile'],
      filters: opts.filters || [{ name: 'All', extensions: ['*'] }],
    });
    return result.canceled ? '' : result.filePaths[0];
  });

  ipcMain.handle('dialog:saveFile', async (_, opts = {}) => {
    const result = await dialog.showSaveDialog(mainWindow, {
      title: opts.title || '保存文件',
      defaultPath: opts.defaultPath || '',
      filters: opts.filters || [{ name: 'All', extensions: ['*'] }],
    });
    return result.canceled ? '' : (result.filePath || '');
  });

  ipcMain.handle('path:dirname', (_, filePath) => {
    if (!filePath) return '';
    return path.dirname(filePath);
  });

  ipcMain.handle('path:openInExplorer', async (_, targetPath) => {
    try {
      if (!targetPath || typeof targetPath !== 'string') {
        return { ok: false, message: '路径为空' };
      }
      const resolved = path.resolve(targetPath);
      if (!fs.existsSync(resolved)) {
        return { ok: false, message: '路径不存在', target: resolved };
      }
      const stat = fs.statSync(resolved);
      if (stat.isDirectory()) {
        const errMsg = await shell.openPath(resolved);
        if (errMsg) {
          return { ok: false, message: errMsg, target: resolved };
        }
        return { ok: true, target: resolved };
      }
      shell.showItemInFolder(resolved);
      return { ok: true, target: resolved };
    } catch (err) {
      return { ok: false, message: String(err) };
    }
  });

  ipcMain.handle('project:openOutputDir', async (_, outputDir) => {
    try {
      if (!outputDir || typeof outputDir !== 'string') {
        return { ok: false, message: '输出目录为空' };
      }
      const projectRoot = path.resolve(outputDir);
      const exportDir = path.join(projectRoot, 'export');
      let target = projectRoot;
      if (
        fs.existsSync(path.join(exportDir, 'voicepack.kigvpk')) ||
        fs.existsSync(path.join(exportDir, 'voicepack.zip')) ||
        fs.existsSync(exportDir)
      ) {
        target = exportDir;
      }
      if (!fs.existsSync(target)) {
        return { ok: false, message: '目录不存在', target };
      }
      const errMsg = await shell.openPath(target);
      if (errMsg) {
        return { ok: false, message: errMsg, target };
      }
      return { ok: true, target };
    } catch (err) {
      return { ok: false, message: String(err) };
    }
  });

  ipcMain.handle('project:clearWorkCache', async (_, outputDir) => {
    try {
      if (!outputDir || typeof outputDir !== 'string') {
        return { ok: false, message: '输出目录为空' };
      }
      const projectRoot = path.resolve(outputDir);
      if (!fs.existsSync(projectRoot)) {
        return { ok: false, message: '输出目录不存在', path: projectRoot };
      }
      const workDir = path.join(projectRoot, 'work');
      if (!fs.existsSync(workDir)) {
        return { ok: true, cleared: false, path: workDir };
      }
      fs.rmSync(workDir, { recursive: true, force: true });
      fs.mkdirSync(workDir, { recursive: true });
      return { ok: true, cleared: true, path: workDir };
    } catch (err) {
      return { ok: false, message: String(err) };
    }
  });

  ipcMain.handle('project:ensureDefaultOutputDir', async () => {
    try {
      const docsDir = app.getPath('documents');
      const root = path.join(docsDir, 'TTSPACK');
      const now = new Date();
      const pad2 = (n) => String(n).padStart(2, '0');
      const stamp =
        `${now.getFullYear()}${pad2(now.getMonth() + 1)}${pad2(now.getDate())}-` +
        `${pad2(now.getHours())}${pad2(now.getMinutes())}${pad2(now.getSeconds())}`;
      const target = path.join(root, stamp);
      fs.mkdirSync(target, { recursive: true });
      return { ok: true, path: target };
    } catch (err) {
      return { ok: false, message: String(err) };
    }
  });

  ipcMain.handle('fs:saveImage', async (_, payload) => {
    try {
      const dataUrl = payload?.dataUrl;
      if (!dataUrl || typeof dataUrl !== 'string') {
        return '';
      }
      const matches = dataUrl.match(/^data:(image\/[a-zA-Z0-9.+-]+);base64,(.*)$/);
      if (!matches) return '';
      const mime = matches[1];
      const base64 = matches[2];
      const ext = mime.includes('png') ? 'png' : mime.includes('jpeg') ? 'jpg' : 'png';
      const outDir = path.join(app.getPath('userData'), 'avatars');
      fs.mkdirSync(outDir, { recursive: true });
      const filename = `avatar-${Date.now()}.${ext}`;
      const outPath = path.join(outDir, filename);
      fs.writeFileSync(outPath, Buffer.from(base64, 'base64'));
      return outPath;
    } catch (err) {
      return '';
    }
  });

  ipcMain.handle('fs:readImage', async (_, filePath) => {
    try {
      if (!filePath || typeof filePath !== 'string') {
        return '';
      }
      if (!fs.existsSync(filePath)) {
        return '';
      }
      const ext = path.extname(filePath).toLowerCase();
      const mime =
        ext === '.jpg' || ext === '.jpeg'
          ? 'image/jpeg'
          : ext === '.webp'
            ? 'image/webp'
            : ext === '.bmp'
              ? 'image/bmp'
              : 'image/png';
      const data = fs.readFileSync(filePath);
      return `data:${mime};base64,${data.toString('base64')}`;
    } catch (err) {
      return '';
    }
  });

  ipcMain.handle('fs:saveDroppedFile', async (_, payload) => {
    try {
      const name = payload?.name;
      const data = payload?.data;
      if (!name || typeof name !== 'string' || !data) {
        return '';
      }
      let buffer = null;
      if (data instanceof ArrayBuffer) {
        buffer = Buffer.from(new Uint8Array(data));
      } else if (ArrayBuffer.isView(data)) {
        buffer = Buffer.from(data.buffer);
      } else if (Buffer.isBuffer(data)) {
        buffer = data;
      }
      if (!buffer) {
        return '';
      }
      const safeName = name.replace(/[^\w.\-]+/g, '_');
      const outDir = path.join(app.getPath('userData'), 'drops');
      fs.mkdirSync(outDir, { recursive: true });
      const filename = `${Date.now()}-${safeName}`;
      const outPath = path.join(outDir, filename);
      fs.writeFileSync(outPath, buffer);
      return outPath;
    } catch (err) {
      return '';
    }
  });

  ipcMain.handle('fs:copyFile', async (_, payload) => {
    try {
      const src = payload?.src;
      const dst = payload?.dst;
      if (!src || !dst || typeof src !== 'string' || typeof dst !== 'string') {
        return false;
      }
      if (!fs.existsSync(src)) {
        return false;
      }
      fs.mkdirSync(path.dirname(dst), { recursive: true });
      fs.copyFileSync(src, dst);
      return true;
    } catch (err) {
      return false;
    }
  });

  ipcMain.handle('fs:writeTextFile', async (_, payload) => {
    try {
      const filePath = payload?.path;
      const text = payload?.text;
      if (!filePath || typeof filePath !== 'string') {
        return false;
      }
      if (text !== undefined && typeof text !== 'string') {
        return false;
      }
      fs.mkdirSync(path.dirname(filePath), { recursive: true });
      fs.writeFileSync(filePath, text || '', 'utf8');
      return true;
    } catch (err) {
      return false;
    }
  });

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
