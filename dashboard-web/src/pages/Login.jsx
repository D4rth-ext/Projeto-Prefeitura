import { useState } from 'react'
import { useNavigate, Link, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { mensagemDeErro } from '../api/erro'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState(null)
  const [carregando, setCarregando] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setErro(null)
    setCarregando(true)
    try {
      const usuario = await login(email, senha)
      navigate(usuario.trocaSenhaObrigatoria ? '/trocar-senha' : '/', { replace: true })
    } catch (err) {
      setErro(mensagemDeErro(err, err.response?.status === 401
        ? 'E-mail ou senha inválidos.'
        : 'Não foi possível entrar. Verifique sua conexão e tente novamente.'))
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--green)', padding: 16 }}>
      <form onSubmit={handleSubmit} className="card" style={{ width: '100%', maxWidth: 420 }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, marginBottom: 22 }}>
          <img src="/logo.png" alt="Brasão Sant'Ana do Livramento" style={{ width: 90, height: 'auto' }} />
          <h1 style={{ fontSize: 16, fontWeight: 500, margin: 0 }}>Manutenção TI</h1>
          <p style={{ fontSize: 12, color: 'var(--text-secondary)', margin: 0 }}>Acesso de solicitantes e equipe de TI</p>
        </div>
        {params.get('motivo') === 'senha-alterada' && <p role="status" style={{ color: 'var(--final-text)', fontSize: 12 }}>Senha alterada. Entre novamente com sua nova senha.</p>}
        {params.get('motivo') === 'inatividade' && <p role="status" className="muted">Sua sessão foi encerrada após um período sem atividade.</p>}

        <div className="field" style={{ marginBottom: 16 }}>
          <label htmlFor="login-email" style={{ fontSize: 15 }}>E-mail</label>
          <input
            type="email"
            id="login-email"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            style={{ height: 46, fontSize: 15 }}
          />
        </div>
        <div className="field" style={{ marginBottom: 8 }}>
          <label htmlFor="login-senha" style={{ fontSize: 15 }}>Senha</label>
          <input
            type="password"
            id="login-senha"
            autoComplete="current-password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            required
            style={{ height: 46, fontSize: 15 }}
          />
        </div>

        <p style={{ textAlign: 'right', marginTop: 0, marginBottom: 16 }}>
          <Link to="/esqueci-senha" style={{ color: 'var(--text-secondary)', fontSize: 12 }}>Esqueci minha senha</Link>
        </p>

        {erro && <p role="alert" style={{ color: 'var(--red)', fontSize: 12, marginTop: -8, marginBottom: 12 }}>{erro}</p>}

        <button type="submit" className="btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={carregando}>
          {carregando ? 'Entrando...' : 'Entrar'}
        </button>

        <p style={{ textAlign: 'center', fontSize: 12, color: 'var(--text-secondary)', marginTop: 16, marginBottom: 0 }}>
          É solicitante e não tem conta? <Link to="/cadastro" style={{ color: 'var(--green)', fontWeight: 500 }}>Cadastre-se</Link>
        </p>
      </form>
    </div>
  )
}
