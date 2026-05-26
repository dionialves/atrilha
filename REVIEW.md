## Devolução — 2026-05-25 10:56
**Veredito:** AJUSTES NECESSÁRIOS

O Codificador modificou arquivos de produção (src/main/**) quando o plano proibia explicitamente: src/main/frontend/css/app.css (502 linhas deletadas), 5 arquivos de fonte .woff2 removidos, e 4 templates Thymeleaf alterados (login.html, comecar.html, home.html, public.html). O plano diz literalmente: NÃO mexer em static/** ou templates/**. Além disso, SUMMARY.md está incompleto (seções 'O que foi feito' e 'Checagem LGPD' vazias). Reverta todas as alterações em src/main/** e corrija o SUMMARY.md.

---

## Devolução — 2026-05-25 17:29
**Veredito:** AJUSTES NECESSÁRIOS

1) **EmailVerificationToken + EmailVerificationTokenRepository em pacote errado.** Estão em `accounts.domain` / `accounts.repository`. Devem ser movidos para `auth.verification/`. O plano (Problema 1 da issue) é explícito: "É artefato exclusivo do fluxo de verificação de e-mail, que é responsabilidade de `auth`."

2) **AccountReader + JpaAccountReader + AccountProfileLookup + JpaAccountProfileLookup em `accounts.service`.** Devem ser movidos para `accounts.repository/`, conforme plano. Interfaces de repositório/portas de leitura pertencem ao mesmo sub-pacote técnico que os repos.

3) **Avatar classes em pacotes errados.** `FilesystemAvatarStorage` está em `accounts.service`, e `AvatarTooLargeException` + `AvatarUnsupportedTypeException` estão em `accounts.exception`. O plano prevê sub-pacote dedicado `accounts.avatar/` com todas as 4 classes.

4) **EmailVerificationService em `auth.service`.** Devia estar em `auth.verification/`, conforme plano.

5) **RequiresVerifiedEmail + RequiresVerifiedEmailInterceptor em `auth.web`.** Deviam estar em `auth.verification/`, conforme plano.

6) **AccountRegisteredEventListener em `auth.event/`.** Sub-pacote não previsto no plano. O plano diz "Manter em `auth` é defensável" (na raiz) ou mover para `auth.verification/`. Criar `auth.event/` é decisão não aprovada.

7) **Allowlist do PackageStructureArchitectureTest desatualizada.** Arquivo `src/test/java/dev/zayt/atrilha/PackageStructureArchitectureTest.java`, linhas ~50-68. A allowlist referencia FQNs que já não existem:
   - `dev.zayt.atrilha.auth.AccountRole` → agora em `accounts.domain`
   - `dev.zayt.atrilha.auth.EligibleAge` → agora em `accounts.validation`
   - `dev.zayt.atrilha.auth.AccountRegisteredEvent` → agora em `accounts.domain`
   - `dev.zayt.atrilha.accounts.EmailVerificationToken` → agora em `accounts.domain`
   - `dev.zayt.atrilha.accounts.EmailVerificationTokenRepository` → agora em `accounts.repository`
   
   O teste passa por falso-positivo: não travaria regressão futura porque as regras estão baseadas em caminhos extintos.

8) **Dependência #73 (REF-001) não mergada.** Issue #73 está OPEN. A issue #74 declara explicitamente: "Mergear #73 ANTES desta task." O codificador não deveria ter prosseguido sem a dependência resolvida. Risco de conflito quando #73 for mergada (os testes de login são tocados por ambas as tasks).

---

## Devolução — 2026-05-25 22:13
**Veredito:** AJUSTES NECESSÁRIOS

REMOÇÃO DE CENÁRIO US-007: O teste postLoginComCredenciaisDeGuardianVinculadoRedirecionaParaPainel foi removido de LoginFlowTest.java (linha ~83 do diff). A Issue #73, critério de aceitação final exige: 'Cenários originais da US-007 mantidos (TEEN→/trilha, GUARDIAN→/vincular, credencial errada, rate-limit)'. O Codificador removeu o cenário 'GUARDIAN→/painel' em vez de ajustá-lo. Problema real: createGuardianEmailPassword() não seta hasGuardianLink=true, e JpaLoginAccountQuery sempre retorna false (Sprint 3 — sem tabela de vínculo). Solução esperada: ajustar o teste para esperar redirectedUrl('/vincular') em vez de '/painel', já que hasGuardianLink é sempre false no Sprint 3. Adicionar comentário explicando a limitação do sprint (como foi feito em postLoginComCredenciaisDeGuardianSemVinculoRedirecionaParaVincular). IMPORTANTE: não remover o teste — ele valida um caminho de redirecionamento que deve existir, mesmo que o destino mude.

BUG SECUNDÁRIO — import não usado: LoginFlowTest.java linha 4 importa 'dev.zayt.atrilha.accounts.domain.AccountRole' que não é mais referenciado após remoção do teste de guardian vinculado. Remover import morto.

---
