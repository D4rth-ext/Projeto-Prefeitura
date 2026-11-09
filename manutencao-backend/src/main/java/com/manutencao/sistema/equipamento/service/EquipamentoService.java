package com.manutencao.sistema.equipamento.service;

import com.manutencao.sistema.common.exception.ConflitoException;
import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.equipamento.domain.Equipamento;
import com.manutencao.sistema.equipamento.domain.TipoEquipamento;
import com.manutencao.sistema.equipamento.dto.EquipamentoRequest;
import com.manutencao.sistema.equipamento.dto.EquipamentoResponse;
import com.manutencao.sistema.equipamento.repository.EquipamentoRepository;
import com.manutencao.sistema.os.domain.StatusOs;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.os.repository.OrdemServicoRepository.TotalPorEquipamento;
import com.manutencao.sistema.setor.domain.Setor;
import com.manutencao.sistema.setor.repository.SetorRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EquipamentoService {

    private final EquipamentoRepository repository;
    private final SetorRepository setorRepository;
    private final TipoEquipamentoService tipoService;
    private final OrdemServicoRepository ordemServicoRepository;

    public EquipamentoService(EquipamentoRepository repository, SetorRepository setorRepository,
                              TipoEquipamentoService tipoService, OrdemServicoRepository ordemServicoRepository) {
        this.repository = repository;
        this.setorRepository = setorRepository;
        this.tipoService = tipoService;
        this.ordemServicoRepository = ordemServicoRepository;
    }

    public List<EquipamentoResponse> listar(Long setorId, String busca, boolean incluirInativos) {
        String limpa = normalizar(busca);
        String padrao = limpa == null ? "%" : "%" + limpa.toLowerCase(Locale.ROOT) + "%";
        List<Equipamento> equipamentos = repository.buscar(setorId, padrao, incluirInativos);
        if (equipamentos.isEmpty()) {
            return List.of();
        }
        List<Long> ids = equipamentos.stream().map(Equipamento::getId).toList();
        Map<Long, Long> totais = ordemServicoRepository.contarPorEquipamento(ids, StatusOs.FINALIZADO)
                .stream().collect(Collectors.toMap(TotalPorEquipamento::getEquipamentoId, TotalPorEquipamento::getTotal));
        return equipamentos.stream()
                .map(equipamento -> EquipamentoResponse.de(equipamento, totais.getOrDefault(equipamento.getId(), 0L)))
                .toList();
    }

    public EquipamentoResponse buscarPorId(Long id) {
        return respostaComTotal(obter(id));
    }

    @Transactional
    public EquipamentoResponse criar(EquipamentoRequest request) {
        Setor setor = obterSetorAtivo(request.setorId());
        TipoEquipamento tipo = tipoService.obterAtivo(request.tipoId());
        Dados dados = normalizarEValidar(request);
        if (dados.patrimonio() != null && repository.existsByPatrimonio(dados.patrimonio())) {
            throw patrimonioDuplicado(dados.patrimonio());
        }
        Equipamento equipamento = new Equipamento(setor, tipo, dados.marca(), dados.modelo(),
                dados.patrimonio(), dados.numeroSerie(), dados.observacoes());
        return EquipamentoResponse.de(repository.save(equipamento), 0);
    }

    @Transactional
    public EquipamentoResponse atualizar(Long id, EquipamentoRequest request) {
        Equipamento equipamento = obter(id);
        Setor setor = equipamento.getSetor().getId().equals(request.setorId())
                ? equipamento.getSetor() : obterSetorAtivo(request.setorId());
        TipoEquipamento tipo = equipamento.getTipo().getId().equals(request.tipoId())
                ? equipamento.getTipo() : tipoService.obterAtivo(request.tipoId());
        Dados dados = normalizarEValidar(request);
        if (dados.patrimonio() != null && repository.existsByPatrimonioAndIdNot(dados.patrimonio(), id)) {
            throw patrimonioDuplicado(dados.patrimonio());
        }
        equipamento.atualizarDados(setor, tipo, dados.marca(), dados.modelo(),
                dados.patrimonio(), dados.numeroSerie(), dados.observacoes());
        return respostaComTotal(repository.save(equipamento));
    }

    @Transactional
    public void inativar(Long id) {
        Equipamento equipamento = obter(id);
        equipamento.inativar();
        repository.save(equipamento);
    }

    @Transactional
    public EquipamentoResponse reativar(Long id) {
        Equipamento equipamento = obter(id);
        if (!equipamento.getSetor().isAtivo()) {
            throw new RegraNegocioException("Não é possível reativar: o setor do equipamento está inativo.");
        }
        equipamento.reativar();
        return respostaComTotal(repository.save(equipamento));
    }

    private EquipamentoResponse respostaComTotal(Equipamento equipamento) {
        long total = ordemServicoRepository.countByEquipamentoIdAndStatus(equipamento.getId(), StatusOs.FINALIZADO);
        return EquipamentoResponse.de(equipamento, total);
    }

    private Setor obterSetorAtivo(Long setorId) {
        Setor setor = setorRepository.findById(setorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor", setorId));
        if (!setor.isAtivo()) {
            throw new RegraNegocioException("O setor informado está inativo.");
        }
        return setor;
    }

    private Equipamento obter(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento", id));
    }

    private Dados normalizarEValidar(EquipamentoRequest request) {
        String patrimonio = maiusculas(request.patrimonio());
        String numeroSerie = normalizar(request.numeroSerie());
        if (patrimonio == null && numeroSerie == null) {
            throw new RegraNegocioException("Informe o patrimônio ou o número de série do equipamento.");
        }
        return new Dados(normalizar(request.marca()), normalizar(request.modelo()), patrimonio, numeroSerie,
                normalizar(request.observacoes()));
    }

    private ConflitoException patrimonioDuplicado(String patrimonio) {
        return new ConflitoException("Já existe um equipamento com o patrimônio " + patrimonio + ".");
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private String maiusculas(String texto) {
        String limpo = normalizar(texto);
        return limpo == null ? null : limpo.toUpperCase(Locale.ROOT);
    }

    private record Dados(String marca, String modelo, String patrimonio, String numeroSerie, String observacoes) {
    }
}
