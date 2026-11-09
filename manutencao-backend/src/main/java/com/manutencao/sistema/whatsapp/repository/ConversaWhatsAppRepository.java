package com.manutencao.sistema.whatsapp.repository;

import com.manutencao.sistema.whatsapp.domain.ConversaWhatsApp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversaWhatsAppRepository extends JpaRepository<ConversaWhatsApp, String> {}
