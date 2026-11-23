package com.manutencao.sistema.os.dto;

import com.manutencao.sistema.os.domain.StatusOs;
import java.time.Instant;

public record HistoricoStatusResponse(StatusOs anterior, StatusOs status, Instant criadoEm, String autorNome) {
}
