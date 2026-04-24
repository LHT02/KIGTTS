import fs from 'node:fs'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const pkg = JSON.parse(fs.readFileSync(new URL('./package.json', import.meta.url), 'utf-8')) as {
  version?: string
}

// https://vite.dev/config/
export default defineConfig({
  base: './',
  build: {
    outDir: 'dist-renderer',
  },
  plugins: [react()],
  define: {
    __APP_VERSION__: JSON.stringify(pkg.version ?? '0.0.0'),
  },
})
