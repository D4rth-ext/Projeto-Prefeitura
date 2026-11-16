import { Link } from 'react-router-dom'

export default function EsqueciSenha() {
  return <main style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', background: 'var(--green)', padding: 16 }}>
    <section className="card" style={{ width: '100%', maxWidth: 420 }}>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, marginBottom: 22 }}>
        <img src="/logo.png" alt="Brasão Sant'Ana do Livramento" style={{ width: 70, height: 'auto' }} />
        <h1 style={{ fontSize: 16, fontWeight: 500, margin: 0 }}>Solicitar senha temporária</h1>
      </div>
      <p style={{ fontSize: 13, lineHeight: 1.6 }}>A recuperação por e-mail não está disponível. Peça ao administrador uma senha temporária. Ela vale por 24 horas e o sistema solicitará uma senha definitiva no próximo acesso.</p>
      <Link to="/login" className="btn-primary" style={{ width: '100%', justifyContent: 'center' }}>Voltar ao login</Link>
    </section>
  </main>
}
