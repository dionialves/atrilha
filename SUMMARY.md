# Resumo de execução — Issue #105

**Branch:** `feat/105-us-008-d-fluxo-consumir-token-e-nova-senha-get-pos`
**Worktree:** `.qwen/worktrees/feat-105-us-008-d-fluxo-consumir-token-e-nova-senha-get-pos`
**Estado:** PRONTO para revisão.
**Comando de teste:** `./mvnw test`
**Resultado:** VERDE — `Tests run: 190, Failures: 0, Errors: 0, Skipped: 0` — BUILD SUCCESS.
**Warnings:** zero warnings de compilação; warnings de runtime são apenas environmental (JVM byte-buddy agent + Spring "InitializeUserDetailsManagerConfigurer" pré-existente, não introduzidos por esta task).

## Arquivos alterados

```
src/main/java/dev/zayt/atrilha/auth/config/SecurityConfig.java          (editado — bean SessionRegistry + sessionManagement + /reset-senha public)
src/main/java/dev/zayt/atrilha/auth/web/PasswordResetController.java    (novo)
src/main/resources/messages.properties                                   (editado — chaves password.reset.*)
src/main/resources/templates/auth/reset-senha.html                       (novo)
src/test/java/dev/zayt/atrilha/auth/web/PasswordResetControllerIT.java  (novo — 10 testes)
SUMMARY.md                                                               (este arquivo)
```

## Diff (stat)

```
 .../zayt/atrilha/auth/config/SecurityConfig.java   |  37 ++-
 .../atrilha/auth/web/PasswordResetController.java  | 258 ++++++++++++++++
 src/main/resources/messages.properties             |  17 ++
 src/main/resources/templates/auth/reset-senha.html | 128 ++++++++
 .../auth/web/PasswordResetControllerIT.java        | 336 +++++++++++++++++++++
 5 files changed, 773 insertions(+), 3 deletions(-)
```

## Resumo do test runner (final)

