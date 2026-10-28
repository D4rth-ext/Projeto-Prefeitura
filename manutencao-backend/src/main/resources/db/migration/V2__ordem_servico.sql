-- V2__ordem_servico.sql · Sprint 3 (OS, técnicos, registros, backup, agenda) · PostgreSQL 16
-- Destino: backend/src/main/resources/db/migration/V2__ordem_servico.sql

-- ============================================================
-- ORDEM DE SERVIÇO (= chamado). Nasce sem equipamento e sem
-- modalidade definida; a triagem (Atendente/Admin) preenche o
-- resto. "aberta_em" funciona como o criado_em desta tabela.
-- ============================================================
CREATE TABLE ordem_servico (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- é o próprio nº da OS
    setor_id              BIGINT       NOT NULL REFERENCES setor(id),       -- setor no momento da abertura (RF01.4)
    solicitante_id        BIGINT       NOT NULL REFERENCES usuario(id),
    aberto_por_id         BIGINT       NOT NULL REFERENCES usuario(id),     -- solicitante ou atendente
    equipamento_id        BIGINT       REFERENCES equipamento(id),          -- só obrigatório em Bancada

    categoria             VARCHAR(20)  NOT NULL
                          CHECK (categoria IN ('NAO_LIGA','LENTO','SEM_REDE','IMPRESSORA','SISTEMA','EMAIL','OUTRO')),
    descricao_problema    TEXT         NOT NULL CHECK (btrim(descricao_problema) <> ''),
    patrimonio_informado  VARCHAR(40),                                      -- o que o solicitante digitou, se digitou

    -- questionário rápido da abertura (RF03.1) — opcional
    computador_liga       VARCHAR(10)  CHECK (computador_liga IN ('SIM','NAO','NAO_SEI')),
    tem_rede              VARCHAR(10)  CHECK (tem_rede IN ('SIM','NAO','NAO_SEI')),
    consegue_anydesk      VARCHAR(10)  CHECK (consegue_anydesk IN ('SIM','NAO','NAO_SEI')),
    anydesk_id            VARCHAR(50),                                      -- nunca a senha do AnyDesk (RNF01)
    contato               VARCHAR(60),
    melhor_horario        VARCHAR(100),

    modalidade            VARCHAR(15)  NOT NULL DEFAULT 'A_DEFINIR'
                          CHECK (modalidade IN ('A_DEFINIR','REMOTO','PRESENCIAL','BANCADA')),
    motivo_bancada        VARCHAR(30)
                          CHECK (motivo_bancada IN ('NAO_LIGA_SEM_ACESSO','FALHA_HARDWARE',
                                                     'TENTATIVA_REMOTA_SEM_SUCESSO','SERVICO_EXIGE_BANCADA','OUTRO')),

    status                VARCHAR(20)  NOT NULL DEFAULT 'RECEBIDO'
                          CHECK (status IN ('RECEBIDO','EM_ANALISE','AGUARDANDO_PECA',
                                             'FINALIZADO','ENTREGUE','CANCELADO')),
    prioridade             VARCHAR(10) CHECK (prioridade IN ('BAIXA','MEDIA','ALTA','URGENTE')),

    prazo_previsto        DATE,
    resumo_solucao        TEXT,
    acessorios            TEXT,                                            -- só preenchido no recebimento (Bancada)
    motivo_cancelamento   TEXT,

    recebido_em           TIMESTAMPTZ,                                     -- equipamento chegou ao TI (Bancada)
    retirado_por          VARCHAR(120),                                    -- nome de quem retirou, na Entrega
    aberta_em             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finalizado_em         TIMESTAMPTZ,
    entregue_em           TIMESTAMPTZ,
    cancelado_em          TIMESTAMPTZ,

    versao                BIGINT       NOT NULL DEFAULT 0,                 -- @Version (RNF04: concorrência otimista)
    atualizado_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Recebido/Em Análise/Aguardando Peça exigem equipamento SE a modalidade for Bancada (RF04, linha "Recebido → Em Análise")
    CONSTRAINT ck_os_equipamento_bancada
        CHECK (modalidade <> 'BANCADA' OR status IN ('RECEBIDO','CANCELADO') OR equipamento_id IS NOT NULL)
);

CREATE INDEX idx_os_status      ON ordem_servico (status);
CREATE INDEX idx_os_setor       ON ordem_servico (setor_id);
CREATE INDEX idx_os_solicitante ON ordem_servico (solicitante_id);
CREATE INDEX idx_os_equipamento ON ordem_servico (equipamento_id);
CREATE INDEX idx_os_aberta_em   ON ordem_servico (aberta_em DESC);
-- alerta de atraso (RF05.2, vem na Sprint 4): prazo vencido entre as OS ainda abertas
CREATE INDEX idx_os_prazo_aberta ON ordem_servico (prazo_previsto)
    WHERE status IN ('RECEBIDO','EM_ANALISE','AGUARDANDO_PECA');

