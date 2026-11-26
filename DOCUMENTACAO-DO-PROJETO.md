# Documentação do projeto

> Documentação de leitura para entender a aplicação, acompanhar um chamado desde a tela até o banco e explicar as decisões principais do código. Este guia descreve o estado encontrado no repositório em **13/11/2026**, incluindo as alterações feitas nesta sessão.

## Como usar este guia

Leia primeiro as seções 1 a 4 para entender a arquitetura e o fluxo de uma requisição. Depois escolha uma funcionalidade (chamados, usuários, setores, equipamentos, agenda ou WhatsApp) e acompanhe os arquivos indicados. O arquivo GUIA-DE-ESTUDO-COMPLEMENTAR.docx reúne as seções 10 a 13, com recomendações, roteiro de estudo, índice por assunto e limites da documentação.

Os exemplos são trechos representativos do código. Os caminhos são relativos à raiz do repositório `projeto-praticas`.

## 1. O que o sistema faz

O sistema registra solicitações de manutenção de TI. Um solicitante abre uma Ordem de Serviço (OS); Admin ou Atendente faz a triagem; técnicos vinculados registram diagnóstico e ações; a equipe pode finalizar, devolver equipamentos ou acompanhar backups. Há cadastros de setores, equipamentos, usuários e agenda. O backend também contém integração de abertura de OS pelo WhatsApp.

O projeto tem duas aplicações:

| Parte | Tecnologia | Responsabilidade |
|---|---|---|
| `dashboard-web/` | React, JavaScript, Vite, React Router, Axios | Interface, formulários, navegação, sessão no navegador e chamadas HTTP. |
| `manutencao-backend/` | Java 21, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL | API REST, validação, regras de negócio, autorização, persistência e auditoria. |

Uma ação na interface segue, em geral, este caminho:

```text
Componente React → função em src/api → Axios → Controller Spring
→ Service (regra de negócio) → Repository/JPA/JDBC → PostgreSQL
← DTO de resposta ← Axios ← atualização da tela
```

Exemplo: o botão **Enviar chamado** em `dashboard-web/src/pages/NovaOS.jsx` chama `abrirOrdem`; `dashboard-web/src/api/ordensServico.js` envia `POST /os`; `OrdemServicoController` recebe e valida o JSON; `OrdemServicoService` resolve o solicitante e cria a OS; `OrdemServico` guarda os dados e o repository persiste.

## 2. Mapa do repositório

```text
projeto-praticas/
├── dashboard-web/
│   ├── src/
│   │   ├── api/          cliente HTTP e funções por recurso
│   │   ├── components/   componentes usados em várias telas
│   │   ├── context/      estado global de autenticação
│   │   ├── pages/        páginas e formulários
│   │   ├── routes/       proteção de rotas por perfil
│   │   ├── styles/       tema visual
│   │   └── utils/        rótulos e catálogos
│   ├── public/           ícones, manifesto, service worker e offline
│   ├── tests/            testes do service worker
│   └── vite.config.js    servidor de desenvolvimento e proxy da API
└── manutencao-backend/
    ├── src/main/java/com/manutencao/sistema/
    │   ├── auth/         login, JWT e filtro de autenticação
    │   ├── common/       segurança comum, erros e dados iniciais
    │   ├── os/           ordens de serviço e operações associadas
    │   ├── usuario/      usuários, perfis e solicitações
    │   ├── setor/        setores
    │   ├── equipamento/  equipamentos e tipos
    │   ├── agenda/       agendamentos
    │   └── whatsapp/     webhook e conversa do bot
    └── src/main/resources/
        ├── application.yml
        └── db/migration/ migrações versionadas do Flyway
```

Arquivos de apoio importantes:

