# Workflow de Agentes

Guidance for agentic coding agents working in this repository.

Este projeto adota um ciclo de **seis agentes especializados** com responsabilidades **mutuamente exclusivas**: PO → Arquiteto/CTO ⇄ Designer → Codificador → QA → Revisor. Todo trabalho flui por esse pipeline; nenhuma task pode pular etapas.

---

## 1. Workflow de Versionamento

### 1.1 Versionamento Semântico (SemVer)

O projeto segue Semantic Versioning no formato `MAJOR.MINOR.PATCH`:

| Componente | Quando incrementar |
|------------|-------------------|
| `MAJOR` | Mudança incompatível com versões anteriores |
| `MINOR` | Nova funcionalidade compatível com versões anteriores (`US`) |
| `PATCH` | Correção de bug (`FIX`) ou refactor sem impacto funcional (`REF`) |

### 1.2 Gestão de Tasks e Documentação

**Tasks vivem no GitHub Issues.** Cada task é uma Issue com labels, plano detalhado e critérios de aceitação. O número da Issue (`#42`) é o identificador primário.

**Documentação de release** permanece em arquivos locais:

```
doc/
├── Requisitos/
│   └── UserStory.md        # Catálogo de User Stories (mantido pelo PO)
├── UX/
│   └── <CODE>-spec.md      # Specs visuais por task (mantido pelo Designer, sob demanda)
├── changelog.md            # Histórico de mudanças por versão (fechado pelo Revisor)
└── release_notes/
    ├── unreleased.md       # Notas da próxima versão (fechado pelo Revisor)
    └── v1.2.3.md           # Notas de versões já publicadas
```

### 1.3 Dois Caminhos de Entrada

1. **Demanda de cliente/produto** (funcionalidade nova, bug funcional) → **PO** recebe, refina em User Story em `doc/Requisitos/UserStory.md`, entrega ao **CTO/Arquiteto** que cria a GitHub Issue técnica.

2. **Demanda técnica** (dívida técnica, refactor, segurança, infra, bug de código) → **CTO/Arquiteto** recebe diretamente e cria a GitHub Issue sem passar pelo PO.

### 1.4 Ciclo de Vida de uma Task

Toda mudança percorre cinco estados (mais a publicação):

0. **DISCOVER** — Para demandas de produto: **PO** transforma a demanda em User Story em `doc/Requisitos/UserStory.md`. Para demandas técnicas: o **CTO/Arquiteto** recebe diretamente.
1. **CREATE** — **CTO/Arquiteto** investiga o código, eventualmente invoca o **Designer** (em US com superfície visual) que produz `doc/UX/<CODE>-spec.md`, e cria a **GitHub Issue** com plano detalhado, labels e critérios de aceitação.
2. **EXECUTE** — **Codificador** cria uma **feature branch** a partir de `main`, implementa conforme o plano da Issue. Entrega branch + resumo escrito ao QA. Em seguida o **QA** expande a cobertura de testes na mesma branch e produz relatório de testes para o Revisor.
3. **COMPLETE** — feito pelo **Revisor** após emitir o veredito `APROVADO`:
   - Squash dos commits da branch em um único commit limpo
   - Atualizar `doc/changelog.md` e `doc/release_notes/unreleased.md`
   - Push da branch e abertura do **PR no GitHub** via `gh pr create`, com `Closes #<numero>` no body
   - O **humano** revisa o PR e faz merge. A Issue fecha automaticamente.
4. **PUBLISH** (release de versão) — somente sob solicitação explícita do usuário:
   - Renomear `doc/release_notes/unreleased.md` → `doc/release_notes/vX.Y.Z.md`
   - Criar novo `unreleased.md` em branco
   - Substituir `[Unreleased]` por `[X.Y.Z] - YYYY-MM-DD` no `changelog.md`

> Nunca publicar versão sem que todos os itens de `[Unreleased]` estejam documentados em `unreleased.md`. Sempre confirmar com o usuário antes do fluxo de publicação.

