# Slicing decision log — US-009 · Edição do perfil do adolescente

### Metadados
- **Tipo:** User Story
- **Código:** `US-009`
- **Label gh:** `user-story`
- **Prioridade sugerida:** `alta` — Sprint 4 do MVP (dependência direta de US-010 e US-011)

### Demanda original (verbatim do humano)
> Como Júlia, quero editar meu apelido, foto, data de nascimento e e-mail, para que meu perfil reflita quem eu sou hoje.
>
> **Contexto:** Direito de correção (LGPD, PRD §11.5). Mudanças não devem quebrar progresso nem desfazer vinculação. Mudança de e-mail exige nova verificação.

---

### 1. Tamanho medido
- N_cameras=4 (controller/service/repository/security-config), N_arquivos_novos=5, N_arquivos_editar=3, N_migrations=0, N_templates=1, N_endpoints=2, N_i18n≈15, N_testes=3
- output_estimado_chars ≈ (5×1500) + (3×800) + 0 + (1×4000) + (2×600) + (15×50) + 3000 = **~17.950 chars**
- Cap soft estourado: N_cameras=4 (>2), N_arquivos_novos+editar=8 (>5). Decisão: Tier 2.

### 2. Por que dividir
- `N_cameras`: controller + service + security-config + view = 4 camadas distintas (cap soft >2)
- `N_testes`: happy path, erro validação, bloqueio idade, upload foto, remoção foto = 5 cenários testáveis separados

### 3. Estimativa total
~5h (sprint planeja 4h; o extra vem da complexidade client-side Alpine.js que não estava no cálculo original)

### 4. Subtasks propostas (ordem topológica)

#### US-009-a · Backend core (controller + service + validação)
**Escopo:** Controller `ProfileController` com GET/POST, Service `UpdateProfileService`, Form DTO `UpdateProfileForm` (nickname + birthDate), validação server-side via `AgeEligibilityChecker`. MockMvc tests. SecurityConfig update para proteger `/perfil/**`. Foto não incluída nesta subtask.
**Camadas tocadas:** controller, service, security config
**Arquivos principais:**
- `src/main/java/.../accounts/web/ProfileController.java` [novo]
- `src/main/java/.../accounts/service/UpdateProfileService.java` [novo]
- `src/main/java/.../accounts/web/UpdateProfileForm.java` [novo]
- `src/main/java/.../auth/config/SecurityConfig.java` [editar — adicionar `/perfil/**` hasRole("TEEN")]
**Depende de:** nada
**Bloqueia:** US-009-b, US-009-c
**Estimativa:** ~2h

#### US-009-b · View + Client-side (template + Alpine.js + CSS)
**Escopo:** Template Thymeleaf `perfil/adolescente-editar.html` com avatar block, campos nickname/birthDate/email(readonly), save bar sticky, modal de bloqueio por idade. Alpine.js para dirty state detection e toggle da save bar. CSS para avatar-block, save-bar, modal. i18n messages. Action sheet não incluída (vai em US-009-c).
**Camadas tocadas:** view, frontend assets, i18n
**Arquivos principais:**
- `src/main/resources/templates/perfil/adolescente-editar.html` [novo]
- `src/main/resources/static/css/app.css` [editar — adicionar avatar-block, save-bar, modal styles]
- `src/main/resources/messages.properties` [editar — adicionar chaves de UI e validação]
**Depende de:** US-009-a (controller precisa existir para renderizar e POST)
**Bloqueia:** US-009-c (action sheet anexa ao avatar block criado aqui)
**Estimativa:** ~1.5h

#### US-009-c · Photo upload + Action sheet + Testes
**Escopo:** Action sheet bottom UI, `<input type="file">` ocultos para câmera/galeria, integração multipart no controller/service (`AvatarStorage.store()`), remoção de foto (setar `avatarUrl = null`). Tests para salvar/remover foto e rejeitar arquivo inválido.
**Camadas tocadas:** controller (editar), service (editar), view (editar), tests
**Arquivos principais:**
- `src/main/java/.../accounts/web/ProfileController.java` [editar — adicionar multipart param]
- `src/main/java/.../accounts/service/UpdateProfileService.java` [editar — adicionar lógica de avatar]
- `src/main/resources/templates/perfil/adolescente-editar.html` [editar — adicionar action sheet + file inputs]
**Depende de:** US-009-a, US-009-b
**Estimativa:** ~1.5h

### 5. Diagrama de dependências
```
US-009-a (backend core)
    ├──→ US-009-b (view + client-side)
    │        └──→ US-009-c (photo upload + action sheet)
    └─────────────→ US-009-c (photo upload + action sheet)
```

### 6. Quebras alternativas consideradas
1. **Foto em vez de separada, integrar tudo no controller desde o início:** rejeitado — mistura validação de texto com multipart num único diff denso, difícil de revisar.
2. **Action sheet na US-009-b junto com a view:** rejeitado — action sheet tem lógica de `<input type="file">` + `AvatarStorage` que pertence ao fluxo de upload (US-009-c). Separar evita que a view dependa de um service que ainda não existe.

### 7. Validações
- [x] Independência dentro da worktree: cada subtask pode ser implementada e ter testes verdes isoladamente
- [x] Recorte de testes isolado por subtask: (a) MockMvc GET/POST, (b) MockMvc render + visual, (c) MockMvc multipart
- [x] DAG sem ciclos
- [x] Subtask ≤ 2h cada
- [x] Recheck §4 por subtask: nenhuma estoura cap duro individualmente

### 8. Arquivos gerados nesta passada
- `.opencode/briefs/US-009-slicing.md` (este arquivo)
- `.opencode/briefs/US-009-a.md`
- `.opencode/briefs/US-009-b.md`
- `.opencode/briefs/US-009-c.md`

### 9. Para auditar e ajustar
`rm .opencode/briefs/US-009*` + re-invocar `Use o scout para preparar o brief de US-009, considerando: <ajuste>`
