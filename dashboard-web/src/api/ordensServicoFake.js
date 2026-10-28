// Dados fake — a Ordem de Serviço ainda não existe no backend (é a próxima
// parte, V2). Guardamos no localStorage pra simular persistência: uma OS
// aberta continua aparecendo depois, inclusive pro Solicitante que abriu.
// Quando o Aluno A publicar as rotas reais, troque as chamadas em
// OrdensServico.jsx / NovaOS.jsx de volta para api/ordensServico.js.

const CHAVE = 'ordens-fake'

const SEED = [
  { id: 1, numero: '0142', dataAbertura: '03/12/2024', setor: 'Financeiro', equipamento: 'Impressora', patrimonio: 'PAT-00231', tecnico: 'Diego Souza', status: 'FINALIZADO', abertoPor: null },
  { id: 2, numero: '0141', dataAbertura: '02/12/2024', setor: 'Recursos humanos', equipamento: 'Notebook', patrimonio: 'PAT-00198', tecnico: 'Aline Costa', status: 'EM_ANALISE', abertoPor: null },
  { id: 3, numero: '0140', dataAbertura: '01/12/2024', setor: 'Recepção', equipamento: 'Roteador', patrimonio: 'PAT-00177', tecnico: 'Diego Souza', status: 'AGUARDANDO_PECA', abertoPor: null },
  { id: 4, numero: '0139', dataAbertura: '29/11/2024', setor: 'Financeiro', equipamento: 'Monitor', patrimonio: 'PAT-00150', tecnico: null, status: 'RECEBIDO', abertoPor: null },
  { id: 5, numero: '0138', dataAbertura: '27/11/2024', setor: 'Comercial', equipamento: 'Notebook', patrimonio: 'PAT-00121', tecnico: 'Aline Costa', status: 'ENTREGUE', abertoPor: null }
]

function lerTudo() {
  const salvo = localStorage.getItem(CHAVE)
  if (salvo) return JSON.parse(salvo)
  localStorage.setItem(CHAVE, JSON.stringify(SEED))
  return SEED
}

function salvarTudo(lista) {
  localStorage.setItem(CHAVE, JSON.stringify(lista))
}

function espera(ms = 300) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

// usuario: passe o usuário logado (do useAuth()) pra filtrar o que um
// Solicitante vê (só as próprias OS) — Admin/Técnico/Atendente veem tudo.
export async function buscarOrdensFake(filtros = {}, usuario = null) {
  await espera()
  let lista = lerTudo()

  if (usuario?.perfil === 'SOLICITANTE') {
    lista = lista.filter((os) => os.abertoPor === usuario.email)
  }

  const termo = (filtros.termo || '').toLowerCase()
  if (!termo) return lista

  return lista.filter((os) =>
    [os.numero, os.setor, os.equipamento, os.patrimonio, os.tecnico].some((campo) =>
      campo?.toLowerCase().includes(termo)
    )
  )
}

export async function buscarResumoFake(usuario = null) {
  await espera()
  let lista = lerTudo()
  if (usuario?.perfil === 'SOLICITANTE') {
    lista = lista.filter((os) => os.abertoPor === usuario.email)
  }
  return {
    emAberto: lista.filter((os) => os.status !== 'ENTREGUE' && os.status !== 'CANCELADO').length,
    aguardandoPeca: lista.filter((os) => os.status === 'AGUARDANDO_PECA').length,
    atrasadas: lista.some((os) => os.status === 'AGUARDANDO_PECA') ? 1 : 0
  }
}

// RF04.1 — transição de status. Só Admin/Técnico devem chamar isso
// (a tela já restringe quem vê o seletor de status).
export async function alterarStatusFake(id, novoStatus) {
  await espera(150)
  const lista = lerTudo()
  const atualizada = lista.map((os) => (os.id === id ? { ...os, status: novoStatus } : os))
  salvarTudo(atualizada)
  return atualizada.find((os) => os.id === id)
}

export async function abrirOrdemFake(dados, usuario = null) {
  await espera()
  const lista = lerTudo()
  const nova = {
    id: Date.now(),
    numero: String(1000 + lista.length + 1),
    dataAbertura: new Date().toLocaleDateString('pt-BR'),
    setor: dados.setorNome || 'Setor selecionado',
    equipamento: dados.equipamento,
    patrimonio: dados.patrimonio || '—',
    tecnico: null,
    status: 'RECEBIDO',
    abertoPor: usuario?.email || null
  }
  salvarTudo([nova, ...lista])
  return nova
}
