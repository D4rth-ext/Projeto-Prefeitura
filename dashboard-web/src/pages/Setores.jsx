import { useEffect, useState } from 'react'
import { buscarSetores, criarSetor, atualizarSetor, excluirSetor } from '../api/setores'
import { mensagemDeErro } from '../api/erro'
import { useAuth } from '../context/AuthContext'
import CampoComLista from '../components/CampoComLista'
import { catalogoSetores, telefoneDoSetor } from '../utils/catalogoSetores'

const VAZIO = { nome: '', telefone: '', email: '', endereco: '' }
const normalizarTexto = texto => texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim()

export default function Setores() {
  const { usuario } = useAuth()
  const [setores, setSetores] = useState([])
  const [busca, setBusca] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)

  const [form, setForm] = useState(VAZIO)
  const [editandoId, setEditandoId] = useState(null)
  const [mostrarForm, setMostrarForm] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const buscaNormalizada = normalizarTexto(busca)
  const ramalBusca = busca.replace(/\D/g, '')
  const setoresFiltrados = setores.filter(setor => {
    const nomeETelefone = normalizarTexto(`${setor.nome} ${setor.telefone || ''}`)
    const ramais = (setor.telefone || '').replace(/\D/g, '')
    return nomeETelefone.includes(buscaNormalizada) || (ramalBusca && ramais.includes(ramalBusca))
  })

  async function carregar() {
    setCarregando(true)
    setErro(null)
    try {
      setSetores(await buscarSetores())
    } catch (err) {
      setErro('Não foi possível carregar os setores.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => { carregar() }, [])

  function abrirNovo() {
    setForm(VAZIO)
    setEditandoId(null)
    setMostrarForm(true)
  }

  function abrirEdicao(setor) {
    setForm({ nome: setor.nome, telefone: setor.telefone, email: setor.email, endereco: setor.endereco })
    setEditandoId(setor.id)
    setMostrarForm(true)
  }

  async function handleSalvar(e) {
    e.preventDefault()
    setSalvando(true)
    try {
      if (editandoId) {
        await atualizarSetor(editandoId, form)
      } else {
        await criarSetor(form)
      }
      setMostrarForm(false)
      carregar()
    } catch (err) {
      // Cobre 409 (nome de setor já existe entre os ativos)
      setErro(mensagemDeErro(err, 'Não foi possível salvar o setor.'))
    } finally {
      setSalvando(false)
    }
  }

  async function handleExcluir(id) {
    if (!confirm('Inativar este setor? Ele deixa de aparecer nas listas, mas pode ser reativado depois.')) return
    try {
      await excluirSetor(id)
      carregar()
    } catch (err) {
      setErro(mensagemDeErro(err, 'Não foi possível inativar o setor.'))
    }
  }

  return (
    <div>
      <div style={{
        background: 'var(--surface)', padding: '16px 24px', display: 'flex',
        alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid var(--border)'
      }}>
        <h1 style={{ fontSize: 18, fontWeight: 500, margin: 0, display: 'flex', alignItems: 'center', gap: 10 }}>
          <i className="ti ti-building" style={{ color: 'var(--green)' }} />
          Setores
        </h1>
        <button className="btn-primary" onClick={abrirNovo}>
          <i className="ti ti-plus" />
          Novo setor
        </button>
      </div>

      <div className="content">
        <div className="card" style={{ display: 'flex', gap: 10, marginBottom: 20 }}>
          <input
            type="search"
            aria-label="Buscar setor por nome ou ramal"
            placeholder="Buscar setor por nome ou ramal"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            style={{ flex: 1, height: 34, border: '1px solid var(--border)', borderRadius: 6, padding: '0 12px', fontSize: 13 }}
          />
        </div>

        {mostrarForm && (
          <form onSubmit={handleSalvar} className="card" style={{ marginBottom: 20, display: 'flex', flexDirection: 'column', gap: 14 }}>
            <h2 style={{ fontSize: 14, fontWeight: 500, margin: 0 }}>
              {editandoId ? 'Editar setor' : 'Novo setor'}
            </h2>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
              <div className="field">
                <label htmlFor="nome-setor">Nome do setor</label>
                {usuario?.perfil === 'ADMIN' && !editandoId
                  ? <CampoComLista id="nome-setor" nomeLista="setores" itens={catalogoSetores} valor={form.nome} obrigatorio ramalPorItem={telefoneDoSetor}
                      selecionar={nome => setForm(atual => ({ ...atual, nome, telefone: telefoneDoSetor(nome) }))}
                      alterar={nome => setForm(atual => {
                        const telefoneManual = atual.telefone && atual.telefone !== telefoneDoSetor(atual.nome)
                        return { ...atual, nome, telefone: telefoneManual ? atual.telefone : telefoneDoSetor(nome) }
                      })} />
                  : <input id="nome-setor" type="text" value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} required />}
              </div>
              <div className="field">
                <label>Telefone</label>
                <input type="text" value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} />
              </div>
              <div className="field">
                <label>E-mail</label>
                <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
              </div>
              <div className="field">
                <label>Endereço</label>
                <input type="text" value={form.endereco} onChange={(e) => setForm({ ...form, endereco: e.target.value })} />
              </div>
            </div>
            <div style={{ display: 'flex', gap: 10 }}>
              <button type="submit" className="btn-primary" disabled={salvando}>
                {salvando ? 'Salvando...' : 'Salvar'}
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
          {!carregando && !erro && setoresFiltrados.length === 0 && (
            <p style={{ padding: 18, color: 'var(--text-secondary)' }}>
              {setores.length === 0 ? 'Nenhum setor cadastrado.' : 'Nenhum setor encontrado.'}
            </p>
          )}
          {!carregando && !erro && setoresFiltrados.length > 0 && (
            <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ background: 'var(--page-bg)' }}>
                  {['Setor', 'Ramal / telefone', 'E-mail', 'Endereço', ''].map((col) => (
                    <th key={col} style={{ textAlign: 'left', padding: '10px 14px', fontSize: 11, color: 'var(--text-secondary)', borderBottom: '1px solid var(--border)' }}>
                      {col}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {setoresFiltrados.map((setor) => (
                  <tr key={setor.id}>
                    <td style={{ padding: '13px 14px', borderBottom: '1px solid var(--border)', fontWeight: 500 }}>{setor.nome}</td>
                    <td style={{ padding: '13px 14px', borderBottom: '1px solid var(--border)', color: 'var(--text-secondary)' }}>{setor.telefone || '—'}</td>
                    <td style={{ padding: '13px 14px', borderBottom: '1px solid var(--border)', color: 'var(--text-secondary)' }}>{setor.email || '—'}</td>
                    <td style={{ padding: '13px 14px', borderBottom: '1px solid var(--border)', color: 'var(--text-secondary)' }}>{setor.endereco || '—'}</td>
                    <td style={{ padding: '13px 14px', borderBottom: '1px solid var(--border)', textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <button onClick={() => abrirEdicao(setor)} title="Editar" style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: '4px 8px', fontSize: 12, color: 'var(--green)' }}>
                        <i className="ti ti-pencil" /> Editar
                      </button>
                      <button onClick={() => handleExcluir(setor.id)} title="Inativar" style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: '4px 8px', fontSize: 12, color: 'var(--red)' }}>
                        <i className="ti ti-trash" /> Excluir
                      </button>
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
