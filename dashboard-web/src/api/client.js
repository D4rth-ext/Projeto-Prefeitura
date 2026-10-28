import axios from 'axios'

// Em dev, o Vite faz proxy de /api -> http://localhost:8080 (vite.config.js).
// Em produção, ajuste VITE_API_URL no .env do build.
// O backend expõe tudo sob /api/v1 (ver SecurityConfig.java do Aluno A).
const baseURL = import.meta.env.VITE_API_URL || '/api/v1'

export const api = axios.create({ baseURL })

// Anexa o token JWT (retornado pelo login) em toda requisição autenticada
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Se o backend responder 401 (token expirado/inválido), desloga e manda pro login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const loginInvalido = error.config?.url === '/auth/login'
    if (error.response?.status === 401 && !loginInvalido) {
      localStorage.removeItem('token')
      localStorage.removeItem('usuario')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
