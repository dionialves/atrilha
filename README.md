# atrilha

[![CI](https://github.com/dionialves/atrilha/actions/workflows/ci.yml/badge.svg)](https://github.com/dionialves/atrilha/actions/workflows/ci.yml)
[![Deploy](https://github.com/dionialves/atrilha/actions/workflows/deploy.yml/badge.svg)](https://github.com/dionialves/atrilha/actions/workflows/deploy.yml)

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

## Build da imagem

### Construir a imagem Docker

```bash
docker build -t atrilha:dev .
```

### Rodar o container (com postgres ja rodando via compose)

```bash
docker run --rm -p 8084:8084 -e SPRING_PROFILES_ACTIVE=dev atrilha:dev
```

### Verificar saude da aplicacao

```bash
curl http://localhost:8084/health
```

### Subir stack completa (postgres + app) via Compose

```bash
docker compose --profile full up --build
```

## Frontend

### Tailwind CSS

O projeto usa o **Tailwind Play CDN** temporariamente (chore-003 / Sprint 1) para destravar o desenvolvimento
sem introduzir Node.js ou Tailwind CLI como dependencia de build. Isso e intencional e documentado.

A Sprint 2 (ux-002) substituira o CDN pelo Tailwind standalone CLI ou integrado ao build Maven,
junto com a identidade visual definitiva.