package com.manutencao.sistema.whatsapp.service;

import com.manutencao.sistema.os.domain.CategoriaOs;
import com.manutencao.sistema.os.dto.AberturaOsRequest;
import com.manutencao.sistema.os.dto.OrdemServicoResponse;
import com.manutencao.sistema.os.service.OrdemServicoService;
import com.manutencao.sistema.whatsapp.domain.ConversaWhatsApp;
import com.manutencao.sistema.whatsapp.repository.ConversaWhatsAppRepository;
import com.manutencao.sistema.whatsapp.repository.MensagemWhatsAppProcessadaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppBotService {
    private final ConversaWhatsAppRepository conversas;
    private final MensagemWhatsAppProcessadaRepository processadas;
    private final OrdemServicoService osService;
    private final WhatsAppPropertiesBridge bridge;

    public WhatsAppBotService(ConversaWhatsAppRepository conversas,
            MensagemWhatsAppProcessadaRepository processadas,
            OrdemServicoService osService, WhatsAppPropertiesBridge bridge) {
        this.conversas = conversas;
        this.processadas = processadas;
        this.osService = osService;
        this.bridge = bridge;
    }

    @Transactional
    public void receber(String telefone, String mensagemId, String texto) {
        if (processadas.registrarSeNova(mensagemId) == 0) return;
        String entrada = texto == null ? "" : texto.trim();
        String baixa = entrada.toLowerCase(java.util.Locale.ROOT);
        ConversaWhatsApp conversa = conversas.findById(telefone)
                .orElseGet(() -> new ConversaWhatsApp(telefone, "INICIO"));
        if (baixa.equals("cancelar") || baixa.equals("sair")) {
            conversas.deleteById(telefone);
            bridge.enviar(telefone, "Tudo bem, cancelei. Quando quiser abrir um chamado, mande qualquer mensagem.");
            return;
        }
        switch (conversa.getEtapa()) {
            case "INICIO" -> {
                if (baixa.equals("1") || baixa.contains("chamado") || baixa.contains("começar")) {
                    conversa.atualizar("CATEGORIA", null, null);
                    conversas.save(conversa);
                    bridge.enviar(telefone, "Qual é o tipo do problema?\n1. Computador não liga\n2. Computador lento\n3. Sem rede\n4. Impressora\n5. Sistema\n6. E-mail\n7. Outro\n\nResponda com o número. Digite CANCELAR para sair.");
                } else {
                    conversas.save(conversa);
                    bridge.enviar(telefone, "Olá! Posso ajudar a abrir um chamado de manutenção.\nResponda 1 para começar.");
                }
            }
            case "CATEGORIA" -> {
                CategoriaOs categoria = categoria(baixa);
                if (categoria == null) {
                    bridge.enviar(telefone, "Não reconheci essa opção. Responda com um número de 1 a 7.");
                } else {
                    conversa.atualizar("DESCRICAO", categoria.name(), null);
                    conversas.save(conversa);
                    bridge.enviar(telefone, "Descreva o que está acontecendo. (até 2000 caracteres)");
                }
            }
            case "DESCRICAO" -> {
                if (entrada.length() < 5 || entrada.length() > 2000) {
                    bridge.enviar(telefone, "A descrição deve ter entre 5 e 2000 caracteres. Pode detalhar um pouco mais?");
                } else {
                    conversa.atualizar("CONFIRMAR", conversa.getCategoria(), entrada);
                    conversas.save(conversa);
                    bridge.enviar(telefone, "Confirma a abertura deste chamado?\nCategoria: "
                            + categoriaNome(conversa.getCategoria()) + "\nDescrição: " + entrada
                            + "\n\nResponda SIM para abrir ou NÃO para recomeçar.");
                }
            }
            case "CONFIRMAR" -> {
                if (baixa.equals("sim") || baixa.equals("s")) {
                    try {
                        OrdemServicoResponse os = osService.abrirPeloWhatsApp(
                                new AberturaOsRequest(CategoriaOs.valueOf(conversa.getCategoria()), conversa.getDescricao(), null), telefone);
                        conversas.deleteById(telefone);
                        bridge.enviar(telefone, "Chamado aberto com sucesso! Número: " + os.id() + ". A equipe dará continuidade pelo sistema.");
                    } catch (RuntimeException erro) {
                        conversas.deleteById(telefone);
                        bridge.enviar(telefone, erro.getMessage() == null
                                ? "Não consegui abrir o chamado agora. Tente novamente mais tarde ou fale com o atendimento."
                                : erro.getMessage());
                    }
                } else if (baixa.equals("não") || baixa.equals("nao") || baixa.equals("n")) {
                    conversa.atualizar("INICIO", null, null);
                    conversas.save(conversa);
                    bridge.enviar(telefone, "Certo. Responda 1 para começar um novo chamado ou CANCELAR para sair.");
                } else {
                    bridge.enviar(telefone, "Responda SIM para confirmar ou NÃO para recomeçar.");
                }
            }
            default -> {
                conversa.atualizar("INICIO", null, null);
                conversas.save(conversa);
                bridge.enviar(telefone, "Vamos começar de novo. Responda 1 para abrir um chamado.");
            }
        }
    }

    private CategoriaOs categoria(String entrada) {
        return switch (entrada) {
            case "1" -> CategoriaOs.NAO_LIGA;
            case "2" -> CategoriaOs.LENTO;
            case "3" -> CategoriaOs.SEM_REDE;
            case "4" -> CategoriaOs.IMPRESSORA;
            case "5" -> CategoriaOs.SISTEMA;
            case "6" -> CategoriaOs.EMAIL;
            case "7" -> CategoriaOs.OUTRO;
            default -> null;
        };
    }

    private String categoriaNome(String categoria) {
        return switch (categoria) {
            case "NAO_LIGA" -> "Computador não liga";
            case "LENTO" -> "Computador lento";
            case "SEM_REDE" -> "Sem rede";
            case "IMPRESSORA" -> "Impressora";
            case "SISTEMA" -> "Sistema";
            case "EMAIL" -> "E-mail";
            default -> "Outro";
        };
    }
}
