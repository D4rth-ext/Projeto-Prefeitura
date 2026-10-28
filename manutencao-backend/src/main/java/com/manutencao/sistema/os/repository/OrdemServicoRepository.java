package com.manutencao.sistema.os.repository;

import com.manutencao.sistema.os.domain.OrdemServico;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    @Override
    @EntityGraph(attributePaths = {"setor", "solicitante", "abertoPor"})
    Optional<OrdemServico> findById(Long id);

    @EntityGraph(attributePaths = {"setor", "solicitante", "abertoPor"})
    List<OrdemServico> findAllByOrderByAbertaEmDesc();

    @EntityGraph(attributePaths = {"setor", "solicitante", "abertoPor"})
    List<OrdemServico> findBySolicitanteIdOrderByAbertaEmDesc(Long solicitanteId);

    @Query(value = """
            select vinculo.os_id
            from os_tecnico vinculo
            where vinculo.tecnico_id = :tecnicoId
              and vinculo.saida_em is null
            """, nativeQuery = true)
    List<Long> buscarIdsPorTecnicoAtivo(@Param("tecnicoId") Long tecnicoId);

    @EntityGraph(attributePaths = {"setor", "solicitante", "abertoPor"})
    @Query("""
            select os from OrdemServico os
            where os.id in :ids
            order by os.abertaEm desc, os.id desc
            """)
    List<OrdemServico> buscarPorIds(@Param("ids") List<Long> ids);

    @Query(value = """
            select exists (
                select 1 from os_tecnico vinculo
                where vinculo.os_id = :osId
                  and vinculo.tecnico_id = :tecnicoId
                  and vinculo.saida_em is null
            )
            """, nativeQuery = true)
    boolean existeVinculoAtivo(
            @Param("osId") Long osId,
            @Param("tecnicoId") Long tecnicoId
    );
}
