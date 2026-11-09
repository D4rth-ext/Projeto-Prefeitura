ALTER TABLE usuario
    ADD COLUMN revisao_senha UUID NOT NULL DEFAULT gen_random_uuid();

CREATE TABLE usuario_evento_senha (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario (id),
    autor_id BIGINT NOT NULL REFERENCES usuario (id),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE TRIGGER trg_usuario_evento_senha_imutavel
    BEFORE UPDATE OR DELETE OR TRUNCATE ON usuario_evento_senha
    FOR EACH STATEMENT EXECUTE FUNCTION impedir_alteracao_historico_os();

-- A aplicação não utiliza este campo. O RNF01 proíbe guardar dados de acesso remoto.
ALTER TABLE ordem_servico DROP COLUMN anydesk_id;
