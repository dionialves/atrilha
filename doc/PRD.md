# atrilha — Product Requirements Document (PRD)

> Documento de requisitos do produto **atrilha**, derivado do documento de contexto v1.0 (2026-05-16).
> Este PRD é a fonte da verdade para escopo, requisitos e critérios de aceite do MVP v1.

| Campo | Valor |
|---|---|
| Produto | **atrilha** |
| Versão do documento | 1.1 |
| Data | 2026-05-16 |
| Autor | Dioni (Zayt) |
| Status | Em revisão — base para decomposição em User Stories |
| Público da PRD | Founder/dev solo (operacional, técnico) |
| Escopo | MVP v1 detalhado; v2+ como roadmap |
| Documento de origem | `contexto.md` (v1.0, 2026-05-16) |

### Changelog

**v1.1 — 2026-05-16**
- Tempo-alvo de sessão atualizado de 5–7 min para **até 10 min** (RNF-PERF-05, jornadas J1/J3, seções 1.1 e 1.3).
- Nomes editoriais das sessões (Teaser, Roteiro Original, Zoom, Making Of, Extras, Panorâmica, OFF, Boss) **removidos** — eram nomes da Lição oficial. atrilha é releitura, terá nomes próprios a definir em doc complementar.
- Princípios visuais ajustados: identidade **clara, jovem, colorida**; **sem tema escuro/evocação de trevas** (P14 reformulado, ADR-013 adicionado).
- Mecânicas interativas (E5) e estrutura de bloco de sessão (E4) marcadas como **rascunho a refinar** em docs complementares.
- Sistema de XP/recompensas (E6) marcado como **alto nível**; loop completo de gamificação vai em doc complementar.
- **Compartilhamento social via WhatsApp/Instagram entra no MVP** — novo Épico 11 (selos, marcos de streak, versículos memorizados, convite). Antes estava no roadmap v1.1.
- Lista de **docs complementares a criar** adicionada ao Anexo B.
- ADR-013 (visual claro), ADR-014 (compartilhamento social no MVP) registrados.
- **NÃO alterado:** RF-E2-11 (vinculação obrigatória ao responsável). Após discussão, mantida a obrigação por implicação LGPD para menores.

**v1.0 — 2026-05-16** — versão inicial.

---

## Sumário

