# Plano de Sprints — atrilha MVP v1

| Campo | Valor |
|---|---|
| Documento | Plano de execução do MVP v1 em sprints semanais |
| Versão | 1.0 |
| Data | 2026-05-17 |
| Autor | CTO/Arquiteto + Humano (Dioni) |
| Fonte de tasks | `doc/Requisitos/UserStory.md` (69 US, MVP v1) |
| Fonte de escopo | `doc/PRD.md` v1.1 |
| Status | Inicial — sujeito a recalibração após cada sprint |

---

## 1. Premissas de capacidade

- **Duração de sprint:** 1 semana (segunda a domingo).
- **Dias úteis:** 6 por semana (segunda a sexta + domingo). **Sábado é shabbat — zero trabalho.**
- **Capacidade diária:** 4h efetivas (founder solo).
- **Capacidade semanal nominal:** **24h/sprint**.
- **Capacidade alocável:** **~20h/sprint** (4h/semana reservadas para revisão, code-review, devoluções entre agentes, imprevistos).
- **Granularidade:** estimativas são em horas-equivalente para o ciclo completo PO→Arquiteto→Codificador→QA→Revisor de cada US. Estimativa por sprint inclui handoffs.

---

## 2. Marcos do MVP

| Marco | Sprint | Critério |
|---|---|---|
| M0 — Aplicação no ar | S1 | `https://atrilha.app` (ou fallback) responde com health check público |
| M1 — Identidade visual definida | S2 | Design system base + protótipos HTML aprovados pelo founder |
| M2 — Auth + Vinculação operacionais | S6 | Adolescente cadastra, vincula responsável, consentimento registrado |
| M3 — Trilha + Sessão diária navegáveis | S9 | Uma semana de conteúdo executável de ponta a ponta |
| M4 — Mecânicas + Gamificação completas | S13 | Quiz, memorização, caça-palavras, XP, streak, selos |
| M5 — Painel dos pais funcional | S14 | Carlos enxerga sinais positivos sem expor reflexões privadas |
| M6 — Comunicação por e-mail ativa | S16 | Lembrete diário, e-mails de sábado, descadastramento operam |
| M7 — PWA + LGPD operantes | S17 | Instalação PWA, modo offline mínimo, export/exclusão de dados |
| M8 — Compartilhamento social no ar | S19 | Selos, streak, versículo memorizado, convite via WhatsApp/Instagram |
| M9 — Soft launch | S20 | Teste com 3–5 famílias concluído, bugs críticos resolvidos |

**Duração total estimada do MVP: 20 sprints (~5 meses)** a partir do início do Sprint 1.

---

## 3. Regras do plano

1. **Toda US do MVP entra em algum sprint** — nada solto. 69/69 endereçadas.
2. **Sprint 1 e Sprint 2 não têm US do catálogo.** São fundação técnica (infra/deploy) e fundação visual (identidade). O Designer só começa a produzir specs por-US a partir do Sprint 3.
3. **Ordem entre sprints respeita dependências técnicas.** E1 antes de E2; E9 antes de E3 (precisa de conteúdo); E3 antes de E4; E4 antes de E5/E6; E6 antes de E7 (sinais positivos vêm da gamificação); E11 depende de E5/E6.
4. **Cada sprint deve fechar em PR mergeado.** Se uma US escorrega, é movida para o próximo sprint e o plano é recalibrado.
5. **Buffer obrigatório por sprint:** ~4h reservadas para handoffs, revisões e ajustes.
6. **Recalibração:** ao fim de cada sprint, revisar este documento — atualizar US realmente entregues, mover restantes, ajustar estimativas. Versionar (1.0 → 1.1 → …).
7. **Tracking de eventos (US-069) é instrumentado conforme as US correspondentes entram em produção** — a US final consolida e valida cobertura completa, mas eventos vão sendo emitidos desde o Sprint 3.

---

## 4. Sprint 1 — Fundação técnica & Deploy

> **Objetivo:** colocar a aplicação no ar com health check público antes de escrever qualquer linha de feature.
> **Marco no fim:** M0.

