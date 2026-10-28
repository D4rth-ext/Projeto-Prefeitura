package com.manutencao.sistema.usuario.service;

import com.manutencao.sistema.common.exception.ConflitoException;
import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.setor.domain.Setor;
import com.manutencao.sistema.setor.repository.SetorRepository;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.dto.RegistroSolicitanteRequest;
import com.manutencao.sistema.usuario.dto.UsuarioEquipeRequest;
import com.manutencao.sistema.usuario.dto.UsuarioResponse;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import java.util.List;
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
    private final SetorRepository setorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final AcessoOsService acesso;

    public UsuarioService(UsuarioRepository usuarioRepository, SetorRepository setorRepository,
                          PasswordEncoder passwordEncoder, JdbcTemplate jdbc, AcessoOsService acesso) {
        this.usuarioRepository = usuarioRepository;
        this.setorRepository = setorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.acesso = acesso;
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::de).toList();
    }

    /** Só o Admin chama (rota protegida no SecurityConfig). Nunca cria Solicitante por aqui. */
    @Transactional
    public UsuarioResponse criarEquipe(UsuarioEquipeRequest request) {
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
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
        if (!usuario.isAtivo()) {
            return;
        }

        if (usuario.getPerfil() == Perfil.TECNICO) {
            encerrarVinculosDoTecnico(id, autor.getId());
        }
        usuario.inativar();
        usuarioRepository.saveAndFlush(usuario);
    }

    private void encerrarVinculosDoTecnico(Long tecnicoId, Long autorId) {
        // A mesma ordem de bloqueio impede que duas inativações concorrentes
        // deixem uma OS em análise sem nenhum técnico ativo.
        List<Long> ids = jdbc.queryForList("""
                select os.id from ordem_servico os
                where exists (select 1 from os_tecnico vinculo
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
