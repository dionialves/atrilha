# Catálogo de User Stories — atrilha

| Campo | Valor |
|---|---|
| Produto | **atrilha** |
| Documento | Catálogo de User Stories — MVP v1 |
| Data | 2026-05-17 |
| Autor | PO (Zayt) |
| Fonte da verdade | `doc/PRD.md` v1.1 (2026-05-16), com foco em §3 (personas), §4 (princípios), §5 (jornadas), §6.1 (escopo MVP), §7 (RF por épico), §11 (LGPD), §16 (ADRs) |
| Status | Revisão 1 — questões em aberto do v1.0 resolvidas |

> **Convenções editoriais do catálogo**
> - Cada US tem número sequencial `US-###` com zero à esquerda.
> - As US estão agrupadas por épico (E1–E11), conforme o PRD.
> - Personas usadas: **Júlia** (adolescente 13–17 anos, primária), **Carlos** (responsável 18+, secundário), **Admin/founder** (operação de conteúdo e LGPD).
> - Critérios de aceitação descrevem comportamento observável pelo usuário. Decisões de stack, schema, endpoints e algoritmos ficam fora — pertencem ao Arquiteto.
> - "Fora do Escopo" referencia PRD §6.2 (não-MVP) e §14 (roadmap) sempre que aplicável.

### Changelog

**v1.1 — 2026-05-17**
- Q1 resolvida: migração 13–17 → 18+ explicitada como pós-MVP em US-009 (fora do escopo).
- Q2 resolvida: critério de substituição de responsável adicionado em US-016.
- Q3 resolvida: termo de consentimento gerado por IA para MVP — critério em US-014.
- Q4 resolvida: política de privacidade gerada por IA para MVP — critério em US-048.
- Q5 resolvida: e-mails em lote (US-051, US-052, US-053) às 05:00 horário de Brasília.
- Q6 resolvida: doc complementar de fluxo da semana + nomes das sessões vira pré-requisito de US-023.
- Q7 resolvida: doc de gamificação vira pré-requisito de US-036.
- Q8 sem mudança: trimestres anteriores permanecem fora do MVP (PRD §14).
- Q9 resolvida: nova US-069 — Instrumentação de eventos de produto, seção "Telemetria e instrumentação (transversal)".
- Seção "Questões em aberto" removida.

**v1.0 — 2026-05-16** — versão inicial com 68 US.

---

## Sumário por Épico

### E1 — Cadastro e autenticação
- US-001 — Cadastro de adolescente por e-mail e senha
- US-002 — Cadastro de adolescente via Google
- US-003 — Cadastro de responsável por e-mail e senha
- US-004 — Cadastro de responsável via Google
- US-005 — Bloqueio de cadastro por idade fora da faixa
- US-006 — Verificação de e-mail
- US-007 — Login recorrente (e-mail/senha e Google)
- US-008 — Recuperação de senha
- US-009 — Edição do perfil do adolescente
- US-010 — Edição do perfil do responsável
- US-011 — Logout de todos os dispositivos

### E2 — Vinculação adolescente ↔ responsável
- US-012 — Geração e exibição do código de vinculação pelo adolescente
- US-013 — Compartilhamento e regeração do código de vinculação
- US-014 — Vinculação do responsável com código + consentimento parental
- US-015 — Limitação funcional do adolescente sem responsável vinculado em 7 dias
- US-016 — Revogação de vinculação pelo adolescente
- US-017 — Notificação ao adolescente quando vinculação é concretizada

### E3 — Trilha e navegação
- US-018 — Trilha da semana atual como tela inicial
- US-019 — Estados dos dias na trilha (bloqueado/disponível/em progresso/concluído)
- US-020 — Liberação da sessão de sábado mediante progresso semanal
- US-021 — Navegação entre semanas do trimestre corrente
- US-022 — Visão de trimestre (13 semanas) acessível em poucos toques

### E4 — Estrutura da sessão diária
- US-023 — Execução da sessão diária com 5 blocos sequenciais
- US-024 — Reflexão privada por default com texto opcional
- US-025 — Retomada da sessão a partir do bloco onde parou
- US-026 — Fechamento da sessão com XP, streak e prévia do próximo dia

### E5 — Mecânicas interativas
- US-027 — Quiz de múltipla escolha com explicação após resposta
- US-028 — Quiz verdadeiro/falso
- US-029 — Completar frase com pool de palavras
- US-030 — Ordenar acontecimentos por arrastar
- US-031 — Completar versículo por drag-and-drop (Fase 1 de memorização)
- US-032 — Caça-palavras temático 9×9
- US-033 — Fase 2 de memorização — palavras escondidas reveláveis
- US-034 — Fase 3 de memorização — primeira letra como pista
- US-035 — Fase 4 de memorização — digitação livre do versículo

### E6 — Gamificação
- US-036 — Ganho de XP por ações na sessão
- US-037 — Streak diário com incremento e reset
- US-038 — Escudo de proteção semanal para o streak
- US-039 — Selo de semana completa
- US-040 — Selo de trimestre completo
- US-041 — Heatmap anual de progresso navegável

### E7 — Painel dos pais
- US-042 — Onboarding obrigatório do painel do responsável
- US-043 — Painel do responsável com sinais positivos da semana
- US-044 — Pergunta de discussão familiar no painel
- US-045 — Marcar "conversamos sobre isso" no painel
- US-046 — Aba "Compartilhado por [apelido]" para reflexões opt-in
- US-047 — Compartilhamento opt-in de reflexão pelo adolescente (com revogação)
- US-048 — Link "Sobre privacidade" sempre acessível no painel

### E8 — Notificações por e-mail
- US-049 — E-mails transacionais essenciais
- US-050 — Lembrete diário ao adolescente, configurável e suprimível
- US-051 — E-mail de sábado ao adolescente com o tema da nova semana
- US-052 — E-mail ao responsável quando a filha completa a semana
- US-053 — E-mail ao responsável no sábado com tema e pergunta familiar
- US-054 — Gerenciamento e descadastramento de e-mails

### E9 — Pipeline de conteúdo (operação interna)
- US-055 — Importação de uma semana de conteúdo pelo admin
- US-056 — Estados editoriais do conteúdo da semana
- US-057 — Exibição ao usuário apenas de conteúdo publicado

### E10 — PWA e instalação
- US-058 — Instalação do atrilha como PWA na tela inicial
- US-059 — Prompt de instalação após duas sessões completas
- US-060 — Funcionamento básico offline da sessão já carregada

### E11 — Compartilhamento social (WhatsApp/Instagram)
- US-061 — Compartilhar selo conquistado como imagem
- US-062 — Compartilhar marco de streak (7, 14, 30, 60, 100 dias)
- US-063 — Compartilhar versículo memorizado
- US-064 — Compartilhar link de convite "junte-se ao atrilha"
- US-065 — Controle de exibição do apelido a cada compartilhamento
- US-066 — Compartilhamentos permitidos ao responsável

### LGPD e direitos do titular (transversal a E1, E2, E7)
- US-067 — Solicitação manual de exportação de dados
- US-068 — Exclusão de conta pelo titular

### Telemetria e instrumentação (transversal)
- US-069 — Instrumentação de eventos de produto

---

## Épico E1 — Cadastro e autenticação

## US-001 — Cadastro de adolescente por e-mail e senha

**Como** Júlia (adolescente, 13–17 anos),
**quero** criar minha conta no atrilha informando e-mail, senha, apelido, data de nascimento e foto opcional,
**para que** eu tenha um perfil próprio e consiga começar minha trilha sem depender de cadastro de outra pessoa.

### Contexto
O produto é do adolescente (ADR-004): a iniciativa de cadastro parte dela. O cadastro precisa ser curto para que a primeira sessão aconteça em até 90 segundos (PRD §7-E1, critério de aceite). Apelido tem entre 3 e 20 caracteres; data de nascimento determina elegibilidade (13–17). Foto é opcional; quando ausente, o sistema apresenta inicial do apelido. O tom é jovem, sem moralismo (P1–P4).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A partir da tela inicial pública, Júlia escolhe "Começar" e seleciona o caminho "Sou adolescente" antes de informar e-mail e senha. |
| 2 | Júlia informa apelido (3–20 caracteres), data de nascimento e, opcionalmente, foto, e consegue enviar o formulário em uma única tela rolável. |
| 3 | Senha com menos de 8 caracteres é recusada com mensagem clara ao lado do campo, sem perder o restante dos dados já preenchidos. |
| 4 | E-mail mal formatado ou já cadastrado é sinalizado inline; outros campos válidos permanecem preenchidos. |
| 5 | Quando o cadastro é aceito, Júlia é levada ao próximo passo do fluxo de ativação (verificação de e-mail e geração de código de vinculação) sem precisar logar de novo. |
| 6 | Se Júlia não informar foto, o avatar default é a inicial do apelido (não há cobrança visual para enviar foto). |
| 7 | Linguagem da tela e mensagens de erro respeita P1–P4: sem moralismo, sem ameaça, sem infantilização. |

### Fora do Escopo
- Cadastro de responsável (US-003 e US-004).
- Cadastro de menores de 13 anos (US-005; bloqueado).
- Múltiplos perfis de adolescente numa mesma conta — fora do MVP (PRD §6.2).
- MFA — fora do MVP (PRD §11.1).

---

## US-002 — Cadastro de adolescente via Google

**Como** Júlia,
**quero** criar minha conta usando minha conta Google,
**para que** eu não precise inventar mais uma senha nem digitar e-mail.

### Contexto
Login social reduz fricção. O Google fornece e-mail verificado, mas o produto ainda precisa do apelido, data de nascimento e foto opcional para montar o perfil de adolescente (RF-E1-10). O fluxo deve preservar a separação entre "adolescente" e "responsável" desde o primeiro toque (RF-E1-03).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A partir da tela inicial, Júlia escolhe "Começar" como adolescente e vê "Continuar com Google" como opção de criação de conta. |
| 2 | Ao autorizar a conta Google, Júlia volta ao atrilha já reconhecida pelo e-mail, sem ter que repetir e-mail nem confirmar senha. |
| 3 | Júlia ainda informa apelido (3–20 chars), data de nascimento e, opcionalmente, foto antes de concluir o cadastro. |
| 4 | Se a conta Google indicar idade que torne Júlia inelegível (ver US-005), o cadastro é interrompido com mensagem apropriada e os dados não são armazenados como conta ativa. |
| 5 | Após sucesso, Júlia segue direto para o próximo passo do fluxo (geração do código de vinculação), sem precisar logar de novo. |

### Fora do Escopo
- Login Google para responsáveis (US-004).
- Vincular conta Google a uma conta de e-mail/senha já existente — fora do MVP.

---

## US-003 — Cadastro de responsável por e-mail e senha

**Como** Carlos (responsável, 18+),
**quero** criar minha conta no atrilha informando nome, e-mail e senha,
**para que** eu possa inserir o código da minha filha, aceitar o consentimento parental e acessar o painel.

### Contexto
O responsável só existe no produto para complementar o perfil do adolescente (ADR-004). Ele pode chegar via link de convite enviado pela filha (`atrilha.app/responsavel`) ou diretamente na tela inicial. O perfil mínimo é nome + e-mail verificado (RF-E1-11). Adulto < 18 anos é bloqueado.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A partir da tela inicial, Carlos escolhe "Sou responsável" e informa nome, e-mail, senha e data de nascimento em uma tela curta. |
| 2 | Tentar enviar senha com menos de 8 caracteres ou e-mail inválido mostra erro inline sem apagar os demais campos. |
| 3 | Ao concluir, Carlos é convidado a inserir o código de vinculação de 6 dígitos da adolescente, podendo pular esse passo e voltar depois. |
| 4 | Conta de responsável criada sem código vinculado existe, mas não dá acesso a nenhum painel até a vinculação ocorrer. |
| 5 | A linguagem comunica a postura não-vigilante (P9) já na tela de cadastro (ex.: "você verá sinais positivos, não horários ou erros"). |

