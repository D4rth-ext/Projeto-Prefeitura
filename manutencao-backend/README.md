# Backend

## Executar localmente

Com PostgreSQL local iniciado e o banco `manutencao` criado, execute na pasta `manutencao-backend`:

```powershell
mvn spring-boot:run
```

O Maven ativa o perfil `local` nesse comando. Ele usa `DB_PASSWORD=dev` e uma chave JWT apenas para desenvolvimento quando não há variáveis de ambiente definidas. Se seu banco usa outros dados, defina `DB_URL`, `DB_USER` e `DB_PASSWORD`.

## Produção

Não use o perfil `local` em produção. Configure `DB_URL`, `DB_USER`, `DB_PASSWORD` e uma `JWT_SECRET` privada, forte e com pelo menos 32 bytes. Execute a aplicação sem o perfil `local`.
