# Release Notes — Unreleased

## chore(chore-002): Baseline Flyway + PostgreSQL 18 local via Docker Compose

**Issue:** [#2](https://github.com/dionialves/atrilha/issues/2)
**Branch:** chore/2-baseline-flyway-postgresql
**Data:** 2026-05-17

### O que foi feito

Adicionada a infraestrutura de persistência que serve de fundação para todas as features de banco do projeto Atrilha (Sprint 1, chore-002).

### Arquivos introduzidos

- `docker-compose.yml` — serviço `postgres:18-alpine` com volume nomeado, porta 5432 e healthcheck via `pg_isready`.
- `src/main/resources/application-dev.properties` — datasource apontando para PostgreSQL local, `ddl-auto=validate`, Flyway habilitado.
- `src/main/resources/db/migration/V1__baseline.sql` — migration sentinela que cria `schema_baseline` e insere 1 linha para validar o pipeline Flyway de ponta a ponta.
- `src/test/resources/application-test.properties` — exclui autoconfig de DataSource, JPA e Flyway no perfil test (pacotes Spring Boot 4).
- `src/test/java/dev/zayt/atrilha/FlywayMigrationIT.java` — teste de integração com Testcontainers: sobe Postgres efêmero, executa `Flyway.migrate()` e valida `schema_baseline` via JDBC. Guarded por `assumeTrue(DockerClientFactory.instance().isDockerAvailable())`.

### Arquivos modificados

- `pom.xml` — dependências `spring-boot-starter-data-jpa`, `flyway-core`, `flyway-database-postgresql`, `postgresql` (runtime), `testcontainers:postgresql` e `testcontainers:junit-jupiter` (1.21.3 inline, fora do BOM SB4); plugin `maven-failsafe-plugin` para rodar ITs.
- `src/test/java/dev/zayt/atrilha/AtrilhaApplicationTests.java` — adicionado `@ActiveProfiles("test")`.
- `.gitignore` — adicionadas entradas `*.log` e `data/`.
- `README.md` — seção "Dev local" com instruções de `docker compose up` e `spring-boot:run -Pdev`.

### Desvios documentados

- Nomes de pacotes de autoconfigure atualizados para Spring Boot 4 (`boot.jdbc.autoconfigure`, `boot.hibernate.autoconfigure`, `boot.flyway.autoconfigure`). Necessário — Spring Boot 3 usa pacotes diferentes.
- Testcontainers 1.21.3 declarado com versão inline: o BOM do Spring Boot 4.0.6 não inclui Testcontainers no gerenciamento de versões. Versão explícita é a abordagem correta neste contexto.
- `maven-failsafe-plugin` adicionado para que `FlywayMigrationIT` (sufixo `IT`) execute na fase `integration-test`.

### Resultado de build

`./mvnw clean verify` — BUILD SUCCESS, 2 testes passados (1 unit + 1 IT com Testcontainers), 0 warnings do compilador.

### Como testar

1. `docker compose up -d postgres` — aguardar healthcheck verde.
2. `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` — Flyway aplica `V1__baseline.sql`; verificar log `Successfully applied 1 migration`.
3. Segunda execução: Flyway não reaplica; `flyway_schema_history` contém `V1__baseline` com `success=true`.
4. `./mvnw clean verify` — deve passar sem Docker obrigatório (IT pula com `assumeTrue` se Docker não disponível).

## chore(chore-001): Esqueleto Spring Boot 4.0.6 + Java 21

**Issue:** [#1](https://github.com/dionialves/atrilha/issues/1)
**Branch:** chore/1-esqueleto-spring-boot
**Data:** 2026-05-17

### O que foi feito

Criação do esqueleto Maven/Spring Boot que serve de fundação técnica para todo o projeto Atrilha (Sprint 1, chore-001).

### Arquivos introduzidos

- `pom.xml` — parent `spring-boot-starter-parent:4.0.6`, Java 21, dependências mínimas (web, validation, test), flags `-Xlint:all -Werror`.
- `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` — Maven Wrapper 3.9.9 reproduzível.
- `src/main/java/dev/zayt/atrilha/AtrilhaApplication.java` — entry-point da aplicação.
- `src/main/java/dev/zayt/atrilha/{auth,accounts,content,progress,notifications,admin}/package-info.java` — 6 módulos vazios com Javadoc de fronteira conforme PRD §9.3.
- `src/main/resources/application.properties` — configuração mínima (nome da app + porta 8084).
- `src/test/java/dev/zayt/atrilha/AtrilhaApplicationTests.java` — teste `contextLoads`.
- `.tool-versions` — versão Java fixada para asdf (`openjdk-21`).

### Arquivos modificados

- `.gitignore` — adicionadas entradas para `target/`, `.idea/`, `*.iml`, `.vscode/`, `.DS_Store`, `HELP.md`, `!.mvn/wrapper/maven-wrapper.jar`, `.worktrees/`.

### Resultado de build

`./mvnw clean verify` — BUILD SUCCESS, 1 teste passado, 0 warnings do compilador.
