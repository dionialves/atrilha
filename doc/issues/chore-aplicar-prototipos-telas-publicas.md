# chore: aplicar protótipos aprovados às telas públicas (home, /comecar, /login)

> **Rascunho de GitHub Issue — produzido pelo Arquiteto/CTO.**
> Abrir no GitHub com `gh issue create --title "<title>" --body-file <este-arquivo> --label chore --label média`.
> O número definitivo (`#NNN`) é atribuído na criação. Código interno: **CHORE-016**.

**Tipo:** chore (task técnica — troca de camada de apresentação, sem mudança de comportamento)
**Prioridade:** média
**Branch:** `chore/<NNN>-aplicar-prototipos-telas-publicas`
**Depende de:** `doc/UX/00-identidade-visual.md` · `doc/UX/01-design-tokens.md` · `doc/UX/02-componentes-base.md`
**Artefatos de referência (aprovados):** `doc/UX/prototypes/home.html` · `doc/UX/prototypes/comecar.html` · `doc/UX/prototypes/login.html`

---

## 1. Contexto

As telas públicas de entrada do app — `GET /` (home), `GET /comecar` e `GET /login` — foram implementadas de forma mínima nos sprints de auth e ainda não refletem a identidade visual aprovada. Em paralelo, foram produzidos e aprovados protótipos estáticos navegáveis em `doc/UX/prototypes/` que materializam essas três telas seguindo integralmente o design system (`chore-ux-001/002/003`).

Esta task fecha a lacuna: leva o layout dos protótipos para os templates Thymeleaf de produção. **Nenhuma rota, formulário, validação, fluxo de autenticação ou contrato de teste muda** — é uma substituição da camada de apresentação.

## 2. Objetivo

Cada uma das três telas de produção deve ficar visualmente equivalente ao seu protótipo aprovado, preservando 100% do comportamento atual (rotas, `POST /login`, CSRF, OAuth Google, banners de erro com `data-error`/`data-state`, toggle de senha).

## 3. Escopo

### Incluído
- `src/main/resources/templates/home.html`
- `src/main/resources/templates/comecar.html`
- `src/main/resources/templates/auth/login.html`
- `src/main/resources/templates/layout/` — novo decorator para telas públicas (ver §5.2)
- `src/main/frontend/css/app.css` — estilos das telas acima
- Self-host das fontes da marca (ver §5.1)

### Fora de escopo (follow-ups separados — ver §8)
- `/trilha`, `/trilha/{dia}`, `/painel` — placeholders ou rota ainda não definida.
- Qualquer mudança em controllers, services, segurança ou modelo.
- Telas de cadastro internas (`cadastro/**`), que já têm spec própria (US-002).

## 4. Arquivos afetados

| Arquivo | Ação |
|---|---|
| `templates/home.html` | Reescrever o `layout:fragment="content"` conforme protótipo |
| `templates/comecar.html` | Reescrever o `layout:fragment="content"` conforme protótipo |
| `templates/auth/login.html` | Reescrever markup; **manter** form, CSRF, `data-error`, Alpine |
| `templates/layout/public.html` *(novo)* | Decorator enxuto para telas públicas |
| `frontend/css/app.css` | Adicionar estilos de página; **não** redeclarar tokens |
| `static/fonts/**` *(novo)* | Arquivos WOFF2 self-hosted |

## 5. Plano de implementação

### 5.1 Pré-requisito — self-host das fontes da marca

`doc/UX/00-identidade-visual.md §4.1` exige **Bricolage Grotesque** (display, pesos 600/700) e **Inter** (texto, 400/500/600) servidas via **self-host com `font-display: swap`**. Os protótipos usam Google Fonts via CDN apenas por conveniência — **em produção isso não vale**.

1. Baixar os WOFF2 variáveis (subset `latin`) e gravar em `src/main/resources/static/fonts/`.
2. Declarar `@font-face` no topo de `app.css` com `font-display: swap`.
3. Confirmar que os stacks `--font-display` e `--font-sans` (já no `@theme`) resolvem para as famílias self-hosted.
4. Orçamento: tipografia total ≤ ~100KB (identidade §4.1). Não importar pesos fora de 400/500/600/700.

### 5.2 Decisão de arquitetura — layout das telas públicas

`layout/base.html` decora as telas com `header` (nav do app logado), `email-verification-banner` e `footer`, dentro de `<main class="container mx-auto px-4 py-6">`. Esse shell **não serve** para telas públicas: home/comecar/login têm header próprio (marca centrada na home; botão voltar em comecar/login), não têm bottom-nav nem footer do app, e controlam o próprio padding.

**Decisão:** criar um decorator novo, `templates/layout/public.html`, enxuto — `<head>` com os mesmos assets (`app.css`, Alpine, HTMX, meta CSRF) e um `<body>` que expõe **apenas** o `layout:fragment="content"`, sem header/footer/banner do app. As três telas passam a usar `layout:decorate="~{layout/public}"`. Cada página renderiza o próprio header (o markup já está nos protótipos). `layout/base.html` permanece intocado para as telas internas.

> Alternativa considerada e rejeitada: tornar header/footer condicionais no `base.html`. Rejeitada por acoplar a tela pública ao shell logado e exigir flags de modelo — `public.html` é mais limpo e isola as duas superfícies.

### 5.3 CSS — `app.css`

