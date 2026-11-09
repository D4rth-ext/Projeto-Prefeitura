package com.manutencao.sistema.usuario.controller;

import com.manutencao.sistema.usuario.dto.RedefinicaoSenhaRequest;
import com.manutencao.sistema.usuario.dto.SolicitanteResumoResponse;
import com.manutencao.sistema.usuario.dto.UsuarioEquipeRequest;
import com.manutencao.sistema.usuario.dto.UsuarioResponse;
import com.manutencao.sistema.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @GetMapping("/solicitantes")
    public List<SolicitanteResumoResponse> listarSolicitantes(
            @RequestParam(required = false) Long setorId,
            @RequestParam(defaultValue = "") String busca,
            Principal principal) {
        return service.listarSolicitantes(setorId, busca, principal.getName());
    }

    @PatchMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@PathVariable Long id, @Valid @RequestBody RedefinicaoSenhaRequest request,
                               Principal principal) {
        service.redefinirSenha(id, request, principal.getName());
    }

    @PatchMapping("/me/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarMinhaSenha(@Valid @RequestBody RedefinicaoSenhaRequest request, Principal principal) {
        service.trocarMinhaSenha(request, principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioEquipeRequest request) {
        return service.criarEquipe(request);
    }

    public record AlteracaoSetor(@NotNull @Positive Long setorId) {}

    @PatchMapping("/{id}/setor")
    public UsuarioResponse alterarSetor(@PathVariable Long id, @Valid @RequestBody AlteracaoSetor request, Principal principal) {
        return service.alterarSetor(id, request.setorId(), principal.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id, Principal principal) {
        service.inativar(id, principal.getName());
    }
}
