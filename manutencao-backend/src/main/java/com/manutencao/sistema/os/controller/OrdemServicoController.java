package com.manutencao.sistema.os.controller;

import com.manutencao.sistema.os.dto.AberturaOsRequest;
import com.manutencao.sistema.os.dto.EdicaoSolicitacaoRequest;
import com.manutencao.sistema.os.dto.OrdemServicoResponse;
import com.manutencao.sistema.os.dto.PatrimonioOsRequest;
import com.manutencao.sistema.os.dto.TriagemOsRequest;
import com.manutencao.sistema.os.service.OrdemServicoService;
import com.manutencao.sistema.os.service.TriagemOsService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/os")
public class OrdemServicoController {

    private final OrdemServicoService service;
    private final TriagemOsService triagemService;

    public OrdemServicoController(
            OrdemServicoService service,
            TriagemOsService triagemService
    ) {
        this.service = service;
        this.triagemService = triagemService;
    }

    @PostMapping
    public ResponseEntity<OrdemServicoResponse> abrir(
            @Valid @RequestBody AberturaOsRequest request,
            Principal principal
    ) {
        OrdemServicoResponse resposta = service.abrir(
                request,
                principal.getName()
        );

        URI localizacao = URI.create("/api/v1/os/" + resposta.id());

        return ResponseEntity.created(localizacao).body(resposta);
    }

    @GetMapping
    public List<OrdemServicoResponse> listar(Principal principal) {
        return service.listar(principal.getName());
    }

    @PatchMapping("/{id}/solicitacao")
    public OrdemServicoResponse editarSolicitacao(@PathVariable Long id,
            @Valid @RequestBody EdicaoSolicitacaoRequest request, Principal principal) {
        return service.editarSolicitacao(id, request, principal.getName());
    }

    @PatchMapping("/{id}/patrimonio")
    public OrdemServicoResponse atualizarPatrimonio(@PathVariable Long id,
            @Valid @RequestBody PatrimonioOsRequest request, Principal principal) {
        return service.atualizarPatrimonio(id, request, principal.getName());
    }

    @GetMapping("/{id}")
    public OrdemServicoResponse buscarPorId(
            @PathVariable Long id,
            Principal principal
    ) {
        return service.buscarPorId(id, principal.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Principal principal) {
        service.excluir(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/triagem")
    public OrdemServicoResponse triar(
            @PathVariable Long id,
            @Valid @RequestBody TriagemOsRequest request,
            Principal principal
    ) {
        return triagemService.triar(id, request, principal.getName());
    }

    @GetMapping("/{id}/tecnicos")
    public List<Long> tecnicosAtivos(
            @PathVariable Long id,
            Principal principal
    ) {
        return triagemService.tecnicosAtivos(id, principal.getName());
    }
}