```
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 17.44 s -- in dev.zayt.atrilha.auth.web.PasswordResetControllerIT
...
[INFO] Tests run: 190, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

A suíte inteira passa (190 testes), sem erros nem falhas, inclusive os 10 testes novos da Issue #105 e todos os pré-existentes (US-008-a service, US-008-c request controller, US-007 login, US-006 verificação, etc.). Sem regressões.

## TDD — Ordem RED → GREEN

Foi escrito **primeiro** o `PasswordResetControllerIT.java` com os 10 testes da seção "Ordem TDD" da Issue. A primeira execução confirmou RED: `Tests run: 10, Errors: 10` (UnsatisfiedDependencyException — `SessionRegistry` bean ausente + `PasswordResetController` inexistente). Em seguida, implementadas em ordem:

1. `SecurityConfig` — bean `SessionRegistry` + `HttpSessionEventPublisher`; `sessionManagement` com `.maximumSessions(-1).sessionRegistry(...)` para popular o registry; `/reset-senha` adicionado à lista `permitAll`.
2. `PasswordResetController` — rota GET/POST `/reset-senha`, com peek read-only no GET, validação de senha, consumo atômico via `verify`, persistência do hash, invalidação de sessões pré-existentes, auto-login via `SessionAuthenticator`.
3. `reset-senha.html` — três estados (`SUCCESS` / `EXPIRED_OR_INVALID` / `ALREADY_USED`), seguindo o padrão visual de `esqueci-senha.html` (layout `public`, Alpine.js toggle de senha como em `login.html`).
4. `messages.properties` — 15 chaves `password.reset.*` em pt-BR (título, body, CTAs, hints, mensagens de validação).

Segunda execução confirmou GREEN: `Tests run: 10, Failures: 0, Errors: 0`. Suite inteira `mvn test` também verde.

## O que foi feito (mapa funcional)

### Endpoint `GET /reset-senha?token=<UUID>`

- Parse defensivo do parâmetro `token` (UUID inválido / ausente → `EXPIRED_OR_INVALID` — não vaza estado).
- **Peek read-only** via `PasswordResetTokenRepository.findByToken` para classificar o token sem consumi-lo:
  - inexistente / expirado → outcome `EXPIRED_OR_INVALID`;
  - `usedAt != null` → outcome `ALREADY_USED`;
  - válido → outcome `SUCCESS` + token preservado em hidden field no form.
- Renderiza `auth/reset-senha` em todos os casos (200 OK), com CTAs apropriadas (solicitar novo link / ir para login).

### Endpoint `POST /reset-senha` (CSRF protegido)

Fluxo defensivo em 7 passos:

1. Peek read-only do token (mesmo método do GET) — se inválido/expirado/usado, devolve a tela de erro sem invocar `verify` (preserva o token para reuso em outra aba se já consumido).
2. Validação Bean Validation: `newPassword` `@Size(min=8)`. Se falha, renderiza form de novo com erro de campo (200) — sem consumir o token.
3. `passwordResetService.verify(token)` — consumo atômico (defesa contra race entre peek e verify; o método usa `PESSIMISTIC_WRITE` lock e marca `usedAt` na mesma transação).
4. **Derivação server-side do `accountId`** a partir do `PasswordResetToken.getAccountId()` — **NÃO confiamos no form**. Mitiga ataque de tampering em que um atacante reseta a senha de outra conta enviando um `accountId` arbitrário.
5. Persistência do hash via `accountRepository.save(...)` (BCrypt 12 rounds, encoder injetado).
6. Invalidação de **todas as sessões pré-existentes** do usuário via `sessionRegistry.getAllSessions(principal, false)` + `expireNow()` — atende CA-4 (comprometimento de senha invalida sessões em outros dispositivos).
7. Auto-login via `SessionAuthenticator.authenticate(request, response, accountId, role)` + `redirect:/`.

### Configuração de sessão

- Bean novo `SessionRegistry` (`SessionRegistryImpl`).
- Bean novo `HttpSessionEventPublisher` — sem ele o registry não recebe eventos de criação/expiração de HttpSession (gotcha do Spring Security).
- `sessionManagement` estendido com `.maximumSessions(-1).sessionRegistry(...)` — `-1` = ilimitado (não bloqueia múltiplos dispositivos, apenas rastreia para CA-4).

### LGPD

- `password_hash` é gerado server-side com BCrypt 12; nunca é logado nem retornado em resposta HTTP.
- O token UUID consumido não aparece em logs (a entidade `PasswordResetToken` propositalmente não tem `toString` Lombok).
- E-mail do usuário **não é alterado** durante o reset — em conformidade com a memória do projeto ("e-mail é imutável após cadastro").
- O endpoint não revela existência de conta: tokens malformados/ausentes/inexistentes resultam todos na mesma tela (`EXPIRED_OR_INVALID`).

## Divergências do plano

A Issue #105 (escrita pelo Arquiteto fase 1) fez assunções sobre a interface de US-008-a que **não bateram com o estado real no `main`**. Como instruído pelo input do PO, ajustei o código para a interface real e documento aqui as divergências.

### D1. `PasswordResetResult` — pacote diferente

- **Plano:** `dev.zayt.atrilha.auth.verification.domain.PasswordResetResult`
- **Real:** `dev.zayt.atrilha.auth.domain.PasswordResetResult` (sem subpacote `verification.domain`).
- **Ajuste:** import corrigido no controller e no teste. Sem mudança de comportamento.

### D2. `PasswordResetService.verify(UUID)` é destrutivo

- **Plano (implícito):** `verify` é read-only; o controller chamaria `verify` no GET para classificar e novamente no POST + chamaria `consume(token)` após persistir o hash.
- **Real:** `verify` consome o token na hora — em SUCCESS marca `usedAt = clock.instant()` dentro da mesma transação (`@Transactional` + `PESSIMISTIC_WRITE`). Chamar `verify` duas vezes resulta em `SUCCESS` na 1ª chamada e `ALREADY_USED` na 2ª.
- **Ajuste:** introduzido helper `peekOutcome(UUID)` no controller que usa `passwordResetTokenRepository.findByToken(token)` (read-only) para classificar o token sem consumi-lo. `verify` é chamado **uma única vez** no POST, logo após a validação de senha e logo antes da persistência do novo hash. `consume(token)` **não é mais necessário** — já é feito por `verify` quando devolve SUCCESS. Isso simplifica o controller e elimina o problema de race condition (a Issue tinha "re-verify after validation" como passo, mas a 2ª verify falharia com ALREADY_USED).

### D3. `Account.type` é getter Lombok, não record accessor

- **Plano:** `account.type()` (estilo record).
- **Real:** `Account` é entidade JPA com `@Getter` Lombok — acesso via `account.getType()`. Mesmo idioma usado em `JpaLoginAccountQuery`.
- **Ajuste:** método helper `toRole(Account)` usa `getType()`.

### D4. Plano confiava em `accountId` vindo do form HTML

- **Plano:** template passa `accountId` como hidden field; controller usa esse valor para `accountRepository.findById(form.accountId())`.
- **Risco identificado:** atacante poderia forjar o `accountId` no POST e resetar a senha de outra conta cujo token nunca foi emitido.
- **Ajuste:** o `accountId` é **derivado server-side** a partir do `PasswordResetToken.getAccountId()` após `verify` retornar SUCCESS. O form continua tendo apenas `token` (hidden) + `newPassword`. Defesa contra tampering.

### D5. `SessionRegistry` ainda não existia

- **Plano:** "verificar se já existe bean SessionRegistry; se não, adicionar".
- **Real:** não existia. `sessionManagement` só tinha `enableSessionUrlRewriting(true)`.
- **Ajuste:** adicionado bean `SessionRegistry` (`SessionRegistryImpl`) + bean `HttpSessionEventPublisher` (necessário para o registry receber eventos do servlet container) + `.maximumSessions(-1).sessionRegistry(sessionRegistry)` em `sessionManagement`. `maximumSessions(-1)` = ilimitado (apenas rastreia, não bloqueia login concorrente).

### D6. Teste de invalidação de sessão (teste 10) — estratégia simplificada

- **Plano:** sugeria testar via `@Testcontainers` por causa de limitações do MockMvc com sessões reais.
- **Real:** o teste usa `sessionRegistry.registerNewSession(oldId, principal)` para simular uma sessão pré-existente e depois verifica que `getAllSessions(principal, false)` (filtro de não-expiradas) deixa de listá-la após o POST. Funciona com H2 in-memory + MockMvc, sem Testcontainers.
- **Ajuste:** teste compacto e determinístico; não requer Docker.

### D7. Hash inicial em testes precisava de comprimento exato

- **Plano:** testes usam `setPasswordHash(null)`.
- **Real:** a entidade tem `@Column(length = 72)` e o CHECK do banco `accounts_credential_chk` exige password_hash NOT NULL para contas ativas. Mantemos um hash BCrypt válido inicial (`"$2b$12$" + "a".repeat(53)` — mesmo padrão de `AccountTestFactory.newAdolescent`) e o teste verifica que ele **muda** após o POST, em vez de ir de null para algo.
- **Ajuste:** helper `persistAccount(email)` no teste cria a conta com um hash dummy válido; o teste de POST feliz verifica que `getPasswordHash()` não é nulo, começa com `$2`, e que `passwordEncoder.matches("SenhaSegura123!", updated.getPasswordHash())` retorna `true`.

### D8. Sender de e-mail não é mais exercitado por este fluxo

- **Plano:** controller usaria `PasswordResetSender` em algum momento.
- **Real:** US-008-d **consome** o token; quem **envia** o e-mail é US-008-c. O controller de consumo (esta task) não tem nada a enviar — apenas valida e troca a senha. O `PasswordResetSender` permanece intocado.

## Checagem de compliance

### LGPD

- **password_hash** é dado pessoal sensível (credencial). É gerado server-side com BCrypt 12 rounds (configurado em `SecurityConfig.passwordEncoder()`); nunca é logado nem exposto em resposta HTTP. Atende ADR-005/006/007.
- **CA-4 — invalidação de sessões pré-existentes:** garantida via `SessionRegistry.expireNow()` aplicado a TODAS as sessões ativas do usuário antes da nova sessão ser criada. Defesa em profundidade contra cenário de comprometimento.
- **Anti-enumeration:** GET com token malformado, ausente ou inexistente devolve a mesma tela `EXPIRED_OR_INVALID`. Não há side-channel que revele "este token existiu" vs "nunca foi emitido".
- **E-mail imutável:** o fluxo de reset NÃO oferece troca de e-mail (apenas senha). Em conformidade com a memória do projeto.

### Stack constraints (ADR-011)

- Sem dependências novas. Apenas APIs já presentes em Spring Boot 4.0.6 / Spring Security 7 (`SessionRegistry`, `SessionRegistryImpl`, `HttpSessionEventPublisher` — todos no security core já no classpath).
- Java 21, Maven, BCrypt 12 rounds — todos respeitados.

### Padrões de código (AGENTS.md)

- Visibilidade package-private no `PasswordResetController` (consistente com `EmailVerificationController`, `PasswordResetRequestController`).
- Injeção por construtor com campos `final`.
- DTO `ResetForm` é nested class (estilo `ForgotPasswordRequest` em `PasswordResetRequestController`). Usa `@NotBlank` + `@Size`.
- Sem `@Data`, `@Builder`, `@AllArgsConstructor`. Sem `ddl-auto=update`. Sem `/h2-console` em prod.
- Não há migration Flyway nova (esta slice não altera schema).
- Não foi tocado nada em `doc/**` nem em `AGENTS.md`.
- Nada de log de hash, token ou senha.

### Documentação

- O Codificador NÃO editou `doc/changelog.md`, `doc/release_notes/**`, `doc/Requisitos/**`, `doc/UX/**`, `AGENTS.md`. Essas atualizações são responsabilidade do Revisor quando fechar o PR.

## Nota ao Revisor

Esta é a última slice de US-008 (recuperação de senha). Após o merge, o fluxo end-to-end fica:

1. Usuário em `/login` clica em "Esqueci minha senha" → `/esqueci-senha` (US-008-c) → envia e-mail com link `/reset-senha?token=<UUID>`.
2. Usuário clica no link → `GET /reset-senha?token=...` (US-008-d) → vê formulário de nova senha.
3. Submete nova senha → `POST /reset-senha` → hash persistido, sessões antigas invalidadas, auto-login, redirect para `/`.

Pontos de atenção que o Revisor deve avaliar:

- **D4 (decisão de segurança):** trocar a confiança no `accountId` do form pela derivação server-side é um desvio do plano motivado por defesa contra tampering. Acho importante manter, mas se preferir a leitura literal do plano, é viável reintroduzir o hidden field (com risco). Recomendo manter como está.
- **D2 (verify destrutivo):** o ajuste do fluxo (peek + verify único + sem consume) simplifica e elimina race conditions, mas diverge do plano literal. Está documentado e coberto pelos 10 testes.
- O bean `HttpSessionEventPublisher` é adicional ao que o plano pediu, mas necessário para o `SessionRegistryImpl` funcionar com sessões servlet (sem ele, a invalidação seria silenciosamente no-op em produção). Esse ponto é fácil de errar — incluí-lo é defensivo.
