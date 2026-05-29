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

Cada task vive em uma worktree física isolada em `<repo>/<runner>/worktrees/<tipo>-<numero>-<slug>`. As tools de Git são **scripts determinísticos** no diretório do runner ativo (`.pi/scripts/`, `.opencode/scripts/`, `.qwen/scripts/`) — o LLM nunca compõe `git worktree`, `push` ou `gh pr create` cru. Setup específico de cada runner está no `README.md` da sua pasta.

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

LGPD é load-bearing — ver ADR-005/006/007 antes de tocar consentimento, compartilhamento ou dados de menor. Reflexões são opt-in **por item**, nunca global. Idade mínima 13; 13–17 exige responsável vinculado. Texto bíblico default ARC (domínio público). **Estas constraints são bloqueio automático na revisão.**

<!-- qwen:compliance-required -->
<!-- qwen:compliance-label: LGPD -->
<!-- opencode:compliance-required -->
<!-- opencode:compliance-label: LGPD -->

## Subagent Routing (qwen-code) — sessão raiz é roteador, NÃO executor

> Esta seção é **boilerplate obrigatório** do template `.qwen/`. Sem ela, a sessão raiz tenta executar trabalho de subagent diretamente e o pipeline quebra. Quando portar `.qwen/` para outro projeto, copie esta seção inteira para o `AGENTS.md` do novo repo.

A sessão raiz do qwen é um **roteador**. Quando você (LLM da raiz) receber um pedido que se encaixa em qualquer dos papéis abaixo, **invoque o subagent correspondente via a tool `Agent`** — nunca tente executar o papel você mesmo, mesmo que tenha as tools necessárias.

### Tabela de roteamento por intenção

| O usuário pede algo como... | Delegue para | Frase canônica para invocar |
|---|---|---|
| "planejar US-XXX", "preparar brief de", "investigar para X" | `scout` | `Use o scout para preparar o brief de <CODE>` |
| "criar / gerar a Issue de", "sintetizar o brief de" | `arquiteto` | `Use o arquiteto para gerar a Issue de <CODE>` |
| "implementar / codificar / iniciar a issue #N" | `codificador` | `Use o codificador para iniciar a implementação da issue #N` |
| "revisar / auditar / aprovar a issue #N" | `revisor` | `Use o revisor para auditar a issue #N` |

### Decisão por estado (quando o pedido é ambíguo, ex.: "vamos para US-008-b")

Inspecione o disco/GitHub do **estado mais avançado para o mais atrasado** — a primeira regra que bate ganha. Estados anteriores podem ter artefatos ausentes (ex.: brief apagado após Issue criada — comportamento normal, ver `briefs/README.md`).

1. **Worktree tem `SUMMARY.md` e ainda não há PR** (`gh pr list --head <branch>` vazio) → delegue ao **`revisor`**
2. **Issue OPEN existe mas worktree não** (`ls .qwen/worktrees/*-<N>-*` vazio) → delegue ao **`codificador`** (o brief é INPUT do arquiteto que já passou; **não é pré-requisito do codificador**)
3. **Brief existe mas Issue não foi criada** (`gh issue list --state all --search "<CODE> in:title"` vazio) → delegue ao **`arquiteto`**
4. **Nada existe** (sem brief, sem Issue, sem worktree) → delegue ao **`scout`**

⚠️ **Anti-padrão comum**: brief ausente NÃO é sinal automático de "rode scout". Se a Issue para o `<CODE>` já existe no GitHub, o brief virou descartável (foi consumido pelo arquiteto). Cheque GH **antes** de assumir que precisa do scout.

### Proibições absolutas da sessão raiz

A sessão raiz **NUNCA**:

- Roda `.qwen/scripts/create_issue.sh`, `start_task.sh`, `finish_task.sh`, `load_review.sh`, `approve.sh`, `reject.sh` diretamente
- Escreve em `.qwen/briefs/`, `.qwen/tmp/`, `.qwen/worktrees/`
- Lê `.qwen/agents/scout.md`, `arquiteto.md`, `codificador.md`, `revisor.md` para "imitar" o subagent (esses prompts pertencem aos próprios subagents — você lê AGENTS.md, não os deles)
- Improvisa o body de uma Issue, executa o plano de uma Issue, audita um diff
- Responde com "ok, vou escrever o body eu mesmo" quando um script de subagent falha — em vez disso, delegue ao subagent correto

Se a sessão raiz se vir tentada a fazer qualquer dessas coisas, **pare e delegue** ao subagent apropriado. Errar por excesso de delegação é OK; errar por excesso de execução quebra o pipeline.

### Exceção: perguntas conceituais e investigação leve

A sessão raiz **pode**:

