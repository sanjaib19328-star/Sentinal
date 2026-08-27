import { defineConfig, Plugin } from 'vite';
import react from '@vitejs/plugin-react';

const blockSourceFileNavigation = (): Plugin => ({
  name: 'block-source-file-navigation',
  configureServer(server) {
    server.middlewares.use((req, res, next) => {
      const url = req.url?.split('?')[0] || '';
      const isHtmlNav = req.headers.accept?.includes('text/html') || req.headers['sec-fetch-dest'] === 'document';
      if (isHtmlNav && (url.startsWith('/src/') || url.endsWith('.tsx') || url.endsWith('.ts'))) {
        res.writeHead(302, { Location: '/dashboard' });
        res.end();
        return;
      }
      next();
    });
  },
});

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react(), blockSourceFileNavigation()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
});
