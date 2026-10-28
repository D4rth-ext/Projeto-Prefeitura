import { useEffect, useState } from 'react'
import { buscarEquipamentos, criarEquipamento, buscarTiposEquipamento, excluirEquipamento } from '../api/equipamentos'
import { buscarSetores } from '../api/setores'
import { mensagemDeErro } from '../api/erro'
import { useAuth } from '../context/AuthContext'

const VAZIO = {
  setorId: '', tipoId: '', marca: '', modelo: '', patrimonio: '', numeroSerie: '', observacoes: ''
}

export default function Equipamentos() {
  const { usuario } = useAuth()
  // O backend só deixa Admin e Atendente criar/excluir; Técnico só consulta.
  const podeEditar = usuario?.perfil === 'ADMIN' || usuario?.perfil === 'ATENDENTE'
  const [equipamentos, setEquipamentos] = useState([])
  const [setores, setSetores] = useState([])
  const [tipos, setTipos] = useState([])
  const [busca, setBusca] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)

  const [form, setForm] = useState(VAZIO)
  const [mostrarForm, setMostrarForm] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [erroForm, setErroForm] = useState(null)

  async function carregar(filtros = {}) {
    setCarregando(true)
    setErro(null)
    try {
      const [dadosEquip, dadosSetores, dadosTipos] = await Promise.all([
        buscarEquipamentos(filtros),
        buscarSetores(),
        buscarTiposEquipamento()
      ])
      setEquipamentos(dadosEquip)
      setSetores(dadosSetores)
      setTipos(dadosTipos)
    } catch (err) {
      setErro('Não foi possível carregar os equipamentos.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregar() }, [])

  function handleBuscar(e) {
    e.preventDefault()
    carregar({ busca })
  }

  async function handleExcluir(eq) {
    const nome = eq.patrimonio || eq.numeroSerie || `#${eq.id}`
    if (!confirm(`Excluir o equipamento ${nome}? Ele deixa de aparecer nas listas, mas o histórico das OS é mantido.`)) return
    try {
      await excluirEquipamento(eq.id)
      carregar({ busca })
    } catch (err) {
      setErro(mensagemDeErro(err, 'Não foi possível excluir o equipamento.'))
    }
  }

  function campo(nome, valor) {
    setForm((f) => ({ ...f, [nome]: valor }))
  }

  async function handleSalvar(e) {
    e.preventDefault()
    setErroForm(null)

    if (!form.patrimonio && !form.numeroSerie) {
      setErroForm('Informe o patrimônio ou o número de série (pelo menos um dos dois).')
      return
    }

    setSalvando(true)
    try {
      await criarEquipamento({
        setorId: Number(form.setorId),
        tipoId: Number(form.tipoId),
        marca: form.marca || null,
        modelo: form.modelo || null,
        patrimonio: form.patrimonio || null,
        numeroSerie: form.numeroSerie || null,
        observacoes: form.observacoes || null
      })
      setForm(VAZIO)
      setMostrarForm(false)
      carregar({ busca })
    } catch (err) {
      setErroForm(mensagemDeErro(err, 'Não foi possível salvar. Verifique se o patrimônio já não está cadastrado (RNF04).'))
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div>
      <div style={{
        background: 'var(--surface)', padding: '16px 24px', display: 'flex',
        alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid var(--border)'
      }}>
        <h1 style={{ fontSize: 18, fontWeight: 500, margin: 0, display: 'flex', alignItems: 'center', gap: 10 }}>
          <i className="ti ti-devices" style={{ color: 'var(--green)' }} />
          Equipamentos
        </h1>
        {podeEditar && (
          <button className="btn-primary" onClick={() => setMostrarForm((v) => !v)}>
            <i className="ti ti-plus" />
            Novo equipamento
          </button>
        )}
      </div>

      <div className="content">
        <form onSubmit={handleBuscar} className="card" style={{ display: 'flex', gap: 10, marginBottom: 20 }}>
          <input
            type="text"
            placeholder="Buscar por marca, modelo, patrimônio ou série"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            style={{ flex: 1, height: 34, border: '1px solid var(--border)', borderRadius: 6, padding: '0 12px', fontSize: 13 }}
          />
          <button type="submit" className="btn-primary">
            <i className="ti ti-search" />
            Pesquisar
          </button>
        </form>

        {mostrarForm && (
          <form onSubmit={handleSalvar} className="card" style={{ marginBottom: 20, display: 'flex', flexDirection: 'column', gap: 14 }}>
            <h2 style={{ fontSize: 14, fontWeight: 500, margin: 0 }}>Novo equipamento</h2>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
              <div className="field">
                <label>Setor (RF02.3)</label>
                <select value={form.setorId} onChange={(e) => campo('setorId', e.target.value)} required>
                  <option value="">Selecione o setor</option>
                  {setores.map((s) => (
                    <option key={s.id} value={s.id}>{s.nome}</option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>Tipo de equipamento</label>
                <select value={form.tipoId} onChange={(e) => campo('tipoId', e.target.value)} required>
                  <option value="">Selecione o tipo</option>
                  {tipos.map((t) => (
                    <option key={t.id} value={t.id}>{t.nome}</option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>Marca</label>
                <input type="text" value={form.marca} onChange={(e) => campo('marca', e.target.value)} />
              </div>
              <div className="field">
                <label>Modelo</label>
                <input type="text" value={form.modelo} onChange={(e) => campo('modelo', e.target.value)} />
              </div>
              <div className="field">
                <label>Patrimônio</label>
                <input type="text" placeholder="Ex: PAT-00231" value={form.patrimonio} onChange={(e) => campo('patrimonio', e.target.value)} />
              </div>
              <div className="field">
                <label>Número de série</label>
                <input type="text" value={form.numeroSerie} onChange={(e) => campo('numeroSerie', e.target.value)} />
              </div>
            </div>
            <p style={{ fontSize: 11, color: 'var(--text-secondary)', margin: 0 }}>
              Informe patrimônio ou número de série (pelo menos um dos dois).
            </p>

            <div className="field">
              <label>Observações</label>
              <textarea
                rows={2}
                value={form.observacoes}
                onChange={(e) => campo('observacoes', e.target.value)}
                style={{ width: '100%', padding: 8, borderRadius: 6, border: '1px solid var(--border)', fontFamily: 'inherit', fontSize: 13 }}
              />
            </div>

            {erroForm && <p style={{ color: 'var(--red)', fontSize: 12, margin: 0 }}>{erroForm}</p>}

            <div style={{ display: 'flex', gap: 10 }}>
              <button type="submit" className="btn-primary" disabled={salvando}>
                {salvando ? 'Salvando...' : 'Salvar equipamento'}
              </button>
              <button
                type="button"
                onClick={() => setMostrarForm(false)}
                style={{ background: 'transparent', border: '1px solid var(--border)', borderRadius: 6, padding: '9px 16px', fontSize: 13, cursor: 'pointer' }}
              >
                Cancelar
              </button>
            </div>
          </form>
        )}

        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          {carregando && <p style={{ padding: 18, color: 'var(--text-secondary)' }}>Carregando...</p>}
          {erro && <p style={{ padding: 18, color: 'var(--red)' }}>{erro}</p>}
          {!carregando && !erro && equipamentos.length === 0 && (
            <p style={{ padding: 18, color: 'var(--text-secondary)' }}>Nenhum equipamento cadastrado.</p>
          )}
          {!carregando && !erro && equipamentos.length > 0 && (
            <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ background: 'var(--page-bg)' }}>
                  {['Patrimônio / série', 'Tipo', 'Marca / modelo', 'Setor', ''].map((col) => (
                    <th key={col} style={{ textAlign: 'left', padding: '10px 14px', fontSize: 11, color: 'var(--text-secondary)', borderBottom: '1px solid var(--border)' }}>
                      {col}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {equipamentos.map((eq) => (
                  <tr key={eq.id}>
                    <td style={{ padding: '13px 14px', borderBottom: '1px solid var(--border)', fontWeight: 500 }}>{eq.patrimonio || eq.numeroSerie}</td>
                    <td style={{ padding: '13px 14px', borderBottom: '1px solid var(--border)' }}>{eq.tipo?.nome}</td>
                    <td style={{ padding: '13px 14px', borderBottom: '1px solid var(--border)', color: 'var(--text-secondary)' }}>{eq.marca} {eq.modelo}</td>
                    <td style={{ padding: '13px 14px', borderBottom: '1px solid var(--border)', color: 'var(--text-secondary)' }}>{eq.setor?.nome}</td>
                    <td style={{ padding: '13px 14px', borderBottom: '1px solid var(--border)', textAlign: 'right', whiteSpace: 'nowrap' }}>
                      {podeEditar && (
                        <button onClick={() => handleExcluir(eq)} title="Excluir" style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: '4px 8px', fontSize: 12, color: 'var(--red)' }}>
                          <i className="ti ti-trash" /> Excluir
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
