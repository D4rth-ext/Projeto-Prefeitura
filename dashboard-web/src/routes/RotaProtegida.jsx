import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// perfisPermitidos: lista opcional (ex: [PERFIS.ADMIN]).
// Se omitido, so exige estar logado.
export default function RotaProtegida({ perfisPermitidos, children }) {
  const { usuario } = useAuth()

  if (!usuario) {
    return <Navigate to="/login" replace />
  }

  if (perfisPermitidos && !perfisPermitidos.includes(usuario.perfil)) {
    return <Navigate to="/" replace />
  }

  return children
}
