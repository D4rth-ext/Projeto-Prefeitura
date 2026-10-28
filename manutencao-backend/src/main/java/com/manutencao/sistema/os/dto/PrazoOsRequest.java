package com.manutencao.sistema.os.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public record PrazoOsRequest(
        @NotNull LocalDate prazoPrevisto,
        @NotBlank String justificativa,
        @NotNull @PositiveOrZero Long versao
) {
}
