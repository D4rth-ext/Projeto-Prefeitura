// A API sempre responde erro nesse formato (ver contrato do backend):
// { timestamp, status, erro, mensagem, caminho, campos: [{ campo, mensagem }] }
// Essa função extrai a melhor mensagem pra mostrar pro usuário.
export function mensagemDeErro(err, fallback = 'Algo deu errado. Tente novamente.') {
  const dados = err?.response?.data
  if (!dados) return fallback

  if (Array.isArray(dados.campos) && dados.campos.length > 0) {
    return dados.campos.map((c) => c.mensagem).join(' ')
  }

  return dados.mensagem || fallback
}
