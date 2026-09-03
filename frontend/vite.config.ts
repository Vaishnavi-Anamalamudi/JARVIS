import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': {
          target: env.VITE_BACKEND_PROXY_TARGET ?? 'http://localhost:8080',
          changeOrigin: true,
          ws: true
        }
      }
    },
    build: {
      chunkSizeWarningLimit: 650,
      rollupOptions: {
        output: {
          manualChunks(id) {
            const normalizedId = id.replace(/\\/g, '/');
            if (normalizedId.includes('node_modules')) {
              if (normalizedId.includes('@mui') || normalizedId.includes('@emotion')) {
                return 'mui';
              }
              if (normalizedId.includes('/node_modules/react') || normalizedId.includes('/node_modules/redux')) {
                return 'react';
              }
              if (normalizedId.includes('/node_modules/d3-') || normalizedId.includes('/node_modules/victory-vendor')) {
                return 'charts-vendor';
              }
              if (normalizedId.includes('/node_modules/recharts')) {
                return 'charts';
              }
              return 'vendor';
            }
          }
        }
      }
    }
  };
});
