package com.manutencao.sistema.whatsapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "whatsapp_conversa")
public class ConversaWhatsApp {
    @Id
    @Column(length = 20)
    private String telefone;

    @Column(nullable = false, length = 20)
    private String etapa;

    @Column(length = 30)
    private String categoria;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ConversaWhatsApp() {}

    public ConversaWhatsApp(String telefone, String etapa) {
        this.telefone = telefone;
        this.etapa = etapa;
        this.atualizadoEm = Instant.now();
    }

    public String getEtapa() { return etapa; }
    public String getCategoria() { return categoria; }
    public String getDescricao() { return descricao; }

    public void atualizar(String etapa, String categoria, String descricao) {
        this.etapa = etapa;
        this.categoria = categoria;
        this.descricao = descricao;
        this.atualizadoEm = Instant.now();
    }
}
