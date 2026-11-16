package com.manutencao.sistema.usuario.dto;

import com.manutencao.sistema.usuario.service.PoliticaSenha;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpo do POST usado pelo Admin para criar Técnico, Atendente ou outro Admin. */
public record UsuarioEquipeRequest(
        @NotBlank(message = "O nome é obrigatório.") @Size(max = 120) String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.") @Size(max = 160) String email,

        @Size(max = 20) String telefone,

        @NotBlank(message = "A senha é obrigatória.")
        @Pattern(regexp = PoliticaSenha.FORMATO, message = PoliticaSenha.MENSAGEM)
        String senha,

        @NotBlank(message = "O perfil é obrigatório.") String perfil) {
}
