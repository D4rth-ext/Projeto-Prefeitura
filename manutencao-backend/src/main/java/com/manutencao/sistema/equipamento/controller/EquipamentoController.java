package com.manutencao.sistema.equipamento.controller;

import com.manutencao.sistema.equipamento.dto.EquipamentoRequest;
import com.manutencao.sistema.equipamento.dto.EquipamentoResponse;
import com.manutencao.sistema.equipamento.service.EquipamentoService;
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
@RequestMapping("/api/v1/equipamentos")
public class EquipamentoController {

    private final EquipamentoService service;

    public EquipamentoController(EquipamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<EquipamentoResponse> listar(@RequestParam(required = false) Long setorId,
                                            @RequestParam(required = false) String busca,
                                            @RequestParam(defaultValue = "false") boolean incluirInativos) {
        return service.listar(setorId, busca, incluirInativos);
    }

    @GetMapping("/{id}")
    public EquipamentoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipamentoResponse criar(@Valid @RequestBody EquipamentoRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public EquipamentoResponse atualizar(@PathVariable Long id, @Valid @RequestBody EquipamentoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        service.inativar(id);
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<EquipamentoResponse> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(service.reativar(id));
    }
}
