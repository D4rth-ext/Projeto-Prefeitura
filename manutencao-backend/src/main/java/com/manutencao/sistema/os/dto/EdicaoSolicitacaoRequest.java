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
        @Size(max = 50, message = "O ID do AnyDesk deve ter até 50 caracteres.") String anydeskId,
        @NotBlank(message = "O telefone para contato é obrigatório.") @Size(max = 60) String contato,
        @Size(max = 100) String melhorHorario,
        @NotNull(message = "Informe se deseja solicitar backup.") Boolean backupSolicitado,
        @Size(max = 2000, message = "A especificação do backup deve ter até 2000 caracteres.") String escopoBackup,
        @NotNull @PositiveOrZero Long versao
) {}
