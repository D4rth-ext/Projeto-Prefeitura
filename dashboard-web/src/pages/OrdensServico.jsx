import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { pesquisarOrdens, listarEquipe } from '../api/ordensServico'
import { buscarSetoresPublicos } from '../api/setores'
import { mensagemDeErro } from '../api/erro'
import StatusBadge from '../components/StatusBadge'
import { useAuth } from '../context/AuthContext'
import { CATEGORIAS, MODALIDADES, PRIORIDADES, rotulo, formatarData, formatarPrazo } from '../utils/osLabels'

const ABERTAS = ['RECEBIDO', 'EM_ANALISE', 'AGUARDANDO_PECA']

export default function OrdensServico() {
  const { usuario } = useAuth()
  const navigate = useNavigate()
  const [ordens, setOrdens] = useState([])
  const [busca, setBusca] = useState('')
  const [ordem, setOrdem] = useState('recentes')
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)
  const [filtros, setFiltros] = useState({ id: '', setorId: '', patrimonio: '', tecnicoId: '', status: '', modalidade: '', backupEmAberto: '', inicio: '', fim: '', atrasadas: '' })
  const [consulta, setConsulta] = useState({})
  const [pagina, setPagina] = useState(0)
  const [total, setTotal] = useState(0)
  const [paginas, setPaginas] = useState(0)
  const [setores, setSetores] = useState([])
  const [equipe, setEquipe] = useState([])

  const ehSolicitante = usuario?.perfil === 'SOLICITANTE'
  const podeAbrir = usuario?.perfil !== 'TECNICO'

  useEffect(() => {
    let ativo = true
    setCarregando(true); setErro(null)
    pesquisarOrdens({ ...consulta, pagina, tamanho: 20 })
      .then(d => { if (ativo) { setOrdens(d.conteudo); setTotal(d.totalElementos); setPaginas(d.totalPaginas) } })
      .catch(err => { if (ativo) { setOrdens([]); setTotal(0); setPaginas(0); setErro(mensagemDeErro(err, 'Não foi possível carregar as ordens de serviço.')) } })
      .finally(() => { if (ativo) setCarregando(false) })
    return () => { ativo = false }
  }, [consulta, pagina])
  useEffect(() => {
    buscarSetoresPublicos().then(setSetores).catch(() => {})
    if (['ADMIN', 'ATENDENTE'].includes(usuario?.perfil)) listarEquipe().then(l => setEquipe(l.filter(t => t.perfil === 'TECNICO'))).catch(() => {})
  }, [usuario?.perfil])

  const resumo = useMemo(() => ({
    emAberto: ordens.filter((os) => ABERTAS.includes(os.status)).length,
    aguardandoPeca: ordens.filter((os) => os.status === 'AGUARDANDO_PECA').length,
    atrasadas: ordens.filter((os) => os.atrasada).length
  }), [ordens])

  const filtradas = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    // O backend devolve do mais novo pro mais antigo; aqui dá pra inverter.
    const ordenadas = [...ordens].sort((x, y) => {
      const dif = new Date(x.abertaEm) - new Date(y.abertaEm)
      return ordem === 'antigos' ? dif : -dif
    })
    if (!termo) return ordenadas
    return ordenadas.filter((os) =>
      [String(os.id), os.descricaoProblema, os.setorNome, os.solicitanteNome, os.patrimonioInformado, rotulo(CATEGORIAS, os.categoria)]
        .some((campo) => campo?.toLowerCase().includes(termo))
    )
  }, [ordens, busca, ordem])

  const celula = { padding: '13px 14px', borderBottom: '1px solid var(--border)' }

  return (
    <div>
      <header className="ticket-heading"><div><h1>{ehSolicitante ? 'Meus chamados' : 'Chamados'}</h1><p className="muted">Acompanhe as solicitações de suporte</p></div>{podeAbrir && <Link to="/ordens-servico/nova" className="btn-primary">+ Novo chamado</Link>}</header>

      <div className="content">

        <p>{total} OSs encontradas · Indicadores e busca textual referentes à página atual.</p>
        <div className="ticket-stats">
          {[['Total encontrado', total], ['Recebidos nesta página', ordens.filter(o => o.status === 'RECEBIDO').length], ['Em atendimento nesta página', ordens.filter(o => ['EM_ANALISE', 'AGUARDANDO_PECA'].includes(o.status)).length], ['Resolvidos nesta página', ordens.filter(o => ['FINALIZADO', 'ENTREGUE'].includes(o.status)).length]].map(([label, value]) => <div className="card" key={label}><span className="ticket-stat-label">{label}</span><p className="ticket-stat-value">{carregando ? '—' : value}</p></div>)}
        </div>
        <div className="ticket-tools">
          <input aria-label="Buscar nesta página" placeholder="Buscar nesta página por descrição, solicitante ou nº..." value={busca} onChange={e => setBusca(e.target.value)} />
          <div className="quick-filters" aria-label="Filtrar por status">{[['', 'Todos'], ['RECEBIDO', 'Recebidos'], ['EM_ANALISE', 'Em análise'], ['FINALIZADO', 'Finalizados']].map(([status, label]) => <button key={status} type="button" aria-pressed={(consulta.status || '') === status} onClick={() => { setPagina(0); setFiltros(f => ({ ...f, status })); setConsulta(c => ({ ...c, status: status || undefined })) }}>{label}</button>)}</div>
          <select aria-label="Ordenar página" value={ordem} onChange={e => setOrdem(e.target.value)}><option value="recentes">Mais recentes</option><option value="antigos">Mais antigos</option></select>
        </div>
        <details className="advanced-filters"><summary>Filtros avançados</summary>
        <form className="card filter-grid" onSubmit={e => { e.preventDefault(); setPagina(0); setConsulta(Object.fromEntries(Object.entries(filtros).filter(([, v]) => v !== ''))) }}>
          <div className="field"><label>Nº da OS</label><input type="number" min="1" value={filtros.id} onChange={e => setFiltros(f => ({ ...f, id: e.target.value }))} /></div>
          {!ehSolicitante && <div className="field"><label>Setor</label><select value={filtros.setorId} onChange={e => setFiltros(f => ({ ...f, setorId: e.target.value }))}><option value="">Todos</option>{setores.map(s => <option key={s.id} value={s.id}>{s.nome}</option>)}</select></div>}
          <div className="field"><label>Patrimônio</label><input value={filtros.patrimonio} onChange={e => setFiltros(f => ({ ...f, patrimonio: e.target.value }))} /></div>
          {!!equipe.length && <div className="field"><label>Técnico</label><select value={filtros.tecnicoId} onChange={e => setFiltros(f => ({ ...f, tecnicoId: e.target.value }))}><option value="">Todos</option>{equipe.map(t => <option key={t.id} value={t.id}>{t.nome}</option>)}</select></div>}
          <div className="field"><label>Status</label><select value={filtros.status} onChange={e => setFiltros(f => ({ ...f, status: e.target.value }))}><option value="">Todos</option>{Object.entries({ RECEBIDO: 'Recebido', EM_ANALISE: 'Em análise', AGUARDANDO_PECA: 'Aguardando peça', FINALIZADO: 'Finalizado', ENTREGUE: 'Entregue', CANCELADO: 'Cancelado' }).map(([v, t]) => <option key={v} value={v}>{t}</option>)}</select></div>
          <div className="field"><label>Modalidade</label><select value={filtros.modalidade} onChange={e => setFiltros(f => ({ ...f, modalidade: e.target.value }))}><option value="">Todas</option>{Object.entries(MODALIDADES).map(([v, t]) => <option key={v} value={v}>{t}</option>)}</select></div>
          {['inicio', 'fim'].map(k => <div key={k} className="field"><label>{k === 'inicio' ? 'Abertura desde' : 'Abertura até'}</label><input type="date" value={filtros[k]} onChange={e => setFiltros(f => ({ ...f, [k]: e.target.value }))} /></div>)}
          <label className="check"><input type="checkbox" checked={filtros.backupEmAberto === 'true'} onChange={e => setFiltros(f => ({ ...f, backupEmAberto: e.target.checked ? 'true' : '' }))} />Backup em aberto</label>
          <label className="check"><input type="checkbox" checked={filtros.atrasadas === 'true'} onChange={e => setFiltros(f => ({ ...f, atrasadas: e.target.checked ? 'true' : '' }))} />Somente atrasadas</label>
          <button className="btn-primary" disabled={carregando}>Aplicar filtros</button>
          <button className="btn-secondary" type="button" onClick={() => { setFiltros(f => Object.fromEntries(Object.keys(f).map(k => [k, '']))); setConsulta({}); setPagina(0); setBusca('') }}>Limpar filtros</button>
        </form>
        </details>
        <div className="card ticket-table" style={{ padding: 0, overflow: 'hidden' }}>
          {carregando && <p style={{ padding: 18, color: 'var(--text-secondary)' }}>Carregando...</p>}
          {erro && <p style={{ padding: 18, color: 'var(--red)' }}>{erro}</p>}
          {!carregando && !erro && filtradas.length === 0 && (
            <p style={{ padding: 18, color: 'var(--text-secondary)' }}>
              {ordens.length === 0 ? 'Nenhuma ordem de serviço por aqui ainda.' : 'Nenhuma OS encontrada para essa busca.'}
            </p>
          )}
          {!carregando && !erro && filtradas.length > 0 && (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ background: 'var(--page-bg)' }}>
                    {['Nº / abertura', 'Chamado', 'Setor / solicitante', 'Categoria', 'Atendimento', 'Prazo', 'Status'].map((col) => (
                      <th key={col} style={{ textAlign: 'left', padding: '10px 14px', fontSize: 11, color: 'var(--text-secondary)', borderBottom: '1px solid var(--border)', whiteSpace: 'nowrap' }}>
                        {col}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {filtradas.map((os) => (
                    <tr key={os.id} onClick={() => navigate(`/ordens-servico/${os.id}`)} style={{ cursor: 'pointer' }}>
                      <td style={celula}>
                        <div style={{ fontSize: 15, fontWeight: 500, display: 'flex', alignItems: 'center', gap: 6 }}>
                          {os.atrasada && <span title="Prazo vencido" style={{ width: 7, height: 7, borderRadius: '50%', background: 'var(--red)' }} />}
                          <Link to={`/ordens-servico/${os.id}`}>#{os.id}</Link>
                        </div>
                        <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>
                          Aberta em {formatarData(os.abertaEm)}{os.patrimonioInformado ? ` · ${os.patrimonioInformado}` : ''}
                        </div>
                      </td>
                      <td style={celula}><div className="ticket-title" title={os.descricaoProblema}>{os.descricaoProblema}</div></td>
                      <td style={celula}>
                        <div>{os.setorNome}</div>
                        <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{os.solicitanteNome}</div>
                      </td>
                      <td style={celula}>{rotulo(CATEGORIAS, os.categoria)}</td>
                      <td style={{ ...celula, color: 'var(--text-secondary)' }}>
                        {os.modalidade === 'A_DEFINIR' ? 'Aguardando triagem' : `${rotulo(MODALIDADES, os.modalidade)} · ${rotulo(PRIORIDADES, os.prioridade)}`}
                      </td>
                      <td style={{ ...celula, color: os.atrasada ? 'var(--red)' : 'var(--text-secondary)' }}>{formatarPrazo(os.prazoPrevisto)}</td>
                      <td style={celula}><StatusBadge status={os.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        <div className="actions"><button className="btn-secondary" disabled={carregando || pagina === 0} onClick={() => setPagina(p => p - 1)}>Anterior</button><span>Página {paginas ? pagina + 1 : 0} de {paginas}</span><button className="btn-secondary" disabled={carregando || pagina + 1 >= paginas} onClick={() => setPagina(p => p + 1)}>Próxima</button></div>
      </div>
    </div>
  )
}
