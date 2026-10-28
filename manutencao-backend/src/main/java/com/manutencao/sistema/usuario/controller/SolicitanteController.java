package com.manutencao.sistema.usuario.controller;

import com.manutencao.sistema.os.service.AcessoOsService;
import com.manutencao.sistema.usuario.dto.SolicitanteResumo;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/solicitantes")
public class SolicitanteController {
    private final AcessoOsService acesso;
    private final UsuarioRepository repository;

    public SolicitanteController(AcessoOsService acesso, UsuarioRepository repository) {
        this.acesso = acesso;
        this.repository = repository;
    }

    @GetMapping
    public List<SolicitanteResumo> listar(Principal principal) {
        acesso.exigirGestao(acesso.usuario(principal.getName()));
        return repository.listarSolicitantesAtivos();
    }
}
