/*
 * Pré-load que zera os timeouts default do undici (cliente HTTP do Node).
 *
 * Motivação:
 *   O qwen-code usa o `fetch` global do Node, que internamente usa undici.
 *   undici tem dois timeouts hardcoded em 300_000 ms (5 min):
 *     - headersTimeout: tempo máximo aguardando os headers de resposta
 *     - bodyTimeout:    tempo máximo entre chunks do body de resposta
 *
 *   Para um modelo grande rodando localmente em LM Studio (MLX, 35B):
 *     - Cold load + processamento de prompt de 40k+ tokens pode estourar 5 min
 *       antes do primeiro chunk (headersTimeout/TTFT).
 *     - Pausas mid-stream por pressão de RAM/swap podem estourar 5 min entre
 *       chunks (bodyTimeout).
 *
 *   Sintoma típico:
 *     - qwen:      "API Error: terminated (cause: Body Timeout Error)"
 *     - LM Studio: "Client disconnected. Stopping generation..."
 *
 *   O `timeout: 1800000` ms (30 min) do generationConfig do qwen NÃO ajuda
 *   porque ele cobre a request inteira via AbortController, mas o undici
 *   aborta antes disso por causa do body/headers timeout interno.
 *
 * Como usar:
 *   1. Instalar undici neste diretório (uma vez):
 *        cd .qwen/scripts && npm init -y && npm install undici
 *      (node_modules/ já está em .qwen/.gitignore, sem leak no repo)
 *   2. Rodar qwen via o wrapper .qwen/scripts/qwen.sh (que ativa este preload).
 *
 * Segurança:
 *   - Mesmo com undici sem timeout, o AbortController do generationConfig
 *     (timeout: 1800000 = 30 min) ainda cobre a request inteira como teto.
 *   - keepAliveTimeout em 60s para evitar conexões pendentes acumularem.
 */

(function installNoTimeoutDispatcher() {
  try {
    const { setGlobalDispatcher, Agent } = require('undici');
    setGlobalDispatcher(new Agent({
      headersTimeout: 0,        // 0 = desabilitado, sem limite no TTFT
      bodyTimeout: 0,           // 0 = desabilitado, sem limite entre chunks
      keepAliveTimeout: 60_000, // 60s para fechar conexões idle
      keepAliveMaxTimeout: 600_000,
    }));
    if (process.env.QWEN_DEBUG_PRELOAD) {
      console.error('[no-undici-timeout] global dispatcher patched: headersTimeout=0, bodyTimeout=0');
    }
  } catch (err) {
    console.error('[no-undici-timeout] FAILED to patch undici dispatcher.');
    console.error('[no-undici-timeout] Instale undici neste diretório:');
    console.error('[no-undici-timeout]   cd .qwen/scripts && npm init -y && npm install undici');
    console.error('[no-undici-timeout] Detalhe:', err && err.message ? err.message : err);
  }
})();
