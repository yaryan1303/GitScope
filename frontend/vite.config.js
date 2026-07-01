import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev server on :3000, proxying API calls to the Spring Boot backend on :8080.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
