package com.manutencao.sistema.equipamento.repository;

import com.manutencao.sistema.equipamento.domain.Equipamento;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {

    // o patrimônio é gravado em maiúsculas e sem espaços pelo service
    boolean existsByPatrimonio(String patrimonio);

    boolean existsByPatrimonioAndIdNot(String patrimonio, Long id);

    /** EntityGraph carrega setor e tipo junto, numa query só (evita N+1). */
    @EntityGraph(attributePaths = {"setor", "tipo"})
    @Query("""
            select e from Equipamento e
            where (e.ativo = true or :incluirInativos = true)
              and (:setorId is null or e.setor.id = :setorId)
              and (lower(e.patrimonio) like :busca or lower(e.numeroSerie) like :busca
                   or lower(e.marca) like :busca or lower(e.modelo) like :busca)
            order by e.setor.nome, e.patrimonio
            """)
    List<Equipamento> buscar(@Param("setorId") Long setorId,
                             @Param("busca") String busca,
                             @Param("incluirInativos") boolean incluirInativos);
}
