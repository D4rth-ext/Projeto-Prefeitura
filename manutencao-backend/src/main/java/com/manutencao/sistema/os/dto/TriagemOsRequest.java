package com.manutencao.sistema.os.dto;

import com.manutencao.sistema.os.domain.ModalidadeOs;
import com.manutencao.sistema.os.domain.MotivoBancada;
import com.manutencao.sistema.os.domain.PrioridadeOs;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public record TriagemOsRequest(
        @NotNull(message = "A modalidade é obrigatória.")
        ModalidadeOs modalidade,

        @NotNull(message = "A prioridade é obrigatória.")
        PrioridadeOs prioridade,

        @NotEmpty(message = "Informe pelo menos um técnico.")
        List<@NotNull @Positive Long> tecnicosIds,

        MotivoBancada motivoBancada,
        String justificativa,
        String motivoRemocao,

        @NotNull(message = "A versão da OS é obrigatória.")
        @PositiveOrZero(message = "A versão não pode ser negativa.")
        Long versao
) {
}