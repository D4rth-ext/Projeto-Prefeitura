ALTER TABLE ordem_servico
    ADD COLUMN backup_solicitado BOOLEAN,
    ADD COLUMN escopo_backup VARCHAR(2000),
    ADD CONSTRAINT ck_os_backup_escopo CHECK (
        backup_solicitado IS DISTINCT FROM TRUE OR NULLIF(BTRIM(escopo_backup), '') IS NOT NULL
    );
