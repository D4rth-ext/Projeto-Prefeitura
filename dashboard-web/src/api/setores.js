import { api } from './client'

// RF01.3 — pesquisa por nome, setor ou telefone
export function buscarSetores(busca = '') {
  return api.get('/setores', { params: { busca } }).then((r) => r.data)
}

// Lista enxuta (id, nome) para o select público da tela de Cadastro —
// não exige login, ver GET /api/v1/auth/setores-publicos
export function buscarSetoresPublicos() {
  return api.get('/auth/setores-publicos').then((r) => r.data)
}

export function buscarSetorPorId(id) {
  return api.get(`/setores/${id}`).then((r) => r.data)
}

// RF01.1 — cadastro de setor
export function criarSetor(dados) {
  return api.post('/setores', dados).then((r) => r.data)
}

// RF01.2 — edição
export function atualizarSetor(id, dados) {
  return api.put(`/setores/${id}`, dados).then((r) => r.data)
}

// RF01.2 — exclusão
export function excluirSetor(id) {
  return api.delete(`/setores/${id}`).then((r) => r.data)
}

// RF01.4 — histórico de chamados/atendimentos do setor
export function buscarHistoricoSetor(id) {
  return api.get(`/setores/${id}/historico`).then((r) => r.data)
}
