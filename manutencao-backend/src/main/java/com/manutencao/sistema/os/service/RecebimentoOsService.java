package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.ConflitoException;
import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.equipamento.domain.Equipamento;
import com.manutencao.sistema.equipamento.repository.EquipamentoRepository;
import com.manutencao.sistema.os.domain.ModalidadeOs;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.domain.StatusOs;
import com.manutencao.sistema.os.dto.RecebimentoOsRequest;
import com.manutencao.sistema.os.dto.RecebimentoOsResponse;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.os.repository.RecebimentoOsRepository;
import com.manutencao.sistema.os.repository.TriagemOsRepository;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class RecebimentoOsService {

    private static final String TERMO_CIENCIA_RISCO =
            "O solicitante declarou estar ciente do risco de perda de dados "
                    + "e optou por não solicitar backup. Confirmação registrada pelo atendente/admin.";

    private final OrdemServicoRepository osRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RecebimentoOsRepository recebimentoRepository;
    private final TriagemOsRepository auditoria;
    private final OrdemServicoService osService;

    public RecebimentoOsService(
            OrdemServicoRepository osRepository,
            EquipamentoRepository equipamentoRepository,
            UsuarioRepository usuarioRepository,
            RecebimentoOsRepository recebimentoRepository,
            TriagemOsRepository auditoria,
            OrdemServicoService osService
    ) {
        this.osRepository = osRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.recebimentoRepository = recebimentoRepository;
        this.auditoria = auditoria;
        this.osService = osService;
    }

    @Transactional
    public RecebimentoOsResponse receber(
            Long id,
            RecebimentoOsRequest request,
            String email
    ) {
        Usuario autor = usuarioAutenticado(email);

        if (autor.getPerfil() != Perfil.ADMIN && autor.getPerfil() != Perfil.ATENDENTE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Somente admin e atendente registram o recebimento."
            );
        }

        OrdemServico os = osRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("OS", id));

        if (!os.getVersao().equals(request.versao())) {
            throw new ConflitoException(
                    "A OS foi alterada. Consulte novamente e use a versão atual."
            );
        }

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

        String escopo = normalizar(request.escopoBackup());
        String midia = normalizar(request.midiaTi());
        String acessorios = normalizar(request.acessorios());
        boolean solicitado = request.backupSolicitado();

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
        osService.buscarPorId(id, email);

        return recebimentoRepository.buscar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Recebimento da OS", id
                ));
    }

    private Usuario usuarioAutenticado(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário não encontrado."
                ));

        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "O usuário está inativo."
            );
        }

        return usuario;
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}