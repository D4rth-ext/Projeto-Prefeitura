import { useEffect, useMemo, useState } from 'react'
import { buscarUsuarios, criarUsuario, inativarUsuario, alterarSetorUsuario, buscarCadastrosPendentes, decidirCadastro, editarUsuario, redefinirSenhaUsuario } from '../api/usuarios'
import { buscarSetoresPublicos } from '../api/setores'
import { mensagemDeErro } from '../api/erro'
import { useAuth } from '../context/AuthContext'

const VAZIO = { nome: '', email: '', senha: '', telefone: '', perfil: 'ATENDENTE' }

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
  const [pendentes, setPendentes] = useState([])
  const [aba, setAba] = useState('EQUIPE') // EQUIPE | TECNICOS | FUNCIONARIOS | PENDENTES
  const [busca, setBusca] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)

  const [form, setForm] = useState(VAZIO)
  const [mostrarForm, setMostrarForm] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [erroForm, setErroForm] = useState(null)
  const [funcionario, setFuncionario] = useState(null)
  const [setoresAtivos, setSetoresAtivos] = useState([])
  const [novoSetor, setNovoSetor] = useState('')
  const [ocupadoSetor, setOcupadoSetor] = useState(false)
  const [erroSetor, setErroSetor] = useState('')
  const [senhaTemporaria, setSenhaTemporaria] = useState(null)
  const [editandoEquipe, setEditandoEquipe] = useState(null)
  const [salvandoEdicao, setSalvandoEdicao] = useState(false)

  async function abrirTroca(u) {
    setFuncionario(u); setNovoSetor(''); setSetoresAtivos([]); setErroSetor(''); setOcupadoSetor(true)
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
      const [listaUsuarios, listaPendentes] = await Promise.all([buscarUsuarios(), buscarCadastrosPendentes()])
      setUsuarios(listaUsuarios)
      setPendentes(listaPendentes)
    } catch (err) {
      setErro(mensagemDeErro(err, 'Não foi possível carregar os usuários.'))
    } finally {
      setCarregando(false)
    }
  }

  async function decidir(u, decisao) {
    const texto = decisao === 'APROVADO' ? `Aprovar o cadastro de ${u.nome}?` : `Rejeitar o cadastro de ${u.nome}?`
    if (!confirm(texto)) return
    setErro(null)
    try { await decidirCadastro(u.id, decisao); await carregar() }
    catch (err) { setErro(mensagemDeErro(err, 'Não foi possível registrar a decisão.')) }
  }

  async function gerarSenhaTemporaria(u) {
    setSenhaTemporaria(null)
    try { setSenhaTemporaria({ usuario: u, ...(await redefinirSenhaUsuario(u.id)) }) }
    catch (err) { setErro(mensagemDeErro(err, 'Não foi possível redefinir a senha.')) }
  }

  async function salvarEdicaoEquipe(e) {
    e.preventDefault()
    setSalvandoEdicao(true); setErroForm(null)
    try {
      const { id, ...dados } = editandoEquipe
      const atualizado = await editarUsuario(id, dados)
      setUsuarios(lista => lista.map(u => u.id === atualizado.id ? atualizado : u))
      setEditandoEquipe(null)
    } catch (err) { setErroForm(mensagemDeErro(err, 'Não foi possível salvar a conta.')) }
    finally { setSalvandoEdicao(false) }
  }

  useEffect(() => { carregar() }, [])

  const lista = useMemo(
    () => usuarios.filter((u) => {
      const perfilCorresponde = aba === 'EQUIPE' ? ['ADMIN', 'ATENDENTE'].includes(u.perfil)
        : aba === 'TECNICOS' ? u.perfil === 'TECNICO' : aba === 'FUNCIONARIOS'
          ? u.perfil === 'SOLICITANTE' && u.situacaoCadastro !== 'PENDENTE' : false
      return perfilCorresponde && `${u.nome} ${u.email} ${u.setor?.nome || ''}`.toLocaleLowerCase().includes(busca.toLocaleLowerCase())
    }),
    [usuarios, aba, busca]
  )

  const listaExibida = aba === 'PENDENTES'
    ? pendentes.filter(u => `${u.nome} ${u.email} ${u.setor?.nome || ''}`.toLocaleLowerCase().includes(busca.toLocaleLowerCase()))
    : lista

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
            <div className="field"><label>Telefone</label><input type="tel" maxLength={20} value={form.telefone} onChange={e => campo('telefone', e.target.value)} /></div>
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

        {editandoEquipe && <form onSubmit={salvarEdicaoEquipe} className="card form-stack" style={{ marginBottom: 20, maxWidth: 420 }}>
          <h2>Editar conta de equipe</h2>
          <div className="field"><label>Nome</label><input required maxLength={120} value={editandoEquipe.nome} onChange={e => setEditandoEquipe(f => ({ ...f, nome: e.target.value }))} /></div>
          <div className="field"><label>E-mail</label><input type="email" required value={editandoEquipe.email} onChange={e => setEditandoEquipe(f => ({ ...f, email: e.target.value }))} /></div>
          <div className="field"><label>Telefone</label><input type="tel" maxLength={20} value={editandoEquipe.telefone || ''} onChange={e => setEditandoEquipe(f => ({ ...f, telefone: e.target.value }))} /></div>
          {erroForm && <p className="erro" role="alert">{erroForm}</p>}
          <div className="actions"><button className="btn-primary" disabled={salvandoEdicao}>{salvandoEdicao ? 'Salvando…' : 'Salvar'}</button><button type="button" className="btn-secondary" onClick={() => setEditandoEquipe(null)}>Cancelar</button></div>
        </form>}

        {senhaTemporaria && <section className="card" role="status" style={{ marginBottom: 16, border: '1px solid var(--green)' }}>
          <h2>Senha temporária de {senhaTemporaria.usuario.nome}</h2>
          <p>Mostre esta senha uma vez à pessoa por um canal interno. Ela expira em {new Date(senhaTemporaria.expiraEm).toLocaleString()}.</p>
          <p style={{ fontFamily: 'monospace', fontSize: 18, userSelect: 'all' }}>{senhaTemporaria.senhaTemporaria}</p>
          <button type="button" className="btn-secondary" onClick={() => setSenhaTemporaria(null)}>Fechar e apagar da tela</button>
        </section>}

        <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
          <button type="button" style={abaStyle(aba === 'EQUIPE')} disabled={ocupadoSetor} onClick={() => { setAba('EQUIPE'); setFuncionario(null) }}>Administração e atendimento</button>
          <button type="button" style={abaStyle(aba === 'TECNICOS')} onClick={() => setAba('TECNICOS')}>Técnicos</button>
          <button type="button" style={abaStyle(aba === 'FUNCIONARIOS')} onClick={() => setAba('FUNCIONARIOS')}>Funcionários</button>
          <button type="button" style={abaStyle(aba === 'PENDENTES')} onClick={() => setAba('PENDENTES')}>Cadastros pendentes ({pendentes.length})</button>
        </div>

        <input type="search" aria-label="Pesquisar usuários" placeholder="Pesquisar por nome, e-mail ou setor" value={busca}
          onChange={e => setBusca(e.target.value)} style={{ width: '100%', marginBottom: 14 }} />

        {aba === 'FUNCIONARIOS' && funcionario && <form className="card form-stack" onSubmit={salvarTroca}>
          <h2>Alterar setor de {funcionario.nome}</h2>
          <p>Setor atual: {funcionario.setor?.nome || 'Não informado'}. A mudança vale para novos chamados.</p>
          <div className="field">
            <label htmlFor="novo-setor-funcionario">Novo setor</label>
            <select id="novo-setor-funcionario" required disabled={ocupadoSetor} value={novoSetor} onChange={e => setNovoSetor(e.target.value)}>
              <option value="">Selecione um setor ativo</option>
              {setoresAtivos.map(setor => <option key={setor.id} value={setor.id}>{setor.nome}</option>)}
            </select>
          </div>
          {erroSetor && <p className="erro" role="alert">{erroSetor}</p>}
          {!ocupadoSetor && !erroSetor && !setoresAtivos.length && <p>Nenhum outro setor ativo disponível.</p>}
          <div className="actions"><button className="btn-primary" disabled={ocupadoSetor || !novoSetor}>Salvar setor</button><button type="button" className="btn-secondary" disabled={ocupadoSetor} onClick={() => setFuncionario(null)}>Cancelar</button></div>
        </form>}
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          {carregando && <p style={{ padding: 18, color: 'var(--text-secondary)' }}>Carregando...</p>}
          {erro && <p style={{ padding: 18, color: 'var(--red)' }}>{erro}</p>}
          {!carregando && listaExibida.length === 0 && (
            <p style={{ padding: 18, color: 'var(--text-secondary)' }}>
              {aba === 'PENDENTES' ? 'Nenhum cadastro aguardando aprovação.' : aba === 'EQUIPE' ? 'Nenhum membro da equipe cadastrado.' : 'Nenhum funcionário cadastrado.'}
            </p>
          )}
          {!carregando && listaExibida.length > 0 && (
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
                  {listaExibida.map((u) => {
                    const ehVoce = u.email?.toLowerCase() === logado?.email?.toLowerCase()
                    return (
                      <tr key={u.id} style={{ opacity: u.ativo ? 1 : 0.5 }}>
                        <td style={{ ...celula, fontWeight: 500 }}>{u.nome}{ehVoce ? ' (você)' : ''}</td>
                        <td style={{ ...celula, color: 'var(--text-secondary)' }}>{u.email}</td>
                        <td style={celula}>{LABEL_PERFIL[u.perfil] || u.perfil}</td>
                        <td style={{ ...celula, color: 'var(--text-secondary)' }}>{u.setor?.nome || '—'}</td>
                        <td style={celula}>
                          {u.situacaoCadastro === 'PENDENTE' ? <span className="badge badge-recebido">Aguardando aprovação</span> : u.situacaoCadastro === 'REJEITADO' ? <span className="badge badge-cancelado">Cadastro rejeitado</span> : <span className={`badge ${u.ativo ? 'badge-finalizado' : 'badge-entregue'}`}>{u.ativo ? 'Ativo' : 'Inativo'}</span>}
                        </td>
                        <td style={{ ...celula, textAlign: 'right', whiteSpace: 'nowrap' }}>
                          {aba === 'PENDENTES' && <><button type="button" className="btn-primary" onClick={() => decidir(u, 'APROVADO')}>Aprovar</button> <button type="button" className="btn-secondary" onClick={() => decidir(u, 'REJEITADO')}>Rejeitar</button></>}
                          {u.ativo && u.perfil === 'SOLICITANTE' && <button type="button" className="btn-secondary" disabled={ocupadoSetor} onClick={() => abrirTroca(u)}>Alterar setor</button>}
                          {u.ativo && u.perfil !== 'SOLICITANTE' && <button type="button" className="btn-secondary" onClick={() => { setEditandoEquipe({ id: u.id, nome: u.nome, email: u.email, telefone: u.telefone || '' }); setErroForm(null) }}>Editar</button>}
                          {u.ativo && !ehVoce && <button type="button" className="btn-secondary" onClick={() => gerarSenhaTemporaria(u)}>Senha temporária</button>}
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