- [`dashboard-web/README.md`](dashboard-web/README.md): instruções e visão original do frontend. Algumas notas desse README são históricas; compare com o código atual e com este guia, principalmente sobre edição de chamados e busca de solicitantes.
- [`manutencao-backend/API-MVP.md`](manutencao-backend/API-MVP.md): referência de API, ambiente e contratos.
- [`VALIDACAO-SPRINT3.md`](VALIDACAO-SPRINT3.md): roteiro existente para validação integrada e PWA.
- `dashboard-web/dist/`: arquivos gerados para distribuição. Normalmente são saída do build; o código que se estuda e altera fica em `dashboard-web/src/` e `dashboard-web/public/`.
- `dashboard-web.zip`: arquivo compactado presente na raiz; não é a fonte principal da aplicação.

## 3. Frontend: como a interface se organiza

### 3.1 Inicialização e rotas

`dashboard-web/src/main.jsx` monta a aplicação React no elemento HTML raiz. `dashboard-web/src/App.jsx` define as rotas e envolve as telas autenticadas com `RotaProtegida` e `Layout`.

Trecho representativo de `App.jsx`:

```jsx
<AuthProvider>
  <Routes>
    <Route path="/login" element={<Login />} />
    <Route path="/" element={
      <RotaProtegida>
        <Layout><OrdensServico /></Layout>
      </RotaProtegida>
    } />
    <Route path="/ordens-servico/nova" element={
      <RotaProtegida perfisPermitidos={[
        PERFIS.ADMIN, PERFIS.ATENDENTE, PERFIS.SOLICITANTE
      ]}>
        <Layout><NovaOS /></Layout>
      </RotaProtegida>
    } />
  </Routes>
</AuthProvider>
```

`AuthProvider` disponibiliza login e usuário para as telas. `RotaProtegida` decide se a pessoa pode abrir a rota. Essas verificações melhoram a navegação, mas **não substituem** as verificações de segurança do backend: alguém pode chamar a API fora do navegador.

`Layout` coloca a barra lateral junto ao conteúdo. As rotas protegidas incluem detalhes `/ordens-servico/:id`, perfil, setores, equipamentos, agenda, histórico, backups e a tela de usuários/técnicos.

### 3.2 Login, sessão e perfis

Arquivo central: `dashboard-web/src/context/AuthContext.jsx`.

Os perfis reconhecidos na interface são:

```js
export const PERFIS = {
  ADMIN: 'ADMIN',
  TECNICO: 'TECNICO',
  ATENDENTE: 'ATENDENTE',
  SOLICITANTE: 'SOLICITANTE'
}
```

`login(email, senha)` envia as credenciais à API, recebe token, nome e perfil, grava `token` e `usuario` no `localStorage`, e publica o usuário no contexto React. `logout()` remove esses dois itens e zera o contexto. O contexto também encerra a sessão depois de **15 minutos sem atividade**, reiniciando o temporizador ao detectar interação (ponteiro, teclado, rolagem ou toque).

Exemplo simplificado do login:

```js
const { data } = await api.post('/auth/login', { email, senha })
localStorage.setItem('token', data.token)
localStorage.setItem('usuario', JSON.stringify({
  nome: data.nome, perfil: data.perfil, email
}))
setUsuario(usuarioLogado)
```

O token é enviado pelo interceptor de `dashboard-web/src/api/client.js`. No servidor, o filtro JWT autentica cada chamada protegida. O logout por inatividade é uma regra do navegador; a API também valida token, usuário ativo e permissões em cada rota.

### 3.3 Cliente HTTP e funções de API

`dashboard-web/src/api/client.js` configura Axios, base URL, token e tratamento de falhas comuns. Os arquivos restantes separam as operações por assunto:

| Arquivo | Chamadas agrupadas |
|---|---|
| `src/api/ordensServico.js` | Lista, detalhe, abertura, triagem, status, prazo, registros, eventos, técnicos, edição, recebimento e backup. |
| `src/api/usuarios.js` | Lista/cria/inativa contas e troca o setor do usuário. |
| `src/api/setores.js` | Busca, cria, edita, inativa e reativa setores; consulta setores públicos. |
| `src/api/equipamentos.js` | Consulta e gerencia equipamentos e tipos. |
| `src/api/agenda.js` | Consulta e gerencia agendamentos. |
| `src/api/recuperacaoSenha.js` | Operações de recuperação de senha da interface. |
| `src/api/erro.js` | Extrai uma mensagem legível das respostas de erro. |
| `src/api/ordensServicoFake.js` | Dados falsos auxiliares; não confundir com o cliente da API real. |

