import { useRef, useState } from 'react'
import { useAuth } from '../context/AuthContext'

function iniciais(nome = '') {
  return nome
    .trim()
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('')
}

export default function Perfil() {
  const { usuario, atualizarUsuario } = useAuth()
  const inputFotoRef = useRef(null)

  const [form, setForm] = useState({
    nome: usuario?.nome || '',
    email: usuario?.email || '',
    telefone: usuario?.telefone || '',
    fotoUrl: usuario?.fotoUrl || null
  })
  const [salvando, setSalvando] = useState(false)
  const [sucesso, setSucesso] = useState(false)
  const [erro, setErro] = useState(null)

  function campo(nome, valor) {
    setForm((f) => ({ ...f, [nome]: valor }))
    setSucesso(false)
  }

  function handleEscolherFoto() {
    inputFotoRef.current?.click()
  }

  function handleFotoSelecionada(e) {
    const arquivo = e.target.files?.[0]
    if (!arquivo) return

    if (arquivo.size > 2 * 1024 * 1024) {
      setErro('A imagem precisa ter no máximo 2MB.')
      return
    }

    const leitor = new FileReader()
    leitor.onload = () => campo('fotoUrl', leitor.result)
    leitor.readAsDataURL(arquivo)
    // Quando o backend tiver upload de arquivo, troque isso por um
    // POST /usuarios/me/foto (multipart) e salve a URL retornada.
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSalvando(true)
    setErro(null)
    try {
      await atualizarUsuario(form)
      setSucesso(true)
    } catch (err) {
      setErro('Não foi possível salvar as alterações.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div>
      <div style={{
        background: 'var(--surface)', padding: '16px 24px', display: 'flex',
        alignItems: 'center', gap: 10, borderBottom: '1px solid var(--border)'
      }}>
        <h1 style={{ fontSize: 18, fontWeight: 500, margin: 0, display: 'flex', alignItems: 'center', gap: 10 }}>
          <i className="ti ti-user-circle" style={{ color: 'var(--green)' }} />
          Meu perfil
        </h1>
      </div>

      <div className="content" style={{ maxWidth: 480 }}>
        <form onSubmit={handleSubmit} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>

          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
            <div style={{ position: 'relative', width: 88, height: 88 }}>
              {form.fotoUrl ? (
                <img
                  src={form.fotoUrl}
                  alt="Foto do perfil"
                  style={{ width: 88, height: 88, borderRadius: '50%', objectFit: 'cover' }}
                />
              ) : (
                <div style={{
                  width: 88, height: 88, borderRadius: '50%', background: 'var(--green-light)',
                  color: 'var(--green-dark)', display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 26, fontWeight: 500
                }}>
                  {iniciais(form.nome) || <i className="ti ti-user" style={{ fontSize: 32 }} />}
                </div>
              )}

              <button
                type="button"
                onClick={handleEscolherFoto}
                title="Trocar foto"
                style={{
                  position: 'absolute', bottom: 0, right: 0, width: 30, height: 30, borderRadius: '50%',
                  background: 'var(--green)', border: '2px solid var(--surface)', color: '#fff',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer'
                }}
              >
                <i className="ti ti-camera" style={{ fontSize: 14 }} />
              </button>

              <input
                ref={inputFotoRef}
                type="file"
                accept="image/*"
                onChange={handleFotoSelecionada}
                style={{ display: 'none' }}
              />
            </div>
            <button type="button" onClick={handleEscolherFoto} style={{ background: 'none', border: 'none', color: 'var(--green)', fontSize: 12, fontWeight: 500, cursor: 'pointer' }}>
              Alterar foto
            </button>
          </div>

          <div className="field">
            <label>Nome completo</label>
            <input type="text" value={form.nome} onChange={(e) => campo('nome', e.target.value)} required />
          </div>

          <div className="field">
            <label>E-mail</label>
            <input type="email" value={form.email} onChange={(e) => campo('email', e.target.value)} required />
          </div>

          <div className="field">
            <label>Telefone</label>
            <input type="text" value={form.telefone} onChange={(e) => campo('telefone', e.target.value)} />
          </div>

          {usuario?.perfil && (
            <div className="field">
              <label>Perfil de acesso</label>
              <input type="text" value={usuario.perfil} disabled style={{ background: 'var(--page-bg)', color: 'var(--text-secondary)' }} />
            </div>
          )}

          {erro && <p style={{ color: 'var(--red)', fontSize: 12, margin: 0 }}>{erro}</p>}
          {sucesso && <p style={{ color: 'var(--final-text)', fontSize: 12, margin: 0 }}>Perfil atualizado com sucesso.</p>}

          <button type="submit" className="btn-primary" disabled={salvando}>
            {salvando ? 'Salvando...' : 'Salvar alterações'}
          </button>
        </form>
      </div>
    </div>
  )
}
