# Repository Guidelines

## Project Context

**atrilha** is a PWA that turns the official Adventist Sabbath School Junior lesson into a gamified daily trail for teens (13–17). Greenfield repo — only `README.md` and `.gitignore` are tracked at `c7e1700` (Initial commit). Product spec lives in `doc/PRD.md` (pt-BR). Documentation and product copy are pt-BR; code identifiers stay in English.

## Workflow — Canonical Source

**`doc/workflow.md` é a fonte canônica de todo o ciclo de desenvolvimento e da atualização de documentação. Leitura obrigatória antes de qualquer mudança.** Define SemVer, GitHub Issues como fonte da verdade de tasks, ciclo DISCOVER → CREATE → EXECUTE → COMPLETE → PUBLISH, labels, branches, Definition of Done, templates de handoff e hard rules.

Pipeline (não pular etapas):

```
PO → CTO/Arquiteto ⇄ Designer → Codificador → QA → Revisor → Humano (merge)
```

- `po` refina demanda de produto em `doc/Requisitos/UserStory.md`; demanda técnica vai direto ao Arquiteto.
- `arquiteto` cria GitHub Issue com plano, critérios e labels; invoca `designer` em US visual.
- `designer` produz `doc/UX/<CODE>-spec.md`.
- `codificador` cria branch `<tipo>/<numero>-<slug>` e edita só `src/**`, `pom.xml`, templates, static, properties — sem push, sem PR, sem tocar `doc/**`.
- `qa` expande apenas `src/test/**` (comportamento, não texto literal de UI).
- `revisor`, se APROVADO: squash → atualiza `doc/changelog.md` + `doc/release_notes/unreleased.md` → push → `gh pr create` com `Closes #<N>`. Merge é sempre do humano.

**Sem Issue no GitHub → sem mudança de código.**

## Project Structure & Module Organization

Module boundaries fixadas em `doc/PRD.md` §9.3: `auth`, `accounts`, `content`, `progress`, `notifications`, `admin`. Documentação (workflow.md §1.2):

```
doc/
├── PRD.md, workflow.md          # Fonte da verdade — só humano edita
├── Requisitos/UserStory.md      # PO
├── UX/<CODE>-spec.md            # Designer
├── changelog.md                 # Revisor
└── release_notes/{unreleased,vX.Y.Z}.md   # Revisor
```

Testes em `src/test/**`.

## Stack (ADR-011 — locked)

Java 21 (LTS) + Spring Boot 4.0.6 (Spring Framework 7.0, Hibernate 7.1, Flyway 11.11), Maven, Spring Security (OAuth Google + email/senha BCrypt), PostgreSQL 18, Spring Data JPA, Spring Mail. View: Thymeleaf + HTMX + Tailwind + Alpine.js + Lottie + PWA. **Não introduzir** React/Next, Redis, RabbitMQ, microservices ou CDNs pagos — rejeitados em PRD §9.4.

## Build, Test, and Development Commands

Build ainda não scaffolded. Quando inicializado: `./mvnw spring-boot:run`, `./mvnw test`, `./mvnw verify`. Teste único: `./mvnw test -Dtest=ClassName#method`. DoD exige `mvn test` verde e zero warnings (workflow.md §2.4).

## Commit & Pull Request Guidelines

Conventional Commits simplificado em português (workflow.md §4):

```
tipo(identificador): titulo-curto
```

`tipo` ∈ {`feat`, `fix`, `refactor`, `chore`}; `identificador` ∈ {`us-042`, `fix-017`, `ref-009`, `chore-055`}; título kebab-case. Ex.: `feat(us-042): adiciona-formulario-contato`.

Uma task = uma branch = um PR = um commit squash. Só o `revisor` faz squash e abre PR (com `Closes #<N>`). Só o humano faz merge. Nunca `--force` (use `--force-with-lease` se rebase exigir).

## Atualização de Documentação

Matriz de propriedade (workflow.md §5): PO edita `doc/Requisitos/`; Designer edita `doc/UX/`; Revisor edita `doc/changelog.md` e `doc/release_notes/unreleased.md` ao fechar a task; Codificador e QA nunca editam `doc/**`. `doc/PRD.md` e `doc/workflow.md` só mudam por pedido explícito do humano. Publicação de versão (`unreleased.md` → `vX.Y.Z.md`) **somente sob pedido explícito**.

## Non-Negotiable Product Constraints

LGPD é load-bearing — ver ADR-005/006/007 antes de tocar consentimento, compartilhamento ou dados de menor. Reflexões são opt-in **por item**, nunca global. Idade mínima 13; 13–17 exige responsável vinculado. Texto bíblico default ARC (domínio público).
