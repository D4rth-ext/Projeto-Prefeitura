import { useEffect, useState } from 'react'
import { listarEventos, pesquisarOrdens } from '../api/ordensServico'
import { mensagemDeErro } from '../api/erro'
import StatusBadge from '../components/StatusBadge'
import { formatarDataHora } from '../utils/osLabels'

const TIPOS = { STATUS: 'Mudança de status', MODALIDADE: 'Modalidade', PRIORIDADE: 'Prioridade', PRAZO: 'Prazo', TECNICO_ENTROU: 'Técnico vinculado', TECNICO_SAIU: 'Técnico removido', RECEBIMENTO: 'Recebimento', BACKUP: 'Backup' }

function Eventos({ os, voltar }) {
  const [eventos, setEventos] = useState(null)
  const [erro, setErro] = useState('')
  useEffect(() => {
    let ativo = true
    listarEventos(os.id)
      .then(dados => { if (ativo) setEventos(dados) })
      .catch(e => { if (ativo) setErro(mensagemDeErro(e, 'Não foi possível carregar o histórico.')) })
    return () => { ativo = false }
  }, [os.id])

  return <section className="card os-history-card">
    <div className="page-heading"><h2>Histórico da OS #{os.id}</h2><button className="btn-secondary" type="button" onClick={voltar}>Voltar à lista</button></div>
    {erro ? <p role="alert" className="erro">{erro}</p> : eventos === null ? <p role="status">Carregando histórico…</p> : !eventos.length ? <p>Nenhuma alteração registrada.</p> : (
      <ol className="historico-eventos">
        {eventos.map(evento => <li key={evento.id}>
          <span className="muted">{formatarDataHora(evento.criadoEm)} · {evento.autorNome}</span>
          <div><strong>{TIPOS[evento.tipo] || evento.tipo}</strong>{evento.valorAnterior || evento.valorNovo ? `: ${[evento.valorAnterior, evento.valorNovo].filter(Boolean).join(' → ')}` : ''}</div>
          {evento.motivo && <p>{evento.motivo}</p>}
        </li>)}
      </ol>
    )}
  </section>
}

export default function Historico() {
  const [numero, setNumero] = useState('')
  const [consulta, setConsulta] = useState({ id: undefined, pagina: 0 })
  const [dados, setDados] = useState(null)
  const [erro, setErro] = useState('')
  const [selecionada, setSelecionada] = useState(null)
  useEffect(() => {
    let ativo = true
    setDados(null); setErro('')
    pesquisarOrdens({ ...consulta, tamanho: 20 })
      .then(resultado => { if (ativo) setDados(resultado) })
      .catch(e => { if (ativo) setErro(mensagemDeErro(e, 'Não foi possível carregar os chamados.')) })
    return () => { ativo = false }
  }, [consulta])

  return <main className="content">
    <h1>Histórico</h1>
    <p className="muted">Consulte as alterações registradas nos chamados.</p>
    {selecionada ? <Eventos key={selecionada.id} os={selecionada} voltar={() => setSelecionada(null)} /> : <>
      <form className="ticket-tools" onSubmit={e => { e.preventDefault(); setConsulta({ id: numero || undefined, pagina: 0 }) }}>
        <input type="number" min="1" step="1" aria-label="Número da OS" placeholder="Buscar pelo número da OS" value={numero} onChange={e => setNumero(e.target.value)} />
        <button className="btn-primary" type="submit">Buscar</button>
        {consulta.id && <button className="btn-secondary" type="button" onClick={() => { setNumero(''); setConsulta({ id: undefined, pagina: 0 }) }}>Limpar busca</button>}
      </form>
      {erro ? <p role="alert" className="erro">{erro}</p> : dados === null ? <p role="status">Carregando chamados…</p> : !dados.conteudo.length ? <p>Nenhum chamado encontrado.</p> : <>
        <ul className="historico-os-lista card" aria-label="Chamados disponíveis">
          {dados.conteudo.map(os => <li key={os.id}><button type="button" onClick={() => setSelecionada(os)}>
            <span><strong>OS #{os.id}</strong><span className="historico-os-descricao">{os.descricaoProblema}</span></span><StatusBadge status={os.status} />
          </button></li>)}
        </ul>
        {dados.totalPaginas > 1 && <div className="actions">
          <button className="btn-secondary" type="button" disabled={consulta.pagina === 0} onClick={() => setConsulta(c => ({ ...c, pagina: c.pagina - 1 }))}>Anterior</button>
          <span>Página {consulta.pagina + 1} de {dados.totalPaginas}</span>
          <button className="btn-secondary" type="button" disabled={consulta.pagina + 1 >= dados.totalPaginas} onClick={() => setConsulta(c => ({ ...c, pagina: c.pagina + 1 }))}>Próxima</button>
        </div>}
      </>}
    </>}
  </main>
}
