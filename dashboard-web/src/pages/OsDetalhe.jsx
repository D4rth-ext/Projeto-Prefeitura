import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  buscarOrdem, triarOrdem, alterarStatus, listarRegistros, criarRegistro,
  listarEventos, listarTecnicosDaOs, listarEquipe, editarChamado
} from '../api/ordensServico'
import { mensagemDeErro } from '../api/erro'
import StatusBadge from '../components/StatusBadge'
import CamposChamado from '../components/CamposChamado'
import OperacoesOs from '../components/OperacoesOs'
import { useAuth } from '../context/AuthContext'
import {
  CATEGORIAS, MODALIDADES, PRIORIDADES, RESPOSTAS, MOTIVOS_BANCADA, TIPOS_REGISTRO,
  rotulo, formatarDataHora, formatarPrazo
} from '../utils/osLabels'

const textarea = { width: '100%', padding: 8, borderRadius: 6, border: '1px solid var(--border)', fontFamily: 'inherit', fontSize: 13 }

// Transições possíveis a partir de cada status (ver FluxoOsService no backend).
// "campos" diz o que o formulário precisa pedir; quem pode executar cada uma é
// validado pelo backend (técnico vinculado / admin / atendente).
function transicoesDe(os) {
  const t = []
  const bancada = os.modalidade === 'BANCADA'
  switch (os.status) {
    case 'RECEBIDO':
      t.push({ para: 'EM_ANALISE', rotulo: 'Iniciar análise', campos: ['prazoPrevisto'] })
      break
    case 'EM_ANALISE':
      t.push({ para: 'AGUARDANDO_PECA', rotulo: 'Aguardar peça', campos: ['justificativa', 'registroPecaId'] })
      t.push({ para: 'FINALIZADO', rotulo: 'Finalizar', campos: ['resumoSolucao'] })
      if (os.modalidade === 'REMOTO' || os.modalidade === 'PRESENCIAL') {
        t.push({ para: 'RECEBIDO', rotulo: 'Escalar para bancada', campos: ['justificativa', 'motivoBancada'] })
      }
      break
    case 'AGUARDANDO_PECA':
      t.push({ para: 'EM_ANALISE', rotulo: 'Retomar análise', campos: [] })
      break
    case 'FINALIZADO':
      t.push({ para: 'EM_ANALISE', rotulo: 'Reabrir', campos: ['justificativa'] })
      if (bancada && os.recebidoEm) t.push({ para: 'ENTREGUE', rotulo: 'Registrar entrega', campos: ['retiradoPor'] })
      break
    default:
      break
  }
  if (!['ENTREGUE', 'CANCELADO'].includes(os.status)) {
    t.push({ para: 'CANCELADO', rotulo: 'Cancelar OS', campos: os.recebidoEm ? ['justificativa', 'retiradoPor'] : ['justificativa'], perigo: true })
  }
  return t
}

function Linha({ rotuloCampo, children }) {
  return (
    <div className="detail-field">
      <div className="detail-label">{rotuloCampo}</div>
      <div className="detail-value">{children || '—'}</div>
    </div>
  )
}

