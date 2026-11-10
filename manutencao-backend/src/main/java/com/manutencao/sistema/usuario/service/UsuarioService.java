package com.manutencao.sistema.usuario.service;

import com.manutencao.sistema.common.exception.ConflitoException;
import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.os.repository.OrdemServicoRepository.TotalPorUsuario;
import com.manutencao.sistema.setor.domain.Setor;
import com.manutencao.sistema.setor.repository.SetorRepository;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.dto.RedefinicaoSenhaRequest;
import com.manutencao.sistema.usuario.dto.RegistroSolicitanteRequest;
import com.manutencao.sistema.usuario.dto.SolicitanteResumoResponse;
import com.manutencao.sistema.usuario.dto.UsuarioEquipeRequest;
import com.manutencao.sistema.usuario.dto.UsuarioResponse;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
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

    private final UsuarioRepository usuarioRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final SetorRepository setorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final AcessoOsService acesso;
    private final EntityManager entityManager;

    public UsuarioService(UsuarioRepository usuarioRepository, OrdemServicoRepository ordemServicoRepository,
                          SetorRepository setorRepository,
                          PasswordEncoder passwordEncoder, JdbcTemplate jdbc, AcessoOsService acesso,
                          EntityManager entityManager) {
        this.usuarioRepository = usuarioRepository;
        this.ordemServicoRepository = ordemServicoRepository;
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
        redefinirSenha(autor.getId(), request, autor);
    }

    @Transactional
    public void redefinirSenha(Long id, RedefinicaoSenhaRequest request, String email) {
        redefinirSenha(id, request, acesso.usuario(email));
    }

    private void redefinirSenha(Long id, RedefinicaoSenhaRequest request, Usuario autor) {
        boolean propria = autor.getId().equals(id);
        if (autor.getPerfil() == Perfil.TECNICO || autor.getPerfil() == Perfil.SOLICITANTE && !propria) {
            throw redefinicaoProibida();
        }

        Usuario alvo = obterParaAlteracao(id);
        if (autor.getPerfil() == Perfil.ATENDENTE && alvo.getPerfil() != Perfil.SOLICITANTE) {
            throw redefinicaoProibida();
        }
        if (propria) {
            // O usuário autenticado já pode estar no cache da transação. Releia sob o bloqueio.
            entityManager.refresh(alvo);
            if (!alvo.isAtivo() || request.senhaAtual() == null || request.senhaAtual().isBlank()
                    || !passwordEncoder.matches(request.senhaAtual(), alvo.getSenhaHash())) {
                throw new RegraNegocioException("Informe a senha atual correta para alterar sua senha.");
            }
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
        jdbc.update("insert into usuario_evento_senha (usuario_id, autor_id) values (?, ?)", id, autor.getId());
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
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    @Transactional
    public void inativar(Long id, String email) {
        Usuario autor = acesso.usuario(email);
        if (autor.getPerfil() != Perfil.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação exclusiva de admin.");
        }
        Usuario usuario = obterParaAlteracao(id);
        if (usuario.getId().equals(autor.getId())) {
            entityManager.refresh(usuario);
        }
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
        if (acesso.usuario(email).getPerfil() != Perfil.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação exclusiva de admin.");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
        if (usuario.getPerfil() != Perfil.SOLICITANTE || !usuario.isAtivo()) {
            throw new RegraNegocioException("Somente funcionários solicitantes ativos podem mudar de setor.");
        }
        Setor setor = setorRepository.findById(setorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor", setorId));
        if (!setor.isAtivo()) {
            throw new RegraNegocioException("O setor informado está inativo.");
        }
        usuario.alterarSetor(setor);
        Usuario atualizado = usuarioRepository.saveAndFlush(usuario);
        long chamadosAbertos = ordemServicoRepository.contarChamadosCriadosPorUsuario(atualizado.getId());
        return UsuarioResponse.de(atualizado, chamadosAbertos);
    }

    private Usuario obterParaAlteracao(Long id) {
        return usuarioRepository.buscarParaAlteracao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    private ResponseStatusException redefinicaoProibida() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para redefinir esta senha.");
    }

    private void encerrarVinculosDoTecnico(Long tecnicoId, Long autorId) {
        // A mesma ordem de bloqueio impede que duas inativações concorrentes
        // deixem uma OS em análise sem nenhum técnico ativo.
        List<Long> ids = jdbc.queryForList("""
                select os.id from ordem_servico os
                where os.excluida_em is null
                  and exists (select 1 from os_tecnico vinculo
                    where vinculo.os_id = os.id and vinculo.tecnico_id = ?
                      and vinculo.saida_em is null)
                order by os.id for update
                """, Long.class, tecnicoId);

        for (Long osId : ids) {
            String status = jdbc.queryForObject("select status from ordem_servico where id = ?", String.class, osId);
            Long outros = jdbc.queryForObject("""
                    select count(*) from os_tecnico vinculo
                    join usuario tecnico on tecnico.id = vinculo.tecnico_id
                    where vinculo.os_id = ? and vinculo.saida_em is null
                      and tecnico.ativo and tecnico.perfil = 'TECNICO'
                      and tecnico.id <> ?
                    """, Long.class, osId, tecnicoId);
            if (("EM_ANALISE".equals(status) || "AGUARDANDO_PECA".equals(status))
                    && (outros == null || outros == 0)) {
                throw new RegraNegocioException("Designe outro técnico para a OS " + osId + " antes de inativar este usuário.");
            }
        }

        for (Long osId : ids) {
            jdbc.update("""
                    update os_tecnico set saida_em = current_timestamp, motivo_saida = 'Usuário inativado.'
                    where os_id = ? and tecnico_id = ? and saida_em is null
                    """, osId, tecnicoId);
            jdbc.update("""
                    update ordem_servico set versao = versao + 1, atualizado_em = current_timestamp
                    where id = ?
                    """, osId);
            jdbc.update("""
                    insert into os_evento (os_id, tipo, autor_id, valor_anterior, valor_novo, motivo)
                    values (?, 'TECNICO_SAIU', ?, ?, null, 'Usuário inativado.')
                    """, osId, autorId, tecnicoId.toString());
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
        return email.trim().toLowerCase();
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
