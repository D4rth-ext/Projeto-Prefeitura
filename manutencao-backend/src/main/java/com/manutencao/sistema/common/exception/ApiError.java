package com.manutencao.sistema.common.exception;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public record ApiError(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> campos) {

    public static ApiError de(HttpStatusCode status, String mensagem, String caminho, List<CampoInvalido> campos) {
        HttpStatus conhecido = HttpStatus.resolve(status.value());
        String erro = conhecido == null ? "Erro" : conhecido.getReasonPhrase();
        return new ApiError(Instant.now(), status.value(), erro, mensagem, caminho, campos);
    }

    public record CampoInvalido(String campo, String mensagem) {
    }
}
