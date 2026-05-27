# `.qwen/briefs/` — handoff Scout → Synthesizer

Pasta de **artefatos transientes** trocados pelo pipeline de planejamento em duas fases:

```
[arquiteto-scout]   →  escreve  →  .qwen/briefs/<CODE>.md
                                              │
                                              ▼
[arquiteto]         →  lê       →  gera Issue no GitHub
```

Para demandas grandes (que estouram os limites de "brief único"), o fluxo ganha duas passadas:

```
[arquiteto-scout] 1ª passada  →  escreve  →  .qwen/briefs/<CODE>-slicing.md  (proposta de quebra)
                                                          │
                                                          ▼
                                                  humano aprova
                                                          │
[arquiteto-scout] 2ª passada  →  escreve  →  .qwen/briefs/<CODE>-a.md, <CODE>-b.md, ...
                                                          │
                                                          ▼
[arquiteto] (uma invocação por slice, em ordem topológica)  →  gera Issue de cada
```

## Convenção de nomes

`<CODE>` em maiúsculas, sem espaços:

| Arquivo | Quem escreve | Quem lê | Quando |
|---|---|---|---|
| `<CODE>.md` (ex.: `US-042.md`) | Scout (Tier 1) | Arquiteto | Demanda cabe em uma task |
| `<CODE>-slicing.md` (ex.: `US-042-slicing.md`) | Scout (Tier 2, 1ª passada) | Humano (aprova/refina) | Demanda grande precisa quebra |
| `<CODE>-<letra>.md` (ex.: `US-042-a.md`, `US-042-b.md`) | Scout (Tier 2, 2ª passada) | Arquiteto | Slice individual aprovada |

Sufixo de slice: letras minúsculas sequenciais sem pular (`-a`, `-b`, `-c`, ..., `-z`). Limite prático: 6 slices por demanda; mais que isso, é EPIC e o scout devolve para refinamento.

## Quando o Scout dispara slicing (Tier 2)

Heurística objetiva — se **QUALQUER** sinal estoura o limite:

| Sinal | Limite "brief único" |
|---|---|
| Camadas tocadas | ≤ 2 |
| Arquivos `(novo)+(editar)` | ≤ 5 |
| Migrations Flyway novas | ≤ 1 |
| Telas/fluxos de usuário novos | ≤ 1 |
| Testes novos esperados | ≤ 5 |

Casos especiais que sempre disparam slicing:
- Mais de 1 fluxo de usuário independente.
- Mais de 1 migration Flyway nova.
- Mistura de schema/migration + UI/template.

## Ciclo de vida

1. **Scout 1ª passada** — produz `<CODE>.md` (Tier 1) ou `<CODE>-slicing.md` (Tier 2).
2. **Humano** — se Tier 2, lê a slicing proposal, aprova/refina/rejeita. Aprovação dispara 2ª passada.
3. **Scout 2ª passada** (só Tier 2) — produz `<CODE>-a.md`, `<CODE>-b.md`, etc., com `Depende de:` populado por código.
4. **Arquiteto** — uma invocação por brief (Tier 1: 1 invocação; Tier 2: N invocações em ordem topológica). Cria a Issue, resolve `Depende de: <CODE>-<letra>` em `Depende de: #<N> (<CODE>-<letra>)` via `gh issue list`.
5. **Após a Issue criada** — o brief perde valor operacional; a Issue passa a ser o contrato. Briefs e slicing proposals podem ser apagados a qualquer momento.

## Resolução de dependências entre slices

Briefs declaram dependências por **código** (`Depende de: US-042-a`), não por `#N` — números das Issues só existem depois da criação. O Arquiteto resolve no momento de criar cada Issue:

- Se `gh issue list --search "US-042-a"` retorna a Issue → renderiza `Depende de: #142 (US-042-a)` no corpo.
- Se não retorna → **recusa** com instrução de criar a dependência antes. Isso força ordem topológica e evita Issues órfãs.

## Quem edita o quê

| Quem | Escreve aqui | Lê aqui |
|------|--------------|---------|
| `arquiteto-scout` | ✅ cria/sobrescreve `<CODE>.md`, `<CODE>-slicing.md`, `<CODE>-<letra>.md` | ✅ lê `<CODE>-slicing.md` na 2ª passada |
| `arquiteto` | ❌ nunca escreve | ✅ lê `<CODE>.md` ou `<CODE>-<letra>.md` (nunca `-slicing`) |
| Humano | Pode apagar arquivos obsoletos | Lê `<CODE>-slicing.md` para aprovar quebra |
| Codificador / Revisor | ❌ não toca | ❌ não usa (consultam a Issue, não o brief) |

## Comandos canônicos (para o humano)

```
# Demanda nova, qualquer tamanho:
> Use o arquiteto-scout para preparar o brief de US-042
  → Scout decide: produz <CODE>.md OU <CODE>-slicing.md

# Se foi proposta de quebra e você aprovou:
> Use o arquiteto-scout para gerar os briefs das slices aprovadas de US-042
  → Scout produz <CODE>-a.md, <CODE>-b.md, etc.

# Se pediu refinamento da proposta:
> Refaça a slicing proposal de US-042 considerando <ajuste>
  → Scout sobrescreve <CODE>-slicing.md

# Para criar as Issues (uma por vez, em ordem topológica):
> Use o arquiteto para gerar a Issue de US-042-a
> Use o arquiteto para gerar a Issue de US-042-b
> Use o arquiteto para gerar a Issue de US-042-c
```

## Gitignore

`.qwen/.gitignore` ignora `briefs/*.md` mas preserva este `README.md`. Briefs são locais por design — cada operador roda o Scout no seu próprio repo e produz seus próprios briefs.