Exemplo de função por recurso:

```js
export function abrirOrdem(dados) {
  return api.post('/os', dados).then((r) => r.data)
}
```

A função não implementa a regra da OS; ela apenas traduz a intenção da tela para HTTP. A regra pertence ao serviço do backend.

### 3.4 Componentes reutilizáveis

- `components/Sidebar.jsx`: navegação, links filtrados por perfil, perfil corrente e botão Sair.
- `components/StatusBadge.jsx`: converte status da OS em texto, ícone e classe visual.
- `components/CamposChamado.jsx`: formulário reutilizado ao abrir e editar chamado. Mantém categoria, descrição, telefone, patrimônio e perguntas rápidas num único lugar.
- `components/CampoComLista.jsx`: entrada com sugestões navegável por teclado, usada em catálogos como setor/marca.
- `components/OperacoesOs.jsx`: operações de recebimento e backup para bancada e alteração de prazo, dependendo da aba e do perfil.

Separar os campos do chamado num componente compartilhado evita que abertura e edição aceitem conjuntos diferentes de dados por acidente.

### 3.5 Páginas do frontend

| Página | Arquivo | O que estudar |
|---|---|---|
| Login | `pages/Login.jsx` | Envio das credenciais e mensagens de erro. |
| Cadastro | `pages/Cadastro.jsx` | Cadastro público de solicitante, setor e telefone obrigatório. |
| Lista de OS | `pages/OrdensServico.jsx` | Busca/lista e navegação para detalhes. |
| Nova OS | `pages/NovaOS.jsx` | Seleção do solicitante por pesquisa (gestão), montagem do JSON e envio. |
| Detalhe da OS | `pages/OsDetalhe.jsx` | Abas, transições, triagem, edição, cancelamento, registros e histórico. |
| Histórico | `pages/Historico.jsx` | Consulta histórica de OS. |
| Usuários | `pages/Usuarios.jsx` | Abas de administração/atendimento, técnicos e funcionários; filtro e troca de setor. |
| Setores | `pages/Setores.jsx` | Pesquisa, cadastro, edição e inativação. |
| Equipamentos | `pages/Equipamentos.jsx` | Cadastro, tipo, setor, marca e pesquisa. |
| Agenda | `pages/Agenda.jsx` | Agendamentos de equipe/técnico. |
| Backups | `pages/Backups.jsx` | Pendências de descarte. |
| Perfil | `pages/Perfil.jsx` | Dados locais exibidos e troca de senha conforme endpoints disponíveis. |
| Esqueci senha / Redefinir senha | `pages/EsqueciSenha.jsx`, `pages/RedefinirSenha.jsx` | Formulários de recuperação; confirme no backend qual contrato está implantado antes de depender deles. |

`styles/theme.css` define variáveis de cores e estilos de controles, cartões, badges e layout. `utils/osLabels.js` centraliza rótulos de enums/status; `utils/catalogoSetores.js` mantém catálogo e telefones conhecidos usados por formulários.

### 3.6 Exemplo completo: abertura de chamado na interface

`NovaOS.jsx` mantém os valores num objeto `form`. `campo(nome, valor)` altera uma chave sem sobrescrever o resto:

```js
function campo(nome, valor) {
  setForm((f) => ({ ...f, [nome]: valor }))
}
```

Ao submeter, campos vazios são omitidos; `solicitanteId` é convertido de string para número. Solicitante abre em nome próprio. Admin e Atendente precisam selecionar uma conta ativa. Para isso, a tela pesquisa a lista de solicitantes e envia o ID selecionado, não o texto digitado.

`CamposChamado` exige telefone na interface, e o DTO do backend também o exige. Patrimônio é opcional: um serviço em cabo de rede pode não corresponder a um ativo patrimonial.

### 3.7 Exemplo completo: detalhe e edição

