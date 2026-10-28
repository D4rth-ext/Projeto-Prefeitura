package com.manutencao.sistema.setor.service;

import com.manutencao.sistema.common.exception.ConflitoException;
import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.setor.domain.Setor;
import com.manutencao.sistema.setor.dto.SetorRequest;
import com.manutencao.sistema.setor.dto.SetorResponse;
import com.manutencao.sistema.setor.repository.SetorRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SetorService {

    private final SetorRepository repository;

    public SetorService(SetorRepository repository) {
        this.repository = repository;
    }

    public List<SetorResponse> listar(String busca, boolean incluirInativos) {
        String limpa = normalizar(busca);
        String padrao = limpa == null ? "%" : "%" + limpa.toLowerCase(Locale.ROOT) + "%";
        return repository.buscar(padrao, incluirInativos).stream().map(SetorResponse::de).toList();
    }

    public SetorResponse buscarPorId(Long id) {
        return SetorResponse.de(obter(id));
    }

    @Transactional
    public SetorResponse criar(SetorRequest request) {
        String nome = nomeObrigatorio(request);
        if (repository.existsByNomeIgnoreCaseAndAtivoTrue(nome)) {
            throw new ConflitoException("Já existe um setor ativo com o nome \"" + nome + "\".");
        }
        Setor setor = new Setor(nome, normalizar(request.telefone()), normalizar(request.email()),
                normalizar(request.endereco()));
        return SetorResponse.de(repository.save(setor));
    }

    @Transactional
    public SetorResponse atualizar(Long id, SetorRequest request) {
        Setor setor = obter(id);
        String nome = nomeObrigatorio(request);
        if (setor.isAtivo() && repository.existsByNomeIgnoreCaseAndAtivoTrueAndIdNot(nome, id)) {
            throw new ConflitoException("Já existe um setor ativo com o nome \"" + nome + "\".");
        }
        setor.atualizarDados(nome, normalizar(request.telefone()), normalizar(request.email()),
                normalizar(request.endereco()));
        return SetorResponse.de(repository.save(setor));
    }

    @Transactional
    public void inativar(Long id) {
        Setor setor = obter(id);
        setor.inativar();
        repository.save(setor);
    }

    @Transactional
    public SetorResponse reativar(Long id) {
        Setor setor = obter(id);
        if (!setor.isAtivo() && repository.existsByNomeIgnoreCaseAndAtivoTrue(setor.getNome())) {
            throw new ConflitoException(
                    "Não é possível reativar: já existe outro setor ativo com o nome \"" + setor.getNome() + "\".");
        }
        setor.reativar();
        return SetorResponse.de(repository.save(setor));
    }

    private Setor obter(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Setor", id));
    }

    private String nomeObrigatorio(SetorRequest request) {
        String nome = normalizar(request.nome());
        if (nome == null) {
            throw new RegraNegocioException("O nome do setor é obrigatório.");
        }
        return nome;
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
