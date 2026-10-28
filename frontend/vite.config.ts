import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// В dev-режиме Vite проксирует API на сервисы, запущенные из IDEA.
// В проде маршрутизацией занимается Traefik, и прокси не нужен.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/learning': 'http://localhost:8081',
      '/api/ai': 'http://localhost:8082',
    },
  },
})