### Fora do Escopo
- Cadastro de responsável com idade < 18 (US-005; bloqueado).
- Vincular múltiplos filhos a um responsável — fora do MVP (PRD §6.2; v1.5 §14.2).
- MFA — fora do MVP.

---

## US-004 — Cadastro de responsável via Google

**Como** Carlos,
**quero** criar minha conta de responsável usando Google,
**para que** eu não precise criar mais uma senha.

### Contexto
Mesma motivação de US-002, agora no caminho "Sou responsável". O produto ainda precisa do nome (RF-E1-11) e da data de nascimento para validar idade ≥ 18.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A partir da tela inicial, Carlos escolhe "Sou responsável" e vê "Continuar com Google" como opção. |
| 2 | Após autorizar, Carlos confirma nome (pré-preenchido pelo Google) e informa data de nascimento. |
| 3 | Idade < 18 interrompe o cadastro com mensagem clara, sem armazenar conta ativa. |
| 4 | Concluído o cadastro, Carlos é convidado a inserir o código da adolescente, podendo pular e voltar depois. |

### Fora do Escopo
- Vincular conta Google a uma conta e-mail/senha existente — fora do MVP.

---

## US-005 — Bloqueio de cadastro por idade fora da faixa

**Como** Júlia (ou Carlos), tentando me cadastrar com idade inválida,
**quero** ver uma mensagem clara e respeitosa explicando que não posso usar o atrilha,
**para que** eu entenda o motivo sem me sentir punida e não fique tentando burlar.

### Contexto
A faixa do produto é 13–17 (adolescente) e 18+ (responsável), conforme ADR-006. A mensagem de bloqueio não deve revelar regra interna (ex.: "menos de 13 não é permitido") para evitar gaming, mas deve ser inequívoca para a pessoa.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | No caminho "Sou adolescente", informar uma data de nascimento que torne a idade inferior a 13 anos resulta em mensagem explicativa, sem criação de conta. |
| 2 | No caminho "Sou adolescente", informar idade ≥ 18 anos resulta em mensagem sugerindo o caminho de responsável, sem criação de conta. |
| 3 | No caminho "Sou responsável", informar idade < 18 resulta em mensagem explicativa, sem criação de conta. |
| 4 | A mensagem usa tom respeitoso (P1, P4); não usa rótulos como "criança" ou "menor de idade" de forma estigmatizante. |
| 5 | Nenhuma das tentativas bloqueadas gera e-mail de verificação nem deixa rastros visíveis ao usuário (sem "conta pendente"). |

### Fora do Escopo
- Permitir cadastro de < 13 com consentimento parental — explicitamente rejeitado (ADR-006).
- Mensagem revelando regra interna detalhada — proibido por design.

---

## US-006 — Verificação de e-mail

**Como** Júlia (ou Carlos), com conta recém-criada por e-mail e senha,
**quero** confirmar que o e-mail é meu clicando num link enviado pelo atrilha,
**para que** ninguém use um e-mail alheio para criar conta e eu possa receber comunicações.

### Contexto
RF-E1-07 exige verificação de e-mail. Contas Google já vêm com e-mail verificado pelo provedor. O fluxo precisa ser tolerante a "fechei a aba" — reenviar o link deve ser trivial.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Logo após o cadastro por e-mail e senha, o usuário vê uma tela "Confirmamos teu e-mail" e recebe a mensagem com um link de verificação. |
| 2 | Clicar no link válido marca o e-mail como verificado e leva o usuário ao próximo passo do fluxo (geração de código no caso da adolescente; inserção de código no caso do responsável). |
| 3 | Clicar em link já usado leva ao login, com mensagem "este e-mail já foi confirmado". |
| 4 | O usuário pode pedir "reenviar e-mail de verificação" a qualquer momento na própria tela pendente. |
| 5 | Enquanto o e-mail não estiver verificado, o usuário pode logar mas vê banner persistente solicitando verificação; funcionalidades sensíveis (vinculação) ficam indisponíveis até confirmar. |

### Fora do Escopo
- Verificação por SMS — fora do MVP.
- Verificação obrigatória para contas Google — desnecessário (já verificadas pelo provedor).

---

## US-007 — Login recorrente (e-mail/senha e Google)

**Como** usuário já cadastrado (Júlia ou Carlos),
**quero** logar de novo com meu método original (e-mail+senha ou Google),
**para que** eu continue a usar o atrilha sem refazer cadastro.

### Contexto
Login precisa ser previsível, com proteção contra força bruta básica (rate limit, PRD §11.8). Mensagens de erro não devem distinguir entre "usuário inexistente" e "senha errada" (boa prática de privacidade).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Na tela inicial, a opção "Já tenho conta" abre a tela de login com e-mail+senha e "Continuar com Google". |
| 2 | Login com credenciais corretas direciona o adolescente à trilha da semana atual e o responsável ao painel (se vinculado) ou à tela de inserção de código (se não). |
| 3 | Credencial incorreta exibe mensagem única "e-mail ou senha incorretos", sem distinguir os dois casos. |
| 4 | Após várias tentativas falhas em sequência, o usuário vê mensagem informando temporariamente bloqueio de novas tentativas (sem revelar exatos limites). |
| 5 | Sessão permanece ativa entre fechamentos do navegador no mesmo dispositivo até logout ou expiração razoável. |

### Fora do Escopo
- MFA — fora do MVP.
- "Lembrar-me" em dispositivos confiáveis com cookie distinto — fora do MVP.

---

## US-008 — Recuperação de senha

**Como** usuário que esqueceu a senha,
**quero** receber um link de redefinição no meu e-mail e criar uma senha nova,
**para que** eu volte ao app sem precisar pedir ajuda.

### Contexto
RF-E1-08 exige link com expiração de 1 hora. Critério de aceite do épico: reset funciona em < 2 minutos após clique. A mensagem do app não deve revelar se o e-mail está cadastrado (privacidade).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Na tela de login, o link "esqueci minha senha" leva a um formulário de e-mail; após enviar, o usuário vê sempre a mensagem "se este e-mail estiver cadastrado, enviamos um link". |
| 2 | Quando o e-mail existe, o usuário recebe a mensagem com link de redefinição dentro do tempo de entrega normal de e-mail transacional. |
| 3 | Clicar no link dentro de 1 hora abre formulário de nova senha; clicar após 1 hora exibe "link expirado, peça um novo". |
| 4 | Definir a nova senha (mínimo 8 caracteres) loga o usuário automaticamente e invalida sessões antigas no servidor. |
| 5 | Cada link de redefinição é de uso único; reabri-lo após o uso mostra "link já utilizado". |

### Fora do Escopo
- Recuperação por SMS ou perguntas de segurança — fora do MVP.

---

## US-009 — Edição do perfil do adolescente

**Como** Júlia,
**quero** editar meu apelido, foto, data de nascimento e e-mail,
**para que** meu perfil reflita quem eu sou hoje.

### Contexto
Direito de correção (LGPD, PRD §11.5). Mudanças não devem quebrar progresso nem desfazer vinculação. Mudança de e-mail exige nova verificação.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A partir de "Meu perfil", Júlia consegue alterar apelido (3–20 chars), foto e data de nascimento. |
| 2 | Trocar o e-mail dispara fluxo de verificação para o novo endereço; até confirmar, o e-mail antigo continua válido para login. |
| 3 | Mudar o apelido reflete no avatar inicial e no painel do responsável vinculado a partir da próxima abertura, sem perder vinculação. |
| 4 | Trocar a data de nascimento que torne Júlia inelegível (ex.: passa de 17 para 18) não apaga a conta, mas inicia o fluxo de migração para conta de responsável (decisão futura) — no MVP, basta bloquear a alteração com mensagem "entre em contato para regularizar". |
| 5 | Nenhuma edição apaga progresso, XP, streak, selos ou versículos memorizados. |

### Fora do Escopo
- Migração automática de conta adolescente → conta "Jovem" (17–35) ao completar 18 anos — pós-MVP (PRD §14 roadmap). No MVP, o sistema continua tratando a conta como adolescente independentemente de a idade ultrapassar 17 durante a janela do MVP; o desenho do gatilho e do fluxo automático será decidido no roadmap pós-MVP.
- Edição de campos do perfil pelo responsável (proibido — princípio de não-vigilância).

---

## US-010 — Edição do perfil do responsável

**Como** Carlos,
**quero** editar meu nome e e-mail,
**para que** meu perfil esteja correto.

### Contexto
Direito de correção, paralelo à US-009. Mudança de e-mail exige nova verificação.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A partir de "Meu perfil", Carlos altera nome e e-mail. |
| 2 | Trocar o e-mail dispara fluxo de verificação; até confirmar, o e-mail antigo permanece válido. |
| 3 | Nenhuma edição quebra a vinculação com a adolescente. |

### Fora do Escopo
- Editar dados da adolescente — proibido pelo princípio P9.

---

## US-011 — Logout de todos os dispositivos

**Como** usuário (Júlia ou Carlos),
**quero** poder desconectar todas as sessões ativas a partir do meu perfil,
**para que** eu encerre acessos esquecidos em dispositivos antigos.

### Contexto
RF-E1-12 (SHOULD). Útil quando o usuário perde o celular ou usou um dispositivo emprestado.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em "Meu perfil > Segurança", há a opção "Sair de todos os dispositivos" com confirmação. |
| 2 | Após confirmar, todas as outras sessões ficam imediatamente inválidas: o usuário precisa logar de novo nos demais navegadores/dispositivos. |
| 3 | A sessão atual (de onde o comando foi dado) permanece ativa, exceto se o usuário optar por sair também. |

### Fora do Escopo
- Listar dispositivos individualmente para desconexão seletiva — fora do MVP.

---

## Épico E2 — Vinculação adolescente ↔ responsável

## US-012 — Geração e exibição do código de vinculação pelo adolescente

**Como** Júlia,
**quero** receber um código de 6 caracteres logo após meu cadastro para entregar ao meu responsável,
**para que** ele possa confirmar que sou eu e aceitar o consentimento parental.

### Contexto
ADR-004: a iniciativa parte da adolescente. RF-E2-01 a RF-E2-04. O código tem 6 caracteres alfanuméricos, sem caracteres ambíguos (0/O, 1/I/L), validade de 7 dias corridos.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Logo após o cadastro e a verificação de e-mail, Júlia vê uma tela "Quem cuida de você precisa autorizar" com seu código de 6 caracteres em destaque. |
| 2 | O código é alfanumérico, fácil de ler em voz alta, e nunca contém caracteres ambíguos como 0/O, 1/I/L. |
| 3 | A tela exibe a validade restante em formato amigável (ex.: "expira em 7 dias"). |
| 4 | A mesma tela é acessível a qualquer momento a partir de "Meu perfil > Vincular responsável" enquanto a conta estiver sem responsável vinculado. |
| 5 | Júlia consegue avançar para a trilha mesmo sem ainda ter vinculado (com limitação após 7 dias, ver US-015). |

### Fora do Escopo
- Vinculação com múltiplos responsáveis — fora do MVP (PRD §6.2; v1.5).

---

## US-013 — Compartilhamento e regeração do código de vinculação

