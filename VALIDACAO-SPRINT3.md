# Validação do Sprint 3

## Verificações automatizadas

No backend: `mvn test`. Os testes cobrem permissões de consulta de solicitantes, acesso de técnico sem vínculo, conflito de versão e finalização com diagnóstico/resumo/backup. Não precisam do banco local.

No frontend: `npm test` e `npm run build`. Os testes verificam o comportamento do service worker online/offline e a exclusão de dados autenticados do cache.

## Teste integrado com PostgreSQL

1. Inicie o PostgreSQL conforme `manutencao-backend/src/main/resources/application.yml` e execute `mvn spring-boot:run` na pasta do backend.
2. Execute `npm run dev` na pasta `dashboard-web`.
3. Cadastre um solicitante em setor ativo e entre pela tela `/login` para abrir uma OS.
4. Entre como Atendente em `/login` (a mesma tela dos demais perfis). Em Nova OS, confirme a seleção do solicitante por nome e setor. Contas e setores inativos não devem aparecer.
5. Faça triagem remota com um técnico. Como técnico vinculado, inicie análise com prazo, registre Diagnóstico e Ação. Finalize com resumo. Sem diagnóstico, a finalização deve ser bloqueada.
6. Repita em Bancada: selecione equipamento do setor, registre acessórios e escolha backup. Sem backup, confirme ciência de risco. Com backup, informe escopo/mídia e confirme Concluído e Restaurado/devolvido antes de finalizar.
7. Como Atendente, registre entrega com nome de quem retirou. Confira a OS no menu Descarte de backups. Após eliminar efetivamente os dados da mídia, confirme o descarte na OS: ela deve sair da lista de pendentes.
8. Adicione dois técnicos, remova um com motivo e confira o histórico. Tente consultar com técnico não vinculado e com outro solicitante: o acesso deve ser negado.
9. Crie, edite e conclua um agendamento. Como técnico, consulte apenas a própria agenda. Combine filtros de OS e confira a paginação.
10. Abra a mesma OS em duas sessões e tente salvar com versão antiga: a segunda gravação deve ser rejeitada.

Use dados de teste. O roteiro modifica OSs e agenda e não é executado automaticamente contra dados reais.

## PWA

1. Execute `npm run build` e `npm run preview -- --host localhost` no frontend. O service worker só é registrado na versão de produção, não no servidor de desenvolvimento.
2. Abra a URL indicada pelo Vite. Em produção, configure HTTPS, fallback das rotas React para `index.html` e encaminhamento de `/api` ao backend. O preview valida os arquivos da PWA; operações da API dependem dessa configuração.
3. No navegador, confira o manifesto e o service worker em Ferramentas do desenvolvedor > Application. Instale pelo menu do navegador quando disponível.
4. Simule offline e recarregue: deve aparecer a página Sem conexão. Os chamados não ficam disponíveis offline e não há fila de alterações pendentes.
5. Confira em Cache Storage: apenas a página offline, manifesto e ícones devem estar armazenados. Tokens e respostas da API não são armazenados pelo service worker.
6. Teste larguras de 360, 390 e 768 pixels, incluindo Agenda, triagem e recebimento.

## Descarte de mídias

O sistema controla prazo, atraso, responsável e confirmação. A mídia do TI é identificada por texto; não existe acesso do servidor aos seus arquivos. A eliminação efetiva deve ser realizada pelo responsável antes da confirmação. Não é seguro registrar um descarte automático sem eliminar os dados.
