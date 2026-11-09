package com.manutencao.sistema.usuario.service;

import com.manutencao.sistema.common.exception.RegraNegocioException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class PoliticaSenha {

    public static final String FORMATO = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";
    public static final String MENSAGEM = "A senha deve ter no mínimo 8 caracteres, com letra e número.";
    private static final Pattern PADRAO = Pattern.compile(FORMATO);

    private PoliticaSenha() {
    }

    public static void validar(String senha) {
        if (senha == null || !PADRAO.matcher(senha).matches()) {
            throw new RegraNegocioException(MENSAGEM);
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegraNegocioException("A senha excede o limite de 72 bytes.");
        }
    }
}