**Como** Júlia,
**quero** poder copiar o código ou compartilhá-lo pelo menu nativo do celular e gerar um código novo se eu precisar,
**para que** seja fácil mandar pro meu pai pelo WhatsApp e eu tenha controle quando o anterior expira ou cai em mãos erradas.

### Contexto
RF-E2-03 (regerar invalida o anterior) e RF-E2-04 (compartilhar via `navigator.share` ou copiar). Tom: ação simples, sem fricção.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Na tela do código, o botão "Compartilhar" abre o menu nativo do celular para enviar por WhatsApp, e-mail ou outro app instalado. |
| 2 | Em navegadores sem menu nativo, o botão "Copiar código" coloca o código na área de transferência e exibe confirmação visual breve. |
| 3 | O botão "Gerar novo código" pede confirmação ("o código atual deixa de funcionar") e, após confirmar, mostra um código novo com nova validade de 7 dias. |
| 4 | Tentar usar o código antigo após regeração resulta em mensagem "código inválido ou expirado" para o responsável. |

### Fora do Escopo
- Compartilhamento via QR Code — fora do MVP (pode ir em v1.1).

---

## US-014 — Vinculação do responsável com código + consentimento parental

**Como** Carlos,
**quero** inserir o código da minha filha, confirmar a identidade pela foto e apelido, ler e aceitar o termo de consentimento parental,
**para que** a conta dela seja desbloqueada e eu tenha acesso ao painel.

### Contexto
RF-E2-05 a RF-E2-09 + ADR-007 + §11.6. Vinculação é o evento legal mais sensível do produto. Exige confirmação visual antes do termo e registro auditável (timestamp, IP, user agent, hash do termo). Cópia do termo é enviada por e-mail. **Texto do termo no MVP:** será **gerado por IA em linguagem simples** (acessível ao público adulto leigo), publicado e versionado antes do launch. A revisão jurídica formal do termo está planejada para **pós-MVP**, após o produto provar engajamento (decisão 2026-05-17).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Carlos insere o código de 6 caracteres (case-insensitive); o sistema mostra apelido e foto da adolescente para confirmação. |
| 2 | Carlos vê o termo de consentimento parental por inteiro, rolável, com checkbox "Sou responsável legal por [apelido] e autorizo o uso do atrilha sob os termos acima" obrigatório. |
| 3 | Sem marcar o checkbox, o botão "Confirmar vinculação" permanece desabilitado. |
| 4 | Ao confirmar, Carlos é levado ao onboarding obrigatório do painel (US-042) e a conta da adolescente é desbloqueada. |
| 5 | Carlos recebe por e-mail uma cópia do termo aceito, com data e hora. |
| 6 | Código expirado, inválido ou de uma adolescente já vinculada exibe mensagem específica e não confirma vinculação. |
| 7 | Confirmar identidade errada (Carlos percebe que não é a filha dele) é reversível pelo botão "Não é ela, voltar" antes de aceitar o termo. |
| 8 | Antes do launch, o texto do termo de consentimento (gerado por IA, em linguagem simples) está publicado, versionado e armazenado em local que permita auditoria LGPD (hash + versão associados a cada aceite). |

### Fora do Escopo
- Substituição de responsável (ex.: divórcio, falecimento, mudança de guarda) como fluxo dedicado — não há US própria; o caminho suportado é a composição US-016 → US-012 → US-014.
- Múltiplos responsáveis por adolescente — fora do MVP (PRD §6.2; v1.5).
- Termos versionados com aceite incremental (re-aceite ao mudar a versão) — fora do MVP (basta hash + versão do termo vigente no momento do aceite).
- Revisão jurídica formal do termo — pós-MVP.

---

## US-015 — Limitação funcional do adolescente sem responsável vinculado em 7 dias

**Como** Júlia, sem responsável vinculado depois de 7 dias,
**quero** entender com clareza por que parte do app deixou de funcionar e o que fazer,
**para que** eu (ou o atendimento) resolva sem me sentir punida.

### Contexto
RF-E2-11 + ADR-007. A conta não pode ficar sem responsável de longo prazo (LGPD). A limitação é funcional, não punitiva: somente a semana atual fica disponível; trilha completa, sessão de sábado, painel próprio de versículos memorizados e pareamento futuro com responsável ficam bloqueados até a vinculação ocorrer.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A partir do 8º dia sem responsável vinculado, Júlia vê banner persistente na trilha explicando que parte das funcionalidades está em espera até o responsável vincular. |
| 2 | A semana atual continua acessível e jogável; semanas anteriores do trimestre, sessão de sábado e visão de trimestre ficam visivelmente bloqueadas, com tooltip "vincule um responsável para destravar". |
| 3 | XP, streak e selos já conquistados permanecem visíveis e não são apagados. |
| 4 | A tela do código de vinculação fica diretamente acessível a partir do banner. |
| 5 | Assim que a vinculação ocorre, todas as funcionalidades destravam imediatamente, sem precisar relogar. |

### Fora do Escopo
- Bloqueio total (login impossível) — explicitamente rejeitado (princípio P9, jornada J1).
- Conta deletada automaticamente após N dias sem vinculação — fora do MVP.

---

## US-016 — Revogação de vinculação pelo adolescente

**Como** Júlia,
**quero** poder revogar a vinculação com meu responsável a qualquer momento,
**para que** eu preserve minha autonomia se a relação mudar, sem perder meu progresso.

### Contexto
RF-E2-10. Revogação é direito do adolescente (princípio P8/P9). Não apaga progresso. Pode ter implicações operacionais (volta para o estado da US-015) — a comunicação ao adolescente precisa antecipar isso. Cenários reais motivadores incluem mudanças de configuração familiar (divórcio, falecimento, mudança de guarda) em que o adolescente precisa trocar o responsável vinculado. No MVP, a "substituição de responsável" se resolve pela composição **US-016 (revogar) → US-012 (gerar novo código) → US-014 (novo responsável vincula e aceita o termo)**.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em "Meu perfil > Vínculo com responsável", Júlia vê quem está vinculado (nome do responsável) e o botão "Revogar vinculação". |
| 2 | Ao clicar, Júlia vê confirmação que explica: o responsável perde acesso imediato ao painel; XP, streak, selos e versículos não são apagados; se nenhum responsável vincular em 7 dias, parte do app fica limitada (US-015). |
| 3 | Após confirmar, o responsável é deslogado do painel daquela adolescente imediatamente e não consegue voltar ao painel sem nova vinculação. |
| 4 | O responsável recebe um e-mail neutro avisando que a vinculação foi revogada. |
| 5 | A janela de 7 dias para nova vinculação reinicia a partir da revogação. |
| 6 | Após a revogação, Júlia pode imediatamente gerar um novo código de vinculação (US-012) e iniciar a vinculação de um novo responsável (US-014) sem restrições adicionais — esse é o caminho suportado para trocar o responsável (ex.: divórcio, falecimento, mudança de guarda). |

### Fora do Escopo
- Revogação iniciada pelo responsável — fora do MVP (relação parte do adolescente).
- Logs de motivos de revogação — fora do MVP.

---

## US-017 — Notificação ao adolescente quando vinculação é concretizada

**Como** Júlia,
**quero** receber um e-mail quando meu responsável conclui a vinculação,
**para que** eu saiba que está tudo certo, mesmo se eu estiver longe do app no momento.

### Contexto
RF-E2-09. Comunicação positiva, breve, sem moralismo (P1). Não inclui dados sensíveis do responsável.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Quando o responsável confirma a vinculação, Júlia recebe um e-mail em poucos minutos com a confirmação. |
| 2 | A mensagem informa o nome (primeiro nome) do responsável vinculado e a data; nada além disso. |
| 3 | O e-mail tem o link padrão "gerenciar minhas notificações" no rodapé (ver US-054). |

### Fora do Escopo
- Notificação push (fora do MVP, vai em v1.1, ADR-002).
- SMS — fora do MVP.

---

## Épico E3 — Trilha e navegação

## US-018 — Trilha da semana atual como tela inicial

**Como** Júlia, já logada e vinculada,
**quero** que a tela inicial do app seja a trilha da semana atual com a sessão de hoje em destaque,
**para que** eu inicie a sessão do dia em um único toque.

### Contexto
RF-E3-01, RF-E3-09, RF-E3-10. A trilha exibe 7 nós (sábado a sábado), tema da semana com ilustração de cabeçalho, e ancoragem visual ao "dia de hoje". Mobile-first em 320px (RNF-COMP-04).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Ao abrir o app logada, Júlia chega na trilha da semana atual com a sessão do dia destacada visualmente. |
| 2 | A trilha mostra 7 nós (um por dia), o título da semana e a ilustração de cabeçalho do tema. |
| 3 | Ao abrir, a tela rola/ancora automaticamente para o nó do dia de hoje, sem ação manual. |
| 4 | Um toque no nó do dia de hoje abre a sessão correspondente. |
| 5 | A trilha permanece utilizável em viewport 320px de largura, sem rolagem horizontal nem corte de elementos críticos. |

### Fora do Escopo
- Visualização desktop com layout exclusivo — atendido como "cortesia" (P12), não como design dedicado.
- Múltiplas trilhas paralelas (vários filhos) — fora do MVP (v1.5).

---

## US-019 — Estados dos dias na trilha (bloqueado/disponível/em progresso/concluído)

**Como** Júlia,
**quero** ver com clareza qual sessão está disponível, qual estou no meio, quais já completei e quais ainda virão,
**para que** eu não fique confusa sobre por onde continuar.

### Contexto
RF-E3-02 a RF-E3-04. Dias futuros ficam bloqueados até sua data nominal (00:00 no fuso do usuário). Dias passados da semana corrente permanecem jogáveis para quem ficou atrasada.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Cada nó da trilha apresenta um estado visualmente distinto entre: bloqueado, disponível, em progresso, concluído. |
| 2 | Tocar em um nó bloqueado exibe a data em que ele será liberado, sem permitir avanço. |
| 3 | Tocar em um nó concluído reabre a sessão em modo "revisão" sem alterar XP nem streak. |
| 4 | Júlia pode iniciar e concluir uma sessão de um dia já passado da semana corrente; isso conta para o selo de semana completa, mas não para streak retroativo. |
| 5 | Mudar o fuso horário do dispositivo não destrava sessões futuras antes da data nominal no fuso configurado no perfil. |

### Fora do Escopo
- Refazer uma sessão concluída para "tentar mais XP" — explicitamente proibido (P11, sem FOMO).
- Sessões de semanas futuras — bloqueadas até a semana começar.

---

## US-020 — Liberação da sessão de sábado mediante progresso semanal

**Como** Júlia,
**quero** que a sessão de sábado fique acessível somente depois que eu tiver completado pelo menos 5 das 6 sessões da semana,
**para que** o fechamento faça sentido com o que vivi nos dias anteriores.

### Contexto
RF-E3-05. A sessão de sábado é especial: recap + memorização Fase 4 + pergunta família (J4).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Antes de completar 5 das 6 sessões anteriores, o nó da sessão de sábado aparece bloqueado com mensagem explicando o critério. |
| 2 | Ao completar a 5ª sessão (de 6), o nó de sábado desbloqueia visualmente, sem necessidade de recarregar. |
| 3 | Mesmo desbloqueada, a sessão de sábado só fica acessível a partir de 00:00 de sábado no fuso da usuária. |
| 4 | Se Júlia chegar ao sábado sem completar 5 sessões, a sessão de sábado permanece bloqueada com o critério à vista; ela ainda pode jogar os dias passados durante o próprio sábado e destravar quando atingir 5. |

