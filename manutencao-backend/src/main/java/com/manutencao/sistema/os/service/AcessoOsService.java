package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.ConflitoException;
import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AcessoOsService {

    private final UsuarioRepository usuarioRepository;
    private final OrdemServicoRepository osRepository;

    public AcessoOsService(UsuarioRepository usuarioRepository, OrdemServicoRepository osRepository) {
        this.usuarioRepository = usuarioRepository;
        this.osRepository = osRepository;
    }

    public Usuario usuario(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "O usuário está inativo.");
        }

        return usuario;
    }

    public OrdemServico consultar(Long id, Usuario usuario) {
        OrdemServico os = osRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("OS", id));

        boolean permitido = switch (usuario.getPerfil()) {
            case ADMIN, ATENDENTE -> true;
            case SOLICITANTE -> os.getSolicitante().getId().equals(usuario.getId());
            case TECNICO -> osRepository.existeVinculoAtivo(id, usuario.getId());
        };

        if (!permitido) {
            throw new RecursoNaoEncontradoException("OS", id);
        }

        return os;
    }

    public void exigirGestao(Usuario usuario) {
        if (usuario.getPerfil() != Perfil.ADMIN && usuario.getPerfil() != Perfil.ATENDENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação exclusiva de admin e atendente.");
        }
    }

    public void exigirTecnicoOuAdmin(OrdemServico os, Usuario usuario) {
        if (usuario.getPerfil() == Perfil.ADMIN) {
            return;
        }

        if (usuario.getPerfil() != Perfil.TECNICO
                || !osRepository.existeVinculoAtivo(os.getId(), usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação exclusiva de admin ou técnico vinculado.");
        }
    }

    public void exigirEquipe(Usuario usuario) {
        if (usuario.getPerfil() == Perfil.SOLICITANTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação exclusiva da equipe de TI.");
        }
    }

    public void conferirVersao(OrdemServico os, Long versao) {
        if (!os.getVersao().equals(versao)) {
            throw new ConflitoException("A OS foi alterada. Consulte novamente e use a versão atual.");
        }
    }
}
