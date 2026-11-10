ALTER TABLE ordem_servico
    ADD COLUMN excluida_em TIMESTAMPTZ,
    ADD COLUMN excluida_por_id BIGINT REFERENCES usuario(id),
    ADD CONSTRAINT ck_ordem_servico_exclusao CHECK (
        (excluida_em IS NULL AND excluida_por_id IS NULL)
        OR (excluida_em IS NOT NULL AND excluida_por_id IS NOT NULL)
    );

CREATE INDEX idx_os_ativas_abertura
    ON ordem_servico (aberta_em DESC, id DESC)
    WHERE excluida_em IS NULL;
