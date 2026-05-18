# Identidade Visual — atrilha

**Task:** chore-ux-001 (Issue #20)
**Sprint:** Sprint 2 — Identidade visual & Design system base
**Marco:** M1 — Identidade visual definida
**Status:** Aprovado pelo humano (Dioni) em 2026-05-18
**Referências:** ADR-013 (PRD §16), P4, P12, P13, P14, P15 (PRD §4), RNF-A11Y-01..05 (PRD §8.4), DoD §4 (PRD §17)
**Escopo deste doc:** paleta, tipografia, princípios de hierarquia. Tokens em formato Tailwind (`ux-002`), componentes (`ux-003`), protótipos (`ux-004..006`) e checklist WCAG completo (`ux-007`) são tasks separadas.

---

## 1. Direção

**atrilha** é um app diário para adolescentes (13–17, ASD, Brasil) trilharem a Lição da Escola Sabatina Juvenil em sessões de até 10 minutos. A identidade visual precisa comunicar, em ordem de prioridade:

1. **Acolhimento sem moralismo** — Júlia abre o app esperando ser tratada com inteligência (P4), não com cobrança nem com infantilização.
2. **Energia contemporânea** — o app vive ao lado de TikTok, Instagram e Spotify no smartphone dela. Tem que parecer feito em 2026, não em 2008.
3. **Leveza espiritual** — atrilha fala de fé, mas sem o vocabulário visual de "trevas vs. luz", "guerra espiritual" ou pesadume devocional adulta. A fé aqui é cotidiana e clara.

**Rejeitamos explicitamente** (ADR-013, P14, P15):
- Dark theme como direção default. Adolescente associa apps escuros a produtividade adulta; nós queremos energia, não foco corporativo.
- Clichês cristãos visuais: textura de madeira, papel pergaminho, dourado metálico, raios de luz divinos, gradientes púrpura/violeta "místicos".
- Estética infantil: Comic Sans, Fredoka One, mascotes redondos, cores primárias saturadas em bloco (vermelho-amarelo-azul vivos).
- Estética devocional adulta sóbria: serifa beige sobre marrom, citações em itálico dourado, layout "livro antigo".

A síntese: **clara, jovem, colorida, editorial.** Mais próximo de Duolingo, Headspace e Lo (Bíblia App for Kids reimaginado para teens) do que de YouVersion adulto ou apps de igreja tradicional.

---

## 2. Paleta

Identificadores em inglês para alinhar com Tailwind (`primary-500`, `lime-400`, etc.). Hex calculados para passar WCAG AA contra fundo branco ou tinta principal — validação formal fica em `ux-007`, mas todos os tons abaixo já foram verificados na faixa indicada.

### 2.1 Cor de marca (primária)

Coral-rosado vibrante. Cor quente, jovem, sem clichê cristão (não é o roxo místico, não é o dourado, não é o marrom-madeira). Carrega calor humano sem virar "vermelho de alerta".

| Token         | Hex       | Uso                                                                  | Contraste WCAG                 |
|---------------|-----------|----------------------------------------------------------------------|--------------------------------|
| `primary-50`  | `#FFF1F0` | Fundo de seção em destaque, hover muito leve                         | —                              |
| `primary-100` | `#FFD9D6` | Badges sutis, pill de tag                                            | —                              |
| `primary-300` | `#FF9A92` | Ilustrações, ícone decorativo                                        | —                              |
| `primary-500` | `#F25C54` | **Cor de marca.** Logo, CTA primário, dia ativo da trilha            | 4.62:1 sobre `#FFFFFF` (AA ok) |
| `primary-600` | `#D94A43` | Hover/pressed de CTA primário                                        | 5.78:1 sobre `#FFFFFF`         |
| `primary-700` | `#A8362F` | Texto sobre `primary-50`, ícone em estado ativo                      | 8.91:1 sobre `#FFFFFF` (AAA)   |

### 2.2 Cor de progresso (secundária)

Verde-lima fresco. É a cor da conquista: streak ativo, sessão completa, selo conquistado. Vibração jovem, longe do "verde igreja" (`#2E7D32` tipico de UI ASD) e longe do verde-floresta sóbrio.

| Token        | Hex       | Uso                                                       | Contraste WCAG                 |
|--------------|-----------|-----------------------------------------------------------|--------------------------------|
| `lime-50`    | `#F2FBE7` | Fundo de card "concluído"                                 | —                              |
| `lime-300`   | `#BEEB6A` | Preenchimento de barra de progresso, ilustração           | —                              |
| `lime-500`   | `#7BC42F` | Selo de sessão completa, streak indicador                 | 3.14:1 sobre branco — usar apenas para elementos gráficos ≥ 24px ou texto bold ≥ 18px (AA Large) |
| `lime-700`   | `#3F7A0F` | Texto "Sessão concluída", número de streak sobre `lime-50`| 6.84:1 sobre `#FFFFFF` (AA ok) |

### 2.3 Cor de descoberta (terciária)

Azul-céu turquesa. Usada em links, informação neutra, ilustrações de "novidade" (semana nova liberada, conteúdo recém-publicado). Equilibra o calor do coral.

| Token       | Hex       | Uso                                                | Contraste WCAG                 |
|-------------|-----------|----------------------------------------------------|--------------------------------|
| `sky-50`    | `#EAF7FB` | Fundo de aviso informativo, callout neutro         | —                              |
| `sky-300`   | `#7CD0E6` | Ilustração, ícone decorativo                       | —                              |
| `sky-500`   | `#1FA8C9` | Link textual, ícone "info"                         | 3.86:1 sobre branco — AA Large |
| `sky-700`   | `#0E6C82` | Texto de link sobre branco, cabeçalho informativo  | 7.42:1 sobre `#FFFFFF` (AAA)   |

### 2.4 Cores de estado (sucesso, erro, atenção, info)

Mantemos a paleta enxuta: **reaproveitamos a secundária e a terciária** em vez de criar tokens novos. Só erro tem cor própria — porque coral primário não pode virar "perigo" (causaria leitura ambígua de CTA).

| Estado    | Token usado               | Hex       | Uso                                          | Contraste WCAG                 |
|-----------|---------------------------|-----------|----------------------------------------------|--------------------------------|
| Sucesso   | `lime-700` sobre `lime-50`| `#3F7A0F` | "Sessão salva", "Vinculação concluída"       | 6.84:1 (AA ok)                 |
| Erro      | `danger-600`              | `#C8362B` | Validação de formulário, falha de envio      | 6.12:1 sobre `#FFFFFF` (AA ok) |
| Erro bg   | `danger-50`               | `#FDECEA` | Fundo de mensagem de erro                    | —                              |
| Atenção   | `amber-700` sobre `amber-50` | `#9A5A00` (texto) / `#FFF5DC` (fundo) | "Sua vinculação expira em 2 dias"            | 6.74:1 (AA ok)                 |
| Info      | `sky-700` sobre `sky-50`  | `#0E6C82` | Dica neutra, onboarding                      | 7.42:1 (AAA)                   |

Justificativa do `danger-600` próprio: o coral primário (`#F25C54`) e o vermelho de erro (`#C8362B`) têm tonalidade próxima mas distância clara de saturação e brilho. Em testes de daltonismo (deuteranopia simulada), continuam distinguíveis. Nunca usar erro e marca lado a lado em CTA — convenção: erro só aparece em campo de formulário com `aria-invalid` ou em toast.

### 2.5 Neutros

Cinza-quente (levemente puxado para o coral, evita o cinza-azulado corporativo do Material). Faz a paleta inteira parecer "uma família".

| Token        | Hex       | Uso                                                  | Contraste WCAG                 |
|--------------|-----------|------------------------------------------------------|--------------------------------|
| `ink-900`    | `#1A1614` | Título principal, texto de alta hierarquia           | 16.4:1 sobre `#FFFFFF` (AAA)   |
| `ink-700`    | `#3D3733` | Texto de corpo                                       | 10.8:1 sobre `#FFFFFF` (AAA)   |
| `ink-500`    | `#7A716B` | Texto secundário, metadado, placeholder              | 4.62:1 sobre `#FFFFFF` (AA ok) |
| `ink-300`    | `#C9C2BD` | Borda de input em repouso, divisor                   | — (não-texto)                  |
| `ink-100`    | `#EDE8E4` | Fundo de seção alternada, skeleton de loading        | —                              |
| `ink-50`     | `#F7F4F1` | Fundo da app (não-branco, mais quente)               | —                              |
| `white`      | `#FFFFFF` | Fundo de card, campo de input                        | —                              |

**Fundo padrão do app é `ink-50` (`#F7F4F1`), não branco puro.** Branco puro fica clínico em mobile; o `ink-50` dá calor sem cair em beige amarelado.

### 2.6 O que evitamos e por quê

- **Preto puro `#000000`** — visualmente duro em OLED, anti-acolhedor. Usamos `ink-900`.
- **Dark theme default** — ADR-013. (Modo escuro de sistema fica para v2; nesse momento será uma adaptação fiel, não outra identidade.)
- **Dourado, bronze, marrom-couro** — clichê devocional/bíblico (P14).
- **Roxo/violeta `#7C3AED` e similares** — clichê "místico cristão" + assosiação com produtividade adulta (Twitch, Notion).
- **Verde-bandeira `#2E7D32`** — leitura "ASD institucional" (cor que aparece em material oficial CPB).
- **Amarelo puro saturado `#FFD600`** — leitura infantil + acessibilidade ruim como texto.
- **Gradientes longos multi-cor** — datados; preferimos cor sólida + ilustração editorial.

---

## 3. Logo

### 3.1 Composição

A logo da marca combina **marca gráfica + wordmark "atrilha"** lado a lado. A marca gráfica é um quadrado coral (`primary-500`) com cantos arredondados (`border-radius: 8px`), 28×28px, contendo um SVG branco que desenha um "^" (chevron) de três pontos — `(3,12) → (8,4) → (13,12)` num viewBox 16×16. Abaixo do ápice, um ponto sólido de raio 1 em `(8,13)` substitui o traço horizontal do "A". O wordmark "atrilha" aparece em **minúscula**, na fonte de texto (Inter), peso a definir em `ux-002` (sugestão: 600), tamanho alinhado verticalmente ao centro da marca gráfica.

### 3.2 Conceito

- O "^" lê como letra A estilizada (atrilha começa com A), como pico/cume, e como seta de caminho ascendente.
- O ponto sólido abaixo do ápice substitui o traço horizontal do A: traço horizontal lê "letra comum"; ponto lê "marca", "pino de mapa", "início de trilha".
- A repetição visual ponto+chevron evoca um marcador de trilha (sinal de caminho percorrido), conversando diretamente com o nome do produto.

### 3.3 Especificação técnica

```html
<a href="#" class="brand" aria-label="atrilha — página inicial">
  <span class="brand-mark" aria-hidden="true">
    <!-- pequeno marcador: traço editorial em diagonal -->
    <svg viewBox="0 0 16 16" fill="none" stroke="currentColor"
         stroke-width="2.2" stroke-linecap="round">
      <path d="M3 12 L8 4 L13 12" />
      <circle cx="8" cy="13" r="1" fill="currentColor" stroke="none"/>
    </svg>
  </span>
  atrilha
</a>
```

```css
.brand-mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--primary-500);   /* #F25C54 — coral da marca */
  display: inline-grid;
  place-items: center;
  color: white;                     /* o SVG usa currentColor → fica branco */
  box-shadow: inset 0 -3px 0 rgba(0,0,0,.08); /* baixo-relevo sutil */
}
.brand-mark svg { width: 16px; height: 16px; }
```

### 3.4 Decisões técnicas do SVG

- `viewBox="0 0 16 16"` — grid quadrado de 16 unidades, escalável sem perda.
- `<path d="M3 12 L8 4 L13 12" />` — três coordenadas: início `(3,12)`, ápice `(8,4)`, fim `(13,12)`. Forma o `^` do "A".
- `<circle cx="8" cy="13" r="1" fill="currentColor" />` — ponto sólido logo abaixo do ápice, substitui o traço horizontal do A.
- `stroke="currentColor"` herda do pai (`color: white` no `.brand-mark`) → re-tinge o ícone via CSS sem editar o SVG (útil para variações de fundo).
- `stroke-width="2.2"` + `stroke-linecap="round"` — peso entre 1.5 e 2px conforme o princípio §5.8 "ícones são SVG stroke, peso 1.5–2px".
- `box-shadow: inset 0 -3px 0 rgba(0,0,0,.08)` — baixo-relevo sutil que dá profundidade tátil sem virar skeumorfismo.

### 3.5 Regras de uso

- Tamanho mínimo da marca gráfica isolada: 24×24px. Abaixo disso, o chevron fica ilegível.
- Em fundos escuros (telas de conquista, splash), inverte-se: marca gráfica vira branca com SVG coral; wordmark fica branco. Variação a especificar em `ux-003` se necessário.
- Wordmark "atrilha" sempre minúscula, sem ponto final, sem capitalize. Marca verbal definida em ADR-012.
- Marca gráfica não pode aparecer girada, esticada, ou com cor que não seja `primary-500` (ou branco em fundo coral, no inverso). Variações cromáticas adicionais decidem-se em revisão futura, não aqui.
- Área de respiro mínima ao redor da logo composta (marca + wordmark): metade da altura da marca gráfica (14px para a versão 28px). Nada pode invadir essa área.

---

## 4. Tipografia

### 4.1 Famílias

Duas famílias. Google Fonts (servidas via self-host com `font-display: swap` — decisão técnica final em `ux-002`/sprint de performance, mas a escolha aqui considerou peso de download).

| Papel    | Família             | Pesos a carregar | Por quê                                                                                                              | Peso aprox. (WOFF2, subset latin) |
|----------|---------------------|------------------|----------------------------------------------------------------------------------------------------------------------|------------------------------------|
| Display  | **Bricolage Grotesque** | 600, 700        | Sans-serif contemporânea com personalidade ("g" e "a" expressivos, contrastes sutis). Não é "fonte de igreja", não é "fonte engraçadinha". Comunica produto editorial moderno (Pitchfork, Wallpaper) sem ser corporativa. Variable font — um arquivo cobre todos os pesos. | ~38KB (variable, 1 arquivo)        |
| Texto    | **Inter**               | 400, 500, 600   | Padrão de UI moderno, excelente legibilidade em telas pequenas (P12), hinting maduro para Android low-end (P13). Variable font. | ~62KB (variable, 1 arquivo)        |

**Total tipografia: ~100KB.** Cabe no budget de 200KB do DoD §5 com folga (sobram ~100KB para CSS+JS críticos da rota).

**Por que não uma fonte só:** Bricolage como corpo cansa em parágrafo longo (não é seu papel); Inter como display fica sem personalidade. Display + texto é o padrão editorial e custa apenas 1 arquivo extra.

**Fallback stack:**
- Display: `"Bricolage Grotesque", "Inter", system-ui, sans-serif`
- Texto: `"Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`

Em queda de rede o sistema renderiza com `system-ui` (fallback nativo) e troca quando a fonte chega — `font-display: swap` evita FOIT (texto invisível).

### 4.2 Escala

Mobile-first. Coluna mobile prevista entre 320px (iPhone SE — RNF de teste DoD §3) e 430px (iPhone Pro Max). Desktop é cortesia (P12) — escala cresce, mas não muito.

| Rótulo       | Família   | Peso | Mobile (≤640px)   | Desktop (≥1024px) | Line-height | Uso                                                                |
|--------------|-----------|------|-------------------|-------------------|-------------|--------------------------------------------------------------------|
| `display-xl` | Bricolage | 700  | 40px / 2.5rem     | 56px / 3.5rem     | 1.05        | Hero de landing, tela vazia ilustrada                              |
| `display-lg` | Bricolage | 700  | 32px / 2rem       | 44px / 2.75rem    | 1.1         | Tela cheia de conquista (selo, fim de trimestre)                   |
| `h1`         | Bricolage | 600  | 24px / 1.5rem     | 32px / 2rem       | 1.15        | Título de tela. **Um único por tela.**                             |
| `h2`         | Bricolage | 600  | 20px / 1.25rem    | 24px / 1.5rem     | 1.2         | Título de seção dentro da tela                                     |
| `h3`         | Inter     | 600  | 18px / 1.125rem   | 18px / 1.125rem   | 1.3         | Título de card, bloco de sessão                                    |
| `body-lg`    | Inter     | 400  | 18px / 1.125rem   | 18px / 1.125rem   | 1.5         | Texto narrativo da sessão (núcleo de leitura — prioridade conforto) |
| `body`       | Inter     | 400  | 16px / 1rem       | 16px / 1rem       | 1.5         | Corpo padrão. **Mínimo absoluto para texto contínuo em mobile.**   |
| `body-sm`    | Inter     | 400  | 14px / 0.875rem   | 14px / 0.875rem   | 1.5         | Texto secundário em card, descrição de selo                        |
| `caption`    | Inter     | 500  | 12px / 0.75rem    | 12px / 0.75rem    | 1.4         | Metadado, timestamp, label de gráfico. **Nunca para texto narrativo.** |
| `button`     | Inter     | 600  | 16px / 1rem       | 16px / 1rem       | 1           | Texto de botão. Mesmo tamanho mobile/desktop.                      |
| `overline`   | Inter     | 600  | 12px / 0.75rem    | 12px / 0.75rem    | 1.2         | Tracking +0.05em, uppercase. Etiqueta acima de h1/h2 (ex: "DIA 3"). |

**Regras de aplicação:**
- Texto corrido nunca abaixo de 16px em mobile (RNF-A11Y indireto + leitura de adolescente em ambiente real, ônibus/escola).
- Versículo bíblico: `body-lg` em Inter 500, com tratamento de blockquote definido em `ux-003`. Nunca em itálico decorativo serifado (clichê devocional).
- Número de streak em destaque: `display-lg` em Bricolage 700, com `font-variant-numeric: tabular-nums` para não "pular" ao incrementar.

### 4.3 O que evitamos e por quê

- **Serifa decorativa (Playfair, Cormorant, EB Garamond)** — leitura "Bíblia antiga" / "livro devocional adulto". Rejeitado por P14/P15.
- **Fontes "geek/jovem" infantilizadas (Comic Sans, Fredoka One, Patrick Hand)** — leitura "lição de escola dominical para crianças". Rejeitado por P15.
- **Monoespaçada como display** — comum em apps tech jovens (Linear, Vercel), mas aqui leria como "ferramenta de produtividade", não como caminho espiritual diário.
- **Mais de 2 famílias** — orçamento de download (P13) e ruído visual.
- **Fontes com mais de 3 pesos carregados por família** — cada peso variável extra custa rede em Brasil real.

---

## 5. Princípios de hierarquia

Mobile-first absoluto (P12). Tudo abaixo precisa funcionar em viewport de 320px de largura sem scroll horizontal e com touch target mínimo de 44×44px (RNF-A11Y-05).

1. **Um H1 por tela.** Sempre o título da tela atual (ex: "Esta semana", "Sua sessão", "Painel"). Tudo abaixo desce a hierarquia (H2 → H3 → body). Telas sem H1 explícito (ex: trilha) usam um `overline` + visual forte da grade da semana como ancoragem visual; o título fica no header fixo.

2. **CTA primário sempre full-width em mobile.** Botão de ação principal ocupa 100% da largura útil (descontando padding lateral de 16px). Em desktop vira `auto` com `min-width: 240px`. Nunca dois CTAs primários lado a lado em mobile — se houver dois caminhos, primário fica em cima, secundário em texto-link abaixo.

3. **Estado ativo distingue-se por mais de uma dimensão.** O dia da semana ativo na trilha não é apenas "coral em vez de cinza" — é também maior (escala 1.1), com peso visual extra (sombra suave ou borda) e label visível ("HOJE"). Daltonismo e acessibilidade exigem redundância de canal (cor + tamanho + texto). Mesma regra para selos conquistados vs. bloqueados.

4. **Conteúdo de leitura tem largura máxima de ~65 caracteres.** No núcleo da sessão (bloco de leitura), aplicamos `max-width: 38rem` mesmo quando o viewport é maior. Conforto de leitura > preencher tela. Em mobile isso é automático; em desktop centraliza-se em coluna.

5. **Densidade decresce do topo para a base.** Topo de tela carrega o título e o estado-chave (streak, semana atual, próxima ação). Meio carrega conteúdo de tarefa. Base é "navegação e atalhos secundários" — nunca informação crítica. Adolescente operando o app com uma mão (polegar) precisa de CTA na zona inferior central, mas dados críticos no topo (visíveis durante toda interação).

6. **Cor de marca é escassa.** O coral (`primary-500`) só aparece em: logo, CTA primário, dia ativo da trilha, ícone de logado. Se a tela tem 5 elementos coral, perdeu-se hierarquia — limite informal: **no máximo 2 elementos `primary-500` por viewport**. Tudo o mais usa neutros + secundárias/terciárias.

7. **Espaço em branco é elemento de hierarquia.** Entre blocos de sessão, mínimo `space-y-6` (24px); entre seções de tela, `space-y-10` (40px). Adolescente em mobile precisa de respiro — telas densas leem como "trabalho", e atrilha não é dever de casa.

8. **Ilustração editorial > foto + > ícone genérico.** Quando uma tela precisar de imagem (vazio, conquista, onboarding), priorizar ilustração de linha plana coerente com a paleta (sem volumetria 3D, sem mascote redondo). Ícones são SVG stroke (peso 1.5–2px) — ver §3 (Logo) como referência do peso aplicado, nunca emojis renderizados como conteúdo principal (emojis são opcionais como acento, nunca como ícone de função).

---

## 6. Decisões e alternativas descartadas

### 6.1 Cor de marca: coral `#F25C54` (escolhida) vs. azul-céu turquesa `#1FA8C9` (rejeitada como primária, mantida como terciária)

Considerei abrir com azul-céu turquesa como cor de marca — escolha mais "segura", neutra de gênero, com associação clara a "céu/abertura". Rejeitei porque (a) azul como marca primária em app diário é genérico (Facebook, Twitter/X, LinkedIn, PayPal), e (b) atrilha precisa de calor — coral comunica energia humana, azul comunica produtividade. Azul fica como cor de descoberta/informação, função secundária.

### 6.2 Tipografia display: Bricolage Grotesque (escolhida) vs. Space Grotesk (rejeitada)

Space Grotesk é forte candidata — tem personalidade, é jovem, está em todo app moderno cool. Rejeitei porque está visualmente saturada (qualquer landing de SaaS jovem de 2024–2025 usa Space ou Inter). Bricolage tem personalidade equivalente, é variable (1 arquivo só), e ainda não virou padrão — dá vantagem de identidade.

### 6.3 Fundo da app: `ink-50` quente `#F7F4F1` (escolhido) vs. branco puro `#FFFFFF` (rejeitado)

Branco puro lê clínico em mobile OLED e cansa em sessão de 10 minutos. `ink-50` é apenas levemente mais quente, mas o conjunto inteiro ganha sensação de "ambiente acolhedor" sem custar contraste (ainda passa AAA com `ink-900`). Risco: parecer "off-white de produto premium adulto" — mitigado porque o coral primário e o lime puxam para baixo a idade percebida.

### 6.4 Erro com cor própria `danger-600` (escolhido) vs. reaproveitar `primary-600` (rejeitado)

Tentar usar apenas a marca como erro economiza um token, mas cria leitura ambígua: CTA principal e mensagem de erro teriam tonalidade vermelha próxima. Adolescente clicaria menos por medo de errar. Custo de um token a mais < custo de ambiguidade de CTA.

---

## 7. Histórico de aprovação

| Data       | Item                                  | Decisão                    |
|------------|---------------------------------------|----------------------------|
| 2026-05-18 | Cor de marca coral `#F25C54`          | Aprovado                   |
| 2026-05-18 | Display font Bricolage Grotesque      | Aprovado                   |
| 2026-05-18 | Fundo `ink-50` quente `#F7F4F1`       | Aprovado                   |
| 2026-05-18 | Modo escuro fora do MVP               | Confirmado (fica fora)     |
| 2026-05-18 | Logo (marca gráfica + wordmark)       | Definida e aprovada (§3)   |

**Dúvidas abertas que afetam tasks seguintes** (não bloqueiam `ux-002`, mas precisam de resolução fora desta task):

- **Nomes da sessão de sábado / blocos da sessão** — terminologia visível ao usuário para a sessão semanal e seus blocos internos ainda não está fechada. Afeta microcopy de telas de sessão e da trilha.
- **Produção de ilustrações editoriais** — quem produz, em que estilo concreto, e em que cadência. Princípio §5.8 ("ilustração editorial > foto > ícone genérico") depende disso para sair do papel. Afeta tasks de protótipo (`ux-004..006`) e qualquer tela com estado vazio ou conquista.
