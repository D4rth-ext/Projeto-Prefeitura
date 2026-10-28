// RF04.1 — os 6 status possíveis da OS.
const CONFIG = {
  RECEBIDO: { classe: 'badge-pendente', icone: 'ti-hourglass', texto: 'Recebido / pendente' },
  EM_ANALISE: { classe: 'badge-analise', icone: 'ti-search', texto: 'Em análise' },
  AGUARDANDO_PECA: { classe: 'badge-peca', icone: 'ti-clock-pause', texto: 'Aguardando peça' },
  FINALIZADO: { classe: 'badge-finalizado', icone: 'ti-check', texto: 'Finalizado' },
  ENTREGUE: { classe: 'badge-entregue', icone: 'ti-package', texto: 'Entregue' },
  CANCELADO: { classe: 'badge-cancelado', icone: 'ti-x', texto: 'Cancelado' }
}

export const STATUS_OPCOES = Object.entries(CONFIG).map(([valor, cfg]) => ({ valor, texto: cfg.texto }))

export default function StatusBadge({ status }) {
  const cfg = CONFIG[status] ?? { classe: 'badge-pendente', icone: 'ti-help', texto: status }
  return (
    <span className={`badge ${cfg.classe}`}>
      <i className={`ti ${cfg.icone}`} />
      {cfg.texto}
    </span>
  )
}
