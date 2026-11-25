-- Complementa V13 com endereços próprios publicados no portal municipal.
-- Limpa endereços herdados para unidades sociais cuja localização específica
-- não foi confirmada em fonte oficial atual.
UPDATE setor
SET endereco = CASE nome
    WHEN 'Administração' THEN 'Rua Rivadávia Corrêa, 858 - Centro - RS, 97573-010'
    WHEN 'folha' THEN 'Rua Rivadávia Corrêa, 858 - Centro - RS, 97573-010'
    WHEN 'Defesa Civil' THEN 'Rua Barão do Triunfo, 1556'
    WHEN 'Ouvidoria' THEN 'Rua Duque de Caxias, 1783 - Centro - CEP 97573-616'
    WHEN 'Secretaria Social - Conselho Tutelar' THEN 'Rua dos Andradas, 1157 - Centro'
    WHEN 'Secretaria Social - Albergue' THEN 'Rua Sete de Setembro, 515 - Centro'
    WHEN 'Secretaria Social - CRAS Armour' THEN NULL
    WHEN 'Secretaria Social - CRAS Prado' THEN 'Av. Alm. Saldanha da Gama, 1879 - Prado'
    WHEN 'Secretaria Social - Recepção CREAS' THEN 'Rua dos Andradas, 1265 - Centro'
    WHEN 'Secretaria Social - Restaurante Social' THEN NULL
    WHEN 'Secretaria Social - SCFV' THEN NULL
    WHEN 'Secretaria Social - PCF' THEN NULL
    WHEN 'Almoxarifado da Saúde' THEN 'Rua Rivadávia Corrêa, 429 - Centro'
    WHEN 'CAPS 1 - Recepção' THEN 'Rua Senador Salgado Filho, 962 - Centro'
    WHEN 'CAPS AD - Recepção' THEN 'Rua João Manoel, 277 - Prado'
    WHEN 'CAPS i - Recepção' THEN 'Rua General Câmara, 1668 - Centro'
    WHEN 'Clínica de Fisioterapia - Recepção' THEN 'Av. Daltro Filho, 824 - fundos - Centro'
    WHEN 'ESF Armour Jovelino Santana 1 - Recepção' THEN 'Rua Nazareno Cenereli, 284 - Armour'
    WHEN 'ESF Armour Jovelino Santana 2 - Recepção' THEN 'Rua Nazareno Cenereli, 284 - Armour'
    WHEN 'ESF Armour Jovelino Santana 3 - Recepção' THEN 'Rua Nazareno Cenereli, 284 - Armour'
    WHEN 'ESF Planalto - Recepção' THEN 'Rua Irmão Rafael, s/n - Planalto'
    WHEN 'ESF Prado - Recepção' THEN 'Rua Rodrigues Barbosa, 31 - Prado'
    WHEN 'ESF Santa Rosa - Recepção' THEN 'Rua Prefeito Agenor Barcelo Feio, 9 - Santa Rosa'
    WHEN 'ESF Simon Bolívar - Recepção' THEN 'Rua Mª Aldina Saraiva Juvenal, 195 - Simon Bolívar'
    WHEN 'ESF Tabatinga - Recepção' THEN 'Rua Camilo Alves Gisler, 1411 - Tabatinga'
    WHEN 'ESF Vila Real - Recepção' THEN 'Rua Vicente Ilha de Vargas, s/n - Vila Real'
    WHEN 'ESF Wilson - Recepção' THEN 'Av. Dom Pedro II, 2628 - Wilson'
    WHEN 'EAP Divisa - Área 11' THEN 'Rua Joaquim da Costa Vargas, 201 - Divisa'
    WHEN 'EAP Divisa - Área 13' THEN 'Rua Joaquim da Costa Vargas, 201 - Divisa'
    WHEN 'EAP Divisa - Recepção' THEN 'Rua Joaquim da Costa Vargas, 201 - Divisa'
    WHEN 'Farmácia Municipal - Atendimento' THEN 'Rua Rivadávia Corrêa, 429 - Centro'
    WHEN 'Farmácia Municipal - Recepção' THEN 'Rua Rivadávia Corrêa, 429 - Centro'
    WHEN 'Laboratório da Fronteira - Recepção' THEN 'Av. Daltro Filho, 824 - Centro'
    WHEN 'PAM - Administrativo' THEN 'Rua Manduca Rodrigues, 595 - Centro'
    WHEN 'PAM - Perícia' THEN 'Rua Manduca Rodrigues, 595 - Centro'
    WHEN 'PAM - Recepção' THEN 'Rua Manduca Rodrigues, 595 - Centro'
    WHEN 'PAM - Vacinas' THEN 'Rua Manduca Rodrigues, 595 - Centro'
    WHEN 'Secretaria de Saúde - SAE' THEN 'Rua Rivadávia Corrêa, 794 - Centro'
    WHEN 'Secretaria de Saúde - Vigilância Sanitária' THEN 'Rua Rivadávia Corrêa, 908 - Centro'
    WHEN 'Unidade Sanitária Carlos I - Pré-natal' THEN 'Av. Daltro Filho, 824 - Centro'
    WHEN 'Unidade Sanitária Carlos I - Recepção' THEN 'Av. Daltro Filho, 824 - Centro'
    WHEN 'Unidade Sanitária Carlos I - Saúde da Mulher' THEN 'Av. Daltro Filho, 824 - Centro'
    ELSE endereco
END
WHERE nome IN (
    'Administração', 'folha', 'Defesa Civil', 'Ouvidoria',
    'Secretaria Social - Conselho Tutelar', 'Secretaria Social - Albergue',
    'Secretaria Social - CRAS Armour', 'Secretaria Social - CRAS Prado',
    'Secretaria Social - Recepção CREAS', 'Secretaria Social - Restaurante Social',
    'Secretaria Social - SCFV', 'Secretaria Social - PCF', 'Almoxarifado da Saúde',
    'CAPS 1 - Recepção', 'CAPS AD - Recepção', 'CAPS i - Recepção',
    'Clínica de Fisioterapia - Recepção', 'ESF Armour Jovelino Santana 1 - Recepção',
    'ESF Armour Jovelino Santana 2 - Recepção', 'ESF Armour Jovelino Santana 3 - Recepção',
    'ESF Planalto - Recepção', 'ESF Prado - Recepção', 'ESF Santa Rosa - Recepção',
    'ESF Simon Bolívar - Recepção', 'ESF Tabatinga - Recepção',
    'ESF Vila Real - Recepção', 'ESF Wilson - Recepção',
    'EAP Divisa - Área 11', 'EAP Divisa - Área 13', 'EAP Divisa - Recepção',
    'Farmácia Municipal - Atendimento', 'Farmácia Municipal - Recepção',
    'Laboratório da Fronteira - Recepção', 'PAM - Administrativo', 'PAM - Perícia',
    'PAM - Recepção', 'PAM - Vacinas', 'Secretaria de Saúde - SAE',
    'Secretaria de Saúde - Vigilância Sanitária',
    'Unidade Sanitária Carlos I - Pré-natal',
    'Unidade Sanitária Carlos I - Recepção',
    'Unidade Sanitária Carlos I - Saúde da Mulher'
);