### Fora do Escopo
- Permitir fazer sessão de sábado sem nenhum progresso anterior — explicitamente proibido pelo RF.

---

## US-021 — Navegação entre semanas do trimestre corrente

**Como** Júlia,
**quero** poder voltar para semanas anteriores do trimestre que estou cursando e ver como me saí em cada uma,
**para que** eu possa rever conteúdo ou completar dias atrasados.

### Contexto
RF-E3-06. Indicador "concluída", "parcial", "não iniciada". Trimestres anteriores não são acessíveis na v1 (RF-E3-08, fica para premium v1.5).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A partir da trilha, Júlia acessa a lista de semanas do trimestre corrente em um único toque. |
| 2 | Cada semana aparece com um indicador visual de estado: concluída (todas as 7 sessões), parcial (1–6 concluídas), não iniciada (0 concluídas), futura (ainda não começou). |
| 3 | Tocar em uma semana já iniciada abre a trilha daquela semana, respeitando os bloqueios de dias futuros. |
| 4 | Tocar em uma semana futura mostra apenas o título e a data de início, sem permitir avanço. |
| 5 | Semanas de trimestres anteriores não aparecem nesta lista no MVP. |

### Fora do Escopo
- Acesso a trimestres anteriores — fora do MVP (RF-E3-08; vai em v1.5).
- Comparação de desempenho com outros usuários — proibido (P10).

---

## US-022 — Visão de trimestre (13 semanas) acessível em poucos toques

**Como** Júlia,
**quero** ver de uma vez todas as 13 semanas do trimestre,
**para que** eu tenha noção do todo e veja o quanto avancei.

### Contexto
RF-E3-07. Acessível em ≤ 2 toques desde a trilha.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em no máximo dois toques a partir da trilha, Júlia chega à visão das 13 semanas do trimestre corrente. |
| 2 | Cada semana aparece com título e indicador de estado igual ao da US-021. |
| 3 | A visão é navegável até em viewport 320px, sem rolagem horizontal. |

### Fora do Escopo
- Visualização multitrimestre — fora do MVP.

---

## Épico E4 — Estrutura da sessão diária

## US-023 — Execução da sessão diária com 5 blocos sequenciais

**Como** Júlia,
**quero** percorrer a sessão do dia em cinco blocos curtos (gancho, núcleo, quiz, reflexão, fechamento),
**para que** eu sinta progresso dentro da sessão e consiga terminar em até 10 minutos.

### Contexto
RF-E4-01 a RF-E4-06, RF-E4-11. A estrutura é o esqueleto do consumo diário. Indicador de progresso (1/5 ... 5/5) reforça a percepção de avanço. **Nomes das sessões diárias:** os nomes da Lição oficial **não podem ser usados** no produto (ADR-013 e changelog v1.1 do PRD); o atrilha precisa de nomes próprios definitivos para as 7 sessões da semana, escritos no tom da marca, e de uma progressão narrativa explícita ao longo da semana. Esses nomes e a estrutura completa de cada bloco vivem em documento complementar que é pré-requisito desta US.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Ao abrir a sessão do dia, Júlia entra no bloco "gancho", curto (até 30 segundos de leitura), com botão "Continuar". |
| 2 | O bloco "núcleo" apresenta de 3 a 4 cards swipáveis (contexto, texto bíblico, explicação, ponte para aplicação), navegáveis com gesto ou botão. |
| 3 | O bloco "quiz" apresenta 2 perguntas; cada uma mostra explicação imediatamente após a resposta, independentemente de acerto ou erro. |
| 4 | O bloco "reflexão" apresenta uma pergunta aberta com campo de texto (até 1000 caracteres), com indicação visível de que é privada por default. |
| 5 | O bloco "fechamento" exibe XP ganho na sessão, streak atual e prévia textual breve do próximo dia, e marca a sessão como concluída ao toque em "Encerrar". |
| 6 | A sessão exibe indicador de progresso (ex.: "passo 2 de 5") em todos os blocos. |
| 7 | Linguagem dos blocos respeita P1–P5 (sem moralismo, comparação culposa, ameaça, infantilização) e P6 (doutrina ASD preservada). |
| 8 | O texto bíblico exibido nos blocos é da ARC por default (ADR-008). |
| 9 | Cada uma das 7 sessões da semana é apresentada ao usuário pelo seu nome próprio definitivo do atrilha — em nenhum momento o produto exibe os nomes oficiais da Lição (ADR-013). |
| 10 | Antes do início do build desta US, existe documento complementar (`doc/conteudo/fluxo-semana.md` ou nome equivalente — caminho a confirmar pelo Arquiteto) contendo: (a) nomes próprios definitivos das 7 sessões da semana, substituindo os nomes da Lição oficial; (b) estrutura de cada bloco da sessão diária; (c) progressão narrativa ao longo da semana. |

### Fora do Escopo
- Áudio narrado das sessões — fora do MVP (PRD §6.2; v1.1).
- Vídeos curtos — fora do MVP.
- Mecânicas interativas dentro do bloco (quiz V/F, drag, caça-palavras etc.) — cobertas pelas US-027 a US-035.

---

## US-024 — Reflexão privada por default com texto opcional

**Como** Júlia,
**quero** que minha reflexão escrita fique privada por default e que eu não seja obrigada a escrever para terminar a sessão,
**para que** eu sinta que o app respeita meu espaço interno.

### Contexto
RF-E4-05, RF-E4-12 + princípio P8. Reflexão é o dado mais sensível do produto. Texto até 1000 chars. Em branco é aceito.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | O bloco "reflexão" mostra uma etiqueta visível "privado" sempre que o campo de texto está em foco. |
| 2 | Júlia pode avançar para o fechamento da sessão sem digitar nada no campo de reflexão. |
| 3 | Se Júlia digitar e sair da sessão no meio, o texto é preservado para quando ela voltar (ver US-025). |
| 4 | A reflexão não aparece no painel do responsável (P9), exceto se Júlia ativar opt-in individual (US-047). |
| 5 | Tentar digitar acima de 1000 caracteres é impedido com contador visível indicando "restam X". |

### Fora do Escopo
- Reflexão por áudio — fora do MVP.
- Edição da reflexão depois da sessão concluída — fora do MVP (decisão de privacidade: registro do momento).

---

## US-025 — Retomada da sessão a partir do bloco onde parou

**Como** Júlia,
**quero** poder fechar o app no meio da sessão e voltar exatamente no ponto em que parei, com minhas respostas e texto preservados,
**para que** uma interrupção não me obrigue a recomeçar.

### Contexto
RF-E4-08, RF-E4-09. Crítico para a confiança da usuária. A sessão é "salvável a cada bloco".

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Sair de uma sessão em qualquer bloco (sem completar o fechamento) preserva o progresso da sessão como "em progresso" no estado do nó. |
| 2 | Reabrir a sessão depois leva Júlia diretamente ao bloco onde parou, com respostas de quiz, escolhas e texto de reflexão preservados. |
| 3 | A sessão só é marcada como concluída após o bloco fechamento; até lá, o nó na trilha aparece como "em progresso". |
| 4 | Se Júlia trocar de dispositivo, ao reabrir no segundo, ela volta ao mesmo bloco com o mesmo estado salvo. |

### Fora do Escopo
- Edição de respostas já confirmadas em blocos anteriores — fora do MVP (mantém aprendizado linear).

---

## US-026 — Fechamento da sessão com XP, streak e prévia do próximo dia

**Como** Júlia,
**quero** ver, ao terminar a sessão, quanto XP ganhei, em quanto está meu streak e o que vem amanhã,
**para que** eu sinta que valeu a pena e fique curiosa pelo próximo dia.

### Contexto
RF-E4-06, RF-E4-10. Microcelebração curta. Sem dark patterns (P11): nada de "se você não voltar amanhã, perde tudo".

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | O bloco "fechamento" exibe o XP ganho na sessão e o XP total atualizado. |
| 2 | O streak atualizado é apresentado de forma visualmente positiva (ex.: número + ícone). |
| 3 | Uma prévia textual curta do próximo dia (data, título da sessão, gancho de 1 linha) aparece, sem revelar conteúdo crítico. |
| 4 | Mensagens de fechamento respeitam P11: sem cobrança ("não perca o streak!"), sem comparações com outros usuários. |
| 5 | O toque em "Encerrar" marca a sessão como concluída e devolve Júlia à trilha com o nó atualizado para o estado concluído. |

### Fora do Escopo
- Convidar amigos a partir do fechamento — fora do MVP (compartilhamento social é uma ação à parte, US-061 a US-066).

---

## Épico E5 — Mecânicas interativas

## US-027 — Quiz de múltipla escolha com explicação após resposta

**Como** Júlia,
**quero** responder perguntas de múltipla escolha sobre o conteúdo do dia e ver imediatamente se acertei e por quê,
**para que** eu aprenda com o erro sem ser punida.

### Contexto
E5.1 (RF-E5-01 a RF-E5-04) + princípio P7 (erro ensina, não pune). 2–5 alternativas, 1 correta.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A pergunta exibe entre 2 e 5 alternativas, uma única correta. |
| 2 | Ao tocar em uma alternativa, Júlia vê imediatamente feedback visual de correto ou incorreto, com a alternativa certa destacada quando ela errou. |
| 3 | A explicação aparece logo após o feedback, independentemente de ter acertado ou errado. |
| 4 | Não há retentativa obrigatória nem perda de "vida": Júlia segue para a próxima pergunta após ler a explicação. |
| 5 | Acertar concede XP conforme regra de gamificação (ver US-036); errar não subtrai XP. |

### Fora do Escopo
- Penalidade por erro — explicitamente proibido (P7).
- Ranking de acerto entre usuários — proibido (P10).

---

## US-028 — Quiz verdadeiro/falso

**Como** Júlia,
**quero** responder a afirmações com "verdadeiro" ou "falso" e ver a explicação,
**para que** eu teste compreensão de forma rápida.

### Contexto
E5.2 (RF-E5-05). Variante leve do múltipla escolha.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A pergunta apresenta uma afirmação clara e dois botões "Verdadeiro" e "Falso". |
| 2 | Após responder, Júlia vê feedback (correto ou incorreto) e a explicação. |
| 3 | Mesmas regras de XP e ausência de penalidade da US-027 se aplicam. |

### Fora do Escopo
- "Talvez" como terceira opção — fora do MVP.

---

## US-029 — Completar frase com pool de palavras

**Como** Júlia,
**quero** completar a lacuna de uma frase escolhendo entre palavras de um pool com distratores,
**para que** eu pratique vocabulário e atenção sem digitar.

### Contexto
E5.3 (RF-E5-06, RF-E5-07). Frase com 1 ou mais lacunas; pool com 3–6 distratores; tap ou drag para colocar a palavra.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A frase tem uma ou mais lacunas indicadas visualmente; o pool de palavras aparece próximo da frase. |
| 2 | Júlia consegue colocar uma palavra na lacuna por tap (selecionar palavra → tap na lacuna) ou por arrastar. |
| 3 | Júlia consegue remover uma palavra da lacuna devolvendo-a ao pool. |
| 4 | Ao confirmar, palavras corretas aparecem destacadas em uma cor, e palavras incorretas aparecem destacadas em outra com a correta visível ao lado. |
| 5 | A mecânica funciona em viewport 320px (iPhone SE). |

### Fora do Escopo
- Digitação livre como alternativa ao pool — fora do MVP.

---

## US-030 — Ordenar acontecimentos por arrastar

