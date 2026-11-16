import { useEffect, useState } from 'react'
import { buscarRelatorioOs } from '../api/relatorios'
import { listarTodosSetores } from '../api/setores'
import { mensagemDeErro } from '../api/erro'
import { MODALIDADES, rotulo } from '../utils/osLabels'

const STATUS = { RECEBIDO: 'Recebido', EM_ANALISE: 'Em análise', AGUARDANDO_PECA: 'Aguardando peça', FINALIZADO: 'Finalizado', ENTREGUE: 'Entregue', CANCELADO: 'Cancelado' }
const dataPt = data => data ? data.split('-').reverse().join('/') : 'Sem limite'

export default function Relatorios() {
  const [filtros, setFiltros] = useState({ inicio: '', fim: '', setorId: '', status: '' })
  const [setores, setSetores] = useState([])
  const [dados, setDados] = useState(null)
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  useEffect(() => { listarTodosSetores().then(setSetores).catch(() => setSetores([])) }, [])

  async function consultar(e, sobrescrito) {
    e?.preventDefault()
    setCarregando(true); setErro('')
    try {
      const params = sobrescrito ?? Object.fromEntries(Object.entries(filtros).filter(([, valor]) => valor !== ''))
      setDados(await buscarRelatorioOs(params))
    } catch (err) { setErro(mensagemDeErro(err, 'Não foi possível gerar o relatório.')) }
    finally { setCarregando(false) }
  }

  useEffect(() => { consultar() }, [])

  return <main>
    <header className="ticket-heading"><div><h1>Relatórios de OS</h1><p className="muted">Totais por período de abertura, setor e status atual.</p></div>{dados && <button type="button" className="btn-secondary no-print" onClick={() => window.print()}>Imprimir</button>}</header>
    <div className="content">
      <form className="card filter-grid no-print" onSubmit={consultar}>
        <div className="field"><label>Aberta desde</label><input type="date" value={filtros.inicio} onChange={e => setFiltros(f => ({ ...f, inicio: e.target.value }))} /></div>
        <div className="field"><label>Aberta até</label><input type="date" value={filtros.fim} onChange={e => setFiltros(f => ({ ...f, fim: e.target.value }))} /></div>
        <div className="field"><label>Setor</label><select value={filtros.setorId} onChange={e => setFiltros(f => ({ ...f, setorId: e.target.value }))}><option value="">Todos</option>{setores.map(s => <option key={s.id} value={s.id}>{s.nome}</option>)}</select></div>
        <div className="field"><label>Status atual</label><select value={filtros.status} onChange={e => setFiltros(f => ({ ...f, status: e.target.value }))}><option value="">Todos</option>{Object.entries(STATUS).map(([v, texto]) => <option key={v} value={v}>{texto}</option>)}</select></div>
        <button className="btn-primary" disabled={carregando}>{carregando ? 'Gerando…' : 'Atualizar relatório'}</button>
        <button type="button" className="btn-secondary" disabled={carregando} onClick={() => { setFiltros({ inicio: '', fim: '', setorId: '', status: '' }); consultar(undefined, {}) }}>Limpar filtros</button>
      </form>
      {erro && <p role="alert" className="erro">{erro}</p>}
      {dados && <>
        <div className="ticket-stats">
          {[['Total de OS', dados.total], ['Atrasadas', dados.atrasadas], ['Período inicial', dataPt(dados.inicio)], ['Período final', dataPt(dados.fim)]].map(([label, valor]) => <div className="card" key={label}><span className="ticket-stat-label">{label}</span><p className="ticket-stat-value">{valor}</p></div>)}
        </div>
        <div className="filter-grid" style={{ alignItems: 'start' }}>
          <section className="card"><h2>Por status</h2>{Object.entries(dados.porStatus || {}).map(([status, total]) => <p key={status}>{STATUS[status] || status}: <strong>{total}</strong></p>)}</section>
          <section className="card"><h2>Por modalidade</h2>{Object.entries(dados.porModalidade || {}).map(([modalidade, total]) => <p key={modalidade}>{rotulo(MODALIDADES, modalidade)}: <strong>{total}</strong></p>)}</section>
          <section className="card"><h2>Por setor</h2>{dados.porSetor?.length ? dados.porSetor.map(s => <p key={s.setorId}>{s.setorNome}: <strong>{s.total}</strong></p>) : <p className="muted">Sem OS no período.</p>}</section>
        </div>
      </>}
    </div>
  </main>
}
