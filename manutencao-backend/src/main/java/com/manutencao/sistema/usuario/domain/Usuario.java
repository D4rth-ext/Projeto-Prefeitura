package com.manutencao.sistema.usuario.domain;

import com.manutencao.sistema.setor.domain.Setor;
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
import java.time.Instant;
import java.util.UUID;

/** Espelha "usuario" (V1__init.sql). Senha sempre em hash (BCrypt), nunca em texto puro. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(name = "revisao_senha", nullable = false)
    private UUID revisaoSenha = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id")
    private Setor setor; // só para SOLICITANTE

    @Column(nullable = false)
    private boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_cadastro", nullable = false, length = 12)
    private SituacaoCadastro situacaoCadastro = SituacaoCadastro.APROVADO;

    @Column(name = "senha_temporaria_expira_em")
    private Instant senhaTemporariaExpiraEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Usuario() {
        // exigido pelo JPA
    }

    /** Uso geral (Admin, Atendente, Técnico): sem setor. */
    public Usuario(String nome, String email, String telefone, String senhaHash, Perfil perfil) {
        this(nome, email, telefone, senhaHash, perfil, null);
    }

    /** Uso para Solicitante: com setor. */
    public Usuario(String nome, String email, String telefone, String senhaHash, Perfil perfil, Setor setor) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.setor = setor;
    }

    public void inativar() {
        this.ativo = false;
        this.revisaoSenha = UUID.randomUUID();
    }

    public void aguardarAprovacao() {
        this.situacaoCadastro = SituacaoCadastro.PENDENTE;
        this.ativo = false;
    }

    public void decidirCadastro(SituacaoCadastro decisao) {
        this.situacaoCadastro = decisao;
        this.ativo = decisao == SituacaoCadastro.APROVADO;
    }

    public void editarDadosDaEquipe(String nome, String email, String telefone) {
        this.nome = nome;
        this.telefone = telefone;
        if (!this.email.equals(email)) {
            this.email = email;
            this.revisaoSenha = UUID.randomUUID();
        }
    }

    public void alterarSetor(Setor setor) {
        this.setor = setor;
    }

    public void trocarSenha(String novaSenhaHash) {
        this.senhaHash = novaSenhaHash;
        this.revisaoSenha = UUID.randomUUID();
        this.senhaTemporariaExpiraEm = null;
    }

    public void definirSenhaTemporaria(String hash, Instant expiraEm) {
        trocarSenha(hash);
        this.senhaTemporariaExpiraEm = expiraEm;
    }

    public boolean senhaTemporaria() {
        return senhaTemporariaExpiraEm != null;
    }

    public boolean senhaTemporariaVencida() {
        return senhaTemporaria() && !senhaTemporariaExpiraEm.isAfter(Instant.now());
    }

    public SituacaoCadastro getSituacaoCadastro() {
        return situacaoCadastro;
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

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() { return telefone;}

    public String getSenhaHash() {
        return senhaHash;
    }

    public UUID getRevisaoSenha() {
        return revisaoSenha;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public Setor getSetor() {
        return setor;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
