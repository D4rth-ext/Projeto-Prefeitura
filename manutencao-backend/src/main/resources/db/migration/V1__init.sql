-- V1__init.sql · Sprint 2 (Setor, Usuário, Tipo de equipamento, Equipamento) · PostgreSQL 14+
-- Destino: src/main/resources/db/migration/
-- atualizado_em é preenchido pelo JPA Auditing (@LastModifiedDate), não por trigger.
-- Patrimônio/e-mail devem ser normalizados (trim) na aplicação; os índices abaixo
-- são a segunda linha de defesa e ignoram diferença de maiúsculas e espaços.

CREATE TABLE setor (
                       id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       nome          VARCHAR(120) NOT NULL CHECK (btrim(nome) <> ''),
                       telefone      VARCHAR(20),
                       email         VARCHAR(160),
                       endereco      VARCHAR(255),
                       ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
                       criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_setor_nome_ativo ON setor (lower(btrim(nome))) WHERE ativo;
CREATE INDEX idx_setor_telefone ON setor (telefone);

CREATE TABLE usuario (
                         id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         nome          VARCHAR(120) NOT NULL CHECK (btrim(nome) <> ''),
                         email         VARCHAR(160) NOT NULL,
                         telefone      VARCHAR(20),
                         senha_hash    VARCHAR(100) NOT NULL,            -- BCrypt
                         perfil        VARCHAR(20)  NOT NULL
                             CHECK (perfil IN ('ADMIN','TECNICO','ATENDENTE','SOLICITANTE')),
                         setor_id      BIGINT REFERENCES setor(id),      -- somente SOLICITANTE
                         ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
                         criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                         atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- solicitante sempre tem setor; equipe interna nunca tem
                         CONSTRAINT ck_usuario_setor CHECK ((perfil = 'SOLICITANTE') = (setor_id IS NOT NULL))
);
CREATE UNIQUE INDEX uq_usuario_email ON usuario (lower(btrim(email)));
CREATE INDEX idx_usuario_perfil ON usuario (perfil) WHERE ativo;

CREATE TABLE tipo_equipamento (
                                  id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                  nome  VARCHAR(60) NOT NULL CHECK (btrim(nome) <> ''),
                                  ativo BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX uq_tipo_equipamento_nome ON tipo_equipamento (lower(btrim(nome)));

INSERT INTO tipo_equipamento (nome) VALUES
                                        ('Computador'), ('Notebook'), ('Monitor'), ('Impressora'),
                                        ('Nobreak'), ('Switch / Roteador'), ('Telefone IP'), ('Outro');

-- Identificação permanente do bem. Dados de cada ENTRADA (acessórios, backup,
-- defeito) ficam na ordem_servico (V2).
CREATE TABLE equipamento (
                             id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             setor_id      BIGINT       NOT NULL REFERENCES setor(id),            -- RF02.3
                             tipo_id       BIGINT       NOT NULL REFERENCES tipo_equipamento(id),
                             marca         VARCHAR(60),
                             modelo        VARCHAR(80),
                             patrimonio    VARCHAR(40),
                             numero_serie  VARCHAR(80),
                             observacoes   TEXT,
                             ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
                             criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                             atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
                             CONSTRAINT ck_equip_patrimonio_nao_vazio CHECK (patrimonio IS NULL OR btrim(patrimonio) <> ''),
                             CONSTRAINT ck_equip_serie_nao_vazia      CHECK (numero_serie IS NULL OR btrim(numero_serie) <> ''),
                             CONSTRAINT ck_equip_identificacao        CHECK (patrimonio IS NOT NULL OR numero_serie IS NOT NULL)
);
-- RNF04: patrimônio único (inclui inativos, para preservar o histórico por patrimônio)
CREATE UNIQUE INDEX uq_equip_patrimonio ON equipamento (upper(btrim(patrimonio)))
    WHERE patrimonio IS NOT NULL;
-- nº de série único dentro da mesma marca
CREATE UNIQUE INDEX uq_equip_serie ON equipamento (lower(coalesce(marca, '')), lower(btrim(numero_serie)))
    WHERE numero_serie IS NOT NULL;
CREATE INDEX idx_equip_setor ON equipamento (setor_id);
CREATE INDEX idx_equip_tipo  ON equipamento (tipo_id);