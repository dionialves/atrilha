# `.qwen/briefs/` — handoff Scout → Synthesizer

Pasta de **artefatos transientes** trocados pelo pipeline de planejamento em duas fases:

```
[arquiteto-scout]   →  escreve  →  .qwen/briefs/<CODE>.md
                                              │
                                              ▼
[arquiteto]         →  lê       →  gera Issue no GitHub
```

## Convenção de nome

`<CODE>.md` em maiúsculas, sem espaços:

- `US-042.md`
- `FIX-017.md`
- `REF-009.md`
- `CHORE-055.md`

Se uma demanda for quebrada em vários briefs (escopo grande), use sufixo: `US-042-a.md`, `US-042-b.md`.

## Ciclo de vida

1. **Scout** (`arquiteto-scout`, modelo MoE 35B) cria/sobrescreve `<CODE>.md` com **dados factuais** (arquivos, snippets literais, migrations existentes, testes, issues relacionadas). NÃO decide arquitetura.
2. **Synthesizer** (`arquiteto`, modelo 27B) lê **apenas o brief** (não abre código), decide arquitetura, gera a Issue extremamente detalhada via `gh issue create`.
3. Após a Issue criada, o brief perde valor operacional — a Issue passa a ser o contrato. O brief pode ser apagado a qualquer momento; o repo gitignora `*.md` daqui (exceto este README).

## Quem edita o quê

| Quem | Escreve aqui | Lê aqui |
|------|--------------|---------|
| `arquiteto-scout` | ✅ cria/sobrescreve `<CODE>.md` | — |
| `arquiteto` | ❌ nunca escreve | ✅ lê `<CODE>.md` |
| Codificador / Revisor / Humano | ❌ não toca | ❌ não usa (consultam a Issue) |

## Gitignore

`.qwen/.gitignore` ignora `briefs/*.md` mas preserva este `README.md`. Briefs são locais por design — cada operador roda o Scout no seu próprio repo e produz seus próprios briefs.
