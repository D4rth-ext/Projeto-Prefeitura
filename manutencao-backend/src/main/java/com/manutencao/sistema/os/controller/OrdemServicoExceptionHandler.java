package com.manutencao.sistema.os.controller;

import com.manutencao.sistema.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {
        OrdemServicoController.class,
        RecebimentoOsController.class,
        BackupOsController.class,
        RegistroOsController.class,
        FluxoOsController.class,
        BuscaOsController.class,
        com.manutencao.sistema.agenda.controller.AgendamentoController.class,
        com.manutencao.sistema.usuario.controller.EquipeController.class,
        com.manutencao.sistema.usuario.controller.UsuarioController.class
})
public class OrdemServicoExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> acesso(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        return resposta(ex.getStatusCode(), ex.getReason(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> jsonInvalido(HttpServletRequest request) {
        return resposta(
                HttpStatus.BAD_REQUEST,
                "JSON inválido. Verifique os tipos dos campos e os valores permitidos.",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> parametroInvalido(HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido. Confira IDs, datas e valores permitidos.", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> concorrencia(HttpServletRequest request) {
        return resposta(
                HttpStatus.CONFLICT,
                "A OS foi alterada por outra pessoa. Consulte novamente antes de salvar.",
                request
        );
    }

    private ResponseEntity<ApiError> resposta(
            HttpStatusCode status,
            String mensagem,
            HttpServletRequest request
    ) {
        HttpStatus conhecido = HttpStatus.resolve(status.value());
        String erro = conhecido != null ? conhecido.getReasonPhrase() : "Erro";

        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(),
                status.value(),
                erro,
                mensagem,
                request.getRequestURI(),
                List.of()
        ));
    }
}
