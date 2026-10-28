package com.manutencao.sistema.os.domain;

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
import java.time.LocalDate;
import java.time.ZoneId;

@Entity
@Table(name = "ordem_servico")
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "setor_id", nullable = false, updatable = false)
    private Setor setor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false, updatable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aberto_por_id", nullable = false, updatable = false)
    private Usuario abertoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaOs categoria;

    @Column(name = "descricao_problema", nullable = false, columnDefinition = "text")
    private String descricaoProblema;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ModalidadeOs modalidade = ModalidadeOs.A_DEFINIR;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PrioridadeOs prioridade;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_bancada", length = 30)
    private MotivoBancada motivoBancada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOs status = StatusOs.RECEBIDO;

    @Column(name = "aberta_em", nullable = false, updatable = false)
    private Instant abertaEm;

    @Version
    @Column(nullable = false)
    private Long versao;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "equipamento_id")
    private Long equipamentoId;

    @Column(name = "patrimonio_informado", length = 40)
    private String patrimonioInformado;

    @Enumerated(EnumType.STRING)
    @Column(name = "computador_liga", length = 10)
    private RespostaRapida computadorLiga;

    @Enumerated(EnumType.STRING)
    @Column(name = "tem_rede", length = 10)
    private RespostaRapida temRede;

    @Enumerated(EnumType.STRING)
    @Column(name = "consegue_anydesk", length = 10)
    private RespostaRapida consegueAnyDesk;

    @Column(length = 60)
    private String contato;

    @Column(name = "melhor_horario", length = 100)
    private String melhorHorario;

    @Column(name = "prazo_previsto")
    private LocalDate prazoPrevisto;

    @Column(name = "resumo_solucao", columnDefinition = "text")
    private String resumoSolucao;

    @Column(columnDefinition = "text")
    private String acessorios;

    @Column(name = "motivo_cancelamento", columnDefinition = "text")
    private String motivoCancelamento;

    @Column(name = "recebido_em")
    private Instant recebidoEm;

    @Column(name = "retirado_por", length = 120)
    private String retiradoPor;

    @Column(name = "finalizado_em")
    private Instant finalizadoEm;

    @Column(name = "entregue_em")
    private Instant entregueEm;

    @Column(name = "cancelado_em")
    private Instant canceladoEm;

    protected OrdemServico() {
        // Construtor exigido pelo JPA.
    }

    public OrdemServico(
            Setor setor,
            Usuario solicitante,
            Usuario abertoPor,
            CategoriaOs categoria,
            String descricaoProblema
    ) {
        this.setor = setor;
        this.solicitante = solicitante;
        this.abertoPor = abertoPor;
        this.categoria = categoria;
        this.descricaoProblema = descricaoProblema.trim();
    }

    public void aplicarTriagem(
            ModalidadeOs modalidade,
            PrioridadeOs prioridade,
            MotivoBancada motivoBancada
    ) {
        this.modalidade = modalidade;
        this.prioridade = prioridade;
        this.motivoBancada = motivoBancada;
        this.atualizadoEm = Instant.now();
    }

    public void preencherAbertura(
            String patrimonioInformado, RespostaRapida computadorLiga,
            RespostaRapida temRede, RespostaRapida consegueAnyDesk,
            String contato, String melhorHorario
    ) {
        this.patrimonioInformado = normalizar(patrimonioInformado);
        this.computadorLiga = computadorLiga;
        this.temRede = temRede;
        this.consegueAnyDesk = consegueAnyDesk;
        this.contato = normalizar(contato);
        this.melhorHorario = normalizar(melhorHorario);
    }

    public void definirPrazo(LocalDate prazo) {
        this.prazoPrevisto = prazo;
        registrarAlteracao();
    }

    public void alterarStatus(StatusOs novoStatus) {
        this.status = novoStatus;
        registrarAlteracao();
    }

    public void finalizar(String resumo) {
        this.resumoSolucao = resumo.trim();
        this.finalizadoEm = Instant.now();
        alterarStatus(StatusOs.FINALIZADO);
    }

    public void reabrir() {
        this.finalizadoEm = null;
        alterarStatus(StatusOs.EM_ANALISE);
    }

    public void entregar(String responsavel) {
        registrarDevolucao(responsavel);
        alterarStatus(StatusOs.ENTREGUE);
    }

    public void cancelar(String motivo, String responsavel) {
        this.motivoCancelamento = motivo.trim();
        this.canceladoEm = Instant.now();
        if (recebidoEm != null) {
            registrarDevolucao(responsavel);
        }
        alterarStatus(StatusOs.CANCELADO);
    }

    public void registrarAlteracao() {
        this.atualizadoEm = Instant.now();
    }

    public boolean isAtrasada() {
        return prazoPrevisto != null
                && prazoPrevisto.isBefore(LocalDate.now(ZoneId.of("America/Sao_Paulo")))
                && (status == StatusOs.RECEBIDO || status == StatusOs.EM_ANALISE
                    || status == StatusOs.AGUARDANDO_PECA);
    }

    public boolean encerrada() {
        return status == StatusOs.ENTREGUE || status == StatusOs.CANCELADO;
    }

    private void registrarDevolucao(String responsavel) {
        this.retiradoPor = responsavel.trim();
        this.entregueEm = Instant.now();
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        this.abertaEm = agora;
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

    public Usuario getSolicitante() {
        return solicitante;
    }

    public Usuario getAbertoPor() {
        return abertoPor;
    }

    public CategoriaOs getCategoria() {
        return categoria;
    }

    public String getDescricaoProblema() {
        return descricaoProblema;
    }

    public ModalidadeOs getModalidade() {
        return modalidade;
    }

    public PrioridadeOs getPrioridade() {
        return prioridade;
    }

    public MotivoBancada getMotivoBancada() {
        return motivoBancada;
    }

    public StatusOs getStatus() {
        return status;
    }

    public Instant getAbertaEm() {
        return abertaEm;
    }

    public Long getVersao() {
        return versao;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Long getEquipamentoId() {
        return equipamentoId;
    }

    public String getPatrimonioInformado() {
        return patrimonioInformado;
    }

    public RespostaRapida getComputadorLiga() {
        return computadorLiga;
    }

    public RespostaRapida getTemRede() {
        return temRede;
    }

    public RespostaRapida getConsegueAnyDesk() {
        return consegueAnyDesk;
    }

    public String getContato() {
        return contato;
    }

    public String getMelhorHorario() {
        return melhorHorario;
    }

    public LocalDate getPrazoPrevisto() {
        return prazoPrevisto;
    }

    public String getResumoSolucao() {
        return resumoSolucao;
    }

    public String getAcessorios() {
        return acessorios;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public Instant getRecebidoEm() {
        return recebidoEm;
    }

    public String getRetiradoPor() {
        return retiradoPor;
    }

    public Instant getFinalizadoEm() {
        return finalizadoEm;
    }

    public Instant getEntregueEm() {
        return entregueEm;
    }

    public Instant getCanceladoEm() {
        return canceladoEm;
    }
}
