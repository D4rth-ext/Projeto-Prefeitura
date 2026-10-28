package com.manutencao.sistema.os.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record DescarteBackupRequest(
        @NotNull @AssertTrue(message = "Confirme a eliminação dos dados da mídia do TI.")
        Boolean descarteConfirmado,
        @NotNull @PositiveOrZero Long versao
) {
}
