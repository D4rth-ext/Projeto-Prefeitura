CREATE TABLE whatsapp_conversa (
    telefone VARCHAR(20) PRIMARY KEY,
    etapa VARCHAR(20) NOT NULL,
    categoria VARCHAR(30),
    descricao TEXT,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE whatsapp_mensagem_processada (
    mensagem_id VARCHAR(160) PRIMARY KEY,
    processada_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