`OsDetalhe.jsx` carrega a OS, registros, eventos e técnicos por ID. O perfil decide quais ações aparecem. A edição reutiliza `CamposChamado`, inclui `versao` no corpo e chama `editarChamado`. O backend autoriza e valida novamente.

O botão de edição aparece enquanto a OS está `RECEBIDO`, com modalidade `A_DEFINIR`, para o solicitante dono, Admin ou Atendente. Depois da triagem, a edição deixa de ser permitida.

O solicitante pode pedir cancelamento de OS presencial ainda em `RECEBIDO`, antes do recebimento do equipamento. A interface pede justificativa e chama a rota de status com `CANCELADO`.

## 4. Backend: camadas e fluxo de execução

### 4.1 Inicialização e configuração

`ManutencaoApplication.java` inicializa Spring Boot. `application.yml` contém configuração de servidor, banco, JWT, Flyway e integração externa; credenciais podem vir de variáveis de ambiente. `pom.xml` declara Java 21 e dependências Maven (web, validação, JPA, segurança, PostgreSQL, Flyway e testes).

`common/config/SecurityConfig.java` configura autenticação e rotas; `CorsConfig.java` configura origens aceitas. `common/seed/AdminSeed.java` trata a criação/configuração inicial do Admin conforme a configuração do projeto.

### 4.2 Controller, DTO, Service, Domain e Repository

| Camada | Papel | Exemplo |
|---|---|---|
| Controller | Mapeia verbo/caminho HTTP e obtém principal autenticado. | `os/controller/OrdemServicoController.java` |
| DTO | Define formato de entrada/saída e valida campos. | `os/dto/AberturaOsRequest.java` |
| Service | Decide regras, permissões, sequência transacional e persistência. | `os/service/FluxoOsService.java` |
| Domain | Entidade e operações coerentes com o estado do negócio. | `os/domain/OrdemServico.java` |
| Repository | Consulta ou altera dados no PostgreSQL. | `os/repository/OrdemServicoRepository.java` |

Por exemplo, `@Valid @RequestBody AberturaOsRequest` faz Spring validar o corpo antes da chamada ao serviço. A validação básica fica no DTO; regras que dependem de estado/perfil ficam no serviço. A entidade encapsula mudanças como `cancelar()` ou `finalizar()`.

### 4.3 DTO e validação

Os DTOs ficam organizados por domínio, em `dto/`. Anotações como `@NotNull`, `@NotBlank`, `@Positive`, `@Size` e `@Email` impedem dados malformados antes de executar a regra de negócio.

Exemplo do contrato de abertura (`AberturaOsRequest`):

```java
@NotNull(message = "A categoria é obrigatória.")
CategoriaOs categoria,
@NotBlank(message = "A descrição do problema é obrigatória.")
String descricaoProblema,
@Positive Long solicitanteId,
@Size(max = 40) String patrimonioInformado,
@NotBlank(message = "O telefone para contato é obrigatório.")
@Size(max = 60) String contato
```

`solicitanteId` é opcional no DTO porque o servidor identifica o próprio solicitante autenticado; para Admin/Atendente ele é exigido pela regra de `OrdemServicoService`. `patrimonioInformado` pode ficar ausente.

### 4.4 Autenticação e autorização

1. `AuthController` recebe login/registro.
2. `JwtService` emite e interpreta tokens.
3. `JwtAuthenticationFilter` lê `Authorization: Bearer ...` e constrói principal autenticado.
4. Controller recebe `Principal` e passa o nome de usuário ao serviço.
5. Serviços buscam o usuário atual e conferem perfil, dono da OS, vínculo técnico e estado.

Perfis reais: `ADMIN`, `ATENDENTE`, `TECNICO`, `SOLICITANTE`. São strings de enum, portanto os valores da API precisam ser escritos exatamente assim.

`AcessoOsService` concentra operações de autorização reutilizadas para uma OS: consultar a OS de acordo com perfil, exigir gestão, exigir técnico vinculado/Admin e comparar versão. Essa centralização evita duplicar as mesmas regras em cada serviço.

Matriz resumida de acesso (a regra específica do endpoint sempre prevalece):

