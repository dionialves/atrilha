# Release Notes — Unreleased

## chore(chore-009): smoke test em producao — /health responde 200 via HTTPS

**Issue:** [#9](https://github.com/dionialves/atrilha/issues/9)
**Branch:** chore/9-smoke-test-producao-health-https
**Data:** 2026-05-17

### O que foi feito

Script bash `infra/scripts/smoke.sh` criado como artefato reutilizavel de validacao de producao, executavel localmente ou em pipeline CI/CD. Realiza 5 checks encadeados:

1. `GET /health` retorna 200 com `{"status":"UP"}` no corpo.
2. `GET /` retorna 200 com HTML valido.
3. `GET /rota-inexistente-xyz` retorna 404 e renderiza pagina de erro customizada ("Pagina nao encontrada").
4. Cabecalho `Strict-Transport-Security` presente na resposta HTTPS.
5. Certificado TLS com validade superior a 30 dias.

O script aceita URL base como primeiro argumento (default `https://atrilha.app`); relata `PASS`/`FAIL` por check e sai com `exit 0` (Smoke OK) ou `exit 1` (Smoke FAILED).

Workflow `.github/workflows/deploy.yml` atualizado: o curl inline de retry do chore-008 foi substituido pelo step `Smoke producao` que executa `./infra/scripts/smoke.sh https://atrilha.app`. Adicionado `actions/checkout@v4` no job `deploy` para disponibilizar o script no runner.

Secao `chore-009 — Smoke Test Manual` adicionada ao `infra/RUNBOOK.md` com: comando de execucao, pre-requisitos, tabela de criterios esperados, tabela de acoes por check com FAIL e saida esperada em caso de sucesso.

### Desvios documentados

- Variavel `HOST` extraida para o topo do script (clareza e reutilizacao no check TLS).
- `actions/checkout@v4` adicionado ao job `deploy` porque sem ele o script `infra/scripts/smoke.sh` nao estaria disponivel no runner do GitHub Actions.
- Check TLS usa `date -d` (GNU coreutils/Linux). Em macOS, falha com erro de opcao invalida — comportamento esperado e documentado no RUNBOOK. Execucao em CI (Ubuntu) e na VPS (Ubuntu) funciona corretamente.

### Impacto

- Arquivos novos: `infra/scripts/smoke.sh` (executavel, chmod 755).
- Arquivos editados: `.github/workflows/deploy.yml`, `infra/RUNBOOK.md`.
- Nenhuma alteracao em codigo Java, migrations, templates ou `pom.xml`.

### Como testar

Em Linux ou CI (Ubuntu):
```bash
./infra/scripts/smoke.sh https://atrilha.app
```
Em macOS (requer Docker):
```bash
docker run --rm -v "$PWD:/w" --workdir /w alpine sh -c \
  "apk add --quiet bash curl openssl && bash infra/scripts/smoke.sh https://atrilha.app"
```

---

## chore(chore-008): pipeline CI/CD GitHub Actions — build linux/amd64 + SSH deploy para VPS

**Issue:** [#8](https://github.com/dionialves/atrilha/issues/8)
**Branch:** chore/8-pipeline-cicd-github-actions
**Data:** 2026-05-17

### O que foi feito

Dois workflows GitHub Actions criados para fechar o ciclo de deploy automatico do M0:

- `.github/workflows/ci.yml` — dispara em `pull_request` e `push main`; configura Java 21 (Temurin) com cache Maven e executa `./mvnw -B -ntp verify`. Serve como gating de CI para branch protection.
- `.github/workflows/deploy.yml` — dispara em `push main`; bloco `concurrency: deploy-prod` impede dois deploys simultaneos. Job `build-and-push` (permissions: `packages: write`) autentica no GHCR via `GITHUB_TOKEN`, constroi imagem `linux/amd64` com cache BuildKit GHA e publica com tags `latest` e `sha-<short>`. Job `deploy` (needs: build-and-push) conecta via SSH usando `appleboy/ssh-action@v1.2.0` com secrets `SSH_HOST`/`SSH_USER`/`SSH_PORT`/`SSH_PRIVATE_KEY`; na VPS: atualiza `APP_TAG` no `.env`, executa `docker compose pull app && up -d app && image prune -f`. Smoke test pos-deploy: curl com retry 10x/6s em `https://atrilha.app/health` aguardando `{"status":"UP"}`.
- `doc/infra/RUNBOOK.md` — secao `chore-008` adicionada com: tabela de secrets necessarios, geracao do par ed25519 dedicado ao GitHub Actions, instalacao da chave publica na VPS, explicacao do `GITHUB_TOKEN` para GHCR, instrucoes de branch protection (Require status checks: job `verify`).
- `README.md` — badges CI e Deploy adicionados.

### Desvios documentados

- Nomes dos secrets SSH alterados de `VPS_*` (plano) para `SSH_*` conforme contexto do orquestrador; RUNBOOK atualizado com os nomes corretos.
- RUNBOOK em `doc/infra/RUNBOOK.md` (path real do repositorio, em vez de `infra/RUNBOOK.md` citado no plano).

### Resultado de build

`mvn test` — BUILD SUCCESS, 3 testes passados, 0 warnings.

### Como testar

1. Abrir PR contra `main` — workflow `CI` deve disparar e passar (job `verify`).
2. Configurar secrets no GitHub (SSH_HOST, SSH_USER, SSH_PORT, SSH_PRIVATE_KEY) seguindo o RUNBOOK.
3. Fazer merge em `main` — workflow `Deploy` deve: buildar imagem, publicar em `ghcr.io/dionialves/atrilha`, fazer SSH na VPS, atualizar APP_TAG, recriar container `app`, e o smoke test deve retornar `Healthy`.
4. Verificar badge "Deploy" no README aponta para ultima execucao verde.

---

## chore(chore-006): provisionar-vps-nginx-letsencrypt-docker

**Issue:** [#6](https://github.com/dionialves/atrilha/issues/6)
**Branch:** chore/6-provisionar-vps-nginx-letsencrypt-docker
**Data:** 2026-05-17

### O que foi feito

Provisionamento da VPS Zayt documentado e versionado como infraestrutura reproduzivel. Entregues quatro artefatos:

- `infra/RUNBOOK.md` — 12 passos manuais auditaveis para provisionar do zero uma VPS Ubuntu 24.04 com usuario `deploy` nao-root, UFW (22/80/443), Docker CE, Nginx, Let's Encrypt via certbot e diretorio de deploy `/opt/atrilha/`. Inclui secao **Cloudflare pre-requisitos** (DNS-only antes do certbot, proxy ligado apos emissao do certificado, Full strict, sem HSTS duplicado).
- `infra/nginx/atrilha.app.conf` — configuracao Nginx com redirect 80->443, bloco ACME challenge, HSTS (`max-age=31536000; includeSubDomains`), headers de seguranca (X-Content-Type-Options, X-Frame-Options, Referrer-Policy), proxy para `127.0.0.1:8084`, e `/health` com `access_log off`.
- `infra/compose/docker-compose.prod.yml` — stack de producao com `postgres:18-alpine` (porta 5432 nao publicada, rede interna `backend`), servico `app` vinculado a `127.0.0.1:8084:8084`, healthcheck de postgres, dependencia condicional `service_healthy`, `TZ: America/Sao_Paulo`.
- `infra/compose/.env.example` — template de variaveis (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `APP_TAG`) sem valores reais.

`.gitignore` atualizado para ignorar `infra/compose/.env` (credenciais de producao ficam apenas na VPS).

### Impacto

- Novo diretorio `infra/` na raiz do repositorio.
- Sem alteracao de codigo Java, migrations, templates ou `pom.xml`.
- Criterios operacionais (VPS ativa, certificado emitido, UFW configurado) validaveis apenas manualmente pelo operador seguindo o RUNBOOK.

### Como testar

1. Seguir `infra/RUNBOOK.md` passo a passo em VPS Ubuntu 24.04 LTS.
2. Apos Passo 8: `certbot certificates` deve listar certificado valido para `atrilha.app` e `www.atrilha.app`.
3. Apos Passo 9: `certbot renew --dry-run` deve concluir com sucesso.
4. Apos `chore-008` (primeiro deploy): `curl -s https://atrilha.app/health` deve retornar HTTP 200; header `strict-transport-security` deve estar presente.

---

## chore(chore-005): Dockerfile multi-stage + imagem buildada localmente

**Issue:** [#5](https://github.com/dionialves/atrilha/issues/5)
**Branch:** chore/5-dockerfile-imagem-local
**Data:** 2026-05-17

### O que foi feito

Adicionado `Dockerfile` multi-stage com stage `build` usando `eclipse-temurin:21-jdk` + Maven Wrapper para empacotar o jar, e stage `runtime` usando `eclipse-temurin:21-jre-alpine` copiando apenas o artefato final. A imagem roda com usuario nao-root `app` (uid=100), expoe a porta 8084, configura healthcheck via `wget` apontando para `/health` e honra `JAVA_TOOL_OPTIONS` via ENTRYPOINT em shell form.

Criado `.dockerignore` excluindo `target/`, `.git/`, `doc/`, `.idea/`, `.vscode/`, `*.iml`, `.DS_Store`, `README.md` e `docker-compose.yml` do contexto de build.

Adicionado `application-prod.properties` com datasource via variaveis de ambiente (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`), `ddl-auto=validate`, cache Thymeleaf e `forward-headers-strategy=framework`.

Atualizado `docker-compose.yml` com servico `app` no profile `full` (porta 8084:8084, depends_on postgres healthy, variaveis de ambiente injetadas). Uso: `docker compose --profile full up --build`.

Atualizado `README.md` com secao "Build da imagem" cobrindo `docker build`, `docker run` e `docker compose --profile full up --build`.

### Desvios documentados

- Porta alterada de 8080 para 8084: ajuste justificado por mudanca realizada em task anterior (chore-001), aprovada pelo usuario. Todas as referencias (EXPOSE, healthcheck, ports, README, docker-compose) foram atualizadas para 8084.

### Validacao Docker (executada pelo Revisor)

- `docker build -t atrilha:dev .` — BUILD SUCCESS.
- Content size da imagem: 203 MB (dentro do limite de 250 MB). Virtual size reportado por `docker images`: 561 MB (soma de camadas compartilhadas com base; nao representa tamanho real em disco ou banda de download).
- Container sobe com usuario nao-root: `uid=100(app) gid=101(app) groups=101(app)`.
- `docker inspect --format='{{.State.Health.Status}}'` retorna `healthy` apos start-period.
- `curl http://localhost:8084/health` retorna `{"groups":["liveness","readiness"],"status":"UP"}`.

### Resultado de build

`mvn test` — BUILD SUCCESS, 3 testes passados, 0 warnings do compilador.

### Como testar

1. `docker build -t atrilha:dev .` — deve completar sem erro.
2. `docker compose --profile full up --build` — sobe postgres + app; app conecta e `/health` retorna UP.
3. `curl http://localhost:8084/health` — deve retornar `{"status":"UP"}`.
4. `docker exec <container> id` — deve mostrar `uid=100(app)`.
5. `docker inspect --format='{{.State.Health.Status}}' <container>` — deve mostrar `healthy`.

---

## chore(chore-004): endpoint /health publico e paginas de erro 404/5xx

**Issue:** [#4](https://github.com/dionialves/atrilha/issues/4)
**Branch:** chore/4-endpoint-health-pagina-404
**Data:** 2026-05-17

### O que foi feito

Adicionado `spring-boot-starter-actuator` com exposicao exclusiva do endpoint `/health` (sem detalhes, sem auth). Sete propriedades de management garantem que apenas `/health` responde — todos os outros endpoints Actuator ficam desabilitados. Criadas paginas de erro `templates/error/404.html` e `templates/error/5xx.html` estendendo o layout base do chore-003, entregando experiencia visual consistente em rotas inexistentes e erros internos.

### Arquivos introduzidos

- `src/main/resources/templates/error/404.html` — pagina 404 elegante estendendo `layout/base` com link de retorno ao inicio.
- `src/main/resources/templates/error/5xx.html` — pagina de erro 500 com mesma estrutura visual.
- `src/test/java/dev/zayt/atrilha/HealthEndpointIT.java` — IT com `RANDOM_PORT` que verifica `GET /health` retorna HTTP 200 e corpo `{"status":"UP"}`.
- `src/test/java/dev/zayt/atrilha/NotFoundPageTest.java` — IT com `RANDOM_PORT` e `Accept: text/html` que verifica `GET /rota-inexistente` retorna 404 com o texto "Pagina nao encontrada".

### Arquivos modificados

- `pom.xml` — dependencia `spring-boot-starter-actuator`.
- `src/main/resources/application.properties` — 7 propriedades de management: endpoints desabilitados por padrao, health habilitado, exposto em `/health`, `show-details=never`, probes habilitados.

### Desvios documentados

- `TestRestTemplate` foi removido no Spring Boot 4 — substituido por `RestTemplate` + `@LocalServerPort`. Comportamento equivalente.
- `@WebMvcTest` / MockMvc nao dispara `BasicErrorController` para paginas de erro — substituido por `RANDOM_PORT` com `Accept: text/html`. Desvio necessario e justificado.

### Resultado de build

`mvn verify` — BUILD SUCCESS, 5 testes passados (3 unit + 2 IT), 0 warnings do compilador.

### Como testar

1. `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
2. `curl -sS http://localhost:8084/health` — deve retornar `{"status":"UP"}` com HTTP 200.
3. `curl -sS http://localhost:8084/actuator/beans` — deve retornar 404 (endpoint nao exposto).
4. Abrir `http://localhost:8084/rota-inexistente` — deve exibir a pagina 404 com header/footer do layout base.
5. `./mvnw clean verify` — BUILD SUCCESS.

---

## chore(chore-003): Thymeleaf + HTMX + Tailwind + Alpine.js + Lottie no layout base

**Issue:** [#3](https://github.com/dionialves/atrilha/issues/3)
**Branch:** chore/3-thymeleaf-htmx-tailwind-alpine-lottie
**Data:** 2026-05-17

### O que foi feito

Adicionada a camada de apresentacao SSR (Server-Side Rendering) com Thymeleaf e layout reutilizavel via `thymeleaf-layout-dialect`. O layout base centraliza os scripts CDN (HTMX, Alpine.js, Lottie) e o Play CDN do Tailwind (temporario para Sprint 1), de forma que todas as views futuras herdem header, footer e libs automaticamente.

### Arquivos introduzidos

- `src/main/resources/templates/layout/base.html` — layout base com placeholders para header, footer e conteudo; meta CSRF null-safe; CDNs Tailwind/HTMX/Alpine/Lottie.
- `src/main/resources/templates/layout/fragments/header.html` — fragment de header reutilizavel.
- `src/main/resources/templates/layout/fragments/footer.html` — fragment de footer com ano dinamico.
- `src/main/resources/templates/home.html` — view minimal que estende o layout base via `layout:decorate`.
- `src/main/resources/static/css/app.css` — placeholder; tokens e estilos entram na Sprint 2 (ux-002).
- `src/main/java/dev/zayt/atrilha/web/HomeController.java` — controller que serve `GET /` retornando a view `home`.
- `src/main/java/dev/zayt/atrilha/web/package-info.java` — Javadoc documentando o package transversal.
- `src/test/java/dev/zayt/atrilha/web/HomeControllerTest.java` — teste de integracao com SpringBootTest(MOCK) + MockMvcBuilders.webAppContextSetup.

### Arquivos modificados

- `pom.xml` — dependencias `spring-boot-starter-thymeleaf` e `nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect`.
- `src/main/resources/application-dev.properties` — `spring.thymeleaf.cache=false` e `spring.web.resources.cache.period=0`.
- `README.md` — nota sobre uso temporario do Tailwind Play CDN e plano de migracao para Sprint 2.

### Desvios documentados

- `@WebMvcTest` foi removido no Spring Boot 4.0.6. O codificador usou `@SpringBootTest(MOCK) + MockMvcBuilders.webAppContextSetup`, mantendo asercoes equivalentes (status 200, view name, conteudo HTML). Desvio justificado e documentado.

### Resultado de build

`mvn test` — BUILD SUCCESS, 2 testes passados (HomeControllerTest + AtrilhaApplicationTests), 0 warnings do compilador.

### Como testar

1. `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
2. Abrir `http://localhost:8084/` — deve exibir header, "Bem-vindo a atrilha", footer.
3. Inspecionar o HTML fonte — deve conter CDNs Tailwind, HTMX, Alpine.js e Lottie.
4. Alterar o `<title>Inicio</title>` em `home.html` — apos salvar, o browser reflete sem reiniciar (cache desabilitado em dev).

---

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
