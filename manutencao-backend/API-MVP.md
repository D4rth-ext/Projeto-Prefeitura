# API do MVP de manutenção

A URL local é `http://localhost:8080/api/v1`. O frontend de desenvolvimento pode usar a origem `http://localhost:5173` ou o proxy do Vite. A rota real de chamados é `/os`.

## Iniciar o backend local

Use Java 21, Maven e um banco PostgreSQL 16 com suporte a ICU. O Flyway aplica as migrações V1 a V7 na inicialização.

| Variável | Uso |
| --- | --- |
| `DB_URL` | Padrão `jdbc:postgresql://localhost:5432/manutencao` |
| `DB_USER` | Padrão `manutencao` |
| `DB_PASSWORD` | Senha do usuário do banco |
| `JWT_SECRET` | Chave aleatória, com pelo menos 32 bytes |
| `APP_ADMIN_INICIAL_EMAIL` | E-mail para o primeiro administrador, exigido se não houver Admin ativo |
| `APP_ADMIN_INICIAL_SENHA` | Senha inicial forte, exigida na criação do primeiro Admin |
| `PORT` | Padrão `8080` |

Defina as variáveis no ambiente do IntelliJ ou do terminal e execute `mvn spring-boot:run`. Consulte `GET /actuator/health` no servidor. Use credenciais diferentes por ambiente.

Em um banco existente, redefina a senha de qualquer administrador criado com a senha fixa das versões anteriores antes de disponibilizar o ambiente. Os tokens emitidos antes da V5 precisam ser renovados pelo login. A V5 remove o campo legado de acesso remoto. A V6 exige nomes de setores únicos mesmo entre inativos, ignorando maiúsculas e espaços. Se houver colisões, ajuste os nomes preservando os IDs antes de reaplicar a migração.

## Autenticação e cadastros

As rotas protegidas recebem `Authorization: Bearer <token>`. Para JSON, envie `Content-Type: application/json`.

| Método e rota | Uso |
| --- | --- |
| `POST /auth/login` | Envia `email` e `senha`. Retorna `token`, `nome` e `perfil`. |
| `GET /auth/setores-publicos` | Retorna ID e nome de setores ativos para o autocadastro. |
| `POST /auth/registro` | Envia `nome`, `email`, `setorId`, `senha`, `confirmarSenha` e, opcionalmente, `telefone`. Cria um Solicitante. |
| `GET /usuarios/solicitantes` | Admin e Atendente. Filtra contas ativas por `busca` (parte do nome ou e-mail) e `setorId`. Retorna `id`, `nome`, `setorId`, `setorNome` e `setorAtivo`. Para abrir OS, ofereça apenas os resultados com `setorAtivo: true`. |
| `GET /equipe` | Admin e Atendente. Lista equipe ativa. Use o perfil `TECNICO` na designação. |
| `GET /usuarios`, `POST /usuarios`, `DELETE /usuarios/{id}` | Admin. Lista usuários, cria contas da equipe e inativa usuários. Para criar, envie `nome`, `email`, `senha`, `perfil` e, opcionalmente, `telefone`. |
| `GET /setores`, `GET /setores/{id}` | Equipe. Listagem aceita `busca` e `incluirInativos`. |
| `GET /equipamentos`, `GET /equipamentos/{id}`, `GET /tipos-equipamento` | Equipe. Equipamentos aceitam `busca`, `setorId` e `incluirInativos`. |
| `POST /setores`, `PUT /setores/{id}`, `DELETE /setores/{id}`, `PATCH /setores/{id}/reativar` | Admin e Atendente. Corpo de criação/edição com `nome`, `telefone`, `email` e `endereco`. |
| `POST /equipamentos`, `PUT /equipamentos/{id}`, `DELETE /equipamentos/{id}`, `PATCH /equipamentos/{id}/reativar` | Admin e Atendente. Corpo de criação/edição com `setorId`, `tipoId`, `marca`, `modelo`, `patrimonio`, `numeroSerie` e `observacoes`. Informe patrimônio ou número de série. |

## Alteração de senha

- `PATCH /usuarios/me/senha` para Solicitante e Admin. Envie `senhaAtual`, `novaSenha` e `confirmarSenha`.
- `PATCH /usuarios/{id}/senha` para Admin e Atendente. O Admin pode redefinir a senha de qualquer conta. O Atendente pode redefinir a de solicitantes. Envie `novaSenha` e `confirmarSenha`. Para alterar a própria senha, o Admin também informa `senhaAtual`.
- Uma mudança bem-sucedida retorna `204` e encerra as sessões anteriores da conta alterada. Peça novo login após a troca da própria senha. A senha deve ter pelo menos oito caracteres, com letra e número, respeitando o limite de 72 bytes do BCrypt.

## Ordens de serviço

### Abertura e consulta

`POST /os` retorna `201`, o corpo da OS e o cabeçalho `Location`.

```json
{
  "categoria": "SEM_REDE",
  "descricaoProblema": "O computador não consegue acessar a rede."
}
```

