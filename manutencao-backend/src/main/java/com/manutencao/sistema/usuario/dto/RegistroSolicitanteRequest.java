package com.manutencao.sistema.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Corpo do autocadastro público. O perfil NUNCA vem daqui — é fixado no servidor como SOLICITANTE. */
public record RegistroSolicitanteRequest(
        @NotBlank(message = "O nome é obrigatório.") String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.") String email,

        String telefone,

        @NotNull(message = "O setor é obrigatório.") Long setorId,

        @NotBlank(message = "A senha é obrigatória.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "A senha deve ter no mínimo 8 caracteres, com letra e número.")
        String senha,

        @NotBlank(message = "Confirme a senha.") String confirmarSenha) {
}
