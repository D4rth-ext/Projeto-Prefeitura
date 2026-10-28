import { api } from './client'

// AINDA NÃO EXISTE NO BACKEND — está na lista de pendências do contrato do
// Aluno A ("Redefinição de senha"). Documentado no README o que ele precisa
// criar. Enquanto não existir, essas chamadas vão dar 404.

// POST /auth/esqueci-senha — público. Recebe o e-mail e, se existir conta,
// envia um link/código de redefinição por e-mail. Deve responder 200 sempre
// (mesmo se o e-mail não existir), pra não revelar quais e-mails têm conta.
export function solicitarRecuperacao(email) {
  return api.post('/auth/esqueci-senha', { email }).then((r) => r.data)
}

// POST /auth/redefinir-senha — público. Recebe o token que veio no e-mail
// mais a nova senha (e confirmação, seguindo o mesmo padrão do /auth/registro).
export function redefinirSenha({ token, novaSenha, confirmarNovaSenha }) {
  return api.post('/auth/redefinir-senha', { token, novaSenha, confirmarNovaSenha }).then((r) => r.data)
}
