package com.manutencao.sistema.agenda.dto;

import com.manutencao.sistema.agenda.domain.StatusAgendamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StatusAgendamentoRequest(
        @NotNull StatusAgendamento status,
        @NotNull @PositiveOrZero Long versao
) {
}