**Sem US do catálogo.** Tasks técnicas (criadas como `chore-###` no GitHub Issues pelo Arquiteto):

| Task | Descrição                                                                                                                           | Estimativa |
|---|-------------------------------------------------------------------------------------------------------------------------------------|---|
| chore-001 | Esqueleto Spring Boot 3 + Java 21 + Maven, pacotes `auth`/`accounts`/`content`/`progress`/`notifications`/`admin` vazios (PRD §9.3) | 3h |
| chore-002 | Baseline Flyway + PostgreSQL 18 local via Docker Compose                                                                            | 3h |
| chore-003 | Thymeleaf + HTMX + Tailwind + Alpine.js + Lottie wired no layout base                                                               | 3h |
| chore-004 | Endpoint `/health` público + 404 elegante                                                                                           | 1h |
| chore-005 | `Dockerfile` da aplicação + imagem buildada                                                                                         | 2h |
| chore-006 | VPS Zayt: Nginx + Let's Encrypt + Docker daemon                                                                                     | 4h |
| chore-007 | Cloudflare: DNS de `atrilha.app` + CDN free                                                            | 1h |
| chore-008 | Pipeline CI/CD: GitHub Actions → build → SSH deploy para VPS                                                                        | 4h |
| chore-009 | Smoke test em produção: `/health` retorna 200 via HTTPS                                                                             | 1h |
| chore-010 | `AGENTS.md` + `doc/workflow.md` + `doc/PRD.md` + `doc/Requisitos/UserStory.md` commitados                                           | 2h |

**Total: 24h. Saída: app em produção sem feature de usuário, pronta para receber a primeira US.**

---

## 5. Sprint 2 — Identidade visual & Design system base

> **Objetivo:** travar a identidade visual e o design system antes de qualquer tela. Tudo que vier depois reusa estes tokens.
> **Marco no fim:** M1.

**Sem US do catálogo.** Trabalho do Designer (sob direção do humano), entregue como specs em `doc/UX/`:

| Task | Descrição | Estimativa |
|---|---|---|
| ux-001 | `doc/UX/00-identidade-visual.md` — paleta clara/jovem/colorida (ADR-013), tipografia, princípios de hierarquia | 4h |
| ux-002 | `doc/UX/01-design-tokens.md` — tokens Tailwind (cores, espaçamento, raios, sombras, tipografia) | 3h |
| ux-003 | `doc/UX/02-componentes-base.md` — botão, input, card, modal, header, navegação, badge, toast | 5h |
| ux-004 | `doc/UX/03-protótipo-trilha.md` + HTML estático da trilha vazia (sem dados) | 3h |
| ux-005 | `doc/UX/04-protótipo-sessao.md` + HTML estático de bloco da sessão | 3h |
| ux-006 | `doc/UX/05-protótipo-painel-pais.md` + HTML estático do painel vazio | 2h |
| ux-007 | `doc/UX/06-acessibilidade.md` — checklist WCAG 2.1 AA mínimo (PRD §8.4) | 2h |
| ux-008 | Aprovação visual pelo humano (Dioni) — iteração com Designer se necessário | 2h |

**Total: 24h. Saída: design system documentado e protótipos HTML aprovados. Componentes implementáveis em Tailwind nos sprints seguintes.**

---

## 6. Sprint 3 — Auth essencial (E1 parte 1)

> **Objetivo:** adolescente consegue se cadastrar, validar e-mail e entrar.

| US | Título | Estimativa |
|---|---|---|
| US-005 | Bloqueio de cadastro por idade fora da faixa | 2h |
| US-001 | Cadastro de adolescente por e-mail e senha | 6h |
| US-006 | Verificação de e-mail | 5h |
| US-007 | Login recorrente (e-mail/senha e Google) | 3h |
| US-002 | Cadastro de adolescente via Google | 5h |

**Total: 21h.**

---

## 7. Sprint 4 — Auth do responsável + perfil (E1 parte 2)

> **Objetivo:** responsável também se cadastra; ambos editam perfil e fazem logout global.

