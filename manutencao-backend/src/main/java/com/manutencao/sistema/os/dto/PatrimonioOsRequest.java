package com.manutencao.sistema.os.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PatrimonioOsRequest(
        @NotBlank @Size(max = 40) String patrimonioInformado,
        @NotNull @PositiveOrZero Long versao
) {
}
