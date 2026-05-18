# Changelog

Todas as mudanças notáveis deste projeto são documentadas aqui.
Formato baseado em [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [0.0.1] - 2026-05-17

Fechamento do Sprint 1 / Marco M0 — fundação técnica e deploy automatizado em produção (`https://atrilha.app`).

### Chore
- chore(chore-001): esqueleto Spring Boot 4.0.6 + Java 21 com 6 módulos vazios ([#1](https://github.com/dionialves/atrilha/issues/1))
- chore(chore-002): baseline Flyway + PostgreSQL 18 local via Docker Compose ([#2](https://github.com/dionialves/atrilha/issues/2))
- chore(chore-003): Thymeleaf + HTMX + Tailwind + Alpine.js + Lottie no layout base ([#3](https://github.com/dionialves/atrilha/issues/3))
- chore(chore-004): endpoint /health público e páginas de erro 404/5xx ([#4](https://github.com/dionialves/atrilha/issues/4))
- chore(chore-005): Dockerfile multi-stage + imagem buildada localmente ([#5](https://github.com/dionialves/atrilha/issues/5))
- chore(chore-006): provisionar VPS Zayt — Nginx + Let's Encrypt + Docker daemon ([#6](https://github.com/dionialves/atrilha/issues/6))
- chore(chore-007): Cloudflare — DNS de atrilha.app + CDN free tier ([#7](https://github.com/dionialves/atrilha/issues/7))
- chore(chore-008): pipeline CI/CD GitHub Actions — build linux/amd64 + SSH deploy para VPS ([#8](https://github.com/dionialves/atrilha/issues/8))
- chore(chore-009): smoke test em produção — /health responde 200 via HTTPS ([#9](https://github.com/dionialves/atrilha/issues/9))
- chore(chore-010): commit dos documentos de governança (AGENTS, workflow, PRD, UserStory) ([#10](https://github.com/dionialves/atrilha/issues/10))
