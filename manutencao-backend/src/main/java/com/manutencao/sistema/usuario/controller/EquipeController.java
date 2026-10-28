package com.manutencao.sistema.usuario.controller;

import com.manutencao.sistema.os.service.AcessoOsService;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.dto.UsuarioResponse;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/equipe")
public class EquipeController {

    private final AcessoOsService acesso;
    private final UsuarioRepository repository;

    public EquipeController(AcessoOsService acesso, UsuarioRepository repository) {
        this.acesso = acesso;
        this.repository = repository;
    }

    @GetMapping
    public List<UsuarioResponse> listar(Principal principal) {
        acesso.exigirGestao(acesso.usuario(principal.getName()));
        return repository.findAll().stream()
                .filter(usuario -> usuario.isAtivo() && usuario.getPerfil() != Perfil.SOLICITANTE)
                .map(UsuarioResponse::de).toList();
    }
}
