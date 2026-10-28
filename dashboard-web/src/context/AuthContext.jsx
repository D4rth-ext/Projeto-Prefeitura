import { createContext, useContext, useState } from 'react'
import { api } from '../api/client'

const AuthContext = createContext(null)

// Perfis exatamente como o backend define (Perfil.java / CHECK da tabela usuario)
export const PERFIS = {
  ADMIN: 'ADMIN',
  TECNICO: 'TECNICO',
  ATENDENTE: 'ATENDENTE',
  SOLICITANTE: 'SOLICITANTE'
}

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(() => {
    const salvo = localStorage.getItem('usuario')
    return salvo ? JSON.parse(salvo) : null
  })

  // O backend identifica o perfil; menus e rotas aplicam as permissões.
  async function login(email, senha) {
    // POST /api/v1/auth/login -> { token, nome, perfil }
    const { data } = await api.post('/auth/login', { email, senha })

    const usuarioLogado = { nome: data.nome, perfil: data.perfil, email }
    localStorage.setItem('token', data.token)
    localStorage.setItem('usuario', JSON.stringify(usuarioLogado))
    setUsuario(usuarioLogado)
    return usuarioLogado
  }

  // Autoatendimento do funcionário (perfil SOLICITANTE no backend).
  async function cadastrar({ nome, email, senha, confirmarSenha, telefone, setorId }) {
    // POST /api/v1/auth/registro
    await api.post('/auth/registro', {
      nome, email, telefone, setorId: Number(setorId), senha, confirmarSenha
    })
    // O endpoint de registro não faz login automático — loga em seguida
    return login(email, senha)
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('usuario')
    setUsuario(null)
  }

  // O backend ainda não tem um PUT /usuarios/me (só /usuarios, exclusivo
  // de ADMIN, pra gerenciar a equipe). Até esse endpoint existir, a edição
  // do próprio perfil fica só local.
  async function atualizarUsuario(dados) {
    const atualizado = { ...usuario, ...dados }
    localStorage.setItem('usuario', JSON.stringify(atualizado))
    setUsuario(atualizado)
    return atualizado
  }

  return (
    <AuthContext.Provider value={{ usuario, login, cadastrar, logout, atualizarUsuario }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth precisa estar dentro de <AuthProvider>')
  return ctx
}
