// Setores identificados na lista telefônica municipal.
const grupos = [
  ['Paço Municipal', 201, 'TI|TI 2|Vice-prefeito|Controle Interno|Desenvolvimento Econômico|Gabinete do Vice-prefeito|Procuradoria|Protocolo da Procuradoria|Administração 1|Administração 2|Recepção Administrativa da Secretaria Geral|Secretaria Geral do Governo||Habitação 2|Junta Militar|Almoxarifado|Patrimônio|Departamento Pessoal 1|Departamento Pessoal 2|Folha de Pagamento|Arquivos'],
  ['', 222, 'Comunicação|Ouvidoria|COMECON|Defesa Civil|CMJDIR|Cultura'],
  ['Secretaria de Trânsito', 228, 'Fiscalização|Protocolo|Videomonitoramento|Compras|Almoxarifado'],
  ['Secretaria de Planejamento', 233, 'Recepção|Gabinete|Engenheiro|Recepção DEMA|DEMA|Plano Diretor|DCO|Análise|Obras|Plano Diretor 2'],
  ['Serviços Urbanos', 243, 'Secretário|Jurídico|Gabinete|Recepção|RH|Iluminação Pública|Compras|Administrativo|Almoxarifado'],
  ['Secretaria de Trânsito', 252, 'Sinalização'],
  ['Secretaria de Obras', 253, 'Engenharia|Recepção|Gabinete|Administrativo|Guarita|Almoxarifado'],
  ['Secretaria de Agricultura', 259, 'Administrativo|SIM|Estradas Rurais|DTA|Sanidade Animal'],
  ['Secretaria de Obras', 264, 'Oficina'],
  ['Secretaria da Fazenda', 265, 'Tesouraria|Assessoria do Gabinete|Gabinete|Licitações|Empenhos|Procuradoria|Contabilidade|Cobrança|ICMS|ITR|Sala de Reunião 2º Andar|Arrecadação|ITBI|Protocolo|Sala de Reunião 1º Piso|Cadastro|Compras|Tributário|Sala do Empreendedor|Fiscalização do Comércio|Treinamento'],
  ['Secretaria de Saúde', 300, 'GI|Gabinete|RH|Transporte|Transporte|Contabilidade|TFD|Jurídico|Compras|TFD|Vigilância Sanitária||NASF|SAE'],
  ['', 314, 'SF Rural - Recepção|Farmácia Municipal - Recepção|Farmácia Municipal - Atendimento|ESF Planalto - Recepção||CAPS 1 - Recepção|ESF Tabatinga - Recepção|CAPS AD - Recepção|CAPS i - Recepção|ESF Simon Bolívar - Recepção'],
  ['PAM', 324, 'Recepção|Administrativo|Vacinas'],
  ['', 327, 'Clínica de Fisioterapia - Recepção|Laboratório da Fronteira - Recepção|ESF Vila Real - Recepção'],
  ['Unidade Sanitária Carlos I', 330, 'Recepção|Saúde da Mulher|Recepção|Pré-natal'],
  ['', 334, 'ESF Santa Rosa - Recepção|Unidade Sanitária Carlos I - Saúde da Mulher|ESF Prado - Recepção|Almoxarifado da Saúde|PAM - Perícia|EAP Divisa - Recepção|EAP Divisa - Área 11|EAP Divisa - Área 13|Garagem da Saúde - Recepção|ESF Wilson - Recepção|ESF Armour Jovelino Santana 1 - Recepção|ESF Armour Jovelino Santana 2 - Recepção|ESF Armour Jovelino Santana 3 - Recepção'],
  ['Secretaria Social', 500, 'Recepção|Bolsa Família|Técnica|Administrativo|Conselho Tutelar|Recepção CREAS|CRAS Prado|CRAS Armour|PCF|Albergue|Restaurante Social|SCFV']
]

const ramaisPorSetor = new Map()
for (const [grupo, primeiroRamal, nomes] of grupos) {
  nomes.split('|').forEach((nome, indice) => {
    if (!nome) return
    const setor = grupo ? `${grupo} - ${nome}` : nome
    const ramais = ramaisPorSetor.get(setor) || []
    ramais.push(primeiroRamal + indice)
    ramaisPorSetor.set(setor, ramais)
  })
}

export const catalogoSetores = [...ramaisPorSetor.keys()].sort((a, b) => a.localeCompare(b, 'pt-BR'))
export const telefoneDoSetor = nome => ramaisPorSetor.get(nome)?.join(' / ') || ''
