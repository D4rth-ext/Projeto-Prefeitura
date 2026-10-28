package com.manutencao.sistema.equipamento.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Regra adicional (validada no service): informar patrimônio OU número de série. */
public record EquipamentoRequest(
        @NotNull(message = "O setor é obrigatório.")
        Long setorId,

        @NotNull(message = "O tipo de equipamento é obrigatório.")
        Long tipoId,

        @Size(max = 60, message = "A marca deve ter no máximo 60 caracteres.")
        String marca,

        @Size(max = 80, message = "O modelo deve ter no máximo 80 caracteres.")
        String modelo,

        @Size(max = 40, message = "O patrimônio deve ter no máximo 40 caracteres.")
        String patrimonio,

        @Size(max = 80, message = "O número de série deve ter no máximo 80 caracteres.")
        String numeroSerie,

        String observacoes) {
}
