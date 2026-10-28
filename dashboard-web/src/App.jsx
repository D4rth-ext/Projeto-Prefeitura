import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, PERFIS } from './context/AuthContext'
import RotaProtegida from './routes/RotaProtegida'
import Sidebar from './components/Sidebar'
import Login from './pages/Login'
import Cadastro from './pages/Cadastro'
import EsqueciSenha from './pages/EsqueciSenha'
import RedefinirSenha from './pages/RedefinirSenha'
import Perfil from './pages/Perfil'
import Usuarios from './pages/Usuarios'
import OrdensServico from './pages/OrdensServico'
import NovaOS from './pages/NovaOS'
import OsDetalhe from './pages/OsDetalhe'
import Setores from './pages/Setores'
import Equipamentos from './pages/Equipamentos'
import Agenda from './pages/Agenda'
import Backups from './pages/Backups'

function Layout({ children }) {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-area">{children}</div>
    </div>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/backups" element={<RotaProtegida perfisPermitidos={[PERFIS.ADMIN, PERFIS.ATENDENTE]}><Layout><Backups /></Layout></RotaProtegida>} />
        <Route path="/agenda" element={<RotaProtegida perfisPermitidos={[PERFIS.ADMIN, PERFIS.ATENDENTE, PERFIS.TECNICO]}><Layout><Agenda /></Layout></RotaProtegida>} />
        <Route path="/login" element={<Login />} />
        <Route path="/acesso-equipe" element={<Navigate to="/login" replace />} />
        <Route path="/esqueci-senha" element={<EsqueciSenha />} />
        <Route path="/redefinir-senha" element={<RedefinirSenha />} />
        <Route path="/cadastro" element={<Cadastro />} />

        <Route
          path="/"
          element={
            <RotaProtegida>
              <Layout><OrdensServico /></Layout>
            </RotaProtegida>
          }
        />

        <Route
          path="/ordens-servico/nova"
          element={
            <RotaProtegida perfisPermitidos={[PERFIS.ADMIN, PERFIS.ATENDENTE, PERFIS.SOLICITANTE]}>
              <Layout><NovaOS /></Layout>
            </RotaProtegida>
          }
        />

        <Route
          path="/ordens-servico/:id"
          element={
            <RotaProtegida>
              <Layout><OsDetalhe /></Layout>
            </RotaProtegida>
          }
        />

        <Route
          path="/perfil"
          element={
            <RotaProtegida>
              <Layout><Perfil /></Layout>
            </RotaProtegida>
          }
        />

        <Route
          path="/setores"
          element={
            <RotaProtegida>
              <Layout><Setores /></Layout>
            </RotaProtegida>
          }
        />

        <Route
          path="/equipamentos"
          element={
            <RotaProtegida>
              <Layout><Equipamentos /></Layout>
            </RotaProtegida>
          }
        />

        <Route
          path="/tecnicos"
          element={
            <RotaProtegida perfisPermitidos={[PERFIS.ADMIN]}>
              <Layout><Usuarios /></Layout>
            </RotaProtegida>
          }
        />

        {/*
          Proximas rotas a implementar, seguindo o mesmo padrao:
          /relatorios (RF06.2 - so ADMIN)
        */}
      </Routes>
    </AuthProvider>
  )
}
