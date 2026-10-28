import { CATEGORIAS, RESPOSTAS } from '../utils/osLabels'

const textarea = { width: '100%', padding: 8, borderRadius: 6, border: '1px solid var(--border)', fontFamily: 'inherit', fontSize: 13 }

function SelectResposta({ label, valor, onChange }) {
  return (
    <div className="field">
      <label>{label}</label>
      <select value={valor} onChange={(e) => onChange(e.target.value)}>
        <option value="">—</option>
        {Object.entries(RESPOSTAS).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
      </select>
    </div>
  )
}

// Campos do chamado, usados na abertura (NovaOS) e na edição (OsDetalhe).
// form: { categoria, descricaoProblema, patrimonioInformado, computadorLiga,
//         temRede, consegueAnyDesk, contato, melhorHorario }
export default function CamposChamado({ form, campo }) {
  return (
    <>
      <div className="field">
        <label>O que está acontecendo?</label>
        <select value={form.categoria} onChange={(e) => campo('categoria', e.target.value)} required>
          <option value="">Selecione a categoria</option>
          {Object.entries(CATEGORIAS).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
        </select>
      </div>

      <div className="field">
        <label>Descreva o problema</label>
        <textarea
          rows={4}
          placeholder="Conte o que aconteceu, quando começou, mensagens de erro..."
          value={form.descricaoProblema}
          onChange={(e) => campo('descricaoProblema', e.target.value)}
          style={textarea}
          required
        />
      </div>

      <details className="optional-fields"><summary>Adicionar informações para ajudar o atendimento <span className="muted">· opcional</span></summary><div className="form-stack">
      <div className="field">
        <label>Patrimônio do equipamento (se souber)</label>
        <input type="text" maxLength={40} placeholder="Ex: 0123" value={form.patrimonioInformado}
          onChange={(e) => campo('patrimonioInformado', e.target.value)} />
      </div>

      <div className="form-columns">
        <SelectResposta label="O computador liga?" valor={form.computadorLiga} onChange={(v) => campo('computadorLiga', v)} />
        <SelectResposta label="Tem rede?" valor={form.temRede} onChange={(v) => campo('temRede', v)} />
        <SelectResposta label="Consegue usar AnyDesk?" valor={form.consegueAnyDesk} onChange={(v) => campo('consegueAnyDesk', v)} />
      </div>

      <div className="form-columns">
        <div className="field">
          <label>Contato (telefone / ramal)</label>
          <input type="text" maxLength={60} value={form.contato} onChange={(e) => campo('contato', e.target.value)} />
        </div>
        <div className="field">
          <label>Melhor horário</label>
          <input type="text" maxLength={100} placeholder="Ex: manhãs" value={form.melhorHorario}
            onChange={(e) => campo('melhorHorario', e.target.value)} />
        </div>
      </div>
      </div></details>
    </>
  )
}
