package com.manutencao.sistema.whatsapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "whatsapp_mensagem_processada")
public class MensagemWhatsAppProcessada {
    @Id
    @Column(name = "mensagem_id", length = 160)
    private String mensagemId;
    @Column(name = "processada_em", nullable = false)
    private Instant processadaEm = Instant.now();
    protected MensagemWhatsAppProcessada() {}
    public MensagemWhatsAppProcessada(String mensagemId) { this.mensagemId = mensagemId; }
}
