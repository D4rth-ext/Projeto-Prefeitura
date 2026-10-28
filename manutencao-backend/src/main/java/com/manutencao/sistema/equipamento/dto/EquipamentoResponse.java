package com.manutencao.sistema.equipamento.dto;

import com.manutencao.sistema.equipamento.domain.Equipamento;
import java.time.Instant;

public record EquipamentoResponse(
        Long id,
        SetorResumo setor,
        TipoResumo tipo,
        String marca,
        String modelo,
        String patrimonio,
        String numeroSerie,
        String observacoes,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm) {

    public record SetorResumo(Long id, String nome) {
    }

    public record TipoResumo(Long id, String nome) {
    }

    public static EquipamentoResponse de(Equipamento e) {
        return new EquipamentoResponse(
                e.getId(),
                new SetorResumo(e.getSetor().getId(), e.getSetor().getNome()),
                new TipoResumo(e.getTipo().getId(), e.getTipo().getNome()),
                e.getMarca(), e.getModelo(), e.getPatrimonio(), e.getNumeroSerie(), e.getObservacoes(),
                e.isAtivo(), e.getCriadoEm(), e.getAtualizadoEm());
    }
}
