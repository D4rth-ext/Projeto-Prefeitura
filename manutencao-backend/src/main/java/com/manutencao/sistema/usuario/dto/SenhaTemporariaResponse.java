package com.manutencao.sistema.usuario.dto;

import java.time.Instant;

public record SenhaTemporariaResponse(String senhaTemporaria, Instant expiraEm) {

    @Override
    public String toString() {
        return "SenhaTemporariaResponse[expiraEm=" + expiraEm + "]";
    }
}
