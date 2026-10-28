ALTER TABLE os_backup
    ADD COLUMN termo_ciencia_risco TEXT;

ALTER TABLE os_evento
DROP CONSTRAINT os_evento_tipo_check;

ALTER TABLE os_evento
    ADD CONSTRAINT ck_os_evento_tipo CHECK (
        tipo IN (
                 'STATUS', 'MODALIDADE', 'PRIORIDADE', 'PRAZO',
                 'TECNICO_ENTROU', 'TECNICO_SAIU',
                 'RECEBIMENTO', 'BACKUP'
            )
        );

ALTER TABLE ordem_servico
    ADD CONSTRAINT ck_os_recebimento_bancada CHECK (
        recebido_em IS NULL
            OR (modalidade = 'BANCADA' AND equipamento_id IS NOT NULL)
        );