**Como** Júlia,
**quero** colocar 3–6 cards em ordem cronológica arrastando,
**para que** eu fixe a sequência de uma história bíblica.

### Contexto
E5.4 (RF-E5-08, RF-E5-09).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | 3 a 6 cards aparecem em ordem aleatória; Júlia pode arrastá-los para reordenar. |
| 2 | Em mobile, o arrastar funciona com toque sustentado (long press → drag → release). |
| 3 | Ao confirmar, a ordem correta é destacada e Júlia vê quais cards estavam fora da posição. |
| 4 | A mecânica funciona em viewport 320px. |

### Fora do Escopo
- Reordenar após confirmar — fora do MVP.

---

## US-031 — Completar versículo por drag-and-drop (Fase 1 de memorização)

**Como** Júlia, na sessão de segunda-feira,
**quero** completar o versículo da semana arrastando palavras de um pool com distratores próximos,
**para que** eu comece a memorizar de forma ativa.

### Contexto
E5.5 + E5.7 (RF-E5-10 a RF-E5-14, RF-E5-23). Primeira das 4 fases progressivas de memorização do versículo-chave da semana. 30–50% das palavras-chave removidas; distratores semanticamente próximos. Reversível. Mobile-first.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | O versículo aparece com 30 a 50% das palavras-chave substituídas por slots vazios. |
| 2 | O pool contém as palavras corretas mais 2–4 distratores semanticamente próximos. |
| 3 | Júlia pode arrastar uma palavra para o slot e remover de volta ao pool. |
| 4 | Em mobile, a interação alternativa por tap funciona (selecionar palavra → tap no slot). |
| 5 | Ao confirmar, palavras corretas ficam destacadas em verde; incorretas, em vermelho, com a correta visível ao lado. |
| 6 | A referência do versículo (ex.: "Salmos 23:1") fica sempre visível. |

### Fora do Escopo
- Outras fases de memorização (US-033 a US-035).
- Versículos em traduções modernas com direitos autorais — proibido (PRD §12.4; usar ARC).

---

## US-032 — Caça-palavras temático 9×9

**Como** Júlia, na sessão de terça-feira,
**quero** achar 5 a 8 palavras-chave da história da semana num grid 9×9,
**para que** eu fixe vocabulário e tenha uma quebra de ritmo divertida.

### Contexto
E5.6 (RF-E5-15 a RF-E5-21). Grid dinâmico. Palavras em horizontal, vertical, diagonal (sem reversa na v1). Dica opcional custa -2 XP.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | O grid 9×9 aparece com 5 a 8 palavras escondidas em horizontal, vertical ou diagonal. |
| 2 | Uma lista lateral mostra as palavras a encontrar; cada uma é marcada visualmente quando achada. |
| 3 | Júlia consegue selecionar uma palavra com toque sustentado e arrastar até o final dela; ao soltar, se for válida, fica destacada permanentemente no grid. |
| 4 | A mecânica funciona em viewport 320px com toque, e em desktop com mouse drag. |
| 5 | Quando todas as palavras são achadas, o próximo bloco da sessão é liberado. |
| 6 | O botão "Dica" revela a primeira letra de uma palavra ainda não encontrada e subtrai 2 XP, com confirmação prévia para não consumir XP por engano. |

### Fora do Escopo
- Palavras em ordem reversa — fora do MVP (RF-E5-17).
- Modo cronometrado — fora do MVP (P11, sem ansiedade artificial).

---

## US-033 — Fase 2 de memorização — palavras escondidas reveláveis

**Como** Júlia, na sessão de quarta-feira,
**quero** ler o versículo com algumas palavras-chave escondidas em blocos cinza e poder tocar nelas para revelar quando travar,
**para que** eu pratique a memória com apoio.

### Contexto
RF-E5-24. Fase 2 de 4 da memorização progressiva.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | O versículo aparece com algumas palavras-chave substituídas por blocos cinza. |
| 2 | Tocar em um bloco cinza revela a palavra e o bloco permanece visível assim. |
| 3 | Júlia consegue concluir o bloco mesmo revelando todas as palavras (sem penalidade). |
| 4 | A referência do versículo permanece visível. |

### Fora do Escopo
- Penalidade por revelar — proibido (P7).

---

## US-034 — Fase 3 de memorização — primeira letra como pista

**Como** Júlia, na sessão de sexta-feira,
**quero** ver apenas a primeira letra de cada palavra-chave em destaque, com o resto oculto,
**para que** eu treine a recuperação ativa do versículo.

### Contexto
RF-E5-25. Fase 3 de 4.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | O versículo aparece com a primeira letra de cada palavra-chave destacada e o restante das palavras oculto. |
| 2 | Tocar (ou interagir conforme padrão da plataforma) revela a palavra completa correspondente. |
| 3 | A referência permanece visível. |
| 4 | Concluir não exige todas as palavras reveladas. |

### Fora do Escopo
- Cronômetro — proibido (P11).

---

## US-035 — Fase 4 de memorização — digitação livre do versículo

**Como** Júlia, na sessão de sábado,
**quero** digitar o versículo da semana inteiro de memória e ver o quanto acertei,
**para que** eu sinta a recompensa de ter memorizado e adicione o versículo ao meu repositório pessoal.

### Contexto
RF-E5-26 a RF-E5-29 + jornada J4. Acurácia ≥ 90% registra como "memorizado". Botão "Dica" mostra iniciais (estilo Fase 3). A precisão usa critério tolerante a maiúsculas/minúsculas e pontuação.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A tela apresenta a referência do versículo e um campo de texto vazio para Júlia digitar. |
| 2 | O botão "Dica" mostra as primeiras letras de cada palavra-chave (mesmo padrão da Fase 3). |
| 3 | Ao confirmar, Júlia vê uma porcentagem de acurácia em destaque, e o texto correto comparado lado a lado ou abaixo, com diferenças destacadas. |
| 4 | Quando a acurácia é ≥ 90%, o versículo é registrado no repositório pessoal "Versículos memorizados" com data. |
| 5 | A porcentagem ignora diferenças de maiúsculas/minúsculas e pontuação (critério "tolerante"). |
| 6 | Mesmo abaixo de 90%, Júlia consegue concluir a sessão de sábado; pode tentar de novo na próxima semana. |

### Fora do Escopo
- Spaced repetition completo (revisões em 3/7/21 dias) — fora do MVP (RF-E5-30, vai em v1.1).
- Modo voz (ditar em voz alta) — fora do MVP.

---

## Épico E6 — Gamificação

## US-036 — Ganho de XP por ações na sessão

**Como** Júlia,
**quero** ganhar XP por ações concretas (concluir sessão, acertar quiz, escrever reflexão suficiente, completar sessão de sábado),
**para que** eu veja meu progresso somar de forma justa.

### Contexto
RF-E6-01, RF-E6-02. Valores preliminares do PRD §7 E6 — sessão completa (10), acerto quiz (+5), reflexão escrita ≥ 30 chars (+10), sessão de sábado completa (+30) — ficam como **referência inicial**, sujeitos a confirmação ou ajuste no documento complementar de gamificação (pré-requisito desta US). Reflexão em branco não dá XP (mas a sessão pode ser concluída — US-024).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Concluir uma sessão diária (bloco fechamento) concede XP fixo. |
| 2 | Cada acerto no quiz concede XP adicional; errar não subtrai. |
| 3 | Escrever uma reflexão de pelo menos 30 caracteres concede XP de reflexão; reflexão em branco ou abaixo do limite não concede esse XP, mas não impede a conclusão. |
| 4 | Concluir a sessão de sábado concede XP extra de fechamento de semana. |
| 5 | O XP total acumulado é exibido na trilha e no perfil, sempre atualizado. |
| 6 | Refazer uma sessão concluída em modo revisão (US-019) não concede XP novamente. |
| 7 | Antes do início do build desta US, existe documento complementar de gamificação (`doc/gamificacao.md` ou nome equivalente — caminho a confirmar pelo Arquiteto) contendo: (a) valores definitivos de XP por ação (sessão diária, semana completa, streak, memorização, etc.); (b) regras do escudo de proteção de streak (referenciado por US-038); (c) critérios de ganho de selos (US-039, US-040); (d) curva de progresso/níveis, se aplicável. |

### Fora do Escopo
- Trade do XP por itens cosméticos — fora do MVP (vai em doc de gamificação + sprint posterior).
- Nível baseado em XP — RF-E6-03 é SHOULD; fora do escopo desta US (pode virar US própria se priorizado).

---

## US-037 — Streak diário com incremento e reset

**Como** Júlia,
**quero** que meu streak suba a cada dia em que eu concluo pelo menos uma sessão e que ele só zere depois de eu falhar (respeitando o escudo),
**para que** eu sinta consistência sem ser punida por uma única falha.

### Contexto
RF-E6-04, RF-E6-05. Streak incrementa em dias consecutivos com sessão completa. Resetar para 0 só ocorre após 1 dia sem sessão sem escudo (ver US-038). Sensível a fuso horário do usuário.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Concluir uma sessão em um novo dia (no fuso configurado) incrementa o streak em 1. |
| 2 | Concluir mais de uma sessão no mesmo dia não incrementa o streak múltiplas vezes. |
| 3 | Passar um dia inteiro sem sessão zera o streak, exceto se o escudo for consumido automaticamente (ver US-038). |
| 4 | O número do streak é visível na trilha o tempo todo. |
| 5 | Voltar a completar uma sessão depois do reset reinicia o streak em 1. |
| 6 | Mudanças de fuso horário não geram saltos artificiais (ex.: viajar e "ganhar" um dia). |

### Fora do Escopo
- Streak retroativo (completar sessão de dia passado para "recuperar" streak) — explicitamente fora do MVP (RF-E3-04 já indica que não retroage).
- Comparação de streak com outros usuários — proibido (P10).

---

## US-038 — Escudo de proteção semanal para o streak

**Como** Júlia,
**quero** ter 1 escudo de proteção por semana que se gasta automaticamente quando eu falhar um dia,
**para que** uma falha pontual não me quebre o streak.

### Contexto
RF-E6-06, RF-E6-07. Renova no sábado. Consumido sem ação do usuário. Notificação clara.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Júlia vê em seu perfil um indicador do escudo disponível na semana corrente. |
| 2 | Em uma semana, ao passar um dia sem sessão, o escudo é consumido automaticamente e o streak não zera; Júlia recebe notificação no app (e e-mail, opcional) explicando o consumo. |
| 3 | Após o consumo do escudo na semana, uma segunda falha na mesma semana zera o streak normalmente. |
| 4 | O escudo é renovado no sábado e fica disponível para a nova semana. |
| 5 | Acumular escudos entre semanas não é permitido (máximo 1 disponível por vez). |

### Fora do Escopo
- Comprar escudos extras com XP ou dinheiro — fora do MVP.
- Escudo manual ("usar agora") — fora do MVP; o uso é automático.

---

## US-039 — Selo de semana completa

**Como** Júlia,
**quero** ganhar um selo quando completar todas as 7 sessões de uma semana,
**para que** eu colecione e celebre marcos consistentes.

### Contexto
RF-E6-08, RF-E6-14.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Ao concluir a 7ª sessão (sábado) de uma semana, Júlia vê uma animação curta de até 2 segundos, dispensável com toque. |
| 2 | O selo é registrado no perfil "Meus selos" com nome da semana e data. |
| 3 | O selo da semana fica visível na trilha ao revisitar a semana correspondente. |
| 4 | A animação não bloqueia a navegação por mais de 2 segundos. |

### Fora do Escopo
- Trade de selos — fora do MVP.

---

