package com.manutencao.sistema.whatsapp.repository;

import com.manutencao.sistema.whatsapp.domain.MensagemWhatsAppProcessada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MensagemWhatsAppProcessadaRepository extends JpaRepository<MensagemWhatsAppProcessada, String> {
    @Modifying
    @Query(value = "insert into whatsapp_mensagem_processada (mensagem_id, processada_em) values (:id, now()) on conflict (mensagem_id) do nothing", nativeQuery = true)
    int registrarSeNova(@Param("id") String mensagemId);
}
