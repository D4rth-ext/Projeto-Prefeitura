package com.manutencao.sistema.agenda.repository;

import com.manutencao.sistema.agenda.domain.Agendamento;
import com.manutencao.sistema.agenda.domain.StatusAgendamento;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Override
    @EntityGraph(attributePaths = {"tecnico", "os", "setor", "criadoPor"})
    Optional<Agendamento> findById(Long id);

    @EntityGraph(attributePaths = {"tecnico", "os", "setor", "criadoPor"})
    @Query("""
            select agenda from Agendamento agenda
            where (:tecnicoId is null or agenda.tecnico.id = :tecnicoId)
              and (:status is null or agenda.status = :status)
              and agenda.fim > :inicio and agenda.inicio < :fim
            order by agenda.inicio, agenda.id
            """)
    List<Agendamento> buscar(@Param("tecnicoId") Long tecnicoId,
                             @Param("status") StatusAgendamento status,
                             @Param("inicio") Instant inicio, @Param("fim") Instant fim);
}
