# RUNBOOK — Provisionar VPS Zayt

**Versao:** chore-006  
**Dominio alvo:** atrilha.app  
**SO alvo:** Ubuntu 24.04 LTS  
**Autor:** chore-006 (Arquiteto/Codificador)

---

## Pre-requisitos

- VPS HostGator com Ubuntu 24.04 LTS, minimo 1 vCPU / 1 GB RAM.
- IPv4 publico da VPS ja conhecido.
- Acesso root inicial via SSH (temporario).
- Registro DNS A apontando `atrilha.app` para o IPv4 da VPS (ver secao **Cloudflare — pre-requisitos** abaixo).
- `chore-007` (DNS Cloudflare) deve ser concluido **antes** do Passo 8 (certbot).

---

## Cloudflare — pre-requisitos

> Execute esta configuracao no painel da Cloudflare **antes** de rodar o certbot.

1. **Registro A — DNS-only (nuvem cinza) ANTES do certbot.**  
   Crie (ou edite) o registro `A atrilha.app -> <IP da VPS>` com o proxy **desligado** (icone de nuvem cinza, modo "DNS only"). Isso e obrigatorio para que o certbot valide o dominio via HTTP-01. Com o proxy da Cloudflare ativo, o certbot nao consegue ver o servidor real.

2. **Tambem crie `A www.atrilha.app -> <IP da VPS>`** em DNS-only pela mesma razao.

3. **Apos o certificado ser emitido com sucesso** (Passo 8 concluido), volte ao painel da Cloudflare e:
   - Ligue o **Proxy** nos dois registros A (icone de nuvem laranja).
   - Em **SSL/TLS > Overview**, selecione **Full (strict)**.
   - Em **SSL/TLS > Edge Certificates**, ative **Always Use HTTPS**.
   - Em **SSL/TLS > Edge Certificates**, defina **Minimum TLS Version** para **TLS 1.2**.
   - **Nao** ative HSTS pelo painel da Cloudflare — o HSTS ja esta declarado no Nginx (`Strict-Transport-Security`). Duplicar causa conflito de headers.

4. **Rocket Loader: OFF** (em **Speed > Optimization**) — evita quebra de HTMX e Alpine.js.

5. **Auto Minify JavaScript: OFF** (em **Speed > Optimization**) — evita quebra de HTMX e Alpine.js.

---

## Passo 1 — Usuario `deploy`

Execute como `root`:

```bash
adduser deploy
usermod -aG sudo deploy
mkdir -p /home/deploy/.ssh
# Cole aqui a chave publica SSH do operador:
echo "<SUA_CHAVE_PUBLICA_SSH>" > /home/deploy/.ssh/authorized_keys
chmod 700 /home/deploy/.ssh
chmod 600 /home/deploy/.ssh/authorized_keys
chown -R deploy:deploy /home/deploy/.ssh
```

**Teste antes de fechar a sessao root:**

```bash
ssh deploy@<IP_DA_VPS>
```

Confirme que o login funciona sem senha antes de continuar.

---

## Passo 2 — Endurecer SSH

Edite `/etc/ssh/sshd_config` e garanta:

```
PermitRootLogin no
PasswordAuthentication no
ChallengeResponseAuthentication no
```

Reinicie o servico:

```bash
systemctl restart sshd
```

> Mantenha uma sessao SSH aberta enquanto faz isso — se fechar tudo e a config estiver errada, voce perde o acesso.

---

## Passo 3 — Firewall UFW

```bash
ufw default deny incoming
ufw default allow outgoing
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
ufw status verbose
```

Resultado esperado: regras para 22 (OpenSSH), 80/tcp, 443/tcp. Tudo mais bloqueado.

---

## Passo 4 — Docker

```bash
curl -fsSL https://get.docker.com | sh
usermod -aG docker deploy
```

Faca logout e login novamente com o usuario `deploy` para o grupo `docker` entrar em vigor:

```bash
# Em nova sessao como deploy:
docker run --rm hello-world
```

Resultado esperado: mensagem "Hello from Docker!".

---

