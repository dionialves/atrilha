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

### Build de CSS

O CSS (incluindo o `@theme` Tailwind v4 com os tokens do design system) e compilado em build time
pelo `frontend-maven-plugin`. Nao ha passo manual: `./mvnw clean package` baixa Node localmente
em `node/`, instala dependencias em `node_modules/` (via `npm ci` deterministico a partir do
`package-lock.json`) e gera `target/classes/static/css/app.css` antes de empacotar o jar.

Fonte do CSS: `src/main/frontend/css/app.css`. Saida: `target/classes/static/css/app.css`
(servida como `/css/app.css` em runtime pelo `ResourceHandler` do Spring Boot).