package com.manutencao.sistema.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Corpo do POST usado pelo Admin para criar Técnico, Atendente ou outro Admin. */
public record UsuarioEquipeRequest(
        @NotBlank(message = "O nome é obrigatório.") String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.") String email,

        String telefone,

        @NotBlank(message = "A senha é obrigatória.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "A senha deve ter no mínimo 8 caracteres, com letra e número.")
        String senha,

        @NotBlank(message = "O perfil é obrigatório.") String perfil) {
}
