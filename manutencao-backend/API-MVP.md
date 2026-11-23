# API do MVP de manutenção

Este guia serve para integrar o dashboard ao backend. Use `http://localhost:8080/api/v1` em desenvolvimento. As rotas abaixo são relativas a essa URL. O dashboard pode chamar a API pela origem `http://localhost:5173` ou pelo proxy do Vite.

## Roteiro para integrar o dashboard

1. **Cadastro e acesso.** Cadastre um Solicitante pelo dashboard, confirme que a tela informa a espera e aprove o cadastro com uma conta Admin. Volte ao login do Solicitante e confira se ele consegue entrar.
2. **Abertura e triagem.** Abra uma OS pelo Solicitante. Na conta do Atendente, localize o mesmo ID em `GET /os/busca` e faça a triagem com um Técnico ativo. Na conta do Solicitante, confira o detalhe e o histórico de status.
3. **Atendimento e linha do tempo.** Entre como o Técnico vinculado e inicie a análise. Registre um diagnóstico e uma orientação. Enquanto a OS estiver em análise, use a conta do Solicitante para ler a orientação e enviar uma observação. Confira se a observação aparece para a equipe e finalize a OS com a conta do Técnico.
4. **Senha temporária.** Redefina a senha de uma conta de teste como Admin. Entre com a senha temporária e confira se o dashboard abre a tela de troca. Após salvar a senha definitiva, faça login novamente.
5. **Bancada e relatórios.** Faça uma OS de bancada até a entrega e, se houver backup, registre o descarte. Confira a agenda, os filtros de busca e os totais do relatório com as contas correspondentes.

Em 16/11/2026, a nova execução passou em **10 testes unitários** e **164 verificações HTTP** contra PostgreSQL 16. O teste também aplicou as migrações da V8 à V11 preservando contas preexistentes. Use o roteiro acima para conferir esses fluxos nas telas do dashboard.

## Iniciar o backend local

Use Java 21, Maven e um banco PostgreSQL 16 com suporte a ICU. O Flyway aplica as migrações até a V11 na inicialização.

| Variável | Uso |
| --- | --- |
| `DB_URL` | Padrão `jdbc:postgresql://localhost:5432/manutencao` |
| `DB_USER` | Padrão `manutencao` |
| `DB_PASSWORD` | Senha do usuário do banco |
| `JWT_SECRET` | Chave aleatória, com pelo menos 32 bytes |
| `APP_ADMIN_INICIAL_EMAIL` | E-mail para o primeiro administrador, exigido se não houver Admin ativo |
| `APP_ADMIN_INICIAL_SENHA` | Senha inicial forte, exigida na criação do primeiro Admin |
| `PORT` | Padrão `8080` |

Defina as variáveis no ambiente do IntelliJ ou do terminal e execute `mvn spring-boot:run`. A chave externa é obrigatória também no perfil `local`. Consulte `GET /actuator/health` na raiz do servidor. Use credenciais diferentes por ambiente.

Em uma base existente, a V11 classifica os cadastros anteriores como `APROVADO` e conserva o campo `ativo`. A aprovação manual vale para novos autocadastros. Revise as contas que já tinham acesso antes de liberar a aplicação. Se a V6 ainda for aplicada, ajuste eventuais colisões de nomes de setores preservando seus IDs. A comparação ignora espaços e maiúsculas.

## Autenticação e cadastros

As rotas protegidas recebem `Authorization: Bearer <token>`. Para JSON, envie `Content-Type: application/json`.

