package com.manutencao.sistema.agenda.controller;

import com.manutencao.sistema.agenda.domain.StatusAgendamento;
import com.manutencao.sistema.agenda.dto.AgendamentoRequest;
import com.manutencao.sistema.agenda.dto.AgendamentoResponse;
import com.manutencao.sistema.agenda.dto.StatusAgendamentoRequest;
import com.manutencao.sistema.agenda.service.AgendamentoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agendamentos")
public class AgendamentoController {

    private final AgendamentoService service;

    public AgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<AgendamentoResponse> listar(
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Principal principal) {
        return service.listar(tecnicoId, status, inicio, fim, principal.getName());
    }

    @GetMapping("/{id}")
    public AgendamentoResponse consultar(@PathVariable Long id, Principal principal) {
        return service.consultar(id, principal.getName());
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponse> criar(@Valid @RequestBody AgendamentoRequest request,
                                                     Principal principal) {
        AgendamentoResponse resposta = service.criar(request, principal.getName());
        return ResponseEntity.created(URI.create("/api/v1/agendamentos/" + resposta.id())).body(resposta);
    }

    @PutMapping("/{id}")
    public AgendamentoResponse atualizar(@PathVariable Long id,
                                         @Valid @RequestBody AgendamentoRequest request,
                                         Principal principal) {
        return service.atualizar(id, request, principal.getName());
    }

    @PatchMapping("/{id}/status")
    public AgendamentoResponse alterarStatus(@PathVariable Long id,
                                             @Valid @RequestBody StatusAgendamentoRequest request,
                                             Principal principal) {
        return service.alterarStatus(id, request, principal.getName());
    }
}
