package com.manutencao.sistema.os.dto;

import com.manutencao.sistema.os.domain.SituacaoBackup;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AtualizacaoBackupRequest(
        @NotNull SituacaoBackup situacao,
        @NotNull @PositiveOrZero Long versao
) {
}
