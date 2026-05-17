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

## Frontend

### Tailwind CSS

O projeto usa o **Tailwind Play CDN** temporariamente (chore-003 / Sprint 1) para destravar o desenvolvimento
sem introduzir Node.js ou Tailwind CLI como dependencia de build. Isso e intencional e documentado.

A Sprint 2 (ux-002) substituira o CDN pelo Tailwind standalone CLI ou integrado ao build Maven,
junto com a identidade visual definitiva.