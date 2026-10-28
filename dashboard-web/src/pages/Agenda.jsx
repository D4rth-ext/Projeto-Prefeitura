import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { listarEquipe, listarOrdens, listarTecnicosDaOs } from '../api/ordensServico'
import { listarAgendamentos, criarAgendamento, atualizarAgendamento, alterarStatusAgendamento } from '../api/agenda'
import { mensagemDeErro } from '../api/erro'
import { formatarDataHora } from '../utils/osLabels'

const vazio = { titulo: '', descricao: '', local: '', tecnicoId: '', osId: '', inicio: '', fim: '' }
const localData = iso => {
  if (!iso) return ''
  const data = new Date(iso)
  return new Date(data.getTime() - data.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}
export default function Agenda() {
  const { usuario } = useAuth()
  const gestao = ['ADMIN', 'ATENDENTE'].includes(usuario?.perfil)
  const [itens, setItens] = useState([])
  const [equipe, setEquipe] = useState([])
  const [ordens, setOrdens] = useState([])
  const [vinculados, setVinculados] = useState([])
  const [filtro, setFiltro] = useState({ tecnicoId: '', status: '', inicio: '', fim: '' })
  const [form, setForm] = useState(vazio)
  const [editando, setEditando] = useState(null)
  const [aberto, setAberto] = useState(false)
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [ocupado, setOcupado] = useState(false)
  const [erroVinculo, setErroVinculo] = useState('')
  async function carregar() {
    setCarregando(true); setErro('')
    if (filtro.inicio && filtro.fim && filtro.fim < filtro.inicio) { setErro('A data final deve ser igual ou posterior à inicial.'); setCarregando(false); return }
    try { setItens(await listarAgendamentos(Object.fromEntries(Object.entries(filtro).filter(([, v]) => v !== '')))) }
    catch (e) { setItens([]); setErro(mensagemDeErro(e, 'Não foi possível carregar a agenda.')) }
    finally { setCarregando(false) }
  }
  useEffect(() => { carregar() }, [])
  useEffect(() => {
    if (gestao) Promise.all([listarEquipe(), listarOrdens()]).then(([e, o]) => {
      setEquipe(e.filter(t => t.perfil === 'TECNICO' && t.ativo !== false))
      setOrdens(o.filter(os => ['RECEBIDO', 'EM_ANALISE', 'AGUARDANDO_PECA'].includes(os.status)))
    }).catch(e => setErro(mensagemDeErro(e, 'Não foi possível carregar técnicos e OSs.')))
  }, [gestao])
  useEffect(() => {
    let ativo = true
    setVinculados([]); setErroVinculo('')
    if (form.osId) listarTecnicosDaOs(form.osId).then(ids => { if (ativo) setVinculados(ids) }).catch(e => { if (ativo) setErroVinculo(mensagemDeErro(e, 'Não foi possível consultar os técnicos da OS.')) })
    return () => { ativo = false }
  }, [form.osId])
  async function salvar(e) {
    e.preventDefault(); setErro('')
    if (new Date(form.fim) <= new Date(form.inicio)) { setErro('O fim deve ser posterior ao início.'); return }
    setOcupado(true)
    try {
      const dados = { titulo: form.titulo.trim(), descricao: form.descricao, local: form.local, tecnicoId: Number(form.tecnicoId), osId: form.osId ? Number(form.osId) : null, setorId: form.osId ? null : editando?.setorId || null, inicio: new Date(form.inicio).toISOString(), fim: new Date(form.fim).toISOString(), versao: editando?.versao }
      if (editando) await atualizarAgendamento(editando.id, dados)
      else await criarAgendamento(dados)
      setAberto(false); setEditando(null); setForm(vazio); await carregar()
    } catch (e) { setErro(mensagemDeErro(e, 'Não foi possível salvar o compromisso.')) }
    finally { setOcupado(false) }
  }
  async function status(item, novoStatus) {
    setOcupado(true); setErro('')
    try { await alterarStatusAgendamento(item.id, { status: novoStatus, versao: item.versao }); await carregar() }
    catch (e) { setErro(mensagemDeErro(e, 'Não foi possível atualizar o compromisso.')) }
    finally { setOcupado(false) }
  }
  const campo = (k, v) => setForm(f => ({ ...f, [k]: v, ...(k === 'osId' ? { tecnicoId: '' } : {}) }))
  const tecnicos = form.osId ? equipe.filter(t => vinculados.includes(t.id)) : equipe
  return <div className="content">
    <div className="page-heading"><h1>Agenda técnica</h1>{gestao && <button className="btn-primary" onClick={() => { setForm(vazio); setEditando(null); setAberto(true) }}>Novo compromisso</button>}</div>
    <p>{gestao ? 'Visitas, acessos remotos e serviços externos da equipe.' : 'Seus compromissos de atendimento.'} Sem filtro de período, são exibidos os compromissos do mês atual.</p>
    <form className="card filter-grid" onSubmit={e => { e.preventDefault(); carregar() }}>
      {gestao && <div className="field"><label>Técnico</label><select value={filtro.tecnicoId} onChange={e => setFiltro(f => ({ ...f, tecnicoId: e.target.value }))}><option value="">Todos</option>{equipe.map(t => <option key={t.id} value={t.id}>{t.nome}</option>)}</select></div>}
      <div className="field"><label>Situação</label><select value={filtro.status} onChange={e => setFiltro(f => ({ ...f, status: e.target.value }))}><option value="">Todas</option><option value="AGENDADO">Agendado</option><option value="CONCLUIDO">Concluído</option><option value="CANCELADO">Cancelado</option></select></div>
      {['inicio', 'fim'].map(k => <div className="field" key={k}><label>{k === 'inicio' ? 'De' : 'Até'}</label><input type="date" value={filtro[k]} onChange={e => setFiltro(f => ({ ...f, [k]: e.target.value }))} /></div>)}
      <button className="btn-primary" disabled={carregando || ocupado}>Filtrar</button>
    </form>
    {erro && <p className="erro" role="alert">{erro}</p>}
    {aberto && <form className="card form-stack" onSubmit={salvar}>
      <h2>{editando ? 'Editar compromisso' : 'Novo compromisso'}</h2>
      <div className="field"><label>Título</label><input required maxLength={160} value={form.titulo} onChange={e => campo('titulo', e.target.value)} /></div>
      <div className="field"><label>OS vinculada (opcional)</label><select value={form.osId} onChange={e => campo('osId', e.target.value)}><option value="">Sem OS</option>{ordens.map(o => <option key={o.id} value={o.id}>#{o.id} · {o.setorNome}</option>)}</select></div>
      <div className="field"><label>Técnico responsável</label><select required value={form.tecnicoId} onChange={e => campo('tecnicoId', e.target.value)}><option value="">Selecione</option>{tecnicos.map(t => <option key={t.id} value={t.id}>{t.nome}</option>)}</select></div>
      {form.osId && !tecnicos.length && <p>Vincule um técnico à OS pela triagem antes de agendar.</p>}
      {erroVinculo && <p className="erro" role="alert">{erroVinculo}</p>}
      <div className="filter-grid">{['inicio', 'fim'].map(k => <div key={k} className="field"><label>{k === 'inicio' ? 'Início' : 'Fim'} (horário local)</label><input required type="datetime-local" value={form[k]} onChange={e => campo(k, e.target.value)} /></div>)}</div>
      <div className="field"><label>Local / modalidade de atendimento</label><input maxLength={255} value={form.local} onChange={e => campo('local', e.target.value)} /></div>
      <div className="field"><label>Descrição</label><textarea value={form.descricao} onChange={e => campo('descricao', e.target.value)} /></div>
      <div className="actions"><button className="btn-primary" disabled={ocupado}>Salvar compromisso</button><button type="button" className="btn-secondary" disabled={ocupado} onClick={() => setAberto(false)}>Cancelar edição</button></div>
    </form>}
    {carregando ? <p>Carregando agenda...</p> : !itens.length ? <p>Nenhum compromisso no período.</p> : <div className="agenda-list">{itens.map(item => <article className="card" key={item.id}>
      <div className="page-heading"><h2>{item.titulo}</h2><span className="badge badge-analise">{{ AGENDADO: 'Agendado', CONCLUIDO: 'Concluído', CANCELADO: 'Cancelado' }[item.status]}</span></div>
      <p>{formatarDataHora(item.inicio)} até {formatarDataHora(item.fim)}<br />{item.tecnicoNome} · {item.local || 'Local não informado'}{item.setorNome && ` · ${item.setorNome}`}</p>
      {item.descricao && <p style={{ whiteSpace: 'pre-wrap' }}>{item.descricao}</p>}
      {item.osId && <Link to={`/ordens-servico/${item.osId}`}>Abrir OS #{item.osId}</Link>}
      {gestao && item.status === 'AGENDADO' && <div className="actions">
        <button disabled={ocupado} className="btn-secondary" onClick={() => { setEditando(item); setForm({ ...item, osId: item.osId || '', inicio: localData(item.inicio), fim: localData(item.fim), descricao: item.descricao || '', local: item.local || '' }); setAberto(true) }}>Editar</button>
        <button disabled={ocupado} className="btn-primary" onClick={() => status(item, 'CONCLUIDO')}>Concluir</button>
        <button disabled={ocupado} className="btn-secondary" onClick={() => status(item, 'CANCELADO')}>Cancelar compromisso</button>
      </div>}
    </article>)}</div>}
  </div>
}