### 1.5 Labels do GitHub

| Label | Tipo | Uso |
|-------|------|-----|
| `user-story` | Tipo | Nova funcionalidade vinculada a uma US |
| `bug-fix` | Tipo | Correção de defeito |
| `refactor` | Tipo | Melhoria interna sem mudança de comportamento |
| `chore` | Tipo | Task técnica operacional |
| `alta` | Prioridade | Bloqueia outras tasks / crítico |
| `média` | Prioridade | Importante mas não bloqueante |
| `baixa` | Prioridade | Melhoria incremental / dívida técnica |

### 1.6 Padrão de Branches

```
<tipo>/<numero-issue>-<slug>
```

| Tipo | Quando usar | Exemplo |
|------|-------------|---------|
| `feat/` | User Stories | `feat/42-formulario-contato` |
| `fix/` | Bug Fixes | `fix/17-slug-duplicado` |
| `refactor/` | Refactors | `refactor/78-extrair-helper` |
| `chore/` | Tasks técnicas | `chore/55-atualizar-dependencias` |

O `<numero>` é o número da GitHub Issue. O `<slug>` é kebab-case, sem acentos, max 50 chars.

---

## 2. Atualização de Documentação

### 2.1 GitHub Issues

Fonte única da verdade do trabalho pendente. Cada Issue contém o plano completo da task no formato definido no agent do Arquiteto. Labels categorizam tipo e prioridade.

### 2.2 Changelog (`doc/changelog.md`)

Histórico de tudo que foi entregue, organizado por versão.

```markdown
# Changelog

## [Unreleased]
### User Stories
- US-042 · Nome da Funcionalidade (#42)

### Bug Fixes
- FIX-017 · Descrição curta (#17)

### Refactors
- REF-005 · Descrição curta (#78)

---

## [1.2.0] - 2025-03-15
### User Stories
- US-035 · ...
```

Regras:
- Ao concluir uma task, o Revisor adiciona entrada em `[Unreleased]` com referência à Issue
- O registro contém **apenas o tipo, número, título e link da Issue** (sem descrição detalhada)
- Entradas agrupadas por tipo: User Stories / Bug Fixes / Refactors
- Ao publicar versão, `[Unreleased]` vira `[X.Y.Z] - YYYY-MM-DD`

### 2.3 Release Notes (`doc/release_notes/`)

Detalha o que foi feito em cada task ou versão.

#### Template por task em `unreleased.md`

```markdown
## <TIPO> · <Título> (#<numero>)
**Tipo:** User Story | Bug Fix | Refactor
**Data de conclusão:** YYYY-MM-DD

### O que foi feito
<O que foi implementado, decisões tomadas, comportamento esperado.>

### Impacto
- <Módulos afetados>
- <Número da migration, se aplicável>

### Como testar
1. <passo>
2. <passo>
```

> Bug fixes incluem adicionalmente as seções **Problema / Causa Raiz / Solução**.

### 2.4 Definition of Done

Uma task só é "Done" quando **todos** os itens forem verdadeiros:

1. Código compila com **zero warnings** do compilador
2. `mvn test` passa, incluindo os testes novos
3. Critérios de aceite validados manualmente no browser (quando aplicável)
4. Sem `TODO`/`FIXME` órfão sem issue vinculada
5. Migrations aplicam-se de zero (dev e prod)
6. Templates estendem layout correto e incluem token CSRF em todo `POST`
7. Entradas de texto livre validadas (Jakarta Validation); HTML sanitizado (Jsoup)
8. Mensagens ao usuário em português, sem jargão técnico
9. PR criado com commit squash no padrão `<tipo>(<código>): <título-kebab>`
10. Pelo menos um teste por controller/service novo
11. `doc/changelog.md` e `doc/release_notes/unreleased.md` atualizados no PR
12. PR linka a Issue com `Closes #<numero>`

---

## 3. Workflow dos Agentes

