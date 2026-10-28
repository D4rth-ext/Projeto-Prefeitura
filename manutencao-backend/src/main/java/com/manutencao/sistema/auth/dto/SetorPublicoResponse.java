package com.manutencao.sistema.auth.dto;

import com.manutencao.sistema.setor.domain.Setor;

/** Só id e nome — usado no formulário de autocadastro (sem expor telefone/e-mail do setor). */
public record SetorPublicoResponse(Long id, String nome) {

    public static SetorPublicoResponse de(Setor setor) {
        return new SetorPublicoResponse(setor.getId(), setor.getNome());
    }
}
