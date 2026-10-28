package com.manutencao.sistema.equipamento.dto;

import com.manutencao.sistema.equipamento.domain.TipoEquipamento;

public record TipoEquipamentoResponse(Long id, String nome) {

    public static TipoEquipamentoResponse de(TipoEquipamento tipo) {
        return new TipoEquipamentoResponse(tipo.getId(), tipo.getNome());
    }
}
