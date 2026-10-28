package com.manutencao.sistema.os.dto;

import java.time.Instant;

public record VinculoTecnicoResponse(
        Long id, Long tecnicoId, String tecnicoNome, Long designadoPorId,
        Instant entradaEm, Instant saidaEm, String motivoSaida
) {
}
