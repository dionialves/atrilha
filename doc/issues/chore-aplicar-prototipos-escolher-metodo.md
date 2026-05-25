# chore: aplicar protótipos de escolha de método ao fluxo de cadastro

> **Rascunho de GitHub Issue — produzido pelo Arquiteto/CTO.**
> Abrir no GitHub com `gh issue create --title "<title>" --body-file <este-arquivo> --label chore --label média`.
> O número definitivo (`#NNN`) é atribuído na criação. Código interno: **CHORE-017**.

**Tipo:** chore (task técnica — troca de camada de apresentação)
**Prioridade:** média
**Branch:** `chore/<NNN>-aplicar-prototipos-escolher-metodo-cadastro`
**Depende de:** **CHORE-016** (decorator `layout/public.html`, fontes self-hosted e classes de componente em `app.css` — este chore reusa tudo isso) · `doc/UX/00-identidade-visual.md` · `doc/UX/01-design-tokens.md` · `doc/UX/02-componentes-base.md` · `doc/UX/us-002-spec.md`
**Artefatos de referência (aprovados):** `doc/UX/prototypes/cadastro-adolescente-metodo.html` · `doc/UX/prototypes/cadastro-responsavel-metodo.html`

---

## 1. Contexto

A CHORE-016 leva os protótipos das telas públicas de entrada (home, `/comecar`, `/login`) para produção. Ela deixou explicitamente **fora de escopo** o fluxo de cadastro (`cadastro/**`) — registrado como follow-up na §8 daquela issue. Esta é essa issue.

Foram produzidos e aprovados os protótipos das telas de **escolha de método de cadastro**, que padronizam o mesmo bloco de autenticação das demais telas: **Google e Apple como métodos principais (desativados nesta fase)** e **e-mail como caminho ativo logo abaixo**. Esta task leva esse layout para os templates Thymeleaf de produção.

## 2. Objetivo

Alinhar a tela `cadastro/adolescente_escolher_metodo.html` ao protótipo aprovado, com o bloco padronizado Google + Apple (desativados) + e-mail (ativo), reusando a infraestrutura entregue pela CHORE-016. O fluxo de cadastro de responsável recebe o **protótipo como referência de design**, mas a troca do template só ocorre quando o épico de responsável existir (ver §8).

## 3. Escopo

### Incluído
- `src/main/resources/templates/cadastro/adolescente_escolher_metodo.html` — reescrever o markup conforme o protótipo.
- `src/main/frontend/css/app.css` — classes `.btn-social` / `.social-stack` / `.social-note` / `.btn-email`, **caso a CHORE-016 não as tenha entregue de forma reutilizável**; se já existirem, apenas reusar.
- Migrar a tela para o decorator `layout/public.html` (criado na CHORE-016).

### Fora de escopo
- Criar a rota/tela real de `/cadastro/responsavel/escolher-metodo` — depende do épico de cadastro de responsável (ver §8).
- Controllers, services, segurança, modelo, fluxo de OAuth.
- Demais telas de `cadastro/**` (formulário US-001, complementação US-002, conclusão) — têm spec própria (`us-002-spec.md`); não mudam aqui.

## 4. Arquivos afetados

| Arquivo | Ação |
|---|---|
| `templates/cadastro/adolescente_escolher_metodo.html` | Reescrever o `layout:fragment="content"` conforme protótipo; decorar com `layout/public.html` |
| `frontend/css/app.css` | Reusar (ou consolidar) as classes de bloco social/e-mail |

## 5. Plano de implementação

### 5.1 `cadastro/adolescente_escolher_metodo.html`

Aplicar o protótipo `cadastro-adolescente-metodo.html`:

- Header com botão voltar → `GET /cadastro` (ou `/comecar`, conforme a entrada canônica vigente — manter o destino atual do template).
- Bloco de intro: `overline` "Cadastro · Adolescente", `h1` "Como você quer entrar?", lead.
- **Métodos principais Google e Apple, no topo, desativados.** Os dois botões com `disabled` + `aria-disabled="true"` + `data-test="cta-google-disabled"` / `data-test="cta-apple-disabled"`, logos Google e Apple, e a legenda "Entrar com Google e Apple está indisponível no momento". É a mesma decisão de produto da CHORE-016 (login social principal, ainda não liberado).
  - **Atenção ao contrato existente:** o template hoje usa `data-test="cta-google-disabled"` no botão Google — manter. Adicionar o botão Apple com `data-test="cta-apple-disabled"`.
