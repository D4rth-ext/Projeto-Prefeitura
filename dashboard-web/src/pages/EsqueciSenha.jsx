import { useState } from 'react'
import { Link } from 'react-router-dom'
import { solicitarRecuperacao } from '../api/recuperacaoSenha'
import { mensagemDeErro } from '../api/erro'

export default function EsqueciSenha() {
  const [email, setEmail] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [enviado, setEnviado] = useState(false)
  const [erro, setErro] = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await solicitarRecuperacao(email)
      setEnviado(true)
    } catch (err) {
      setErro(mensagemDeErro(err, 'Não foi possível enviar o e-mail de recuperação. Tente novamente mais tarde.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--green)' }}>
      <div className="card" style={{ width: 420 }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, marginBottom: 22 }}>
          <img src="/logo.png" alt="Brasão Sant'Ana do Livramento" style={{ width: 70, height: 'auto' }} />
          <h1 style={{ fontSize: 16, fontWeight: 500, margin: 0 }}>Recuperar senha</h1>
        </div>

        {enviado ? (
          <div style={{ textAlign: 'center' }}>
            <p style={{ fontSize: 13, color: 'var(--text-primary)' }}>
              Se <strong>{email}</strong> estiver cadastrado, você vai receber um e-mail com as
              instruções pra criar uma nova senha em alguns minutos.
            </p>
            <Link to="/login" style={{ color: 'var(--green)', fontWeight: 500, fontSize: 13 }}>
              Voltar pro login
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 0, marginBottom: 18 }}>
              Informe o e-mail da sua conta. Vamos enviar um link pra você criar uma senha nova.
            </p>

            <div className="field" style={{ marginBottom: 16 }}>
              <label>E-mail</label>
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </div>

            {erro && <p style={{ color: 'var(--red)', fontSize: 12, marginBottom: 12 }}>{erro}</p>}

            <button type="submit" className="btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={enviando}>
              {enviando ? 'Enviando...' : 'Enviar link de recuperação'}
            </button>

            <p style={{ textAlign: 'center', fontSize: 12, color: 'var(--text-secondary)', marginTop: 16, marginBottom: 0 }}>
              <Link to="/login" style={{ color: 'var(--green)', fontWeight: 500 }}>Voltar pro login</Link>
            </p>
          </form>
        )}
      </div>
    </div>
  )
}
