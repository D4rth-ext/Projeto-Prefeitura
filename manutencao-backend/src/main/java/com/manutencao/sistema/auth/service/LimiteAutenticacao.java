package com.manutencao.sistema.auth.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LimiteAutenticacao {

    private static final int MAX_ENDERECOS = 4096;
    private final Map<String, Integer> logins = new HashMap<>();
    private final Map<String, Integer> cadastros = new HashMap<>();
    private long minuto;

    public void registrarLogin(String endereco) {
        conferir(endereco, logins, 30);
    }

    public void registrarCadastro(String endereco) {
        conferir(endereco, cadastros, 5);
    }

    private synchronized void conferir(String endereco, Map<String, Integer> contagem, int limite) {
        long minutoAtual = Instant.now().getEpochSecond() / 60;
        if (minutoAtual != minuto) {
            logins.clear();
            cadastros.clear();
            minuto = minutoAtual;
        }
        Integer usados = contagem.get(endereco);
        if ((usados == null && contagem.size() >= MAX_ENDERECOS) || (usados != null && usados >= limite)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Aguarde um minuto e tente novamente.");
        }
        contagem.put(endereco, usados == null ? 1 : usados + 1);
    }
}