## Passo 5 — Nginx

```bash
apt update && apt install -y nginx
systemctl enable nginx
systemctl start nginx
```

---

## Passo 6 — Configurar Nginx (atrilha.app)

```bash
# Copie o arquivo de configuracao do repositorio para o servidor
scp infra/nginx/atrilha.app.conf deploy@<IP_DA_VPS>:/tmp/

# Na VPS:
sudo cp /tmp/atrilha.app.conf /etc/nginx/sites-available/atrilha.app
sudo ln -s /etc/nginx/sites-available/atrilha.app /etc/nginx/sites-enabled/atrilha.app

# Remova o site padrao
sudo rm -f /etc/nginx/sites-enabled/default

# Valide e recarregue
sudo nginx -t && sudo systemctl reload nginx
```

Resultado esperado: `nginx -t` retorna `syntax is ok` e `test is successful`.

---

## Passo 7 — Confirmar DNS

Antes de prosseguir para o certbot, confirme que o registro DNS ja propagou:

```bash
dig +short atrilha.app
dig +short www.atrilha.app
```

Ambos devem retornar o IPv4 da VPS. Se retornar outro IP (ex: IP da Cloudflare com proxy ligado), aguarde a propagacao ou ajuste conforme secao **Cloudflare — pre-requisitos**.

```bash
# Teste HTTP basico (deve retornar redirect 301 para HTTPS):
curl -I http://atrilha.app
```

---

## Passo 8 — Let's Encrypt (certbot)

> Execute somente apos o Passo 7 confirmar propagacao DNS e registro A em DNS-only.

```bash
apt install -y certbot python3-certbot-nginx

certbot --nginx \
  -d atrilha.app \
  -d www.atrilha.app \
  -m <SEU_EMAIL> \
  --agree-tos \
  --no-eff-email \
  -n
```

O certbot modifica automaticamente `/etc/nginx/sites-available/atrilha.app` para adicionar as diretivas `ssl_certificate` e `ssl_certificate_key`.

---

## Passo 9 — Testar Renovacao Automatica

```bash
certbot renew --dry-run
```

Resultado esperado: `Congratulations, all simulated renewals succeeded.`

O timer systemd `snap.certbot.renew.timer` (ou `certbot.timer`) ja agenda renovacao automatica — verifique:

```bash
systemctl list-timers | grep certbot
```

---

## Passo 10 — Diretorio de Deploy

```bash
sudo mkdir -p /opt/atrilha
sudo chown deploy:deploy /opt/atrilha
```

---

## Passo 11 — Arquivos de Deploy na VPS

```bash
# Na sua maquina local:
scp infra/compose/docker-compose.prod.yml deploy@<IP_DA_VPS>:/opt/atrilha/docker-compose.yml
scp infra/compose/.env.example            deploy@<IP_DA_VPS>:/opt/atrilha/.env.example
```

Na VPS, crie o `.env` real com as credenciais de producao:

```bash
# Na VPS como deploy:
cp /opt/atrilha/.env.example /opt/atrilha/.env
nano /opt/atrilha/.env   # preencha POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD, APP_TAG
```

> O arquivo `/opt/atrilha/.env` **nunca** deve ser commitado no repositorio. Ele contem credenciais de producao.

Verifique permissoes:

```bash
chmod 600 /opt/atrilha/.env
ls -la /opt/atrilha/
```

Resultado esperado: `docker-compose.yml`, `.env.example` e `.env`, todos de propriedade de `deploy:deploy`.

---

## Passo 12 — Smoke Test

> Execute apos `chore-008` fazer o primeiro deploy real da imagem.

```bash
cd /opt/atrilha
docker compose pull
docker compose up -d

# Aguarde ~15s para o healthcheck do postgres e a inicializacao do Spring Boot
docker compose ps

# Teste o health endpoint:
curl -s https://atrilha.app/health
```

Resultados esperados:
- `docker compose ps` mostra ambos os servicos (`postgres` e `app`) com status `running`.
- `curl https://atrilha.app/health` retorna HTTP 200 com corpo JSON de status.
- `curl -I https://atrilha.app` mostra cabecalho `strict-transport-security` na resposta.

