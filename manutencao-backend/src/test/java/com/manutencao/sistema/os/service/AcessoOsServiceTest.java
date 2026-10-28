package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.ConflitoException;
import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.usuario.domain.*;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import com.manutencao.sistema.usuario.controller.SolicitanteController;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcessoOsServiceTest {
    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final OrdemServicoRepository ordens = mock(OrdemServicoRepository.class);
    final AcessoOsService acesso = new AcessoOsService(usuarios, ordens);

    @Test void somenteGestaoConsultaSolicitantes() {
        var controller = new SolicitanteController(acesso, usuarios);
        for (Perfil perfil : new Perfil[]{Perfil.TECNICO, Perfil.SOLICITANTE}) {
            Usuario usuario = new Usuario("Pessoa", "pessoa@teste.com", null, "hash", perfil);
            when(usuarios.findByEmailIgnoreCase("pessoa@teste.com")).thenReturn(Optional.of(usuario));
            assertThrows(ResponseStatusException.class, () -> controller.listar(() -> "pessoa@teste.com"));
        }
        verify(usuarios, never()).listarSolicitantesAtivos();
    }

    @Test void atendentePodeConsultarSolicitantes() {
        Usuario usuario = new Usuario("Atendente", "atendente@teste.com", null, "hash", Perfil.ATENDENTE);
        when(usuarios.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));
        new SolicitanteController(acesso, usuarios).listar(usuario::getEmail);
        verify(usuarios).listarSolicitantesAtivos();
    }

    @Test void tecnicoSemVinculoNaoConsultaOs() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(3L);
        when(usuario.getPerfil()).thenReturn(Perfil.TECNICO);
        when(ordens.findById(10L)).thenReturn(Optional.of(mock(OrdemServico.class)));
        assertThrows(RecursoNaoEncontradoException.class, () -> acesso.consultar(10L, usuario));
    }

    @Test void versaoAntigaImpedeSobrescrita() {
        OrdemServico os = mock(OrdemServico.class);
        when(os.getVersao()).thenReturn(2L);
        assertThrows(ConflitoException.class, () -> acesso.conferirVersao(os, 1L));
    }
}
