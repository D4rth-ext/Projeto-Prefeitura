package com.manutencao.sistema.os.dto;

import com.manutencao.sistema.os.domain.MotivoBancada;
import com.manutencao.sistema.os.domain.StatusOs;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record StatusOsRequest(
        @NotNull StatusOs status,
        LocalDate prazoPrevisto,
        String justificativa,
        String resumoSolucao,
        @Size(max = 120) String retiradoPor,
        MotivoBancada motivoBancada,
        @Positive Long registroPecaId,
        @NotNull @PositiveOrZero Long versao
) {
}
