import { api } from './client'

export function buscarRelatorioOs(params) {
  return api.get('/relatorios/os', { params }).then(r => r.data)
}
