# Complemento do guia de estudo

Este arquivo reúne as seções finais do guia principal, a partir do item 10.

## 10. Pontos que merecem atenção durante estudo

1. **Frontend não é segurança.** Botões/rotas podem ser escondidos, mas permissões precisam continuar no backend.
2. **Não confundir estado de UI e estado de domínio.** `setState` atualiza a tela; `OrdemServico` e banco são a fonte persistente.
3. **Respeitar os enums exatos.** `RECEBIDO` e `PRESENCIAL` são valores do contrato, mesmo que a interface mostre rótulos em português.
4. **Mandar `versao` nas alterações.** Sem ela, o backend rejeita ou não consegue proteger de concorrência conforme o DTO.
5. **Inativação não é exclusão física.** Setores, usuários e ativos podem preservar o histórico ligado a eles.
6. **Dados opcionais diferem de dados obrigatórios.** Patrimônio é opcional; telefone do chamado/cadastro foi tornado obrigatório nesta sessão.
7. **Cuidado com documentação antiga.** `dashboard-web/README.md` contém notas que eram verdadeiras antes das rotas de edição e das listas de solicitantes atuais. Prefira código e `API-MVP.md` atualizados.
8. **Segredos são configuração.** Banco, JWT e credenciais Meta não devem entrar no frontend nem em commits/documentos compartilhados.

## 11. Roteiro de estudo sugerido

### Primeira leitura (30–45 min)

1. Leia `dashboard-web/src/App.jsx` para ver páginas e perfis.
2. Leia `AuthContext.jsx`, `api/client.js` e `RotaProtegida.jsx` para entender sessão.
3. Leia `NovaOS.jsx` e `CamposChamado.jsx` para entender um formulário completo.
4. Leia `OrdemServicoController.java`, `AberturaOsRequest.java` e `OrdemServicoService.java` acompanhando o mesmo envio.

### Segunda leitura: regras do negócio

1. Leia `OrdemServico.java` e seus enums.
2. Leia `AcessoOsService.java` antes dos outros serviços: ele apresenta as regras comuns de autorização.
3. Leia `TriagemOsService.java` e depois `FluxoOsService.java`.
4. Leia DTOs das rotas que estiver estudando; eles mostram o formato aceito e as validações.
5. Compare persistência JPA em `OrdemServicoRepository` e consultas JDBC em `TriagemOsRepository`.

### Exercício de explicação

Tente explicar em voz alta, usando os caminhos dos arquivos:

- O que acontece entre clicar em Enviar e ver o detalhe da OS?
- Como o sistema sabe que um técnico só pode ver OS vinculada?
- Por que `versao` vai no PATCH?
- Como o serviço distingue patrimônio opcional de telefone obrigatório?
- Em que ponto uma OS presencial pode ser cancelada por seu solicitante?
- Por que esconder um link no Sidebar não basta para proteger uma rota?

### Navegação Git útil

```powershell
# Histórico de commits, do mais recente ao mais antigo
git log --oneline --date=short --pretty=format:"%h %ad %s"

# Histórico de um arquivo específico
git log --follow -- dashboard-web/src/pages/OsDetalhe.jsx

# Arquivos alterados no último commit
git show --stat --oneline HEAD

# Estado atual, incluindo alterações ainda sem commit
git status --short
```

## 12. Índice por assunto (onde procurar)

| Quero entender... | Comece aqui | Continue aqui |
|---|---|---|
| Inicialização do React | `dashboard-web/src/main.jsx` | `dashboard-web/src/App.jsx` |
| Controle de acesso no navegador | `dashboard-web/src/routes/RotaProtegida.jsx` | `dashboard-web/src/components/Sidebar.jsx` |
| JWT no navegador | `dashboard-web/src/context/AuthContext.jsx` | `dashboard-web/src/api/client.js` |
| Abertura de OS | `dashboard-web/src/pages/NovaOS.jsx` | `OrdemServicoController.java` → `AberturaOsRequest.java` → `OrdemServicoService.java` |
| Edição de OS | `dashboard-web/src/pages/OsDetalhe.jsx` | `EdicaoSolicitacaoRequest.java` → `OrdemServicoService.java` |
| Cancelamento/status | `dashboard-web/src/pages/OsDetalhe.jsx` | `FluxoOsController.java` → `FluxoOsService.java` |
| Triagem | `dashboard-web/src/pages/OsDetalhe.jsx` | `OrdemServicoController.java` → `TriagemOsService.java` |
| Acesso de técnico | `dashboard-web/src/pages/OsDetalhe.jsx` | `AcessoOsService.java` → `OrdemServicoRepository.java` |
| Banco de dados | `manutencao-backend/src/main/resources/db/migration/` | entidades em `domain/` e repositories |
| Erros da API | `dashboard-web/src/api/erro.js` | `common/exception/` e handlers de controller |
| Setores | `dashboard-web/src/pages/Setores.jsx` | `setor/controller`, `service`, `repository`, `domain`, `dto` |
| Equipamentos | `dashboard-web/src/pages/Equipamentos.jsx` | `equipamento/controller`, `service`, `repository`, `domain`, `dto` |
| Agenda | `dashboard-web/src/pages/Agenda.jsx` | `agenda/controller`, `service`, `repository`, `domain`, `dto` |
| WhatsApp | `whatsapp/WhatsAppWebhookController.java` | `WhatsAppBotService.java` e `whatsapp/domain/` |
| Aparência | `dashboard-web/src/styles/theme.css` | componentes e páginas |
| PWA/offline | `dashboard-web/public/manifest.webmanifest` | `dashboard-web/public/sw.js`, `offline.html` |

## 13. Limite desta documentação

Este guia é uma explicação de arquitetura e de caminhos de leitura, não uma anotação linha a linha de cada método/getter gerado. Para consulta exata de um parâmetro, regra ou coluna, confira a implementação e o DTO correspondentes. O banco e as regras podem evoluir; atualize este documento junto com mudanças futuras de contrato.