- Responder perguntas sobre o projeto / convenções / arquitetura (consultando esta AGENTS.md ou rodando Grep/Read pontuais)
- Listar briefs/Issues/worktrees existentes para diagnóstico
- Apontar o estado do pipeline para o usuário ("o brief X existe, próximo passo: invoque o arquiteto")
- Editar este `AGENTS.md` e arquivos em `.qwen/` quando o usuário pedir explicitamente para configurar/ajustar o pipeline

## Subagent Routing (opencode) — sessão principal é roteador, NÃO executor

> Esta seção é **boilerplate obrigatório** do template `.opencode/` (runner opencode). Sem ela, a sessão principal tenta executar trabalho de subagent diretamente e o pipeline quebra. Ao portar `.opencode/` para outro projeto, copie esta seção inteira. Ela coexiste com a seção do qwen acima — os dois runners compartilham o mesmo repo, mas têm **workflows diferentes** (ver abaixo).

### ⚠️ Diferença de workflow: opencode usa `1 US = 1 worktree = 1 PR`

Diferente do `.qwen` (1 brief → 1 Issue → 1 worktree → 1 PR por *slice*), o `.opencode` trata **uma demanda inteira como uma única worktree/branch/PR**, com N **subtasks** revisadas uma a uma:

- O **scout** mede o tamanho (§4) e escreve: **Tier 1** (pequena) = 1 brief `.opencode/briefs/<CODE>.md`; **Tier 2** (grande) = N **briefs de subtask** `<CODE>-a.md`, `<CODE>-b.md`, … + `<CODE>-slicing.md`, numa única passada. Em ambos, o downstream converge para 1 Issue / 1 PR — só o *planejamento* é fatiado.
- O **arquiteto fase 1** consolida o(s) brief(s) em **1 Issue-resumo** (visão geral + lista de subtasks a/b/c).
- O **arquiteto fase 2** escreve **1 spec por subtask** em `.opencode/tasks/<CODE>-<letra>.md`.
- O **codificador** abre **1 worktree** (`.opencode/worktrees/<CODE>/`, nomeada pelo código) e implementa subtask a subtask.
- O **revisor** aprova **por subtask** (cada uma vira 1 commit local, sem push) e, só quando todas aprovadas, abre **1 PR** via `open_pr`.

### Tabela de roteamento por intenção (opencode)

| O usuário pede algo como... | Delegue para | Frase canônica |
|---|---|---|
| "planejar US-XXX", "preparar brief de", "investigar para X" | `scout` | `Use o scout para preparar o brief de <CODE>` |
| "criar / gerar a Issue de" | `arquiteto` (fase 1) | `Use o arquiteto para gerar a Issue de <CODE>` |
| "detalhar / especificar as subtasks de" | `arquiteto` (fase 2) | `Use o arquiteto para detalhar as subtasks de <CODE>` |
| "implementar / codificar a demanda <CODE>" | `codificador` | `Use o codificador para implementar <CODE>` |
| "revisar / auditar a subtask <CODE>-a" | `revisor` | `Use o revisor para auditar a subtask <CODE>-<letra>` |
| "abrir o PR de <CODE>" (todas subtasks aprovadas) | `revisor` | `Use o revisor para abrir o PR de <CODE>` |

### Decisão por estado (opencode — do mais avançado ao mais atrasado)

1. **Todas as subtasks têm commit aprovado e não há PR** → delegue ao **`revisor`** (`open_pr <CODE>`).
2. **Worktree existe** (`ls .opencode/worktrees/<CODE>`) **com subtask finalizada** (SUMMARY em `.opencode/tmp/<CODE>-<letra>-SUMMARY.md`) → delegue ao **`revisor`** (auditar a subtask).
3. **Specs existem** (`.opencode/tasks/<CODE>-*.md`) **mas worktree não** → delegue ao **`codificador`**.
4. **Issue-resumo existe mas sem specs** → delegue ao **`arquiteto` fase 2**.
5. **Brief existe** (`<CODE>.md` Tier 1, ou `<CODE>-slicing.md`/`<CODE>-a.md` Tier 2) **mas Issue não** → delegue ao **`arquiteto` fase 1**.
6. **Nada existe** → delegue ao **`scout`**.

### Proibições absolutas da sessão principal (opencode)

A sessão principal **NUNCA**:
- Roda `.opencode/scripts/*.sh` (`create_issue`, `start_us`, `finish_task`, `load_review`, `approve`, `reject`, `open_pr`) diretamente.
- Escreve em `.opencode/briefs/`, `.opencode/tasks/`, `.opencode/tmp/`, `.opencode/worktrees/`.
- Lê `.opencode/agent/*.md` para "imitar" o subagent.
- Improvisa body de Issue, escreve spec de subtask, executa o plano, audita um diff.

Pode (exceção): responder perguntas conceituais; listar briefs/Issues/specs/worktrees para diagnóstico; apontar o próximo passo; editar `AGENTS.md` e `.opencode/` quando o usuário pedir explicitamente para configurar o pipeline. Atalho de diagnóstico: comando `/orquestrar-pipeline-us <CODE>`.
