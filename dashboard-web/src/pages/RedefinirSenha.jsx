import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { redefinirSenha } from '../api/recuperacaoSenha'
import { mensagemDeErro } from '../api/erro'

export default function RedefinirSenha() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const navigate = useNavigate()

  const [novaSenha, setNovaSenha] = useState('')
  const [confirmarNovaSenha, setConfirmarNovaSenha] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)
  const [sucesso, setSucesso] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setErro(null)

    if (novaSenha !== confirmarNovaSenha) {
      setErro('As senhas não conferem.')
      return
    }

    setEnviando(true)
    try {
      await redefinirSenha({ token, novaSenha, confirmarNovaSenha })
      setSucesso(true)
      setTimeout(() => navigate('/login'), 2500)
    } catch (err) {
      setErro(mensagemDeErro(err, 'Link inválido ou expirado. Solicite uma nova recuperação.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--green)' }}>
      <div className="card" style={{ width: 420 }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, marginBottom: 22 }}>
          <img src="/logo.png" alt="Brasão Sant'Ana do Livramento" style={{ width: 70, height: 'auto' }} />
          <h1 style={{ fontSize: 16, fontWeight: 500, margin: 0 }}>Criar nova senha</h1>
        </div>

        {!token && (
          <p style={{ color: 'var(--red)', fontSize: 13 }}>
            Link inválido — abra o link que veio no e-mail de recuperação.
          </p>
        )}

        {sucesso ? (
          <p style={{ fontSize: 13, textAlign: 'center' }}>
            Senha alterada! Redirecionando pro login...
          </p>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="field" style={{ marginBottom: 14 }}>
              <label>Nova senha</label>
              <input
                type="password"
                value={novaSenha}
                onChange={(e) => setNovaSenha(e.target.value)}
                minLength={8}
                pattern="^(?=.*[A-Za-z])(?=.*\d).{8,}$"
                title="Mínimo 8 caracteres, com letra e número"
                required
                disabled={!token}
              />
            </div>
            <p style={{ fontSize: 11, color: 'var(--text-secondary)', margin: '0 0 14px' }}>
              Mínimo 8 caracteres, com letra e número.
            </p>

            <div className="field" style={{ marginBottom: 20 }}>
              <label>Confirmar nova senha</label>
              <input
                type="password"
                value={confirmarNovaSenha}
                onChange={(e) => setConfirmarNovaSenha(e.target.value)}
                required
                disabled={!token}
              />
            </div>

            {erro && <p style={{ color: 'var(--red)', fontSize: 12, marginBottom: 12 }}>{erro}</p>}

            <button type="submit" className="btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={enviando || !token}>
              {enviando ? 'Salvando...' : 'Salvar nova senha'}
            </button>

            <p style={{ textAlign: 'center', fontSize: 12, color: 'var(--text-secondary)', marginTop: 16, marginBottom: 0 }}>
              <Link to="/esqueci-senha" style={{ color: 'var(--green)', fontWeight: 500 }}>Pedir um novo link</Link>
            </p>
          </form>
        )}
      </div>
    </div>
  )
}
