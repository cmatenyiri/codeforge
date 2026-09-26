import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // sockjs-client was written for browserify and reads Node's `global`, which a
  // browser does not have. Pointing it at `globalThis` is all it needs.
  define: {
    global: 'globalThis',
  },
  server: {
    port: 5173,
    // Fail loudly rather than drifting to the next free port. The backend's CORS
    // allowlist names this exact origin, so a silent port bump surfaces in the
    // browser as an unexplained network error with no obvious cause.
    strictPort: true,
  },
});
