import { api } from './client'

export function alterarSetorUsuario(id, setorId) {
  return api.patch(`/usuarios/${id}/setor`, { setorId }).then(r => r.data)
}

// PATCH /usuarios/:id/senha — Admin redefine a senha de outro usuário.
export function redefinirSenhaUsuario(id, dados) {
  return api.patch(`/usuarios/${id}/senha`, dados)
}

// GET /usuarios — só ADMIN. Lista toda a equipe (e solicitantes, se vierem juntos)
export function buscarUsuarios() {
  return api.get('/usuarios').then((r) => r.data)
}

export function buscarCadastrosPendentes() {
  return api.get('/usuarios/cadastros-pendentes').then((r) => r.data)
}

export function decidirCadastro(id, decisao) {
  return api.patch(`/usuarios/${id}/cadastro`, { decisao }).then((r) => r.data)
}

// POST /usuarios — só ADMIN. Cria Técnico, Atendente ou outro Admin.
// perfil aceita só ADMIN | ATENDENTE | TECNICO (nunca SOLICITANTE — isso é
// exclusivo do autocadastro em /auth/registro).
export function criarUsuario({ nome, email, senha, perfil, telefone }) {
  return api.post('/usuarios', { nome, email, senha, perfil, telefone }).then((r) => r.data)
}

export function editarUsuario(id, dados) {
  return api.put(`/usuarios/${id}`, dados).then((r) => r.data)
}

// DELETE /usuarios/:id — só ADMIN. Na prática INATIVA a conta (não apaga do
// banco). Serve para equipe e para solicitantes. Se for um técnico com OS em
// análise e sem outro técnico vinculado, o backend recusa (422) pedindo para
// designar outro técnico antes.
export function inativarUsuario(id) {
  return api.delete(`/usuarios/${id}`)
}

