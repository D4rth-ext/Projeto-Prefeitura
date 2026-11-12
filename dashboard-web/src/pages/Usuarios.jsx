import { useEffect, useMemo, useState } from 'react'
import { buscarUsuarios, criarUsuario, inativarUsuario, alterarSetorUsuario, redefinirSenhaUsuario } from '../api/usuarios'
import { buscarSetoresPublicos } from '../api/setores'
import { mensagemDeErro } from '../api/erro'
import { useAuth } from '../context/AuthContext'
import CampoComLista from '../components/CampoComLista'
import { telefoneDoSetor } from '../utils/catalogoSetores'

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
  const [funcionario, setFuncionario] = useState(null)
  const [setoresAtivos, setSetoresAtivos] = useState([])
  const [novoSetor, setNovoSetor] = useState('')
  const [novoSetorNome, setNovoSetorNome] = useState('')
  const [ocupadoSetor, setOcupadoSetor] = useState(false)
  const [erroSetor, setErroSetor] = useState('')
  const [alvoSenha, setAlvoSenha] = useState(null)
  const [formSenha, setFormSenha] = useState({ senhaAtual: '', novaSenha: '', confirmarSenha: '' })
  const [salvandoSenha, setSalvandoSenha] = useState(false)
  const [erroSenha, setErroSenha] = useState('')
  const [sucessoSenha, setSucessoSenha] = useState('')

  function abrirRedefinicaoSenha(u) {
    setAlvoSenha(u)
    setFormSenha({ senhaAtual: '', novaSenha: '', confirmarSenha: '' })
    setErroSenha('')
    setSucessoSenha('')
  }

  async function salvarRedefinicaoSenha(e) {
    e.preventDefault()
    setErroSenha('')
    if (formSenha.novaSenha !== formSenha.confirmarSenha) {
      setErroSenha('As senhas não coincidem.')
      return
    }
    setSalvandoSenha(true)
    try {
      const dados = {
        novaSenha: formSenha.novaSenha,
        confirmarSenha: formSenha.confirmarSenha
      }
      if (alvoSenha.email?.toLowerCase() === logado?.email?.toLowerCase()) dados.senhaAtual = formSenha.senhaAtual
      await redefinirSenhaUsuario(alvoSenha.id, dados)
      setSucessoSenha(`Senha de ${alvoSenha.nome} redefinida com sucesso.`)
      setAlvoSenha(null)
    } catch (err) {
      setErroSenha(mensagemDeErro(err, 'Não foi possível redefinir a senha.'))
    } finally {
      setSalvandoSenha(false)
    }
  }

  async function abrirTroca(u) {
    setFuncionario(u); setNovoSetor(''); setNovoSetorNome(''); setSetoresAtivos([]); setErroSetor(''); setOcupadoSetor(true)
    try {
      const setores = await buscarSetoresPublicos()
      setSetoresAtivos(setores.filter(s => s.id !== u.setor?.id))
    } catch (err) { setErroSetor(mensagemDeErro(err, 'Não foi possível carregar os setores.')) }
    finally { setOcupadoSetor(false) }
  }

  async function salvarTroca(e) {
    e.preventDefault()
    if (!novoSetor || ocupadoSetor) return
    setOcupadoSetor(true); setErroSetor('')
    try {
      const atualizado = await alterarSetorUsuario(funcionario.id, Number(novoSetor))
      setUsuarios(lista => lista.map(u => u.id === atualizado.id ? atualizado : u))
      setFuncionario(null)
    } catch (err) { setErroSetor(mensagemDeErro(err, 'Não foi possível alterar o setor.')) }
    finally { setOcupadoSetor(false) }
  }

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
          <button type="button" style={abaStyle(aba === 'EQUIPE')} disabled={ocupadoSetor} onClick={() => { setAba('EQUIPE'); setFuncionario(null) }}>Equipe</button>
          <button type="button" style={abaStyle(aba === 'FUNCIONARIOS')} onClick={() => setAba('FUNCIONARIOS')}>Funcionários</button>
        </div>

        {aba === 'FUNCIONARIOS' && funcionario && <form className="card form-stack" onSubmit={salvarTroca}>
          <h2>Alterar setor de {funcionario.nome}</h2>
          <p>Setor atual: {funcionario.setor?.nome || 'Não informado'}. A mudança vale para novos chamados.</p>
          <div className="field">
            <label htmlFor="novo-setor-funcionario">Novo setor</label>
            <CampoComLista
              id="novo-setor-funcionario"
              nomeLista="setores ativos"
              itens={setoresAtivos.map((setor) => setor.nome)}
              valor={novoSetorNome}
              alterar={(nome) => { setNovoSetorNome(nome); setNovoSetor('') }}
              selecionar={(nome) => {
                const setor = setoresAtivos.find((item) => item.nome === nome)
                setNovoSetorNome(nome)
                setNovoSetor(setor ? String(setor.id) : '')
              }}
              obrigatorio
              desabilitado={ocupadoSetor}
              ramalPorItem={telefoneDoSetor}
              mensagemSemResultado="Nenhum setor encontrado. Escolha um da lista."
            />
          </div>
          {erroSetor && <p className="erro" role="alert">{erroSetor}</p>}
          {!ocupadoSetor && !erroSetor && !setoresAtivos.length && <p>Nenhum outro setor ativo disponível.</p>}
          <div className="actions"><button className="btn-primary" disabled={ocupadoSetor || !novoSetor}>Salvar setor</button><button type="button" className="btn-secondary" disabled={ocupadoSetor} onClick={() => setFuncionario(null)}>Cancelar</button></div>
        </form>}
        {alvoSenha && <form className="card" onSubmit={salvarRedefinicaoSenha} style={{ marginBottom: 16, display: 'flex', flexDirection: 'column', gap: 14, maxWidth: 420 }}>
          <h2 style={{ fontSize: 14, fontWeight: 500, margin: 0 }}>Redefinir senha de {alvoSenha.nome}</h2>
          {alvoSenha.email?.toLowerCase() === logado?.email?.toLowerCase() && <div className="field">
            <label htmlFor="senha-atual-usuario">Senha atual</label>
            <input id="senha-atual-usuario" type="password" autoComplete="current-password" value={formSenha.senhaAtual} onChange={e => setFormSenha(f => ({ ...f, senhaAtual: e.target.value }))} required disabled={salvandoSenha} />
          </div>}
          <div className="field">
            <label htmlFor="nova-senha-usuario">Nova senha</label>
            <input id="nova-senha-usuario" type="password" autoComplete="new-password" value={formSenha.novaSenha} onChange={e => setFormSenha(f => ({ ...f, novaSenha: e.target.value }))} minLength={8} pattern="^(?=.*[A-Za-z])(?=.*\d).{8,}$" title="Mínimo 8 caracteres, com letra e número" required disabled={salvandoSenha} />
          </div>
          <div className="field">
            <label htmlFor="confirmar-senha-usuario">Confirmar nova senha</label>
            <input id="confirmar-senha-usuario" type="password" autoComplete="new-password" value={formSenha.confirmarSenha} onChange={e => setFormSenha(f => ({ ...f, confirmarSenha: e.target.value }))} required disabled={salvandoSenha} />
          </div>
          {erroSenha && <p role="alert" style={{ color: 'var(--red)', fontSize: 12, margin: 0 }}>{erroSenha}</p>}
          <div style={{ display: 'flex', gap: 10 }}>
            <button type="submit" className="btn-primary" disabled={salvandoSenha}>{salvandoSenha ? 'Salvando...' : 'Salvar nova senha'}</button>
            <button type="button" className="btn-secondary" onClick={() => { setAlvoSenha(null); setErroSenha('') }} disabled={salvandoSenha}>Cancelar</button>
          </div>
        </form>}
        {sucessoSenha && <p role="status" style={{ color: 'var(--green)', fontSize: 13 }}>{sucessoSenha}</p>}
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
                    {['Nome', 'E-mail', 'Perfil', 'Setor', ...(aba === 'FUNCIONARIOS' ? ['Chamados abertos'] : []), 'Situação', ''].map((col) => (
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
                        {aba === 'FUNCIONARIOS' && <td style={celula}>{u.chamadosAbertos ?? 0}</td>}
                        <td style={celula}>
                          <span className={`badge ${u.ativo ? 'badge-finalizado' : 'badge-entregue'}`}>{u.ativo ? 'Ativo' : 'Excluído'}</span>
                        </td>
                        <td style={{ ...celula, textAlign: 'right', whiteSpace: 'nowrap' }}>
                          {u.ativo && u.perfil === 'SOLICITANTE' && <button type="button" className="btn-secondary" disabled={ocupadoSetor} onClick={() => abrirTroca(u)}>Alterar setor</button>}
                          {u.ativo && <button type="button" className="btn-secondary" onClick={() => abrirRedefinicaoSenha(u)} title={`Redefinir senha de ${u.nome}`} style={{ marginRight: 8 }}><i className="ti ti-key" /> Redefinir senha</button>}
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

