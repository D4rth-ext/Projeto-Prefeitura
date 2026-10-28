package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.os.domain.*;
import com.manutencao.sistema.os.dto.*;
import com.manutencao.sistema.os.repository.*;
import com.manutencao.sistema.setor.domain.Setor;
import com.manutencao.sistema.usuario.domain.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FluxoOsServiceTest {
    final AcessoOsService acesso = mock(AcessoOsService.class);
    final OrdemServicoRepository ordens = mock(OrdemServicoRepository.class);
    final TriagemOsRepository auditoria = mock(TriagemOsRepository.class);
    final RegistroOsRepository registros = mock(RegistroOsRepository.class);
    final BackupOsRepository backups = mock(BackupOsRepository.class);
    final FluxoOsService service = new FluxoOsService(acesso, ordens, auditoria, registros, backups);
    OrdemServico os;
    Usuario tecnico;

    @BeforeEach void preparar() {
        Setor setor = new Setor("Administração", null, null, null);
        Usuario solicitante = new Usuario("Servidor", "servidor@teste.com", null, "hash", Perfil.SOLICITANTE, setor);
        tecnico = new Usuario("Técnico", "tecnico@teste.com", null, "hash", Perfil.TECNICO);
        ReflectionTestUtils.setField(setor, "id", 1L);
        ReflectionTestUtils.setField(solicitante, "id", 2L);
        ReflectionTestUtils.setField(tecnico, "id", 3L);
        os = new OrdemServico(setor, solicitante, solicitante, CategoriaOs.LENTO, "Computador lento");
        ReflectionTestUtils.setField(os, "id", 10L);
        ReflectionTestUtils.setField(os, "versao", 0L);
        os.aplicarTriagem(ModalidadeOs.REMOTO, PrioridadeOs.MEDIA, null);
        os.alterarStatus(StatusOs.EM_ANALISE);
        when(acesso.usuario("tecnico@teste.com")).thenReturn(tecnico);
        when(acesso.consultar(10L, tecnico)).thenReturn(os);
    }

    StatusOsRequest finalizar(String resumo) {
        return new StatusOsRequest(StatusOs.FINALIZADO, null, null, resumo, null, null, null, 0L);
    }

    @Test void acaoSemDiagnosticoNaoPermiteFinalizar() {
        when(registros.existeDiagnostico(10L)).thenReturn(false);
        assertThrows(RegraNegocioException.class, () -> service.alterarStatus(10L, finalizar("Formatado"), "tecnico@teste.com"));
        assertEquals(StatusOs.EM_ANALISE, os.getStatus());
        verify(ordens, never()).saveAndFlush(any());
    }

    @Test void diagnosticoNaoSubstituiResumoDaSolucao() {
        assertThrows(RegraNegocioException.class, () -> service.alterarStatus(10L, finalizar("  "), "tecnico@teste.com"));
        assertEquals(StatusOs.EM_ANALISE, os.getStatus());
    }

    @Test void tecnicoFinalizaRemotoComDiagnosticoEResumo() {
        when(registros.existeDiagnostico(10L)).thenReturn(true);
        var resposta = service.alterarStatus(10L, finalizar("Sistema reinstalado e validado"), "tecnico@teste.com");
        assertEquals(StatusOs.FINALIZADO, resposta.status());
        assertEquals("Sistema reinstalado e validado", resposta.resumoSolucao());
        verify(acesso).exigirTecnicoOuAdmin(os, tecnico);
        verify(auditoria).registrarEvento(10L, "STATUS", 3L, StatusOs.EM_ANALISE, StatusOs.FINALIZADO, null);
    }

    BackupOsResponse protocolo(SituacaoBackup situacao) {
        return new BackupOsResponse(10L, true, null, null, null, "Documentos", "HD TI", situacao, null, null, null, false, 0L);
    }

    @Test void bancadaComBackupPendenteBloqueiaFinalizacao() {
        os.aplicarTriagem(ModalidadeOs.BANCADA, PrioridadeOs.MEDIA, MotivoBancada.OUTRO);
        when(registros.existeDiagnostico(10L)).thenReturn(true);
        when(backups.buscar(10L)).thenReturn(Optional.of(protocolo(SituacaoBackup.PENDENTE)));
        assertThrows(RegraNegocioException.class, () -> service.alterarStatus(10L, finalizar("Reparado"), "tecnico@teste.com"));
        assertEquals(StatusOs.EM_ANALISE, os.getStatus());
    }

    @Test void bancadaComBackupDevolvidoPermiteFinalizacao() {
        os.aplicarTriagem(ModalidadeOs.BANCADA, PrioridadeOs.MEDIA, MotivoBancada.OUTRO);
        when(registros.existeDiagnostico(10L)).thenReturn(true);
        when(backups.buscar(10L)).thenReturn(Optional.of(protocolo(SituacaoBackup.RESTAURADO_DEVOLVIDO)));
        assertEquals(StatusOs.FINALIZADO, service.alterarStatus(10L, finalizar("Reparado"), "tecnico@teste.com").status());
    }
}
