package com.manutencao.sistema.auth.controller;

import com.manutencao.sistema.auth.dto.LoginRequest;
import com.manutencao.sistema.auth.dto.LoginResponse;
import com.manutencao.sistema.auth.dto.SetorPublicoResponse;
import com.manutencao.sistema.auth.service.JwtService;
import com.manutencao.sistema.setor.repository.SetorRepository;
import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.dto.RegistroSolicitanteRequest;
import com.manutencao.sistema.usuario.dto.UsuarioResponse;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import com.manutencao.sistema.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Tudo aqui é público — liberado em SecurityConfig ("/api/v1/auth/**"). */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos.";

    private final UsuarioRepository usuarioRepository;
    private final SetorRepository setorRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UsuarioRepository usuarioRepository, SetorRepository setorRepository,
                          UsuarioService usuarioService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.setorRepository = setorRepository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(this::credenciaisInvalidas);

        if (!usuario.isAtivo() || !passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw credenciaisInvalidas();
        }

        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getPerfil().name());
        return new LoginResponse(token, usuario.getNome(), usuario.getPerfil().name());
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registro(@Valid @RequestBody RegistroSolicitanteRequest request) {
        return usuarioService.registrarSolicitante(request);
    }

    /** Lista enxuta (só id e nome) para o select do formulário de autocadastro. */
    @GetMapping("/setores-publicos")
    public List<SetorPublicoResponse> setoresPublicos() {
        return setorRepository.buscar("%", false).stream().map(SetorPublicoResponse::de).toList();
    }

    private ResponseStatusException credenciaisInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS);
    }
}
