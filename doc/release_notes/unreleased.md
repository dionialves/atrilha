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
