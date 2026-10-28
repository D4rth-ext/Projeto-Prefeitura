package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.os.domain.ModalidadeOs;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.domain.SituacaoBackup;
import com.manutencao.sistema.os.domain.StatusOs;
import com.manutencao.sistema.os.dto.BackupOsResponse;
import com.manutencao.sistema.os.dto.OrdemServicoResponse;
import com.manutencao.sistema.os.dto.PrazoOsRequest;
import com.manutencao.sistema.os.dto.StatusOsRequest;
import com.manutencao.sistema.os.repository.BackupOsRepository;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.os.repository.RegistroOsRepository;
import com.manutencao.sistema.os.repository.TriagemOsRepository;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FluxoOsService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final AcessoOsService acesso;
    private final OrdemServicoRepository osRepository;
    private final TriagemOsRepository auditoria;
    private final RegistroOsRepository registroRepository;
    private final BackupOsRepository backupRepository;

    public FluxoOsService(AcessoOsService acesso, OrdemServicoRepository osRepository,
                         TriagemOsRepository auditoria, RegistroOsRepository registroRepository,
                         BackupOsRepository backupRepository) {
        this.acesso = acesso;
        this.osRepository = osRepository;
        this.auditoria = auditoria;
        this.registroRepository = registroRepository;
        this.backupRepository = backupRepository;
    }

    @Transactional
    public OrdemServicoResponse alterarPrazo(Long id, PrazoOsRequest request, String email) {
        Usuario autor = acesso.usuario(email);
        OrdemServico os = acesso.consultar(id, autor);
        acesso.exigirTecnicoOuAdmin(os, autor);
        acesso.conferirVersao(os, request.versao());

        if (os.encerrada() || os.getStatus() == StatusOs.FINALIZADO) {
            throw new RegraNegocioException("Só é possível alterar prazo de uma OS em atendimento.");
        }

        validarPrazo(request.prazoPrevisto());
        LocalDate anterior = os.getPrazoPrevisto();
        os.definirPrazo(request.prazoPrevisto());
        osRepository.saveAndFlush(os);
        auditoria.registrarEvento(id, "PRAZO", autor.getId(), anterior,
                request.prazoPrevisto(), request.justificativa().trim());
        return OrdemServicoResponse.de(os);
    }

    @Transactional
    public OrdemServicoResponse alterarStatus(Long id, StatusOsRequest request, String email) {
        Usuario autor = acesso.usuario(email);
        acesso.exigirEquipe(autor);
        OrdemServico os = acesso.consultar(id, autor);
        acesso.conferirVersao(os, request.versao());
        StatusOs anterior = os.getStatus();

        if (os.encerrada() || anterior == request.status()) {
            throw new RegraNegocioException("A OS está encerrada ou já possui o status informado.");
        }

        if (request.prazoPrevisto() != null
                && !(anterior == StatusOs.RECEBIDO && request.status() == StatusOs.EM_ANALISE)) {
            throw new RegraNegocioException("Informe prazoPrevisto ao iniciar a análise; para ajustes, use a rota de prazo.");
        }

        if (request.status() == StatusOs.CANCELADO) {
            cancelar(os, request, autor);
        } else if (anterior == StatusOs.RECEBIDO && request.status() == StatusOs.EM_ANALISE) {
            iniciar(os, request, autor);
        } else if (anterior == StatusOs.EM_ANALISE && request.status() == StatusOs.AGUARDANDO_PECA) {
            acesso.exigirTecnicoOuAdmin(os, autor);
            exigirTexto(request.justificativa(), "Informe a peça faltante na justificativa.");
            if (request.registroPecaId() == null
                    || !registroRepository.existeApontamento(id, request.registroPecaId())) {
                throw new RegraNegocioException("Informe registroPecaId de um diagnóstico ou ação desta OS que descreva a peça faltante.");
            }
            os.alterarStatus(StatusOs.AGUARDANDO_PECA);
        } else if (anterior == StatusOs.AGUARDANDO_PECA && request.status() == StatusOs.EM_ANALISE) {
            acesso.exigirTecnicoOuAdmin(os, autor);
            validarEquipe(os);
            os.alterarStatus(StatusOs.EM_ANALISE);
        } else if (anterior == StatusOs.EM_ANALISE && request.status() == StatusOs.RECEBIDO) {
            escalonar(os, request, autor);
        } else if (anterior == StatusOs.EM_ANALISE && request.status() == StatusOs.FINALIZADO) {
            finalizar(os, request, autor);
        } else if (anterior == StatusOs.FINALIZADO && request.status() == StatusOs.EM_ANALISE) {
            acesso.exigirTecnicoOuAdmin(os, autor);
            exigirTexto(request.justificativa(), "A reabertura exige justificativa.");
            if (os.getEntregueEm() != null) {
                throw new RegraNegocioException("Não é possível reabrir depois da entrega.");
            }
            validarEquipe(os);
            os.reabrir();
        } else if (anterior == StatusOs.FINALIZADO && request.status() == StatusOs.ENTREGUE) {
            acesso.exigirGestao(autor);
            if (os.getModalidade() != ModalidadeOs.BANCADA || os.getRecebidoEm() == null) {
                throw new RegraNegocioException("A entrega física é exclusiva de um equipamento recebido na bancada.");
            }
            exigirTexto(request.retiradoPor(), "Informe o nome de quem retirou o equipamento.");
            os.entregar(request.retiradoPor());
        } else {
            throw new RegraNegocioException("Transição de status não permitida.");
        }

        osRepository.saveAndFlush(os);
        auditoria.registrarEvento(id, "STATUS", autor.getId(), anterior, os.getStatus(),
                normalizar(request.justificativa()));
        return OrdemServicoResponse.de(os);
    }

    private void iniciar(OrdemServico os, StatusOsRequest request, Usuario autor) {
        acesso.exigirTecnicoOuAdmin(os, autor);
        validarEquipe(os);
        LocalDate prazo = request.prazoPrevisto() == null ? os.getPrazoPrevisto() : request.prazoPrevisto();
        validarPrazo(prazo);

        if (os.getModalidade() == ModalidadeOs.BANCADA) {
            if (os.getEquipamentoId() == null || os.getRecebidoEm() == null) {
                throw new RegraNegocioException("Bancada exige recebimento do equipamento antes da análise.");
            }
            backup(os.getId());
        }

        if (!prazo.equals(os.getPrazoPrevisto())) {
            LocalDate anterior = os.getPrazoPrevisto();
            os.definirPrazo(prazo);
            auditoria.registrarEvento(os.getId(), "PRAZO", autor.getId(), anterior, prazo,
                    "Prazo definido ao iniciar a análise.");
        }
        os.alterarStatus(StatusOs.EM_ANALISE);
    }

    private void validarEquipe(OrdemServico os) {
        if (os.getModalidade() == ModalidadeOs.A_DEFINIR || os.getPrioridade() == null
                || auditoria.quantidadeTecnicosAtivos(os.getId()) == 0) {
            throw new RegraNegocioException("A análise exige modalidade, prioridade e pelo menos um técnico ativo.");
        }
    }

    private void escalonar(OrdemServico os, StatusOsRequest request, Usuario autor) {
        if (autor.getPerfil() != Perfil.ATENDENTE) {
            acesso.exigirTecnicoOuAdmin(os, autor);
        }
        if (os.getModalidade() != ModalidadeOs.REMOTO && os.getModalidade() != ModalidadeOs.PRESENCIAL) {
            throw new RegraNegocioException("Só atendimento remoto ou presencial pode ser escalonado para bancada.");
        }
        exigirTexto(request.justificativa(), "O escalonamento exige justificativa.");
        if (request.motivoBancada() == null) {
            throw new RegraNegocioException("Informe motivoBancada.");
        }
        ModalidadeOs anterior = os.getModalidade();
        os.aplicarTriagem(ModalidadeOs.BANCADA, os.getPrioridade(), request.motivoBancada());
        os.alterarStatus(StatusOs.RECEBIDO);
        auditoria.registrarEvento(os.getId(), "MODALIDADE", autor.getId(), anterior,
                "BANCADA (" + request.motivoBancada() + ")", request.justificativa().trim());
    }

    private void finalizar(OrdemServico os, StatusOsRequest request, Usuario autor) {
        acesso.exigirTecnicoOuAdmin(os, autor);
        exigirTexto(request.resumoSolucao(), "A finalização exige resumoSolucao.");
        if (!registroRepository.existeDiagnostico(os.getId())) {
            throw new RegraNegocioException("A finalização exige pelo menos um diagnóstico registrado.");
        }
        if (os.getModalidade() == ModalidadeOs.BANCADA) {
            BackupOsResponse backup = backup(os.getId());
            if (backup.solicitado() && backup.situacao() != SituacaoBackup.RESTAURADO_DEVOLVIDO) {
                throw new RegraNegocioException("Finalize somente após restaurar/devolver o backup solicitado.");
            }
        }
        os.finalizar(request.resumoSolucao());
    }

    private void cancelar(OrdemServico os, StatusOsRequest request, Usuario autor) {
        if (autor.getPerfil() == Perfil.TECNICO) {
            acesso.exigirTecnicoOuAdmin(os, autor);
        } else {
            acesso.exigirGestao(autor);
        }
        exigirTexto(request.justificativa(), "O cancelamento exige justificativa.");
        if (os.getRecebidoEm() != null) {
            exigirTexto(request.retiradoPor(), "Devolva o equipamento e informe quem o retirou antes de cancelar.");
        }
        os.cancelar(request.justificativa(), request.retiradoPor());
    }

    private BackupOsResponse backup(Long id) {
        BackupOsResponse backup = backupRepository.buscar(id)
                .orElseThrow(() -> new RegraNegocioException("Responda o protocolo de backup antes da análise."));
        if (backup.solicitado() && (normalizar(backup.escopo()) == null
                || normalizar(backup.midiaTi()) == null || backup.situacao() == null)
                || !backup.solicitado() && (backup.cienciaRiscoPorId() == null
                || backup.cienciaRiscoEm() == null || normalizar(backup.termoCienciaRisco()) == null)) {
            throw new RegraNegocioException("O protocolo de backup está incompleto.");
        }
        return backup;
    }

    private void validarPrazo(LocalDate prazo) {
        if (prazo == null || prazo.isBefore(LocalDate.now(FUSO))) {
            throw new RegraNegocioException("Informe prazoPrevisto sem usar uma data passada.");
        }
    }

    private void exigirTexto(String texto, String mensagem) {
        if (normalizar(texto) == null) {
            throw new RegraNegocioException(mensagem);
        }
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
