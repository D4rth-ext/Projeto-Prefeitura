ALTER TABLE usuario
    ADD COLUMN situacao_cadastro VARCHAR(12) NOT NULL DEFAULT 'APROVADO',
    ADD COLUMN senha_temporaria_expira_em TIMESTAMPTZ,
    ADD CONSTRAINT ck_usuario_situacao_cadastro CHECK (
        situacao_cadastro = 'APROVADO'
        OR (perfil = 'SOLICITANTE' AND NOT ativo AND situacao_cadastro IN ('PENDENTE', 'REJEITADO'))
    );

CREATE TABLE usuario_evento_cadastro (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    autor_id BIGINT NOT NULL REFERENCES usuario(id),
    decisao VARCHAR(9) NOT NULL CHECK (decisao IN ('APROVADO', 'REJEITADO')),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TRIGGER trg_usuario_evento_cadastro_imutavel
    BEFORE UPDATE OR DELETE OR TRUNCATE ON usuario_evento_cadastro
    FOR EACH STATEMENT EXECUTE FUNCTION impedir_alteracao_historico_os();
