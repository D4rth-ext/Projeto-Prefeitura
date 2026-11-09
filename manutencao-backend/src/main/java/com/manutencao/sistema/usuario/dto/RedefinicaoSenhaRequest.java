package com.manutencao.sistema.usuario.dto;

import jakarta.validation.constraints.NotBlank;

public record RedefinicaoSenhaRequest(String senhaAtual, @NotBlank String novaSenha, @NotBlank String confirmarSenha) {

    @Override
    public String toString() {
        return "RedefinicaoSenhaRequest";
    }
}
