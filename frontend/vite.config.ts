import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('/node_modules/')) return;
          if (/\/(recharts|victory-vendor|d3-[^/]+|internmap|react-smooth)\//.test(id)) return 'charts';
          if (/\/(react|react-dom|react-is|react-router|react-router-dom|scheduler)\//.test(id)) return 'react-vendor';
          if (id.includes('/lucide-react/')) return 'icons';
          if (/\/(zod|react-hook-form|@hookform)\//.test(id)) return 'forms';
        },
      },
    },
  },
});
