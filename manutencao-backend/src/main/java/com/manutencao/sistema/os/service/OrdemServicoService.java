package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.dto.AberturaOsRequest;
import com.manutencao.sistema.os.dto.OrdemServicoResponse;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.setor.domain.Setor;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class OrdemServicoService {

    private final OrdemServicoRepository osRepository;
    private final UsuarioRepository usuarioRepository;

    public OrdemServicoService(
            OrdemServicoRepository osRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.osRepository = osRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public OrdemServicoResponse abrir(AberturaOsRequest request, String email) {
        Usuario autor = usuarioAutenticado(email);

        if (autor.getPerfil() == Perfil.TECNICO) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Técnico não pode abrir uma ordem de serviço."
            );
        }

        Usuario solicitante = resolverSolicitante(request.solicitanteId(), autor);
        Setor setor = solicitante.getSetor();

        if (setor == null) {
            throw new RegraNegocioException("O solicitante não possui setor vinculado.");
        }

        if (!setor.isAtivo()) {
            throw new RegraNegocioException("Não é possível abrir OS para um setor inativo.");
        }

        OrdemServico os = new OrdemServico(
                setor,
                solicitante,
                autor,
                request.categoria(),
                request.descricaoProblema()
        );

        os.preencherAbertura(
                request.patrimonioInformado(), request.computadorLiga(), request.temRede(),
                request.consegueAnyDesk(), request.contato(), request.melhorHorario()
        );

        return OrdemServicoResponse.de(osRepository.save(os));
    }

    public List<OrdemServicoResponse> listar(String email) {
        Usuario usuario = usuarioAutenticado(email);

        List<OrdemServico> ordens = switch (usuario.getPerfil()) {
            case ADMIN, ATENDENTE -> osRepository.findAllByOrderByAbertaEmDesc();
            case SOLICITANTE -> osRepository
                    .findBySolicitanteIdOrderByAbertaEmDesc(usuario.getId());
            case TECNICO -> listarDoTecnico(usuario.getId());
        };

        return ordens.stream().map(OrdemServicoResponse::de).toList();
    }

    public OrdemServicoResponse buscarPorId(Long id, String email) {
        Usuario usuario = usuarioAutenticado(email);

        OrdemServico os = osRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("OS", id));

        boolean podeConsultar = switch (usuario.getPerfil()) {
            case ADMIN, ATENDENTE -> true;
            case SOLICITANTE -> os.getSolicitante().getId().equals(usuario.getId());
            case TECNICO -> osRepository.existeVinculoAtivo(id, usuario.getId());
        };

        if (!podeConsultar) {
            throw new RecursoNaoEncontradoException("OS", id);
        }

        return OrdemServicoResponse.de(os);
    }

    private List<OrdemServico> listarDoTecnico(Long tecnicoId) {
        List<Long> ids = osRepository.buscarIdsPorTecnicoAtivo(tecnicoId);

        if (ids.isEmpty()) {
            return List.of();
        }

        return osRepository.buscarPorIds(ids);
    }

    private Usuario resolverSolicitante(Long solicitanteId, Usuario autor) {
        if (autor.getPerfil() == Perfil.SOLICITANTE) {
            if (solicitanteId != null && !solicitanteId.equals(autor.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Você só pode abrir OS em seu próprio nome."
                );
            }

            return autor;
        }

        if (solicitanteId == null) {
            throw new RegraNegocioException(
                    "Admin e atendente devem informar o solicitanteId."
            );
        }

        Usuario solicitante = usuarioRepository.findById(solicitanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Solicitante", solicitanteId
                ));

        if (solicitante.getPerfil() != Perfil.SOLICITANTE) {
            throw new RegraNegocioException(
                    "O usuário informado deve possuir o perfil SOLICITANTE."
            );
        }

        if (!solicitante.isAtivo()) {
            throw new RegraNegocioException("O solicitante informado está inativo.");
        }

        return solicitante;
    }

    private Usuario usuarioAutenticado(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário autenticado não encontrado."
                ));

        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "O usuário está inativo."
            );
        }

        return usuario;
    }
}
