package com.manutencao.sistema.os.dto;

import java.time.Instant;

public record EventoOsResponse(
        Long id, Long osId, String tipo, Long autorId, String autorNome,
        String valorAnterior, String valorNovo, String motivo, Instant criadoEm
) {
}