export default function OsDetalhe() {
  const { id } = useParams()
  const { usuario } = useAuth()
  const perfil = usuario?.perfil
  const ehEquipe = perfil && perfil !== 'SOLICITANTE'
  const ehGestao = perfil === 'ADMIN' || perfil === 'ATENDENTE'

  const [os, setOs] = useState(null)
  const [abaEscolhida, setAba] = useState(null)
  const [registrando, setRegistrando] = useState(false)
  const [registros, setRegistros] = useState([])
  const [eventos, setEventos] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)

  // --- ação de status ---
  const [transicao, setTransicao] = useState(null)
  const [campos, setCampos] = useState({})
  const [msgStatus, setMsgStatus] = useState(null)
  const [enviandoStatus, setEnviandoStatus] = useState(false)

  // --- triagem ---
  const [equipe, setEquipe] = useState([])
  const [triagem, setTriagem] = useState({ modalidade: 'REMOTO', prioridade: 'MEDIA', tecnicosIds: [], motivoBancada: '', justificativa: '', motivoRemocao: '' })
  const [tecnicosAtuais, setTecnicosAtuais] = useState([])
  const [msgTriagem, setMsgTriagem] = useState(null)
  const [enviandoTriagem, setEnviandoTriagem] = useState(false)

  // --- edição do chamado pelo solicitante ---
  const [editando, setEditando] = useState(false)
  const [formEdicao, setFormEdicao] = useState(null)
  const [msgEdicao, setMsgEdicao] = useState(null)
  const [enviandoEdicao, setEnviandoEdicao] = useState(false)

  // --- registros ---
  const [novoRegistro, setNovoRegistro] = useState({ tipo: 'OBSERVACAO', texto: '' })
  const [msgRegistro, setMsgRegistro] = useState(null)
  const [enviandoRegistro, setEnviandoRegistro] = useState(false)

  const carregar = useCallback(async () => {
    setErro(null)
    try {
      const dados = await buscarOrdem(id)
      setOs(dados)
      setRegistros(await listarRegistros(id))
      if (ehEquipe) setEventos(await listarEventos(id).catch(() => []))
      if (ehEquipe) {
        const ids = await listarTecnicosDaOs(id).catch(() => [])
        setTecnicosAtuais(ids)
        setTriagem((t) => ({
          ...t,
          tecnicosIds: ids,
          modalidade: dados.modalidade !== 'A_DEFINIR' ? dados.modalidade : t.modalidade,
          prioridade: dados.prioridade || t.prioridade,
          motivoBancada: dados.motivoBancada || ''
        }))
      }
    } catch (err) {
      setErro(mensagemDeErro(err, 'Não foi possível carregar essa OS.'))
    } finally {
      setCarregando(false)
    }
  }, [id, ehEquipe])

  useEffect(() => { carregar() }, [carregar])

  useEffect(() => {
    if (ehGestao) listarEquipe().then((l) => setEquipe(l.filter((u) => u.perfil === 'TECNICO'))).catch(() => setEquipe([]))
  }, [ehGestao])

  if (carregando) return <div className="content"><p style={{ color: 'var(--text-secondary)' }}>Carregando...</p></div>
  if (erro) return (
    <div className="content">
      <p style={{ color: 'var(--red)' }}>{erro}</p>
      <Link to="/" style={{ color: 'var(--green)' }}>Voltar</Link>
    </div>
  )

  const aba = abaEscolhida || (ehGestao && os.modalidade === 'A_DEFINIR' ? 'triagem' : 'atendimento')
  const abas = [['atendimento', ehEquipe ? 'Atendimento' : 'Acompanhamento'], ['dados', 'Dados do chamado'], ...(ehGestao && ['RECEBIDO', 'EM_ANALISE', 'AGUARDANDO_PECA'].includes(os.status) ? [['triagem', 'Triagem']] : []), ...(ehEquipe && os.modalidade === 'BANCADA' ? [['recebimento', 'Recebimento'], ['backup', 'Backup']] : []), ...(ehGestao && os.status === 'FINALIZADO' && os.recebidoEm && os.modalidade === 'BANCADA' ? [['entrega', 'Entrega']] : []), ...(ehEquipe ? [['historico', 'Histórico']] : [])]
  function escolherAba(valor) {
    setAba(valor)
    setTransicao(valor === 'entrega' ? transicoesDe(os).find(t => t.para === 'ENTREGUE') : null)
    setCampos({}); setMsgStatus(null); setRegistrando(false)
  }
  const encerrada = ['ENTREGUE', 'CANCELADO'].includes(os.status)
  const transicoes = ehEquipe && !encerrada ? transicoesDe(os).filter(t => {
    if (t.para === 'ENTREGUE') return ehGestao
    if (t.para === 'CANCELADO') return true
    if (t.para === 'RECEBIDO') return true
    if (perfil === 'ATENDENTE') return false
    if (os.status === 'RECEBIDO' && t.para === 'EM_ANALISE') return os.modalidade !== 'A_DEFINIR' && (os.modalidade !== 'BANCADA' || os.recebidoEm)
    return true
  }) : []
  const apontamentos = registros.filter((r) => r.tipo === 'DIAGNOSTICO' || r.tipo === 'ACAO')

  function abrirTransicao(t) {
    setRegistrando(false)
    setAba('atendimento')
    setTransicao(t)
    setCampos({})
    setMsgStatus(null)
  }

  async function enviarStatus(e) {
    e.preventDefault()
    setEnviandoStatus(true)
    setMsgStatus(null)
    const corpo = { status: transicao.para, versao: os.versao }
    transicao.campos.forEach((c) => {
      if (campos[c]) corpo[c] = c === 'registroPecaId' ? Number(campos[c]) : campos[c]
    })
    try {
      setOs(await alterarStatus(id, corpo))
      setEventos(await listarEventos(id).catch(() => []))
      setTransicao(null)
      setAba('atendimento')
    } catch (err) {
      setMsgStatus(mensagemDeErro(err, 'Não foi possível alterar o status.'))
    } finally {
      setEnviandoStatus(false)
    }
  }

  async function enviarTriagem(e) {
    e.preventDefault()
    setEnviandoTriagem(true)
    setMsgTriagem(null)
    const corpo = {
      modalidade: triagem.modalidade,
      prioridade: triagem.prioridade,
      tecnicosIds: triagem.tecnicosIds,
      versao: os.versao
    }
    if (triagem.modalidade === 'BANCADA') corpo.motivoBancada = triagem.motivoBancada || undefined
    if (triagem.justificativa) corpo.justificativa = triagem.justificativa
    if (triagem.motivoRemocao) corpo.motivoRemocao = triagem.motivoRemocao
    try {
      setOs(await triarOrdem(id, corpo))
      setTecnicosAtuais(triagem.tecnicosIds)
      setEventos(await listarEventos(id).catch(() => []))
      setMsgTriagem({ ok: true, texto: 'Triagem salva.' })
    } catch (err) {
      setMsgTriagem({ ok: false, texto: mensagemDeErro(err, 'Não foi possível salvar a triagem.') })
    } finally {
      setEnviandoTriagem(false)
    }
  }

  async function enviarRegistro(e) {
    e.preventDefault()
    setEnviandoRegistro(true)
    setMsgRegistro(null)
    try {
      await criarRegistro(id, { ...novoRegistro, versao: os.versao })
      setNovoRegistro({ tipo: 'OBSERVACAO', texto: '' })
      // Registrar altera a versão da OS — recarrega tudo pra pegar a atual
      await carregar()
      setRegistrando(false)
    } catch (err) {
      setMsgRegistro(mensagemDeErro(err, 'Não foi possível salvar o registro.'))
    } finally {
      setEnviandoRegistro(false)
    }
  }

  function abrirEdicao() {
    setFormEdicao({
      categoria: os.categoria || '',
      descricaoProblema: os.descricaoProblema || '',
      patrimonioInformado: os.patrimonioInformado || '',
      computadorLiga: os.computadorLiga || '',
      temRede: os.temRede || '',
      consegueAnyDesk: os.consegueAnyDesk || '',
      contato: os.contato || '',
      melhorHorario: os.melhorHorario || ''
    })
    setMsgEdicao(null)
    setEditando(true)
  }

  async function enviarEdicao(e) {
    e.preventDefault()
    setEnviandoEdicao(true)
    setMsgEdicao(null)
    // Campos opcionais vazios viram null (limpa o valor)
    const corpo = { versao: os.versao }
    Object.entries(formEdicao).forEach(([k, v]) => { corpo[k] = v === '' ? null : v })
    try {
      setOs(await editarChamado(id, corpo))
      setEditando(false)
    } catch (err) {
      const status = err.response?.status
      setMsgEdicao(
        status === 404 || status === 405
          ? 'A edição de chamado ainda não está disponível no servidor (rota pendente no backend).'
          : mensagemDeErro(err, 'Não foi possível salvar as alterações.')
      )
    } finally {
      setEnviandoEdicao(false)
    }
  }

  function alternarTecnico(tid) {
    setTriagem((t) => ({
      ...t,
      tecnicosIds: t.tecnicosIds.includes(tid) ? t.tecnicosIds.filter((x) => x !== tid) : [...t.tecnicosIds, tid]
    }))
  }

  // O próprio solicitante pode corrigir o chamado enquanto ele não foi triado
  const podeEditarChamado = false // A API atual ainda não oferece edição da solicitação.

  const removendoTecnico = tecnicosAtuais.some((tid) => !triagem.tecnicosIds.includes(tid))

  return (
    <main className="os-detail-page">
      <header className="os-detail-header">
        <div className="os-detail-title">
          <Link to="/" aria-label="Voltar para ordens de serviço" className="os-back-link">
            <i className="ti ti-arrow-left" aria-hidden="true" />
          </Link>
          <div className="os-detail-title-copy">
            <span className="os-detail-eyebrow">Detalhes da ordem de serviço</span>
            <h1>OS <span>#{os.id}</span></h1>
          </div>
          <div className="os-detail-status">
            <StatusBadge status={os.status} />
            {os.atrasada && <span className="badge badge-cancelado">Atrasada</span>}
          </div>
        </div>
      </header>

      <nav className="os-tabs content" aria-label="Seções do chamado">{abas.map(([key, label]) => <button key={key} type="button" className="tab-button" aria-pressed={aba === key} onClick={() => escolherAba(key)}>{label}</button>)}</nav>
      <div className={`content os-detail-grid ${ehEquipe ? '' : 'os-detail-grid-requester'}`}>

        {/* ---------- coluna esquerda ---------- */}
        <div className="os-detail-main">

          <section className="card" hidden={aba !== 'dados'}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', margin: '0 0 14px' }}>
              <h2 style={{ fontSize: 14, fontWeight: 500, margin: 0 }}>Chamado</h2>
              {podeEditarChamado && !editando && (
                <button type="button" onClick={abrirEdicao}
                  style={{ background: 'transparent', border: '1px solid var(--border)', borderRadius: 6, padding: '5px 12px', fontSize: 12, cursor: 'pointer', color: 'var(--green)' }}>
                  <i className="ti ti-pencil" /> Editar chamado
                </button>
              )}
            </div>

            {editando ? (
              <form onSubmit={enviarEdicao} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <CamposChamado form={formEdicao} campo={(n, v) => setFormEdicao((f) => ({ ...f, [n]: v }))} />
                {msgEdicao && <p style={{ color: 'var(--red)', fontSize: 12, margin: 0 }}>{msgEdicao}</p>}
                <div style={{ display: 'flex', gap: 8 }}>
                  <button type="submit" className="btn-primary" disabled={enviandoEdicao}>{enviandoEdicao ? 'Salvando...' : 'Salvar alterações'}</button>
                  <button type="button" onClick={() => setEditando(false)}
                    style={{ background: 'transparent', border: '1px solid var(--border)', borderRadius: 6, padding: '9px 14px', fontSize: 13, cursor: 'pointer' }}>Cancelar</button>
                </div>
              </form>
            ) : (
            <>
            <p className="os-description" style={{ margin: '0 0 18px', whiteSpace: 'pre-wrap' }}>{os.descricaoProblema}</p>
            <div className="os-fields-grid">
              <Linha rotuloCampo="Categoria">{rotulo(CATEGORIAS, os.categoria)}</Linha>
              <Linha rotuloCampo="Setor">{os.setorNome}</Linha>
              <Linha rotuloCampo="Solicitante">{os.solicitanteNome}</Linha>
              <Linha rotuloCampo="Aberta por">{os.abertoPorNome}</Linha>
              <Linha rotuloCampo="Aberta em">{formatarDataHora(os.abertaEm)}</Linha>
              <Linha rotuloCampo="Patrimônio informado">{os.patrimonioInformado}</Linha>
              <Linha rotuloCampo="Contato">{os.contato}</Linha>
              <Linha rotuloCampo="Melhor horário">{os.melhorHorario}</Linha>
              <Linha rotuloCampo="Computador liga?">{os.computadorLiga && RESPOSTAS[os.computadorLiga]}</Linha>
              <Linha rotuloCampo="Tem rede?">{os.temRede && RESPOSTAS[os.temRede]}</Linha>
              <Linha rotuloCampo="Consegue usar AnyDesk?">{os.consegueAnyDesk && RESPOSTAS[os.consegueAnyDesk]}</Linha>
            </div>
            </>
            )}
          </section>

          <section className="card" hidden={aba !== 'dados'}>
            <h2 style={{ fontSize: 14, fontWeight: 500, margin: '0 0 14px' }}>Atendimento</h2>
            <div className="os-fields-grid">
              <Linha rotuloCampo="Modalidade">{os.modalidade === 'A_DEFINIR' ? 'Aguardando triagem' : rotulo(MODALIDADES, os.modalidade)}</Linha>
              <Linha rotuloCampo="Prioridade">{rotulo(PRIORIDADES, os.prioridade)}</Linha>
              <Linha rotuloCampo="Prazo previsto">{formatarPrazo(os.prazoPrevisto)}</Linha>
              <Linha rotuloCampo="Motivo da bancada">{os.motivoBancada && rotulo(MOTIVOS_BANCADA, os.motivoBancada)}</Linha>
              {os.resumoSolucao && <Linha rotuloCampo="Resumo da solução">{os.resumoSolucao}</Linha>}
              {os.motivoCancelamento && <Linha rotuloCampo="Motivo do cancelamento">{os.motivoCancelamento}</Linha>}
              {os.retiradoPor && <Linha rotuloCampo="Retirado por">{os.retiradoPor}</Linha>}
              {os.finalizadoEm && <Linha rotuloCampo="Finalizada em">{formatarDataHora(os.finalizadoEm)}</Linha>}
              {os.entregueEm && <Linha rotuloCampo="Entregue em">{formatarDataHora(os.entregueEm)}</Linha>}
            </div>
          </section>

          <section className="card" hidden={aba !== 'atendimento'}>
            <h2 style={{ fontSize: 14, fontWeight: 500, margin: '0 0 14px' }}>Registros do atendimento</h2>
            {registros.length === 0 && <p style={{ color: 'var(--text-secondary)', margin: 0 }}>Nenhum registro ainda.</p>}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {registros.map((r) => (
                <div key={r.id} style={{ borderLeft: '3px solid var(--green)', paddingLeft: 12 }}>
                  <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>
                    {rotulo(TIPOS_REGISTRO, r.tipo)} · {r.autorNome} · {formatarDataHora(r.criadoEm)}
                  </div>
                  <div style={{ whiteSpace: 'pre-wrap' }}>{r.texto}</div>
                </div>
              ))}
            </div>

            {ehEquipe && !encerrada && registrando && (
              <form id="registro-os" onSubmit={enviarRegistro} style={{ marginTop: 18, paddingTop: 16, borderTop: '1px solid var(--border)', display: 'flex', flexDirection: 'column', gap: 10 }}>
                <div className="field">
                  <label>Novo registro</label>
                  <select value={novoRegistro.tipo} onChange={(e) => setNovoRegistro({ ...novoRegistro, tipo: e.target.value })}>
                    {Object.entries(TIPOS_REGISTRO).filter(([v]) => perfil !== 'ATENDENTE' || !['DIAGNOSTICO', 'ACAO'].includes(v)).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
                  </select>
                </div>
                <textarea rows={3} style={textarea} required value={novoRegistro.texto}
                  onChange={(e) => setNovoRegistro({ ...novoRegistro, texto: e.target.value })} placeholder="Escreva o registro" />
                {msgRegistro && <p style={{ color: 'var(--red)', fontSize: 12, margin: 0 }}>{msgRegistro}</p>}
                <div><button type="submit" className="btn-primary" disabled={enviandoRegistro}>{enviandoRegistro ? 'Salvando...' : 'Salvar registro'}</button><button type="button" className="btn-secondary" onClick={() => setRegistrando(false)}>Voltar</button></div>
              </form>
            )}
          </section>
        </div>

        {/* ---------- coluna direita (só equipe) ---------- */}
        {ehEquipe && (
        <aside className="os-detail-sidebar" aria-label="Ações e acompanhamento da OS">
          <OperacoesOs os={os} perfil={perfil} recarregar={carregar} aba={aba} />

          {transicao && ['atendimento', 'entrega'].includes(aba) && (
            <section className="card os-transition-card">
              <h2>{transicao.rotulo}</h2>
              {transicao && (
                <form onSubmit={enviarStatus} style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
                  {transicao.campos.includes('prazoPrevisto') && (
                    <div className="field">
                      <label>Prazo previsto</label>
                      <input type="date" required value={campos.prazoPrevisto || ''} onChange={(e) => setCampos({ ...campos, prazoPrevisto: e.target.value })} />
                    </div>
                  )}
                  {transicao.campos.includes('registroPecaId') && (
                    <div className="field">
                      <label>Registro que descreve a peça faltante</label>
                      <select required value={campos.registroPecaId || ''} onChange={(e) => setCampos({ ...campos, registroPecaId: e.target.value })}>
                        <option value="">Selecione um diagnóstico/ação</option>
                        {apontamentos.map((r) => <option key={r.id} value={r.id}>{rotulo(TIPOS_REGISTRO, r.tipo)}: {r.texto.slice(0, 50)}</option>)}
                      </select>
                      {apontamentos.length === 0 && (
                        <p style={{ fontSize: 11, color: 'var(--text-secondary)', margin: '4px 0 0' }}>Registre antes um diagnóstico ou ação descrevendo a peça.</p>
                      )}
                    </div>
                  )}
                  {transicao.campos.includes('motivoBancada') && (
                    <div className="field">
                      <label>Motivo da bancada</label>
                      <select required value={campos.motivoBancada || ''} onChange={(e) => setCampos({ ...campos, motivoBancada: e.target.value })}>
                        <option value="">Selecione</option>
                        {Object.entries(MOTIVOS_BANCADA).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
                      </select>
                    </div>
                  )}
                  {transicao.campos.includes('resumoSolucao') && (
                    <div className="field">
                      <label>Resumo da solução</label>
                      <textarea rows={3} style={textarea} required value={campos.resumoSolucao || ''} onChange={(e) => setCampos({ ...campos, resumoSolucao: e.target.value })} />
                      <p style={{ fontSize: 11, color: 'var(--text-secondary)', margin: '4px 0 0' }}>Exige ao menos um diagnóstico registrado.</p>
                    </div>
                  )}
                  {transicao.campos.includes('justificativa') && (
                    <div className="field">
                      <label>Justificativa</label>
                      <textarea rows={2} style={textarea} required value={campos.justificativa || ''} onChange={(e) => setCampos({ ...campos, justificativa: e.target.value })} />
                    </div>
                  )}
                  {transicao.campos.includes('retiradoPor') && (
                    <div className="field">
                      <label>Retirado por</label>
                      <input type="text" required maxLength={120} value={campos.retiradoPor || ''} onChange={(e) => setCampos({ ...campos, retiradoPor: e.target.value })} />
                    </div>
                  )}

                  {msgStatus && <p style={{ color: 'var(--red)', fontSize: 12, margin: 0 }}>{msgStatus}</p>}
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button type="submit" className="btn-primary" disabled={enviandoStatus}>{enviandoStatus ? 'Salvando...' : `Confirmar: ${transicao.rotulo}`}</button>
                    <button type="button" onClick={() => setTransicao(null)} style={{ background: 'transparent', border: '1px solid var(--border)', borderRadius: 6, padding: '9px 14px', fontSize: 13, cursor: 'pointer' }}>Cancelar</button>
                  </div>
                </form>
              )}
            </section>
          )}

          {ehGestao && ['RECEBIDO', 'EM_ANALISE', 'AGUARDANDO_PECA'].includes(os.status) && (
            <section className="card os-triage-card" hidden={aba !== 'triagem'}>
              <h2 style={{ fontSize: 14, fontWeight: 500, margin: '0 0 14px' }}>Triagem</h2>
              <form onSubmit={enviarTriagem} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                <div className="field">
                  <label>Modalidade</label>
                  <select disabled={os.status !== 'RECEBIDO' || !!os.recebidoEm} value={triagem.modalidade} onChange={(e) => setTriagem({ ...triagem, modalidade: e.target.value })}>
                    {['REMOTO', 'PRESENCIAL', 'BANCADA'].map((m) => <option key={m} value={m}>{MODALIDADES[m]}</option>)}
                  </select>
                </div>
                <div className="field">
                  <label>Prioridade</label>
                  <select value={triagem.prioridade} onChange={(e) => setTriagem({ ...triagem, prioridade: e.target.value })}>
                    {Object.entries(PRIORIDADES).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
                  </select>
                </div>

                <div className="field">
                  <label>Técnicos responsáveis</label>
                  {equipe.length === 0 && <p style={{ fontSize: 12, color: 'var(--text-secondary)', margin: 0 }}>Nenhum técnico ativo cadastrado.</p>}
                  {equipe.map((tec) => (
                    <label key={tec.id} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, marginBottom: 4 }}>
                      <input type="checkbox" checked={triagem.tecnicosIds.includes(tec.id)} onChange={() => alternarTecnico(tec.id)} />
                      {tec.nome}
                    </label>
                  ))}
                </div>

                {triagem.modalidade === 'BANCADA' && (
                  <>
                    <div className="field">
                      <label>Motivo da bancada</label>
                      <select required value={triagem.motivoBancada} onChange={(e) => setTriagem({ ...triagem, motivoBancada: e.target.value })}>
                        <option value="">Selecione</option>
                        {Object.entries(MOTIVOS_BANCADA).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
                      </select>
                    </div>
                    <div className="field">
                      <label>Justificativa</label>
                      <textarea rows={2} style={textarea} required value={triagem.justificativa} onChange={(e) => setTriagem({ ...triagem, justificativa: e.target.value })} />
                    </div>
                  </>
                )}

                {removendoTecnico && (
                  <div className="field">
                    <label>Motivo da remoção do técnico</label>
                    <input type="text" required value={triagem.motivoRemocao} onChange={(e) => setTriagem({ ...triagem, motivoRemocao: e.target.value })} />
                  </div>
                )}

                {msgTriagem && <p style={{ color: msgTriagem.ok ? 'var(--final-text)' : 'var(--red)', fontSize: 12, margin: 0 }}>{msgTriagem.texto}</p>}
                <div><button type="submit" className="btn-primary" disabled={enviandoTriagem || triagem.tecnicosIds.length === 0}>{enviandoTriagem ? 'Salvando...' : 'Salvar triagem'}</button></div>
              </form>
            </section>
          )}

          {ehEquipe && (
            <section className="card os-history-card" hidden={aba !== 'historico'}>
              <h2 style={{ fontSize: 14, fontWeight: 500, margin: '0 0 14px' }}>Histórico</h2>{eventos.length === 0 && <p>Nenhuma alteração registrada.</p>}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {eventos.map((ev) => (
                  <div key={ev.id} style={{ fontSize: 12 }}>
                    <div style={{ color: 'var(--text-secondary)' }}>{formatarDataHora(ev.criadoEm)} · {ev.autorNome}</div>
                    <div>
                      <strong>{ev.tipo}</strong>: {ev.valorAnterior ? `${ev.valorAnterior} → ` : ''}{ev.valorNovo}
                      {ev.motivo ? ` (${ev.motivo})` : ''}
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </aside>
        )}
      </div>
      {ehEquipe && aba === 'atendimento' && !registrando && !transicao && <footer className="os-action-footer">
        <details className="more-actions"><summary>Mais ações</summary><div className="more-actions-menu">
          {['ADMIN', 'TECNICO'].includes(perfil) && !encerrada && os.status !== 'FINALIZADO' && <button type="button" className="btn-secondary" onClick={() => escolherAba('prazo')}>Alterar prazo</button>}
          {transicoes.filter(t => !['FINALIZADO', 'ENTREGUE'].includes(t.para) && !(t.para === 'EM_ANALISE' && os.status !== 'FINALIZADO')).map(t => <button type="button" className="btn-secondary" key={t.rotulo} onClick={() => abrirTransicao(t)}>{t.rotulo}</button>)}
        </div></details>
        <div className="actions">
          {!encerrada && <button type="button" className="btn-secondary" onClick={() => { setRegistrando(true); setMsgRegistro(null) }}>Registrar atendimento</button>}
          {transicoes.filter(t => ['FINALIZADO', 'ENTREGUE'].includes(t.para) || (t.para === 'EM_ANALISE' && os.status !== 'FINALIZADO')).map(t => <button key={t.rotulo} type="button" className="btn-primary" onClick={() => abrirTransicao(t)}>{t.rotulo}</button>)}
        </div>
      </footer>}
    </main>
  )
}
