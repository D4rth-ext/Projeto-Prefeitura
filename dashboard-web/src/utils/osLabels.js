// Rótulos em português dos enums do backend (módulo OS)

export const CATEGORIAS = {
  NAO_LIGA: 'Computador não liga',
  LENTO: 'Computador lento',
  SEM_REDE: 'Sem rede / internet',
  IMPRESSORA: 'Impressora',
  SISTEMA: 'Sistema / programa',
  EMAIL: 'E-mail',
  OUTRO: 'Outro'
}

export const MODALIDADES = {
  A_DEFINIR: 'A definir',
  REMOTO: 'Remoto',
  PRESENCIAL: 'Presencial',
  BANCADA: 'Bancada'
}

export const PRIORIDADES = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
  URGENTE: 'Urgente'
}

export const RESPOSTAS = { SIM: 'Sim', NAO: 'Não', NAO_SEI: 'Não sei' }

export const MOTIVOS_BANCADA = {
  NAO_LIGA_SEM_ACESSO: 'Não liga / sem acesso',
  FALHA_HARDWARE: 'Falha de hardware',
  TENTATIVA_REMOTA_SEM_SUCESSO: 'Tentativa remota sem sucesso',
  SERVICO_EXIGE_BANCADA: 'Serviço exige bancada',
  OUTRO: 'Outro'
}

export const TIPOS_REGISTRO = {
  DIAGNOSTICO: 'Diagnóstico',
  ACAO: 'Ação realizada',
  OBSERVACAO: 'Observação',
  ORIENTACAO: 'Orientação ao solicitante'
}

export const rotulo = (mapa, valor) => (valor ? mapa[valor] || valor : '—')

export function formatarData(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('pt-BR')
}

export function formatarDataHora(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

// prazoPrevisto vem como "2026-10-15" (LocalDate) — não usar new Date() direto,
// senão o fuso desloca o dia.
export function formatarPrazo(dataIso) {
  if (!dataIso) return '—'
  const [a, m, d] = dataIso.split('-')
  return `${d}/${m}/${a}`
}