> **Nota:** Antes do primeiro deploy real (`chore-008`), `curl https://atrilha.app` pode retornar 502 — isso e esperado. O importante e que o TLS esteja funcionando (certificado valido, sem aviso de segurança no browser).

---

## Verificacoes de Estado da VPS

```bash
# Docker daemon ativo?
systemctl is-active docker

# Nginx ativo?
systemctl is-active nginx

# UFW habilitado com regras corretas?
ufw status verbose

# Certificado valido?
certbot certificates

# Login root bloqueado (deve falhar):
ssh root@<IP_DA_VPS>

# Renovacao automatica funcionando?
certbot renew --dry-run
```

---

## Notas de Seguranca

- **HSTS** esta declarado no Nginx com `max-age=31536000` (1 ano). Em emergencia de rollback de HTTPS, lembre que browsers com cache do HSTS continuarao forçando HTTPS por ate 1 ano.
- **Credenciais** de banco ficam exclusivamente em `/opt/atrilha/.env` na VPS. O `.env.example` no repositorio contem apenas chaves vazias.
- **Porta 5432** do PostgreSQL nao esta exposta no host — o container so e acessivel pela rede Docker interna `backend`.
- **Porta 8084** esta vinculada apenas ao loopback (`127.0.0.1:8084`) — inacessivel diretamente da internet; todo acesso externo passa pelo Nginx.

---

## chore-008 — CI/CD GitHub Actions: Secrets e Configuracao

Esta secao documenta os segredos e passos manuais necessarios para o pipeline de deploy automatico
(`.github/workflows/ci.yml` e `.github/workflows/deploy.yml`).

### Secrets necessarios no GitHub

Acesse **GitHub → repositorio `dionialves/atrilha` → Settings → Secrets and variables → Actions**
e crie os seguintes secrets:

| Secret | Valor | Descricao |
|--------|-------|-----------|
| `SSH_HOST` | IP ou hostname da VPS | Endereco da VPS de producao |
| `SSH_USER` | `deploy` | Usuario SSH na VPS (criado no Passo 1 deste RUNBOOK) |
| `SSH_PORT` | `22` | Porta SSH (altere se customizou) |
| `SSH_PRIVATE_KEY` | Conteudo da chave privada ed25519 | Chave privada dedicada ao GitHub Actions (ver abaixo) |

> `GITHUB_TOKEN` e injetado automaticamente pelo GitHub Actions — **nao e necessario criar** este secret manualmente.
> Ele e usado para autenticar o push de imagens no GHCR (`ghcr.io/dionialves/atrilha`) e para o `docker login` efemero na VPS durante o deploy.

### Gerar o par de chaves SSH dedicado para o GitHub Actions

Execute na sua maquina local (nao na VPS):

```bash
ssh-keygen -t ed25519 -C "gh-actions-atrilha" -f ~/.ssh/gh_actions_atrilha -N ""
```

Isso gera dois arquivos:
- `~/.ssh/gh_actions_atrilha` — **chave privada** → valor do secret `SSH_PRIVATE_KEY`
- `~/.ssh/gh_actions_atrilha.pub` — **chave publica** → vai para `authorized_keys` na VPS

### Instalar a chave publica na VPS

```bash
# Na VPS, como deploy:
cat ~/.ssh/gh_actions_atrilha.pub >> /home/deploy/.ssh/authorized_keys
chmod 600 /home/deploy/.ssh/authorized_keys
```

Ou via `ssh-copy-id`:

```bash
ssh-copy-id -i ~/.ssh/gh_actions_atrilha.pub deploy@<IP_DA_VPS>
```

### Registrar a chave privada como secret no GitHub

1. Copie o conteudo completo da chave privada:
   ```bash
   cat ~/.ssh/gh_actions_atrilha
   ```
2. No GitHub: **Settings → Secrets and variables → Actions → New repository secret**
3. Nome: `SSH_PRIVATE_KEY`
4. Valor: cole o conteudo inteiro (incluindo as linhas `-----BEGIN ...-----` e `-----END ...-----`)

