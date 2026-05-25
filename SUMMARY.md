# Resumo de execução — Issue #86

**Branch:** `chore/86-chore-aplicar-prototipos-de-escolha-de-metodo-ao-f`
**Testes:** 159/159 verdes (Tests run: 159, Failures: 0, Errors: 0, Skipped: 0)
**Warnings de compilação:** 0
**Auditoria CSS:** 130 seletores ativos, 0 órfãos (todos referenciados por template).

## Escopo

A entrega começou pela US chore-86 (aplicar protótipo de escolha de método) e
evoluiu, em iterações com revisão visual, para uma consolidação visual e
arquitetural da camada de apresentação:

1. **Tailwind 100% eliminado.** Sem `@import "tailwindcss"`, sem `@source`,
   sem `@theme`. Tokens migrados para `:root` como CSS custom properties.
   `package.json`, `package-lock.json`, `node/`, `node_modules/` e o
   `frontend-maven-plugin` removidos. CSS servido direto de
   `src/main/resources/static/css/app.css` pelo Spring Boot.
2. **Reset CSS mínimo** — `box-sizing: border-box` global e
   `font/line-height: inherit` para form controls. Sem isso, inputs e
   botões com `width: 100%` extrapolavam padding+border (overflow) e
   `<a class="btn">` ficava mais alto que `<button class="btn">` por
   conta do `line-height` herdado.
3. **Fragmentos Thymeleaf consolidados.** Padrões repetidos viraram
   fragments reaproveitáveis (ver §Fragmentos abaixo).
4. **Padrão visual único.** Todas as telas (públicas e autenticadas com
   chrome enxuto) usam `layout/public` + `public-header`, com wrappers que
   respeitam o header sticky.

## Fragmentos Thymeleaf

| Fragmento | Arquivo | Uso |
|-----------|---------|-----|
| `public-header(backHref, backLabel)` | `layout/fragments/public-header.html` | Header sticky com botão voltar + marca. Usado em `/comecar`, `/login`, `/cadastro/adolescente`, `/cadastro/adolescente/escolher-metodo`, `/cadastro/adolescente/bloqueado`, `/cadastro/responsavel`. |
| `public-header-brand` | `layout/fragments/public-header.html` | Variante só com a marca centrada (sem voltar). Usado em `/trilha`, `/verificar-email`, `/verify-email`, `error/403`, `error/404`, `error/5xx`. |
| `social-methods` | `components/social-methods.html` | Stack vertical com Google e Apple desativados + nota. Preserva `data-test="cta-google-disabled"` e `data-test="cta-apple-disabled"`. Reaproveitado em `/login` e `/cadastro/adolescente/escolher-metodo`. |
| `brand(href)` / `brand-body` | `components/brand.html` | Marca como link (`<a>`) ou só o miolo (SVG + wordmark) para uso dentro de outros elementos. Consumido pelo `public-header` e pela home. |

## Telas migradas para `layout/public` + header padrão

| Tela | Header | Voltar para |
|------|--------|-------------|
| `/comecar` | public-header | `/` |
| `/login` | public-header | `/` |
| `/cadastro/adolescente/escolher-metodo` | public-header | `/comecar` |
| `/cadastro/adolescente` | public-header | `/cadastro/adolescente/escolher-metodo` |
| `/cadastro/adolescente/bloqueado` | public-header | `/comecar` |
| `/cadastro/responsavel` (em breve) | public-header | `/comecar` |
| `/trilha` | public-header-brand | — |
| `/verificar-email` | public-header-brand | — |
| `/verify-email` (resultado) | public-header-brand | — |
| `error/403`, `error/404`, `error/5xx` | public-header-brand | — |

Wrappers padronizados: `.cadastro-form`, `.coming-soon`, `.placeholder`,
`.verify-email`, `.verify-email-result`, `.age-block` e `.escolher-metodo`
têm `max-width: 28rem` e padding vertical/horizontal próprio.

## Ajustes visuais pontuais

- Botão "Começar" da home com `btn--block` (era btn-lg mas ficava
  `width: auto` acima de 640px). Secundário "Já tenho conta. Entrar"
  igualado em tamanho.
- /login: senha, "Entrar" e Google/Apple alinhados ao input de e-mail
  (combinação do reset box-sizing + remoção do `display: flex` desnecessário
  em `.login__password-row`).
- /login agora exibe Google **e** Apple desativados (mesmo padrão visual de
  /cadastro/adolescente/escolher-metodo).
- "Continuar mesmo assim" em /verificar-email direciona para `/trilha`.
- `.escolher-metodo` deixou de constranger o header (a regra antiga
  `max-width: 38rem` aplicada ao `<section>` cortava o header sticky).

## Limpeza CSS (23 seletores apagados)

- Órfãos: `.avatar-initial`, `.card--raised`,
  `.escolher-metodo__alert/intro/lead/options/divider/back`,
  `.google-account-card*`, `.radio-option`, `.input-group--upload`.
- Duplicatas removidas: bloco repetido de `.social-stack/.btn-social/.divider/
  .btn-email/.note/.social-note`; duplicata de `.brand-mark`.
- Headers consolidados (3 → 1): `.header`, `.header-brand`, `.icon-button`,
  `.comecar__header`, `.comecar__back`, `.login__header`, `.login__back`
  → `.public-header`, `.public-header__back`, `.public-header--brand-only`.
- Botão Google específico: `.btn-google`, `.btn-google__logo`, `.login__google`
  (login agora usa `.btn-social` via fragmento).
- `.main`, `.intro`, `.intro h1`, `.intro p`, segunda declaração de
  `.overline` consolidadas/removidas.

## Build & contratos

- `./mvnw -DskipTests package` produz `target/atrilha-0.0.1.jar` sem invocar
  Node/npm — pipeline frontend totalmente eliminado.
- `target/classes/static/css/app.css` byte-idêntico ao fonte (sem build step).
- `Dockerfile` deixou de copiar `package*.json`.
- Contratos testáveis preservados: `cta-google-disabled`, `cta-apple-disabled`,
  `data-error`, `data-state`, `data-testid` (`resend-status-*`, `result-*`),
  `view().name(...)` em todos os controllers.

## ⚠️ Checagem LGPD (atrilha)

N/A — entrega altera apenas a camada de apresentação (templates Thymeleaf,
CSS, fragments, pipeline frontend). Não toca em consentimento, compartilhamento,
dados de menor (13–17) ou em qualquer lógica de ADR-005/006/007.
