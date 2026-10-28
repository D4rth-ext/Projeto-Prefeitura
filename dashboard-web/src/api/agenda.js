import { api } from './client'

export const listarAgendamentos = params => api.get('/agendamentos', { params }).then(r => r.data)
export const criarAgendamento = dados => api.post('/agendamentos', dados).then(r => r.data)
export const atualizarAgendamento = (id, dados) => api.put(`/agendamentos/${id}`, dados).then(r => r.data)
export const alterarStatusAgendamento = (id, dados) => api.patch(`/agendamentos/${id}/status`, dados).then(r => r.data)
