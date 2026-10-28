package com.manutencao.sistema.equipamento.domain;

import com.manutencao.sistema.setor.domain.Setor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Espelha "equipamento" (V1__init.sql). Sempre vinculado a um SETOR, nunca a uma pessoa.
 * Acessórios, backup e defeito NÃO ficam aqui — vão para a OS (Sprint 3), pois mudam a cada entrada.
 */
@Entity
@Table(name = "equipamento")
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "setor_id", nullable = false)
    private Setor setor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_id", nullable = false)
    private TipoEquipamento tipo;

    @Column(length = 60)
    private String marca;

    @Column(length = 80)
    private String modelo;

    @Column(length = 40)
    private String patrimonio;

    @Column(name = "numero_serie", length = 80)
    private String numeroSerie;

    @Column(columnDefinition = "text")
    private String observacoes;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Equipamento() {
        // exigido pelo JPA
    }

    public Equipamento(Setor setor, TipoEquipamento tipo, String marca, String modelo,
                       String patrimonio, String numeroSerie, String observacoes) {
        atualizarDados(setor, tipo, marca, modelo, patrimonio, numeroSerie, observacoes);
    }

    public void atualizarDados(Setor setor, TipoEquipamento tipo, String marca, String modelo,
                               String patrimonio, String numeroSerie, String observacoes) {
        this.setor = setor;
        this.tipo = tipo;
        this.marca = marca;
        this.modelo = modelo;
        this.patrimonio = patrimonio;
        this.numeroSerie = numeroSerie;
        this.observacoes = observacoes;
    }

    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Setor getSetor() {
        return setor;
    }

    public TipoEquipamento getTipo() {
        return tipo;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public String getPatrimonio() {
        return patrimonio;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
