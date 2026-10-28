package com.manutencao.sistema.os.dto;

import com.manutencao.sistema.os.domain.SituacaoBackup;
import java.time.Instant;

public record BackupOsResponse(
        Long osId,
        boolean solicitado,
        Long cienciaRiscoPorId,
        Instant cienciaRiscoEm,
        String termoCienciaRisco,
        String escopo,
        String midiaTi,
        SituacaoBackup situacao,
        Instant descartadoEm,
        Long descartadoPorId,
        Instant limiteDescarteEm,
        boolean descarteAtrasado,
        Long versao
) {
}
