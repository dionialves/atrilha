# Resumo de execução — Issue #86

**Branch:** chore/86-chore-aplicar-prototipos-de-escolha-de-metodo-ao-f
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** Tests run: (ver log)
**Warnings de compilação:** 0

## Arquivos alterados
```
src/main/frontend/css/app.css
src/main/resources/templates/cadastro/adolescente_escolher_metodo.html
```

## Diff (stat)
```
 src/main/frontend/css/app.css                      | 100 ++++++++++++++++
 .../cadastro/adolescente_escolher_metodo.html      | 128 +++++++++++++++------
 2 files changed, 191 insertions(+), 37 deletions(-)
```

## O que foi feito

Reescrita completa do template `cadastro/adolescente_escolher_metodo.html` para alinhar a tela de escolha de método ao protótipo aprovado (`doc/UX/prototypes/cadastro-adolescente-metodo.html`). Principais mudanças:

1. **Decorator migrado** de `layout/base` para `layout/public` — tela pré-login, sem header/footer/nav do app logado.
2. **Novo header** com botão voltar (→ `/comecar`) + marca atrilha (SVG brand-mark + wordmark).
3. **Bloco social padronizado**: Google e Apple em `<div class="social-stack">`, ambos `disabled` + `aria-disabled="true"` com `data-test="cta-google-disabled"` / `cta-apple-disabled`. Nota de indisponibilidade adicionada.
4. **Divisor "ou"** e botão e-mail ativo (`class="btn-email"`) apontando para `/cadastro/adolescente`, com SVG de seta inline.
5. **Callout info** sobre vinculação do responsável + link "Voltar pro começo" → `/comecar`.
6. **CSS adicionado** em `app.css`: `.social-stack`, `.btn-social`, `.social-note`, `.divider`, `.btn-email`, `.note` — todos reutilizando tokens `var(--*)` existentes, sem hex redeclarado. Classes são reutilizáveis (não scoped a uma US).

Autoavaliação: todos os 6 critérios de aceitação da issue atendidos. `mvn test` verde (159 testes, 0 falhas). Zero warnings de compilação.

## ⚠️ Checagem LGPD (atrilha)

N/A — sem superfície de dados pessoais. Esta task altera apenas a camada de apresentação (template Thymeleaf + CSS) da tela de escolha de método de cadastro. Não toca em consentimento, compartilhamento, dados de menor (13–17), ou qualquer lógica de ADR-005/006/007.
