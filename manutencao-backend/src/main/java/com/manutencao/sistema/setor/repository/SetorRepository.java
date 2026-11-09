package com.manutencao.sistema.setor.repository;

import com.manutencao.sistema.setor.domain.Setor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SetorRepository extends JpaRepository<Setor, Long> {

    @Query(value = """
            select exists (
                select 1 from setor
                where id is distinct from cast(:id as bigint)
                  and lower(regexp_replace(nome collate "und-x-icu", '[[:space:]]', '', 'g')) =
                      lower(regexp_replace(cast(:nome as text) collate "und-x-icu", '[[:space:]]', '', 'g'))
            )
            """, nativeQuery = true)
    boolean existeNome(@Param("nome") String nome, @Param("id") Long id);

    @Query("""
            select s from Setor s
            where (s.ativo = true or :incluirInativos = true)
              and (lower(s.nome) like :busca or s.telefone like :busca)
            order by s.nome
            """)
    List<Setor> buscar(@Param("busca") String busca, @Param("incluirInativos") boolean incluirInativos);
}
