package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.domain.SituacaoBackup;
import com.manutencao.sistema.os.dto.AtualizacaoBackupRequest;
import com.manutencao.sistema.os.dto.BackupOsResponse;
import com.manutencao.sistema.os.dto.DescarteBackupRequest;
import com.manutencao.sistema.os.repository.BackupOsRepository;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.os.repository.TriagemOsRepository;
import com.manutencao.sistema.usuario.domain.Usuario;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BackupOsService {

    private final AcessoOsService acesso;
    private final OrdemServicoRepository osRepository;
    private final BackupOsRepository backupRepository;
    private final TriagemOsRepository auditoria;

    public BackupOsService(AcessoOsService acesso, OrdemServicoRepository osRepository,
                           BackupOsRepository backupRepository, TriagemOsRepository auditoria) {
        this.acesso = acesso;
        this.osRepository = osRepository;
        this.backupRepository = backupRepository;
        this.auditoria = auditoria;
    }

    public BackupOsResponse consultar(Long id, String email) {
        acesso.consultar(id, acesso.usuario(email));
        return protocolo(id);
    }

    public List<BackupOsResponse> descartesPendentes(String email) {
        acesso.exigirGestao(acesso.usuario(email));
        return backupRepository.descartesPendentes();
    }

    @Transactional
    public BackupOsResponse atualizar(Long id, AtualizacaoBackupRequest request, String email) {
        Usuario autor = acesso.usuario(email);
        acesso.exigirGestao(autor);
        OrdemServico os = acesso.consultar(id, autor);
        acesso.conferirVersao(os, request.versao());
        BackupOsResponse backup = protocolo(id);

        if (!backup.solicitado() || backup.descartadoEm() != null) {
            throw new RegraNegocioException("Não existe backup solicitado disponível para atualização.");
        }

        boolean permitido = backup.situacao() == SituacaoBackup.PENDENTE
                && request.situacao() == SituacaoBackup.CONCLUIDO
                || backup.situacao() == SituacaoBackup.CONCLUIDO
                && request.situacao() == SituacaoBackup.RESTAURADO_DEVOLVIDO;

        if (!permitido) {
            throw new RegraNegocioException("Siga PENDENTE → CONCLUIDO → RESTAURADO_DEVOLVIDO.");
        }

        os.registrarAlteracao();
        osRepository.saveAndFlush(os);
        backupRepository.atualizarSituacao(id, request.situacao());
        auditoria.registrarEvento(id, "BACKUP", autor.getId(), backup.situacao(), request.situacao(), null);
        return protocolo(id);
    }

    @Transactional
    public BackupOsResponse descartar(Long id, DescarteBackupRequest request, String email) {
        Usuario autor = acesso.usuario(email);
        acesso.exigirGestao(autor);
        OrdemServico os = acesso.consultar(id, autor);
        acesso.conferirVersao(os, request.versao());
        BackupOsResponse backup = protocolo(id);

        if (!Boolean.TRUE.equals(request.descarteConfirmado())) {
            throw new RegraNegocioException("Confirme o descarte dos dados da mídia do TI.");
        }

        if (!backup.solicitado() || backup.situacao() != SituacaoBackup.RESTAURADO_DEVOLVIDO
                || os.getEntregueEm() == null || backup.descartadoEm() != null) {
            throw new RegraNegocioException("O descarte exige backup devolvido e equipamento entregue, sem descarte anterior.");
        }

        os.registrarAlteracao();
        osRepository.saveAndFlush(os);
        backupRepository.registrarDescarte(id, autor.getId());
        auditoria.registrarEvento(id, "BACKUP", autor.getId(), backup.situacao(), "DESCARTADO",
                "Eliminação dos dados da mídia do TI confirmada pelo responsável.");
        return protocolo(id);
    }

    private BackupOsResponse protocolo(Long id) {
        return backupRepository.buscar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Protocolo de backup da OS", id));
    }
}
