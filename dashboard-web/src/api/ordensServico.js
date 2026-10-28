import { api } from './client'

export const pesquisarOrdens = (params) => api.get('/os/busca', { params }).then(r => r.data)
export const consultarRecebimento = (id) => api.get(`/os/${id}/recebimento`).then(r => r.data)
export const receberEquipamento = (id, dados) => api.post(`/os/${id}/recebimento`, dados).then(r => r.data)
export const consultarBackup = (id) => api.get(`/os/${id}/backup`).then(r => r.data)
export const atualizarBackup = (id, dados) => api.patch(`/os/${id}/backup`, dados).then(r => r.data)
export const descartarBackup = (id, dados) => api.post(`/os/${id}/backup/descarte`, dados).then(r => r.data)
export const listarSolicitantes = () => api.get('/solicitantes').then(r => r.data)
export const listarDescartesPendentes = () => api.get('/os/backups/descarte-pendente').then(r => r.data)

// Backend v3 — módulo de Ordem de Serviço (/api/v1/os)

// Lista já filtrada pelo perfil: Admin/Atendente veem todas, Solicitante só
// as suas, Técnico só as que está vinculado.
export function listarOrdens() {
  return api.get('/os').then((r) => r.data)
}

export function buscarOrdem(id) {
  return api.get(`/os/${id}`).then((r) => r.data)
}

// Solicitante abre em nome próprio; Admin/Atendente precisam informar
// solicitanteId. Técnico não pode abrir (403). O setor vem do solicitante.
export function abrirOrdem(dados) {
  return api.post('/os', dados).then((r) => r.data)
}

// Só Admin/Atendente. { modalidade, prioridade, tecnicosIds, motivoBancada,
// justificativa, motivoRemocao, versao }
export function triarOrdem(id, dados) {
  return api.patch(`/os/${id}/triagem`, dados).then((r) => r.data)
}

// Só equipe. { status, prazoPrevisto, justificativa, resumoSolucao,
// retiradoPor, motivoBancada, registroPecaId, versao } — cada transição exige
// campos diferentes (ver OsDetalhe.jsx).
export function alterarStatus(id, dados) {
  return api.patch(`/os/${id}/status`, dados).then((r) => r.data)
}

export function alterarPrazo(id, dados) {
  return api.patch(`/os/${id}/prazo`, dados).then((r) => r.data)
}

export function listarRegistros(id) {
  return api.get(`/os/${id}/registros`).then((r) => r.data)
}

// { tipo: DIAGNOSTICO|ACAO|OBSERVACAO|ORIENTACAO, texto, versao }
export function criarRegistro(id, dados) {
  return api.post(`/os/${id}/registros`, dados).then((r) => r.data)
}

export function listarEventos(id) {
  return api.get(`/os/${id}/eventos`).then((r) => r.data)
}

// IDs dos técnicos atualmente vinculados à OS
export function listarTecnicosDaOs(id) {
  return api.get(`/os/${id}/tecnicos`).then((r) => r.data)
}

// Técnicos/atendentes/admins ativos — só Admin e Atendente
export function listarEquipe() {
  return api.get('/equipe').then((r) => r.data)
}

// AINDA NÃO EXISTE NO BACKEND (v3). Proposta de contrato — ver README:
// PATCH /os/{id}/solicitacao, permitido ao solicitante dono da OS (e à equipe)
// enquanto a OS está RECEBIDO e sem triagem. Corpo: os mesmos campos da
// abertura + versao. Responde com a OS atualizada.
export function editarChamado(id, dados) {
  return api.patch(`/os/${id}/solicitacao`, dados).then((r) => r.data)
}
