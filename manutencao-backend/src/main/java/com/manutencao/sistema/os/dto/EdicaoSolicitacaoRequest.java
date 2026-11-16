package com.manutencao.sistema.os.dto;

import com.manutencao.sistema.os.domain.CategoriaOs;
import com.manutencao.sistema.os.domain.RespostaRapida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record EdicaoSolicitacaoRequest(
        @NotNull CategoriaOs categoria,
        @NotBlank String descricaoProblema,
        @Size(max = 40) String patrimonioInformado,
        RespostaRapida computadorLiga,
        RespostaRapida temRede,
        RespostaRapida consegueAnyDesk,
        @NotBlank(message = "O telefone para contato é obrigatório.") @Size(max = 60) String contato,
        @Size(max = 100) String melhorHorario,
        @NotNull @PositiveOrZero Long versao
) {}