### Autenticacao no GHCR via GITHUB_TOKEN

O workflow usa `GITHUB_TOKEN` para:
1. **Push da imagem** no job `build-and-push` (via `docker/login-action` com `packages: write`).
2. **Pull da imagem** na VPS durante o deploy (`docker login ghcr.io ... --password-stdin`).

O `GITHUB_TOKEN` tem escopo e TTL limitados ao job — o `docker login` na VPS e feito a cada deploy
e o token nao e armazenado permanentemente. O `.docker/config.json` pode ser sobrescrito a cada execucao.

> Se o repositorio for privado e o `GITHUB_TOKEN` nao tiver permissao de leitura no GHCR para a VPS,
> crie um **Personal Access Token (classic)** com escopo `read:packages` e registre como secret `GHCR_PAT`,
> substituindo `${{ secrets.GITHUB_TOKEN }}` pelo `${{ secrets.GHCR_PAT }}` no script de deploy.
> Para repositorios publicos (caso atual), `GITHUB_TOKEN` basta.

### Protecao de branch (`main`) — Require Status Checks

Para garantir que o deploy so ocorra apos CI verde:

1. **GitHub → Settings → Branches → Add branch protection rule**
2. Branch name pattern: `main`
3. Marque: **Require status checks to pass before merging**
4. Em "Status checks that are required", adicione: `verify` (nome exato do job em `ci.yml`)
5. Marque: **Require branches to be up to date before merging**

> O nome do job deve coincidir exatamente com o definido em `.github/workflows/ci.yml` (`jobs.verify`).
> Se alterado, atualize tambem a regra de protecao.

---

## chore-009 — Smoke Test Manual

Esta secao descreve como executar o smoke test completo de producao fora do pipeline de CI/CD.

### Comando

```bash
./infra/scripts/smoke.sh https://atrilha.app
```

### Pre-requisito

Bash com `curl` e `openssl` instalados. Em macOS, o check de TLS usa `date -d` (GNU coreutils), que nao esta disponivel nativamente. Use container:

```bash
docker run --rm -v "$PWD:/w" --workdir /w alpine sh -c \
  "apk add --quiet bash curl openssl && bash infra/scripts/smoke.sh https://atrilha.app"
```

### Criterios esperados (todos devem reportar PASS)

| Check | O que valida |
|-------|-------------|
| `/health 200 + status UP` | Endpoint responde 200 com `{"status":"UP"}` no corpo |
| `/ 200 + html` | Pagina inicial responde 200 com HTML valido |
| `/rota-inexistente-xyz 404 + pagina customizada` | Retorna 404 e renderiza a pagina de erro customizada ("Pagina nao encontrada") |
| `HSTS header presente` | Cabecalho `Strict-Transport-Security` presente na resposta |
| `TLS valido por >30 dias` | Certificado TLS com mais de 30 dias de validade restantes |

### O que fazer em caso de falha por item

| Check com FAIL | Acao |
|----------------|------|
| `/health 200 + status UP` | Verificar `docker compose ps` na VPS; consultar logs: `docker compose logs app --tail 50` |
| `/ 200 + html` | Verificar Nginx: `systemctl status nginx`; checar config em `/etc/nginx/sites-enabled/` |
| `/rota-inexistente-xyz 404 + pagina customizada` | Verificar se o template `templates/error/404.html` esta no JAR e se o Spring esta configurado para erro customizado |
| `HSTS header presente` | Verificar bloco `server` HTTPS no Nginx; reiniciar Nginx apos correcao |
| `TLS valido por >30 dias` | Verificar certbot: `certbot certificates`; renovar manualmente: `certbot renew` |

### Saida esperada (sucesso)

```
PASS  /health 200 + status UP
PASS  / 200 + html
PASS  /rota-inexistente-xyz 404 + pagina customizada
PASS  HSTS header presente
PASS  TLS valido por >30 dias
Smoke OK
```

O script termina com exit code `0` em sucesso e `1` em falha (qualquer FAIL).