| US | Título | Estimativa |
|---|---|---|
| US-003 | Cadastro de responsável por e-mail e senha | 3h |
| US-004 | Cadastro de responsável via Google | 3h |
| US-008 | Recuperação de senha | 5h |
| US-009 | Edição do perfil do adolescente | 4h |
| US-010 | Edição do perfil do responsável | 3h |
| US-011 | Logout de todos os dispositivos | 3h |

**Total: 21h.**

---

## 8. Sprint 5 — Vinculação adolescente ↔ responsável (E2 parte 1)

> **Objetivo:** fluxo de código de vinculação iniciado pelo adolescente + consentimento parental registrado (ADR-004/007).

| US | Título | Estimativa |
|---|---|---|
| US-012 | Geração e exibição do código de vinculação | 3h |
| US-013 | Compartilhamento e regeração do código | 3h |
| US-014 | Vinculação do responsável + consentimento parental | 10h |
| US-017 | Notificação ao adolescente quando vinculação concretiza | 2h |
| US-015 | Limitação funcional sem responsável em 7 dias | 4h |

**Total: 22h.**

> **Nota:** US-014 referencia ADR-007 — registro de hash do termo, IP, user-agent, timestamp + e-mail de cópia ao responsável. Texto do termo é gerado por IA nesta fase (revisão jurídica pós-MVP).

---

## 9. Sprint 6 — Vinculação fim + Pipeline de conteúdo (E2 + E9)

> **Objetivo:** completar vinculação e ter um caminho para publicar conteúdo de uma semana.
> **Marco no fim:** M2.

| US | Título | Estimativa |
|---|---|---|
| US-016 | Revogação de vinculação pelo adolescente | 4h |
| US-055 | Importação de uma semana de conteúdo pelo admin | 10h |
| US-056 | Estados editoriais do conteúdo da semana | 4h |
| US-057 | Exibição ao usuário apenas de conteúdo publicado | 3h |

**Total: 21h.**

---

## 10. Sprint 7 — Trilha (E3 parte 1)

> **Objetivo:** trilha da semana atual visível e navegável.

| US | Título | Estimativa |
|---|---|---|
| US-018 | Trilha da semana atual como tela inicial | 6h |
| US-019 | Estados dos dias na trilha | 5h |
| US-020 | Liberação da sessão de sábado | 4h |
| US-021 | Navegação entre semanas do trimestre corrente | 5h |

**Total: 20h.**

---

## 11. Sprint 8 — Trilha fim + Sessão diária início (E3 fim + E4 parte 1)

> **Objetivo:** ver trimestre completo e começar a sessão diária dos 5 blocos.

| US | Título | Estimativa |
|---|---|---|
| US-022 | Visão de trimestre (13 semanas) | 5h |
| US-023 | Execução da sessão diária com 5 blocos sequenciais | 12h |
| US-024 | Reflexão privada por default com texto opcional | 5h |

**Total: 22h.**

> **Nota:** US-023 depende de `doc/conteudo/fluxo-semana.md` (criado pelo humano) que define os nomes próprios e a ordem dos 5 blocos.

---

## 12. Sprint 9 — Sessão fim + Gamificação base (E4 fim + E6 base)

> **Objetivo:** uma semana de conteúdo executável de ponta a ponta, com XP e streak.
> **Marco no fim:** M3.

| US | Título | Estimativa |
|---|---|---|
| US-025 | Retomada da sessão a partir do bloco onde parou | 5h |
| US-026 | Fechamento da sessão com XP, streak e prévia | 6h |
| US-036 | Ganho de XP por ações na sessão | 5h |
| US-037 | Streak diário com incremento e reset | 5h |

**Total: 21h.**

> **Nota:** US-036 depende de `doc/conteudo/gamificacao.md` (criado pelo humano) que define valores de XP por ação.

---

## 13. Sprint 10 — Mecânicas de quiz (E5 parte 1)

> **Objetivo:** quatro mecânicas de quiz operando dentro da sessão diária.