| Método e rota | Uso |
| --- | --- |
| `POST /auth/login` | Envia `email` e `senha`. Retorna `token`, `nome`, `perfil` e `trocaSenhaObrigatoria`. |
| `GET /auth/setores-publicos` | Retorna ID e nome de setores ativos para o autocadastro. |
| `POST /auth/registro` | Envia `nome`, `email`, `setorId`, `senha`, `confirmarSenha` e `telefone`. Retorna `201` com o Solicitante pendente, sem token. |
| `GET /usuarios/cadastros-pendentes` | Admin. Lista os autocadastros que aguardam decisão. |
| `PATCH /usuarios/{id}/cadastro` | Admin. Aprova ou rejeita um cadastro pendente. |
| `GET /usuarios/solicitantes` | Admin e Atendente. Filtra contas ativas por `busca` (parte do nome ou e-mail) e `setorId`. Retorna `id`, `nome`, `setorId`, `setorNome` e `setorAtivo`. Para abrir OS, ofereça apenas os resultados com `setorAtivo: true`. |
| `GET /solicitantes` | Admin e Atendente. Lista simples para abrir OS em nome de alguém. Inclui solicitantes ativos de setores ativos e retorna `id`, `nome`, `setorId` e `setorNome`. |
| `GET /equipe` | Admin e Atendente. Lista equipe ativa. Use o perfil `TECNICO` na designação. |
| `GET /usuarios`, `POST /usuarios`, `PUT /usuarios/{id}`, `DELETE /usuarios/{id}` | Admin. Lista usuários, cria e edita contas da equipe e inativa usuários. Para criar, envie `nome`, `email`, `senha`, `perfil` e, opcionalmente, `telefone`. Para editar, envie `nome`, `email` e `telefone`. Alterar o e-mail encerra as sessões anteriores da conta. |
| `PATCH /usuarios/{id}/setor` | Admin. Altera o setor de um solicitante ativo. Envie `setorId` de um setor ativo. |
| `GET /setores`, `GET /setores/{id}` | Equipe. Listagem aceita `busca` e `incluirInativos`. |
| `GET /equipamentos`, `GET /equipamentos/{id}`, `GET /tipos-equipamento` | Equipe. Equipamentos aceitam `busca`, `setorId` e `incluirInativos`. |
| `POST /setores`, `PUT /setores/{id}`, `DELETE /setores/{id}`, `PATCH /setores/{id}/reativar` | Admin e Atendente. Corpo de criação/edição com `nome`, `telefone`, `email` e `endereco`. |
| `POST /equipamentos`, `PUT /equipamentos/{id}`, `DELETE /equipamentos/{id}`, `PATCH /equipamentos/{id}/reativar` | Admin e Atendente. Corpo de criação/edição com `setorId`, `tipoId`, `marca`, `modelo`, `patrimonio`, `numeroSerie` e `observacoes`. Informe patrimônio ou número de série. |

### Quando alguém se cadastra

1. Depois do `201` no autocadastro, mostre **Cadastro recebido. Aguarde a aprovação do administrador.** A conta nasce com `situacaoCadastro: "PENDENTE"` e `ativo: false`.
2. O Admin abre a fila de pendentes, confere a identidade e o vínculo com o setor, e envia `PATCH /usuarios/{id}/cadastro` com `{"decisao":"APROVADO"}` ou `{"decisao":"REJEITADO"}`.
3. A API retorna `200` com o usuário atualizado e registra a decisão com autor e horário. Só a conta aprovada e ativa pode entrar. Uma segunda decisão para o mesmo cadastro retorna `409`. Aprovação com setor inativo retorna `422`.

`situacaoCadastro` informa se o cadastro está `PENDENTE`, `APROVADO` ou `REJEITADO`. Use `ativo` para indicar se uma conta aprovada tem acesso. Login com cadastro pendente, rejeitado, inativo ou credencial incorreta recebe `401`.

### Quando alguém precisa redefinir a senha

1. O Admin chama `PATCH /usuarios/{id}/senha` sem corpo para outra conta ativa. A resposta `200` traz `senhaTemporaria` e `expiraEm`. Mostre a senha uma vez para encaminhamento por um canal interno. Evite registrar a resposta em logs ou salvar a credencial no navegador. A senha temporária dura 24 horas e a emissão invalida as sessões anteriores da conta.
2. A pessoa entra com a senha temporária. Quando `trocaSenhaObrigatoria` vier como `true`, encaminhe para a tela de troca. Esse token permite somente `PATCH /usuarios/me/senha` entre as operações autenticadas.
3. Envie `senhaAtual` com a senha temporária, mais `novaSenha` e `confirmarSenha`. Após o `204`, descarte o token temporário e solicite login com a senha definitiva.

