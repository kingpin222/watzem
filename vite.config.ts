import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
import fs from 'fs';

// Helper plugin to ensure APK binaries are never bundled into web distribution assets
function excludeApkPlugin() {
  return {
    name: 'exclude-apk-from-dist',
    closeBundle() {
      const distDir = path.resolve(__dirname, 'dist');
      if (fs.existsSync(distDir)) {
        const files = fs.readdirSync(distDir);
        for (const file of files) {
          if (file.endsWith('.apk')) {
            fs.unlinkSync(path.join(distDir, file));
          }
        }
      }
    }
  };
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue(), excludeApkPlugin()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:2727',
        changeOrigin: true
      },
      '/download': {
        target: 'http://localhost:2727',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true
  }
});