| US | Título | Estimativa |
|---|---|---|
| US-027 | Quiz de múltipla escolha com explicação | 6h |
| US-028 | Quiz verdadeiro/falso | 3h |
| US-029 | Completar frase com pool de palavras | 5h |
| US-030 | Ordenar acontecimentos por arrastar | 6h |

**Total: 20h.**

---

## 14. Sprint 11 — Memorização de versículo (E5 parte 2)

> **Objetivo:** ciclo de quatro fases progressivas de memorização do versículo da semana.

| US | Título | Estimativa |
|---|---|---|
| US-031 | Completar versículo por drag-and-drop (Fase 1) | 6h |
| US-033 | Fase 2 — palavras escondidas reveláveis | 5h |
| US-034 | Fase 3 — primeira letra como pista | 5h |
| US-035 | Fase 4 — digitação livre do versículo | 5h |

**Total: 21h.**

---

## 15. Sprint 12 — Caça-palavras + Gamificação avançada (E5 fim + E6 parte 2)

> **Objetivo:** última mecânica especial e selos/escudo do sistema de XP.

| US | Título | Estimativa |
|---|---|---|
| US-032 | Caça-palavras temático 9×9 | 10h |
| US-038 | Escudo de proteção semanal para o streak | 5h |
| US-039 | Selo de semana completa | 5h |

**Total: 20h.**

---

## 16. Sprint 13 — Gamificação fim + Painel pais início (E6 fim + E7 início)

> **Objetivo:** fechar gamificação e começar painel do responsável com o onboarding obrigatório.
> **Marco no fim:** M4.

| US | Título | Estimativa |
|---|---|---|
| US-040 | Selo de trimestre completo | 3h |
| US-041 | Heatmap anual de progresso navegável | 10h |
| US-042 | Onboarding obrigatório do painel | 5h |
| US-048 | Link "Sobre privacidade" sempre acessível | 2h |

**Total: 20h.**

> **Nota:** US-042 é mandatório por ADR-003 — sem este onboarding o painel não é exibido.

---

## 17. Sprint 14 — Painel dos pais completo (E7 fim)

> **Objetivo:** painel mostra sinais positivos, pergunta de discussão, opt-in de reflexão.
> **Marco no fim:** M5.

| US | Título | Estimativa |
|---|---|---|
| US-043 | Painel com sinais positivos da semana | 6h |
| US-044 | Pergunta de discussão familiar | 3h |
| US-045 | Marcar "conversamos sobre isso" | 3h |
| US-046 | Aba "Compartilhado por [apelido]" | 5h |
| US-047 | Compartilhamento opt-in de reflexão | 5h |

**Total: 22h.**

> **Nota:** US-047 reflete ADR-005 — opt-in **por item**, nunca global.

---

## 18. Sprint 15 — Notificações por e-mail (E8 parte 1)

> **Objetivo:** lembrete diário do adolescente + e-mails de sábado para ambos os perfis.

| US | Título | Estimativa |
|---|---|---|
| US-049 | E-mails transacionais essenciais | 6h |
| US-050 | Lembrete diário ao adolescente (configurável) | 5h |
| US-051 | E-mail de sábado ao adolescente com tema da nova semana | 3h |
| US-052 | E-mail ao responsável por semana completa | 3h |
| US-053 | E-mail ao responsável no sábado com tema e pergunta | 3h |

**Total: 20h.**

> **Nota:** todos os e-mails em lote disparam às **05:00 América/São_Paulo** (decisão do founder).

---

## 19. Sprint 16 — E-mails fim + PWA início + LGPD parte 1 (E8 fim + E10 início + LGPD)

> **Objetivo:** descadastramento de e-mail, instalação como PWA, export de dados sob solicitação.
> **Marco no fim:** M6.

| US | Título | Estimativa |
|---|---|---|
| US-054 | Gerenciamento e descadastramento de e-mails | 5h |
| US-058 | Instalação como PWA na tela inicial | 6h |
| US-059 | Prompt de instalação após duas sessões | 3h |
| US-067 | Solicitação manual de exportação de dados | 5h |

**Total: 19h.**

---

## 20. Sprint 17 — PWA offline + LGPD fim + Tracking (E10 fim + LGPD + US-069)