## US-040 — Selo de trimestre completo

**Como** Júlia,
**quero** ganhar um selo especial quando completar as 13 semanas do trimestre,
**para que** eu tenha um marco simbólico maior.

### Contexto
RF-E6-09.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Concluir a sessão de sábado da 13ª semana com selo de semana completa concede o selo de trimestre. |
| 2 | O selo é distinto visualmente dos selos semanais. |
| 3 | O selo é registrado em "Meus selos" com nome do trimestre e data. |
| 4 | A animação de celebração dura no máximo 2 segundos e é dispensável. |

### Fora do Escopo
- Selo de ano completo (4 trimestres) — fora do MVP (pode ir em v1.5).

---

## US-041 — Heatmap anual de progresso navegável

**Como** Júlia,
**quero** ver um heatmap anual estilo grade colorida com a intensidade de uso por dia,
**para que** eu visualize meu histórico geral.

### Contexto
RF-E6-10, RF-E6-11. Visual estilo GitHub contributions: cinza (sem atividade), 3 tons de laranja crescentes. Tap em dia mostra resumo da sessão.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em "Meu perfil > Histórico", Júlia vê o heatmap anual com cada dia representado por uma célula. |
| 2 | Dias sem atividade aparecem em cinza; dias com atividade aparecem em 1 de 3 tons de laranja crescentes conforme intensidade (ex.: 1 sessão / 2 / 3+). |
| 3 | Tocar em um dia colorido mostra um resumo breve da sessão daquele dia (semana, sessão, status). |
| 4 | O heatmap é navegável (rolagem) em mobile sem corte e funciona em viewport 320px. |

### Fora do Escopo
- Heatmap com vários anos lado a lado — fora do MVP (apenas ano corrente).
- Comparar heatmap com amigos — proibido (P10).

---

## Épico E7 — Painel dos pais

## US-042 — Onboarding obrigatório do painel do responsável

**Como** Carlos, recém-vinculado,
**quero** passar por um tutorial curto de 3 telas explicando o que verei e o que NÃO verei,
**para que** eu use o painel já entendendo o contrato de privacidade e não fique cobrando minha filha pelo que ele não mostra.

### Contexto
ADR-003 + RF-E7-06 + jornada J2. Obrigatório no primeiro acesso. Crítico para que o produto não vire ferramenta de vigilância.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | No primeiro acesso ao painel, Carlos é levado a um tutorial de 3 telas que ele só consegue pular após chegar à última. |
| 2 | A 1ª tela explica o que ele verá (heatmap binário, streak, tema da semana, pergunta de discussão, versículos memorizados). |
| 3 | A 2ª tela explica o que ele NÃO verá (reflexões, horários de uso, % de acerto em quiz, tempo gasto, dias específicos sem atividade). |
| 4 | A 3ª tela orienta como conversar no sábado a partir da pergunta de discussão. |
| 5 | Carlos só chega ao dashboard depois de avançar todas as 3 telas. |
| 6 | Carlos pode revisitar o tutorial a qualquer momento a partir do painel. |

### Fora do Escopo
- Onboarding ramificado por perfil — fora do MVP.

---

## US-043 — Painel do responsável com sinais positivos da semana

**Como** Carlos, vinculado e onboardado,
**quero** ver no painel um resumo positivo da semana da minha filha (heatmap binário, streak atual, tema da semana, versículos memorizados),
**para que** eu acompanhe sem invadir.

### Contexto
RF-E7-01 a RF-E7-03, RF-E7-10 + princípio P9. Heatmap no painel pai é binário (fez/não fez), sem intensidade. Sem dados de quiz, tempo, horário, ausências como métrica negativa.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | O painel exibe um heatmap da semana corrente em escala binária (preencheu/não preencheu) por dia, sem intensidade nem horário. |
| 2 | O streak atual da adolescente aparece com número. |
| 3 | O tema da semana corrente aparece com título e ilustração de cabeçalho. |
| 4 | A lista de versículos memorizados do trimestre aparece com referência e data. |
| 5 | O painel NÃO exibe: texto de reflexão (exceto na aba opt-in, US-046), horário de uso, % de acerto em quiz, tempo gasto na sessão, marcadores específicos de dias sem atividade como "alerta". |
| 6 | Nenhuma das mensagens do painel usa linguagem negativa ou de cobrança. |

### Fora do Escopo
- Visão histórica do heatmap (heatmap anual) no painel pai — fora do MVP.
- Painel multi-filho — fora do MVP (v1.5).

---

## US-044 — Pergunta de discussão familiar no painel

**Como** Carlos,
**quero** ver no painel a pergunta de discussão familiar da semana corrente,
**para que** eu tenha um assunto natural pra puxar no sábado.

### Contexto
RF-E7-04 + jornada J4. Pergunta gerada pelo conteúdo da semana, campo fixo no YAML do conteúdo.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A pergunta da semana corrente aparece com destaque no painel, claramente identificada como "Para conversar em família". |
| 2 | A pergunta é coerente com o tema da semana exibido no painel. |
| 3 | A pergunta não revela qualquer dado privado da adolescente (não cita reflexões dela). |

### Fora do Escopo
- Histórico de perguntas anteriores no painel — fora do MVP.

---

## US-045 — Marcar "conversamos sobre isso" no painel

**Como** Carlos,
**quero** marcar a pergunta de discussão como "conversamos sobre isso" depois de falar com minha filha,
**para que** eu deixe registrado meu cuidado sem cobrar nada dela.

### Contexto
RF-E7-05 + métrica `guardian_marked_discussed` (PRD §13.1).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A pergunta de discussão tem um botão simples "Conversamos sobre isso". |
| 2 | Após tocar, o botão muda para um estado "conversamos em [data]" e fica desabilitado para nova marcação naquela semana. |
| 3 | A marcação não dispara notificação para a adolescente (P9). |
| 4 | A marcação não é visível para a adolescente em seu próprio app. |

### Fora do Escopo
- Avaliar a conversa (rating) — fora do MVP.

---

## US-046 — Aba "Compartilhado por [apelido]" para reflexões opt-in

**Como** Carlos,
**quero** que reflexões compartilhadas por minha filha apareçam em uma aba separada do painel,
**para que** elas não sejam misturadas com sinais quantitativos e fique claro que estou vendo algo que ela escolheu compartilhar.

### Contexto
RF-E7-08. A aba só aparece se houver pelo menos 1 reflexão compartilhada. Reforça a postura de respeito a P8.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Se a adolescente nunca compartilhou nenhuma reflexão, a aba "Compartilhado por [apelido]" não aparece. |
| 2 | Quando há reflexões compartilhadas, a aba aparece com o nome dela ("Compartilhado por [apelido]"). |
| 3 | A aba lista cada reflexão com data da sessão, tema, e o texto integral. |
| 4 | Nenhuma das reflexões aparece misturada com o dashboard padrão. |
| 5 | Se a adolescente revogar o compartilhamento (US-047), a reflexão some imediatamente da aba. |

### Fora do Escopo
- Notificar o responsável quando algo é compartilhado — fora do MVP (decisão de não-vigilância; ela conta se quiser).
- Comentar a reflexão pelo painel — fora do MVP.

---

## US-047 — Compartilhamento opt-in de reflexão pelo adolescente (com revogação)

**Como** Júlia,
**quero** poder marcar uma reflexão específica como "compartilhar com meu responsável" e poder revogar isso a qualquer momento,
**para que** eu tenha controle individual sobre cada texto que sai do meu espaço privado.

### Contexto
ADR-005 + RF-E7-08, RF-E7-09 + princípio P8. Opt-in por item, nunca global.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em "Minhas reflexões", cada reflexão tem um toggle individual "Compartilhar com meu responsável", off por default. |
| 2 | Não existe nenhum botão "compartilhar todas" nem configuração global de compartilhamento. |
| 3 | Ao ligar o toggle, a reflexão passa a aparecer na aba "Compartilhado por [apelido]" do responsável (US-046) na próxima abertura. |
| 4 | Ao desligar o toggle, a reflexão some imediatamente da aba do responsável, mesmo se ele estiver com o painel aberto (some na próxima atualização da tela). |
| 5 | Júlia pode ver, em "Minhas reflexões", quais estão atualmente compartilhadas. |

### Fora do Escopo
- Opt-in global — explicitamente proibido (ADR-005).
- Compartilhamento parcial (trechos) da mesma reflexão — fora do MVP.

---

## US-048 — Link "Sobre privacidade" sempre acessível no painel

**Como** Carlos,
**quero** ter acesso permanente a uma página explicando o que vejo e o que não vejo e por quê,
**para que** eu lembre do contrato e possa mostrar a outras pessoas que questionarem.

### Contexto
RF-E7-07. **Política de privacidade pública no MVP:** será **gerada por IA em linguagem simples**, acessível tanto ao público adolescente quanto adulto. A revisão jurídica formal está planejada para **pós-MVP**, após o produto provar engajamento (decisão 2026-05-17).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em todas as telas do painel há um link discreto "Sobre privacidade". |
| 2 | A página explica detalhadamente o que aparece, o que não aparece e por quê (linguagem clara, não jurídica). |
| 3 | A página inclui link para a política de privacidade completa do produto. |
| 4 | Antes do launch, a página de privacidade (texto gerado por IA em linguagem simples e acessível ao público adolescente e adulto) está publicada e disponível a partir do link "Sobre privacidade" no painel. |

### Fora do Escopo
- Versionamento dessa página com aceite — fora do MVP.
- Revisão jurídica formal do texto — pós-MVP.

---

## Épico E8 — Notificações por e-mail

## US-049 — E-mails transacionais essenciais

**Como** usuário (Júlia ou Carlos),
**quero** receber e-mails transacionais nos momentos certos (verificação, recuperação de senha, código de vinculação, confirmação de vinculação),
**para que** eu confie que o app está respondendo aos meus passos.

### Contexto
RF-E8-01. Cobre os 4 e-mails operacionais essenciais ao fluxo de cadastro e vinculação. Todos com link "gerenciar notificações" (RF-E8-08).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Após cadastro por e-mail e senha, o usuário recebe e-mail de verificação contendo link de confirmação (ver US-006). |
| 2 | Ao solicitar recuperação de senha, o usuário recebe e-mail com link de redefinição (ver US-008). |
| 3 | Ao gerar o código de vinculação, o adolescente pode optar por enviar o código por e-mail para si mesmo (para repasse posterior). |
| 4 | Após confirmação de vinculação, ambos (adolescente e responsável) recebem e-mail; o do responsável inclui cópia do termo aceito (ver US-014, US-017). |
| 5 | Cada e-mail inclui no rodapé o link "gerenciar minhas notificações" e "descadastrar" (ver US-054). |

### Fora do Escopo
- Personalização de templates pelo usuário — fora do MVP.

---

## US-050 — Lembrete diário ao adolescente, configurável e suprimível

**Como** Júlia,
**quero** receber um e-mail por dia me lembrando da sessão, no horário que eu escolher (ou desativar),
**para que** eu não esqueça mas não seja invadida.

### Contexto
RF-E8-02, RF-E8-03. Default 18h00 no fuso do usuário. Suprimido se a sessão do dia já foi feita.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em "Minhas notificações", Júlia consegue alterar o horário do lembrete diário (em intervalos razoáveis) ou desativar inteiramente. |
| 2 | O default do horário é 18h00 do fuso configurado no perfil. |
| 3 | Quando Júlia já completou a sessão do dia antes do horário do lembrete, o e-mail daquele dia não é enviado. |
| 4 | A mensagem é positiva, breve e respeita P1 e P11 (sem cobrança, sem moralismo). |
| 5 | Cada e-mail traz no rodapé o link "gerenciar minhas notificações". |

