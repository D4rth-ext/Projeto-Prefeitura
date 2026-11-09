package com.manutencao.sistema.usuario.dto;

import com.manutencao.sistema.usuario.domain.Usuario;

public record SolicitanteResumoResponse(Long id, String nome, Long setorId, String setorNome, boolean setorAtivo) {

    public static SolicitanteResumoResponse de(Usuario usuario) {
        var setor = usuario.getSetor();
        return new SolicitanteResumoResponse(usuario.getId(), usuario.getNome(), setor.getId(), setor.getNome(),
                setor.isAtivo());
    }
}
