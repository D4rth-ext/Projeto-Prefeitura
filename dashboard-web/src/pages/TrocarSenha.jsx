import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { mensagemDeErro } from '../api/erro'
import { useAuth } from '../context/AuthContext'

export default function TrocarSenha() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ senhaAtual: '', novaSenha: '', confirmarSenha: '' })
  const [erro, setErro] = useState('')
  const [ocupado, setOcupado] = useState(false)

  async function enviar(e) {
    e.preventDefault()
    setErro('')
    if (form.novaSenha !== form.confirmarSenha) return setErro('As senhas não conferem.')
    setOcupado(true)
    try {
      await api.patch('/usuarios/me/senha', form)
      logout()
      navigate('/login?motivo=senha-alterada', { replace: true })
    } catch (err) {
      setErro(mensagemDeErro(err, 'Não foi possível alterar a senha.'))
    } finally {
      setOcupado(false)
    }
  }

  return <main style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', background: 'var(--green)', padding: 16 }}>
    <form className="card form-stack" onSubmit={enviar} style={{ width: '100%', maxWidth: 420 }}>
      <h1 style={{ margin: 0, fontSize: 18 }}>Defina uma senha nova</h1>
      <p className="muted">A senha temporária precisa ser substituída antes de continuar.</p>
      <div className="field"><label>Senha temporária atual</label><input type="password" autoComplete="current-password" required value={form.senhaAtual} onChange={e => setForm(f => ({ ...f, senhaAtual: e.target.value }))} /></div>
      <div className="field"><label>Nova senha</label><input type="password" autoComplete="new-password" minLength={8} pattern="^(?=.*[A-Za-z])(?=.*\d).{8,}$" title="Mínimo 8 caracteres, com letra e número" required value={form.novaSenha} onChange={e => setForm(f => ({ ...f, novaSenha: e.target.value }))} /></div>
      <div className="field"><label>Confirmar nova senha</label><input type="password" autoComplete="new-password" required value={form.confirmarSenha} onChange={e => setForm(f => ({ ...f, confirmarSenha: e.target.value }))} /></div>
      {erro && <p role="alert" className="erro">{erro}</p>}
      <button className="btn-primary" disabled={ocupado}>{ocupado ? 'Salvando…' : 'Salvar senha e entrar novamente'}</button>
    </form>
  </main>
}