### 3.1 Visão Geral do Ciclo

```
       DESCOBERTA                 CONSTRUÇÃO                              GARANTIA
 ┌────┐  US   ┌───────────┐  Issue   ┌─────────────┐  branch   ┌──────┐  branch   ┌─────────┐
 │ PO │ ────▶ │ CTO/Arq.  │ ───────▶ │ Codificador │ ────────▶ │  QA  │ ────────▶ │ Revisor │ ──▶ PR
 └────┘       └─────┬─────┘          └─────────────┘ +resumo   └──────┘ +relat.   └─────────┘
                    │  ⇅ on demand                                                      │
                    ▼                                                                   ▼
              ┌──────────┐                                                        ┌──────────┐
              │ Designer │  → doc/UX/<CODE>-spec.md                               │  Humano  │ merge PR
              └──────────┘                                                        └──────────┘

  Demanda técnica ──────────────────▶ CTO/Arq. (direto, sem PO)
```

Onde:
- **Issue** = GitHub Issue com plano detalhado
- **branch** = feature branch (`feat/`, `fix/`, `refactor/`, `chore/`)
- **PR** = Pull Request no GitHub, criado pelo Revisor após APROVADO
- **⇅ on demand** = Arquiteto invoca o Designer apenas em USs com superfície visual nova
- O **humano** (Dioni) revisa o PR no GitHub e faz o merge final

### 3.2 Os Seis Agentes — Mentalidade

| # | Agente | Mentalidade | Pergunta central |
|---|--------|-------------|-------------------|
| 1 | **PO / Analista de Requisitos** | Cliente / usuário final | "O que essa pessoa precisa, e como saberei se entreguei?" |
| 2 | **CTO / Arquiteto** | Engenheiro de software sênior | "Como implementar isso de forma correta, segura e testável?" |
| 3 | **Designer / UX** | Designer de produto | "Como o usuário vai ver, entender e operar isso?" |
| 4 | **Codificador** | Programador disciplinado | "Como executo este plano sem desviar?" |
| 5 | **QA / Tester** | Adversário do código | "Como faço isso quebrar?" |
| 6 | **Revisor** | Auditor independente | "Isso atende ao plano, ao critério e à qualidade?" |

### 3.3 Responsabilidades e Fronteiras

| Agente | Pode | Não pode |
|--------|------|----------|
| **PO** | Editar `doc/Requisitos/UserStory.md`; fazer perguntas ao cliente; ler código e docs (somente leitura); redirecionar demandas técnicas ao CTO | Editar `src/**`, plano técnico, design visual; tomar decisões de implementação; escrever critérios técnicos; fechar US sem critérios observáveis |
| **CTO/Arquiteto** | Ler todo o código e docs; criar GitHub Issues com plano detalhado; invocar Designer; consultar PO sobre ambiguidades; receber demandas técnicas diretamente | Editar `src/**`, `pom.xml`, `application*.properties`, templates, static; reescrever User Stories; reescrever critérios de aceite (são contrato do PO) |
| **Designer / UX** | Editar arquivos em `doc/UX/`; ler templates, protótipos, design system, US e código | Editar `src/**`, plano técnico, US; aprovar ou bloquear implementação; decidir aspectos não-visuais |
| **Codificador** | Criar feature branch; editar `src/main/**`, `src/test/**`, `pom.xml`, templates, static, properties; rodar `mvn test`; fazer commits WIP na branch; produzir resumo | Atualizar `doc/**`; criar PR ou fazer push; aprovar próprio trabalho; desviar do plano; refatorar fora de escopo |
| **QA / Tester** | Editar **somente** `src/test/**` na feature branch; rodar `mvn test`; ler todo o código, plano, US, UX spec, resumo; produzir relatório | Editar `src/main/**`, plano, US, UX spec, `doc/`; aprovar ou bloquear; criar commit; corrigir bugs (apenas relata) |
| **Revisor** | Ler tudo; rodar `mvn test`; emitir parecer; **se APROVADO**: squash commits, atualizar `doc/changelog.md` e `doc/release_notes/unreleased.md`, push da branch, criar PR via `gh` | Editar `src/**`, `pom.xml`, properties, templates, static, US, UX spec; criar PR antes de aprovar; fazer merge do PR (isso é do humano) |

