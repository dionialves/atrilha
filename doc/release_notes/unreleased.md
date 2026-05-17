# Release Notes — Unreleased

## chore(chore-001): Esqueleto Spring Boot 4.0.6 + Java 21

**Issue:** [#1](https://github.com/dionialves/atrilha/issues/1)
**Branch:** chore/1-esqueleto-spring-boot
**Data:** 2026-05-17

### O que foi feito

Criação do esqueleto Maven/Spring Boot que serve de fundação técnica para todo o projeto Atrilha (Sprint 1, chore-001).

### Arquivos introduzidos

- `pom.xml` — parent `spring-boot-starter-parent:4.0.6`, Java 21, dependências mínimas (web, validation, test), flags `-Xlint:all -Werror`.
- `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` — Maven Wrapper 3.9.9 reproduzível.
- `src/main/java/dev/zayt/atrilha/AtrilhaApplication.java` — entry-point da aplicação.
- `src/main/java/dev/zayt/atrilha/{auth,accounts,content,progress,notifications,admin}/package-info.java` — 6 módulos vazios com Javadoc de fronteira conforme PRD §9.3.
- `src/main/resources/application.properties` — configuração mínima (nome da app + porta 8084).
- `src/test/java/dev/zayt/atrilha/AtrilhaApplicationTests.java` — teste `contextLoads`.
- `.tool-versions` — versão Java fixada para asdf (`openjdk-21`).

### Arquivos modificados

- `.gitignore` — adicionadas entradas para `target/`, `.idea/`, `*.iml`, `.vscode/`, `.DS_Store`, `HELP.md`, `!.mvn/wrapper/maven-wrapper.jar`, `.worktrees/`.

### Resultado de build

`./mvnw clean verify` — BUILD SUCCESS, 1 teste passado, 0 warnings do compilador.
