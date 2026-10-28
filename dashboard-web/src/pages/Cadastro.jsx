import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { buscarSetoresPublicos } from '../api/setores'
import { mensagemDeErro } from '../api/erro'

export default function Cadastro() {
  const { cadastrar } = useAuth()
  const navigate = useNavigate()

  const [setores, setSetores] = useState([])
  const [form, setForm] = useState({ nome: '', email: '', senha: '', confirmarSenha: '', telefone: '', setorId: '' })
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    // RF: selecionar setor de origem entre os já cadastrados
    buscarSetoresPublicos().then(setSetores).catch(() => setSetores([]))
  }, [])

  function campo(nome, valor) {
    setForm((f) => ({ ...f, [nome]: valor }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await cadastrar(form)
      navigate('/')
    } catch (err) {
      // Cobre 409 (e-mail já cadastrado) e 422 (senhas não conferem), entre outros
      setErro(mensagemDeErro(err, 'Não foi possível concluir o cadastro. Verifique os dados e tente novamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--green-dark)', padding: '40px 16px' }}>
      <form onSubmit={handleSubmit} className="card" style={{ width: 420 }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, marginBottom: 22 }}>
          <img src="/logo.png" alt="Brasão Sant'Ana do Livramento" style={{ width: 70, height: 'auto' }} />
          <h1 style={{ fontSize: 16, fontWeight: 500, margin: 0 }}>Criar conta</h1>
        </div>

        <div className="field" style={{ marginBottom: 14 }}>
          <label>Nome completo</label>
          <input type="text" value={form.nome} onChange={(e) => campo('nome', e.target.value)} required />
        </div>

        <div className="field" style={{ marginBottom: 14 }}>
          <label>Setor de origem</label>
          <select value={form.setorId} onChange={(e) => campo('setorId', e.target.value)} required>
            <option value="">Selecione seu setor</option>
            {setores.map((s) => (
              <option key={s.id} value={s.id}>{s.nome}</option>
            ))}
          </select>
        </div>

        <div className="field" style={{ marginBottom: 14 }}>
          <label>Telefone</label>
          <input type="text" value={form.telefone} onChange={(e) => campo('telefone', e.target.value)} />
        </div>

        <div className="field" style={{ marginBottom: 14 }}>
          <label>E-mail</label>
          <input type="email" value={form.email} onChange={(e) => campo('email', e.target.value)} required />
        </div>

        <div className="field" style={{ marginBottom: 6 }}>
          <label>Senha</label>
          <input
            type="password"
            value={form.senha}
            onChange={(e) => campo('senha', e.target.value)}
            minLength={8}
            pattern="^(?=.*[A-Za-z])(?=.*\d).{8,}$"
            title="Mínimo 8 caracteres, com letra e número"
            required
          />
        </div>
        <p style={{ fontSize: 11, color: 'var(--text-secondary)', margin: '0 0 14px' }}>
          Mínimo 8 caracteres, com letra e número.
        </p>

        <div className="field" style={{ marginBottom: 20 }}>
          <label>Confirmar senha</label>
          <input
            type="password"
            value={form.confirmarSenha}
            onChange={(e) => campo('confirmarSenha', e.target.value)}
            required
          />
        </div>

        {erro && <p style={{ color: 'var(--red)', fontSize: 12, marginTop: -8, marginBottom: 12 }}>{erro}</p>}

        <button type="submit" className="btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={enviando}>
          {enviando ? 'Cadastrando...' : 'Criar conta'}
        </button>

        <p style={{ textAlign: 'center', fontSize: 12, color: 'var(--text-secondary)', marginTop: 16, marginBottom: 0 }}>
          Já tem conta? <Link to="/login" style={{ color: 'var(--green)', fontWeight: 500 }}>Entrar</Link>
        </p>
      </form>
    </div>
  )
}
