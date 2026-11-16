package com.manutencao.sistema.usuario.dto;

import com.manutencao.sistema.usuario.service.PoliticaSenha;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Corpo do autocadastro público. O perfil NUNCA vem daqui — é fixado no servidor como SOLICITANTE. */
public record RegistroSolicitanteRequest(
        @NotBlank(message = "O nome é obrigatório.") @Size(max = 120) String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.") @Size(max = 160) String email,

        @NotBlank(message = "O telefone é obrigatório.") @Size(max = 20) String telefone,

        @NotNull(message = "O setor é obrigatório.") @Positive Long setorId,

        @NotBlank(message = "A senha é obrigatória.")
        @Pattern(regexp = PoliticaSenha.FORMATO, message = PoliticaSenha.MENSAGEM)
        String senha,

        @NotBlank(message = "Confirme a senha.") String confirmarSenha) {
}
