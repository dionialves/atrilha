# Release Notes — Unreleased

## US-005 · Bloqueio de cadastro por idade fora da faixa (#36)

**Tipo:** User Story (Sprint 3, marco M2 — Auth essencial)
**Issue:** [#36](https://github.com/dionialves/atrilha/issues/36)
**Branch:** `feat/36-bloqueio-cadastro-por-idade`
**Data de conclusão:** 2026-05-19

### O que foi feito
- Entregue o validador Jakarta Validation reusável `@EligibleAge(role = ...)` que será consumido pelas US-001/002/003/004 nos DTOs/records de cadastro.
- Implementado `AgeEligibilityChecker` como função pura: recebe `LocalDate birthDate` + `AccountRole`, consulta um `Clock` injetado e devolve `Optional<AgeEligibilityViolation>` cobrindo os três casos M1/M2/M3 do `doc/UX/us-005-spec.md`.
- Declarado o enum `AccountRole { TEEN, GUARDIAN }` (RF-E1-03), o enum `AgeEligibilityViolation { TEEN_TOO_YOUNG, TEEN_TOO_OLD, GUARDIAN_TOO_YOUNG }` (cada constante mapeia para uma chave de mensagem) e o record `AgeEligibilityResult` reservado para o caminho OAuth Google das US-002/004.
- Configurado bean `Clock` em `America/Sao_Paulo` (`AgeEligibilityConfig`) para que "hoje" tenha interpretação determinística independente da timezone do container.
- Adicionado `messages.properties` em UTF-8 com as três mensagens contratuais (M1/M2/M3) copiadas literalmente de `doc/UX/us-005-spec.md` §5 + a chave fallback `validation.age.invalid`.
- Habilitado `spring.messages.basename`, `spring.messages.encoding=UTF-8` e `spring.messages.fallback-to-system-locale=false` em `application.properties` para que o `MessageSource` autoconfigurado leia o arquivo corretamente em pt-BR independente do locale do sistema.
- Cobertura de testes: **54/54 verdes**, sem warnings. TDD seguido (Blocos A/B/C do plano da Issue #36 escritos antes do código de produção). QA expandiu com cenários de borda (ano bissexto, fusos), locale-resilience, coexistência `@NotNull` + `@EligibleAge`, contrato do record `AgeEligibilityResult` e guardrail estrutural `AgeEligibilityNoTraceTest` que blinda o CA-5 enquanto o escopo do pacote `auth/` for apenas o validador.

### Impacto
- **Módulo:** `auth` (validador reusável — primeira entrega funcional do módulo).
- **Migrations Flyway:** **nenhuma** (CA-5 obriga zero rastro persistido).
- **Mudanças no `pom.xml`:** nenhuma — `spring-boot-starter-validation` já estava presente.
- **Arquivos novos (produção):**
  - `src/main/java/dev/zayt/atrilha/auth/AccountRole.java`
  - `src/main/java/dev/zayt/atrilha/auth/AgeEligibilityChecker.java`
  - `src/main/java/dev/zayt/atrilha/auth/AgeEligibilityConfig.java`
  - `src/main/java/dev/zayt/atrilha/auth/AgeEligibilityResult.java`
  - `src/main/java/dev/zayt/atrilha/auth/AgeEligibilityViolation.java`
  - `src/main/java/dev/zayt/atrilha/auth/EligibleAge.java`
  - `src/main/java/dev/zayt/atrilha/auth/EligibleAgeValidator.java`
  - `src/main/resources/messages.properties`
- **Arquivos novos (testes):**
  - `src/test/java/dev/zayt/atrilha/auth/AgeEligibilityCheckerTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/AgeEligibilityCheckerEdgeCasesTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/AgeEligibilityMessagesTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/AgeEligibilityMessagesLocaleTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/AgeEligibilityNoTraceTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/AgeEligibilityResultTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/EligibleAgeValidatorTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/EligibleAgeWithNotNullTest.java`
- **Arquivos editados:** `src/main/resources/application.properties` (somente: 3 linhas `spring.messages.*`).
- **Efeitos colaterais:** nenhum — esta US não monta endpoint HTTP, controller, template, persistência ou e-mail. Consumo do `@EligibleAge` virá nas US-001/002/003/004.

### Como testar
1. A partir do worktree (`/Users/dionia.oliveira/sources/atrilha/.claude/worktrees/agent-a03c82f5b7fa3f27e`), rodar `./mvnw clean test` — 54/54 verdes.
2. Inspeção dirigida (opcional) — rodar apenas a suíte da US: `./mvnw test -Dtest='AgeEligibility*,EligibleAge*'`.
3. Conferir que `messages.properties` está em UTF-8 e que `validation.age.teen.tooYoung`, `validation.age.teen.tooOld`, `validation.age.guardian.tooYoung` correspondem literalmente ao `doc/UX/us-005-spec.md` §5.
4. Validar o guardrail estrutural: `AgeEligibilityNoTraceTest` falhará deliberadamente quando a US-001 introduzir `@Entity`/JPA no pacote `auth/` — esta quebra é intencional (forçar revisão consciente de CA-5 no momento certo). Documentado no Javadoc do teste.

### Gaps visuais / manuais
- **Não há tela nesta US.** O comportamento visual do estado de erro inline (borda `--color-danger-700`, ícone, `aria-invalid`, `role="alert"`, foco no campo) será validado manualmente quando US-001/003 introduzirem o formulário de cadastro que consome `@EligibleAge`. Referência: `doc/UX/us-005-spec.md` §2 e §6.

## US-001 · Cadastro de adolescente por e-mail e senha (#40)

**Tipo:** User Story (Sprint 3, marco M2 — Auth essencial)
**Issue:** [#40](https://github.com/dionialves/atrilha/issues/40)
**Branch:** `feat/40-cadastro-adolescente-email-senha`
**Data de conclusão:** 2026-05-19

### O que foi feito
- Entregue o primeiro fluxo de cadastro funcional do produto: `GET /comecar` (seletor de caminho com dois cards), `GET/POST /cadastro/adolescente` (form único rolável com e-mail, senha, apelido, data e foto opcional), redirect autenticado para `/verificar-email` ao sucesso.
- Implementadas entidades JPA `Account` (polimórfica com `type IN (ADOLESCENT, GUARDIAN)`) e `AdolescentProfile` (1:1 via `@MapsId` na mesma PK), persistidas em Postgres pela migration `V2__accounts_and_adolescent_profiles.sql` com índice único case-insensitive em `LOWER(email) WHERE deleted_at IS NULL` e check de credencial XOR (`password_hash` ⊕ `oauth_provider`).
- `RegisterAdolescentService` orquestra normalização de e-mail (trim + lowercase), detecção de duplicidade pré-persistência, sanitização do apelido via Jsoup `Safelist.none()`, hash BCrypt cost 12 da senha, persistência transacional de account + profile e upload opcional via `AvatarStorage`.
- `FilesystemAvatarStorage` grava em `${app.media.upload-dir}/avatars/{accountId}.{ext}` com validação de MIME (`image/jpeg|png|webp`) e limite de 5 MB. Exposto publicamente via `MediaResourceConfig` em `/media/**`.
- `SecurityConfig` habilita Spring Security com BCrypt cost 12, CSRF por default, `/verificar-email` como rota autenticada, demais rotas públicas (preparação para US-006/007).
- `SessionAuthenticator` estabelece sessão imediatamente após cadastro bem-sucedido via `HttpSessionSecurityContextRepository`, satisfazendo CA-5 da US-001.
- Reutilizada a anotação `@EligibleAge(role = TEEN)` da US-005 no DTO `RegisterAdolescentRequest` e no form `RegisterAdolescentForm` — o bloqueio por idade é detectado a partir do `BindingResult` e renderiza `cadastro/adolescente_bloqueado` com `variant=under-13|over-17` **apenas quando o único erro é a idade** (precondição `getFieldErrorCount() == 1`), preservando o caminho do form para erros compostos (CA-3/CA-4 da US-001 + CA-4 da US-005).
- Primeiro consumo do design system: fragments `templates/components/{button,input,card,brand}.html` criados (apenas os efetivamente consumidos) + bloco `@theme` da chore-ux-002 e tokens de componente da chore-ux-003 §9 colados em `static/css/app.css`.
- Placeholders/stubs: `/verificar-email` exibe template estático (US-006 reescreve); `/cadastro/responsavel` retorna stub "em breve" (US-003 reescreve no Sprint 4).
- **Bug fix em 2ª rodada (descoberto pelo QA):** quando o usuário enviava idade fora da faixa simultaneamente com outros erros (e-mail inválido, senha curta, etc.), o controller renderizava a tela de bloqueio por idade — violando CA-3/CA-4 da US-001 (perda de outros valores) e CA-4 da US-005 (revelação indireta da regra). Codificador adicionou a precondição `getFieldErrorCount() == 1` em `detectAgeBlockVariant` e reativou os 2 testes `@Disabled` que documentavam o bug.
- Cobertura de testes: **126/126 verdes** (69 unit + 57 integration), 0 falhas, 0 skipped, 0 warnings (`failOnWarning=true`). TDD seguido (Blocos A–F do plano da Issue #40 escritos antes do código de produção). QA expandiu com 25 testes adicionais cobrindo limites exatos (senha 8/72, apelido 3/20), bloqueio composto, CSRF inválido (não apenas ausente), MIME spoof, path traversal em `/media/**` (plain e URL-encoded), race condition concorrente de e-mail duplicado e contratos estruturais do form HTML via Jsoup-DOM.

### Impacto
- **Módulos:**
  - `accounts` (entidades, repositórios, service, controllers, storage, sanitizer, configuração de mídia).
  - `auth` (`SecurityConfig`, `SessionAuthenticator`, `AuthenticatedAccount`) — primeira config real de Spring Security do projeto.
- **Migration Flyway:** `V2__accounts_and_adolescent_profiles.sql` (testada por `AccountsMigrationIT` e `FlywayMigrationIT` em Testcontainers Postgres 18-alpine).
- **Dependências novas no `pom.xml`:**
  - `spring-boot-starter-security` (BCrypt, CSRF, filter chain).
  - `spring-boot-starter-mail` (dependência apenas — envio real virá na US-006; sem `spring.mail.*` configurado para que auto-config fique inerte).
  - `org.jsoup:jsoup:1.18.1` (sanitização de apelido).
  - `org.projectlombok:lombok` em escopo `provided` com annotation processor (uso **estritamente limitado** a `@Getter`/`@Setter` em entidades JPA — sem `@Data`/`@Builder`/`@AllArgsConstructor`/`@RequiredArgsConstructor`; ratificado pelo plano da Issue #40 passos 11 e 12; recomenda-se atualizar formalmente o `ADR-011`/`AGENTS.md` em chore separada).
  - `spring-security-test` (test scope) para `csrf()` no MockMvc.
  - `com.h2database:h2` (test scope) — smoke do contexto Spring; ITs reais usam Testcontainers Postgres.
- **Arquivos novos (produção):**
  - `src/main/java/dev/zayt/atrilha/accounts/Account.java`
  - `src/main/java/dev/zayt/atrilha/accounts/AccountRepository.java`
  - `src/main/java/dev/zayt/atrilha/accounts/AdolescentProfile.java`
  - `src/main/java/dev/zayt/atrilha/accounts/AdolescentProfileRepository.java`
  - `src/main/java/dev/zayt/atrilha/accounts/AdolescentRegistrationController.java`
  - `src/main/java/dev/zayt/atrilha/accounts/AvatarStorage.java`
  - `src/main/java/dev/zayt/atrilha/accounts/AvatarTooLargeException.java`
  - `src/main/java/dev/zayt/atrilha/accounts/AvatarUnsupportedTypeException.java`
  - `src/main/java/dev/zayt/atrilha/accounts/FilesystemAvatarStorage.java`
  - `src/main/java/dev/zayt/atrilha/accounts/GuardianRegistrationStubController.java`
  - `src/main/java/dev/zayt/atrilha/accounts/HtmlSanitizer.java`
  - `src/main/java/dev/zayt/atrilha/accounts/MediaResourceConfig.java`
  - `src/main/java/dev/zayt/atrilha/accounts/RegisterAdolescentForm.java`
  - `src/main/java/dev/zayt/atrilha/accounts/RegisterAdolescentRequest.java`
  - `src/main/java/dev/zayt/atrilha/accounts/RegisterAdolescentService.java`
  - `src/main/java/dev/zayt/atrilha/accounts/StartFlowController.java`
  - `src/main/java/dev/zayt/atrilha/accounts/VerifyEmailPlaceholderController.java`
  - `src/main/java/dev/zayt/atrilha/auth/AuthenticatedAccount.java`
  - `src/main/java/dev/zayt/atrilha/auth/SecurityConfig.java`
  - `src/main/java/dev/zayt/atrilha/auth/SessionAuthenticator.java`
  - `src/main/resources/db/migration/V2__accounts_and_adolescent_profiles.sql`
  - `src/main/resources/templates/comecar.html`
  - `src/main/resources/templates/verificar-email.html`
  - `src/main/resources/templates/cadastro/adolescente.html`
  - `src/main/resources/templates/cadastro/adolescente_bloqueado.html`
  - `src/main/resources/templates/cadastro/responsavel_em_breve.html`
  - `src/main/resources/templates/components/brand.html`
  - `src/main/resources/templates/components/button.html`
  - `src/main/resources/templates/components/card.html`
  - `src/main/resources/templates/components/input.html`
- **Arquivos novos (testes):**
  - `src/test/java/dev/zayt/atrilha/accounts/AccountPersistenceIT.java`
  - `src/test/java/dev/zayt/atrilha/accounts/AccountsMigrationIT.java`
  - `src/test/java/dev/zayt/atrilha/accounts/AdolescentRegistrationControllerIT.java`
  - `src/test/java/dev/zayt/atrilha/accounts/AdolescentRegistrationEdgeCasesIT.java`
  - `src/test/java/dev/zayt/atrilha/accounts/FilesystemAvatarStorageTest.java`
  - `src/test/java/dev/zayt/atrilha/accounts/HtmlSanitizerTest.java`
  - `src/test/java/dev/zayt/atrilha/accounts/MediaResourcePathTraversalIT.java`
  - `src/test/java/dev/zayt/atrilha/accounts/RegisterAdolescentServiceIT.java`
  - `src/test/java/dev/zayt/atrilha/accounts/RegistrationContractIT.java`
- **Arquivos editados:**
  - `pom.xml` (dependências novas + Lombok provided + annotation processor).
  - `src/main/resources/application-dev.properties` (`app.media.upload-dir`, multipart limits).
  - `src/main/resources/application-prod.properties` (`MEDIA_UPLOAD_DIR` env, multipart limits).
  - `src/test/resources/application-test.properties` (H2 smoke + media dir tmp).
  - `src/main/resources/static/css/app.css` (bloco `@theme` + tokens de componente + receita do avatar inicial).
  - `src/main/resources/templates/home.html` (CTA "Começar" apontando para `/comecar`).
- **Efeitos colaterais:**
  - Spring Security passa a estar ativo no contexto — qualquer rota nova herda `permitAll()` por enquanto e precisa ser autorizada explicitamente quando ganhar gating (US-007).
  - Migrations agora aplicam `V2` automaticamente no boot (dev/prod) — `ddl-auto=validate` mantido.
  - `AgeEligibilityNoTraceTest` da US-005 continua válido: toda JPA nova ficou em `accounts/`, o guardrail varre apenas `auth/`.

### Como testar
1. A partir do worktree, rodar `./mvnw clean verify` — **BUILD SUCCESS**, 126 testes verdes (69 unit + 57 integration), 0 warnings.
2. Subir o app local: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (com Postgres local em `docker-compose up -d`).
3. Abrir `http://localhost:8080/` — clique em "Começar" leva a `/comecar`; clique em "Sou adolescente" leva ao form.
4. Preencher e-mail, senha (≥8), apelido (3–20), data de nascimento entre 13 e 17 anos atrás, foto opcional → submeter → redireciona para `/verificar-email` autenticado.
5. Casos negativos manuais:
   - Idade < 13 → tela de bloqueio variante `under-13`.
   - Idade ≥ 18 → tela de bloqueio variante `over-17`.
   - Idade fora + e-mail inválido simultâneos → fica no form, **não** mostra bloqueio (CA-3/CA-4).
   - E-mail já cadastrado (case-insensitive) → erro inline `Esse e-mail já tem conta. Quer entrar?`.
   - Upload de arquivo > 5 MB ou não-imagem → upload rejeitado.
6. Conferir migração: `psql -U atrilha -d atrilha -c "\dt"` deve mostrar `accounts` e `adolescent_profiles`; senha persistida começa com `$2[aby]$12$`.

### Gaps visuais / manuais (declarados pelo QA + Revisor)
- **Microcopy ligeiramente divergente do UX spec §4.3.** O label do apelido foi implementado como "Como a gente te chama" em vez de "Apelido". A intenção do Codificador alinha-se com P4 (primeira pessoa do plural), mas o spec pediu "Apelido" + placeholder "ex.: ju". Decisão: aceitar como ajuste pequeno e abrir polimento futuro se o PO/Designer pedirem retorno literal.
- **CSS dos componentes** entregue inline em `app.css` sem build dedicado de Tailwind v4. O CDN está em uso conforme `base.html`. Substituição por build standalone é chore separada já registrada em `AGENTS.md` — não bloqueia.
- **Ilustração editorial** das telas de bloqueio (`adolescente_bloqueado.html` variantes `under-13`/`over-17`) usa placeholder SVG stroke neutro, conforme decisão registrada no spec §7.2 — será substituída quando chore-ux-001 §7 fechar a produção das ilustrações.
- **Defesa em profundidade no upload de foto:** validação atual confia no header `Content-Type`. O teste `postWithSpoofedPdfDeclaredAsJpgExtensionRejectsUpload` confirma que o `Content-Type` é honrado, mas magic bytes não são verificados. Recomendação para chore futura (sugestão: CHORE-SEC-001).
- **Acessibilidade**: as marcações `aria-invalid`, `aria-describedby` e foco visível estão presentes no template; validação manual completa via leitor de tela fica como auditoria de acessibilidade pós-MVP (`doc/UX/06-acessibilidade.md`).

## CHORE · Substituir Tailwind Play CDN por build standalone do Tailwind v4 (#43)

**Tipo:** Chore técnica (Sprint 3 — débito acumulado de identidade visual)
**Issue:** [#43](https://github.com/dionialves/atrilha/issues/43)
**Branch:** `worktree-agent-a3625b303d18ce1be`
**Data de conclusão:** 2026-05-19

### O que foi feito
- Removida a dependência do **Tailwind Play CDN** (`<script src="https://cdn.tailwindcss.com">`) do `layout/base.html`, eliminando o warning `cdn.tailwindcss.com should not be used in production` que era emitido em toda página servida desde a Sprint 1 (chore-003).
- Substituído por um pipeline de **build standalone do Tailwind v4** integrado ao Maven via `frontend-maven-plugin` (`com.github.eirslett:frontend-maven-plugin:1.15.1`): `./mvnw clean package` agora baixa Node `v20.18.0` + npm `10.8.2` localmente em `node/`, instala dependências em `node_modules/` via `npm ci` determinístico a partir de `package-lock.json` e compila `src/main/frontend/css/app.css` para `target/classes/static/css/app.css` (minificado, ~23 KB) — embarcado no jar e servido em `/css/app.css`.
- Movido o bloco `@theme {}` com **todos os 86 tokens de design** (cores primárias/secundárias/neutras/semânticas, tipografia, espaçamento, raios, sombras, motion, z-index, tokens de componente) de `src/main/resources/static/css/app.css` (deletado) para `src/main/frontend/css/app.css`, onde o Tailwind v4 expande a at-rule em build time para custom properties em `:root` — resolvendo a regressão visual da US-001 (var(--color-*) que resolviam para `initial` porque o Play CDN v3 não processa `@theme`).
- CSS literal pós-`@theme` (.btn, .btn-primary, .input-field, .input-group--error, .cadastro-form__*, .card, .brand-*, .avatar-initial, etc.) copiado **verbatim** do antigo `static/css/app.css` — sem alteração de valores, preservando rastreabilidade a `doc/UX/01-design-tokens.md §10` e `doc/UX/02-componentes-base.md §9`.
- `tailwind.config.js` mínimo (apenas `content: ["./src/main/resources/templates/**/*.html"]`) garante que as utilitárias Tailwind referenciadas inline no markup (`min-h-screen flex flex-col bg-white text-slate-900 antialiased container mx-auto px-4 py-6`) continuam sendo geradas no CSS final.
- `Dockerfile` ajustado para `COPY package.json package-lock.json tailwind.config.js ./` antes de `COPY src ./src` — o `npm ci` no estágio de build encontra o lockfile sem invalidar a camada de `dependency:go-offline`.
- `.gitignore` e `.dockerignore` atualizados para ignorar `/node/` e `/node_modules/` (geradas em build time); `package-lock.json` permanece **versionado** (fonte da verdade do `npm ci`).
- README — seção "Tailwind CSS" reescrita como "Build de CSS", documentando que não há passo manual: `./mvnw clean package` orquestra tudo.
- Cobertura de testes: **141/141 verdes** (69 unit + 72 integration), partindo de 132 antes do QA. TDD seguido: 6 testes da seção "Ordem TDD" do plano implementados antes da produção (`StaticAssetsCssIT`: endpoint 200, tokens em `:root`, ausência de `@theme` cru, ausência de `cdn.tailwindcss`, preservação do `<link>` para `/css/app.css`, sobrevivência das classes do design system). QA estendeu com 9 cenários estruturais adicionais em `StaticAssetsCssCoverageIT` (utilitárias inline, variação de rotas públicas via `@ParameterizedTest`, `/verificar-email` autenticada via `@WithMockUser`, cap de tamanho do CSS, contrato de minificação, classes do design system referenciadas além das já cobertas, tokens semânticos consumidos pelo CSS literal). Nenhum teste de cor de pixel, fonte, layout ou microcopy — apenas contrato estrutural do CSS gerado.

### Impacto
- **Toolchain:** primeira introdução de Node no projeto. Isolada em `${project.basedir}/node/` — não afeta Node global do dev. `npm ci` é determinístico a partir do lockfile (1.096 linhas).
- **Build time:** primeira execução baixa ~30 MB de Node + ~80 MB de `node_modules/` (`@tailwindcss/cli@^4.0.0` + `tailwindcss@^4.0.0` + transitivas). Execuções seguintes usam o cache local; CI/CD pode estender o cache em chore futura (sem alteração do workflow nesta task — escopo respeitado).
- **Dependências novas no `pom.xml`:** `frontend-maven-plugin:1.15.1` (build-time apenas; não vira artefato de runtime).
- **Migrations Flyway:** nenhuma. Esta chore é exclusivamente toolchain de frontend.
- **Arquivos novos:**
  - `package.json` (manifesto npm — `tailwindcss@^4.0.0`, `@tailwindcss/cli@^4.0.0`, script `build:css`).
  - `package-lock.json` (lockfile determinístico).
  - `tailwind.config.js` (config mínimo, `content` apontando para templates).
  - `src/main/frontend/css/app.css` (input do Tailwind — bloco `@theme` + CSS literal migrado).
  - `src/test/java/dev/zayt/atrilha/StaticAssetsCssIT.java` (6 testes do Codificador — Ordem TDD).
  - `src/test/java/dev/zayt/atrilha/StaticAssetsCssCoverageIT.java` (9 testes adicionais do QA — cobertura estrutural estendida).
- **Arquivos editados:**
  - `pom.xml` (declaração e execuções do `frontend-maven-plugin`).
  - `Dockerfile` (`COPY package.json package-lock.json tailwind.config.js ./`).
  - `.gitignore` (`/node/`, `/node_modules/`; `.claude/worktrees` removido inadvertidamente — observação registrada na revisão, funcionalmente inerte).
  - `.dockerignore` (`node/`, `node_modules/`).
  - `README.md` (seção "Tailwind CSS" → "Build de CSS").
  - `src/main/resources/templates/layout/base.html` (remoção do `<script src="https://cdn.tailwindcss.com">`).
- **Arquivos removidos:**
  - `src/main/resources/static/css/app.css` (o CSS agora é gerado em `target/classes/static/css/app.css` em build time).
- **Efeitos colaterais:**
  - Servir `/css/app.css` agora depende de `process-resources` ter rodado (`./mvnw test`, `verify`, `package` cobrem isso; `./mvnw -DskipFrontend` *não* é uma opção configurada — se for desejado pular o frontend em rodadas rápidas, é nova chore).
  - Gap visual da US-001 (botões sem coral, inputs sem borda) provocado pelo `@theme` não-expandido **está resolvido**: os tokens agora chegam a `:root` como custom properties reais.

### Como testar
1. A partir do worktree (`/Users/dionia.oliveira/sources/atrilha/.claude/worktrees/agent-a3625b303d18ce1be`), rodar `./mvnw clean verify` — **BUILD SUCCESS**, 141 testes verdes (69 unit + 72 integration), 0 warnings (`failOnWarning=true` mantido).
2. Conferir o CSS gerado: `grep -c '@theme' target/classes/static/css/app.css` deve retornar **0** (expansão Tailwind v4 ocorreu); `grep -oE -- '--color-primary-500:[^;]*' target/classes/static/css/app.css` deve retornar `--color-primary-500:#f25c54`; tamanho ~23 KB.
3. Empacotar e inspecionar o jar: `./mvnw clean package -DskipTests` e `jar tf target/atrilha-0.0.1.jar | grep app.css` deve listar `BOOT-INF/classes/static/css/app.css`.
4. Subir local (`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`), abrir DevTools em `/`, `/comecar`, `/cadastro/adolescente`: console **não emite** mais `cdn.tailwindcss.com should not be used in production`. `GET /css/app.css` em runtime retorna 200 `text/css` com `:root{...}` minificado.
5. Conferir o template servido (`curl -sf http://localhost:8080/comecar | grep -E 'cdn\.tailwindcss|tailwindcss\.com'`) — não deve retornar nada.

### Gaps visuais / manuais (declarados pelo QA + Revisor)
- **Build Docker não foi exercitado** nesta revisão (apenas `./mvnw` na host). O Dockerfile foi modificado para copiar `package*.json` e `tailwind.config.js` antes de `COPY src ./src`, mas validar `docker compose --profile full up --build` em volume PostgreSQL limpo fica como inspeção manual recomendada antes do merge — o pipeline CI/CD da chore-008 cobre o build linux/amd64.
- **Inspeção visual pós-build não foi feita** (não é teste automatizável neste projeto: testes de cor de pixel/microcopy/layout são fora de escopo do QA). Recomenda-se ao humano que, antes do merge, abra `/comecar`, `/cadastro/adolescente` e `/verificar-email` no navegador e confirme paridade com o spec do design system (`doc/UX/02-componentes-base.md`): botão coral `#F25C54`, inputs com borda visível e foco, espaçamento coerente.
- **Cache de CI/CD para `~/.npm` e `node/`** não foi adicionado ao workflow `.github/workflows/*` — escopo da chore foi estritamente Tailwind. Pode virar chore futura (sugestão: CHORE-CI-001).
- **CDNs de HTMX/Alpine.js/Lottie permanecem** em `base.html` por decisão explícita do plano (fora de escopo). Migração para self-hosted via mesmo pipeline npm é candidata a chore futura.
- **`.gitignore` linha 50 de main (`.claude/worktrees`) foi removida inadvertidamente** nesta entrega. Funcionalmente inerte (`.claude/` continua sendo escondido por outro mecanismo via `git status --ignored`), mas é desvio menor do plano. Registrado como observação não-bloqueante.

## US-006 · Verificação de e-mail (#39)

**Tipo:** User Story (Sprint 3, marco M2 — Auth essencial / E1 parte 1)
**Issue:** [#39](https://github.com/dionialves/atrilha/issues/39)
**Branch:** `feat/39-verificacao-email`
**Data de conclusão:** 2026-05-19

### O que foi feito
- Implementado o fluxo completo de verificação de e-mail (RF-E1-07): emissão de token UUID v4 + persistência em `email_verification_token`, e-mail multipart (HTML + texto-plano) via `JavaMailSender` + Thymeleaf, tela `/verificar-email` (autenticada) com reenvio, endpoint público `GET /verify-email?token=...` cobrindo os três outcomes (SUCCESS, ALREADY_USED, EXPIRED_OR_INVALID) numa única view com `th:switch`, banner persistente para usuários não-verificados via `@ControllerAdvice` + fragment Thymeleaf, e ponto de extensão `@RequiresVerifiedEmail` + `HandlerInterceptor` (plantado para US-012/US-014, não aplicado a endpoints reais nesta US).
- **Disparo automático pós-cadastro:** `RegisterAdolescentService` publica `AccountRegisteredEvent` no fim da transação; `AccountRegisteredEventListener` consome com `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Transactional(propagation = REQUIRES_NEW)` — garantindo que o e-mail só sai se o INSERT da conta persistiu. Falha SMTP no listener é catchada com WARN sem token nem corpo (PRD §11.8) e a conta + token permanecem no banco, viabilizando o reenvio manual em `/verificar-email`.
- **Rate-limit em SQL (não in-memory):** o service `EmailVerificationService.resend` consulta `count(*) WHERE created_at > now-1h` e `findFirst by accountId order by createdAt desc` para impor cooldown de 60s e limite de 5/hora por usuário. Decisão arquitetural deliberada: a contagem fica no Postgres, sobrevive a restart e funciona multi-instance — supera a sugestão original do plano (`in-memory` evoluiu para `Redis pós-MVP`); o backend atual já está pronto para escala.
- **Privacidade do contrato externo:** token inválido, expirado e malformado caem todos em `EXPIRED_OR_INVALID` (UX spec §5.3); o limite/hora não é revelado ao usuário (apenas `retryAfterSeconds`). UUID malformado no `?token=` é parseado defensivamente em `try/catch` no controller — retorna a mesma view de "link inválido".
- **Race condition de verificação corrigida em bug-fix da 2ª rodada:** `findByTokenForUpdate(UUID)` com `@Lock(LockModeType.PESSIMISTIC_WRITE)` + `@Query` JPQL explícito serializa duas chamadas concorrentes do mesmo token. A primeira marca `used_at`; a segunda, ao adquirir o lock após o commit, vê `used_at != null` e retorna `ALREADY_USED`. Teste de concorrência `verify_concurrentCallsOnSameToken_onlyOneSucceeds` apertado para `isEqualTo(1L)` confirmando contrato externo determinístico.
- **Banner persistente** em `layout/base.html` renderiza apenas para usuário autenticado com `email_verified_at IS NULL`; `EmailVerificationBannerAdvice` injeta o atributo `showEmailVerificationBanner` levando em conta a URI da requisição (não aparece em `/verificar-email`, `/verificar-email/reenviar` nem `/verify-email`, UX §7.1). Botão dispensar usa Alpine.js + `sessionStorage` (key `atrilha.banner.email.dismissed`) sem dark pattern (P11).
- **`SecurityConfig`:** rota pública `/verify-email` (token é a credencial); rotas autenticadas `/verificar-email` e `/verificar-email/reenviar`; CSRF mantido em todo POST.
- **Mailpit em dev** (`docker-compose.yml`): novo serviço `axllent/mailpit:latest`, SMTP 1025 + UI 8025. `application-dev.properties` aponta `spring.mail.host=localhost`. **GreenMail em test** (escopo `test` no `pom.xml`, `com.icegreen:greenmail-junit5:2.1.0`) usado pelo `JavaMailVerificationSenderIT`. **Prod** parametrizado por `${MAIL_HOST}`/`${MAIL_USERNAME}`/etc. — provider real (SES/Mailgun/Postmark/SendGrid) fica como chore separada antes do M2 (registrado em "Ressalvas" no PR).
- **2 testes da US-005 ajustados** (`EligibleAgeValidatorTest`, `EligibleAgeWithNotNullTest`): adicionado `@SpringBootApplication(exclude = {MailSenderAutoConfiguration, DataSourceAutoConfiguration, HibernateJpaAutoConfiguration, DataJpaRepositoriesAutoConfiguration})` + `@ComponentScan` filtrado ao `AgeEligibilityChecker` no `static class TestApp`. Razão: o novo `EmailVerificationService` no pacote `auth/` arrastava beans JPA/Mail no contexto, quebrando os testes puros do validador. Decisão revisor: ajuste cirúrgico aceito (a) — `auth/` é o pacote correto para verificação de e-mail (faz parte de identidade/autenticação); mover o service para um pacote `verification/` criaria fragmentação artificial. Comportamento testado por essas duas suítes permanece preservado.
- **Guardrail `AgeEligibilityNoTraceTest` inalterado e válido**: a varredura é em `auth/` por `@Entity`/`JpaRepository`/`JavaMailSender`/`@Repository`/`EntityManager`/`@Table`/`CrudRepository`. Classes novas em `auth/` não introduzem nenhum desses marcadores — a entidade `EmailVerificationToken` + repositório ficam em `accounts/`. O contrato CA-5 da US-005 continua honrado.
- **Cobertura de testes:** **212/212 verdes** (86 unit + 126 integration), 0 falhas, 0 skipped, 0 warnings (`failOnWarning=true` mantido). TDD seguido conforme Ordem TDD do plano (Suítes 1–6, 32 testes-âncora antes do código de produção); QA expandiu com 18 cenários adicionais cobrindo bordas exatas do rate-limit (59s/60s, 5º/6º na janela), token expirado por 1s, token de outra conta, concorrência real com `CountDownLatch`, contrato HTTP, idempotência, falha SMTP, config Mailpit em dev. Nenhum teste de layout/microcopy.
- **Rebase em main** (`547f86f`): adaptação cirúrgica do teste `StaticAssetsCssCoverageIT#verifyEmailRouteHasNoTailwindPlayCdnScript` (originalmente da chore-ux-009 #43) — `/verificar-email` deixou de ser placeholder público e passou a autenticada, então o teste agora aceita o redirect 302 e valida que o response (corpo vazio do redirect) não contém `cdn.tailwindcss`/`tailwindcss.com`. Intent original preservada; a cobertura efetiva do `<script>` ausente em `base.html` continua viva nas 3 rotas públicas (`/`, `/comecar`, `/cadastro/adolescente`) e nos testes IT específicos da US-006.

### Impacto
- **Módulos:**
  - `auth` (service + controller + event listener + interceptor + advice + anotação pública + 2 exceções + enum). Primeiro orquestrador real de máquina de estado do épico E1.
  - `accounts` (entidade `EmailVerificationToken` + repositório + interfaces de leitura cross-module `AccountReader`/`AccountProfileLookup` com implementações JPA). A entidade vive aqui por pertencer ao subdomínio "conta"; a orquestração (verify/resend) vive em `auth`.
  - `notifications` (interface `EmailVerificationSender` + implementação `JavaMailEmailVerificationSender` com `TemplateEngine` interno separado do Spring MVC).
- **Migration Flyway:** `V3__email_verification.sql` (testada por `EmailVerificationTokenMigrationIT` + `FlywayMigrationIT` em Testcontainers Postgres 18-alpine). Cria tabela `email_verification_token` (UUID PK + FK CASCADE para `accounts(id)`), índice parcial em `account_id WHERE used_at IS NULL` e índice composto `(account_id, created_at DESC)` para rate-limit.
- **Dependências novas no `pom.xml`:**
  - `com.icegreen:greenmail-junit5:2.1.0` (test scope) — SMTP em memória para testes de envio real.
  - `spring-boot-starter-mail` já entrou na US-001 como dependência inerte; aqui passa a ser configurado em `application-{dev,prod,test}.properties` e exercitado em runtime.
- **Arquivos novos (produção):**
  - `src/main/java/dev/zayt/atrilha/accounts/AccountProfileLookup.java`
  - `src/main/java/dev/zayt/atrilha/accounts/AccountReader.java`
  - `src/main/java/dev/zayt/atrilha/accounts/EmailVerificationToken.java`
  - `src/main/java/dev/zayt/atrilha/accounts/EmailVerificationTokenRepository.java`
  - `src/main/java/dev/zayt/atrilha/accounts/JpaAccountProfileLookup.java`
  - `src/main/java/dev/zayt/atrilha/accounts/JpaAccountReader.java`
  - `src/main/java/dev/zayt/atrilha/auth/AccountRegisteredEvent.java`
  - `src/main/java/dev/zayt/atrilha/auth/AccountRegisteredEventListener.java`
  - `src/main/java/dev/zayt/atrilha/auth/AuthWebMvcConfig.java`
  - `src/main/java/dev/zayt/atrilha/auth/EmailResendRateLimitedException.java`
  - `src/main/java/dev/zayt/atrilha/auth/EmailVerificationBannerAdvice.java`
  - `src/main/java/dev/zayt/atrilha/auth/EmailVerificationController.java`
  - `src/main/java/dev/zayt/atrilha/auth/EmailVerificationService.java`
  - `src/main/java/dev/zayt/atrilha/auth/RequiresVerifiedEmail.java`
  - `src/main/java/dev/zayt/atrilha/auth/RequiresVerifiedEmailInterceptor.java`
  - `src/main/java/dev/zayt/atrilha/auth/VerificationResult.java`
  - `src/main/java/dev/zayt/atrilha/notifications/EmailVerificationSender.java`
  - `src/main/java/dev/zayt/atrilha/notifications/JavaMailEmailVerificationSender.java`
  - `src/main/resources/db/migration/V3__email_verification.sql`
  - `src/main/resources/templates/email/verify-email.html`
  - `src/main/resources/templates/email/verify-email-plain.txt`
  - `src/main/resources/templates/layout/fragments/email-verification-banner.html`
  - `src/main/resources/templates/verify-email-resultado.html`
- **Arquivos novos (testes):**
  - `src/test/java/dev/zayt/atrilha/accounts/AccountTestFactory.java`
  - `src/test/java/dev/zayt/atrilha/accounts/EmailVerificationTokenMigrationIT.java`
  - `src/test/java/dev/zayt/atrilha/accounts/EmailVerificationTokenRepositoryIT.java`
  - `src/test/java/dev/zayt/atrilha/auth/AccountRegisteredEventListenerIT.java`
  - `src/test/java/dev/zayt/atrilha/auth/AccountRegisteredEventListenerSmtpFailureIT.java`
  - `src/test/java/dev/zayt/atrilha/auth/EmailVerificationBannerAdviceTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/EmailVerificationControllerContractIT.java`
  - `src/test/java/dev/zayt/atrilha/auth/EmailVerificationControllerIT.java`
  - `src/test/java/dev/zayt/atrilha/auth/EmailVerificationServiceBoundaryIT.java`
  - `src/test/java/dev/zayt/atrilha/auth/EmailVerificationServiceIT.java`
  - `src/test/java/dev/zayt/atrilha/auth/EmailVerificationServiceRateLimitIT.java`
  - `src/test/java/dev/zayt/atrilha/auth/RequiresVerifiedEmailInterceptorTest.java`
  - `src/test/java/dev/zayt/atrilha/notifications/DevMailpitConfigTest.java`
  - `src/test/java/dev/zayt/atrilha/notifications/JavaMailVerificationSenderIT.java`
  - `src/test/java/dev/zayt/atrilha/notifications/JavaMailVerificationSenderLogTest.java`
  - `src/test/java/dev/zayt/atrilha/notifications/RecordedEmail.java`
  - `src/test/java/dev/zayt/atrilha/notifications/RecordingEmailSender.java`
  - `src/test/java/dev/zayt/atrilha/notifications/RecordingEmailSenderTestConfig.java`
- **Arquivos editados:**
  - `docker-compose.yml` (serviço `mailpit`).
  - `pom.xml` (`greenmail-junit5:2.1.0` em scope test).
  - `src/main/java/dev/zayt/atrilha/accounts/RegisterAdolescentService.java` (publica `AccountRegisteredEvent` no fim da transação).
  - `src/main/java/dev/zayt/atrilha/auth/SecurityConfig.java` (libera `/verify-email`, exige sessão em `/verificar-email` e `/verificar-email/reenviar`).
  - `src/main/resources/application.properties`, `application-dev.properties`, `application-prod.properties` (config `spring.mail.*`, `atrilha.mail.from`, `atrilha.base-url`).
  - `src/main/resources/templates/layout/base.html` (inclusão do fragment `email-verification-banner`).
  - `src/main/resources/templates/verificar-email.html` (substituído o placeholder por tela real com form de reenvio + flash messages).
  - `src/test/resources/application-test.properties` (host mail fake + `management.health.mail.enabled=false`).
  - `src/test/java/dev/zayt/atrilha/accounts/AdolescentRegistrationControllerIT.java`, `AdolescentRegistrationEdgeCasesIT.java`, `RegisterAdolescentServiceIT.java`, `RegistrationContractIT.java` (instalações do `RecordingEmailSenderTestConfig` para isolar o e-mail do fluxo de cadastro nos testes existentes da US-001).
  - `src/test/java/dev/zayt/atrilha/auth/EligibleAgeValidatorTest.java`, `EligibleAgeWithNotNullTest.java` (ajuste cirúrgico: excluem autoconfig JPA/Mail no `TestApp` interno; comportamento testado preservado).
  - `src/test/java/dev/zayt/atrilha/StaticAssetsCssCoverageIT.java` (adaptação ao rebase: `/verificar-email` agora autenticado redireciona; teste passa a aceitar 302 e ainda valida ausência de CDN no response).
- **Arquivos removidos:**
  - `src/main/java/dev/zayt/atrilha/accounts/VerifyEmailPlaceholderController.java` (substituído pelo `EmailVerificationController` real).

### Como testar
1. A partir do worktree (`/Users/dionia.oliveira/sources/atrilha/.claude/worktrees/agent-ad302147b722639dc`), rodar `./mvnw clean verify` — **BUILD SUCCESS**, 212 testes verdes (86 unit + 126 integration), 0 warnings (`failOnWarning=true` mantido).
2. Conferir migração: subir o Postgres (`docker compose up -d postgres mailpit`) e em outro shell `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`. Em outro terminal, `psql -U atrilha -d atrilha -c "\dt"` deve listar `email_verification_token`; `\d email_verification_token` deve mostrar os dois índices (`idx_evt_account_active` parcial, `idx_evt_account_created_at` composto).
3. Cadastro manual e fluxo feliz:
   - Abrir `http://localhost:8084/comecar` → "Sou adolescente" → preencher form válido → submeter.
   - Redireciona para `/verificar-email` mostrando o e-mail destacado e o card de reenvio.
   - Abrir `http://localhost:8025` (Mailpit UI) → confirmar que o e-mail chegou; corpo HTML contém link `?token=...`.
   - Clicar no link → tela "E-mail confirmado!" com CTA "Continuar".
   - Voltar para `/` → **banner não aparece mais** (a conta agora tem `email_verified_at`).
4. Reenvio com cooldown:
   - Cadastrar uma nova conta; clicar "Reenviar link" → flash success "Reenviamos...".
   - Clicar novamente em menos de 60s → flash rate_limited "espera alguns segundos...".
5. Token inválido/expirado:
   - Acessar `http://localhost:8084/verify-email?token=00000000-0000-0000-0000-000000000000` → tela "Esse link não tá mais valendo".
   - Acessar com token malformado (`?token=foo`) → mesma tela (privacidade — não revela se o token existiu).
6. Token já usado:
   - Após verificar uma vez, clicar de novo no mesmo link do e-mail → tela "Esse e-mail já foi confirmado".
7. Banner persistente:
   - Cadastrar conta nova, **sem clicar no link** ainda. Navegar para `/`, `/comecar`, qualquer rota — banner âmbar aparece em todas, com link "Verificar agora" → `/verificar-email`. Banner **não** aparece em `/verificar-email` ou `/verify-email?token=...`.
8. Garantir privacidade dos logs: durante o fluxo acima, o stdout do app **não** deve conter nenhum UUID de token nem corpo de e-mail. Cobertura automática: `JavaMailVerificationSenderLogTest`.

### Gaps visuais / manuais (declarados pelo QA + Revisor)
- **Validação visual das 4 telas + banner + e-mail** em viewport (mobile 360px, tablet 768px, desktop 1280px) fica como inspeção manual antes do merge: tela pendente, sucesso, expirado/inválido, já usado, banner âmbar no topo, e-mail HTML no Mailpit. Referência: `doc/UX/us-006-spec.md`.
- **Provider SMTP de produção não decidido** (SES/Mailgun/Postmark/SendGrid). `application-prod.properties` está parametrizado por env vars (`MAIL_HOST`/`MAIL_USERNAME`/`MAIL_PASSWORD`/`MAIL_FROM`/`MAIL_PORT`); sem essas env vars o boot em prod falha cedo (preferível a falha silenciosa). Recomenda-se chore dedicada antes do M2 (Sprint 6, quando vinculação entra e e-mails passam a ser críticos para o fluxo).
- **`@RequiresVerifiedEmail` plantado mas não aplicado a nenhum endpoint** nesta US. US-012 e US-014 (Sprint 5) anotam endpoints reais de vinculação. Comportamento do interceptor demonstrado por `RequiresVerifiedEmailInterceptorTest` com endpoints dummy.
- **Decisão pós-SUCCESS: sem auto-redirect / sem recriação explícita do `Authentication`.** A UX spec §4 originalmente fala em recriar `Authentication` com authorities atualizadas para evitar novo login. A implementação faz mais simples: o `EmailVerificationBannerAdvice` consulta o `AccountReader` a cada request — quando `email_verified_at` muda no banco, o atributo `unverifiedEmail` passa a `false` automaticamente sem mexer no `SecurityContext`. Custo: 1 lookup por request enquanto o usuário está logado (mitigável por cache no advice em chore futura, se virar dor).
- **Telemetria (US-069) não instrumentada nesta task** — eventos `email_verification_sent`/`clicked`/`resend_requested`/`resend_rate_limited` ficam para US-069 (Sprint 17); os pontos de extensão estão claros no service e no controller.
- **Rate-limit em SQL é a escolha entregue** (não in-memory): atende dor sem precisar de Redis. Não é dívida técnica pendente — é melhoria deliberada sobre a sugestão original do plano.
