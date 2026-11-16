package com.manutencao.sistema.os.domain;

public enum StatusOs {
    RECEBIDO,
    EM_ANALISE,
    AGUARDANDO_PECA,
    FINALIZADO,
    ENTREGUE,
    CANCELADO;

    public boolean emAtendimento() {
        return this == RECEBIDO || this == EM_ANALISE || this == AGUARDANDO_PECA;
    }

    public boolean terminal() {
        // Uma OS finalizada ainda pode ser reaberta.
        return this == ENTREGUE || this == CANCELADO;
    }
}
