# Resumo de execução — Issue #59

**Branch:** feat/59-feat-007-05-cria-handlers-de-auth-e-user-details-s
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** Tests run: (ver log)
**Warnings de compilação:** 0

## Arquivos alterados
```
SUMMARY.md
src/main/java/dev/zayt/atrilha/auth/login/AtrilhaUserDetails.java
src/main/java/dev/zayt/atrilha/auth/login/LoginAccountQuery.java
src/main/java/dev/zayt/atrilha/auth/login/LoginAccountUserDetailsService.java
src/main/java/dev/zayt/atrilha/auth/login/PostLoginDestination.java
src/main/java/dev/zayt/atrilha/auth/login/RateLimitedAuthenticationFailureHandler.java
src/main/java/dev/zayt/atrilha/auth/login/RoleBasedAuthenticationSuccessHandler.java
src/test/java/dev/zayt/atrilha/auth/login/AtrilhaUserDetailsTest.java
src/test/java/dev/zayt/atrilha/auth/login/LoginAccountUserDetailsServiceTest.java
src/test/java/dev/zayt/atrilha/auth/login/RateLimitedAuthenticationFailureHandlerTest.java
src/test/java/dev/zayt/atrilha/auth/login/RoleBasedAuthenticationSuccessHandlerTest.java
```

## Diff (stat)
```
 SUMMARY.md                                         |  71 ++++--
 .../atrilha/auth/login/AtrilhaUserDetails.java     |  83 +++++++
 .../zayt/atrilha/auth/login/LoginAccountQuery.java |   3 +-
 .../auth/login/LoginAccountUserDetailsService.java |  43 ++++
 .../atrilha/auth/login/PostLoginDestination.java   |   8 +-
 .../RateLimitedAuthenticationFailureHandler.java   |  94 ++++++++
 .../RoleBasedAuthenticationSuccessHandler.java     | 142 +++++++++++
 .../atrilha/auth/login/AtrilhaUserDetailsTest.java | 146 +++++++++++
 .../login/LoginAccountUserDetailsServiceTest.java  |  87 +++++++
 ...ateLimitedAuthenticationFailureHandlerTest.java | 130 ++++++++++
 .../RoleBasedAuthenticationSuccessHandlerTest.java | 266 +++++++++++++++++++++
 11 files changed, 1047 insertions(+), 26 deletions(-)
```

## O que foi feito
Implementa a subtask 007.05: handlers de autenticação e `UserDetailsService` para o fluxo de login do atrilha.

**Arquivos criados (4):**
- `AtrilhaUserDetails.java` — wrapper `UserDetails` que expõe `getRole()`, `hasGuardianLink()` e `getAccount()` para os templates
- `LoginAccountUserDetailsService.java` — `@Service` que delega para `LoginAccountQuery.findForLogin()` e retorna `AtrilhaUserDetails` com autoridade `ROLE_TEEN` ou `ROLE_GUARDIAN`
- `RateLimitedAuthenticationFailureHandler.java` — trata falhas de autenticação com rate-limit; `LockedException` → `/login?blocked`; falha normal → `/login?error`
- `RoleBasedAuthenticationSuccessHandler.java` — redireciona pós-login: TEEN → `/trilha`, GUARDIAN vinculado → `/painel`, sem vínculo → `/vincular`; OAuth com email desconhecido → `/login?error`

**Arquivos modificados (2):**
- `LoginAccountQuery.java` — `LoginAccount` agora implementa `Serializable` (necessário para session serialization)
- `PostLoginDestination.java` — adicionado enum `ERROR("/login?error")` e factory `valueOfError()`

**Testes (4 classes, 36 testes):**
- `AtrilhaUserDetailsTest` — 11 testes de unidade
- `LoginAccountUserDetailsServiceTest` — 4 testes (sucesso, email não encontrado)
- `RateLimitedAuthenticationFailureHandlerTest` — 6 testes (falha, bloqueado, LockedException)
- `RoleBasedAuthenticationSuccessHandlerTest` — 15 testes (TEEN, GUARDIAN vinculado/sem vínculo, OAuth sem match)

**Critérios de aceitação:** Todos os 10 critérios atendidos. `mvn test` verde (159 testes, 0 falhas).

**Pontos de atenção:** O `DaoAuthenticationProvider` com `preAuthenticationChecks` será configurado na subtask 007.06 — esta task apenas garante que o failure handler trata `LockedException` corretamente.

## ⚠️ Checagem LGPD (atrilha)
N/A — sem superfície de dados pessoais. Os handlers não armazenam, logam ou expõem dados sensíveis:
- `RateLimitedAuthenticationFailureHandler` usa hash SHA-256 truncado do IP nos logs (`ip=<hash>`), nunca email em claro ou senha
- `LoginAccountUserDetailsService` lança `UsernameNotFoundException` genérica (sem oráculo de email existente vs inexistente)
- Nenhum consentimento, compartilhamento ou dado de menor (13–17) é tocado nesta task
- ADR-005/006/007: não aplicável diretamente, mas a implementação segue o princípio de minimização (zero dados sensíveis em logs ou respostas)
