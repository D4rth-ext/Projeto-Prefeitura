package com.manutencao.sistema.usuario.dto;

import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.domain.SituacaoCadastro;

public record UsuarioResponse(
        Long id, String nome, String email, String telefone, String perfil,
        SetorResumo setor, boolean ativo, long chamadosAbertos, SituacaoCadastro situacaoCadastro) {

    public record SetorResumo(Long id, String nome) {
    }

    public static UsuarioResponse de(Usuario u) {
        return de(u, 0L);
    }

    public static UsuarioResponse de(Usuario u, long chamadosAbertos) {
        SetorResumo setor = u.getSetor() == null ? null
                : new SetorResumo(u.getSetor().getId(), u.getSetor().getNome());
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(),
                u.getPerfil().name(), setor, u.isAtivo(), chamadosAbertos, u.getSituacaoCadastro());
    }
}
