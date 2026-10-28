package com.manutencao.sistema.agenda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AgendamentoRequest(
        @NotNull @Positive Long tecnicoId,
        @Positive Long osId,
        @Positive Long setorId,
        @NotBlank @Size(max = 160) String titulo,
        String descricao,
        @Size(max = 255) String local,
        @NotNull Instant inicio,
        @NotNull Instant fim,
        @PositiveOrZero Long versao
) {
}
