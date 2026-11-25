package com.manutencao.sistema.os.dto;

import com.manutencao.sistema.os.domain.CategoriaOs;
import com.manutencao.sistema.os.domain.RespostaRapida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AberturaOsRequest(
        @NotNull(message = "A categoria é obrigatória.")
        CategoriaOs categoria,

        @NotBlank(message = "A descrição do problema é obrigatória.")
        String descricaoProblema,

        @Positive(message = "O ID do solicitante deve ser positivo.")
        Long solicitanteId,

        @Size(max = 40) String patrimonioInformado,
        RespostaRapida computadorLiga,
        RespostaRapida temRede,
        RespostaRapida consegueAnyDesk,
        @Size(max = 50, message = "O ID do AnyDesk deve ter até 50 caracteres.") String anydeskId,
        @NotBlank(message = "O telefone para contato é obrigatório.")
        @Size(max = 60) String contato,
        @Size(max = 100) String melhorHorario,
        @NotNull(message = "Informe se deseja solicitar backup.") Boolean backupSolicitado,
        @Size(max = 2000, message = "A especificação do backup deve ter até 2000 caracteres.") String escopoBackup
) {
    public AberturaOsRequest(CategoriaOs categoria, String descricaoProblema, Long solicitanteId) {
        this(categoria, descricaoProblema, solicitanteId, null, null, null, null, null, null, null, false, null);
    }
}