1. [Visão e problema](#1-visão-e-problema)
2. [Objetivos e métricas de sucesso](#2-objetivos-e-métricas-de-sucesso)
3. [Personas e público-alvo](#3-personas-e-público-alvo)
4. [Princípios de produto (não-negociáveis)](#4-princípios-de-produto-não-negociáveis)
5. [Jornadas-chave](#5-jornadas-chave)
6. [Escopo do MVP v1](#6-escopo-do-mvp-v1)
7. [Requisitos funcionais por épico](#7-requisitos-funcionais-por-épico)
8. [Requisitos não-funcionais](#8-requisitos-não-funcionais)
9. [Arquitetura técnica de alto nível](#9-arquitetura-técnica-de-alto-nível)
10. [Modelo de dados (alto nível)](#10-modelo-de-dados-alto-nível)
11. [Segurança, privacidade e LGPD](#11-segurança-privacidade-e-lgpd)
12. [Conteúdo: produção, pipeline e direitos](#12-conteúdo-produção-pipeline-e-direitos)
13. [Métricas e instrumentação](#13-métricas-e-instrumentação)
14. [Roadmap pós-MVP](#14-roadmap-pós-mvp)
15. [Riscos e mitigações](#15-riscos-e-mitigações)
16. [Registro de decisões (ADRs)](#16-registro-de-decisões-adrs)
17. [Critérios de aceite globais (Definition of Done)](#17-critérios-de-aceite-globais-definition-of-done)
18. [Glossário](#18-glossário)
19. [Anexo A — Resolução das questões em aberto do contexto](#anexo-a--resolução-das-questões-em-aberto-do-contexto)
20. [Anexo B — Próximos passos imediatos](#anexo-b--próximos-passos-imediatos-após-aprovação-desta-prd)

---

## 1. Visão e problema

### 1.1 Visão

**atrilha transforma a Lição da Escola Sabatina Juvenil em uma trilha gamificada que adolescentes adventistas completam por conta própria — em até 10 minutos por dia, com a profundidade doutrinária preservada e a fricção familiar eliminada.**

### 1.2 Problema

A Lição Juvenil oficial (impressa e em PDF/app DSA) é bem produzida visualmente e doutrinariamente sólida, mas:

- **Para o adolescente:** é estática, sem feedback imediato, sem mecânica de progresso visível, sem mobilidade real. Compete por atenção com TikTok, Instagram e Spotify — e perde.
- **Para o pai:** vira fonte recorrente de tensão familiar ("você fez a lição?", "vai fazer?"). O pai não tem visibilidade fora da pergunta direta, que é exatamente o que afasta o adolescente.
- **Para a igreja:** a Escola Sabatina Juvenil tem evasão progressiva — adolescentes chegam ao sábado sem terem aberto a lição na semana.

O problema **não é o conteúdo**. É o **formato e o modelo de engajamento**.

### 1.3 Solução proposta

Um Progressive Web App (PWA) que reaproveita o calendário oficial da Lição Juvenil (público, da Conferência Geral) e reembala didaticamente cada semana como uma trilha de 7 sessões diárias gamificadas, com:

- Sessões diárias de até 10 minutos com estrutura cinematográfica (nomes próprios a definir em doc complementar — substituem os nomes da Lição oficial, pois atrilha é uma releitura).
- Mecânicas interativas (quiz, drag-and-drop, caça-palavras, memorização progressiva) — em rascunho, a refinar.
- Sistema de XP, streak diário e selos — alto nível neste doc; loop completo de gamificação em doc complementar.
- Painel dos pais que mostra sinais positivos, sem expor reflexões privadas nem horários de uso.
- Compartilhamento social de conquistas via WhatsApp e Instagram (selos, marcos de streak, versículos memorizados, link de convite).
- Identidade visual **clara, jovem e colorida** — sem evocação de trevas/escuridão; sem estética cristã genérica; sem infantilização. Paleta e fontes a definir em doc complementar.

### 1.4 Por que agora

- Nenhum produto atende a interseção: Lição Juvenil + gamificação real + painel pais + doutrina ASD preservada + estética jovem.
- A liderança ASD não vai construir isto: o app oficial DSA é um leitor de revista, não um produto gamificado, e a estrutura institucional não prioriza produto de software ágil.
- Stack técnica acessível (Spring + PWA) viabiliza solo founder com produção semanal sustentável.

### 1.5 Não-objetivos do produto

atrilha **não é**:

- Substituto da Escola Sabatina presencial.
- Substituto da revista oficial — é complemento didático.
- Plataforma de devocional adulto.
- Rede social adventista.
- Ferramenta institucional (não é vendido para igrejas; é B2C).

---

## 2. Objetivos e métricas de sucesso

### 2.1 Objetivos da v1 (primeiros 90 dias após launch)

| # | Objetivo | Métrica-alvo |
|---|---|---|
| O1 | Validar engajamento adolescente | DAU/MAU ≥ 35% nos usuários cadastrados |
| O2 | Validar formato semanal | ≥ 40% dos usuários ativos completam ≥ 1 semana inteira |
| O3 | Validar painel pais como não-vigilante | ≥ 60% dos pais cadastrados retornam ao painel ≥ 1×/semana |
| O4 | Validar produção sustentável | Estoque de conteúdo nunca cai abaixo de 4 semanas adiante |
| O5 | Aquisição inicial | 200 adolescentes cadastrados nos 90 dias |

### 2.2 North Star Metric

**% de adolescentes cadastrados que mantêm streak ≥ 7 dias.**

Esta métrica captura simultaneamente engajamento (uso diário), retenção (consistência) e fit do produto (se a mecânica funciona). Alvo nos 90 dias: **25%**.

### 2.3 Métricas secundárias

Detalhadas na [seção 13](#13-métricas-e-instrumentação).

### 2.4 Sinais de falha (kill criteria)

Se aos 90 dias nenhuma destas condições for atingida, repensar premissa de produto antes de investir em v2:

- DAU/MAU < 15%
- Nenhum usuário com streak ≥ 14 dias
- < 30% dos adolescentes completam sequer 1 semana inteira

---

## 3. Personas e público-alvo

### 3.1 Persona primária — Júlia, 14 anos

- Filha de família adventista praticante.
- Smartphone Android próprio com WhatsApp, TikTok e Instagram.
- Faz a Lição Juvenil "quando lembra ou quando o pai insiste".
- Lê pouco em formato longo; consome conteúdo em micro-doses.
- Tem amigas adventistas e não-adventistas; não quer parecer "diferente demais".
- Valoriza: autonomia, estética, não ser tratada como criança.
- Rejeita: moralismo, comparação culposa, infantilização, vigilância parental disfarçada.

**Objetivo dela no produto:** terminar a sessão do dia rapidamente, sentir progresso, não ter o pai enchendo, eventualmente memorizar o versículo sem perceber que está estudando.

### 3.2 Persona secundária — Carlos, 44 anos (pai da Júlia)

- Adventista praticante, vai à igreja com a família.
- Quer que a filha mantenha vínculo com a fé sem forçar.
- Atualmente vive o ciclo "perguntar → ela ignorar → insistir → conflito".
- Tem smartphone, usa WhatsApp e e-mail diariamente.
- Não é tech-savvy avançado — espera UX simples e clara.

**Objetivo dele no produto:** saber que ela está fazendo, ter assunto para conversar no sábado, parar de ser o cobrador.

### 3.3 Faixa etária do produto

- **Adolescente:** **13–17 anos** (alinhado com LGPD para menores com consentimento parental).
- **Responsável:** **18+ anos**, vinculado a pelo menos 1 perfil adolescente.

### 3.4 Anti-personas (quem o produto não atende na v1)

- Crianças < 13 anos (existe lição Primários e Juniores oficial; foco perde se diluir).
- Adultos buscando estudo da Lição Adulto.
- Pais que querem ferramenta de vigilância (o produto recusa esse modelo).
- Igrejas/professores de Escola Sabatina querendo gerenciar turma (não é B2B na v1).

---

## 4. Princípios de produto (não-negociáveis)

Estes princípios têm precedência sobre qualquer requisito ou solicitação de feature subsequente. Toda decisão deve ser auditada contra eles.

### 4.1 Princípios editoriais

- **P1.** Sem moralismo. Sem "joia preciosa", "guerreiro do Senhor", "tem que ser como X".
- **P2.** Sem comparação culposa entre o usuário e personagens bíblicos.
- **P3.** Sem ameaça espiritual ou teologia da prosperidade.
- **P4.** Sem infantilização — linguagem respeita inteligência adolescente.
- **P5.** Permite mencionar medo, dúvida, vergonha, ansiedade como experiências legítimas.
- **P6.** Doutrina ASD preservada integralmente: sábado, segunda vinda, estado dos mortos inconsciente, santuário, profecia.

### 4.2 Princípios de produto

- **P7.** Erro ensina, não pune. Quiz errado mostra explicação; sem sistema de "vidas" que esgotam.
- **P8.** Privacidade do adolescente é sagrada. Reflexões pessoais são privadas por padrão; opt-in por reflexão individual, nunca global.
- **P9.** Painel dos pais mostra sinais positivos; nunca expõe horário de uso, erros, tempo gasto ou ausências como métrica negativa.
- **P10.** Sem ranking obrigatório com estranhos. Gamificação é contra si mesmo, não contra outros.
- **P11.** Sem dark patterns: nenhuma mecânica que gere ansiedade ou FOMO artificial.

### 4.3 Princípios técnicos

- **P12.** Mobile-first absoluto. Desktop é cortesia.
- **P13.** Funciona em conexão ruim (Brasil real). Bundle inicial < 200KB, sessão diária carrega offline após primeiro acesso.
- **P14.** Identidade visual **clara, jovem e colorida**. Sem tema escuro como direção default; sem evocação de trevas, sombra ou pesadume. Sem clichês cristãos (madeira, pergaminho, dourado, raios de luz, gradientes púrpura). Paleta e fontes finais a definir em doc complementar de identidade visual.
- **P15.** Sem infantilização visual. Jovem ≠ infantil. Ilustrações editoriais antes de mascotes; tipografia com personalidade antes de fontes "engraçadinhas".

---

## 5. Jornadas-chave

### 5.1 J1 — Adolescente cadastra e faz primeira sessão

1. Júlia recebe link da mãe via WhatsApp: `atrilha.app`
2. Abre no celular, vê landing (1 tela, headline + botão).
3. Toca **"Começar"** → escolhe entre **Login Google** ou **E-mail + senha**.
4. Cadastra perfil mínimo: apelido, data de nascimento, foto (opcional).
5. Sistema detecta < 18 anos → tela "Quem cuida de você precisa autorizar".
6. Gera **código de vinculação de 6 dígitos** (válido por 7 dias).
7. Telas de onboarding (3 telas, swipe): o que é atrilha, como funciona o sábado, privacidade.
8. Cai direto na **trilha da semana atual**, sessão do dia atual destacada.
9. Toca na sessão → executa o fluxo de 5 blocos (gancho → núcleo → quiz → reflexão → fechamento).
10. Recebe XP, vê streak iniciar em 1, vê próximo dia bloqueado com prévia.

**Critério de sucesso da jornada:** Júlia completa a primeira sessão em ≤ 10 minutos sem ajuda, sem precisar do pai vinculado ainda.

### 5.2 J2 — Pai cadastra e vincula

1. Carlos recebe da filha o código de 6 dígitos (via conversa direta ou compartilhar do app).
2. Acessa `atrilha.app/responsavel` (ou clica em link de convite).
3. Cria conta (Google ou e-mail).
4. Insere código → confirma identidade da filha (apelido + foto aparecem).
5. **Aceita termo de consentimento parental** (obrigatório, registrado com timestamp).
6. Onboarding curto (3 telas): "o que você verá", "o que NÃO verá", "como conversar no sábado".
7. Cai no **painel do responsável** com primeira semana da filha.

**Critério de sucesso:** Carlos entende em < 2 minutos que NÃO verá reflexões privadas e que NÃO receberá notificação de "ela não fez hoje".

### 5.3 J3 — Adolescente faz sessão diária recorrente

1. Júlia recebe (futuramente push, no MVP: e-mail) à tarde: "Tua sessão do dia tá esperando — até 10 min, streak: 12 🔥".
2. Abre o app já logado, cai na trilha, sessão do dia já destacada.
3. Faz a sessão (até 10 min), responde quiz, escreve reflexão (privada por default).
4. Vê barra de XP encher, streak passar para 13, prévia do próximo dia.
5. Fecha o app.

**Critério de sucesso:** ciclo recorrente em ≤ 10 minutos, zero fricção de navegação.

### 5.4 J4 — Sábado: sessão de fechamento da semana + conversa familiar

1. Júlia abre sábado de manhã, recebe a **sessão de sábado** (recap + memorização Fase 4 + pergunta família — nome próprio a definir em doc complementar).
2. Digita o versículo de memória → recebe % de acurácia.
3. Vê selo da semana conquistado, animação Lottie celebra.
4. Sistema gera **pergunta de discussão familiar** (que aparece também no painel do Carlos).
5. À tarde, Júlia e Carlos têm assunto natural para conversar.

**Critério de sucesso:** ≥ 60% dos pares pai-filho com semana completa conversam pelo menos sobre o tema.

### 5.5 J5 — Pai verifica painel (semanal)

1. Carlos abre `atrilha.app/painel` no celular.
2. Vê: heatmap da semana, streak atual da filha, tema da semana, pergunta de discussão.
3. NÃO vê: respostas de reflexão, horários de uso, % acerto em quiz.
4. Marca pergunta como "conversamos sobre isso" → registra timestamp (analytics interna).

**Critério de sucesso:** Carlos sente que o painel é útil sem ser vigilante.

---

## 6. Escopo do MVP v1

### 6.1 Dentro do escopo (must have)

1. **Cadastro e autenticação** — Google OAuth + e-mail/senha, para adolescentes (13–17) e responsáveis (18+).
2. **Vinculação adolescente ↔ responsável** — via código de 6 dígitos gerado pelo adolescente.
3. **Consentimento parental** — fluxo legal de aceite com registro auditável.
4. **Trilha visual da semana atual** — acesso à semana presente + semanas anteriores do trimestre vigente.
5. **Sessão diária completa** — fluxo de blocos editoriais (gancho → núcleo → quiz → reflexão → fechamento). *Estrutura preliminar; doc complementar refinará blocos e ritmo.*
6. **Mecânicas de quiz** — múltipla escolha, V/F, completar frase, ordenar acontecimentos. *Em rascunho; a refinar.*
7. **Mecânica drag-and-drop** — completar versículo. *Em rascunho; a refinar.*
8. **Caça-palavras temático** — 9×9, palavras-chave da história. *Em rascunho; a refinar.*
9. **Memorização progressiva do versículo** — 4 fases ao longo da semana. *Mecânica em rascunho; a refinar.*
10. **Sistema de XP** — pontuação por ação. *Alto nível neste doc; loop completo de gamificação e uso do XP em doc complementar.*
11. **Streak diário** — com 1 escudo de proteção por semana.
12. **Selos** — por semana completa e por trimestre completo.
13. **Heatmap de progresso** — visual estilo GitHub contributions.
14. **Versículos memorizados** — repositório pessoal do adolescente.
15. **Painel dos pais** — heatmap, streak, tema da semana, pergunta familiar, versículos memorizados.
16. **Notificações por e-mail** — convite de vinculação, sábado de boas-vindas à nova semana, parabéns por semana completa.
17. **Conteúdo:** 1 trimestre completo (13 semanas) ao vivo + 1 trimestre em buffer pronto.
18. **PWA instalável** — manifest, service worker, ícone home screen.
19. **Privacidade:** reflexões privadas por default, opt-in por item.
20. **Solicitação de exportação de dados** (LGPD) — formulário simples; export manual pelo admin no MVP.
21. **Compartilhamento social** — adolescente e responsável podem compartilhar conquistas (selos, marcos de streak, versículos memorizados) e link de convite via WhatsApp e Instagram. Imagens geradas dinamicamente, sem expor dados privados.

> **Nota sobre docs complementares pendentes:** alguns itens acima estão em alto nível propositalmente. Os seguintes documentos serão criados separadamente antes da implementação correspondente:
> - **Doc de estrutura da semana e blocos de sessão** (refina item 5).
> - **Doc de mecânicas interativas** com especificação visual e de interação (refina itens 6–9).
> - **Doc de sistema de XP, gamificação e recompensas** — define para que serve o XP, níveis, loop de progressão (refina item 10).
> - **Doc de identidade visual e protótipo** — paleta, tipografia, componentes, mockups das telas principais.
> - **Doc de naming das sessões** — substitui os nomes da Lição oficial por nomes próprios da atrilha.

### 6.2 Fora do escopo da v1 (explicitamente)

- Pagamento, planos premium, cobrança.
- Múltiplos perfis de filhos por conta de responsável.
- Funcionalidades sociais (amigos, ranking, comparação).
- Áudio das sessões (TTS ou narração).
- App nativo (Capacitor).
- Vídeos curtos por sessão.
- Grupos privados de devocional.
- Notificações push (vai como fast-follow v1.1; ver ADR-002).
- Exportação self-service de dados (vai em v2).
- Modo offline robusto além do cache PWA básico de assets.
- Internacionalização (apenas pt-BR na v1).
- Suporte a múltiplas traduções bíblicas (apenas ARC na v1; ver ADR-008).

### 6.3 Cronograma de referência (8 semanas)

Mantém o cronograma do contexto (seção 7.3). Detalhamento de sprints será feito na decomposição em User Stories após aprovação desta PRD.

| Semana | Foco |
|---|---|
| 1 | Modelagem de dados + setup projeto + cadastro/login Google e e-mail |
| 2 | Trilha visual + estrutura base de sessão diária |
| 3 | Sistema de quiz (múltipla, V/F, completar, ordenar) + XP/streak |
| 4 | Mecânicas especiais (caça-palavras, drag versículo, memo progressiva) |
| 5 | Vinculação adolescente-pai + dashboard pais + consentimento |
| 6 | Produção de conteúdo (13 semanas) + importador YAML→DB |
| 7 | Onboarding + e-mails transacionais + polimento UX |
| 8 | Testes com 3–5 famílias + ajustes + soft launch |

---

## 7. Requisitos funcionais por épico

> Nomenclatura: cada requisito tem ID `RF-Exx-yy` (épico-sequência). `MUST` = obrigatório no MVP; `SHOULD` = importante mas não bloqueia; `MAY` = nice-to-have.

### Épico 1 — Cadastro e autenticação (E1)

**Objetivo:** permitir que adolescentes e responsáveis criem conta com fricção mínima e identidade clara.

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E1-01 | O sistema **MUST** oferecer login via Google OAuth 2.0 (`openid`, `email`, `profile`). | MUST |
| RF-E1-02 | O sistema **MUST** oferecer cadastro por e-mail e senha (mínimo 8 caracteres, política simples). | MUST |
| RF-E1-03 | O sistema **MUST** distinguir dois tipos de conta na criação: **adolescente** ou **responsável**. | MUST |
| RF-E1-04 | O sistema **MUST** coletar data de nascimento e calcular idade automaticamente. | MUST |
| RF-E1-05 | O sistema **MUST** bloquear cadastro de adolescente < 13 anos com mensagem clara. | MUST |
| RF-E1-06 | O sistema **MUST** bloquear cadastro de responsável < 18 anos. | MUST |
| RF-E1-07 | O sistema **MUST** enviar e-mail de verificação para contas criadas com e-mail/senha. | MUST |
| RF-E1-08 | O sistema **MUST** permitir recuperação de senha via e-mail (link com expiração 1h). | MUST |
| RF-E1-09 | O sistema **MUST** registrar timestamp de último login. | MUST |
| RF-E1-10 | O perfil do adolescente **MUST** conter: apelido (3–20 chars), data nascimento, avatar (opcional, upload ou inicial). | MUST |
| RF-E1-11 | O perfil do responsável **MUST** conter: nome, e-mail verificado. | MUST |
| RF-E1-12 | O sistema **SHOULD** permitir logout em todos os dispositivos a partir do perfil. | SHOULD |

**Critérios de aceite (E1):**
- Adolescente consegue criar conta e chegar à trilha em ≤ 90 segundos.
- Tentativa de cadastro < 13 anos retorna mensagem explicativa, sem expor por que.
- Reset de senha funciona em < 2 minutos do clique no link.

---

### Épico 2 — Vinculação adolescente ↔ responsável (E2)

**Objetivo:** vincular legalmente um responsável a cada perfil adolescente, com fluxo iniciado pelo adolescente (ver ADR-004).

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E2-01 | Após cadastro, adolescente **MUST** receber um **código de vinculação de 6 dígitos** alfanumérico (sem caracteres ambíguos: 0/O, 1/I/L). | MUST |
| RF-E2-02 | O código **MUST** ter validade de **7 dias corridos**. | MUST |
| RF-E2-03 | O adolescente **MUST** poder regerar o código a qualquer momento (invalida o anterior). | MUST |
| RF-E2-04 | O adolescente **MUST** poder compartilhar o código via botão nativo (`navigator.share`) ou copiar. | MUST |
| RF-E2-05 | O responsável **MUST** poder inserir o código durante seu cadastro ou depois. | MUST |
| RF-E2-06 | Ao inserir código válido, sistema **MUST** exibir apelido + avatar do adolescente para confirmação. | MUST |
| RF-E2-07 | Responsável **MUST** aceitar explicitamente o termo de consentimento parental antes da vinculação se concretizar. | MUST |
| RF-E2-08 | Vinculação **MUST** registrar: timestamp, IP, user agent, hash do termo aceito. | MUST |
| RF-E2-09 | Adolescente **MUST** receber e-mail de notificação quando vinculação ocorrer. | MUST |
| RF-E2-10 | Adolescente **MUST** poder **revogar a vinculação** a qualquer momento (logout do pai do painel; dados do adolescente preservados). | MUST |
| RF-E2-11 | Conta de adolescente **sem responsável vinculado em 7 dias** **MUST** ter funcionalidade limitada (somente semana atual; bloqueia trilha completa até vincular). | MUST |
| RF-E2-12 | Sistema **MUST** permitir 1 responsável por adolescente no MVP (múltiplos vão em v2). | MUST |
| RF-E2-13 | Sistema **MUST** permitir que 1 responsável esteja vinculado a apenas 1 adolescente no MVP (múltiplos filhos por conta vai em v2). | MUST |

**Critérios de aceite (E2):**
- Pai consegue vincular em ≤ 2 minutos a partir do recebimento do código.
- Adolescente sem pai vinculado vê tela explicativa após 7 dias, não erro genérico.
- Revogar vinculação não apaga progresso do adolescente.

---

### Épico 3 — Trilha e navegação (E3)

**Objetivo:** apresentar a estrutura visual do trimestre/semana/dia de forma que o adolescente sempre saiba "onde está" e "o que vem".

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E3-01 | A tela principal **MUST** ser a **trilha da semana atual** (sábado a sábado). | MUST |
| RF-E3-02 | A trilha **MUST** exibir 7 nós (um por dia), com estado visual: bloqueado, disponível, em progresso, concluído. | MUST |
| RF-E3-03 | Dias **MUST** ficar bloqueados até sua data nominal (ex.: sessão de quarta libera 00:00 de quarta no fuso do usuário). | MUST |
| RF-E3-04 | O adolescente **MUST** poder fazer sessões de dias passados da semana corrente (sem perder o bloqueio futuro). | MUST |
| RF-E3-05 | A **sessão de sábado** (fechamento semanal) **MUST** estar bloqueada até que ≥ 5 das 6 sessões anteriores estejam concluídas. | MUST |
| RF-E3-06 | Sistema **MUST** permitir navegar para **semanas anteriores do trimestre corrente** com indicador "concluída", "parcial", "não iniciada". | MUST |
| RF-E3-07 | Sistema **MUST** mostrar visão de trimestre (13 semanas) acessível em ≤ 2 toques. | MUST |
| RF-E3-08 | Trimestres anteriores **SHOULD NOT** estar acessíveis na v1 (vão para Premium em v2). | SHOULD |
| RF-E3-09 | A trilha **MUST** indicar o tema da semana com título + ilustração de cabeçalho. | MUST |
| RF-E3-10 | A trilha **MUST** ter ancoragem visual ao "dia de hoje" (scroll/highlight automático). | MUST |

**Critérios de aceite (E3):**
- Adolescente abre o app e em ≤ 1 toque entra na sessão do dia.
- Visual de trilha funciona em telas 320px-wide (iPhone SE) sem quebra.

---

### Épico 4 — Estrutura da sessão diária (E4)

**Objetivo:** entregar a unidade básica de consumo — uma sessão de até 10 minutos com estrutura fixa de blocos.

> ⚠️ **Em refinamento.** A estrutura de 5 blocos abaixo é o ponto de partida validado pelos protótipos v1/v2. Antes do Sprint 2, será produzido um **doc complementar "Estrutura da semana e dos blocos de sessão"** especificando: nomes próprios das sessões diárias (substituindo os nomes da Lição oficial), ritmo dentro de cada bloco, microcopy padrão, e variações por dia da semana.

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E4-01 | Cada sessão **MUST** seguir a estrutura: **gancho → núcleo → quiz → reflexão → fechamento**. | MUST |
| RF-E4-02 | Bloco **gancho** **MUST** ter ≤ 40 palavras e ≤ 30 segundos de leitura. | MUST |
| RF-E4-03 | Bloco **núcleo** **MUST** ser composto por 3–4 cards swipáveis (contexto, texto bíblico, explicação, ponte aplicação). | MUST |
| RF-E4-04 | Bloco **quiz** **MUST** conter 2 perguntas com explicação após resposta. | MUST |
| RF-E4-05 | Bloco **reflexão** **MUST** apresentar pergunta aberta com textarea (máx 1000 chars), **privada por default**. | MUST |
| RF-E4-06 | Bloco **fechamento** **MUST** exibir: XP ganho, streak atual, prévia textual do próximo dia. | MUST |
| RF-E4-07 | Sessão **MUST** ser identificada por dia da semana com um **nome próprio da atrilha** (substitui os nomes da Lição oficial — Teaser, Roteiro Original, Zoom, Making Of, Extras, Panorâmica, OFF, Boss — que **não serão usados**). Nomes específicos a definir em doc complementar antes do Sprint 2. | MUST |
| RF-E4-08 | Sessão **MUST** salvar progresso a cada bloco (resumível). | MUST |
| RF-E4-09 | Sair no meio da sessão **MUST** preservar respostas já dadas. | MUST |
| RF-E4-10 | Sessão **MUST** ser marcada como concluída apenas após o bloco fechamento. | MUST |
| RF-E4-11 | Sessão **SHOULD** ter indicador de progresso (1/5, 2/5, ...). | SHOULD |
| RF-E4-12 | Resposta de reflexão em branco **MUST** ser aceita (não obrigatória para concluir sessão). | MUST |

**Critérios de aceite (E4):**
- Sessão completa executa em até 10 minutos para usuário médio (alvo de atenção sustentada).
- Fechar e reabrir o app no meio de uma sessão retorna ao bloco onde parou, com dados preservados.

---

### Épico 5 — Mecânicas interativas (E5)

**Objetivo:** entregar as 5 mecânicas validadas no protótipo v2, plus quizzes básicos validados no v1.

> ⚠️ **Em rascunho.** As mecânicas abaixo capturam a intenção a partir dos protótipos, mas detalhes de UX, animação, feedback, parametrização editorial e variações ainda serão refinados em **doc complementar "Mecânicas interativas"**, com mockups por mecânica, antes do Sprint 3. Alguns requisitos abaixo podem ser ajustados ou substituídos nesse refinamento.

#### E5.1 — Quiz múltipla escolha

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E5-01 | Quiz **MUST** suportar 2–5 alternativas, 1 correta. | MUST |
| RF-E5-02 | Após resposta, **MUST** mostrar feedback visual (correto/incorreto) + explicação. | MUST |
| RF-E5-03 | Resposta incorreta **MUST NOT** travar a sessão (sem retentativa obrigatória; explicação ensina). | MUST |
| RF-E5-04 | Sistema **MUST** registrar resposta e timestamp para análise de calibração. | MUST |

#### E5.2 — Quiz verdadeiro/falso

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E5-05 | Apresenta afirmação + dois botões (Verdadeiro / Falso) com feedback após resposta. | MUST |

#### E5.3 — Completar frase

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E5-06 | Frase com lacuna preenchida via pool de palavras (3–6 distratores) com tap ou drag. | MUST |
| RF-E5-07 | Aceita 1 ou mais lacunas por frase. | MUST |

#### E5.4 — Ordenar acontecimentos

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E5-08 | 3–6 cards em ordem aleatória; usuário arrasta para ordem correta. | MUST |
| RF-E5-09 | Feedback após confirmar ordem, com correta destacada. | MUST |

#### E5.5 — Completar versículo (drag-and-drop) — sessão de segunda-feira

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E5-10 | Versículo com 30–50% das palavras-chave removidas, formando slots. | MUST |
| RF-E5-11 | Pool de palavras **MUST** conter as palavras corretas + 2–4 distratores semanticamente próximos. | MUST |
| RF-E5-12 | Usuário pode arrastar palavra para slot e voltar (reversível). | MUST |
| RF-E5-13 | Feedback após confirmar com palavras corretas em verde, incorretas em vermelho destacando posição certa. | MUST |
| RF-E5-14 | Em mobile, **MUST** funcionar com tap (selecionar palavra → tap no slot). | MUST |

#### E5.6 — Caça-palavras — sessão de terça-feira

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E5-15 | Grid **MUST** ser 9×9 dinamicamente gerado. | MUST |
| RF-E5-16 | **MUST** conter 5–8 palavras-chave da história da semana. | MUST |
| RF-E5-17 | Palavras **MUST** poder ficar em horizontal, vertical, diagonal (sem reversa na v1). | MUST |
| RF-E5-18 | Usuário seleciona via drag (tap → drag → release) tanto desktop quanto mobile. | MUST |
| RF-E5-19 | Lista lateral de palavras a encontrar **MUST** ser visível e marcar conforme acha. | MUST |
| RF-E5-20 | Após achar todas, sessão libera o próximo bloco. | MUST |
| RF-E5-21 | Botão **"Dica"** revela 1 letra inicial de palavra ainda não encontrada (custo: -2 XP). | SHOULD |

#### E5.7 — Memorização progressiva do versículo

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E5-22 | Versículo-chave **MUST** aparecer em **4 fases crescentes** ao longo da semana. | MUST |
| RF-E5-23 | **Fase 1 (segunda):** versículo completo + drag-and-drop com distratores (ver RF-E5-10 a RF-E5-14). | MUST |
| RF-E5-24 | **Fase 2 (quarta):** algumas palavras-chave escondidas em blocos cinza; usuário lê em voz alta tentando completar; toca o bloco para revelar a palavra. | MUST |
| RF-E5-25 | **Fase 3 (sexta):** apenas a primeira letra de cada palavra-chave em destaque laranja; resto oculto. | MUST |
| RF-E5-26 | **Fase 4 (sessão de sábado):** textarea vazia; usuário digita o versículo todo de memória. | MUST |
| RF-E5-27 | Fase 4 **MUST** calcular % de acurácia (Levenshtein normalizado, ignorando case e pontuação). | MUST |
| RF-E5-28 | Fase 4 **MUST** ter botão **"Dica"** que mostra iniciais das palavras (mesmo padrão Fase 3). | MUST |
| RF-E5-29 | Fase 4 com acurácia ≥ 90% **MUST** registrar versículo como "memorizado" no repositório pessoal. | MUST |
| RF-E5-30 | Versículos memorizados **SHOULD** retornar para revisão em 3, 7 e 21 dias (spaced repetition). Implementação completa pode ir para v1.1. | SHOULD |

**Critérios de aceite (E5):**
- Cada mecânica funciona em iPhone SE (320px) e desktop sem regressão.
- Tempo de feedback ≤ 200ms após resposta.

---

### Épico 6 — Gamificação (E6)

**Objetivo:** criar reforço positivo sustentável, sem dark patterns.

> ⚠️ **Alto nível neste doc.** Os requisitos abaixo são o esqueleto da gamificação. **Doc complementar "Sistema de XP, gamificação e loop de progressão"** vai detalhar antes do Sprint 3: para que o adolescente *usa* o XP (cosméticos, customização de perfil, desbloqueios não-essenciais), curva de níveis, sistema de selos extras, regras de recuperação de streak, microcopy de celebração, e qualquer mecânica de retorno ao app.

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E6-01 | Sistema de **XP MUST** atribuir pontos por evento: sessão completa (10), acerto quiz (+5), reflexão escrita ≥ 30 chars (+10), sessão de sábado completa (+30). *Valores preliminares; doc complementar de gamificação pode ajustar.* | MUST |
| RF-E6-02 | XP **MUST** ser persistido por adolescente e exibido na trilha. | MUST |
| RF-E6-03 | Sistema **MUST** ter níveis baseados em XP acumulado (curva: nível N requer N×100 XP). | SHOULD |
| RF-E6-04 | **Streak diário MUST** incrementar quando há ≥ 1 sessão completa em dia corrido. | MUST |
| RF-E6-05 | Streak **MUST** reset para 0 após 1 dia sem sessão, **exceto** se o adolescente tiver "escudo de proteção" disponível. | MUST |
| RF-E6-06 | Cada adolescente **MUST** ter 1 escudo de proteção disponível por semana (renova no sábado). | MUST |
| RF-E6-07 | Escudo **MUST** ser consumido automaticamente quando houver falha; notificar adolescente. | MUST |
| RF-E6-08 | **Selo de semana completa MUST** ser concedido ao completar todas as 7 sessões. | MUST |
| RF-E6-09 | **Selo de trimestre MUST** ser concedido ao completar 13 semanas no trimestre. | MUST |
| RF-E6-10 | **Heatmap anual MUST** mostrar cada dia como célula colorida por intensidade: cinza (sem atividade), 3 tons crescentes de laranja. | MUST |
| RF-E6-11 | Heatmap **MUST** ser navegável (tap em dia → mostra resumo da sessão). | SHOULD |
| RF-E6-12 | Sistema **MUST NOT** ter ranking público nem comparação obrigatória com outros usuários. | MUST |
| RF-E6-13 | Sistema **MUST NOT** ter sistema de vidas que esgotam ou forçam compra. | MUST |
| RF-E6-14 | Animações de conquista (Lottie) **MUST** durar ≤ 2 segundos e ser dispensáveis com tap. | MUST |

**Critérios de aceite (E6):**
- Streak não quebra "injustamente" (testes com cenários de fuso horário e madrugada).
- Selos visualmente diferenciados e colecionáveis.

---

### Épico 7 — Painel dos pais (E7)

**Objetivo:** dar ao responsável visibilidade útil sem violar princípios P8 e P9.

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E7-01 | Painel **MUST** mostrar: heatmap da semana corrente, streak atual, tema da semana, pergunta de discussão familiar, versículos memorizados no trimestre. | MUST |
| RF-E7-02 | Painel **MUST NOT** exibir: textos de reflexão (mesmo se opt-in feito? ver RF-E7-08), horários de uso, % acerto em quiz, tempo gasto na sessão, dias específicos sem atividade. | MUST |
| RF-E7-03 | Heatmap no painel pai **MUST** mostrar apenas binário (fez/não fez), sem intensidade. | MUST |
| RF-E7-04 | Pergunta de discussão familiar **MUST** ser gerada pelo conteúdo da semana (campo fixo no YAML). | MUST |
| RF-E7-05 | Responsável **MUST** poder marcar "conversamos sobre isso" (botão simples, registra timestamp). | SHOULD |
| RF-E7-06 | Painel **MUST** ter onboarding inicial (3 telas, primeiro acesso) explicando o que vê e o que NÃO vê e por quê. | MUST |
| RF-E7-07 | Painel **MUST** ter link discreto "Sobre privacidade" sempre visível, com explicação completa. | MUST |
| RF-E7-08 | Reflexões compartilhadas pelo adolescente (opt-in individual) **MUST** aparecer em uma aba separada "Compartilhado por [apelido]", nunca misturadas com o dashboard padrão. | MUST |
| RF-E7-09 | Adolescente **MUST** poder revogar compartilhamento de qualquer reflexão a qualquer momento (some imediatamente do painel). | MUST |
| RF-E7-10 | Notificações ao responsável **MUST** ser positivas-only: "sua filha completou a semana", "tema da próxima semana". **NUNCA**: "sua filha não fez hoje". | MUST |

**Critérios de aceite (E7):**
- 100% dos pais no teste com 3–5 famílias entendem na primeira sessão que NÃO verão reflexões.
- Nenhuma notificação contém linguagem negativa ou de alerta.

---

### Épico 8 — Notificações (E8)

**Objetivo:** lembrar sem irritar, no canal certo para a v1.

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E8-01 | Sistema **MUST** enviar e-mail transacional para: verificação de e-mail, recuperação de senha, código de vinculação, confirmação de vinculação. | MUST |
| RF-E8-02 | Sistema **MUST** enviar **lembrete diário ao adolescente** (e-mail) configurável: horário padrão 18h00 do fuso do usuário; pode desativar/mudar horário. | MUST |
| RF-E8-03 | Lembrete diário **MUST** ser suprimido se o adolescente já completou a sessão do dia. | MUST |
| RF-E8-04 | Sistema **MUST** enviar **e-mail de sábado** ao adolescente: "tema da semana que começa hoje". | MUST |
| RF-E8-05 | Sistema **MUST** enviar **e-mail ao responsável quando filha completar a semana** (sábado à noite). | MUST |
| RF-E8-06 | Sistema **MUST** enviar **e-mail ao responsável no sábado** com tema + pergunta de discussão da semana que começa. | MUST |
| RF-E8-07 | Sistema **MUST NOT** enviar lembrete ao responsável quando filha **não fizer** a sessão. | MUST |
| RF-E8-08 | Todos os e-mails **MUST** ter link de "gerenciar minhas notificações" e "descadastrar" (compliance). | MUST |
| RF-E8-09 | Notificações **push (PWA) SHOULD** ser implementadas em v1.1 (fast-follow); ver ADR-002. | SHOULD |

**Critérios de aceite (E8):**
- Taxa de entrega de e-mail ≥ 98% no primeiro mês.
- Nenhuma reclamação de pai sobre "notificação que cobra".

---

### Épico 9 — Pipeline de conteúdo (E9)

**Objetivo:** sustentar produção semanal com IA + revisão humana, sem burnout.

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E9-01 | Conteúdo **MUST** viver em repositório Git separado (`atrilha-conteudo`) versionado. | MUST |
| RF-E9-02 | Cada semana **MUST** ter: `briefing.md`, `sessoes.yaml`, pasta `midia/`. | MUST |
| RF-E9-03 | YAML **MUST** ter schema validável (versão, semana, sessões com blocos, quiz, versículo, pergunta família). | MUST |
| RF-E9-04 | Sistema **MUST** ter importador YAML→banco invocável via CLI ou CI/CD. | MUST |
| RF-E9-05 | Importador **MUST** ser idempotente (rodar 2× não duplica). | MUST |
| RF-E9-06 | Conteúdo **MUST** ter status: `rascunho`, `revisao`, `aprovado`, `publicado`. | MUST |
| RF-E9-07 | Apenas conteúdo `publicado` **MUST** aparecer aos usuários finais. | MUST |
| RF-E9-08 | Admin **MUST** poder rever histórico de edições por commit. | MUST |
| RF-E9-09 | Sistema **SHOULD** ter buffer mínimo de 4 semanas adiante em status `aprovado`. | SHOULD |
| RF-E9-10 | Pipeline CrewAI (Teólogo + Roteirista + Revisor) **SHOULD** ser usado para gerar primeiro draft das semanas; revisão humana obrigatória antes de `aprovado`. | SHOULD |

**Critérios de aceite (E9):**
- Importar nova semana ao banco leva ≤ 30 segundos.
- Erro no YAML é detectado pelo validador antes do deploy.

---

### Épico 10 — PWA e instalação (E10)

**Objetivo:** garantir instalação simples e experiência app-like sem App Store.

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E10-01 | App **MUST** ter `manifest.json` válido com nome, ícones (192px, 512px), theme color, display standalone. | MUST |
| RF-E10-02 | App **MUST** ter service worker com cache de assets estáticos (CSS, JS, fontes, ícones). | MUST |
| RF-E10-03 | Service worker **MUST** servir página offline mínima ("sem conexão; sessão de hoje disponível em cache se já carregada"). | MUST |
| RF-E10-04 | App **MUST** mostrar prompt nativo de instalação após 2 sessões completas (não no primeiro acesso). | MUST |
| RF-E10-05 | App **MUST** funcionar em Chrome Android, Safari iOS (16+), Edge desktop, Chrome desktop. | MUST |
| RF-E10-06 | Bundle inicial JS+CSS **MUST** ser ≤ 200KB gzipped. | MUST |
| RF-E10-07 | First Contentful Paint (4G) **MUST** ser ≤ 1,5s. | MUST |

**Critérios de aceite (E10):**
- Instalável em Android e iOS com ícone na home screen.
- Score Lighthouse PWA ≥ 90.

---

### Épico 11 — Compartilhamento social (E11)

**Objetivo:** permitir que adolescentes e responsáveis compartilhem conquistas em WhatsApp e Instagram, gerando aquisição orgânica e celebrando publicamente (sem expor dados privados).

| ID | Requisito | Prioridade |
|---|---|---|
| RF-E11-01 | Adolescente **MUST** poder compartilhar **selo conquistado** (semana, trimestre) como imagem gerada dinamicamente, contendo: nome do selo, apelido (opcional, opt-in por compartilhamento), data, marca atrilha. | MUST |
| RF-E11-02 | Adolescente **MUST** poder compartilhar **marco de streak** ao atingir 7, 14, 30, 60, 100 dias — imagem celebratória com o número do streak. | MUST |
| RF-E11-03 | Adolescente **MUST** poder compartilhar **versículo memorizado** (Fase 4 com ≥ 90% acurácia) como card visual com texto do versículo + referência + assinatura atrilha. | MUST |
| RF-E11-04 | Qualquer usuário (adolescente ou responsável) **MUST** poder compartilhar **link de convite "junte-se ao atrilha"** com pré-visualização rica (OG tags). | MUST |
| RF-E11-05 | Botão de compartilhamento **MUST** usar Web Share API (`navigator.share`) quando disponível, permitindo o usuário escolher o destino (WhatsApp, Instagram, etc.) via menu nativo do SO. | MUST |
| RF-E11-06 | Fallback **MUST** existir para navegadores sem Web Share: link `wa.me` direto pra WhatsApp; para Instagram, download da imagem com instrução "publique nos Stories". | MUST |
| RF-E11-07 | Imagens de compartilhamento **MUST** ser geradas server-side (ex.: HTML→canvas ou template renderizado), em proporção 1080×1920 (Stories) e 1080×1080 (feed). | MUST |
| RF-E11-08 | Imagens **MUST NOT** conter dados privados (reflexão, idade exata, e-mail, dados do responsável). | MUST |
| RF-E11-09 | Exibir apelido na imagem **MUST** ser opcional, controlável pelo usuário em cada compartilhamento (default: ON; pode desligar). | MUST |
| RF-E11-10 | Adolescente **MAY** ver, no painel, um indicador discreto de quantas vezes compartilhou (gamificação leve, sem expor identidade de quem clicou). | MAY |
| RF-E11-11 | Sistema **MUST** rastrear `share_initiated`, `invite_clicked`, `invite_converted` para medir efetividade da aquisição orgânica. | MUST |
| RF-E11-12 | Responsável **MUST** poder compartilhar apenas: link de convite e selo da filha (com permissão do adolescente — opt-in implícito ao compartilhar selo é dado pelo adolescente quando consente a feature). Reflexões e dados pessoais **NUNCA** compartilháveis pelo responsável. | MUST |

**Critérios de aceite (E11):**
- Geração da imagem leva ≤ 800ms p95.
- Compartilhamento no Android usa Web Share nativo; no iOS Safari também.
- Link `wa.me` abre WhatsApp com mensagem pré-formatada.
- Imagens em 1080×1920 ficam dentro de 200KB.

---

## 8. Requisitos não-funcionais

### 8.1 Performance

| ID | Requisito | Alvo |
|---|---|---|
| RNF-PERF-01 | First Contentful Paint (FCP) em 4G mediano | ≤ 1,5s |
| RNF-PERF-02 | Tempo de resposta de API para leitura (sessão, trilha) | p95 ≤ 300ms |
| RNF-PERF-03 | Tempo de resposta de API para escrita (resposta de quiz, conclusão) | p95 ≤ 500ms |
| RNF-PERF-04 | Bundle inicial JS+CSS gzipped | ≤ 200KB |
| RNF-PERF-05 | Tempo total de sessão percebido pelo usuário (atenção sustentada) | ≤ 10 minutos |

### 8.2 Disponibilidade

| ID | Requisito | Alvo |
|---|---|---|
| RNF-DISP-01 | Uptime mensal | ≥ 99,5% (~3,6h downtime/mês) |
| RNF-DISP-02 | Tempo médio de recuperação (MTTR) | ≤ 30 min em incidente médio |
| RNF-DISP-03 | Backup automático do banco | Diário, retenção 30 dias |

### 8.3 Escalabilidade (esperada para v1)

| ID | Requisito | Alvo |
|---|---|---|
| RNF-ESC-01 | Usuários cadastrados suportados sem mudança de infra | ≤ 5.000 |
| RNF-ESC-02 | Sessões simultâneas suportadas | ≤ 200 |
| RNF-ESC-03 | Volume de dados (1 ano) | < 5GB |

### 8.4 Acessibilidade

| ID | Requisito | Alvo |
|---|---|---|
| RNF-A11Y-01 | Conformidade mínima WCAG 2.1 | Nível AA (esforço razoável, não certificação) |
| RNF-A11Y-02 | Contraste mínimo de texto sobre fundo | 4.5:1 (corpo) / 3:1 (display) |
| RNF-A11Y-03 | Suporte a navegação por teclado | Todos os fluxos críticos |
| RNF-A11Y-04 | Atributos ARIA em componentes interativos (drag, caça-palavras) | Implementação razoável |
| RNF-A11Y-05 | Tamanho mínimo de touch target | 44×44px |

> Nota: acessibilidade plena para leitores de tela em mecânicas drag/caça-palavras é difícil; v1 entrega versão alternativa textual (fallback) para essas mecânicas.

### 8.5 Compatibilidade

| ID | Requisito |
|---|---|
| RNF-COMP-01 | Chrome Android ≥ 110 |
| RNF-COMP-02 | Safari iOS ≥ 16 |
| RNF-COMP-03 | Chrome / Edge / Firefox desktop versões atuais −2 |
| RNF-COMP-04 | Layout responsivo 320px → 1920px |

### 8.6 Observabilidade

| ID | Requisito |
|---|---|
| RNF-OBS-01 | Logs estruturados (JSON) com correlation ID por request |
| RNF-OBS-02 | Métricas básicas (uptime, p95 latency, error rate) coletadas e visíveis |
| RNF-OBS-03 | Alertas para: erro 5xx > 1% em 5 min; tempo p95 > 1s em 5 min; downtime > 2 min |
| RNF-OBS-04 | Instrumentação de eventos de produto (ver seção 13) |

### 8.7 Manutenibilidade

| ID | Requisito |
|---|---|
| RNF-MANUT-01 | Cobertura de testes unitários do core de gamificação (XP, streak, selos) ≥ 80% |
| RNF-MANUT-02 | Cobertura de testes de integração dos fluxos críticos (cadastro, vinculação, conclusão de sessão) ≥ 70% |
| RNF-MANUT-03 | Migrations versionadas (Flyway), reversíveis quando possível |
| RNF-MANUT-04 | CI bloqueia merge se testes falham ou bundle excede 250KB |

---

## 9. Arquitetura técnica de alto nível

### 9.1 Stack confirmada

**Backend**
- Java 21 + Spring Boot 3
- Spring Security (OAuth Google + login email/senha com BCrypt)
- PostgreSQL 16
- Flyway (migrations)
- Spring Data JPA + Specifications
- Spring Mail (e-mails transacionais)
- Maven

**Frontend**
- Thymeleaf (server-side rendering)
- HTMX (interações sem reload)
- Tailwind CSS (utility-first)
- Alpine.js (interações pontuais: quiz, drag, caça-palavras)
- Lottie (animações de conquista)
- PWA (manifest + service worker próprio)

**Infra**
- VPS HostGator + Nginx (proxy reverso + Let's Encrypt)
- Container Docker dedicado para a aplicação
- Cloudflare DNS + CDN gratuito na frente (estáticos e mídia)
- Repositório de mídia: filesystem do VPS na v1 (ver ADR-010)
- Subdomínio inicial: `atrilha.app` (a confirmar disponibilidade) com fallback `app.zayt.dev`

### 9.2 Diagrama lógico (alto nível)

```
[Usuário (mobile/desktop PWA)]
            │
            │ HTTPS
            ▼
       [Cloudflare CDN + DNS]
            │
            ▼
       [Nginx (HostGator VPS)]
            │
            ▼
   ┌────────────────────────────┐
   │  Spring Boot Application    │
   │  ├── Web (Thymeleaf+HTMX)   │
   │  ├── Auth (Spring Security) │
   │  ├── Domain (gamificação)   │
   │  ├── Content importer       │
   │  └── Mail service           │
   └────────────────────────────┘
            │             │
            ▼             ▼
       [PostgreSQL]   [Filesystem
                       de mídia]
            ▲
            │ (CI/CD)
            │
   [Repositório de conteúdo Git
    + Pipeline CrewAI (Ollama Cloud)]
```

### 9.3 Boundaries de módulos do backend

- `auth` — login, sessão, OAuth, password reset
- `accounts` — perfil adolescente, perfil responsável, vinculação, consentimento
- `content` — trimestre, semana, sessão, blocos, quiz, versículo
- `progress` — conclusão de sessão, respostas, reflexões, XP, streak, selos
- `notifications` — fila de e-mails, templates, scheduling
- `admin` — painel mínimo de operação (status conteúdo, usuários, exportação manual de dados)

### 9.4 Decisões de não-construir agora

- **Sem microsserviços** — monolito Spring Boot suficiente para 5k usuários.
- **Sem cache externo (Redis)** — cache em memória do Spring + Postgres é suficiente.
- **Sem fila de mensagens externa (RabbitMQ/Kafka)** — `@Async` do Spring + thread pool dedicada para e-mails na v1.
- **Sem CDN paga** — Cloudflare free tier.

---

## 10. Modelo de dados (alto nível)

> Não é o schema final — é a estrutura de entidades-chave para guiar a modelagem detalhada.

### 10.1 Entidades principais

```
Account
  ├── id (UUID)
  ├── type (enum: ADOLESCENT | GUARDIAN)
  ├── email, email_verified_at
  ├── password_hash (nullable se OAuth)
  ├── oauth_provider (nullable)
  ├── created_at, last_login_at, deleted_at

AdolescentProfile
  ├── account_id (FK Account, type=ADOLESCENT)
  ├── nickname
  ├── birth_date
  ├── avatar_url
  ├── timezone (default America/Sao_Paulo)

GuardianProfile
  ├── account_id (FK Account, type=GUARDIAN)
  ├── full_name

GuardianLink
  ├── id
  ├── adolescent_id (FK AdolescentProfile)
  ├── guardian_id (FK GuardianProfile, nullable até pareamento)
  ├── code (6 chars, único enquanto ativo)
  ├── code_expires_at
  ├── linked_at
  ├── consent_accepted_at
  ├── consent_ip, consent_user_agent, consent_terms_hash
  ├── revoked_at

ContentQuarter
  ├── id
  ├── year, number (1–4)
  ├── title
  ├── starts_on, ends_on

ContentWeek
  ├── id
  ├── quarter_id (FK)
  ├── week_number (1–13)
  ├── title, header_image_url
  ├── key_verse_reference, key_verse_text
  ├── family_question
  ├── status (RASCUNHO | REVISAO | APROVADO | PUBLICADO)
  ├── starts_on

ContentSession
  ├── id
  ├── week_id (FK)
  ├── day_of_week (SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)
  ├── session_kind (enum próprio da atrilha, a definir em doc complementar de naming)
  ├── available_on (data nominal)
  ├── blocks (JSONB: hook, core, quiz, reflection, closing)

SessionCompletion
  ├── id
  ├── adolescent_id (FK)
  ├── session_id (FK)
  ├── started_at, completed_at
  ├── xp_earned

QuizAnswer
  ├── id
  ├── completion_id (FK)
  ├── question_index
  ├── chosen_option
  ├── is_correct
  ├── answered_at

Reflection
  ├── id
  ├── completion_id (FK)
  ├── text (criptografado at rest opcional na v1)
  ├── is_shared_with_guardian (default false)
  ├── shared_at, unshared_at

MemorizedVerse
  ├── id
  ├── adolescent_id (FK)
  ├── week_id (FK)
  ├── accuracy_percent
  ├── memorized_at
  ├── next_review_at

StreakState
  ├── adolescent_id (FK, PK)
  ├── current_streak, longest_streak
  ├── last_completed_date
  ├── shield_available (boolean)
  ├── shield_renewed_at

Badge
  ├── id
  ├── adolescent_id (FK)
  ├── type (WEEK_COMPLETE | QUARTER_COMPLETE)
  ├── reference (week_id ou quarter_id)
  ├── earned_at

EmailJob
  ├── id
  ├── account_id, template_key, payload (JSONB)
  ├── status (PENDING | SENT | FAILED)
  ├── scheduled_at, sent_at, attempts
```

### 10.2 Considerações

- IDs UUID v7 (ordenáveis temporalmente, bons para índices).
- Soft delete por `deleted_at` para entidades de conta; hard delete via processo LGPD apartado.
- Reflexões: texto em coluna `TEXT`, índice apenas por `completion_id`. Criptografia at-rest opcional na v1 (avaliar custo/benefício).
- JSONB para blocos de sessão dá flexibilidade ao schema editorial sem migrações constantes.

---

## 11. Segurança, privacidade e LGPD

### 11.1 Autenticação e autorização

- Senhas com BCrypt (cost 12).
- Sessões via cookie HttpOnly, Secure, SameSite=Lax.
- CSRF tokens em todas as mutações server-side (Thymeleaf).
- OAuth Google com escopos mínimos (`openid email profile`).
- Sem MFA na v1 (avaliar v2).

### 11.2 Autorização por perfil

- Adolescente acessa apenas seus próprios dados.
- Responsável acessa apenas o adolescente vinculado, escopo definido pelo painel (RF-E7-01 a RF-E7-09).
- Admin: rota apartada (`/admin`), autenticação separada, IP allowlist no início.

### 11.3 Dados sensíveis

- E-mail, data de nascimento → dados pessoais comuns (LGPD).
- Reflexões → conteúdo de menor; tratamento estrito como dado sensível.
- Histórico de progresso → dado pessoal de menor.

### 11.4 LGPD — base legal

| Dado | Base legal | Observação |
|---|---|---|
| Cadastro do adolescente | Consentimento parental (Art. 14 LGPD) | Registrado em `GuardianLink.consent_accepted_at` |
| Cadastro do responsável | Execução de contrato | Termo de uso aceito |
| Progresso e gamificação | Execução de contrato | Necessário para entregar o produto |
| E-mails marketing | Consentimento explícito (opt-in) | NÃO presente no MVP — só transacionais |
| Cookies analíticos | Consentimento explícito | Banner de cookies se usar analytics externo |

### 11.5 Direitos do titular

| Direito | Implementação MVP |
|---|---|
| Acesso aos dados | Painel "Meus dados" mostra cadastro e estatísticas básicas |
| Correção | Edição de perfil (apelido, e-mail, avatar, data nascimento) |
| Anonimização / exclusão | Botão "Excluir minha conta"; processo manual no admin nos 30 dias seguintes (soft delete imediato, hard delete agendado) |
| Portabilidade | Formulário "Solicitar meus dados"; admin gera export JSON manual em ≤ 15 dias |
| Revogação de consentimento | Adolescente: deslogar e excluir; Responsável: revogar vinculação |
| Informação sobre uso compartilhado | Política de privacidade pública, datada |

### 11.6 Consentimento parental — fluxo

1. Adolescente se cadastra (idade 13–17 detectada).
2. Sistema marca conta como "pendente de consentimento".
3. Gera código de vinculação.
4. Responsável insere código → lê termo de consentimento (texto integral, scrollável).
5. Marca checkbox "Sou responsável legal por [apelido] e autorizo o uso do atrilha sob os termos acima".
6. Sistema registra hash do termo, timestamp, IP, user agent.
7. Conta do adolescente é desbloqueada.
8. Cópia do termo enviada por e-mail ao responsável.

### 11.7 Retenção

- Conta ativa: dados mantidos enquanto ativa.
- Conta inativa > 12 meses: e-mail de aviso; se sem retorno em 30 dias, conta arquivada (read-only).
- Conta excluída: soft delete imediato; hard delete em 30 dias (exceto dados exigidos por lei).

### 11.8 Segurança operacional

- HTTPS obrigatório (Let's Encrypt + renovação automática).
- Headers de segurança: `Strict-Transport-Security`, `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`.
- Rate limiting em endpoints de auth (5 tentativas/min por IP).
- Logs sem dados sensíveis (nada de senha, token, reflexão).
- Secrets fora do código (variáveis de ambiente).

---

## 12. Conteúdo: produção, pipeline e direitos

### 12.1 Fonte do calendário

Calendário oficial da Lição Juvenil da Conferência Geral, público com 2+ anos de antecedência. atrilha **não inventa** o tema/semana; **reembala didaticamente** a partir do calendário e do briefing humano.

### 12.2 Pipeline de produção

```
[Calendário CG]
       │
       ▼
[Briefing humano] (Dioni; ~30 min/semana)
       │
       ▼
[Agente Teólogo (GLM-5.1)] → expande briefing por dia
       │
       ▼
[Agente Roteirista (Qwen3-Coder)] → gera 7 sessões em YAML
       │
       ▼
[Agente Revisor (GLM-5.1)] → checa tom, doutrina, qualidade
       │
       ▼
[Revisão humana] (Dioni; ~2h/semana ou 8h em lote/4 semanas)
       │
       ▼
[Aprovado em Git + tag] → CI/CD importa para banco → publicado
```

### 12.3 Tempo humano (estimativa)

- Por semana isolada: ~4h (briefing + revisão + mídia + publicação).
- Em batching de 4 semanas: ~10h (ganho de contexto + foco).
- Recomendação: **trabalhar em lotes de 4 semanas, 1× por mês**, mantendo buffer de 4–8 semanas.

### 12.4 Direitos autorais

| Recurso | Status | Decisão para MVP |
|---|---|---|
| Texto bíblico ARC e ACF | Domínio público | **Usar ambas, ARC default** |
| Texto bíblico NVI, ARA, NTLH | Direitos autorais ativos | NÃO usar |
| Escritos de Ellen White (originais) | Domínio público (falecida 1915) | OK para citar |
| Traduções modernas de Ellen White | Possíveis direitos | Usar apenas traduções de domínio público de egwwritings.org ou produzir tradução própria |
| Material oficial DSA/CPB (revista Juvenil) | Direitos autorais | NUNCA reproduzir; produzir didática própria a partir do calendário público |
| Imagens de banco | Varia | Usar Unsplash/Pexels (licenças permissivas) ou IA generativa licenciada |

### 12.5 Revisão editorial — checklist obrigatório

Antes de marcar `aprovado`, cada semana **MUST** passar pelo checklist:

- [ ] Doutrina ASD verificada (sábado, santuário, segunda vinda, estado dos mortos, profecia)
- [ ] Texto bíblico de versão de domínio público (ARC/ACF)
- [ ] Sem moralismo, comparação culposa, ameaça espiritual ou prosperidade
- [ ] Sem infantilização
- [ ] Linguagem permite mencionar medo/dúvida/vergonha sem patologizar
- [ ] Versículo-chave presente nas 4 fases de memorização
- [ ] Pergunta de discussão familiar presente e útil
- [ ] Caça-palavras tem 5–8 palavras-chave válidas
- [ ] Quiz tem 2 perguntas por dia com explicações
- [ ] Schema YAML valida sem erro

---

## 13. Métricas e instrumentação

### 13.1 Eventos de produto a instrumentar

| Evento | Quando | Propriedades-chave |
|---|---|---|
| `account_created` | Cadastro completo | `type`, `oauth_provider`, `age_bracket` |
| `guardian_linked` | Vinculação concluída | `days_since_adolescent_signup` |
| `session_started` | Bloco gancho exibido | `session_id`, `day_of_week`, `week_id` |
| `session_completed` | Bloco fechamento concluído | `session_id`, `duration_seconds`, `xp_earned` |
| `quiz_answered` | Resposta de quiz | `session_id`, `question_index`, `correct` |
| `reflection_written` | Texto ≥ 30 chars salvo | `session_id`, `length` |
| `reflection_shared` | Opt-in compartilhamento | `session_id` |
| `verse_memorized` | Fase 4 com ≥ 90% acurácia | `week_id`, `accuracy` |
| `streak_milestone` | Streak atinge 7, 14, 30, 60, 100 | `streak_length` |
| `badge_earned` | Selo conquistado | `badge_type`, `reference` |
| `guardian_dashboard_view` | Painel aberto | (sem propriedades adicionais) |
| `guardian_marked_discussed` | Botão "conversamos" | `week_id` |
| `pwa_installed` | Evento `beforeinstallprompt` aceito | `platform` |
| `share_initiated` | Usuário toca em botão de compartilhar | `share_type` (badge, streak, verse, invite), `channel_hint` |
| `invite_clicked` | Alguém abre o link de convite compartilhado | `source_user_id` (opaco) |
| `invite_converted` | Quem clicou no convite criou conta | `source_user_id`, `time_to_convert_hours` |

### 13.2 Dashboards mínimos

1. **Engajamento diário**: DAU, MAU, DAU/MAU.
2. **Funil de cadastro**: visita → cadastro → primeira sessão → primeira semana completa.
3. **Vinculação**: % de adolescentes com responsável vinculado em 24h, 7d.
4. **Streak**: distribuição de streaks ativos; % com streak ≥ 7.
5. **Qualidade de conteúdo por semana**: % acerto médio de quiz; % de memorização Fase 4.

### 13.3 Pesquisa qualitativa

- Entrevistas a cada 4 semanas com 3–5 famílias (separadas: adolescente / responsável).
- NPS qualitativo a cada 8 semanas.
- Suporte/feedback via formulário simples no app + e-mail.

---

## 14. Roadmap pós-MVP

### 14.1 v1.1 (fast-follow, 30–60 dias após launch)

- **Push notifications PWA** (Android primeiro; iOS quando instalado como app).
- **Áudio narrado das sessões** (TTS de qualidade ou narração humana piloto em 2 semanas).
- **Spaced repetition completo** de versículos memorizados (revisão em 3, 7, 21 dias).
- **Onboarding ajustado** baseado em dados das primeiras semanas.
- **Pequenos polimentos** com base em entrevistas qualitativas.
- **Tipos adicionais de compartilhamento social** (selos especiais, milestones de nível) — expandindo o E11 do MVP.

### 14.2 v1.5 (60–120 dias)

- **Monetização: plano Premium família** (ver seção 14.4).
- **Múltiplos perfis de filhos por conta de responsável** (até 4).
- **Múltiplos responsáveis por adolescente** (ex.: pai + mãe).
- **Acesso a trimestres anteriores** (paywall).
- **Modo offline robusto** (cache de 1 trimestre completo).

### 14.3 v2 (120+ dias)

- **App nativo via Capacitor** (reaproveita 90% do código PWA).
- **Vídeos curtos por sessão** (produção própria ou parceria).
- **Grupos privados de devocional** (família estendida, primos).
- **Exportação self-service de dados** (LGPD).
- **Internacionalização** (espanhol primeiro, mercado DSA).

### 14.4 Modelo de monetização (v1.5)

**Freemium familiar:**

- **Grátis:** 1 perfil adolescente + 1 painel pai, trimestre atual, gamificação básica.
- **Premium família (R$ 19,90/mês ou R$ 199/ano):**
  - Até 4 perfis de filhos
  - Trimestres anteriores
  - Modo offline robusto
  - Conteúdo extra (áudio, podcast resumo, ilustrações Stories)
  - Sem anúncios (não há anúncios no Grátis na v1 também)

**Decisão MVP:** lançar 100% gratuito, monetizar após validar tração com a North Star ≥ 25% em 90 dias.

---

## 15. Riscos e mitigações

| # | Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|---|
| R1 | Reação da liderança ASD ("concorrência" institucional) | Média | Alto | Posicionar como complemento, não substituto. Comunicação clara: usar revista oficial + atrilha juntos. |
| R2 | Discordância teológica de pais conservadores | Média | Médio | Alinhamento doutrinário rigoroso. Checklist editorial obrigatório. Revisor ASD recrutado nos 60 dias. |
| R3 | CPB/DSA reclamar de "concorrência" | Baixa | Alto | Produzir didática própria, sem reproduzir material oficial. Calendário CG é público. |
| R4 | Adolescente não engajar suficientemente | Média | Crítico | Validar com 5+ adolescentes antes de codar fase final. Iterar conteúdo após lançamento. Métrica kill criteria. |
| R5 | Pais não aderirem ao painel | Baixa | Médio | Onboarding educativo. Painel deve ser claramente útil, não vigilante. Notificações positivas. |
| R6 | Burnout de produção de conteúdo | Alta | Alto | Batching mensal. Revisor freelance ASAP. Buffer de 4 semanas mínimo. Pipeline IA reduz tempo. |
| R7 | App oficial DSA lançar feature similar | Baixa | Alto | Velocidade de iteração. Identidade jovem. Comunidade que oficial não pode atender. |
| R8 | LGPD — multa por descumprimento de consentimento parental | Baixa | Alto | Fluxo de consentimento auditável (RF-E2-07, RF-E2-08). Política de privacidade pública. Consultoria jurídica antes de cobrar (v1.5). |
| R9 | Custo de infraestrutura escapar do orçamento | Baixa | Médio | VPS HostGator + Cloudflare free. Monitorar uso. Migrar mídia para R2 antes de explodir. |
| R10 | Reflexões privadas vazarem (bug ou ataque) | Baixa | Crítico | Testes de autorização rigorosos. Code review focado em fluxos de privacidade. Possível criptografia at-rest em v1.1. |
| R11 | Validação com poucos adolescentes (n=1) ser viés do founder | Alta | Alto | Antes do soft launch, validar com 3–5 famílias além da do founder. Tracked como entregável da semana 8. |
| R12 | Nome "atrilha" conflitar com marca registrada existente | Média | Médio | Busca no INPI antes de domínio definitivo. Fallback de marca pronto. |

---

## 16. Registro de decisões (ADRs)

> Cada ADR é uma decisão de produto/arquitetura registrada para evitar revisitar sem motivo.

### ADR-001 — Linguagem editorial sem moralismo
**Decisão:** princípios P1–P5 são absolutos no produto.
**Por quê:** validação inicial com filha do founder + leitura da literatura ASD juvenil moderna mostra que o tom da revista oficial é o melhor benchmark; copywriting moralista quebra engajamento adolescente.
**Alternativa rejeitada:** linguagem "tradicional" cristã para evitar atrito com pais conservadores → cria atrito muito maior com o usuário primário.

### ADR-002 — Notificações: e-mail no MVP, push em v1.1
**Decisão:** apenas e-mail no MVP. Push PWA entra como fast-follow.
**Por quê:** push em iOS PWA exige instalação como app e tem suporte limitado; complexidade de permissões e SW dedicado atrasa MVP. E-mail tem entrega previsível e cobre 100% das plataformas.
**Alternativa rejeitada:** push desde o MVP → atraso de 2 semanas no cronograma.

### ADR-003 — Onboarding do responsável é guiado, não direto
**Decisão:** tutorial de 3 telas obrigatório no primeiro acesso ao painel.
**Por quê:** princípio P9 (painel não-vigilante) só funciona se o pai entende as regras desde o início. Caso contrário, vai cobrar a filha por algo que ele não vê e produto vira ferramenta de vigilância na cabeça dele.
**Alternativa rejeitada:** dashboard direto → risco alto de quebrar contrato de privacidade no entendimento do pai.

### ADR-004 — Vinculação iniciada pelo adolescente
**Decisão:** adolescente cadastra primeiro, gera código de 6 dígitos, compartilha com responsável.
**Por quê:** o produto é do adolescente. Se o pai cadastra primeiro, o app vira "ferramenta do pai para forçar a filha". Inverter a iniciativa preserva agência do usuário primário e o tom não-vigilante.
**Alternativa rejeitada:** pai cadastra e convida filha → quebra a postura de produto.

### ADR-005 — Compartilhamento de reflexão: opt-in por item, nunca global
**Decisão:** cada reflexão tem toggle individual "compartilhar com meu responsável". Sem opção global.
**Por quê:** opt-in global cria risco de adolescente esquecer que tá ligado e expor algo sensível. Friction maior por item é uma feature, não um bug.
**Alternativa rejeitada:** opt-in global → vazamento acidental quase certo em 6 meses.

### ADR-006 — Idade mínima: 13 anos
**Decisão:** 13 anos é a idade mínima absoluta; 13–17 requer consentimento parental; 18+ é maioridade.
**Por quê:** alinha com COPPA (referência internacional) e com a interpretação majoritária da LGPD para tratamento de menores. Abaixo de 13 não atendemos. Acima de 13 só com pai vinculado.
**Alternativa rejeitada:** 12 anos → fora da janela COPPA, sem ganho de mercado significativo (12 = início do Juvenil mas pais quase sempre estão envolvidos pesadamente).

### ADR-007 — Consentimento parental com fluxo duplo + e-mail
**Decisão:** vinculação registra hash do termo, IP, user agent e timestamp + envia cópia por e-mail ao responsável. Conta sem responsável em 7 dias fica funcionalmente limitada.
**Por quê:** trilha de auditoria mínima necessária para LGPD; e-mail garante que responsável tem cópia para si; janela de 7 dias respeita realidade familiar mas evita conta de menor sem responsável de longo prazo.
**Alternativa rejeitada:** consentimento implícito por uso → risco LGPD inaceitável.

### ADR-008 — Texto bíblico: ARC default, ACF como alternativa
**Decisão:** versões em domínio público (Almeida Revista e Corrigida, Almeida Corrigida Fiel). ARC default na v1.
**Por quê:** NVI/ARA/NTLH têm direitos autorais e licenciamento caro/lento. ARC é a versão mais usada historicamente no ambiente ASD brasileiro.
**Alternativa rejeitada:** licenciar NVI → custo e prazo incompatíveis com solo founder.

### ADR-009 — Exportação de dados: solicitação manual no MVP
**Decisão:** formulário "solicitar meus dados", export gerado manualmente pelo admin em ≤ 15 dias.
**Por quê:** atende LGPD sem custo de engenharia para self-service. Volume esperado de solicitações na v1 é baixíssimo.
**Alternativa rejeitada:** self-service no MVP → 1 sprint a mais sem ganho proporcional.

### ADR-010 — Mídia: filesystem do VPS + Cloudflare CDN free
**Decisão:** imagens e Lottie ficam no VPS HostGator; Cloudflare free na frente.
**Por quê:** zero custo adicional, simplifica deploy. Migrar para R2/S3 quando volume justificar (> 50GB ou > 100k req/dia).
**Alternativa rejeitada:** R2 desde o MVP → conta extra para configurar sem necessidade.

### ADR-011 — Stack: Java 21 + Spring Boot + Thymeleaf + HTMX
**Decisão:** stack tradicional server-side com HTMX para interações; sem framework JS pesado.
**Por quê:** founder tem domínio da stack; deploy simples em VPS; HTMX cobre 95% das necessidades de interatividade; bundle inicial pequeno; SEO bom.
**Alternativa rejeitada:** SPA (React/Next) → bundle maior, hosting mais complexo, ganho marginal para o que o app precisa.

### ADR-012 — Marca: "atrilha" como nome do produto
**Decisão:** produto se chama **atrilha** (decisão do founder; substitui "Rota" do contexto).
**Por quê:** registrado pelo founder como nome do projeto; conota "a trilha" (caminho) e tem identidade própria.
**Próximos passos:** busca INPI antes de marketing público; registro de marca se livre; domínio `atrilha.app` a confirmar.

### ADR-013 — Identidade visual: clara, jovem, colorida (sem tema escuro)
**Decisão:** abandonar a direção "dark + laranja queimado" descrita no contexto. Identidade visual será **clara, vibrante e jovem**, sem evocação de trevas/escuridão/pesadume.
**Por quê:** público adolescente associa apps escuros a produtividade adulta (Spotify, Notion); identidade clara comunica acolhimento e energia, alinhada à promessa do produto. Evita também leitura cultural de "trevas espirituais" pouco aderente ao tom positivo desejado.
**Estado:** paleta, tipografia e componentes finais a definir em **doc complementar "Identidade visual e protótipo"**, antes do Sprint 2.
**Alternativa rejeitada:** manter dark theme do contexto → fricção com o tom do produto e com a faixa etária.

### ADR-014 — Compartilhamento social entra no MVP
**Decisão:** compartilhamento de conquistas (selos, marcos de streak, versículos memorizados, link de convite) via WhatsApp e Instagram é parte do MVP (E11), não fast-follow.
**Por quê:** aquisição orgânica de novos usuários é alavanca principal sem orçamento de marketing. WhatsApp é o canal real onde família ASD brasileira se comunica; Instagram é onde adolescente está. Adiar essa feature significa lançar sem o motor de crescimento natural.
**Salvaguardas:** imagens geradas server-side jamais expõem reflexões, e-mail, idade exata ou dados do responsável; apelido é opt-in por compartilhamento.
**Alternativa rejeitada:** deixar para v1.1 → desperdiça os primeiros 60 dias de tração potencial.

---

## 17. Critérios de aceite globais (Definition of Done)

Cada User Story derivada desta PRD só é "Done" quando atende **todos** os critérios abaixo:

1. **Funciona conforme especificado** no requisito-fonte (RF-XX-YY).
2. **Testes automatizados** escritos (unit + integration onde aplicável) e passando.
3. **Mobile-first verificado** em viewport 320px (iPhone SE).
4. **Acessibilidade básica**: contraste OK, navegação por teclado, ARIA onde aplicável.
5. **Sem regressão** de bundle (≤ 200KB gzipped) e Lighthouse PWA ≥ 90.
6. **Logs e eventos de produto** instrumentados se a feature gera métrica relevante (ver seção 13).
7. **Doc inline** mínima (Javadoc para classes públicas; comentário de "por quê" onde decisão é não-óbvia).
8. **Revisão própria** com checklist editorial se mexe em copy ou conteúdo.
9. **Migration Flyway** versionada se mexe em schema.
10. **Deploy** validado em staging (subdomínio separado) antes de produção.

---

## 18. Glossário

| Termo | Definição |
|---|---|
| **atrilha** | Nome do produto. PWA gamificada da Lição da Escola Sabatina Juvenil. |
| **ASD** | Adventistas do Sétimo Dia. |
| **Adolescente** | Usuário primário, faixa 13–17 anos. |
| **Responsável** | Usuário secundário, ≥ 18 anos, vinculado a 1 perfil adolescente. |
| **Trilha** | Visualização semanal de 7 sessões diárias. |
| **Sessão** | Unidade diária de consumo (até 10 minutos). |
| **Sessão de sábado** | Fechamento da semana: recap + memorização Fase 4 + pergunta família. Nome próprio a definir em doc complementar. |
| **Quarto/Trimestre** | Bloco de 13 semanas; calendário oficial CG. |
| **Streak** | Sequência de dias consecutivos com sessão completa. |
| **Escudo** | Item que protege o streak de 1 falha por semana. |
| **Selo** | Conquista visual (semana completa, trimestre completo). |
| **Heatmap** | Visualização anual estilo GitHub contributions. |
| **DAU/MAU** | Daily Active Users sobre Monthly Active Users; métrica de engajamento. |
| **North Star** | % de adolescentes cadastrados com streak ≥ 7 dias. |
| **LGPD** | Lei Geral de Proteção de Dados (Lei 13.709/2018). |
| **COPPA** | Children's Online Privacy Protection Act (referência internacional). |
| **PWA** | Progressive Web App; aplicação web instalável. |
| **CG** | Conferência Geral da Igreja Adventista. |
| **CPB** | Casa Publicadora Brasileira. |
| **DSA** | División Sul-Americana da Igreja Adventista. |
| **ARC/ACF** | Almeida Revista e Corrigida / Almeida Corrigida Fiel; traduções em domínio público. |

---

## Anexo A — Resolução das questões em aberto do contexto

> Esta seção endereça as 10 questões abertas listadas na seção 10 do `contexto.md`. Cada resposta é uma decisão fundamentada (não bloqueante para iniciar User Stories); pode ser revisada se nova informação aparecer.

### Q1 — Qual trimestre lançar primeiro?
**Decisão:** **Trimestre 4 de 2026 (outubro–dezembro)**.
**Justificativa:** hoje é 16/05/2026 (sábado). Cronograma de 8 semanas leva a meados de julho. Trimestre 3 começa em 1/julho — apertado demais para produzir 13 semanas em paralelo ao desenvolvimento. Trimestre 4 começa início de outubro, dando ~4 meses de janela: 8 semanas de dev + 8 semanas de produção em batch + 4 semanas de buffer. Confortável.
**Plano de produção:** começar briefings do Tri 4/2026 imediatamente em paralelo ao Sprint 1.

### Q2 — Push vs e-mail no MVP
**Decisão:** **E-mail no MVP. Push PWA como fast-follow v1.1.** (ADR-002)

### Q3 — Onboarding do pai: tutorial guiado ou direto
**Decisão:** **Tutorial guiado curto (3 telas).** (ADR-003)

### Q4 — Quem inicia vinculação adolescente-pai
**Decisão:** **Adolescente cadastra primeiro, gera código de 6 dígitos, compartilha com responsável.** (ADR-004)

### Q5 — Compartilhamento de reflexão: opt-in por item ou global
**Decisão:** **Opt-in por reflexão individual; sem opção global.** (ADR-005)

### Q6 — Idade mínima
**Decisão:** **13 anos.** (ADR-006)

### Q7 — Termo de uso / consentimento de menor
**Decisão:** **Duplo consentimento: adolescente aceita termos no cadastro; responsável aceita termo de consentimento parental ao vincular; cópia enviada por e-mail; conta sem responsável vinculado em 7 dias é funcionalmente limitada.** (ADR-007 + RF-E2-07 a RF-E2-11)

### Q8 — Backup/exportação de anotações: MVP ou v2
**Decisão:** **Solicitação manual de exportação no MVP (LGPD); self-service em v2.** (ADR-009)

### Q9 — Identidade visual final: "Rota" é definitiva?
**Decisão:** **Não. Marca passa a ser "atrilha" (decisão do founder, 2026-05-16). Busca INPI antes de marketing público.** (ADR-012)

### Q10 — Servidor de mídia: VPS ou CDN
**Decisão:** **Filesystem do VPS HostGator + Cloudflare free na frente no MVP. Migrar para R2/S3 quando volume justificar (> 50GB ou > 100k req/dia).** (ADR-010)

---

## Anexo B — Próximos passos imediatos após aprovação desta PRD

### B.1 Documentos complementares a criar (antes da implementação correspondente)

| Doc | Refina | Quando precisa estar pronto |
|---|---|---|
| **Estrutura da semana e dos blocos de sessão** | E4 (sessão diária) | Antes do Sprint 2 |
| **Nomes próprios das sessões da atrilha** | RF-E4-07 | Antes do Sprint 2 |
| **Identidade visual e protótipo** (paleta, tipografia, componentes, mockups) | P14, ADR-013 | Antes do Sprint 2 |
| **Mecânicas interativas** (mockups e detalhamento UX por mecânica) | E5 | Antes do Sprint 3 |
| **Sistema de XP, gamificação e loop de progressão** (para que serve o XP, níveis, retorno ao app) | E6 | Antes do Sprint 3 |
| **Templates de imagens de compartilhamento social** (selos, streak, versículo, convite) | E11 | Antes do Sprint 5 |

### B.2 Ações de produto e negócio

1. **Decompor em User Stories** organizadas pelos 11 épicos (E1–E11) com estimativas.
2. **Organizar em Sprints** — sugestão: 4 sprints de 2 semanas, cobrindo as 8 semanas do cronograma.
3. **Iniciar produção do Trimestre 4/2026 em paralelo** (briefings das primeiras 4 semanas em batch).
4. **Busca INPI** do nome "atrilha".
5. **Confirmar disponibilidade do domínio** `atrilha.app` (ou fallback).
6. **Recrutar revisor editorial ASD** (freelance, 4h/mês inicialmente).
7. **Definir 3–5 famílias-teste** além da família do founder para validação no Sprint 4.

---

*Fim da PRD v1.1 — atrilha.*



