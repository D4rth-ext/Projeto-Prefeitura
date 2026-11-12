package com.manutencao.sistema.os.repository;

import com.manutencao.sistema.equipamento.domain.Equipamento;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.domain.StatusOs;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    interface TotalPorEquipamento {
        Long getEquipamentoId();
        long getTotal();
    }

    interface TotalPorUsuario {
        Long getUsuarioId();
        long getTotal();
    }

    @Query("""
            select equipamento.id as equipamentoId, count(distinct os.id) as total
            from Equipamento equipamento, OrdemServico os
            where equipamento.id in :equipamentoIds
              and os.status in :statuses
              and os.excluidaEm is null
              and (
                  os.equipamentoId = equipamento.id
                  or (equipamento.patrimonio is not null
                      and upper(trim(os.patrimonioInformado)) = upper(trim(equipamento.patrimonio)))
                  or (equipamento.numeroSerie is not null
                      and upper(trim(os.patrimonioInformado)) = upper(trim(equipamento.numeroSerie)))
              )
            group by equipamento.id
            """)
    List<TotalPorEquipamento> contarPorEquipamento(
            @Param("equipamentoIds") List<Long> equipamentoIds, @Param("statuses") List<StatusOs> statuses);

    @Query(value = """
            select aberto_por_id as \"usuarioId\", count(*) as total
            from ordem_servico
            where aberto_por_id in (:usuarioIds)
            group by aberto_por_id
            """, nativeQuery = true)
    List<TotalPorUsuario> contarChamadosAbertosPorUsuario(@Param("usuarioIds") List<Long> usuarioIds);

    @Query(value = "select count(*) from ordem_servico where aberto_por_id = :usuarioId", nativeQuery = true)
    long contarChamadosCriadosPorUsuario(@Param("usuarioId") Long usuarioId);

    @Override
    @EntityGraph(attributePaths = {"setor", "solicitante", "abertoPor"})
    @Query("select os from OrdemServico os where os.id = :id and os.excluidaEm is null")
    Optional<OrdemServico> findById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"setor", "solicitante", "abertoPor"})
    List<OrdemServico> findAllByExcluidaEmIsNullOrderByAbertaEmDesc();

    @EntityGraph(attributePaths = {"setor", "solicitante", "abertoPor"})
    List<OrdemServico> findBySolicitanteIdAndExcluidaEmIsNullOrderByAbertaEmDesc(Long solicitanteId);

    @Query(value = """
            select vinculo.os_id
            from os_tecnico vinculo
            join ordem_servico os on os.id = vinculo.os_id
            where os.excluida_em is null
              and vinculo.tecnico_id = :tecnicoId
              and vinculo.saida_em is null
            """, nativeQuery = true)
    List<Long> buscarIdsPorTecnicoAtivo(@Param("tecnicoId") Long tecnicoId);

    @EntityGraph(attributePaths = {"setor", "solicitante", "abertoPor"})
    @Query("""
            select os from OrdemServico os
            where os.excluidaEm is null and os.id in :ids
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

