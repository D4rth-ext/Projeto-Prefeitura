package com.manutencao.sistema.usuario.dto;

import com.manutencao.sistema.usuario.service.PoliticaSenha;
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
        @Pattern(regexp = PoliticaSenha.FORMATO, message = PoliticaSenha.MENSAGEM)
        String senha,

        @NotBlank(message = "Confirme a senha.") String confirmarSenha) {
}
