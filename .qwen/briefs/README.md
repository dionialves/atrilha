# `.qwen/briefs/` — handoff Scout → Synthesizer

Pasta de **artefatos transientes** trocados pelo pipeline de planejamento em duas fases:

```
[scout]   →  escreve  →  .qwen/briefs/<CODE>.md
                                              │
                                              ▼
[arquiteto]         →  lê       →  gera Issue no GitHub
```

Para demandas grandes (que estouram caps duros do §4 do prompt do scout), o fluxo continua **single-pass** mas produz múltiplos briefs em uma única invocação do scout:

```
[scout]  →  escreve em uma passada:
                       .qwen/briefs/<CODE>-slicing.md   (log de auditoria — referência)
                       .qwen/briefs/<CODE>-a.md         (brief da slice -a)
                       .qwen/briefs/<CODE>-b.md         (brief da slice -b)
                       ...
                       │
                       ▼
[arquiteto] (uma invocação por slice, em ordem topológica)  →  gera Issue de cada
```

**Não há gate de aprovação intermediário.** O scout decide a quebra e escreve tudo de uma vez. Se o humano discorda, apaga os briefs (`rm .qwen/briefs/<CODE>*`) e re-invoca com diretriz no prompt.

## Convenção de nomes

`<CODE>` em **MAIÚSCULAS**, sem espaços:

| Arquivo | Quem escreve | Quem lê | Quando |
|---|---|---|---|
| `<CODE>.md` (ex.: `US-042.md`) | Scout (Tier 1) | Arquiteto | Demanda cabe em uma task |
| `<CODE>-slicing.md` (ex.: `US-042-slicing.md`) | Scout (Tier 2) | humano (auditoria) | Junto com os briefs por slice, na mesma passada |
| `<CODE>-<letra>.md` (ex.: `US-042-a.md`, `US-042-b.md`) | Scout (Tier 2) | Arquiteto | Junto com slicing.md, na mesma passada |

Sufixo de slice: letras minúsculas sequenciais sem pular (`-a`, `-b`, `-c`, ..., `-z`). Limite prático: 8 slices por demanda; mais que isso, o scout devolve a demanda para refinamento conceitual (EPIC).

## Quando o Scout dispara slicing (Tier 2)

Decisão **numérica obrigatória** do protocolo §4 do prompt do scout. Caps duros — **qualquer um** estourado força slicing, sem override:

| Sinal | Cap duro |
|---|---|
| `N_camadas` | > 4 |
| `N_arquivos_novos` | > 8 |
| `N_arquivos_novos + N_arquivos_editar` | > 10 |
| `N_migrations` | > 1 |
| `N_templates_html` | > 2 |
| `N_templates_email` | > 1 |
| `N_endpoints` | > 3 |
| `N_i18n` | > 8 |
| `N_testes` | > 6 |
| `output_estimado_chars` (fórmula no §4.1 do scout) | > 18000 |

E uma rede de segurança pré-gravação: se `brief_chars_estimado > 15000` para Tier 1, o scout aborta a escrita do brief único e converte para Tier 2.

## Ciclo de vida

1. **humano invoca scout** com `Use o scout para preparar o brief de <CODE>` (com diretriz opcional após vírgula).
2. **Scout executa** investigação frugal + medição numérica + decisão.
   - **Tier 1**: escreve `<CODE>.md`. Devolve comando para invocar arquiteto.
   - **Tier 2**: escreve `<CODE>-slicing.md` + `<CODE>-a.md` + `<CODE>-b.md` + ... **na mesma passada**. Devolve lista ordenada topologicamente com os comandos para invocar arquiteto.
3. **humano audita** (opcional) o slicing log. Se discorda, apaga tudo (`rm .qwen/briefs/<CODE>*`) e re-invoca o scout com diretriz (ex.: "considere juntar -c e -d").
4. **Arquiteto** — uma invocação por brief, em ordem topológica. Cria a Issue, resolve `Depende de: <CODE>-<letra>` em `Depende de: #<N> (<CODE>-<letra>)` via `gh issue list`.
5. **Após a Issue criada** — o brief perde valor operacional; a Issue passa a ser o contrato. Briefs e slicing logs podem ser apagados a qualquer momento.

## Resolução de dependências entre slices

Briefs declaram dependências por **código** (`Depende de: US-042-a`), não por `#N` — números das Issues só existem depois da criação. O Arquiteto resolve no momento de criar cada Issue:

- Se `gh issue list --search "US-042-a"` retorna a Issue → renderiza `Depende de: #142 (US-042-a)` no corpo.
- Se não retorna → **recusa** com instrução de criar a dependência antes. Isso força ordem topológica e evita Issues órfãs.

## Quem edita o quê

| Quem | Escreve aqui | Lê aqui |
|------|--------------|---------|
| `scout` | ✅ cria/sobrescreve `<CODE>.md`, `<CODE>-slicing.md`, `<CODE>-<letra>.md` (Tier 2: tudo na mesma passada) | — (não relê briefs próprios) |
| `arquiteto` | ❌ nunca escreve | ✅ lê `<CODE>.md` ou `<CODE>-<letra>.md` (nunca `-slicing.md`) |
| Humano | Pode apagar arquivos para forçar re-geração | Lê `<CODE>-slicing.md` para auditar a decisão do scout |
| Codificador / Revisor | ❌ não toca | ❌ não usa (consultam a Issue, não o brief) |

## Comandos canônicos (para o humano)

```
# Demanda nova, qualquer tamanho:
> Use o scout para preparar o brief de US-042
  → Scout decide: produz <CODE>.md (Tier 1) OU
                  <CODE>-slicing.md + <CODE>-a.md + <CODE>-b.md + ... (Tier 2, tudo de uma vez)

# Com diretriz (qualquer caso — Tier 1 ou Tier 2):
> Use o scout para preparar o brief de US-042, considerando: <ajuste>
  → ex.: "quebre por fluxo de usuário, não por camada"
  → ex.: "force brief único — vou implementar tudo num único PR"
  → ex.: "use 5 slices, separando o auto-login em slice própria"

# Para refazer porque discordou da quebra anterior:
> rm .qwen/briefs/US-042*
> Use o scout para preparar o brief de US-042, considerando: <ajuste>

# Para criar as Issues (uma por vez, em ordem topológica):
> Use o arquiteto para gerar a Issue de US-042         # Tier 1
# OU:
> Use o arquiteto para gerar a Issue de US-042-a       # Tier 2 — sem dep
> Use o arquiteto para gerar a Issue de US-042-b       # após -a virar Issue
> Use o arquiteto para gerar a Issue de US-042-c       # após -b virar Issue
```

## Gitignore

`.qwen/.gitignore` ignora `briefs/*.md` mas preserva este `README.md`. Briefs são locais por design — cada operador roda o Scout no seu próprio repo e produz seus próprios briefs.
