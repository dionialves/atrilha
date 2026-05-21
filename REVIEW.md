
## Devolução — 2026-05-21 16:49
**Veredito:** AJUSTES NECESSÁRIOS

Falta teste de validação do perfil prod. O critério 'No perfil prod, o bean não é instanciado (validar via teste de profile)' exige evidência testável. Adicione um teste @SpringBootTest com spring.profiles.active=prod que verifique InMemoryLoginAccountQuery NÃO está no ApplicationContext (ex: assertThrows ou context.containsBean returning false).

---