Para mudar sua própria senha definitiva, o Admin usa a mesma rota `/usuarios/me/senha` com a senha atual. As senhas definitivas precisam de ao menos oito caracteres, com letra e número, e até 72 bytes.

O backend limita as tentativas de login a 30 e os envios de autocadastro a 5 por minuto, por endereço de conexão e instância da API. No `429`, mostre uma mensagem para aguardar antes de tentar novamente.

## Ordens de serviço

### Abertura e consulta

`POST /os` retorna `201`, o corpo da OS e o cabeçalho `Location`. Use o `id` do corpo para abrir o detalhe no dashboard.

```json
{
  "categoria": "SEM_REDE",
  "descricaoProblema": "O computador não consegue acessar a rede."
}
```

Categorias `NAO_LIGA`, `LENTO`, `SEM_REDE`, `IMPRESSORA`, `SISTEMA`, `EMAIL` e `OUTRO`. Admin e Atendente incluem `solicitanteId`. Para o Solicitante, a API usa a conta autenticada. `contato` e `backupSolicitado` são obrigatórios; quando `backupSolicitado` é `true`, `escopoBackup` também é obrigatório (até 2000 caracteres). `patrimonioInformado`, `computadorLiga`, `temRede`, `consegueAnyDesk` e `melhorHorario` são opcionais. Respostas rápidas aceitam `SIM`, `NAO` e `NAO_SEI`. O atendimento remoto registra a modalidade, sem coletar dados de acesso remoto.

- `PATCH /os/{id}/solicitacao` permite ao Solicitante dono da OS, Admin ou Atendente editar a solicitação enquanto ela está `RECEBIDO` e ainda não foi triada. Envie os campos de abertura e a `versao` atual.
- `PATCH /os/{id}/patrimonio` permite ao Admin ou Técnico vinculado corrigir `patrimonioInformado` durante o atendimento. Envie o valor e a `versao` atual; a correção fica registrada no histórico.

- `GET /os/{id}` para detalhes. O Solicitante só vê suas OSs e o Técnico só vê as que têm vínculo ativo com ele. Na visão do Solicitante, campos de controle interno como `prioridade`, `motivoBancada`, `equipamentoId`, `acessorios` e `retiradoPor` vêm nulos ou ausentes.
- `GET /os/busca` para lista paginada. Filtros combináveis `id`, `setorId`, `status`, `modalidade`, `atrasadas`, `encerrada`, `inicio`, `fim`, `pagina` (base zero) e `tamanho` (1–100). A equipe também pode usar `equipamentoId`, `patrimonio`, `numeroSerie`, `tecnicoId` e `backupEmAberto`. A resposta contém `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`.
- Para o painel de atraso, use `GET /os/busca?atrasadas=true&tamanho=1` e leia `totalElementos`. Cada OS tem também o campo `atrasada`.
- `GET /os/{id}/historico-status` traz `anterior`, `status`, `criadoEm` e `autorNome` em ordem cronológica. Começa na abertura com `status: "RECEBIDO"` e `anterior: null`. Respeita o mesmo acesso do detalhe da OS.
- `GET /os/{id}/registros` para a linha do tempo. O Solicitante recebe orientações da equipe (`ORIENTACAO`) e as próprias observações (`OBSERVACAO_SOLICITANTE`). A equipe autorizada também pode consultar `/os/{id}/eventos`, `/os/{id}/tecnicos/historico` e `/os/{id}/tecnicos`. A última rota retorna os IDs dos vínculos ativos.

### Adicionar observação como solicitante

