-- Endereços publicados no portal oficial da Prefeitura de Sant'Ana do Livramento.
-- A tabela setor já possui a coluna endereco; os grupos importados da lista
-- telefônica recebem o endereço da secretaria correspondente.

UPDATE setor
SET endereco = CASE
    WHEN nome LIKE 'Paço Municipal - %'
        THEN 'Rua Rivadávia Corrêa, 858 - Centro - RS, 97573-010'
    WHEN nome IN ('Secretaria de Administração', 'Secretaria de Desenvolvimento Econômico', 'Gabinete do Prefeito')
        THEN 'Rua Rivadávia Corrêa, 858 - Centro - RS, 97573-010'
    WHEN nome LIKE 'Secretaria da Fazenda - %'
        THEN 'Rua Brigadeiro Canabarro, 740 - Centro'
    WHEN nome IN ('Secretaria da Fazenda') OR nome LIKE 'Secretaria da Fazenda - %'
        THEN 'Rua Brigadeiro Canabarro, 740 - Centro'
    WHEN nome IN ('Secretaria de Saúde', 'Secretaria da Saúde') OR nome LIKE 'Secretaria de Saúde - %'
        THEN 'Rua Prefeito Hugolino Andrade, 433 - Centro'
    WHEN nome IN ('Secretaria de Planejamento e Meio Ambiente') OR nome LIKE 'Secretaria de Planejamento - %'
        THEN 'Rua Alan Kardec, 55 - Centro'
    WHEN nome IN ('Secretaria de Serviços Urbanos') OR nome LIKE 'Serviços Urbanos - %'
        THEN 'Av. Dom Pedro II, 401 - Faculdade'
    WHEN nome IN ('Secretaria Municipal de Obras') OR nome LIKE 'Secretaria de Obras - %'
        THEN 'Avenida Dom Pedro II, 401'
    WHEN nome IN ('Secretaria de Agricultura, Pecuária, Abastecimento e Assuntos Agrários')
         OR nome LIKE 'Secretaria de Agricultura - %'
        THEN 'Av. Dom Pedro II, 401 - Faculdade'
    WHEN nome IN ('Secretaria de Assistência e Inclusão Social') OR nome LIKE 'Secretaria Social - %'
        THEN 'Rua Sete de Setembro, 515 - Centro'
    WHEN nome IN ('Cultura', 'Secretaria de Cultura', 'Secretaria de Turismo')
        THEN 'Rua dos Andradas, 660 - Centro - 2º andar'
    WHEN nome = 'Secretaria de Educação'
        THEN 'Av. Tamandaré, 1759 - Centro'
END
WHERE nome LIKE 'Paço Municipal - %'
   OR nome LIKE 'Secretaria da Fazenda - %'
   OR nome LIKE 'Secretaria de Saúde - %'
   OR nome LIKE 'Secretaria de Planejamento - %'
   OR nome LIKE 'Serviços Urbanos - %'
   OR nome LIKE 'Secretaria de Obras - %'
   OR nome LIKE 'Secretaria de Agricultura - %'
   OR nome LIKE 'Secretaria Social - %'
   OR nome IN (
       'Secretaria de Administração',
       'Secretaria de Desenvolvimento Econômico',
       'Gabinete do Prefeito',
       'Secretaria da Fazenda',
       'Secretaria de Saúde',
       'Secretaria da Saúde',
       'Secretaria de Planejamento e Meio Ambiente',
       'Secretaria de Serviços Urbanos',
       'Secretaria Municipal de Obras',
       'Secretaria de Agricultura, Pecuária, Abastecimento e Assuntos Agrários',
       'Secretaria de Assistência e Inclusão Social',
       'Cultura',
       'Secretaria de Cultura',
       'Secretaria de Turismo',
       'Secretaria de Educação'
   );