### 3.4 Etapas do Ciclo

#### 1. Demanda
Cliente / dono do produto descreve em linguagem natural uma necessidade. Ou uma necessidade técnica é identificada pelo CTO/Arquiteto.

#### 2a. PO refina em User Story (demanda de produto)
- Lê a demanda e identifica ambiguidades.
- Faz perguntas em **rodadas curtas** ao cliente.
- Produz a User Story em `doc/Requisitos/UserStory.md`.
- Entrega ao CTO/Arquiteto o código da US.

#### 2b. CTO recebe diretamente (demanda técnica)
- Dívida técnica, refactor, bug de infra, segurança → CTO/Arquiteto cria a Issue sem US.

#### 3. CTO/Arquiteto planeja (com suporte do Designer quando necessário)
- Lê a US (se houver) e investiga o código afetado.
- **Decide se precisa do Designer:** qualquer US com superfície visual nova → invoca o Designer. Bug fix, refactor, API sem UI → não invoca.
- Cria a **GitHub Issue** com plano detalhado, labels e critérios de aceitação.
- Entrega ao Codificador o número da Issue.

#### 4. Codificador executa
- Lê a Issue, a US e (se houver) o UX spec.
- Valida se o plano é executável — se não for, **devolve ao Arquiteto**.
- Cria **feature branch** a partir de `main` atualizada: `git switch -c <tipo>/<numero>-<slug>`.
- Executa **na ordem exata** os passos do plano.
- Faz commits WIP livremente na branch para salvar progresso.
- `mvn test` deve estar verde ao final.
- **Não cria PR, não faz push.**
- Produz **resumo escrito** e entrega ao QA junto com o nome da branch.

#### 5. QA exercita
- Lê US, Issue, UX spec (se houver), resumo do Codificador e código alterado.
- Roda `mvn test` para confirmar baseline verde.
- Escreve **testes adicionais** na mesma branch, em `src/test/**` apenas:
  - **Caminho infeliz**, **Bordas**, **Regressão**
  - **Critérios de aceitação** que o plano traduziu mal ou ignorou
  - **NUNCA** testa texto literal de UI, strings de front-end, labels, mensagens exatas
  - Testa **comportamento**: status HTTP, presença de atributos, redirecionamentos, chamadas a services
- Se um teste novo falha por bug de produção → devolução ao Codificador.
- Produz **relatório de testes** e entrega ao Revisor.

#### 6. Revisor audita e fecha
- Lê Issue, US, UX spec, resumo, relatório do QA, diff da branch.
- Roda `mvn test`.
- Audita em **três camadas**: (A) aderência ao plano, (B) qualidade técnica, (C) critérios de aceitação.
- Emite parecer:
  - **APROVADO** → o Revisor:
    1. Squash dos commits em um único commit limpo: `<tipo>(<codigo>): <titulo-kebab>`
    2. Atualiza `doc/changelog.md` e `doc/release_notes/unreleased.md`
    3. Push da branch: `git push origin <branch>`
    4. Cria PR no GitHub: `gh pr create --title "..." --body "Closes #<N>" --base main`
    5. Informa o humano que o PR está pronto para revisão
  - **AJUSTES NECESSÁRIOS** → devolve ao agente apropriado
  - **BLOQUEADO** → devolve ao Arquiteto ou PO

### 3.5 Regras do Ciclo