Mostre o botão **Adicionar observação** no detalhe da OS para o Solicitante quando o status for `RECEBIDO`, `EM_ANALISE` ou `AGUARDANDO_PECA`. A API confirma que a OS pertence à conta autenticada.

Envie `POST /os/{id}/registros` com a versão obtida em `GET /os/{id}`.

```json
{
  "tipo": "OBSERVACAO_SOLICITANTE",
  "texto": "Correção: meu ramal é 204.",
  "versao": 3
}
```

A API devolve `201 Created` com o registro da linha do tempo.

```json
{
  "id": 24,
  "osId": 12,
  "tipo": "OBSERVACAO_SOLICITANTE",
  "autorId": 8,
  "autorNome": "Maria Silva",
  "texto": "Correção: meu ramal é 204.",
  "criadoEm": "2026-11-05T17:00:00Z",
  "versao": 4
}
```

Após salvar, mostre o registro e use a nova `versao`. A observação é imutável e visível ao Solicitante, ao Admin, ao Atendente e ao Técnico vinculado. A descrição original da OS é preservada. O tipo `OBSERVACAO` continua representando as observações internas da equipe.

- `400` se o texto estiver em branco. `403` se outro perfil tentar publicar como Solicitante. `404` se a OS estiver indisponível para a conta.
- `409` se a versão estiver desatualizada. Recarregue a OS e preserve o texto digitado para o usuário tentar novamente se ela ainda estiver em atendimento.
- `422` se a OS estiver finalizada, entregue ou cancelada e a versão enviada estiver atualizada.

### Operações da equipe

Todas as operações abaixo recebem a `versao` atual da OS. Use a versão devolvida pela operação ou recarregue os detalhes antes de enviar outra alteração.

| Método e rota | Quem pode usar | Campos além de `versao` |
| --- | --- | --- |
| `PATCH /os/{id}/triagem` | Admin e Atendente | `modalidade` (`REMOTO`, `PRESENCIAL` ou `BANCADA`), `prioridade` (`BAIXA`, `MEDIA`, `ALTA` ou `URGENTE`) e pelo menos um ID em `tecnicosIds`. Em bancada, `motivoBancada` e `justificativa`. Ao retirar técnico, `motivoRemocao`. |
| `POST /os/{id}/recebimento` | Admin e Atendente | `equipamentoId` e `acessorios`. A opção do solicitante em `POST /os` define o backup. Se sim, informe `midiaTi`; se não, envie `cienciaRiscoConfirmada: true`. OS antigas ainda podem receber a resposta durante o recebimento. |
| `POST /os/{id}/registros` | Admin e Técnico vinculado. Atendente para observação interna e orientação. | `tipo` (`DIAGNOSTICO`, `ACAO`, `OBSERVACAO`, `ORIENTACAO`) e `texto`. |
| `PATCH /os/{id}/prazo` | Admin e Técnico vinculado | `prazoPrevisto` no formato `AAAA-MM-DD` e `justificativa`. |
| `PATCH /os/{id}/status` | Depende da transição, conforme a tabela abaixo. | `status` e os campos da transição. |
| `PATCH /os/{id}/backup` | Admin e Atendente | `situacao`. A ordem é `PENDENTE` → `CONCLUIDO` → `RESTAURADO_DEVOLVIDO`. |
| `POST /os/{id}/backup/descarte` | Admin e Atendente | `descarteConfirmado: true` após a eliminação da cópia da mídia. |

#### Mudança de status

