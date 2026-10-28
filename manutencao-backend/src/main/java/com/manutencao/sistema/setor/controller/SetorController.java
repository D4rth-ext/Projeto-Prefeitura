package com.manutencao.sistema.setor.controller;

import com.manutencao.sistema.setor.dto.SetorRequest;
import com.manutencao.sistema.setor.dto.SetorResponse;
import com.manutencao.sistema.setor.service.SetorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/setores")
public class SetorController {

    private final SetorService service;

    public SetorController(SetorService service) {
        this.service = service;
    }

    @GetMapping
    public List<SetorResponse> listar(@RequestParam(required = false) String busca,
                                      @RequestParam(defaultValue = "false") boolean incluirInativos) {
        return service.listar(busca, incluirInativos);
    }

    @GetMapping("/{id}")
    public SetorResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SetorResponse criar(@Valid @RequestBody SetorRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public SetorResponse atualizar(@PathVariable Long id, @Valid @RequestBody SetorRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        service.inativar(id);
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<SetorResponse> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(service.reativar(id));
    }
}
