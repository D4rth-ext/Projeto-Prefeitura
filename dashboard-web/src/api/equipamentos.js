import { api } from './client'

// Busca por tipo, marca, patrimonio/serie ou setor
export function buscarEquipamentos({ setorId, busca } = {}) {
  return api.get('/equipamentos', { params: { setorId, busca } }).then((r) => r.data)
}

// Lista fixa (Computador, Notebook, Monitor, Impressora, Nobreak,
// Switch/Roteador, Telefone IP, Outro) — GET /api/v1/tipos-equipamento
export function buscarTiposEquipamento() {
  return api.get('/tipos-equipamento').then((r) => r.data)
}

export function buscarEquipamentoPorId(id) {
  return api.get(`/equipamentos/${id}`).then((r) => r.data)
}

// RF02.1 — registro do equipamento (setorId, tipoId, marca, modelo,
// patrimonio OU numeroSerie, observacoes). RF02.3 — vinculo com o setor.
// Acessorios/backup/defeito relatado ficam pra quando a Ordem de Servico
// existir no backend (comentário do Aluno A na migration: "fica na V2").
export function criarEquipamento(dados) {
  return api.post('/equipamentos', dados).then((r) => r.data)
}

export function atualizarEquipamento(id, dados) {
  return api.put(`/equipamentos/${id}`, dados).then((r) => r.data)
}

export function excluirEquipamento(id) {
  return api.delete(`/equipamentos/${id}`).then((r) => r.data)
}
