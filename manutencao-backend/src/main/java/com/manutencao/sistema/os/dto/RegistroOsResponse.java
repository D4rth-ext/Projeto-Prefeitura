package com.manutencao.sistema.os.dto;

import com.manutencao.sistema.os.domain.TipoRegistroOs;
import java.time.Instant;

public record RegistroOsResponse(
        Long id, Long osId, TipoRegistroOs tipo, Long autorId,
        String autorNome, String texto, Instant criadoEm, Long versao
) {
}
