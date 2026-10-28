package com.manutencao.sistema.setor.dto;

import com.manutencao.sistema.setor.domain.Setor;
import java.time.Instant;

public record SetorResponse(
        Long id,
        String nome,
        String telefone,
        String email,
        String endereco,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static SetorResponse de(Setor setor) {
        return new SetorResponse(
                setor.getId(), setor.getNome(), setor.getTelefone(), setor.getEmail(),
                setor.getEndereco(), setor.isAtivo(), setor.getCriadoEm(), setor.getAtualizadoEm());
    }
}
