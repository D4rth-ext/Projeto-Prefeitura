import { useEffect, useState } from 'react'
import { buscarEquipamentos } from '../api/equipamentos'
import { alterarPrazo, consultarBackup, consultarRecebimento, receberEquipamento, atualizarBackup, descartarBackup } from '../api/ordensServico'
import { mensagemDeErro } from '../api/erro'
import { formatarDataHora } from '../utils/osLabels'

export default function OperacoesOs({ os, perfil, recarregar, aba = 'backup' }) {
  const gestao = ['ADMIN', 'ATENDENTE'].includes(perfil)
  const [equipamentos, setEquipamentos] = useState([])
  const [recebimento, setRecebimento] = useState(null)
  const [backup, setBackup] = useState(null)
  const [erro, setErro] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [confirmado, setConfirmado] = useState(false)
  const [form, setForm] = useState({ equipamentoId: '', acessorios: '', backupSolicitado: '', escopoBackup: '', midiaTi: '', cienciaRiscoConfirmada: false })
  const [prazo, setPrazo] = useState({ prazoPrevisto: '', justificativa: '' })
  useEffect(() => {
    let ativo = true
    setErro(''); setBackup(null); setRecebimento(null)
    async function carregar() {
      try {
        if (os.modalidade === 'BANCADA') {
          if (os.recebidoEm) {
            const [r, b] = await Promise.all([consultarRecebimento(os.id), consultarBackup(os.id)])
            if (ativo) { setRecebimento(r); setBackup(b) }
          } else if (gestao) {
            const lista = await buscarEquipamentos({ setorId: os.setorId })
            if (ativo) setEquipamentos(lista.filter(e => e.ativo !== false))
          }
        }
      } catch (e) { if (ativo) setErro(mensagemDeErro(e, 'Não foi possível consultar o recebimento.')) }
    }
    carregar()
    return () => { ativo = false }
  }, [os.id, os.versao, os.modalidade, os.recebidoEm, os.setorId, gestao])
  async function executar(acao) {
    setOcupado(true); setErro('')
    try { await acao(); setConfirmado(false); await recarregar() }
    catch (e) { setErro(mensagemDeErro(e, 'Não foi possível salvar. Atualize a OS se ela foi alterada por outra pessoa.')) }
    finally { setOcupado(false) }
  }
  const campo = (nome, valor) => setForm(f => ({ ...f, [nome]: valor }))
  const situacoes = { PENDENTE: 'Pendente', CONCLUIDO: 'Concluído', RESTAURADO_DEVOLVIDO: 'Restaurado / devolvido' }
  return <>
    {os.modalidade === 'BANCADA' && ['recebimento', 'backup'].includes(aba) && <section className="card">
      <h2>{aba === 'recebimento' ? 'Recebimento do equipamento' : 'Protocolo de backup'}</h2>
      {recebimento && <p>Equipamento: {recebimento.patrimonio || recebimento.numeroSerie} · Entrada: {formatarDataHora(recebimento.recebidoEm)}<br />Acessórios: {recebimento.acessorios || 'Nenhum informado'}</p>}
      {aba === 'backup' && !os.recebidoEm && <p>O protocolo será registrado no recebimento do equipamento.</p>}
      {aba === 'backup' && backup && <>
        <p>{backup.solicitado ? `Backup: ${situacoes[backup.situacao] || backup.situacao}` : 'Backup não solicitado; ciência de risco registrada.'}</p>
        {backup.solicitado && <p>Escopo: {backup.escopo}<br />Mídia do TI: {backup.midiaTi}</p>}
        {backup.limiteDescarteEm && <p className={backup.descarteAtrasado ? 'erro' : ''}>Limite para descarte: {formatarDataHora(backup.limiteDescarteEm)}</p>}
        {backup.descartadoEm && <p>Descarte confirmado em {formatarDataHora(backup.descartadoEm)}</p>}
        {gestao && backup.solicitado && !backup.descartadoEm && backup.situacao !== 'RESTAURADO_DEVOLVIDO' && <button disabled={ocupado} className="btn-primary" onClick={() => executar(() => atualizarBackup(os.id, { situacao: backup.situacao === 'PENDENTE' ? 'CONCLUIDO' : 'RESTAURADO_DEVOLVIDO', versao: os.versao }))}>{backup.situacao === 'PENDENTE' ? 'Confirmar backup concluído' : 'Confirmar restauração / devolução'}</button>}
        {gestao && backup.solicitado && !backup.descartadoEm && backup.situacao === 'RESTAURADO_DEVOLVIDO' && os.entregueEm && <div>
          <label className="check"><input type="checkbox" checked={confirmado} onChange={e => setConfirmado(e.target.checked)} />Confirmo que os dados foram eliminados da mídia do TI.</label>
          <button className="btn-primary" disabled={ocupado || !confirmado} onClick={() => executar(() => descartarBackup(os.id, { descarteConfirmado: true, versao: os.versao }))}>Registrar descarte</button>
        </div>}
      </>}
      {gestao && aba === 'recebimento' && !os.recebidoEm && os.status === 'RECEBIDO' && <form className="form-stack" onSubmit={e => { e.preventDefault(); executar(() => receberEquipamento(os.id, { equipamentoId: Number(form.equipamentoId), acessorios: form.acessorios, backupSolicitado: form.backupSolicitado === 'SIM', escopoBackup: form.backupSolicitado === 'SIM' ? form.escopoBackup : null, midiaTi: form.backupSolicitado === 'SIM' ? form.midiaTi : null, cienciaRiscoConfirmada: form.backupSolicitado === 'NAO' && form.cienciaRiscoConfirmada, versao: os.versao })) }}>
        <div className="field"><label>Equipamento do setor</label><select required value={form.equipamentoId} onChange={e => campo('equipamentoId', e.target.value)}><option value="">Selecione</option>{equipamentos.map(e => <option key={e.id} value={e.id}>{e.patrimonio || e.numeroSerie} · {e.marca} {e.modelo}</option>)}</select></div>
        {!equipamentos.length && <p>Cadastre um equipamento ativo deste setor para registrar o recebimento.</p>}
        <div className="field"><label>Acessórios entregues</label><textarea value={form.acessorios} onChange={e => campo('acessorios', e.target.value)} /></div>
        <div className="field"><label>O solicitante pediu backup?</label><select required value={form.backupSolicitado} onChange={e => campo('backupSolicitado', e.target.value)}><option value="">Selecione</option><option value="SIM">Sim</option><option value="NAO">Não</option></select></div>
        {form.backupSolicitado === 'SIM' && <><div className="field"><label>Escopo do backup</label><textarea required value={form.escopoBackup} onChange={e => campo('escopoBackup', e.target.value)} /></div><div className="field"><label>Mídia do TI</label><input required maxLength={80} value={form.midiaTi} onChange={e => campo('midiaTi', e.target.value)} /></div></>}
        {form.backupSolicitado === 'NAO' && <label className="check"><input type="checkbox" required checked={form.cienciaRiscoConfirmada} onChange={e => campo('cienciaRiscoConfirmada', e.target.checked)} />O solicitante está ciente do risco de perda de dados e optou por não solicitar backup.</label>}
        <button className="btn-primary" disabled={ocupado}>Registrar recebimento</button>
      </form>}
    </section>}
    {aba === 'prazo' && ['ADMIN', 'TECNICO'].includes(perfil) && ['RECEBIDO', 'EM_ANALISE', 'AGUARDANDO_PECA'].includes(os.status) && <section className="card">
      <h2>Atualizar prazo</h2>
      <form className="form-stack" onSubmit={e => { e.preventDefault(); executar(() => alterarPrazo(os.id, { ...prazo, versao: os.versao })) }}>
        <div className="field"><label>Novo prazo previsto</label><input type="date" required value={prazo.prazoPrevisto} onChange={e => setPrazo(p => ({ ...p, prazoPrevisto: e.target.value }))} /></div>
        <div className="field"><label>Justificativa</label><textarea required value={prazo.justificativa} onChange={e => setPrazo(p => ({ ...p, justificativa: e.target.value }))} /></div>
        <button className="btn-primary" disabled={ocupado}>Salvar prazo</button>
      </form>
    </section>}
    {erro && <p role="alert" className="erro">{erro}</p>}
  </>
}
