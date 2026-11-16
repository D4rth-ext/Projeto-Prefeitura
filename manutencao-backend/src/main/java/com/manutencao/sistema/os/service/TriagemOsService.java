package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.os.domain.ModalidadeOs;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.domain.StatusOs;
import com.manutencao.sistema.os.dto.OrdemServicoResponse;
import com.manutencao.sistema.os.dto.TriagemOsRequest;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.os.repository.TriagemOsRepository;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TriagemOsService {

    private final OrdemServicoRepository osRepository;
    private final UsuarioRepository usuarioRepository;
    private final TriagemOsRepository triagemRepository;
    private final AcessoOsService acesso;

    public TriagemOsService(
            OrdemServicoRepository osRepository,
            UsuarioRepository usuarioRepository,
            TriagemOsRepository triagemRepository,
            AcessoOsService acesso
    ) {
        this.osRepository = osRepository;
        this.usuarioRepository = usuarioRepository;
        this.triagemRepository = triagemRepository;
        this.acesso = acesso;
    }

    @Transactional
    public OrdemServicoResponse triar(Long id, TriagemOsRequest request, String email) {
        Usuario autor = acesso.usuario(email, "Usuário não encontrado.");
        acesso.exigirGestao(autor, "Somente admin e atendente realizam triagem.");
        OrdemServico os = acesso.consultar(id, autor);
        acesso.conferirVersao(os, request.versao());

        if (!os.getStatus().emAtendimento()) {
            throw new RegraNegocioException("Só é possível ajustar a triagem de uma OS em atendimento.");
        }

        if (os.getStatus() != StatusOs.RECEBIDO && os.getModalidade() != request.modalidade()) {
            throw new RegraNegocioException("Para encaminhar à bancada durante a análise, use o escalonamento.");
        }

        if (os.getRecebidoEm() != null && request.modalidade() != ModalidadeOs.BANCADA) {
            throw new RegraNegocioException("Uma OS com equipamento recebido deve permanecer em BANCADA.");
        }

        if (request.modalidade() == ModalidadeOs.A_DEFINIR) {
            throw new RegraNegocioException("Escolha REMOTO, PRESENCIAL ou BANCADA.");
        }

        String justificativa = normalizar(request.justificativa());
        String motivoRemocao = normalizar(request.motivoRemocao());

        if (request.modalidade() == ModalidadeOs.BANCADA) {
            if (request.motivoBancada() == null || justificativa == null) {
                throw new RegraNegocioException("Bancada exige motivoBancada e justificativa.");
            }
        } else if (request.motivoBancada() != null) {
            throw new RegraNegocioException("motivoBancada só deve ser informado na modalidade BANCADA.");
        }

        Set<Long> desejados = new LinkedHashSet<>(request.tecnicosIds());

        if (desejados.size() != request.tecnicosIds().size()) {
            throw new RegraNegocioException("Não repita IDs na lista de técnicos.");
        }

        validarTecnicos(desejados);

        List<Long> atuais = triagemRepository.tecnicosAtivos(id);
        List<Long> removidos = atuais.stream().filter(tecnico -> !desejados.contains(tecnico)).toList();
        List<Long> adicionados = desejados.stream().filter(tecnico -> !atuais.contains(tecnico)).toList();

        if (!removidos.isEmpty() && motivoRemocao == null) {
            throw new RegraNegocioException("Informe motivoRemocao para retirar um técnico da OS.");
        }

        boolean mudouModalidade = os.getModalidade() != request.modalidade()
                || os.getMotivoBancada() != request.motivoBancada();
        boolean mudouPrioridade = os.getPrioridade() != request.prioridade();

        if (!mudouModalidade && !mudouPrioridade && removidos.isEmpty() && adicionados.isEmpty()) {
            return OrdemServicoResponse.de(os);
        }

        String modalidadeAnterior = descreverModalidade(os.getModalidade(), os.getMotivoBancada());
        Object prioridadeAnterior = os.getPrioridade();

        os.aplicarTriagem(request.modalidade(), request.prioridade(), request.motivoBancada());
        osRepository.saveAndFlush(os);

        if (mudouModalidade) {
            triagemRepository.registrarEvento(
                    id, "MODALIDADE", autor.getId(), modalidadeAnterior,
                    descreverModalidade(os.getModalidade(), os.getMotivoBancada()), justificativa
            );
        }

        if (mudouPrioridade) {
            triagemRepository.registrarEvento(
                    id, "PRIORIDADE", autor.getId(), prioridadeAnterior, os.getPrioridade(), justificativa
            );
        }

        for (Long tecnicoId : removidos) {
            triagemRepository.removerTecnico(id, tecnicoId, motivoRemocao);
            triagemRepository.registrarEvento(
                    id, "TECNICO_SAIU", autor.getId(), tecnicoId, null, motivoRemocao
            );
        }

        for (Long tecnicoId : adicionados) {
            triagemRepository.adicionarTecnico(id, tecnicoId, autor.getId());
            triagemRepository.registrarEvento(
                    id, "TECNICO_ENTROU", autor.getId(), null, tecnicoId, null
            );
        }

        return OrdemServicoResponse.de(os);
    }

    public List<Long> tecnicosAtivos(Long id, String email) {
        Usuario usuario = acesso.usuario(email);
        acesso.exigirEquipe(usuario);
        acesso.consultar(id, usuario);
        return triagemRepository.tecnicosAtivos(id);
    }

    private void validarTecnicos(Set<Long> ids) {
        List<Usuario> usuarios = usuarioRepository.findAllById(ids);

        for (Long id : ids) {
            Usuario tecnico = usuarios.stream()
                    .filter(usuario -> usuario.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Técnico", id));

            if (tecnico.getPerfil() != Perfil.TECNICO || !tecnico.isAtivo()) {
                throw new RegraNegocioException("O usuário " + id + " deve ser um técnico ativo.");
            }
        }
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private String descreverModalidade(ModalidadeOs modalidade, Object motivo) {
        return motivo == null ? modalidade.name() : modalidade.name() + " (" + motivo + ")";
    }
}