1. **Uma task = um ciclo completo.** Nenhuma task pula etapas — exceto **Designer**, que só entra quando há decisão visual.
2. **Apenas o PO** escreve User Stories em `doc/Requisitos/UserStory.md`. CTO/Arquiteto pode propor refinamentos via devolução.
3. **Apenas o CTO/Arquiteto** cria GitHub Issues com plano técnico.
4. **Apenas o Designer** edita `doc/UX/`.
5. **Apenas o Codificador** cria a feature branch e altera `src/main/**`, `pom.xml`, templates, static, properties.
6. **Apenas o QA** expande `src/test/**` na fase de garantia.
7. **Apenas o Revisor cria o PR.** Codificador e QA entregam a branch sem push.
8. **Apenas o humano (Dioni) faz merge do PR** no GitHub.
9. **Apenas o Revisor** atualiza `doc/changelog.md` e `doc/release_notes/unreleased.md` ao fechar.
10. **Critérios de aceitação são contrato do PO.** Só o PO os reescreve.
11. **Devolução segue a hierarquia da causa raiz** (ver §3.7).
12. **Hard Rules do projeto** são bloqueios automáticos na revisão.
13. Recomendações não-bloqueantes do Revisor viram **novas Issues** criadas pelo CTO/Arquiteto.

### 3.6 Entradas e Saídas por Agente

| Agente | Entrada | Saída |
|--------|---------|-------|
| PO | Demanda do cliente em linguagem natural | User Story em `doc/Requisitos/UserStory.md` + código da US |
| CTO/Arquiteto | US do PO ou demanda técnica direta | GitHub Issue com plano detalhado + número da Issue |
| Designer | Número da Issue + US de referência | UX spec em `doc/UX/<CODE>-spec.md` |
| Codificador | Número da Issue | Feature branch + resumo escrito |
| QA | Feature branch + resumo + Issue + US + UX spec | Branch expandida (testes adicionais) + relatório |
| Revisor | Branch QA + resumo + relatório + Issue | Parecer. Se APROVADO: PR no GitHub |

### 3.7 Devoluções e Fluxos de Retorno

| Quem devolve | Para quem | Quando devolve |
|--------------|-----------|----------------|
| CTO/Arquiteto | PO | US ambígua, sem critério observável, escopo incoerente |
| Designer | PO | Demanda visual conflita com a necessidade descrita na US |
| Designer | CTO/Arquiteto | Restrição técnica torna o desenho inviável |
| Codificador | CTO/Arquiteto | Plano com passo ambíguo, contraditório ou que viola Hard Rules |
| Codificador | Designer | Spec visual inviável ou ambígua |
| QA | Codificador | Teste novo falha (bug detectado na implementação) |
| QA | CTO/Arquiteto | Critério de aceite não tem como ser exercitado / plano ignorou cenário óbvio |
| QA | PO | Critério de aceite não faz sentido para o usuário |
| Revisor | Codificador | Falha de execução: passo errado, qualidade técnica baixa, teste faltando |
| Revisor | QA | Cobertura de teste insuficiente; cenários óbvios não exercitados |
| Revisor | CTO/Arquiteto | Plano inviável, viola proibição, decisão arquitetural errada |
| Revisor | PO | Entrega cumpre o plano mas não resolve a US — gap de requisito |

### 3.8 Orquestração em Sessões Separadas

O ciclo foi desenhado para rodar em **sessões separadas**, uma por agente, usando artefatos persistentes como contrato:

| De → Para | Contrato |
|-----------|----------|
| Cliente → PO | Mensagem em linguagem natural |
| PO → CTO/Arquiteto | `doc/Requisitos/UserStory.md` (US registrada) |
| CTO/Arquiteto ⇄ Designer | `doc/UX/<CODE>-spec.md` |
| CTO/Arquiteto → Codificador | GitHub Issue #N (com plano detalhado) |
| Codificador → QA | Feature branch + resumo escrito |
| QA → Revisor | Feature branch expandida + relatório de testes |
| Revisor → Humano | PR no GitHub (com link para Issue) |

### 3.9 Anti-padrões a Evitar

**PO**
- Escrever critérios técnicos em vez de comportamento
- Fechar US sem perguntar ao cliente quando há ambiguidade
- Decidir tecnologia

