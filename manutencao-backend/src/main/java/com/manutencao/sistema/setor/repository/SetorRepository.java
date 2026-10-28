package com.manutencao.sistema.setor.repository;

import com.manutencao.sistema.setor.domain.Setor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SetorRepository extends JpaRepository<Setor, Long> {

    boolean existsByNomeIgnoreCaseAndAtivoTrue(String nome);

    boolean existsByNomeIgnoreCaseAndAtivoTrueAndIdNot(String nome, Long id);

    @Query("""
            select s from Setor s
            where (s.ativo = true or :incluirInativos = true)
              and (lower(s.nome) like :busca or s.telefone like :busca)
            order by s.nome
            """)
    List<Setor> buscar(@Param("busca") String busca, @Param("incluirInativos") boolean incluirInativos);
}
