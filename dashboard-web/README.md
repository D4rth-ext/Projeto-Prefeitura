# Dashboard Web — Sistema de Controle de Manutenção

Frontend em React (Vite) para Administrador, Atendente, Técnico e Solicitante.
Todos os perfis entram em `/login`. O endereço antigo `/acesso-equipe` redireciona para essa tela. WhatsApp está fora do MVP.

O roteiro atualizado de testes integrados, PWA e descarte de backup está em [VALIDACAO-SPRINT3.md](../VALIDACAO-SPRINT3.md).

## Como rodar

```bash
npm install
npm run dev
```

Abre em `http://localhost:5173`. As chamadas a `/api/...` são redirecionadas
via proxy do Vite para `http://localhost:8080` (o backend Spring Boot),
configurado em `vite.config.js`.

## Estrutura

```
src/
  api/            cliente axios + funções por recurso (ordensServico.js)
  context/        AuthContext (login, token, perfil do usuário)
  routes/         RotaProtegida (RF07.2 — restrição por perfil)
  components/     Sidebar, StatusBadge (reutilizáveis)
  pages/          uma página por tela (Login, OrdensServico, NovaOS...)
  styles/         theme.css com as variáveis de cor (verde/branco)
```

## Rodando o backend junto (Spring Boot + PostgreSQL)

O backend do Aluno A já implementa Auth, Setores e Equipamentos (Sprint 2).
Pra rodar:

1. Suba um PostgreSQL local com um banco `manutencao`, usuário `manutencao`,
   senha `dev` (é o que está em `application.properties`). Se tiver Docker:
   ```bash
   docker run -d --name pg-manutencao -e POSTGRES_DB=manutencao \
     -e POSTGRES_USER=manutencao -e POSTGRES_PASSWORD=dev \
     -p 5432:5432 postgres:16
   ```
2. Na pasta do backend: `mvn spring-boot:run` (ele roda o Flyway sozinho e
   cria as tabelas). Sobe em `http://localhost:8080`.
3. Um admin já é criado automaticamente no primeiro start:
   **admin@manutencao.local / admin123**
4. Com o backend de pé, roda o frontend normalmente (`npm run dev`) — o
   proxy do Vite já aponta pra `localhost:8080`.

## Contrato real do backend (`/api/v1`)

- `POST /auth/login` → `{ token, nome, perfil }`
- `POST /auth/registro` → cadastro do Solicitante (nome, email, telefone,
  setorId, senha, confirmarSenha — senha exige 8+ caracteres com letra e número)
- `GET  /auth/setores-publicos` → lista pública (id, nome) pro select do
  autocadastro, sem precisar estar logado
- `GET  /setores?busca=` · `POST /setores` · `PUT /setores/:id` ·
  `DELETE /setores/:id` (inativa) · `PATCH /setores/:id/reativar`
- `GET  /equipamentos?setorId=&busca=` · `POST /equipamentos` ·
  `PUT /equipamentos/:id` · `DELETE /equipamentos/:id` (inativa)
- `GET  /tipos-equipamento` → lista fixa (Computador, Notebook, Monitor,
  Impressora, Nobreak, Switch/Roteador, Telefone IP, Outro)
- `GET/POST/DELETE /usuarios` → só ADMIN, gerencia a equipe (Técnico/Atendente)

Perfis reais: `ADMIN`, `TECNICO`, `ATENDENTE`, `SOLICITANTE` (não
`ADMINISTRADOR`/`CLIENTE` — já corrigido no `AuthContext.jsx`).

## Backend v3 — o que já está integrado

- **Ordem de Serviço** (`/api/v1/os`): lista (já filtrada por perfil), abrir,
  detalhe, triagem (Admin/Atendente), mudança de status com as regras do
  backend, registros de atendimento e histórico — telas `OrdensServico.jsx`,
  `NovaOS.jsx` e `OsDetalhe.jsx`.
- Toda alteração envia a `versao` da OS (controle de concorrência). Se der
  409, é só recarregar a página.
- **Setor da OS vem do cadastro do solicitante** — não se escolhe na abertura.
  Técnico não abre OS; Admin/Atendente abrem em nome de um solicitante.

## O que ainda falta

- **Edição do chamado pelo solicitante — precisa de rota nova no backend.**
  O front já tem o botão "Editar chamado" (`OsDetalhe.jsx`), mas hoje não há
  nenhuma rota que altere os dados de uma OS aberta. Proposta para o Aluno A:
  `PATCH /api/v1/os/{id}/solicitacao`, corpo = `categoria`, `descricaoProblema`,
  `patrimonioInformado`, `computadorLiga`, `temRede`, `consegueAnyDesk`,
  `contato`, `melhorHorario` + `versao` (mesmos validadores da abertura).
  Regras sugeridas: só o solicitante dono da OS (ou a equipe), só com status
  `RECEBIDO` e modalidade `A_DEFINIR` (antes da triagem), registrando o evento
  no histórico e incrementando a versão. Responde com a OS atualizada.
  Enquanto não existir, o botão mostra "ainda não disponível no servidor".
- **Agenda** (`/api/v1/agendamentos`), **busca paginada** (`/os/busca`) e
  **backup / recebimento de equipamento** (`/os/{id}/backup`,
  `/os/{id}/recebimento`) já existem no backend, mas ainda não têm tela.
  Obs.: a transição para "Em análise" na modalidade Bancada exige o
  recebimento + protocolo de backup preenchidos — sem essas telas, esse
  caminho específico dá erro 422.
- **Atendente não consegue listar solicitantes** (`GET /usuarios` é só Admin):
  hoje ele informa o ID do solicitante na mão em "Nova OS". Pedir ao Aluno A
  uma rota de consulta pro Atendente.
- **Edição do próprio perfil** — não existe `PUT /usuarios/me`. A tela
  `Perfil.jsx` salva só localmente.
- **Recuperação de senha** — telas `EsqueciSenha.jsx` e `RedefinirSenha.jsx`
  prontas, chamando `POST /auth/esqueci-senha` e `POST /auth/redefinir-senha`,
  que ainda não existem no backend.

## Exclusões

"Excluir" no sistema é **inativar** (o backend não apaga linhas): o registro
some das listas, mas o histórico das OS continua. Equipamentos: Admin e
Atendente. Usuários (equipe e funcionários): só Admin, e não é possível
excluir a própria conta. Técnico com OS em análise sem outro técnico vinculado
não pode ser excluído (o backend avisa qual OS precisa de outro técnico).
Não há rota de reativar usuário nem de listar equipamentos excluídos no front.
