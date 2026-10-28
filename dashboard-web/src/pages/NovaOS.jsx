import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { abrirOrdem, listarSolicitantes } from '../api/ordensServico'
import { mensagemDeErro } from '../api/erro'
import { useAuth } from '../context/AuthContext'
import CamposChamado from '../components/CamposChamado'

const VAZIO = {
  solicitanteId: '', categoria: '', descricaoProblema: '', patrimonioInformado: '',
  computadorLiga: '', temRede: '', consegueAnyDesk: '', contato: '', melhorHorario: ''
}

export default function NovaOS() {
  const navigate = useNavigate()
  const { usuario } = useAuth()
  const [form, setForm] = useState(VAZIO)
  const [solicitantes, setSolicitantes] = useState([])
  const [carregandoSolicitantes, setCarregandoSolicitantes] = useState(false)
  const [erroSolicitantes, setErroSolicitantes] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)

  const perfil = usuario?.perfil
  const abreEmNomeDeOutro = perfil === 'ADMIN' || perfil === 'ATENDENTE'

  useEffect(() => {
    if (!abreEmNomeDeOutro) return
    let ativo = true
    setCarregandoSolicitantes(true)
    listarSolicitantes()
      .then(lista => { if (ativo) setSolicitantes(lista) })
      .catch(e => { if (ativo) setErroSolicitantes(mensagemDeErro(e, 'Não foi possível carregar os solicitantes. Recarregue a página.')) })
      .finally(() => { if (ativo) setCarregandoSolicitantes(false) })
    return () => { ativo = false }
  }, [abreEmNomeDeOutro])

  function campo(nome, valor) {
    setForm((f) => ({ ...f, [nome]: valor }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setEnviando(true)
    setErro(null)

    // Campos vazios não vão no JSON (o backend trata como não informado)
    const corpo = {}
    Object.entries(form).forEach(([chave, valor]) => {
      if (valor !== '') corpo[chave] = chave === 'solicitanteId' ? Number(valor) : valor
    })

    try {
      const nova = await abrirOrdem(corpo)
      navigate(`/ordens-servico/${nova.id}`)
    } catch (err) {
      setErro(mensagemDeErro(err, 'Não foi possível abrir a OS. Verifique os dados e tente novamente.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="new-ticket-page">
      <div style={{ background: 'var(--surface)', padding: '16px 24px', borderBottom: '1px solid var(--border)' }}>
        <h1 style={{ fontSize: 18, fontWeight: 500, margin: 0 }}>Como podemos ajudar?</h1><p className="muted">Descreva o problema. A equipe de TI cuida do encaminhamento.</p>
      </div>

      <div className="content new-ticket-body">
        <form onSubmit={handleSubmit} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>

          {!abreEmNomeDeOutro && <div className="requester-summary"><span className="muted">Solicitante</span><strong>{usuario?.nome}</strong><span className="muted">O setor é associado automaticamente à sua conta.</span></div>}
          {abreEmNomeDeOutro && (
            <div className="field">
              <label>Solicitante</label>
                <select disabled={carregandoSolicitantes} value={form.solicitanteId} onChange={(e) => campo('solicitanteId', e.target.value)} required>
                  <option value="">{carregandoSolicitantes ? 'Carregando...' : 'Selecione o solicitante'}</option>
                  {solicitantes.map((s) => (
                    <option key={s.id} value={s.id}>{s.nome} — {s.setorNome}</option>
                  ))}
                </select>
                {erroSolicitantes && <p className="erro" role="alert">{erroSolicitantes}</p>}
                {!carregandoSolicitantes && !erroSolicitantes && !solicitantes.length && <p>Nenhum solicitante ativo em setor ativo. O solicitante precisa realizar o autocadastro.</p>}
            </div>
          )}

          <CamposChamado form={form} campo={campo} />

          {erro && <p style={{ color: 'var(--red)', fontSize: 12, margin: 0 }}>{erro}</p>}

          <footer className="action-footer"><Link to="/" className="back-text">Voltar para chamados</Link><button type="submit" className="btn-primary" disabled={enviando || (abreEmNomeDeOutro && (carregandoSolicitantes || !solicitantes.length))}>
            {enviando ? 'Enviando...' : 'Enviar chamado'}
          </button></footer>
        </form>
      </div>
    </div>
  )
}
