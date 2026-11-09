-- Nomes distintos apenas por espaços ou caixa devem ser resolvidos antes desta migração.
CREATE UNIQUE INDEX uq_setor_nome
    ON setor (lower(regexp_replace(nome COLLATE "und-x-icu", '[[:space:]]', '', 'g')));
DROP INDEX uq_setor_nome_ativo;