| Ação | Status enviado | Quem pode usar | O que informar |
| --- | --- | --- | --- |
| Iniciar análise | `EM_ANALISE` | Admin e Técnico vinculado | Um `prazoPrevisto` de hoje em diante. Pode aproveitar um prazo vigente da OS. A OS precisa de triagem e, na bancada, de recebimento. |
| Aguardar peça | `AGUARDANDO_PECA` | Admin e Técnico vinculado | `registroPecaId` de um diagnóstico ou ação da OS e `justificativa`. |
| Retomar após receber a peça | `EM_ANALISE` | Admin e Técnico vinculado | A equipe técnica precisa estar ativa na OS. |
| Encaminhar atendimento remoto ou presencial para bancada | `RECEBIDO` | Admin, Atendente e Técnico vinculado | `motivoBancada` e `justificativa`. |
| Finalizar a análise | `FINALIZADO` | Admin e Técnico vinculado | `resumoSolucao` e um diagnóstico já registrado. Se houver backup solicitado na bancada, o protocolo precisa estar em `RESTAURADO_DEVOLVIDO`. |
| Reabrir uma OS finalizada | `EM_ANALISE` | Admin e Técnico vinculado | `justificativa`. A reabertura acontece antes da entrega física. |
| Entregar equipamento de bancada | `ENTREGUE` | Admin e Atendente | `retiradoPor`. A OS precisa estar finalizada e ter recebimento registrado. |
| Cancelar | `CANCELADO` | Admin, Atendente e Técnico vinculado; o Solicitante dono pode cancelar antes do atendimento somente uma OS presencial ainda não recebida | `justificativa` e, se o equipamento foi recebido, `retiradoPor`. |

O status inicial é `RECEBIDO`. Os demais são `EM_ANALISE`, `AGUARDANDO_PECA`, `FINALIZADO`, `ENTREGUE` e `CANCELADO`. Admin, Atendente e Técnico vinculado consultam `GET /os/{id}/recebimento` e `GET /os/{id}/backup`. Admin e Atendente gerenciam os protocolos e consultam `GET /os/backups/descarte-pendente`, que inclui pendências de OSs já excluídas com equipamento devolvido.

`DELETE /os/{id}` faz a exclusão lógica e é exclusivo do Admin. A API devolve `422` enquanto o equipamento estiver com o TI ou o backup solicitado aguardar descarte.

## Agenda e relatórios

- `GET /agendamentos` aceita `tecnicoId`, `status`, `inicio` e `fim`. O Técnico consulta a própria agenda. Admin e Atendente também usam `POST /agendamentos`, `PUT /agendamentos/{id}` e `PATCH /agendamentos/{id}/status`.
- O corpo para criar ou editar agenda contém `tecnicoId`, `titulo`, `inicio` e `fim` (instantes ISO 8601). Campos opcionais `osId`, `setorId`, `descricao` e `local`. Atualização e alteração de status exigem `versao`. Status `AGENDADO`, `CONCLUIDO` e `CANCELADO`.
- `GET /relatorios/os?inicio=2026-11-01&fim=2026-11-30&setorId=1&status=FINALIZADO` para Admin e Atendente. Todos os filtros são opcionais. O período considera a **data de abertura**, com ambas as pontas inclusivas no fuso de São Paulo. Retorna `total`, `atrasadas`, `porStatus`, `porModalidade` e `porSetor`. A situação é a atual no momento da consulta.
- Para as listagens e a impressão do MVP, use os filtros de `GET /os/busca` e renderize os resultados no dashboard. Use `encerrada=true` para entregues, canceladas e finalizadas fora da bancada. Use `encerrada=false` para abertas, incluindo bancada finalizada que aguarda entrega. O histórico usa `patrimonio` ou `numeroSerie`, e a listagem de cópias em andamento usa `backupEmAberto=true`.

## Tratamento de respostas

- `400` para campos ou parâmetros inválidos. Quando houver JSON com `campos`, use cada par `campo` e `mensagem` ao lado do campo correspondente.
- `401` para credenciais inválidas ou sessão expirada/revogada. `403` para ação vedada ao perfil. `404` para recurso indisponível à conta.
- `409` em caso de duplicidade ou versão desatualizada. Para conflito de versão, recarregue o registro e apresente o estado atual antes de tentar novamente.
- `422` para regra de negócio. Exiba `mensagem` quando houver corpo JSON. A interface deve aceitar também respostas sem corpo.
- `429` ao exceder o limite de tentativas de login ou envios de autocadastro.