| Perfil | Consulta de OS | Triagem | Chamado em nome de outro | Ações técnicas |
|---|---|---|---|---|
| Admin | Todas | Sim | Sim | Sim |
| Atendente | Todas | Sim | Sim | Limitadas às operações de gestão/atendimento |
| Técnico | Só OS com vínculo ativo | Não | Não | OS vinculadas, conforme ação |
| Solicitante | Só suas próprias OS | Não | Não | Abrir para si, acompanhar, editar antes da triagem e cancelar presencial no período permitido |

### 4.5 Persistência, migrações e concorrência

Entidades JPA ficam em `domain/`; repositories encapsulam consultas. Algumas operações de ligação e auditoria usam `JdbcTemplate` para manipular tabelas auxiliares, por exemplo `TriagemOsRepository`.

Flyway executa os arquivos `db/migration/V1__...sql`, `V2__...sql` e assim por diante em ordem. Cada migração aplica uma alteração de esquema/dados uma vez. Antes de editar uma migração que já foi executada em algum ambiente, crie uma migração nova em vez de reescrever o histórico.

`OrdemServico` usa `@Version`:

```java
@Version
@Column(nullable = false)
private Long versao;
```

O frontend inclui essa versão em atualizações. Se outra pessoa atualizou a OS desde a leitura, o backend rejeita a versão antiga. Esse controle otimista evita que uma gravação silenciosamente apague a alteração concorrente.

### 4.6 Erros

`common/exception/` contém exceções de regra, conflito e recurso não encontrado. `GlobalExceptionHandler` e handlers específicos convertem erros Java em respostas HTTP compreensíveis. No frontend, `mensagemDeErro()` tenta exibir a mensagem retornada pela API e usa um fallback.

Erros comuns: `400` validação, `401` sessão inválida, `403` perfil sem permissão, `404` recurso inexistente ou indisponível para a conta, `409` versão/duplicidade e `422` regra de negócio. Consulte a resposta concreta da API porque algumas ações têm handler especializado.

## 5. Chamados: domínio, regras e endpoints

### 5.1 Entidade `OrdemServico`

`os/domain/OrdemServico.java` representa a OS. Ela mantém referências a setor, solicitante e autor; categoria/descrição; modalidade/prioridade/status; prazos; patrimônio e respostas rápidas; dados de recebimento/backup/entrega; motivo de cancelamento; datas de auditoria e versão.

Os enums relacionados são `CategoriaOs`, `ModalidadeOs`, `PrioridadeOs`, `StatusOs`, `MotivoBancada`, `RespostaRapida`, `TipoRegistroOs` e `SituacaoBackup`.

Algumas transições da entidade:

```java
public void cancelar(String motivo, String responsavel) {
    this.motivoCancelamento = motivo.trim();
    this.canceladoEm = Instant.now();
    if (recebidoEm != null) registrarDevolucao(responsavel);
    alterarStatus(StatusOs.CANCELADO);
}
```

O serviço decide **quem** pode chamar esse método e sob quais condições; a entidade atualiza os dados relacionados à mudança.

### 5.2 Serviços de OS

| Service | Responsabilidade |
|---|---|
| `OrdemServicoService` | Abertura, listagem/detalhe por perfil e edição da solicitação antes da triagem. |
| `TriagemOsService` | Escolhe modalidade/prioridade, designa e remove técnicos, registra eventos de triagem. |
| `FluxoOsService` | Transições de status, prazo, início, finalização, entrega, cancelamento e escalonamento. |
| `RegistroOsService` | Registros de diagnóstico, ação, orientação e observação. |
| `BuscaOsService` | Pesquisa e paginação de OS conforme filtros. |
| `RecebimentoOsService` | Recepção de ativo para atendimento de bancada. |
| `BackupOsService` | Protocolo, progresso e descarte de backup. |
| `RelatorioOsService` | Agregações para relatórios. |

Controllers associados: `OrdemServicoController`, `FluxoOsController`, `RegistroOsController`, `BuscaOsController`, `RecebimentoOsController`, `BackupOsController` e `RelatorioOsController`.

### 5.3 Ciclo do chamado

