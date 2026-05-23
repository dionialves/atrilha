# Repository Guidelines

## Project Context

**atrilha** is a PWA that turns the official Adventist Sabbath School Junior lesson into a gamified daily trail for teens (13–17). Greenfield repo. Product spec lives in `doc/PRD.md` (pt-BR). Documentation and product copy are pt-BR; code identifiers stay in English.

## Workflow — Canonical Source vs. Execução Local

**`doc/workflow.md` é a fonte canônica do ciclo completo de seis papéis** (PO → CTO/Arquiteto ⇄ Designer → Codificador → QA → Revisor → Humano). Ele descreve o processo ideal e permanece a referência conceitual de responsabilidades, fronteiras, labels, Definition of Done e hard rules.

**Na execução com LLMs locais, o ciclo roda enxuto:**

```
Dioni (PO + CTO/Arquiteto + Designer)  →  cria GitHub Issue com plano + critérios
        │
        ▼
Codificador (agente)  →  start_task → implementa → finish_task (testes verdes + SUMMARY)
        │
        ▼
Revisor (agente)  →  load_review → audita 3 camadas → approve (PR DRAFT) | reject (volta ao Codificador)
        │
        ▼
Dioni  →  revisa PR draft → converte para ready → merge
```

- **Os papéis PO, CTO/Arquiteto e Designer são exercidos pelo humano (Dioni)**, que produz a GitHub Issue como contrato de entrada. O QA é coberto pelo `mvn test` determinístico embutido nas tools `finish_task`/`load_review` (verde obrigatório), não por um agente separado.
- **Apenas dois agentes de IA rodam**: Codificador e Revisor, em **sessões separadas**, comunicando-se por artefatos persistentes (worktree + SUMMARY.md + REVIEW.md + Issue).
- **Sem Issue no GitHub → sem mudança de código.**

### Isolamento por git worktree

Cada task vive em uma worktree física isolada em `../<repo>-worktrees/<tipo>-<numero>-<slug>`, permitindo tasks em paralelo (o Codificador trabalha a #42 enquanto você revisa o PR da #38). As tools de Git são **scripts determinísticos** em `.pi/scripts/` — o LLM nunca compõe `git worktree`, `push` ou `gh pr create` cru.

### Configuração do PI Agent

Modelo/provider em `~/.pi/agent/models.json` (copiar de `pi-global/models.json` — registra o LM Studio); papéis Codificador/Revisor descritos em `.pi/SYSTEM.md` (auto-carregado); modelos habilitados em `.pi/settings.json`. O agente assume um papel por sessão e troca de modelo com `/model`. Setup completo em `.pi/README.md`.

### Configuração do OpenCode

Mesmo fluxo, runner alternativo: `.opencode/agents/codificador.md` e `.opencode/agents/revisor.md` espelham os papéis (formato YAML frontmatter + prompt). **Os scripts foram copiados para `.opencode/scripts/`** (cópia idêntica de `.pi/scripts/`) — cada runner é autocontido; se evoluir um lado, sincronize o outro. Troca de papel via `/agent codificador|revisor`; troca de modelo via `/model`. Setup completo em `.opencode/README.md`.

### Tools dos agentes (`.pi/scripts/`)

| Tool | Agente | Faz |
|------|--------|-----|
| `start_task <N>` | Codificador | valida issue, deriva branch/slug das labels, cria worktree, devolve plano |
| `finish_task <N>` | Codificador | roda `mvn test`, checa warnings, gera SUMMARY.md |
| `load_review <N>` | Revisor | dossiê read-only: issue + SUMMARY + re-teste + diff |
| `approve <N>` | Revisor | squash → docs → push → **PR draft** com `Closes #N` |
| `reject <N> "<motivo>"` | Revisor | escreve REVIEW.md, preserva worktree, devolve ao Codificador |

## Project Structure & Module Organization

Module boundaries (PRD §9.3): `auth`, `accounts`, `content`, `progress`, `notifications`, `admin`.

```
doc/
├── PRD.md, workflow.md          # Fonte da verdade — só humano edita
├── Requisitos/UserStory.md      # Dioni (papel PO)
├── UX/<CODE>-spec.md            # Dioni (papel Designer), sob demanda
├── changelog.md                 # Revisor fecha
└── release_notes/{unreleased,vX.Y.Z}.md   # Revisor fecha
```

Testes em `src/test/**`.

## Stack (ADR-011 — locked)

Java 21 (LTS) + Spring Boot 4.0.6 (Spring Framework 7.0, Hibernate 7.1, Flyway 11.11), Maven, Spring Security (OAuth Google + email/senha BCrypt), PostgreSQL 18, Spring Data JPA, Spring Mail. View: Thymeleaf + HTMX + Tailwind + Alpine.js + Lottie + PWA. **Não introduzir** React/Next, Redis, RabbitMQ, microservices ou CDNs pagos — rejeitados em PRD §9.4.

## Build, Test, and Development Commands

`./mvnw spring-boot:run`, `./mvnw test`, `./mvnw verify`. Teste único: `./mvnw test -Dtest=ClassName#method`. DoD exige `mvn test` verde e zero warnings (workflow.md §2.4).

## Commit & Pull Request Guidelines

Conventional Commits simplificado em pt-BR (workflow.md §4): `tipo(identificador): titulo-curto`. `tipo` ∈ {`feat`,`fix`,`refactor`,`chore`}; `identificador` ∈ {`us-042`,`fix-017`,`ref-009`,`chore-055`}; título kebab-case.

Uma task = uma branch = um PR = um commit squash. Só o **Revisor** faz squash e abre PR (como **draft**, via `approve`). Só o humano converte para ready e faz merge. Nunca `--force` (use `--force-with-lease` se rebase exigir).

## Atualização de Documentação

Matriz de propriedade (workflow.md §5): `doc/Requisitos/` e `doc/UX/` são do humano nos papéis PO/Designer; o Revisor edita `doc/changelog.md` e `doc/release_notes/unreleased.md` ao fechar; o Codificador nunca edita `doc/**`. `doc/PRD.md` e `doc/workflow.md` só mudam por pedido explícito do humano.

## Non-Negotiable Product Constraints

LGPD é load-bearing — ver ADR-005/006/007 antes de tocar consentimento, compartilhamento ou dados de menor. Reflexões são opt-in **por item**, nunca global. Idade mínima 13; 13–17 exige responsável vinculado. Texto bíblico default ARC (domínio público). **Estas constraints são bloqueio automático na revisão** (ver `.pi/SYSTEM.md`).
