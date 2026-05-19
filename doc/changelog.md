# Changelog

Todas as mudanças notáveis deste projeto são documentadas aqui.
Formato baseado em [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### User Stories
- US-005 · Bloqueio de cadastro por idade fora da faixa ([#36](https://github.com/dionialves/atrilha/issues/36))
- US-001 · Cadastro de adolescente por e-mail e senha ([#40](https://github.com/dionialves/atrilha/issues/40))

### Chore
- chore(chore-ux-009): substituir Tailwind Play CDN por build standalone do Tailwind v4 ([#43](https://github.com/dionialves/atrilha/issues/43))

## [0.2.0] - 2026-05-19

Marco M1 — Identidade visual definida (Sprint 2). APROVADO pelo humano em Issue [#27](https://github.com/dionialves/atrilha/issues/27).

### Chore
- chore(chore-ux-001): identidade visual — paleta, tipografia e princípios de hierarquia ([#20](https://github.com/dionialves/atrilha/issues/20))
- chore(chore-ux-002): design tokens Tailwind — cores, espaçamento, raios, sombras, tipografia ([#21](https://github.com/dionialves/atrilha/issues/21))
- chore(chore-ux-003): componentes base — botão, input, card, modal, header, navegação, badge, toast ([#22](https://github.com/dionialves/atrilha/issues/22))
- chore(chore-ux-004): protótipo da trilha — spec UX + HTML estático da trilha vazia ([#23](https://github.com/dionialves/atrilha/issues/23))
- chore(chore-ux-005): protótipo da sessão diária — spec UX + HTML estático do bloco núcleo ([#24](https://github.com/dionialves/atrilha/issues/24))
- chore(chore-ux-006): protótipo do painel dos pais — spec UX + HTML estático do painel em baixa atividade ([#25](https://github.com/dionialves/atrilha/issues/25))
- chore(chore-ux-007): checklist de acessibilidade — WCAG 2.1 AA mínimo do MVP ([#26](https://github.com/dionialves/atrilha/issues/26))

## [0.0.1] - 2026-05-17

Marco M0 — Infraestrutura básica em produção (Sprint 1). Tag `v0.0.1`; release notes detalhadas em [`doc/release_notes/0.0.1.md`](release_notes/0.0.1.md).

### Chore
- chore(chore-001): esqueleto Spring Boot 4.0.6 + Java 21 com 6 módulos vazios ([#1](https://github.com/dionialves/atrilha/issues/1))
- chore(chore-002): baseline Flyway + PostgreSQL 18 local via Docker Compose ([#2](https://github.com/dionialves/atrilha/issues/2))
- chore(chore-003): Thymeleaf + HTMX + Tailwind + Alpine.js + Lottie no layout base ([#3](https://github.com/dionialves/atrilha/issues/3))
- chore(chore-004): endpoint /health publico e paginas de erro 404/5xx ([#4](https://github.com/dionialves/atrilha/issues/4))
- chore(chore-005): Dockerfile multi-stage + imagem buildada localmente ([#5](https://github.com/dionialves/atrilha/issues/5))
- chore(chore-006): provisionar-vps-nginx-letsencrypt-docker ([#6](https://github.com/dionialves/atrilha/issues/6))
- chore(chore-008): pipeline CI/CD GitHub Actions — build linux/amd64 + SSH deploy para VPS ([#8](https://github.com/dionialves/atrilha/issues/8))
