package com.manutencao.sistema.os.dto;

import com.manutencao.sistema.os.domain.ModalidadeOs;
import com.manutencao.sistema.os.domain.StatusOs;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record BuscaOsRequest(
        @Positive Long id,
        @Positive Long setorId,
        @Positive Long equipamentoId,
        String patrimonio,
        String numeroSerie,
        @Positive Long tecnicoId,
        StatusOs status,
        ModalidadeOs modalidade,
        Boolean backupEmAberto,
        Boolean atrasadas,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
        @Min(0) Integer pagina,
        @Min(1) @Max(100) Integer tamanho
) {
    public int paginaAtual() {
        return pagina == null ? 0 : pagina;
    }

    public int tamanhoAtual() {
        return tamanho == null ? 20 : tamanho;
    }
}
