package com.manutencao.sistema.os.dto;

import java.time.Instant;

public record RecebimentoOsResponse(
        Long osId,
        Long equipamentoId,
        String patrimonio,
        String numeroSerie,
        String acessorios,
        Instant recebidoEm,
        Long recebidoPorId,
        boolean backupSolicitado,
        String escopoBackup,
        String midiaTi,
        String situacaoBackup,
        Long cienciaRiscoPorId,
        Instant cienciaRiscoEm,
        String termoCienciaRisco,
        Long versao
) {
}