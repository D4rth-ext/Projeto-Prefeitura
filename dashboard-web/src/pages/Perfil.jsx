import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { mensagemDeErro } from '../api/erro'
import { useAuth } from '../context/AuthContext'

const PERFIS = { ADMIN: 'Administrador', ATENDENTE: 'Atendente', TECNICO: 'Técnico', SOLICITANTE: 'Solicitante' }

export default function Perfil() {
  const { usuario, logout } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ senhaAtual: '', novaSenha: '', confirmarSenha: '' })
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState(false)
  const [ocupado, setOcupado] = useState(false)

  async function alterarSenha(e) {
    e.preventDefault()
    setErro(''); setSucesso(false)
    if (form.novaSenha !== form.confirmarSenha) return setErro('As senhas não conferem.')
    setOcupado(true)
    try {
      await api.patch('/usuarios/me/senha', form)
      setSucesso(true)
      logout()
      navigate('/login?motivo=senha-alterada', { replace: true })
    } catch (err) { setErro(mensagemDeErro(err, 'Não foi possível alterar a senha.')) }
    finally { setOcupado(false) }
  }

  return <div>
    <header style={{ background: 'var(--surface)', padding: '16px 24px', borderBottom: '1px solid var(--border)' }}>
      <h1 style={{ fontSize: 18, fontWeight: 500, margin: 0 }}>Minha conta</h1>
    </header>
    <div className="content" style={{ maxWidth: 560 }}>
      <section className="card form-stack">
        <h2 style={{ marginTop: 0 }}>Dados da conta</h2>
        <div className="field"><label>Nome</label><input value={usuario?.nome || ''} readOnly /></div>
        <div className="field"><label>E-mail</label><input value={usuario?.email || ''} readOnly /></div>
        <div className="field"><label>Perfil</label><input value={PERFIS[usuario?.perfil] || usuario?.perfil || ''} readOnly /></div>
      </section>

      {usuario?.perfil === 'ADMIN' ? <form onSubmit={alterarSenha} className="card form-stack" style={{ marginTop: 16 }}>
        <h2 style={{ marginTop: 0 }}>Alterar minha senha</h2>
        <div className="field"><label>Senha atual</label><input type="password" autoComplete="current-password" required value={form.senhaAtual} onChange={e => setForm(f => ({ ...f, senhaAtual: e.target.value }))} /></div>
        <div className="field"><label>Nova senha</label><input type="password" autoComplete="new-password" minLength={8} pattern="^(?=.*[A-Za-z])(?=.*\d).{8,}$" title="Mínimo 8 caracteres, com letra e número" required value={form.novaSenha} onChange={e => setForm(f => ({ ...f, novaSenha: e.target.value }))} /></div>
        <div className="field"><label>Confirmar nova senha</label><input type="password" autoComplete="new-password" required value={form.confirmarSenha} onChange={e => setForm(f => ({ ...f, confirmarSenha: e.target.value }))} /></div>
        {erro && <p role="alert" className="erro">{erro}</p>}
        {sucesso && <p role="status">Senha alterada. Entre novamente com a senha nova.</p>}
        <button className="btn-primary" disabled={ocupado}>{ocupado ? 'Salvando…' : 'Alterar senha'}</button>
      </form> : <p className="muted">Para trocar uma senha esquecida, solicite uma senha temporária ao administrador.</p>}
    </div>
  </div>
}
