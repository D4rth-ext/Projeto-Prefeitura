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
//         temRede, consegueAnyDesk, anydeskId, contato, melhorHorario }
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
        <label>Telefone para contato</label>
        <input type="tel" inputMode="numeric" maxLength={20} value={form.contato} onChange={(e) => campo('contato', e.target.value.replace(/\D/g, ''))} required />
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

      <div className="field">
        <label>Você deseja solicitar backup dos seus dados?</label>
        <select value={form.backupSolicitado || ''} onChange={e => campo('backupSolicitado', e.target.value)} required>
          <option value="">Selecione uma opção</option>
          <option value="SIM">Sim, quero solicitar backup</option>
          <option value="NAO">Não quero solicitar backup</option>
        </select>
      </div>
      {form.backupSolicitado === 'SIM' && <div className="field">
        <label>O que você precisa incluir no backup?</label>
        <textarea rows={3} maxLength={2000} placeholder="Ex.: documentos da pasta Meus Documentos e arquivos de trabalho" value={form.escopoBackup || ''} onChange={e => campo('escopoBackup', e.target.value)} required />
        <small className="muted">Descreva as pastas ou os tipos de arquivo que precisam ser preservados.</small>
      </div>}
      {form.backupSolicitado === 'NAO' && <p className="muted">A equipe não fará cópia dos dados quando o equipamento for recebido para atendimento em bancada.</p>}

      <details className="optional-fields"><summary>Adicionar informações para ajudar o atendimento <span className="muted">· opcional</span></summary><div className="form-stack">
      <div className="field">
        <label>Patrimônio do equipamento (opcional)</label>
        <input type="text" maxLength={40} placeholder="Ex: 0123" value={form.patrimonioInformado}
          onChange={(e) => campo('patrimonioInformado', e.target.value)} />
        <small className="muted">Deixe em branco para serviços sem equipamento identificado, como troca de cabo de rede.</small>
      </div>

      <div className="form-columns">
        <SelectResposta label="O computador liga?" valor={form.computadorLiga} onChange={(v) => campo('computadorLiga', v)} />
        <SelectResposta label="Tem rede?" valor={form.temRede} onChange={(v) => campo('temRede', v)} />
        <SelectResposta label="Consegue usar AnyDesk?" valor={form.consegueAnyDesk} onChange={(v) => {
          campo('consegueAnyDesk', v)
          if (v !== 'SIM') campo('anydeskId', '')
        }} />
      </div>

      {form.consegueAnyDesk === 'SIM' && <div className="field">
        <label htmlFor="anydesk-id">ID do AnyDesk</label>
        <input id="anydesk-id" type="text" maxLength={50} autoComplete="off" value={form.anydeskId || ''}
          onChange={(e) => campo('anydeskId', e.target.value)} required />
        <small className="muted">Informe somente o ID do AnyDesk. Não envie a senha.</small>
      </div>}

      <div className="form-columns">
        <div className="field">
          <label>Melhor horário</label>
          <input type="text" maxLength={100} placeholder="Exemplo 08:00 hrs" value={form.melhorHorario}
            onChange={(e) => campo('melhorHorario', e.target.value)} />
        </div>
      </div>
      </div></details>
    </>
  )
}