### Fora do Escopo
- Lembrete por SMS ou push — fora do MVP (push vai em v1.1, ADR-002).
- Múltiplos lembretes por dia — fora do MVP.

---

## US-051 — E-mail de sábado ao adolescente com o tema da nova semana

**Como** Júlia,
**quero** receber, todo sábado, um e-mail introduzindo o tema da semana que começa,
**para que** eu chegue na semana com contexto e curiosidade.

### Contexto
RF-E8-04. Horário operacional do disparo em lote: **05:00 (horário de Brasília)** — decisão 2026-05-17.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Todo sábado, às **05:00 (horário de Brasília)**, Júlia recebe um e-mail com título e prévia do tema da nova semana. |
| 2 | A mensagem inclui link direto para abrir a trilha já na semana nova. |
| 3 | O tom respeita P1 e P11; não cobra a semana anterior. |
| 4 | O e-mail pode ser desativado em "Minhas notificações". |

### Fora do Escopo
- E-mail de prévia diária da próxima sessão — fora do MVP.

---

## US-052 — E-mail ao responsável quando a filha completa a semana

**Como** Carlos,
**quero** receber um e-mail quando minha filha completar todas as 7 sessões da semana,
**para que** eu celebre com ela.

### Contexto
RF-E8-05, RF-E8-07 + princípio P9. Sempre positivo; nunca "ela não fez". Horário operacional do disparo em lote: **05:00 (horário de Brasília)** — decisão 2026-05-17. O e-mail consolida a conclusão da semana e é enviado no sábado seguinte (após o fechamento, na próxima janela de disparo).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Quando a adolescente completa a sessão de sábado e fecha a semana, Carlos recebe o e-mail de conclusão na próxima janela de disparo em lote, às **05:00 (horário de Brasília)**. |
| 2 | O e-mail diz que a semana foi concluída e cita o título da semana, sem expor detalhes de reflexão, horários ou desempenho de quiz. |
| 3 | Nenhuma mensagem ao responsável menciona dias sem atividade, atrasos ou semanas incompletas. |
| 4 | O e-mail pode ser desativado em "Minhas notificações" do responsável. |

### Fora do Escopo
- Notificação ao responsável quando a filha NÃO completa a semana — explicitamente proibido (RF-E8-07, P9).

---

## US-053 — E-mail ao responsável no sábado com tema e pergunta familiar

**Como** Carlos,
**quero** receber no sábado um e-mail com o tema da semana que começa e a pergunta de discussão familiar,
**para que** eu chegue preparado para conversar com minha filha.

### Contexto
RF-E8-06. Horário operacional do disparo em lote: **05:00 (horário de Brasília)** — decisão 2026-05-17.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Todo sábado, às **05:00 (horário de Brasília)**, Carlos recebe um e-mail com o tema da semana que começa e a pergunta de discussão familiar. |
| 2 | A mensagem inclui link direto para o painel. |
| 3 | O e-mail pode ser desativado em "Minhas notificações". |

### Fora do Escopo
- E-mail diário ao responsável — fora do MVP.

---

## US-054 — Gerenciamento e descadastramento de e-mails

**Como** usuário (Júlia ou Carlos),
**quero** poder gerenciar quais e-mails recebo e me descadastrar das comunicações não-essenciais,
**para que** eu controle minha caixa.

### Contexto
RF-E8-08. Compliance + LGPD.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A partir do rodapé de qualquer e-mail, o usuário acessa "Gerenciar notificações" e vê uma lista das categorias (lembrete diário, sábado, conclusão de semana). |
| 2 | Cada categoria pode ser ativada ou desativada individualmente. |
| 3 | E-mails essenciais (verificação de e-mail, recuperação de senha, confirmação de vinculação) não podem ser desativados — aparecem explicitamente como "essenciais" sem toggle. |
| 4 | "Descadastrar de tudo (não-essenciais)" é uma ação explícita e desliga todas as categorias opcionais de uma vez. |
| 5 | Mudanças refletem nas próximas mensagens enviadas. |

### Fora do Escopo
- Granularidade por evento (ex.: ativar lembrete só em dias úteis) — fora do MVP.

---

## Épico E9 — Pipeline de conteúdo (operação interna)

## US-055 — Importação de uma semana de conteúdo pelo admin

**Como** admin/founder,
**quero** importar uma nova semana do repositório de conteúdo para o banco do atrilha de forma idempotente,
**para que** o conteúdo passe a estar disponível aos usuários ao ser publicado, sem risco de duplicação.

### Contexto
RF-E9-01 a RF-E9-05. Importador invocável via CLI ou CI/CD. Idempotente. Critério de aceite: ≤ 30 segundos para importar uma semana.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | A partir do repositório de conteúdo (Git), o admin consegue importar uma semana específica para o ambiente desejado (dev/staging/produção). |
| 2 | Rodar a importação duas vezes seguidas para a mesma semana não cria entradas duplicadas; a segunda execução é silenciosa ou apenas confirma "sem mudanças". |
| 3 | Erros no formato do conteúdo (YAML inválido, campos faltando) abortam a importação com mensagem clara apontando o arquivo e o erro, sem alterar o banco. |
| 4 | Importação bem-sucedida atualiza o status da semana conforme metadado do arquivo (rascunho/revisão/aprovado/publicado), e somente conteúdo "publicado" passa a aparecer aos usuários finais. |
| 5 | A importação de uma semana com conteúdo válido completa em tempo perceptivelmente curto (alvo: ≤ 30 segundos por semana). |

### Fora do Escopo
- Interface gráfica para edição inline do conteúdo — fora do MVP.
- Importação automática agendada — fora do MVP (CLI/CI/CD basta).

---

## US-056 — Estados editoriais do conteúdo da semana

**Como** admin/founder,
**quero** que cada semana tenha um estado claro (rascunho, revisão, aprovado, publicado),
**para que** eu controle o que chega aos usuários e não publique nada por engano.

### Contexto
RF-E9-06, RF-E9-07.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Cada semana tem exatamente um dos estados: rascunho, revisão, aprovado, publicado. |
| 2 | Apenas semanas no estado "publicado" aparecem para os usuários finais (adolescente e responsável). |
| 3 | Mudança de estado é feita pelo admin via metadado/comando, e a mudança refletida pela importação (US-055). |
| 4 | Tentar publicar uma semana com erros (ex.: faltando quiz, sem pergunta familiar) é impedido com mensagem clara. |

### Fora do Escopo
- Aprovação de conteúdo por múltiplos revisores no app — fora do MVP (revisão acontece no Git).
- Histórico de mudança de estado dentro do app — fora do MVP.

---

## US-057 — Exibição ao usuário apenas de conteúdo publicado

**Como** Júlia,
**quero** que somente conteúdo aprovado e publicado apareça pra mim,
**para que** eu não veja rascunhos, esboços ou conteúdo em revisão.

### Contexto
Espelho da US-056 do ponto de vista do usuário final. RF-E9-07.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Júlia nunca vê semanas em estado rascunho, revisão ou aprovado (apenas publicado). |
| 2 | Se a semana corrente ainda não estiver publicada na data de início programada, a trilha mostra um estado de "conteúdo da semana ainda não disponível", sem expor estados internos. |
| 3 | Sessões dentro de uma semana publicada estão todas disponíveis conforme regras de bloqueio por dia (US-019, US-020). |

### Fora do Escopo
- Mensagens diferenciadas por estado interno — fora do MVP (não vaza estado editorial).

---

## Épico E10 — PWA e instalação

## US-058 — Instalação do atrilha como PWA na tela inicial

**Como** Júlia (ou Carlos),
**quero** instalar o atrilha como app no meu celular a partir do navegador,
**para que** eu acesse com um toque no ícone como qualquer outro app.

### Contexto
RF-E10-01, RF-E10-05. O manifesto e o ícone precisam estar corretos para Android (Chrome) e iOS (Safari 16+).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em Android Chrome, a partir do menu do navegador, "Adicionar à tela inicial" cria um ícone do atrilha na home. |
| 2 | Em iOS Safari 16+, a partir do menu de compartilhamento, "Adicionar à tela de início" cria um ícone do atrilha na home. |
| 3 | Ao abrir pelo ícone, o app abre em modo standalone (sem barra de navegador). |
| 4 | O ícone e o nome do app aparecem corretamente, sem placeholder. |
| 5 | O app funciona em Chrome Android, Safari iOS 16+, Chrome desktop e Edge desktop. |

### Fora do Escopo
- App nativo via Capacitor — fora do MVP (v2, PRD §14.3).

---

## US-059 — Prompt de instalação após duas sessões completas

**Como** Júlia,
**quero** que o convite para instalar o app só apareça depois que eu já tiver usado um pouco,
**para que** eu não seja interrompida no primeiro toque.

### Contexto
RF-E10-04. Sem dark patterns; o convite é após 2 sessões completas, nunca no primeiro acesso.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | No primeiro acesso, nenhum prompt de instalação aparece. |
| 2 | Após Júlia concluir 2 sessões completas no navegador, o app mostra (uma única vez) o prompt de instalação. |
| 3 | Júlia pode dispensar o prompt sem prejuízo; ele não reaparece com frequência incômoda. |
| 4 | Em iOS, onde não há prompt nativo, o app exibe uma instrução visual breve sobre "Adicionar à tela de início" na mesma condição (após 2 sessões). |

### Fora do Escopo
- Reapresentar o prompt periodicamente — fora do MVP (P11, sem FOMO).

---

## US-060 — Funcionamento básico offline da sessão já carregada

**Como** Júlia, com conexão ruim ou momentaneamente offline,
**quero** que a sessão que já carreguei continue funcionando,
**para que** uma perda de sinal não me impeça de concluir.

### Contexto
RF-E10-02, RF-E10-03 + princípio P13 (Brasil real, conexão ruim). O escopo é mínimo: assets estáticos + sessão de hoje uma vez carregada. Modo offline robusto vai para v1.5 (PRD §14.2 e §6.2).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Se Júlia já abriu a sessão do dia e o app perde a conexão, ela consegue continuar percorrendo os blocos da mesma sessão. |
| 2 | Sem conexão, ao tentar abrir uma sessão que ainda não foi carregada, o app exibe uma mensagem clara explicando o problema e o que ela pode continuar fazendo. |
| 3 | Respostas dadas offline são sincronizadas com o servidor assim que a conexão volta, sem perda. |

### Fora do Escopo
- Cache completo de um trimestre — fora do MVP (vai em v1.5).
- Modo "100% offline" — fora do MVP.

---

## Épico E11 — Compartilhamento social (WhatsApp/Instagram)

## US-061 — Compartilhar selo conquistado como imagem

**Como** Júlia,
**quero** compartilhar uma imagem do meu selo conquistado no WhatsApp ou nos Stories do Instagram,
**para que** eu celebre publicamente sem expor dados privados.

### Contexto
ADR-014 + RF-E11-01, RF-E11-05 a RF-E11-09. Imagens server-side em 1080×1920 (Stories) e 1080×1080 (feed). Apelido opcional por compartilhamento. Sem dados privados na imagem.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em "Meus selos", cada selo conquistado tem um botão "Compartilhar". |
| 2 | Ao tocar, Júlia vê uma prévia da imagem (formato Stories e feed) e um toggle "Mostrar meu apelido" (default ligado, pode desligar). |
| 3 | O botão de compartilhar abre o menu nativo do celular quando disponível (Web Share API). |
| 4 | Em navegadores sem menu nativo, há fallback para WhatsApp (link direto que abre o app com mensagem pré-formatada) e, para Instagram, download da imagem com instrução "publique nos Stories". |
| 5 | A imagem nunca contém: e-mail, idade, dados do responsável, reflexões, % de quiz. |
| 6 | A geração da imagem do selo é rápida (alvo: ≤ 800ms p95). |
| 7 | Quando o apelido é desligado, a imagem fica genérica (sem identificação pessoal). |

