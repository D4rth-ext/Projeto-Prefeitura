-- Complementos da Sprint 3. Preserva V1, V2 e V3 já executadas.

ALTER TABLE agendamento
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_os_modalidade_status ON ordem_servico (modalidade, status);
CREATE INDEX idx_os_setor_abertura ON ordem_servico (setor_id, aberta_em DESC, id DESC);
CREATE INDEX idx_os_registro_tipo ON os_registro (os_id, tipo);
CREATE INDEX idx_os_backup_pendente ON os_backup (situacao) WHERE solicitado AND descartado_em IS NULL;

-- NOT VALID preserva possíveis registros antigos; inserções e alterações novas
-- já precisam respeitar os dados completos do protocolo.
ALTER TABLE os_backup
    ADD CONSTRAINT ck_backup_resposta_completa CHECK (
        (solicitado AND escopo IS NOT NULL AND btrim(escopo) <> ''
            AND midia_ti IS NOT NULL AND btrim(midia_ti) <> '' AND situacao IS NOT NULL)
        OR
        (NOT solicitado AND ciencia_risco_por_id IS NOT NULL
            AND ciencia_risco_em IS NOT NULL
            AND termo_ciencia_risco IS NOT NULL AND btrim(termo_ciencia_risco) <> '')
    ) NOT VALID;

ALTER TABLE os_backup
    ADD CONSTRAINT ck_backup_descarte CHECK (
        (descartado_em IS NULL AND descartado_por_id IS NULL)
        OR
        (descartado_em IS NOT NULL AND descartado_por_id IS NOT NULL
            AND solicitado AND situacao = 'RESTAURADO_DEVOLVIDO')
    ) NOT VALID;

CREATE FUNCTION impedir_alteracao_historico_os()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'O histórico da OS é imutável.' USING ERRCODE = '23514';
END;
$$;

CREATE TRIGGER trg_os_evento_imutavel
    BEFORE UPDATE OR DELETE ON os_evento
    FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_historico_os();

CREATE TRIGGER trg_os_registro_imutavel
    BEFORE UPDATE OR DELETE ON os_registro
    FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_historico_os();
