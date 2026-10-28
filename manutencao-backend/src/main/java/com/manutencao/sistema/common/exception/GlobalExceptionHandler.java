package com.manutencao.sistema.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converte qualquer exceção da API no formato único ApiError. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // nome da constraint/índice do banco -> mensagem amigável
    private static final Map<String, String> MENSAGENS_DE_RESTRICAO = Map.of(
            "uq_setor_nome_ativo", "Já existe um setor ativo com este nome.",
            "uq_equip_patrimonio", "Já existe um equipamento com este patrimônio.",
            "uq_equip_serie", "Já existe um equipamento com este número de série para a mesma marca.",
            "uq_usuario_email", "Já existe um usuário com este e-mail.");

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ApiError> conflito(ConflitoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiError> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiError.CampoInvalido(e.getField(), e.getDefaultMessage()))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos. Verifique os campos informados.", req, campos);
    }

    /** Rede de segurança: a constraint do banco é a palavra final sobre duplicidade. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridade(DataIntegrityViolationException ex, HttpServletRequest req) {
        String detalhe = ex.getMostSpecificCause().getMessage();
        String mensagem = "Operação viola uma regra de integridade dos dados.";
        if (detalhe != null) {
            for (var restricao : MENSAGENS_DE_RESTRICAO.entrySet()) {
                if (detalhe.contains(restricao.getKey())) {
                    mensagem = restricao.getValue();
                    break;
                }
            }
        }
        return resposta(HttpStatus.CONFLICT, mensagem, req, List.of());
    }

    private ResponseEntity<ApiError> resposta(HttpStatusCode status, String mensagem, HttpServletRequest req,
                                              List<ApiError.CampoInvalido> campos) {
        HttpStatus conhecido = HttpStatus.resolve(status.value());
        String erro = conhecido != null ? conhecido.getReasonPhrase() : "Erro";
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), erro, mensagem, req.getRequestURI(), campos));
    }
}
