package com.manutencao.sistema.agenda.domain;

import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.setor.domain.Setor;
import com.manutencao.sistema.usuario.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "agendamento")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tecnico_id", nullable = false)
    private Usuario tecnico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "os_id")
    private OrdemServico os;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id")
    private Setor setor;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(length = 255)
    private String local;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_id", nullable = false, updatable = false)
    private Usuario criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long versao;

    protected Agendamento() {
    }

    public Agendamento(Usuario tecnico, OrdemServico os, Setor setor, String titulo,
                        String descricao, String local, Instant inicio, Instant fim, Usuario criadoPor) {
        atualizar(tecnico, os, setor, titulo, descricao, local, inicio, fim);
        this.criadoPor = criadoPor;
    }

    public void atualizar(Usuario tecnico, OrdemServico os, Setor setor, String titulo,
                          String descricao, String local, Instant inicio, Instant fim) {
        this.tecnico = tecnico;
        this.os = os;
        this.setor = setor;
        this.titulo = titulo.trim();
        this.descricao = normalizar(descricao);
        this.local = normalizar(local);
        this.inicio = inicio;
        this.fim = fim;
        this.atualizadoEm = Instant.now();
    }

    public void alterarStatus(StatusAgendamento status) {
        this.status = status;
        this.atualizadoEm = Instant.now();
    }

    @PrePersist
    void aoCriar() {
        this.criadoEm = Instant.now();
        this.atualizadoEm = criadoEm;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    public Long getId() {
        return id;
    }

    public Usuario getTecnico() {
        return tecnico;
    }

    public OrdemServico getOs() {
        return os;
    }

    public Setor getSetor() {
        return setor;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getLocal() {
        return local;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFim() {
        return fim;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    public Usuario getCriadoPor() {
        return criadoPor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Long getVersao() {
        return versao;
    }
}
