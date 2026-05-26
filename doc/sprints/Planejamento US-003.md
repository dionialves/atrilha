 Planejamento US-003 — Cadastro de Responsável por E-mail e Senha

    ---

    1. O que é a US-003

    Como Carlos (responsável, 18+),
    quero criar minha conta no atrilha informando nome, e-mail e senha,
    para que eu possa inserir o código da minha filha, aceitar o consentimento parental e acessar o painel.

    Contexto: O responsável só existe no produto para complementar o perfil do adolescente (ADR-004). Ele pode chegar via link de convite ou diretamente na tela inicial. O perfil mínimo é nome + e-mail verificado (RF-E1-11). Adulto <
     18 anos é bloqueado.

    Critérios de Aceitação (observáveis)


    ┌─────┬─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
    │ #   │ Critério                                                                                                    │
    ├─────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
    │ 1   │ Da tela inicial, Carlos escolhe "Sou responsável" e informa nome, e-mail, senha e data de nascimento        │
    ├─────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
    │ 2   │ Senha < 8 caracteres ou e-mail inválido mostra erro inline sem apagar os demais campos                      │
    ├─────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
    │ 3   │ Ao concluir, Carlos é convidado a inserir o código de vinculação de 6 dígitos da adolescente, podendo pular │
    ├─────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
    │ 4   │ Conta criada sem código vinculado existe, mas não dá acesso a nenhum painel até a vinculação                │
    ├─────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
    │ 5   │ Linguagem comunica postura não-vigilante (P9) — "você verá sinais positivos, não horários ou erros"         │
    └─────┴─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘


    Fora do escopo
     - Cadastro com idade < 18 (US-005 — bloqueado)
     - Vincular múltiplos filhos (fora do MVP)
     - MFA

    Requisitos do PRD aplicáveis

    ┌──────────┬───────────────────────────────────────────────────────────────────┬────────────┐
    │ ID       │ Requisito                                                         │ Prioridade │
    ├──────────┼───────────────────────────────────────────────────────────────────┼────────────┤
    │ RF-E1-03 │ Distinguir dois tipos de conta: adolescente ou responsável        │ MUST       │
    ├──────────┼───────────────────────────────────────────────────────────────────┼────────────┤
    │ RF-E1-06 │ Bloquear cadastro de responsável < 18 anos                        │ MUST       │
    ├──────────┼───────────────────────────────────────────────────────────────────┼────────────┤
    │ RF-E1-07 │ Enviar e-mail de verificação para contas criadas com e-mail/senha │ MUST       │
    ├──────────┼───────────────────────────────────────────────────────────────────┼────────────┤
    │ RF-E1-11 │ Perfil do responsável: nome, e-mail verificado                    │ MUST       │
    └──────────┴───────────────────────────────────────────────────────────────────┴────────────┘

    ---

    2. Estado atual da base de código

    O que JÁ EXISTE e pode ser reutilizado

    Entidade Account (polimórfica)
     - Arquivo: src/main/java/dev/zayt/atrilha/accounts/domain/Account.java
     - Tem discriminador type (ADOLESCENT | GUARDIAN) com CHECK constraint
     - Colunas: id (UUID), type, email, emailVerifiedAt, passwordHash (BCrypt), createdAt, lastLoginAt, deletedAt
     - O discriminador GUARDIAN já existe — não precisa criar tabela nova de accounts

    Enum AccountRole
     - Arquivo: src/main/java/dev/zayt/atrilha/accounts/domain/AccountRole.java
     - Tem TEEN e GUARDIAN — ambos prontos

    BCrypt + PasswordEncoder
     - Arquivo: src/main/java/dev/zayt/atrilha/auth/config/SecurityConfig.java
     - new BCryptPasswordEncoder(12) configurado como bean
     - Injetável em qualquer service

    Fluxo de cadastro adolescente (padrão a espelhar)
     - Service: src/main/java/dev/zayt/atrilha/accounts/service/RegisterAdolescentService.java
       - Normaliza email, detecta duplicidade, sanitiza nickname (Jsoup), hash BCrypt cost 12, persiste Account + AdolescentProfile, salva avatar opcional, publica AccountRegisteredEvent
     - Controller: src/main/java/dev/zayt/atrilha/accounts/web/AdolescentRegistrationController.java
       - GET/POST /cadastro/adolescente → form → tela de bloqueio por idade
     - DTO: src/main/java/dev/zayt/atrilha/accounts/domain/RegisterAdolescentRequest.java
     - Form bean: src/main/java/dev/zayt/atrilha/accounts/web/RegisterAdolescentForm.java
     - Template: src/main/resources/templates/cadastro/adolescente.html

    Validação de idade (@EligibleAge)
     - Arquivo: src/main/java/dev/zayt/atrilha/accounts/validation/EligibleAgeValidator.java
     - Já suporta role=GUARDIAN → valida 18+
     - Anotação: src/main/java/dev/zayt/atrilha/accounts/validation/EligibleAge.java

    Fluxo de verificação de e-mail
     - Service: src/main/java/dev/zayt/atrilha/auth/verification/EmailVerificationService.java
     - Entity: src/main/java/dev/zayt/atrilha/auth/verification/EmailVerificationToken.java
     - Listener: src/main/java/dev/zayt/atrilha/auth/verification/AccountRegisteredEventListener.java — dispara envio de e-mail no AFTER_COMMIT
     - Tokens: UUID v4, TTL 24h, tabela email_verification_token

    Autenticação / Login
     - SPI: src/main/java/dev/zayt/atrilha/auth/login/LoginAccountQuery.java — busca conta por email, retorna LoginAccount record (email, passwordHashBcrypt, role, hasGuardianLink, displayName)
     - JPA impl: src/main/java/dev/zayt/atrilha/auth/login/JpaLoginAccountQuery.java — consulta accounts via AccountReader + AccountProfileLookup
     - UserDetailsService: src/main/java/dev/zayt/atrilha/auth/login/LoginAccountUserDetailsService.java
     - Success handler: src/main/java/dev/zayt/atrilha/auth/login/RoleBasedAuthenticationSuccessHandler.java — TEEN → /trilha, GUARDIAN com vínculo → /painel, GUARDIAN sem vínculo → /vincular
     - Rate-limit: src/main/java/dev/zayt/atrilha/auth/login/LoginAttemptService.java — ConcurrentHashMap por IP+email

    Sessão pós-cadastro
     - Componente: src/main/java/dev/zayt/atrilha/auth/session/SessionAuthenticator.java — cria SecurityContext com AuthenticatedAccount(UUID, role) após cadastro

    Templates existentes

    ┌───────────────────────────────────────────────┬────────────────────────────────────────────────────────────────────────────┐
    │ Caminho                                       │ Descrição                                                                  │
    ├───────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
    │ templates/cadastro/comecar.html               │ Entry point — "Qual caminho começa pra você?" (adolescente vs responsável) │
    ├───────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
    │ templates/cadastro/adolescente.html           │ Form de cadastro adolescente (padrão a espelhar)                           │
    ├───────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
    │ templates/cadastro/adolescente_bloqueado.html │ Tela de bloqueio por idade (under-13 / over-17)                            │
    ├───────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
    │ templates/cadastro/concluido.html             │ Placeholder de conclusão do cadastro                                       │
    ├───────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
    │ templates/cadastro/responsavel_em_breve.html  │ STUB ATUAL — página "Em breve" para /cadastro/responsavel                  │
    ├───────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
    │ templates/layout/public.html                  │ Layout base para páginas públicas                                          │
    ├───────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
    │ templates/components/                         │ Componentes reutilizáveis: input, button, card                             │
    └───────────────────────────────────────────────┴────────────────────────────────────────────────────────────────────────────┘


    Login template
     - templates/auth/login.html — tela de login (email+senha, Alpine.js toggle senha, banners de erro)

    ---

    O que NÃO EXISTE e PRECISA ser criado

    1. Entidade GuardianProfile
     - Não existe nenhuma entidade JPA para responsável
     - PRD define: GuardianProfile com account_id (FK 1:1) e full_name
     - Padrão a seguir: espelhar AdolescentProfile (que é 1:1 com Account via @MapsId)

    2. Migration Flyway para guardian_profiles
     - Tabelas atuais: accounts, adolescent_profiles, email_verification_token (e outras)
     - Precisa de V5 (ou próxima versão) criando guardian_profiles

    3. Repository JPA GuardianProfileRepository
     - Não existe — espelhar AdolescentProfileRepository

    4. Controller de cadastro do responsável
     - Stub atual: GuardianRegistrationStubController — retorna apenas "cadastro/responsavel_em_breve"
     - Precisa ser substituído por controller funcional

    5. Template cadastro/responsavel.html
     - O stub usa responsavel_em_breve.html — precisa ser substituído por form real

    6. Extensão de AccountProfileLookup
     - Comentário explícito no código: "Quando GuardianProfile entrar (US-003), esta interface estende para cobrir os dois tipos"
     - Atual só resolve nickname de AdolescentProfile — precisa também resolver full_name de GuardianProfile

    7. Atualização de JpaLoginAccountQuery.toLoginAccount()
     - Precisa resolver displayName de GuardianProfile (full_name) quando type=GUARDIAN

    8. Garantia de que AccountRegisteredEvent dispara verificação para GUARDIAN
     - O listener AccountRegisteredEventListener pode precisar de ajuste para cobrir type=GUARDIAN

    9. Seed em memória para GUARDAN
     - InMemoryLoginAccountQuery tem seeds para teen, guardianLinked, guardianUnlinked — precisa garantir que conta GUARDIAN com perfil funcione

    ---

    3. Agrupamento em 3 Issues

    Issue #1 — Fundação: entidade + migration + repository do GuardianProfile

    Tasks: 1, 2

    O que entrega:
     - Entidade JPA GuardianProfile (account_id 1:1 via `@MapsId`, full_name)
     - Migration Flyway criando tabela guardian_profiles
     - Repository JPA GuardianProfileRepository

    O que espelhar:
     - AdolescentProfile.java (estrutura 1:1 com @MapsId)
     - Migration files existentes em src/main/resources/db/migration/
     - AdolescentProfileRepository.java

    Teste humano:
     - Roda ./mvnw flyway:migrate + verifica tabela no PostgreSQL
     - ./mvn test verde

    Risco LLM: Baixo — padrão já existe, é cópia direta com campos diferentes.

    ---

    Issue #2 — Fluxo completo de cadastro: controller + service + template

    Tasks: 3, 4, 5, 6

    O que entrega (vertical slice funcional):
     - GET/POST /cadastro/responsavel funcionando no browser
     - Form com: nome (full_name), email, senha (8-72 chars), birthDate (@EligibleAge GUARDIAN)
     - Validação inline: senha < 8 → erro sem apagar campos; e-mail inválido → erro inline
     - Se birthDate resultar em idade < 18: redireciona para tela de bloqueio (espelhar adolescente_bloqueado.html)
     - Se e-mail duplicado: erro inline no form
     - Sucesso: persiste Account(type=GUARDIAN) + GuardianProfile, publica AccountRegisteredEvent
     - Autentica sessão via SessionAuthenticator
     - Redireciona para tela de código de vinculação (pode pular e voltar depois)
     - Linguagem não-vigilante na UI

    O que espelhar:
     - RegisterAdolescentService.java → criar RegisterGuardianService.java (mesma orquestração, sem avatar)
     - RegisterAdolescentRequest.java → criar RegisterGuardianRequest.java (email, password, fullName, birthDate)
     - AdolescentRegistrationController.java → criar GuardianRegistrationController.java
     - cadastro/adolescente.html → criar cadastro/responsavel.html
     - cadastro/adolescente_bloqueado.html → criar cadastro/responsavel_bloqueado.html

    Teste humano:
     - Acessa /cadastro/responsavel no browser
     - Preenche form, submete — vê a tela de vinculação aparecer
     - Tenta senha curta → erro inline aparece
     - Tenta birthDate < 18 → vai para tela de bloqueio

    Risco LLM: Médio — é a maior issue, mas não arriscada porque o padrão adolescente já existe como referência idêntica.

    ---

    Issue #3 — Integração com login + limpeza + testes

    Tasks: 7, 8, 9, 10, 11, 12

    O que entrega:
     - Responsável loga e vê nome correto no header (full_name via AccountProfileLookup estendido)
     - JpaLoginAccountQuery.toLoginAccount() resolve displayName de GuardianProfile
     - Stub GuardianRegistrationStubController removido
     - Template responsavel_em_breve.html removido
     - Email de verificação disparado para conta GUARDAN (verificar/ajustar AccountRegisteredEventListener)
     - Testes unitários do RegisterGuardianService
     - Integration test do RegisterGuardianController
     - Seed de conta GUARDIAN no InMemoryLoginAccountQuery

    O que espelhar:
     - Testes existentes de RegisterAdolescentService e AdolescentRegistrationControllerIT
     - Como JpaLoginAccountQuery resolve displayName de AdolescentProfile hoje

    Teste humano:
     - Cadastra responsável → faz login em /login → vê nome correto no header
     - Tenta logar com senha errada múltiplas vezes → rate-limit bloqueia
     - ./mvn test verde

    Risco LLM: Baixo — ajustes finos em código existente.

    ---

    4. Dependências entre issues

     1 Issue #1 (entidade + migration)
     2     │
     3     ▼
     4 Issue #2 (controller + service + template) ← precisa da entidade do #1 para persistir
     5     │
     6     ▼
     7 Issue #3 (integração + testes) ← precisa do service do #2 para testar

    Cada issue é independente para o ciclo Codificador → Revisor → PR draft → Merge. A #1 pode ser mergeada e o banco já terá a tabela. A #2 funciona com a tabela existente. A #3 finaliza tudo.

    ---

    5. Detalhes técnicos importantes

    Modelo de dados futuro (PRD §10.1) — contexto para a Issue #3

      1 GuardianProfile
      2   ├── account_id (FK → Account.id, 1:1 via @MapsId)
      3   └── full_name
      4 
      5 GuardianLink (Sprint 5 — US-012 a US-014)
      6   ├── id
      7   ├── adolescent_id (FK → AdolescentProfile)
      8   ├── guardian_id (FK → GuardianProfile, nullable até pareamento)
      9   ├── code (6 chars, único enquanto ativo)
     10   ├── code_expires_at
     11   ├── linked_at
     12   ├── consent_accepted_at
     13   ├── consent_ip, consent_user_agent, consent_terms_hash
     14   └── revoked_at

    ADR-004 — Vinculação iniciada pelo adolescente
    Adolescente cadastra primeiro, gera código de 6 dígitos, compartilha com responsável. O produto é do adolescente — se o pai cadastra primeiro, vira "ferramenta do pai". Isso significa que na US-003 o responsável não precisa de 
    vínculo prévio — ele chega depois, recebe um código do adolescente, e pode pular a inserção agora.

    Postura não-vigilante (P9)
    A linguagem na UI do cadastro deve comunicar que "você verá sinais positivos, não horários ou erros". Isso se aplica já na tela de cadastro.

    OAuth Google removido (REF-003)
    OAuth2 foi completamente removido. Botões sociais nos templates estão disabled. A US-003 é apenas e-mail + senha. OAuth do responsável será a US-004.

    Rate-limit de login
    LoginAttemptService usa ConcurrentHashMap por IP+email, já configurado no SecurityConfig. Funciona automaticamente para qualquer conta (TEEN ou GUARDIAN) — não precisa de ajuste.

    EmailVerificationService
     - Tokens UUID v4, TTL 24h
     - Cooldown 60s entre reenvios, limite 5/hora
     - AccountRegisteredEventListener escuta AccountRegisteredEvent no AFTER_COMMIT
     - Ponto de atenção: verificar se o listener filtra por type ou cobre ambos. Se filtrar, precisa ajustar para GUARDIAN também.
