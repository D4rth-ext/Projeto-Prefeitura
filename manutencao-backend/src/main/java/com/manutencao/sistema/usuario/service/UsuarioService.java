package com.manutencao.sistema.usuario.service;

import com.manutencao.sistema.common.exception.ConflitoException;
import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.os.domain.StatusOs;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.os.repository.OrdemServicoRepository.TotalPorUsuario;
import com.manutencao.sistema.os.repository.TriagemOsRepository;
import com.manutencao.sistema.os.repository.TriagemOsRepository.OsDoTecnico;
import com.manutencao.sistema.setor.domain.Setor;
import com.manutencao.sistema.setor.repository.SetorRepository;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.SituacaoCadastro;
import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.dto.EdicaoEquipeRequest;
import com.manutencao.sistema.usuario.dto.RedefinicaoSenhaRequest;
import com.manutencao.sistema.usuario.dto.RegistroSolicitanteRequest;
import com.manutencao.sistema.usuario.dto.SenhaTemporariaResponse;
import com.manutencao.sistema.usuario.dto.SolicitanteResumoResponse;
import com.manutencao.sistema.usuario.dto.UsuarioEquipeRequest;
import com.manutencao.sistema.usuario.dto.UsuarioResponse;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import com.manutencao.sistema.os.service.AcessoOsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final TriagemOsRepository triagemOsRepository;
    private final SetorRepository setorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final AcessoOsService acesso;
    private final EntityManager entityManager;

    public UsuarioService(UsuarioRepository usuarioRepository, OrdemServicoRepository ordemServicoRepository,
                          TriagemOsRepository triagemOsRepository, SetorRepository setorRepository,
                          PasswordEncoder passwordEncoder, JdbcTemplate jdbc, AcessoOsService acesso,
                          EntityManager entityManager) {
        this.usuarioRepository = usuarioRepository;
        this.ordemServicoRepository = ordemServicoRepository;
        this.triagemOsRepository = triagemOsRepository;
        this.setorRepository = setorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.acesso = acesso;
        this.entityManager = entityManager;
    }

    public List<UsuarioResponse> listar() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Long> solicitanteIds = usuarios.stream()
                .filter(u -> u.getPerfil() == Perfil.SOLICITANTE)
                .map(Usuario::getId)
                .toList();
        Map<Long, Long> chamadosAbertos = solicitanteIds.isEmpty() ? Map.of()
                : ordemServicoRepository.contarChamadosAbertosPorUsuario(solicitanteIds).stream()
                        .collect(Collectors.toMap(TotalPorUsuario::getUsuarioId, TotalPorUsuario::getTotal));
        return usuarios.stream()
                .map(u -> UsuarioResponse.de(u, chamadosAbertos.getOrDefault(u.getId(), 0L)))
                .toList();
    }

    public List<SolicitanteResumoResponse> listarSolicitantes(Long setorId, String busca, String email) {
        acesso.exigirGestao(acesso.usuario(email));
        return usuarioRepository.buscarAtivosComSetor(Perfil.SOLICITANTE, setorId, busca.trim().toLowerCase(Locale.ROOT))
                .stream().map(SolicitanteResumoResponse::de).toList();
    }

    @Transactional
    public void trocarMinhaSenha(RedefinicaoSenhaRequest request, String email) {
        Usuario autor = acesso.usuario(email);
        Usuario alvo = obterParaAlteracao(autor.getId(), autor);
        if (!alvo.senhaTemporaria() && alvo.getPerfil() != Perfil.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solicite a redefinição da senha a um administrador.");
        }
        if (!alvo.isAtivo() || alvo.senhaTemporariaVencida() || request.senhaAtual() == null
                || !PoliticaSenha.cabeNoBCrypt(request.senhaAtual())
                || !passwordEncoder.matches(request.senhaAtual(), alvo.getSenhaHash())) {
            throw new RegraNegocioException("Informe a senha atual correta e dentro da validade.");
        }
        PoliticaSenha.validar(request.novaSenha());
        if (!request.novaSenha().equals(request.confirmarSenha())) {
            throw new RegraNegocioException("As senhas não coincidem.");
        }
        if (passwordEncoder.matches(request.novaSenha(), alvo.getSenhaHash())) {
            throw new RegraNegocioException("A nova senha deve ser diferente da senha atual.");
        }

        alvo.trocarSenha(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.saveAndFlush(alvo);
        auditarSenha(alvo.getId(), autor.getId());
    }

    @Transactional
    public SenhaTemporariaResponse redefinirSenha(Long id, String email) {
        Usuario autor = exigirAdmin(email);
        Usuario alvo = obterParaAlteracao(id, autor);
        if (!alvo.isAtivo()) {
            throw new RegraNegocioException("A redefinição exige uma conta ativa.");
        }
        if (autor.getId().equals(id)) {
            throw new RegraNegocioException("Para alterar sua própria senha, use a operação de troca de senha.");
        }

        byte[] aleatorio = new byte[24];
        ALEATORIO.nextBytes(aleatorio);
        String senha = "A1-" + Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
        Instant expiraEm = Instant.now().plus(1, ChronoUnit.DAYS);
        alvo.definirSenhaTemporaria(passwordEncoder.encode(senha), expiraEm);
        usuarioRepository.saveAndFlush(alvo);
        auditarSenha(id, autor.getId());
        return new SenhaTemporariaResponse(senha, expiraEm);
    }

    private void auditarSenha(Long alvoId, Long autorId) {
        jdbc.update("insert into usuario_evento_senha (usuario_id, autor_id) values (?, ?)", alvoId, autorId);
    }

    /** Só o Admin chama (rota protegida no SecurityConfig). Nunca cria Solicitante por aqui. */
    @Transactional
    public UsuarioResponse criarEquipe(UsuarioEquipeRequest request) {
        PoliticaSenha.validar(request.senha());
        Perfil perfil = perfilDaEquipe(request.perfil());
        String email = emailNormalizado(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe um usuário com este e-mail.");
        }
        Usuario usuario = new Usuario(request.nome().trim(), email, normalizar(request.telefone()),
                passwordEncoder.encode(request.senha()), perfil);
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    /** Rota pública (auth/registro). O perfil é SEMPRE Solicitante — nunca vem do request. */
    @Transactional
    public UsuarioResponse registrarSolicitante(RegistroSolicitanteRequest request) {
        PoliticaSenha.validar(request.senha());
        if (!request.senha().equals(request.confirmarSenha())) {
            throw new RegraNegocioException("As senhas não coincidem.");
        }
        String email = emailNormalizado(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe um usuário com este e-mail.");
        }
        Setor setor = setorRepository.findById(request.setorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor", request.setorId()));
        if (!setor.isAtivo()) {
            throw new RegraNegocioException("O setor informado está inativo.");
        }
        Usuario usuario = new Usuario(request.nome().trim(), email, normalizar(request.telefone()),
                passwordEncoder.encode(request.senha()), Perfil.SOLICITANTE, setor);
        usuario.aguardarAprovacao();
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    public List<UsuarioResponse> cadastrosPendentes(String email) {
        exigirAdmin(email);
        return usuarioRepository.findBySituacaoCadastroOrderByIdAsc(SituacaoCadastro.PENDENTE)
                .stream().map(UsuarioResponse::de).toList();
    }

    @Transactional
    public UsuarioResponse decidirCadastro(Long id, SituacaoCadastro decisao, String email) {
        Usuario autor = exigirAdmin(email);
        Usuario usuario = obterParaAlteracao(id, autor);
        if (decisao == SituacaoCadastro.PENDENTE || usuario.getPerfil() != Perfil.SOLICITANTE) {
            throw new RegraNegocioException("Informe uma decisão de aprovação ou rejeição de solicitante.");
        }
        if (usuario.getSituacaoCadastro() != SituacaoCadastro.PENDENTE) {
            throw new ConflitoException("Este cadastro já foi decidido.");
        }
        if (decisao == SituacaoCadastro.APROVADO) {
            exigirSetorAtivoParaVinculo(usuario.getSetor().getId());
        }
        usuario.decidirCadastro(decisao);
        usuarioRepository.saveAndFlush(usuario);
        jdbc.update("insert into usuario_evento_cadastro (usuario_id, autor_id, decisao) values (?, ?, ?)",
                id, autor.getId(), decisao.name());
        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public UsuarioResponse editarEquipe(Long id, EdicaoEquipeRequest request, String email) {
        Usuario autor = exigirAdmin(email);
        Usuario usuario = obterParaAlteracao(id, autor);
        if (usuario.getPerfil() == Perfil.SOLICITANTE) {
            throw new RegraNegocioException("Esta edição é exclusiva para contas da equipe.");
        }
        String novoEmail = emailNormalizado(request.email());
        if (!usuario.getEmail().equalsIgnoreCase(novoEmail) && usuarioRepository.existsByEmailIgnoreCase(novoEmail)) {
            throw new ConflitoException("Já existe um usuário com este e-mail.");
        }
        usuario.editarDadosDaEquipe(request.nome().trim(), novoEmail, normalizar(request.telefone()));
        return UsuarioResponse.de(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional
    public void inativar(Long id, String email) {
        Usuario autor = exigirAdmin(email);
        Usuario usuario = obterParaAlteracao(id, autor);
        if (!usuario.isAtivo()) {
            return;
        }

        if (usuario.getPerfil() == Perfil.TECNICO) {
            encerrarVinculosDoTecnico(id, autor.getId());
        }
        usuario.inativar();
        usuarioRepository.saveAndFlush(usuario);
    }

    @Transactional
    public UsuarioResponse alterarSetor(Long id, Long setorId, String email) {
        Usuario autor = exigirAdmin(email);
        Usuario usuario = obterParaAlteracao(id, autor);
        if (usuario.getPerfil() != Perfil.SOLICITANTE || !usuario.isAtivo()) {
            throw new RegraNegocioException("Somente funcionários solicitantes ativos podem mudar de setor.");
        }
        usuario.alterarSetor(exigirSetorAtivoParaVinculo(setorId));
        Usuario atualizado = usuarioRepository.saveAndFlush(usuario);
        long chamadosAbertos = ordemServicoRepository.contarChamadosCriadosPorUsuario(atualizado.getId());
        return UsuarioResponse.de(atualizado, chamadosAbertos);
    }

    private Setor exigirSetorAtivoParaVinculo(Long id) {
        Setor setor = setorRepository.buscarParaVinculo(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor", id));
        if (!setor.isAtivo()) {
            throw new RegraNegocioException("O setor informado está inativo.");
        }
        return setor;
    }

    private Usuario obterParaAlteracao(Long id, Usuario autor) {
        Usuario usuario = usuarioRepository.buscarParaAlteracao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
        if (id.equals(autor.getId())) {
            // Releia o usuário autenticado se ele já estava no contexto da transação.
            entityManager.refresh(usuario);
        }
        return usuario;
    }

    private Usuario exigirAdmin(String email) {
        Usuario autor = acesso.usuario(email);
        if (autor.getPerfil() != Perfil.ADMIN || autor.senhaTemporaria()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação exclusiva de admin.");
        }
        return autor;
    }

    private void encerrarVinculosDoTecnico(Long tecnicoId, Long autorId) {
        List<OsDoTecnico> ordens = triagemOsRepository.bloquearOrdensDoTecnico(tecnicoId);

        // Confira todas as OSs antes de encerrar os vínculos.
        for (OsDoTecnico os : ordens) {
            if ((os.status() == StatusOs.EM_ANALISE || os.status() == StatusOs.AGUARDANDO_PECA)
                    && !triagemOsRepository.temOutroTecnicoAtivo(os.id(), tecnicoId)) {
                throw new RegraNegocioException("Designe outro técnico para a OS " + os.id() + " antes de inativar este usuário.");
            }
        }

        for (OsDoTecnico os : ordens) {
            triagemOsRepository.registrarInativacao(os.id(), tecnicoId, autorId);
        }
    }

    private Perfil perfilDaEquipe(String perfilTexto) {
        Perfil perfil;
        try {
            perfil = Perfil.valueOf(perfilTexto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Perfil inválido. Use ADMIN, ATENDENTE ou TECNICO.");
        }
        if (perfil == Perfil.SOLICITANTE) {
            throw new RegraNegocioException("Solicitante se cadastra pela rota de autocadastro, não por aqui.");
        }
        return perfil;
    }

    private String emailNormalizado(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