Fluxo conceitual:

```text
ABERTURA (RECEBIDO, A_DEFINIR)
  → TRIAGEM: REMOTO, PRESENCIAL ou BANCADA + prioridade + técnicos
  → atendimento / registros / prazo
  → AGUARDANDO_PECA ↔ EM_ANALISE
  → FINALIZADO
  → ENTREGUE (quando houve equipamento de bancada)

Chamado ainda aberto → CANCELADO se as regras autorizarem
```

Status e modalidade são conceitos diferentes. Status diz em que etapa está o trabalho. Modalidade diz como a equipe atenderá: `A_DEFINIR`, `REMOTO`, `PRESENCIAL` ou `BANCADA`.

`TriagemOsService` valida que técnico existe e está ativo, não aceita IDs repetidos e exige motivo ao remover técnico. `FluxoOsService` restringe cada transição. Por exemplo, iniciar atendimento depende de modalidade, prioridade e técnico ativo; bancada ainda depende de recebimento e protocolo de backup.

### 5.4 Contratos HTTP principais

Prefixo: `/api/v1`. As rotas protegidas usam cabeçalho `Authorization: Bearer <token>`.

| Método | Rota | Uso |
|---|---|---|
| `POST` | `/auth/login` | Iniciar sessão. |
| `POST` | `/auth/registro` | Cadastro de solicitante. Telefone obrigatório no código atual. |
| `GET` | `/auth/setores-publicos` | Setores ativos para autocadastro. |
| `GET` | `/os` | Lista filtrada conforme perfil. |
| `POST` | `/os` | Abre OS. Admin/Atendente informam `solicitanteId`; solicitante abre para si. |
| `GET` | `/os/{id}` | Detalhe com autorização por perfil/pertencimento. |
| `PATCH` | `/os/{id}/solicitacao` | Corrige a solicitação antes da triagem, com `versao`. |
| `PATCH` | `/os/{id}/triagem` | Modalidade, prioridade e técnicos. |
| `PATCH` | `/os/{id}/status` | Transição de status com campos e versão exigidos. |
| `PATCH` | `/os/{id}/prazo` | Atualiza prazo e justificativa. |
| `POST/GET` | `/os/{id}/registros` | Cria/lê linha do tempo de registros. |
| `GET` | `/os/{id}/eventos` | Histórico/auditoria de mudanças. |
| `GET` | `/os/{id}/tecnicos` | IDs dos técnicos ativos. |
| `GET` | `/os/busca` | Busca paginada e filtros. |
| `POST/GET` | `/os/{id}/recebimento` | Registra/consulta recebimento de ativo. |
| `GET/PATCH/POST` | `/os/{id}/backup...` | Consulta, atualiza backup e registra descarte. |
| `GET` | `/usuarios/solicitantes` e `/solicitantes` | Listas para gestão, conforme escopo do endpoint. |
| `GET` | `/equipe` | Equipe ativa para triagem. |
| `GET/POST/PUT/DELETE` | `/setores...` | Cadastro e gerenciamento de setores. Excluir significa inativar. |
| `GET/POST/PUT/DELETE` | `/equipamentos...` | Ativos patrimoniais e tipos. Excluir significa inativar. |
| `GET/POST/PUT/PATCH` | `/agendamentos...` | Agenda da equipe. |
| `GET` | `/relatorios/os` | Agregados de OS. |
| `GET/POST` | `/whatsapp/webhook` | Verificação da Meta e entrada de mensagens. |

O contrato completo de parâmetros e transições está em `manutencao-backend/API-MVP.md` e nos DTOs/controllers. Quando houver dúvida, o controller e o DTO atuais são a fonte do comportamento executado.

### 5.5 Exemplo: edição e cancelamento adicionados nesta sessão

Edição usa `EdicaoSolicitacaoRequest`, que recebe os campos editáveis e versão. O Controller encaminha a chamada ao service. O service confirma identidade, autorização, estado `RECEBIDO`, modalidade ainda não definida e ausência de técnicos ativos antes de alterar a entidade. Isso impede correção depois que a triagem encaminhou o chamado.

