import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    proxy: {
      // Proxy keys are tested in declaration order; the first match wins. Most
      // specific patterns FIRST. The social-service runs on :8081 and owns three
      // route prefixes; everything else continues to the monolith on :8080.
      // The /api/users/{id}/posts regex needs to come BEFORE the generic '/api'
      // catch-all so it gets routed to social-service.
      '^/api/users/\\d+/posts': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/social': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/posts': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
