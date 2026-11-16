import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// perfisPermitidos: lista opcional (ex: [PERFIS.ADMIN]).
// Se omitido, so exige estar logado.
export default function RotaProtegida({ perfisPermitidos, children }) {
  const { usuario } = useAuth()
  const location = useLocation()

  if (!usuario) {
    return <Navigate to="/login" replace />
  }

  if (usuario.trocaSenhaObrigatoria && location.pathname !== '/trocar-senha') {
    return <Navigate to="/trocar-senha" replace />
  }

  if (perfisPermitidos && !perfisPermitidos.includes(usuario.perfil)) {
    return <Navigate to="/" replace />
  }

  return children
}
