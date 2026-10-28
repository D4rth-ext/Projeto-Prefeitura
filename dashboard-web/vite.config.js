import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Em dev, o Vite roda em localhost:3000 e faz proxy de /api para o
// backend Spring Boot em localhost:8080, evitando problemas de CORS
// durante o desenvolvimento.
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    allowedHosts: ['sb-s5dqlgf7e6g8.vercel.run'],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