Cancelamento de solicitante segue `PATCH /os/{id}/status` com:

```json
{
  "status": "CANCELADO",
  "justificativa": "O problema foi resolvido antes da visita.",
  "versao": 2
}
```

`FluxoOsService.cancelar()` permite esse caso apenas para o dono da OS, em modalidade `PRESENCIAL`, status `RECEBIDO`, sem equipamento recebido. A equipe continua sujeita às permissões de cancelamento que já existiam.

## 6. Outros domínios do backend

### 6.1 Usuários e autenticação

`usuario/domain/Usuario.java` guarda identidade, e-mail, hash de senha, telefone, perfil, setor, situação ativa e revisão de senha. Nunca se armazena a senha em texto simples. `Perfil.java` enumera os quatro perfis.

`UsuarioService` cadastra usuários e solicitantes, lista resultados, troca setor, redefine senha e inativa conta. `PoliticaSenha` valida a regra de senha. `UsuarioController` trata gerenciamento e rotas de senha; `SolicitanteController` e `EquipeController` fornecem listas específicas. `UsuarioRepository` inclui consultas customizadas, como buscar solicitante pelo telefone para WhatsApp.

### 6.2 Setores

`setor/domain/Setor.java`, `SetorService`, `SetorRepository` e `SetorController` compõem o CRUD. O setor liga solicitante e ativos. `DELETE` normalmente marca inativo, preservando relações/histórico. `SetorRequest` e `SetorResponse` separam entrada de saída.

### 6.3 Equipamentos

`equipamento/domain/Equipamento.java` representa o ativo ligado ao setor; `TipoEquipamento.java` define catálogo. Services validam setor/tipo e regras de patrimônio/série; controllers expõem consulta e manutenção. `EquipamentoResponse` inclui resumos de setor/tipo e dados agregados.

### 6.4 Agenda

`agenda/domain/Agendamento.java`, `StatusAgendamento.java`, `AgendamentoService`, repository, DTOs e controller formam o módulo de agenda. Admin/Atendente gerenciam agendamentos; técnico consulta a própria agenda conforme a regra do service. Alterações usam versão para reduzir sobrescrita concorrente.

### 6.5 WhatsApp

`WhatsAppWebhookController` valida o webhook e recebe as mensagens. `WhatsAppBotService` controla etapas da conversa: início, categoria, descrição, confirmação e abertura. `ConversaWhatsApp` persiste andamento; `MensagemWhatsAppProcessada` permite evitar processar duas vezes a mesma mensagem. `WhatsAppProperties` lê configuração e `WhatsAppPropertiesBridge` chama a Cloud API.

O telefone recebido é associado a um solicitante ativo. Se não houver correspondência única, o bot não escolhe uma conta arbitrariamente. `V8__whatsapp_bot.sql` prepara tabelas do bot. Segredos e tokens devem ficar em configuração de ambiente, nunca em código/documentação pública.

## 7. Fluxos didáticos para explicar o código

### Fluxo A — solicitante abre chamado

1. A pessoa entra em `Login.jsx`; `AuthContext.login()` chama `POST /auth/login`.
2. `client.js` guarda token no navegador; `/` passa pelo `RotaProtegida`.
3. `NovaOS.jsx` mostra nome do solicitante da sessão; perfil de gestão pesquisa e seleciona outro solicitante.
4. `CamposChamado.jsx` valida categoria, descrição e telefone; patrimônio não é obrigatório.
5. `abrirOrdem()` manda JSON a `POST /api/v1/os`.
6. `OrdemServicoController` valida `AberturaOsRequest`.
7. `OrdemServicoService.abrir()` impede técnico, resolve solicitante, checa setor ativo, cria `OrdemServico` e salva.
8. A resposta traz `id`; a página navega para `/ordens-servico/{id}`.

### Fluxo B — gestão tria e designa técnicos

1. `OsDetalhe.jsx` consulta `listarEquipe()` e filtra `TECNICO`.
2. Admin/Atendente escolhem modalidade, prioridade e lista de IDs.
3. `triarOrdem()` envia PATCH com `versao`.
4. `TriagemOsService` valida perfil, estado, modalidade, técnicos ativos e motivos.
5. Entidade recebe a triagem; `TriagemOsRepository` registra vínculos e eventos; versão avança.