**CTO/Arquiteto**
- Escrever código de produção "só para acelerar"
- Pular Designer em US visual
- Reescrever critério de aceite
- Criar Issue sem critérios observáveis

**Designer**
- Editar templates, CSS de produção ou `src/**`
- Decidir sem consultar o design system existente

**Codificador**
- "Corrigir" o plano em silêncio
- Cleanup / refactor oportunista fora do escopo
- Atualizar `doc/**`
- Fazer push ou criar PR
- Entregar ao QA sem o resumo escrito

**QA**
- Editar `src/main/**`
- Testar texto literal de UI (mensagens de erro, labels, placeholders, títulos) em vez de comportamento
- Aprovar ou reprovar (só o Revisor faz)
- Escrever relatório vago ("testei e funciona")
- Parsear CSS para validar propriedades visuais
- Usar regex sobre HTML cru de templates Thymeleaf

**Revisor**
- Aprovar sem rodar `mvn test`
- Criar PR antes de emitir APROVADO
- Fazer merge do PR (isso é do humano)
- Reprovar com base em gosto pessoal

**Geral**
- Task iniciada sem Issue no GitHub
- Commit tocando código fora do escopo
- Pular o Designer em US visual
- Pular o QA em task pequena

### 3.10 Templates de Handoff

#### 3.10.1 User Story (PO → CTO/Arquiteto)

Bloco em `doc/Requisitos/UserStory.md`:

```markdown
## US-### — Título Curto e Descritivo

**Como** <persona>,
**quero** <necessidade>,
**para que** <resultado / valor>.

### Contexto
<Estado atual; por que precisa mudar; restrições conhecidas; dados envolvidos.>

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | <ex.: "Ao clicar em Enviar com campos válidos, o usuário vê confirmação em até 2s"> |
| 2 | <ex.: "Tentar enviar sem preencher 'nome' exibe erro inline"> |

### Fora do Escopo
- <Item excluído>
```

#### 3.10.2 UX Spec (Designer → CTO/Arquiteto)

Arquivo `doc/UX/<CODE>-spec.md`:

```markdown
# UX Spec — <CODE>

**US relacionada:** <US-###>
**GitHub Issue:** #<numero>
**Status:** Proposto | Aprovado pelo Arquiteto

## Objetivo da Tela / Fluxo
## Wireframe Textual
## Componentes do Design System
## Tokens a Reutilizar / Criar
## Estados e Microcopy
## Acessibilidade
## Decisões e Alternativas Descartadas
```

#### 3.10.3 Resumo de Execução (Codificador → QA)

```markdown
# Resumo de execução — <CODE>

**Task:** <CODE> · <título> (Issue #<numero>)
**Branch:** <tipo>/<numero>-<slug>
**Estado:** working tree pronto para QA (sem PR)

## O que foi feito
## Arquivos alterados
## Testes (do plano)
## Decisões implícitas tomadas durante a execução
## Dúvidas / pontos de atenção ao QA
## Critérios de aceitação — autoavaliação
```

#### 3.10.4 Relatório de Testes (QA → Revisor)

```markdown
# Relatório de Testes — <CODE>

**Task:** <CODE> · <título> (Issue #<numero>)
**Branch:** <tipo>/<numero>-<slug>
**Estado:** working tree pronto para revisão (sem PR)

## Cenários Exercitados Além do Plano

| # | Cenário | Tipo | Resultado | Teste |
|---|---------|------|-----------|-------|

## Testes Adicionados
## Achados (gaps de plano ou de critério)

## Veredito do QA
- [ ] Cobertura suficiente para os critérios da US
- [ ] Hard Rules aparentemente respeitadas
- [ ] `mvn test`: VERDE com os testes adicionados
- **Recomendação:** SEGUE PARA REVISOR | DEVOLVER PARA <agente> com motivo
```

---

## 4. Padrão de Commit e PR

