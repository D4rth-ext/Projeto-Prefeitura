import { useEffect, useMemo, useState } from 'react'
import { buscarUsuarios, criarUsuario, inativarUsuario } from '../api/usuarios'
import { mensagemDeErro } from '../api/erro'
import { useAuth } from '../context/AuthContext'

const VAZIO = { nome: '', email: '', senha: '', perfil: 'ATENDENTE' }

const LABEL_PERFIL = {
  ADMIN: 'Administrador',
  TECNICO: 'Técnico',
  ATENDENTE: 'Atendente',
  SOLICITANTE: 'Funcionário'
}

const celula = { padding: '13px 14px', borderBottom: '1px solid var(--border)' }

export default function Usuarios() {
  const { usuario: logado } = useAuth()
  const [usuarios, setUsuarios] = useState([])
  const [aba, setAba] = useState('EQUIPE') // EQUIPE | FUNCIONARIOS
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)

  const [form, setForm] = useState(VAZIO)
  const [mostrarForm, setMostrarForm] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [erroForm, setErroForm] = useState(null)

  async function carregar() {
    setCarregando(true)
    setErro(null)
    try {
      setUsuarios(await buscarUsuarios())
    } catch (err) {
      setErro(mensagemDeErro(err, 'Não foi possível carregar os usuários.'))
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregar() }, [])

  const lista = useMemo(
    () => usuarios.filter((u) => (aba === 'EQUIPE' ? u.perfil !== 'SOLICITANTE' : u.perfil === 'SOLICITANTE')),
    [usuarios, aba]
  )

  function campo(nome, valor) {
    setForm((f) => ({ ...f, [nome]: valor }))
  }

  async function handleSalvar(e) {
    e.preventDefault()
    setErroForm(null)
    setSalvando(true)
    try {
      await criarUsuario(form)
      setForm(VAZIO)
      setMostrarForm(false)
      setAba('EQUIPE')
      carregar()
    } catch (err) {
      setErroForm(mensagemDeErro(err, 'Não foi possível criar o usuário.'))
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir(u) {
    if (!confirm(`Excluir ${u.nome}? A conta é desativada (não consegue mais entrar), mas o histórico das OS é mantido.`)) return
    setErro(null)
    try {
      await inativarUsuario(u.id)
      carregar()
    } catch (err) {
      // Ex.: técnico com OS em análise sem outro técnico vinculado (422)
      setErro(mensagemDeErro(err, 'Não foi possível excluir o usuário.'))
    }
  }

  const abaStyle = (ativa) => ({
    background: ativa ? 'var(--green)' : 'transparent',
    color: ativa ? '#fff' : 'var(--text-primary)',
    border: `1px solid ${ativa ? 'var(--green)' : 'var(--border)'}`,
    borderRadius: 6, padding: '7px 14px', fontSize: 13, cursor: 'pointer'
  })

  return (
    <div>
      <div style={{
        background: 'var(--surface)', padding: '16px 24px', display: 'flex',
        alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid var(--border)'
      }}>
        <h1 style={{ fontSize: 18, fontWeight: 500, margin: 0, display: 'flex', alignItems: 'center', gap: 10 }}>
          <i className="ti ti-users" style={{ color: 'var(--green)' }} />
          Técnicos e usuários
        </h1>
        <button className="btn-primary" onClick={() => setMostrarForm((v) => !v)}>
          <i className="ti ti-plus" />
          Novo usuário da equipe
        </button>
      </div>

      <div className="content">
        <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 0, marginBottom: 20 }}>
          <strong>Equipe</strong> (Administrador, Técnico, Atendente) é criada aqui. Os{' '}
          <strong>funcionários</strong> se cadastram sozinhos pela tela de autocadastro, vinculados a um setor.
        </p>

        {mostrarForm && (
          <form onSubmit={handleSalvar} className="card" style={{ marginBottom: 20, display: 'flex', flexDirection: 'column', gap: 14, maxWidth: 420 }}>
            <h2 style={{ fontSize: 14, fontWeight: 500, margin: 0 }}>Novo usuário</h2>

            <div className="field">
              <label>Nome completo</label>
              <input type="text" value={form.nome} onChange={(e) => campo('nome', e.target.value)} required />
            </div>
            <div className="field">
              <label>E-mail</label>
              <input type="email" value={form.email} onChange={(e) => campo('email', e.target.value)} required />
            </div>
            <div className="field">
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
            <div className="field">
              <label>Perfil de acesso</label>
              <select value={form.perfil} onChange={(e) => campo('perfil', e.target.value)} required>
                <option value="ATENDENTE">Atendente</option>
                <option value="TECNICO">Técnico</option>
                <option value="ADMIN">Administrador</option>
              </select>
            </div>

            {erroForm && <p style={{ color: 'var(--red)', fontSize: 12, margin: 0 }}>{erroForm}</p>}

            <div style={{ display: 'flex', gap: 10 }}>
              <button type="submit" className="btn-primary" disabled={salvando}>
                {salvando ? 'Criando...' : 'Criar usuário'}
              </button>
              <button type="button" onClick={() => setMostrarForm(false)}
                style={{ background: 'transparent', border: '1px solid var(--border)', borderRadius: 6, padding: '9px 16px', fontSize: 13, cursor: 'pointer' }}>
                Cancelar
              </button>
            </div>
          </form>
        )}

        <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
          <button type="button" style={abaStyle(aba === 'EQUIPE')} onClick={() => setAba('EQUIPE')}>Equipe</button>
          <button type="button" style={abaStyle(aba === 'FUNCIONARIOS')} onClick={() => setAba('FUNCIONARIOS')}>Funcionários</button>
        </div>

        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          {carregando && <p style={{ padding: 18, color: 'var(--text-secondary)' }}>Carregando...</p>}
          {erro && <p style={{ padding: 18, color: 'var(--red)' }}>{erro}</p>}
          {!carregando && lista.length === 0 && (
            <p style={{ padding: 18, color: 'var(--text-secondary)' }}>
              {aba === 'EQUIPE' ? 'Nenhum membro da equipe cadastrado.' : 'Nenhum funcionário cadastrado.'}
            </p>
          )}
          {!carregando && lista.length > 0 && (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ background: 'var(--page-bg)' }}>
                    {['Nome', 'E-mail', 'Perfil', 'Setor', 'Situação', ''].map((col) => (
                      <th key={col} style={{ textAlign: 'left', padding: '10px 14px', fontSize: 11, color: 'var(--text-secondary)', borderBottom: '1px solid var(--border)' }}>
                        {col}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {lista.map((u) => {
                    const ehVoce = u.email?.toLowerCase() === logado?.email?.toLowerCase()
                    return (
                      <tr key={u.id} style={{ opacity: u.ativo ? 1 : 0.5 }}>
                        <td style={{ ...celula, fontWeight: 500 }}>{u.nome}{ehVoce ? ' (você)' : ''}</td>
                        <td style={{ ...celula, color: 'var(--text-secondary)' }}>{u.email}</td>
                        <td style={celula}>{LABEL_PERFIL[u.perfil] || u.perfil}</td>
                        <td style={{ ...celula, color: 'var(--text-secondary)' }}>{u.setor?.nome || '—'}</td>
                        <td style={celula}>
                          <span className={`badge ${u.ativo ? 'badge-finalizado' : 'badge-entregue'}`}>{u.ativo ? 'Ativo' : 'Excluído'}</span>
                        </td>
                        <td style={{ ...celula, textAlign: 'right', whiteSpace: 'nowrap' }}>
                          {u.ativo && !ehVoce && (
                            <button onClick={() => handleExcluir(u)} title="Excluir" style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: '4px 8px', fontSize: 12, color: 'var(--red)' }}>
                              <i className="ti ti-trash" /> Excluir
                            </button>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