### Fora do Escopo
- Compartilhar selo de outra pessoa — proibido.
- Publicar direto no Instagram via API — fora do MVP (sem integração oficial).

---

## US-062 — Compartilhar marco de streak (7, 14, 30, 60, 100 dias)

**Como** Júlia,
**quero** compartilhar uma imagem celebratória quando atingir marcos importantes de streak,
**para que** eu marque a continuidade do hábito sem ranking público.

### Contexto
RF-E11-02 + marcos: 7, 14, 30, 60, 100 dias. Sem comparação com outros (P10).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Ao atingir um dos marcos (7, 14, 30, 60, 100), Júlia vê uma opção destacada "Compartilhar este marco". |
| 2 | A imagem gerada destaca o número do streak e a marca atrilha. |
| 3 | Mesmas regras de privacidade e fluxo de Web Share + fallback da US-061 se aplicam. |
| 4 | Marcos passados continuam compartilháveis (Júlia pode voltar e compartilhar um marco anterior). |

### Fora do Escopo
- Streak comparativo com outros usuários — proibido (P10).

---

## US-063 — Compartilhar versículo memorizado

**Como** Júlia,
**quero** compartilhar uma imagem de um versículo que memorizei com sucesso na Fase 4,
**para que** eu celebre a conquista cognitiva.

### Contexto
RF-E11-03 + critério da Fase 4 (≥ 90% acurácia, US-035). Imagem com texto do versículo + referência + marca atrilha.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em "Versículos memorizados", cada versículo tem botão "Compartilhar". |
| 2 | A imagem gerada exibe o texto do versículo (ARC), a referência e a marca atrilha. |
| 3 | O apelido na imagem é opcional, controlado pelo mesmo toggle das outras US de compartilhamento. |
| 4 | Mesmas regras de Web Share + fallback se aplicam. |

### Fora do Escopo
- Compartilhar versículo de outra tradução — fora do MVP (apenas ARC; PRD §6.2; ADR-008).

---

## US-064 — Compartilhar link de convite "junte-se ao atrilha"

**Como** Júlia (ou Carlos),
**quero** compartilhar um link de convite para outras famílias entrarem no atrilha,
**para que** o produto cresça organicamente entre amigos.

### Contexto
RF-E11-04. Pré-visualização rica com OG tags. Tracking de `share_initiated`, `invite_clicked`, `invite_converted` (PRD §13.1).

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em "Convide um amigo" (acessível em pelo menos um lugar visível no app e no painel), Júlia ou Carlos consegue compartilhar um link de convite via menu nativo do celular. |
| 2 | O link, quando colado no WhatsApp ou Instagram, exibe pré-visualização com título e ilustração do produto. |
| 3 | O link não revela dados pessoais de quem convidou — apenas atribui internamente a conversão à origem (opaco para terceiros). |
| 4 | Quem clica no link cai na landing pública do atrilha. |
| 5 | Fallback de copiar link funciona em navegadores sem menu nativo. |

### Fora do Escopo
- Recompensas por indicação — fora do MVP.
- Códigos de convite com expiração — fora do MVP.

---

## US-065 — Controle de exibição do apelido a cada compartilhamento

**Como** Júlia,
**quero** decidir, em cada compartilhamento, se meu apelido aparece na imagem,
**para que** eu mantenha privacidade quando achar melhor.

### Contexto
RF-E11-09. Default: ON. Pode desligar por compartilhamento. Sem opção global silenciosa.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Toda tela de prévia de compartilhamento (selo, marco de streak, versículo) exibe o toggle "Mostrar meu apelido" claramente. |
| 2 | O default é ligado. |
| 3 | Ao desligar, a prévia atualiza imediatamente mostrando a imagem sem o apelido. |
| 4 | A preferência não persiste entre compartilhamentos (cada um exige escolha consciente). |

### Fora do Escopo
- Apelido padrão diferente do apelido do perfil — fora do MVP.

---

## US-066 — Compartilhamentos permitidos ao responsável

**Como** Carlos,
**quero** poder compartilhar apenas o link de convite e o selo da minha filha (com o consentimento implícito ao habilitar a feature),
**para que** eu participe da divulgação sem ferir a privacidade dela.

### Contexto
RF-E11-12. O responsável NUNCA compartilha reflexões, dados pessoais, streak da adolescente. Só selo (conquista pública) e link de convite.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | No painel do responsável, há opção "Compartilhar convite" que produz o mesmo tipo de link da US-064. |
| 2 | Quando a adolescente conquista um selo, Carlos consegue compartilhar o selo dela em imagem celebratória semelhante à da US-061, sem apelido por default. |
| 3 | Nenhum botão de compartilhar exibe nem permite enviar: reflexões, % de quiz, horários, idade exata, marcos de streak (apenas a adolescente compartilha o próprio streak). |

### Fora do Escopo
- Compartilhar marcos de streak pelo responsável — fora do MVP (apenas pela adolescente, US-062).
- Compartilhar versículos memorizados pelo responsável — fora do MVP (apenas pela adolescente, US-063).

---

## LGPD e direitos do titular (transversal a E1, E2, E7)

## US-067 — Solicitação manual de exportação de dados

**Como** usuário (Júlia, com anuência do responsável, ou Carlos),
**quero** poder solicitar uma exportação dos meus dados via formulário,
**para que** eu exerça meu direito à portabilidade (LGPD).

### Contexto
ADR-009 + PRD §11.5. Processo manual no MVP; admin gera export em até 15 dias.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em "Meu perfil > Meus dados", há um botão "Solicitar exportação dos meus dados". |
| 2 | Ao solicitar, o usuário preenche um formulário curto (motivo opcional, e-mail de contato confirmado) e vê confirmação "recebemos sua solicitação; responderemos em até 15 dias". |
| 3 | O usuário recebe um e-mail confirmando o registro da solicitação. |
| 4 | Solicitações ficam visíveis para o admin em fluxo operacional (não pelo app público). |
| 5 | O admin entrega o export por e-mail ao titular dentro de 15 dias. |

### Fora do Escopo
- Exportação self-service automática — fora do MVP (vai em v2, PRD §14.3).

---

## US-068 — Exclusão de conta pelo titular

**Como** usuário (Júlia ou Carlos),
**quero** poder solicitar a exclusão da minha conta,
**para que** eu exerça meu direito à anonimização/exclusão (LGPD).

### Contexto
PRD §11.5. Soft delete imediato, hard delete em 30 dias (exceto dados exigidos por lei). Conta da adolescente excluída implica que o responsável perde o painel.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do usuário) |
|---|------------------------------------------------------|
| 1 | Em "Meu perfil > Meus dados", há o botão "Excluir minha conta" com confirmação dupla. |
| 2 | Após confirmar, o usuário é deslogado imediatamente; o login deixa de funcionar para aquela conta. |
| 3 | O usuário recebe um e-mail confirmando a exclusão e informando o prazo de hard delete (30 dias). |
| 4 | Se a adolescente exclui a conta, o responsável recebe e-mail neutro informando que a vinculação foi encerrada e que o painel não estará mais acessível. |
| 5 | Dentro do prazo de 30 dias, o admin recebe a solicitação no fluxo operacional e executa o hard delete (exceto dados legalmente exigidos). |

### Fora do Escopo
- Restauração da conta após o hard delete — explicitamente proibido (princípio LGPD).
- Exclusão self-service "imediata" sem prazo de 30 dias — fora do MVP (processo operacional).

---

## Telemetria e instrumentação (transversal)

## US-069 — Instrumentação de eventos de produto

**Como** admin/founder,
**quero** que o atrilha emita e disponibilize um conjunto consistente de eventos de produto sempre que ações relevantes acontecem (cadastro, vinculação, conclusão de sessão, conclusão de semana, ganho de selo, compartilhamento, ciclo de e-mail, cancelamento),
**para que** eu consiga acompanhar o engajamento do MVP, decidir o que priorizar a seguir e responder a perguntas como "quantas adolescentes vincularam um responsável?" sem precisar de queries ad-hoc.

### Contexto
PRD §13.1 lista os eventos esperados do MVP; PRD §13.2 prevê um dashboard mínimo. Esta US transforma essa obrigação transversal em escopo explícito do MVP, com persona "admin/founder" e critérios observáveis a partir do ponto de vista de quem precisa olhar para os dados. Os eventos não podem carregar mais dados do que o necessário (princípio P9 e §11) — em particular, **nada** de texto de reflexão, idade exata, dados do responsável vinculado nem PII desnecessária deve viajar nos eventos.

### Critérios de Aceitação

| # | Critério (observável, do ponto de vista do admin/founder) |
|---|------------------------------------------------------|
| 1 | A partir do início do MVP (antes do soft launch), todos os eventos listados no PRD §13.1 estão sendo emitidos pelo produto sempre que a ação correspondente ocorre — cobrindo, no mínimo: cadastro de adolescente, cadastro de responsável, vinculação concretizada, conclusão de sessão diária, conclusão de semana, ganho de selo, compartilhamento social (selo, streak, versículo, convite), envio de e-mail e cancelamento/exclusão de conta. |
| 2 | Existe um caminho prático para o admin/founder consultar os eventos: ao menos um dashboard mínimo (PRD §13.2) **ou** uma forma documentada de exportação para análise externa (planilha/BI). Em ambos os casos, o admin consegue responder em poucos minutos perguntas básicas como "quantas vinculações nos últimos 7 dias" e "quantas semanas concluídas neste trimestre". |
| 3 | Eventos podem ser filtrados por intervalo de datas e segmentados por persona (adolescente vs. responsável) sem expor identidade pessoal — o admin trabalha com identificadores opacos, não com e-mails ou apelidos. |
| 4 | Nenhum evento contém dados sensíveis fora do mínimo necessário (alinhar com LGPD e PRD §11): em particular, eventos **não** carregam texto de reflexão, idade exata, e-mail, nome do responsável, dados de contato, conteúdo do termo aceito nem qualquer campo livre digitado pela usuária. |
| 5 | Para envio de e-mail, é registrado pelo menos o evento "e-mail enviado" com categoria (transacional, lembrete diário, sábado, conclusão de semana etc.); o evento "e-mail aberto" é registrado **se viável tecnicamente sem prejuízo de privacidade** — caso o admin opte por não rastrear abertura, isso é explicitamente documentado e o produto opera sem essa métrica. |
| 6 | A instrumentação não introduz latência perceptível para o usuário final: a captura do evento não bloqueia a experiência (resposta da ação continua dentro dos alvos de p95 do PRD). |
| 7 | Em caso de falha do sistema de telemetria, o produto continua funcionando normalmente — perder eventos é tolerável; quebrar a sessão da adolescente, não. |

### Fora do Escopo
- Dashboard rico com múltiplos cortes, funis e cohort analysis — fora do MVP (pós-MVP).
- A/B testing instrumentado — fora do MVP.
- Tracking comportamental fino (clickstream, heatmaps de scroll) — proibido por design (P9, P10).
- Integração com ferramentas de marketing/atribuição externas — fora do MVP.

---

*Fim do catálogo de User Stories — atrilha v1 (MVP).*