### 4.1 Estrutura do Commit

Conventional Commits **simplificado**, em português:

```
tipo(identificador): titulo-curto
```

### 4.2 Componentes

| Componente | Descrição | Valores aceitos |
|------------|-----------|-----------------|
| `tipo` | Categoria da mudança | `feat`, `fix`, `refactor`, `chore` |
| `identificador` | Código da task | `us-042`, `fix-017`, `ref-009` |
| `titulo-curto` | Descrição em português, kebab-case | livre, breve |

### 4.3 Exemplos

```
feat(us-042): adiciona-formulario-contato
fix(fix-017): corrige-slug-duplicado
refactor(ref-009): remove-duplicacao-de-bean
chore(chore-055): atualiza-dependencias-do-pom
```

### 4.4 Regras

- **Quem cria o commit squash:** o **Revisor**, e apenas após APROVADO.
- **Quem cria o PR:** o **Revisor**, via `gh pr create`.
- **Quem faz merge:** o **humano (Dioni)**, no GitHub.
- **Idioma:** título em português.
- **Formato:** `kebab-case` no título.
- **Granularidade:** uma task = uma branch = um PR = um commit squash.
- O PR deve conter `Closes #<numero>` no body para fechar a Issue automaticamente.
- **Nunca** usar `--force` no push (exceto `--force-with-lease` após rebase, se necessário).
- Antes de criar o PR, o Revisor **roda os testes** — suíte vermelha não vira PR.

---

## 5. Quem Pode Editar o Quê

| Agente | Pode editar | NUNCA pode editar |
|---|---|---|
| PO | `doc/Requisitos/UserStory.md` | Qualquer outro arquivo de `doc/`, código, planos técnicos |
| CTO/Arquiteto | GitHub Issues (criar/editar) | `doc/Requisitos/`, `doc/UX/`, código fonte |
| Designer | `doc/UX/*.md` | `src/**`, planos técnicos, US, código |
| Codificador | `src/**`, `pom.xml`, templates, static, properties | Qualquer arquivo em `doc/` |
| QA | `src/test/**` apenas | `src/main/**`, qualquer arquivo em `doc/` |
| Revisor | `doc/changelog.md`, `doc/release_notes/unreleased.md` | Código fonte (`src/**`), templates, `pom.xml`, US, UX specs |

---

## 6. Proibições (Hard Rules)

| # | Proibição |
|---|---|
| 1 | Nunca modificar `doc/requisitos.md` ou `doc/Sprints/user-stories.md` sem solicitação explícita |
| 2 | Codificador nunca atualiza `doc/Requisitos/**`, `doc/UX/**`, `doc/changelog.md`, `doc/release_notes/**` |
| 3 | QA nunca edita qualquer arquivo em `doc/` |
| 4 | QA nunca testa texto literal de UI (labels, mensagens, placeholders) — testa comportamento |
| 5 | CTO/Arquiteto nunca reescreve critérios de aceitação (são contrato do PO) |
| 6 | PO nunca escreve critérios técnicos (ex: "must use BCrypt") |
| 7 | Codificador nunca cria PR ou faz push |
| 8 | Revisor nunca faz merge do PR (isso é do humano) |
| 9 | Demandas técnicas vão direto ao CTO/Arquiteto, sem passar pelo PO |

---

## 7. Checklist Pré-Tarefa (Leitura de Docs)

Antes de modificar qualquer código, o agente deve:

1. Ler `AGENTS.md`, `doc/workflow.md`
2. Ler a GitHub Issue da tarefa (plano completo)
3. Ler a User Story em `doc/Requisitos/UserStory.md` (se houver US relacionada)
4. Ler o UX spec em `doc/UX/<CODE>-spec.md` (se houver)
5. Inspecionar protótipos HTML em `doc/Prototipo/` (se relevante)
6. Confirmar que a tarefa existe como Issue no GitHub
7. Sem Issue no GitHub → sem mudança de código
