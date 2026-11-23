package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.equipamento.domain.Equipamento;
import com.manutencao.sistema.equipamento.repository.EquipamentoRepository;
import com.manutencao.sistema.os.domain.ModalidadeOs;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.domain.StatusOs;
import com.manutencao.sistema.os.dto.RecebimentoOsRequest;
import com.manutencao.sistema.os.dto.RecebimentoOsResponse;
import com.manutencao.sistema.os.repository.RecebimentoOsRepository;
import com.manutencao.sistema.os.repository.TriagemOsRepository;
import com.manutencao.sistema.usuario.domain.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecebimentoOsService {

    private static final String TERMO_CIENCIA_RISCO =
            "O solicitante declarou estar ciente do risco de perda de dados "
                    + "e optou por não solicitar backup. Confirmação registrada pelo atendente/admin.";

    private final EquipamentoRepository equipamentoRepository;
    private final RecebimentoOsRepository recebimentoRepository;
    private final TriagemOsRepository auditoria;
    private final AcessoOsService acesso;

    public RecebimentoOsService(
            EquipamentoRepository equipamentoRepository,
            RecebimentoOsRepository recebimentoRepository,
            TriagemOsRepository auditoria,
            AcessoOsService acesso
    ) {
        this.equipamentoRepository = equipamentoRepository;
        this.recebimentoRepository = recebimentoRepository;
        this.auditoria = auditoria;
        this.acesso = acesso;
    }

    @Transactional
    public RecebimentoOsResponse receber(
            Long id,
            RecebimentoOsRequest request,
            String email
    ) {
        Usuario autor = acesso.usuario(email, "Usuário não encontrado.");
        acesso.exigirGestao(autor, "Somente admin e atendente registram o recebimento.");
        OrdemServico os = acesso.consultar(id, autor);
        acesso.conferirVersao(os, request.versao());

        if (os.getModalidade() != ModalidadeOs.BANCADA
                || os.getStatus() != StatusOs.RECEBIDO) {
            throw new RegraNegocioException(
                    "O recebimento exige modalidade BANCADA e status RECEBIDO."
            );
        }

        Equipamento equipamento = equipamentoRepository.findById(request.equipamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Equipamento", request.equipamentoId()
                ));

        if (!equipamento.isAtivo()) {
            throw new RegraNegocioException("O equipamento está inativo.");
        }

        if (!equipamento.getSetor().getId().equals(os.getSetor().getId())) {
            throw new RegraNegocioException(
                    "O equipamento deve pertencer ao setor da OS."
            );
        }

        String escopo = normalizar(os.getBackupSolicitado() == null
                ? request.escopoBackup() : os.getEscopoBackup());
        String midia = normalizar(request.midiaTi());
        String acessorios = normalizar(request.acessorios());
        boolean solicitado = os.getBackupSolicitado() == null
                ? request.backupSolicitado() : os.getBackupSolicitado();

        if (solicitado) {
            if (escopo == null || midia == null) {
                throw new RegraNegocioException(
                        "Backup solicitado exige escopoBackup e midiaTi."
                );
            }
        } else if (!Boolean.TRUE.equals(request.cienciaRiscoConfirmada())) {
            throw new RegraNegocioException(
                    "Sem backup, confirme a ciência de risco do solicitante."
            );
        }

        recebimentoRepository.registrarRecebimento(
                id, equipamento.getId(), acessorios, request.versao()
        );

        recebimentoRepository.registrarBackup(
                id, solicitado, os.getSolicitante().getId(),
                escopo, midia, TERMO_CIENCIA_RISCO
        );

        auditoria.registrarEvento(
                id, "RECEBIMENTO", autor.getId(), null,
                "equipamentoId=" + equipamento.getId() + "; acessorios=" + acessorios,
                null
        );

        auditoria.registrarEvento(
                id, "BACKUP", autor.getId(), null,
                solicitado
                        ? "SOLICITADO; escopo=" + escopo + "; midiaTi=" + midia
                        : "NAO_SOLICITADO",
                solicitado ? null : TERMO_CIENCIA_RISCO
        );

        return recebimentoRepository.buscar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Recebimento da OS", id
                ));
    }

    public RecebimentoOsResponse consultar(Long id, String email) {
        Usuario usuario = acesso.usuario(email);
        acesso.exigirEquipe(usuario);
        acesso.consultar(id, usuario);

        return recebimentoRepository.buscar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Recebimento da OS", id
                ));
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
