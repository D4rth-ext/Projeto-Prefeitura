package com.manutencao.sistema.os.dto;

import com.manutencao.sistema.os.domain.TipoRegistroOs;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RegistroOsRequest(
        @NotNull TipoRegistroOs tipo,
        @NotBlank String texto,
        @NotNull @PositiveOrZero Long versao
) {
}
