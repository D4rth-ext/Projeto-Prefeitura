package com.manutencao.sistema.os.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RecebimentoOsRequest(
        @NotNull(message = "O equipamento é obrigatório.")
        @Positive(message = "O ID do equipamento deve ser positivo.")
        Long equipamentoId,

        String acessorios,

        @NotNull(message = "Informe se o backup foi solicitado.")
        Boolean backupSolicitado,

        String escopoBackup,

        @Size(max = 80, message = "A mídia do TI deve ter até 80 caracteres.")
        String midiaTi,

        Boolean cienciaRiscoConfirmada,

        @NotNull(message = "A versão da OS é obrigatória.")
        @PositiveOrZero(message = "A versão não pode ser negativa.")
        Long versao
) {
}