-- ============================================================
-- TÉCNICOS DA OS (RF03.4): vários por OS, todos com o mesmo
-- papel, com histórico de entrada/saída. Índice único parcial
-- garante no máximo um vínculo ATIVO por (OS, técnico).
-- ============================================================
CREATE TABLE os_tecnico (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    os_id          BIGINT      NOT NULL REFERENCES ordem_servico(id),
    tecnico_id     BIGINT      NOT NULL REFERENCES usuario(id),
    designado_por_id BIGINT    NOT NULL REFERENCES usuario(id),
    entrada_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    saida_em       TIMESTAMPTZ,
    motivo_saida   TEXT,
    CONSTRAINT ck_os_tecnico_saida CHECK (saida_em IS NULL OR motivo_saida IS NOT NULL)
);
CREATE UNIQUE INDEX uq_os_tecnico_ativo ON os_tecnico (os_id, tecnico_id) WHERE saida_em IS NULL;
CREATE INDEX idx_os_tecnico_os      ON os_tecnico (os_id);
CREATE INDEX idx_os_tecnico_tecnico ON os_tecnico (tecnico_id);

-- ============================================================
-- AUDITORIA automática: status, modalidade, prioridade, prazo
-- e trocas de técnico (RF04.3, RNF06). Gerada pelo sistema a
-- cada mudança — diferente de os_registro, que é escrita por
-- humanos.
-- ============================================================
CREATE TABLE os_evento (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    os_id           BIGINT      NOT NULL REFERENCES ordem_servico(id),
    tipo            VARCHAR(20) NOT NULL
                    CHECK (tipo IN ('STATUS','MODALIDADE','PRIORIDADE','PRAZO','TECNICO_ENTROU','TECNICO_SAIU')),
    autor_id        BIGINT      NOT NULL REFERENCES usuario(id),
    valor_anterior  TEXT,
    valor_novo      TEXT,
    motivo          TEXT,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_os_evento_os ON os_evento (os_id, criado_em);

-- ============================================================
-- REGISTRO TÉCNICO (RF03.5): linha do tempo imutável, escrita
-- por gente. ORIENTACAO é o único tipo visível ao Solicitante.
-- ============================================================
CREATE TABLE os_registro (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    os_id       BIGINT      NOT NULL REFERENCES ordem_servico(id),
    tipo        VARCHAR(15) NOT NULL
                CHECK (tipo IN ('DIAGNOSTICO','ACAO','OBSERVACAO','ORIENTACAO')),
    autor_id    BIGINT      NOT NULL REFERENCES usuario(id),
    texto       TEXT        NOT NULL CHECK (btrim(texto) <> ''),
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_os_registro_os ON os_registro (os_id, criado_em);

-- ============================================================
-- PROTOCOLO DE BACKUP (RF03.7), 1:1 com a OS. Só existe quando
-- a pergunta já foi respondida no recebimento (Bancada).
-- ============================================================
CREATE TABLE os_backup (
    os_id                BIGINT      PRIMARY KEY REFERENCES ordem_servico(id),
    solicitado           BOOLEAN     NOT NULL,
    ciencia_risco_por_id BIGINT      REFERENCES usuario(id),  -- obrigatório quando solicitado = false
    ciencia_risco_em     TIMESTAMPTZ,
    escopo               TEXT,                                 -- obrigatório quando solicitado = true
    midia_ti             VARCHAR(80),                           -- identificação do HD/pendrive do TI
    situacao             VARCHAR(20)
                         CHECK (situacao IN ('PENDENTE','CONCLUIDO','RESTAURADO_DEVOLVIDO')),
    descartado_em        TIMESTAMPTZ,                           -- até 30 dias após a entrega (RF03.7/RNF07)
    descartado_por_id    BIGINT      REFERENCES usuario(id),
    atualizado_em        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_backup_ciencia   CHECK (solicitado OR ciencia_risco_por_id IS NOT NULL),
    CONSTRAINT ck_backup_escopo    CHECK (NOT solicitado OR escopo IS NOT NULL)
);

-- ============================================================
-- AGENDA TÉCNICA (RF05.1). OS é opcional (visita presencial,
-- sessão remota ou serviço externo sem chamado associado).
-- ============================================================
CREATE TABLE agendamento (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tecnico_id     BIGINT       NOT NULL REFERENCES usuario(id),
    os_id          BIGINT       REFERENCES ordem_servico(id),
    setor_id       BIGINT       REFERENCES setor(id),
    titulo         VARCHAR(160) NOT NULL CHECK (btrim(titulo) <> ''),
    descricao      TEXT,
    local          VARCHAR(255),
    inicio         TIMESTAMPTZ  NOT NULL,
    fim            TIMESTAMPTZ  NOT NULL,
    status         VARCHAR(15)  NOT NULL DEFAULT 'AGENDADO'
                   CHECK (status IN ('AGENDADO','CONCLUIDO','CANCELADO')),
    criado_por_id  BIGINT       NOT NULL REFERENCES usuario(id),
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_agendamento_periodo CHECK (fim > inicio)
);
CREATE INDEX idx_agendamento_tecnico ON agendamento (tecnico_id, inicio);
CREATE INDEX idx_agendamento_os      ON agendamento (os_id);