> **Objetivo:** modo offline mínimo, exclusão de conta, instrumentação consolidada.
> **Marco no fim:** M7.

| US | Título | Estimativa |
|---|---|---|
| US-060 | Funcionamento básico offline da sessão carregada | 10h |
| US-068 | Exclusão de conta pelo titular | 6h |
| US-069 | Instrumentação de eventos de produto (PRD §13.1) | 6h |

**Total: 22h.**

> **Nota:** US-069 valida que todos os eventos do PRD §13.1 estão sendo emitidos a partir das US já entregues. Eventos pontuais que faltarem viram tarefas-fix dentro deste sprint.

---

## 21. Sprint 18 — Compartilhamento social parte 1 (E11 início)

> **Objetivo:** primeira leva de cards compartilháveis via WhatsApp/Instagram (ADR-014).

| US | Título | Estimativa |
|---|---|---|
| US-061 | Compartilhar selo conquistado como imagem | 10h |
| US-062 | Compartilhar marco de streak | 4h |
| US-063 | Compartilhar versículo memorizado | 4h |
| US-064 | Compartilhar link de convite | 3h |

**Total: 21h.**

> **Nota:** US-061 inclui a infraestrutura server-side de geração de imagem (reusada por US-062 e US-063).

---

## 22. Sprint 19 — Compartilhamento social fim + Polimento (E11 fim)

> **Objetivo:** controles de privacidade do compartilhamento e janela de polimento de bugs.
> **Marco no fim:** M8.

| US | Título | Estimativa |
|---|---|---|
| US-065 | Controle de apelido a cada compartilhamento | 3h |
| US-066 | Compartilhamentos permitidos ao responsável | 4h |
| chore-poli | Janela de polimento: bugs P2, microcopy, microinterações, perf básica | 13h |

**Total: 20h.**

---

## 23. Sprint 20 — Teste com famílias + Soft launch

> **Objetivo:** validar o produto com usuários reais e estabilizar para lançamento público inicial.
> **Marco no fim:** M9 — fim do MVP v1.

| Task | Descrição | Estimativa |
|---|---|---|
| qa-real | Recrutar e onboardar 3–5 famílias para teste guiado (PRD §13.3) | 4h |
| qa-real | Sessões de observação + coleta de feedback (1h por família) | 5h |
| qa-real | Triagem de achados; abrir Issues para bugs críticos (P0/P1) | 2h |
| qa-real | Correção de bugs P0/P1 detectados | 8h |
| qa-real | Conferência final dos critérios da DoD do PRD §17 | 2h |
| qa-real | Anúncio de soft launch nos canais informais (WhatsApp da igreja, family-and-friends) | 3h |

**Total: 24h.**

---

## 24. Resumo executivo

| Métrica | Valor |
|---|---|
| Sprints até MVP v1 | **20** |
| Tempo total estimado | **~5 meses** a partir do Sprint 1 |
| US no MVP v1 | 69 (todas) |
| Sprints sem US (fundação) | 2 (S1, S2) |
| Sprint final (validação + launch) | 1 (S20) |
| Capacidade total estimada | 480h (20 × 24h) |
| Esforço alocado em US + tasks | ~420h |
| Buffer total embutido | ~60h (~12%) |

---

## 25. O que NÃO está aqui (não-objetivos)

- **Push notifications** — fora do MVP (ADR-002, sai em v1.1).
- **Modo offline completo (criar reflexão sem rede)** — fora (PRD §6.2).
- **Pagamento / in-app purchase** — fora (modelo de monetização vem em v1.5, PRD §14.4).
- **Trimestres anteriores para "completar buracos"** — fora do MVP (entra em v1.5).
- **App nativo (Android/iOS Play Store)** — fora; só PWA na v1.
- **Versão para multiusuário no mesmo device** — fora.

---

## 26. Próximo passo

Encaminhar este plano ao **CTO/Arquiteto** para abrir as **Issues do Sprint 1** (`chore-001` a `chore-010`) no GitHub e iniciar a execução. A primeira Issue da fundação técnica desbloqueia tudo.
