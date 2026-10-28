import { useState } from 'react'
import { NavLink, Link } from 'react-router-dom'
import { useAuth, PERFIS } from '../context/AuthContext'

const GESTAO = [PERFIS.ADMIN, PERFIS.ATENDENTE]
const EQUIPE = [...GESTAO, PERFIS.TECNICO]
const ITENS = [
  { to: '/', label: 'Chamados', perfis: null },
  { to: '/agenda', label: 'Agenda técnica', perfis: EQUIPE },
  { to: '/equipamentos', label: 'Equipamentos', perfis: EQUIPE },
  { to: '/setores', label: 'Setores', perfis: GESTAO },
  { to: '/tecnicos', label: 'Usuários', perfis: [PERFIS.ADMIN] },
  { to: '/backups', label: 'Descarte de backups', perfis: GESTAO }
]
export default function Sidebar() {
  const { usuario, logout } = useAuth()
  const [aberto, setAberto] = useState(false)
  return <aside className="sidebar">
    <div className="sidebar-brand"><Link to="/" className="sidebar-project"><img src="/logo.png" alt="Brasão de Sant’Ana do Livramento" /><span>Manutenção TI</span></Link><button type="button" className="mobile-menu" aria-label="Abrir ou fechar menu" aria-expanded={aberto} onClick={() => setAberto(v => !v)}><i className="ti ti-menu-2" /></button></div>
    <nav className={aberto ? 'sidebar-nav is-open' : 'sidebar-nav'} aria-label="Navegação principal">
      {ITENS.filter(item => !item.perfis || item.perfis.includes(usuario?.perfil)).map(item => <NavLink key={item.to} to={item.to} end={item.to === '/'} className={({ isActive }) => isActive ? 'sidebar-link active' : 'sidebar-link'} onClick={() => setAberto(false)}>{item.label}</NavLink>)}
    </nav>
    <div className="sidebar-account">
      <NavLink to="/perfil" className={({ isActive }) => `sidebar-profile${isActive ? ' active' : ''}`}>
        <i className="ti ti-user-circle profile-icon" aria-hidden="true" />
        <div className="profile-copy"><strong>Meu perfil</strong><span>{usuario?.nome}</span><span>{({ ADMIN: 'Administrador', TECNICO: 'Técnico', ATENDENTE: 'Atendente', SOLICITANTE: 'Solicitante' })[usuario?.perfil]}</span></div>
        <i className="ti ti-chevron-right profile-arrow" aria-hidden="true" />
      </NavLink>
      <button type="button" className="btn-secondary" onClick={logout}>Sair</button>
    </div>
  </aside>
}
