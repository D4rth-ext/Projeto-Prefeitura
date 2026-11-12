import { Link } from 'react-router-dom'

export default function EsqueciSenha() {
  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--green)', padding: 16 }}>
      <div className="card" style={{ width: '100%', maxWidth: 420 }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, marginBottom: 22 }}>
          <img src="/logo.png" alt="Brasão Sant'Ana do Livramento" style={{ width: 70, height: 'auto' }} />
          <h1 style={{ fontSize: 16, fontWeight: 500, margin: 0 }}>Alterar senha</h1>
        </div>

        <p style={{ fontSize: 13, color: 'var(--text-secondary)', textAlign: 'center', lineHeight: 1.6, marginTop: 0 }}>
          Para alterar sua senha, contate diretamente o DTI:
        </p>
        <p style={{ textAlign: 'center', margin: '0 0 20px' }}>
          <a href="tel:+555539681133" style={{ color: 'var(--green)', fontWeight: 600, fontSize: 16 }}>
            (55) 3968-1133
          </a>
        </p>
        <p style={{ textAlign: 'center', fontSize: 12, margin: 0 }}>
          <Link to="/login" style={{ color: 'var(--green)', fontWeight: 500 }}>Voltar pro login</Link>
        </p>
      </div>
    </div>
  )
}

