-- Armazena somente o identificador do AnyDesk informado pelo solicitante.
-- Senhas ou códigos de acesso remoto nunca devem ser solicitados ou gravados.
ALTER TABLE ordem_servico ADD COLUMN anydesk_id VARCHAR(50);
