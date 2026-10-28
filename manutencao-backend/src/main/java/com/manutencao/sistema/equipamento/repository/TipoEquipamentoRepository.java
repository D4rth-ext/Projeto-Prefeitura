package com.manutencao.sistema.equipamento.repository;

import com.manutencao.sistema.equipamento.domain.TipoEquipamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoEquipamentoRepository extends JpaRepository<TipoEquipamento, Long> {

    List<TipoEquipamento> findByAtivoTrueOrderByNome();
}