### Fluxo C — técnico finaliza

1. Técnico autorizado consulta detalhe e registros.
2. `RegistroOsService` valida vínculo e grava diagnóstico.
3. `FluxoOsService.finalizar()` exige técnico/admin autorizado, resumo e pelo menos um diagnóstico; se bancada, confere backup.
4. OS vira `FINALIZADO`; gestão registra entrega quando aplicável.

### Fluxo D — resolução de conflito

1. Duas sessões leem OS com `versao = 5`.
2. Primeira atualiza e grava; JPA incrementa a versão.
3. Segunda envia versão antiga 5.
4. `AcessoOsService.conferirVersao()` identifica divergência e devolve conflito.
5. Interface deve recarregar antes de tentar novamente.

## 8. PWA e arquivos estáticos

`public/manifest.webmanifest` descreve nome e ícones instaláveis. `public/sw.js` controla cache/offline; `public/offline.html` informa indisponibilidade. Ícones e imagens ficam em `public/`. O projeto mantém cópias geradas em `dist/`; alterações devem ser feitas na fonte (`public/` ou `src/`), não manualmente no build.

A documentação de sprint especifica que a tela offline não significa que OS está disponível offline nem que alterações são enfileiradas. A API continua necessária para ler/gravar chamados.

## 9. Modificações feitas em 13/11/2026

As alterações abaixo foram feitas nesta conversa e estavam **sem commit** no momento em que este guia foi escrito. A data é a data da sessão fornecida pelo projeto; Git ainda não contém hash/data de commit para elas.

| Mudança | Arquivos principais | Como funciona |
|---|---|---|
| Editar chamado antes da triagem | `dashboard-web/src/pages/OsDetalhe.jsx`; `dashboard-web/src/api/ordensServico.js`; backend `OrdemServicoController`, `OrdemServicoService`, `OrdemServico`, novo `EdicaoSolicitacaoRequest` | PATCH `/os/{id}/solicitacao`, com versão; disponível ao dono/Admin/Atendente antes da triagem. |
| Cancelar chamado presencial antes da visita | `dashboard-web/src/pages/OsDetalhe.jsx`; backend `FluxoOsService` | Solicitante informa motivo; backend só permite para o dono, presencial, status recebido e sem recebimento de equipamento. |
| Pesquisa de solicitante na abertura | `dashboard-web/src/pages/NovaOS.jsx` | Busca nome/setor e guarda o ID da opção escolhida. |
| Tela filtrável de técnicos | `dashboard-web/src/pages/Usuarios.jsx` | Aba de Técnicos e pesquisa por nome, e-mail ou setor. |
| Logoff por inatividade | `dashboard-web/src/context/AuthContext.jsx` | Encerra sessão depois de 15 min sem interação e redireciona ao login. |
| Telefone obrigatório | `dashboard-web/src/components/CamposChamado.jsx`, `dashboard-web/src/pages/Cadastro.jsx`; DTOs `AberturaOsRequest` e `RegistroSolicitanteRequest` | Validação no navegador e no backend. |
| Patrimônio opcional | `CamposChamado.jsx`, contrato de abertura/entidade existentes | Formulário explica que pode ficar em branco, inclusive para troca de cabo de rede. |

Na inspeção do Git, os commits visíveis antes dessas alterações eram:

| Data | Hash | Mensagem |
|---|---|---|
| 09/11/2026 | `4c59e18` | Atualiza funcionalidades do sistema de manutenção |
| 05/11/2026 | `9411d96` | Adiciona histórico, catálogos e troca de setor |
| 28/10/2026 | `86726fa` | Versão inicial do sistema de manutenção M |

Essas datas são datas de commit, não necessariamente o dia exato em que cada recurso começou a ser desenvolvido. Para atribuir datas mais antigas a funcionalidades específicas, é preciso consultar `git log -- <arquivo>` e o diff de cada commit.

