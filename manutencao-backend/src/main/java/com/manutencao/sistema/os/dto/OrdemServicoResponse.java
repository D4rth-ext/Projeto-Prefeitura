package com.manutencao.sistema.os.dto;

import com.manutencao.sistema.os.domain.CategoriaOs;
import com.manutencao.sistema.os.domain.ModalidadeOs;
import com.manutencao.sistema.os.domain.MotivoBancada;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.domain.PrioridadeOs;
import com.manutencao.sistema.os.domain.StatusOs;
import java.time.Instant;
import java.time.LocalDate;
import com.manutencao.sistema.os.domain.RespostaRapida;

public record OrdemServicoResponse(
        Long id,
        Long setorId,
        String setorNome,
        Long solicitanteId,
        String solicitanteNome,
        Long abertoPorId,
        String abertoPorNome,
        CategoriaOs categoria,
        String descricaoProblema,
        StatusOs status,
        ModalidadeOs modalidade,
        PrioridadeOs prioridade,
        MotivoBancada motivoBancada,
        Instant abertaEm,
        Instant atualizadoEm,
        Long versao,
        Long equipamentoId,
        String patrimonioInformado,
        RespostaRapida computadorLiga,
        RespostaRapida temRede,
        RespostaRapida consegueAnyDesk,
        String contato,
        String melhorHorario,
        LocalDate prazoPrevisto,
        String resumoSolucao,
        String acessorios,
        String motivoCancelamento,
        Instant recebidoEm,
        String retiradoPor,
        Instant finalizadoEm,
        Instant entregueEm,
        Instant canceladoEm,
        boolean atrasada
) {

    public static OrdemServicoResponse de(OrdemServico os) {
        return new OrdemServicoResponse(
                os.getId(),
                os.getSetor().getId(),
                os.getSetor().getNome(),
                os.getSolicitante().getId(),
                os.getSolicitante().getNome(),
                os.getAbertoPor().getId(),
                os.getAbertoPor().getNome(),
                os.getCategoria(),
                os.getDescricaoProblema(),
                os.getStatus(),
                os.getModalidade(),
                os.getPrioridade(),
                os.getMotivoBancada(),
                os.getAbertaEm(),
                os.getAtualizadoEm(),
                os.getVersao(),
                os.getEquipamentoId(),
                os.getPatrimonioInformado(),
                os.getComputadorLiga(),
                os.getTemRede(),
                os.getConsegueAnyDesk(),
                os.getContato(),
                os.getMelhorHorario(),
                os.getPrazoPrevisto(),
                os.getResumoSolucao(),
                os.getAcessorios(),
                os.getMotivoCancelamento(),
                os.getRecebidoEm(),
                os.getRetiradoPor(),
                os.getFinalizadoEm(),
                os.getEntregueEm(),
                os.getCanceladoEm(),
                os.isAtrasada()
        );
    }
}
