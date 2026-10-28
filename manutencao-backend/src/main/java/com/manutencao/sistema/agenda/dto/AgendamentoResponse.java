package com.manutencao.sistema.agenda.dto;

import com.manutencao.sistema.agenda.domain.Agendamento;
import com.manutencao.sistema.agenda.domain.StatusAgendamento;
import java.time.Instant;

public record AgendamentoResponse(
        Long id, Long tecnicoId, String tecnicoNome, Long osId,
        Long setorId, String setorNome, String titulo, String descricao,
        String local, Instant inicio, Instant fim, StatusAgendamento status,
        Long criadoPorId, Instant criadoEm, Instant atualizadoEm, Long versao
) {
    public static AgendamentoResponse de(Agendamento agenda) {
        return new AgendamentoResponse(
                agenda.getId(), agenda.getTecnico().getId(), agenda.getTecnico().getNome(),
                agenda.getOs() == null ? null : agenda.getOs().getId(),
                agenda.getSetor() == null ? null : agenda.getSetor().getId(),
                agenda.getSetor() == null ? null : agenda.getSetor().getNome(),
                agenda.getTitulo(), agenda.getDescricao(), agenda.getLocal(),
                agenda.getInicio(), agenda.getFim(), agenda.getStatus(),
                agenda.getCriadoPor().getId(), agenda.getCriadoEm(), agenda.getAtualizadoEm(), agenda.getVersao()
        );
    }
}
