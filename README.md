# atrilha

## Dev local

### Subir banco PostgreSQL

```bash
docker compose up -d postgres
```

### Rodar a aplicacao com perfil dev

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O Flyway aplica `V1__baseline.sql` no primeiro boot em banco vazio, criando a tabela `schema_baseline`.
Essa migration e sentinela e sera substituida quando a primeira entidade real entrar (Sprint 3+).