Categorias `NAO_LIGA`, `LENTO`, `SEM_REDE`, `IMPRESSORA`, `SISTEMA`, `EMAIL` e `OUTRO`. Admin e Atendente incluem `solicitanteId`. Para o Solicitante, a API usa a conta autenticada. Campos opcionais `patrimonioInformado`, `computadorLiga`, `temRede`, `consegueAnyDesk`, `contato` e `melhorHorario`. Respostas rápidas aceitam `SIM`, `NAO` e `NAO_SEI`. O atendimento remoto registra a modalidade, sem coletar dados de acesso remoto.

- `GET /os/{id}` para detalhes. O Solicitante só vê suas OSs e o Técnico só vê as que têm vínculo ativo com ele.
- `GET /os/busca` para lista paginada. Filtros combináveis `id`, `setorId`, `equipamentoId`, `patrimonio`, `numeroSerie`, `tecnicoId`, `status`, `modalidade`, `backupEmAberto`, `atrasadas`, `inicio`, `fim`, `pagina` (base zero) e `tamanho` (1–100). A resposta contém `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`. Históricos por setor e por equipamento usam os mesmos filtros.
- Para o painel de atraso, use `GET /os/busca?atrasadas=true&tamanho=1` e leia `totalElementos`. Cada OS tem também o campo `atrasada`.
- `GET /os/{id}/registros` para a linha do tempo. O Solicitante recebe orientações da equipe (`ORIENTACAO`) e as próprias observações (`OBSERVACAO_SOLICITANTE`). A equipe autorizada também pode consultar `/os/{id}/eventos` e `/os/{id}/tecnicos/historico`. `GET /os/{id}/tecnicos` retorna os IDs dos vínculos ativos.

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

| Método e rota | Campos além de `versao` |
| --- | --- |
| `PATCH /os/{id}/triagem` | `modalidade` (`REMOTO`, `PRESENCIAL` ou `BANCADA`), `prioridade` (`BAIXA`, `MEDIA`, `ALTA` ou `URGENTE`), `tecnicosIds`. Em bancada, `motivoBancada` e `justificativa`. Ao retirar técnico, `motivoRemocao`. |
| `POST /os/{id}/recebimento` | `equipamentoId`, `acessorios` e `backupSolicitado`. Se sim, `escopoBackup` e `midiaTi`. Se não, `cienciaRiscoConfirmada: true`. |
| `POST /os/{id}/registros` | `tipo` (`DIAGNOSTICO`, `ACAO`, `OBSERVACAO`, `ORIENTACAO`) e `texto`. Diagnóstico e ação exigem Admin ou Técnico vinculado. |
| `PATCH /os/{id}/prazo` | `prazoPrevisto` e `justificativa`. |
| `PATCH /os/{id}/status` | `status`. Ao iniciar a análise, `prazoPrevisto` se ainda não definido. Ao aguardar peça, `registroPecaId` e `justificativa`. Ao escalonar para bancada, `motivoBancada` e `justificativa`. Ao finalizar, `resumoSolucao`. Ao reabrir ou cancelar, `justificativa`. Na entrega ou cancelamento após recebimento, `retiradoPor`. |
| `PATCH /os/{id}/backup` | `situacao`. A ordem é `PENDENTE` → `CONCLUIDO` → `RESTAURADO_DEVOLVIDO`. |
| `POST /os/{id}/backup/descarte` | `descarteConfirmado: true` após a eliminação da cópia da mídia. |

O status inicial é `RECEBIDO`. Os demais são `EM_ANALISE`, `AGUARDANDO_PECA`, `FINALIZADO`, `ENTREGUE` e `CANCELADO`. Para recebimento e backup, consulte `GET /os/{id}/recebimento` e `GET /os/{id}/backup`. Admin e Atendente consultam `GET /os/backups/descarte-pendente`.

## Agenda e relatórios

- `GET /agendamentos` aceita `tecnicoId`, `status`, `inicio` e `fim`. O Técnico consulta a própria agenda. Admin e Atendente também usam `POST /agendamentos`, `PUT /agendamentos/{id}` e `PATCH /agendamentos/{id}/status`.
- O corpo para criar ou editar agenda contém `tecnicoId`, `titulo`, `inicio` e `fim` (instantes ISO 8601). Campos opcionais `osId`, `setorId`, `descricao` e `local`. Atualização e alteração de status exigem `versao`. Status `AGENDADO`, `CONCLUIDO` e `CANCELADO`.
- `GET /relatorios/os?inicio=2026-11-01&fim=2026-11-30&setorId=1` para Admin e Atendente. Todos os filtros são opcionais. O período considera a **data de abertura**, com ambas as pontas inclusivas no fuso de São Paulo. Retorna `total`, `atrasadas`, `porStatus`, `porModalidade` e `porSetor`. A situação é a atual no momento da consulta.

## Tratamento de respostas

- `400` para campos ou parâmetros inválidos. Quando houver JSON com `campos`, use cada par `campo` e `mensagem` ao lado do campo correspondente.
- `401` para credenciais inválidas ou sessão expirada/revogada. `403` para ação vedada ao perfil. `404` para recurso indisponível à conta.
- `409` em caso de duplicidade ou versão desatualizada. Para conflito de versão, recarregue o registro e apresente o estado atual antes de tentar novamente.
- `422` para regra de negócio. Exiba `mensagem` quando houver corpo JSON. A interface deve aceitar também respostas sem corpo.
