package com.manutencao.sistema.agenda.service;

import com.manutencao.sistema.agenda.domain.Agendamento;
import com.manutencao.sistema.agenda.domain.StatusAgendamento;
import com.manutencao.sistema.agenda.dto.AgendamentoRequest;
import com.manutencao.sistema.agenda.dto.AgendamentoResponse;
import com.manutencao.sistema.agenda.dto.StatusAgendamentoRequest;
import com.manutencao.sistema.agenda.repository.AgendamentoRepository;
import com.manutencao.sistema.common.exception.ConflitoException;
import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.os.service.AcessoOsService;
import com.manutencao.sistema.setor.domain.Setor;
import com.manutencao.sistema.setor.repository.SetorRepository;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AgendamentoService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private final AcessoOsService acesso;
    private final AgendamentoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final SetorRepository setorRepository;
    private final OrdemServicoRepository osRepository;

    public AgendamentoService(AcessoOsService acesso, AgendamentoRepository repository,
                               UsuarioRepository usuarioRepository, SetorRepository setorRepository,
                               OrdemServicoRepository osRepository) {
        this.acesso = acesso;
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.setorRepository = setorRepository;
        this.osRepository = osRepository;
    }

    public List<AgendamentoResponse> listar(Long tecnicoId, StatusAgendamento status,
                                             LocalDate inicio, LocalDate fim, String email) {
        Usuario usuario = acesso.usuario(email);
        acesso.exigirEquipe(usuario);
        if (usuario.getPerfil() == Perfil.TECNICO) {
            if (tecnicoId != null && !tecnicoId.equals(usuario.getId())) {
                throw new RegraNegocioException("Técnico consulta somente a própria agenda.");
            }
            tecnicoId = usuario.getId();
        }
        LocalDate primeiro = inicio == null ? LocalDate.now(FUSO).withDayOfMonth(1) : inicio;
        LocalDate ultimo = fim == null ? primeiro.plusMonths(1).minusDays(1) : fim;
        if (ultimo.isBefore(primeiro) || ultimo.isAfter(primeiro.plusYears(1))) {
            throw new RegraNegocioException("Informe um período válido de até um ano.");
        }
        Instant de = primeiro.atStartOfDay(FUSO).toInstant();
        Instant ate = ultimo.plusDays(1).atStartOfDay(FUSO).toInstant();
        return repository.buscar(tecnicoId, status, de, ate).stream().map(AgendamentoResponse::de).toList();
    }

    public AgendamentoResponse consultar(Long id, String email) {
        Usuario usuario = acesso.usuario(email);
        acesso.exigirEquipe(usuario);
        Agendamento agenda = obter(id);
        if (usuario.getPerfil() == Perfil.TECNICO && !agenda.getTecnico().getId().equals(usuario.getId())) {
            throw new RecursoNaoEncontradoException("Agendamento", id);
        }
        return AgendamentoResponse.de(agenda);
    }

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request, String email) {
        Usuario autor = acesso.usuario(email);
        acesso.exigirGestao(autor);
        Dados dados = validar(request);
        Agendamento agenda = new Agendamento(dados.tecnico(), dados.os(), dados.setor(), request.titulo(),
                request.descricao(), request.local(), request.inicio(), request.fim(), autor);
        return AgendamentoResponse.de(repository.saveAndFlush(agenda));
    }

    @Transactional
    public AgendamentoResponse atualizar(Long id, AgendamentoRequest request, String email) {
        acesso.exigirGestao(acesso.usuario(email));
        Agendamento agenda = obter(id);
        conferirVersao(agenda, request.versao());
        if (agenda.getStatus() != StatusAgendamento.AGENDADO) {
            throw new RegraNegocioException("Só é possível editar um compromisso AGENDADO.");
        }
        Dados dados = validar(request);
        agenda.atualizar(dados.tecnico(), dados.os(), dados.setor(), request.titulo(),
                request.descricao(), request.local(), request.inicio(), request.fim());
        return AgendamentoResponse.de(repository.saveAndFlush(agenda));
    }

    @Transactional
    public AgendamentoResponse alterarStatus(Long id, StatusAgendamentoRequest request, String email) {
        acesso.exigirGestao(acesso.usuario(email));
        Agendamento agenda = obter(id);
        conferirVersao(agenda, request.versao());
        if (agenda.getStatus() != StatusAgendamento.AGENDADO || request.status() == StatusAgendamento.AGENDADO) {
            throw new RegraNegocioException("Um compromisso AGENDADO pode passar para CONCLUIDO ou CANCELADO.");
        }
        agenda.alterarStatus(request.status());
        return AgendamentoResponse.de(repository.saveAndFlush(agenda));
    }

    private Dados validar(AgendamentoRequest request) {
        if (!request.fim().isAfter(request.inicio())) {
            throw new RegraNegocioException("O fim deve ser posterior ao início.");
        }
        Usuario tecnico = usuarioRepository.findById(request.tecnicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Técnico", request.tecnicoId()));
        if (!tecnico.isAtivo() || tecnico.getPerfil() != Perfil.TECNICO) {
            throw new RegraNegocioException("Escolha um técnico ativo.");
        }
        OrdemServico os = request.osId() == null ? null : osRepository.findById(request.osId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("OS", request.osId()));
        if (os != null && (os.encerrada() || os.getStatus() == com.manutencao.sistema.os.domain.StatusOs.FINALIZADO
                || !osRepository.existeVinculoAtivo(os.getId(), tecnico.getId()))) {
            throw new RegraNegocioException("O agendamento exige OS em atendimento e técnico vinculado.");
        }
        Setor setor = request.setorId() == null ? (os == null ? null : os.getSetor())
                : setorRepository.findById(request.setorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor", request.setorId()));
        if (setor != null && !setor.isAtivo()) {
            throw new RegraNegocioException("Escolha um setor ativo.");
        }
        if (os != null && setor != null && !os.getSetor().getId().equals(setor.getId())) {
            throw new RegraNegocioException("O setor do agendamento deve coincidir com o setor da OS.");
        }
        return new Dados(tecnico, os, setor);
    }

    private Agendamento obter(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
    }

    private void conferirVersao(Agendamento agenda, Long versao) {
        if (versao == null) {
            throw new RegraNegocioException("Informe a versão do agendamento para alterá-lo.");
        }
        if (!agenda.getVersao().equals(versao)) {
            throw new ConflitoException("O agendamento foi alterado. Consulte novamente.");
        }
    }

    private record Dados(Usuario tecnico, OrdemServico os, Setor setor) {
    }
}