- Divisor "ou".
- **Botão de e-mail (ativo), abaixo do divisor** — `secondary` (superfície branca, borda forte), apontando para `GET /cadastro/adolescente` (form da US-001). Hoje o template usa `btn btn-secondary btn-lg` para esse botão; manter como `secondary`.
- Callout `info` sobre a vinculação do responsável; link "Voltar pro começo".

### 5.2 CSS

A CHORE-016 já introduz, no `app.css`, o bloco social do login (`.btn-social`, `.social-stack`, `.social-note`) e os ajustes de `.btn` secundário. Esta task **reusa** essas classes — não recriar. Se a CHORE-016 as tiver deixado específicas do login, o Codificador desta task as generaliza para componente reutilizável, sem duplicar regra. Tokens sempre via `var(--token)` do `@theme`; proibido redeclarar hex.

### 5.3 Decorator

A tela passa a usar `layout:decorate="~{layout/public}"` (decorator público criado na CHORE-016), em vez de `layout/base`. É uma tela pré-login: não leva header do app logado, bottom-nav nem footer.

## 6. Diretrizes técnicas

- **Não** copiar os `<style>` inline nem o `<link>` do Google Fonts dos protótipos para produção — usar `app.css` e as fontes self-hosted da CHORE-016.
- **Não** alterar rotas, nomes de campos ou o fluxo de cadastro.
- Preservar/atualizar os atributos `data-test` de contrato (`cta-google-disabled`; adicionar `cta-apple-disabled`).
- Texto visível em pt-BR; classes/ids/atributos em inglês.
- Touch target ≥ 44×44px, `:focus-visible` visível, contraste AA — não regredir.
- SVG decorativo com `aria-hidden="true"`; botões desativados com `disabled` + `aria-disabled="true"`.

## 7. Critérios de aceitação

1. `GET /cadastro/adolescente/escolher-metodo` renderiza visualmente equivalente ao protótipo aprovado, de 320px a 1280px, sem scroll horizontal.
2. Google e Apple aparecem no topo, `disabled`, com `data-test="cta-google-disabled"` / `cta-apple-disabled` e a legenda de indisponibilidade.
3. O botão de e-mail (ativo) aponta para `GET /cadastro/adolescente` e leva ao formulário da US-001 sem regressão.
4. A tela usa `layout/public.html`; sem header/footer do app logado.
5. `app.css` não duplica tokens nem classes do bloco social já entregues pela CHORE-016.
6. `mvn test` passa — incluindo `AdolescentRegistrationControllerIT`, `RegistrationContractIT`, `StaticAssetsCssIT` e afins. Nenhum contrato `data-*`/`aria-*` quebrado.
7. Compila com zero warnings; `prefers-reduced-motion` e foco acessível preservados.

## 8. Follow-up — cadastro de responsável

O protótipo `doc/UX/prototypes/cadastro-responsavel-metodo.html` está pronto e define a versão definitiva da tela de escolha de método do responsável (mesmo bloco Google + Apple + e-mail). **Porém:**

- A rota `/cadastro/responsavel/escolher-metodo` não existe; hoje `/comecar` → `/cadastro/responsavel` → `responsavel_em_breve.html`.
- O fluxo de cadastro de responsável ainda não foi liberado (não há controller/form).

Portanto a aplicação desse protótipo **não entra nesta task**. Ela deve ser feita dentro do épico de cadastro de responsável (US futura): aí se cria a rota, o controller, o form e a view, usando o protótipo como contrato visual. Até lá, `responsavel_em_breve.html` permanece.

## 9. Riscos

- **Testes de contrato de cadastro** (`AdolescentRegistrationControllerIT`, `RegistrationContractIT`): podem verificar markup/`data-test` da tela de método. Mitigação: rodar a suíte cedo; atualizar asserções para incluir `cta-apple-disabled` sem afrouxar as demais.
- **Acoplamento com a CHORE-016**: esta task depende das classes e do decorator entregues lá. Não iniciar antes da CHORE-016 estar em `main`, ou rebasear quando ela fechar.
- **Divergência de entrada `/cadastro` vs `/comecar`**: a `us-002-spec.md` indica `/cadastro` como canônico e `/comecar` como legado. Manter o destino do botão voltar conforme o template atual; não redecidir roteamento nesta task.

## 10. Entregáveis

Branch `chore/<NNN>-aplicar-prototipos-escolher-metodo-cadastro` com os arquivos da §4, resumo escrito para o QA e screenshots da tela (mobile + desktop) anexados à Issue.
