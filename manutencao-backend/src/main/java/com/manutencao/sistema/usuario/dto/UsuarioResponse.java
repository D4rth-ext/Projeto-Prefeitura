package com.manutencao.sistema.usuario.dto;

import com.manutencao.sistema.usuario.domain.Usuario;

public record UsuarioResponse(
        Long id, String nome, String email, String telefone, String perfil,
        SetorResumo setor, boolean ativo) {

    public record SetorResumo(Long id, String nome) {
    }

    public static UsuarioResponse de(Usuario u) {
        SetorResumo setor = u.getSetor() == null ? null
                : new SetorResumo(u.getSetor().getId(), u.getSetor().getNome());
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(),
                u.getPerfil().name(), setor, u.isAtivo());
    }
}
