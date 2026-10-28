package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.domain.TipoRegistroOs;
import com.manutencao.sistema.os.dto.EventoOsResponse;
import com.manutencao.sistema.os.dto.RegistroOsRequest;
import com.manutencao.sistema.os.dto.RegistroOsResponse;
import com.manutencao.sistema.os.dto.VinculoTecnicoResponse;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.os.repository.RegistroOsRepository;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RegistroOsService {

    private final AcessoOsService acesso;
    private final OrdemServicoRepository osRepository;
    private final RegistroOsRepository registroRepository;

    public RegistroOsService(AcessoOsService acesso, OrdemServicoRepository osRepository,
                             RegistroOsRepository registroRepository) {
        this.acesso = acesso;
        this.osRepository = osRepository;
        this.registroRepository = registroRepository;
    }

    @Transactional
    public RegistroOsResponse criar(Long id, RegistroOsRequest request, String email) {
        Usuario autor = acesso.usuario(email);
        acesso.exigirEquipe(autor);
        OrdemServico os = acesso.consultar(id, autor);
        acesso.conferirVersao(os, request.versao());

        if (request.tipo() == TipoRegistroOs.DIAGNOSTICO || request.tipo() == TipoRegistroOs.ACAO) {
            acesso.exigirTecnicoOuAdmin(os, autor);
        }

        if (os.encerrada()) {
            throw new RegraNegocioException("Não é possível acrescentar registros em uma OS entregue ou cancelada.");
        }

        os.registrarAlteracao();
        osRepository.saveAndFlush(os);
        Long registroId = registroRepository.inserir(id, request.tipo(), autor.getId(), request.texto().trim());

        return registroRepository.listar(id, false).stream()
                .filter(registro -> registro.id().equals(registroId))
                .findFirst().orElseThrow();
    }

    public List<RegistroOsResponse> listar(Long id, String email) {
        Usuario usuario = acesso.usuario(email);
        acesso.consultar(id, usuario);
        return registroRepository.listar(id, usuario.getPerfil() == Perfil.SOLICITANTE);
    }

    public List<EventoOsResponse> eventos(Long id, String email) {
        Usuario usuario = acesso.usuario(email);
        acesso.exigirEquipe(usuario);
        acesso.consultar(id, usuario);
        return registroRepository.eventos(id);
    }

    public List<VinculoTecnicoResponse> tecnicos(Long id, String email) {
        Usuario usuario = acesso.usuario(email);
        acesso.exigirEquipe(usuario);
        acesso.consultar(id, usuario);
        return registroRepository.tecnicos(id);
    }
}
