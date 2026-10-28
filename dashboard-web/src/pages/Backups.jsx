import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listarDescartesPendentes } from '../api/ordensServico'
import { mensagemDeErro } from '../api/erro'
import { formatarDataHora } from '../utils/osLabels'

export default function Backups() {
  const [itens, setItens] = useState([])
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)
  async function carregar() {
    setCarregando(true); setErro('')
    try { setItens(await listarDescartesPendentes()) }
    catch (e) { setErro(mensagemDeErro(e, 'Não foi possível consultar os descartes.')) }
    finally { setCarregando(false) }
  }
  useEffect(() => { carregar() }, [])
  return <div className="content">
    <div className="page-heading"><h1>Descarte de backups</h1><button className="btn-secondary" disabled={carregando} onClick={carregar}>Atualizar</button></div>
    <p>Backups de equipamentos devolvidos, aguardando eliminação dos dados da mídia do TI. Prazo: até 30 dias após a devolução.</p>
    <p>Elimine os dados na mídia antes de confirmar o descarte na OS. A confirmação registra a operação; o sistema não apaga os arquivos da mídia.</p>
    {erro && <p role="alert" className="erro">{erro}</p>}
    {carregando ? <p>Carregando...</p> : !erro && <>
      <p>{itens.length} pendentes · {itens.filter(b => b.descarteAtrasado).length} com prazo vencido</p>
      <div className="agenda-list">{itens.map(b => <article className="card" key={b.osId}>
        <h2>OS #{b.osId} {b.descarteAtrasado && <span className="badge badge-cancelado">Descarte atrasado</span>}</h2>
        <p>Mídia do TI: {b.midiaTi}<br />Limite de descarte: {formatarDataHora(b.limiteDescarteEm)}</p>
        <Link to={`/ordens-servico/${b.osId}`}>Abrir OS para registrar descarte</Link>
      </article>)}</div>
    </>}
  </div>
}