- Traduzir o CSS dos protótipos para o sistema **já existente**. Os `<style>` inline dos protótipos foram escritos com um *subset* de tokens — **não copiar esse subset**: `app.css` já tem o `@theme` completo e classes de componente (`.btn`, `.btn-primary`, `.btn-lg`, `.input-field`, `.input-group`, `.card`, `.card--interactive`, `.overline`, `.brand`, `.brand-mark`).
- **Reusar** essas classes. Criar estilos de página novos só para o que não existe (hero da home, header com botão voltar, banners `.alert`, divisor "ou", botão Google, lista de passos). Seguir a convenção de nomes já presente (`.home__*`, `.comecar__*`, etc.).
- Tokens vêm sempre de `var(--token)` do `@theme`. **Proibido** redeclarar hex.
- Preservar a regra global de `prefers-reduced-motion` e `:focus-visible` já existentes.

### 5.4 `home.html`

Aplicar o protótipo `home.html`: header só com a marca centrada, hero (`overline` "Fé no seu ritmo" + `display-xl` "Sua fé, um caminho por dia." + lead), ilustração SVG decorativa de trilha, CTA primário full-width "Começar" → `/comecar`, link secundário "Entrar" → `/login`, rodapé legal discreto. Manter os `th:href="@{/comecar}"` e `@{/login}`.

### 5.5 `comecar.html`

Aplicar o protótipo `comecar.html`: header com botão voltar → `/`, bloco de intro (overline + h1 + lead), dois cards `interactive` de papel (adolescente / responsável) com ícone, faixa etária e seta, callout `info` sobre a vinculação, link "Entrar" no rodapé. **Manter** os destinos atuais (`@{/cadastro/adolescente/escolher-metodo}` e `@{/cadastro/responsavel}`).

### 5.6 `auth/login.html`

Aplicar o protótipo `login.html` **preservando o contrato funcional intacto**:

- `<form th:action="@{/login}" method="post">`, hidden CSRF, `name="username"`/`name="password"`.
- Banners de estado server-side via `th:if` (`errorState`, `infoState`) — manter `role`, `data-error="bad-credentials"`, `data-error="rate-limited"`, `data-state="logged-out"`. **Não** portar a "barra de demonstração" do protótipo (é só ferramenta de preview).
- `disabled` dos campos em `rate-limited`; botão Google **nunca** `disabled`; `data-test="cta-google"`.
- Toggle "mostrar senha" em Alpine; links "Esqueci minha senha" e "Criar conta".
- Aplicar apenas a casca visual nova (header com voltar, `.input-field`, `.alert`, divisor, `.btn-google`).

## 6. Diretrizes técnicas

- **Não** copiar os `<style>` inline nem o `<link>` do Google Fonts dos protótipos para produção.
- **Não** alterar comportamento, rotas, nomes de campos, atributos `data-*` ou `aria-*` de contrato.
- Texto visível em pt-BR; classes/ids/atributos em inglês (componentes-base §0.6).
- Touch target ≥ 44×44px, `:focus-visible` visível, contraste AA — já garantidos pelos tokens; não regredir.
- SVGs decorativos com `aria-hidden="true"`; ícone-botão com `aria-label`.

## 7. Critérios de aceitação

1. `GET /`, `GET /comecar`, `GET /login` renderizam visualmente equivalentes aos protótipos aprovados em viewport de 320px a 1280px, sem scroll horizontal.
2. As três telas usam `layout/public.html`; `layout/base.html` segue inalterado.
3. Fontes Bricolage Grotesque e Inter carregam self-hosted, com `font-display: swap`; sem requisição a `fonts.googleapis.com`.
4. `app.css` não contém hex de cor duplicado de token nem `@theme` redeclarado.
5. Login: `POST /login` funciona; CSRF presente; os três banners aparecem nos estados corretos; campos desabilitam em `rate-limited`; botão Google permanece ativo.
6. `mvn test` passa — incluindo os testes existentes de `LoginPageTest`, `CadastroELoginIT`, `HomeControllerTest`, `StaticAssetsCssIT` e afins. Nenhum contrato `data-*`/`aria-*` quebrado.
7. Compila com zero warnings; `prefers-reduced-motion` e foco acessível preservados.

## 8. Follow-ups (issues separadas)

- **`/trilha/{dia}`** — protótipo `doc/UX/prototypes/trilha-dia.html` pronto, mas a **rota ainda não está definida** (`/trilha/dia/{n}` vs. `/trilha/{slug-do-dia}`). Requer decisão de roteamento + criação da view (hoje só existe `trilha/placeholder.html`).
- **`/trilha`** e **`/painel`** — protótipos `trilha.html` e `painel-pais.html` existentes; aplicar quando as US correspondentes (US-018+ e US-042+) entrarem. Hoje são placeholders.

## 9. Riscos

- **Testes de markup acoplados** (`LoginPageTest`, `StaticAssetsCssIT`): a reescrita pode esbarrar em seletores/strings esperados. Mitigação: rodar a suíte cedo e ajustar markup para manter os contratos, **não** afrouxar os testes.
- **Decoração Thymeleaf Layout Dialect**: o novo `public.html` precisa expor `layout:fragment="content"` exatamente como `base.html` para o `layout:decorate` funcionar.
- **Regressão de FOUT/FOIT** das fontes: validar `font-display: swap` e fallback `system-ui`.

## 10. Entregáveis

Branch `chore/<NNN>-aplicar-prototipos-telas-publicas` com os arquivos da §4, resumo escrito para o QA, e screenshots das três telas (mobile + desktop) anexados à Issue.
