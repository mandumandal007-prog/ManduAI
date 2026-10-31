const config = require('../config');

function pickProvider(name) {
  const p = (name || config.ai.defaultProvider || 'openai').toLowerCase();
  if (p === 'deepseek' && config.ai.deepseek.key) return 'deepseek';
  if (p === 'google' && config.ai.google.key) return 'google';
  if (p === 'anthropic' && config.ai.anthropic.key) return 'anthropic';
  if (config.ai.openai.key) return 'openai';
  if (config.ai.deepseek.key) return 'deepseek';
  if (config.ai.google.key) return 'google';
  if (config.ai.anthropic.key) return 'anthropic';
  throw new Error('No AI provider configured. Set OPENAI_API_KEY, DEEPSEEK_API_KEY, GOOGLE_AI_API_KEY or ANTHROPIC_API_KEY in .env');
}

/**
 * Stream a chat completion as an async generator of text chunks.
 * messages: [{role, content}]
 */
async function* streamChat({ provider, model, messages, temperature = 0.7, signal }) {
  const p = pickProvider(provider);
  if (p === 'openai') {
    yield* streamOpenAICompatible({
      baseUrl: config.ai.openai.baseUrl,
      key: config.ai.openai.key,
      model: model || config.ai.openai.model,
      messages, temperature, signal,
    });
  } else if (p === 'deepseek') {
    yield* streamOpenAICompatible({
      baseUrl: config.ai.deepseek.baseUrl,
      key: config.ai.deepseek.key,
      model: model || config.ai.deepseek.model,
      messages, temperature, signal,
    });
  } else if (p === 'google') {
    yield* streamGoogle({
      key: config.ai.google.key, model: model || config.ai.google.model,
      messages, temperature, signal,
    });
  } else if (p === 'anthropic') {
    yield* streamAnthropic({
      key: config.ai.anthropic.key, model: model || config.ai.anthropic.model,
      messages, temperature, signal,
    });
  }
}

async function* streamOpenAICompatible({ baseUrl, key, model, messages, temperature, signal }) {
  const res = await fetch(`${baseUrl}/chat/completions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${key}` },
    body: JSON.stringify({ model, messages, temperature, stream: true }),
    signal,
  });
  if (!res.ok) {
    const txt = await res.text().catch(() => '');
    throw new Error(`Provider ${res.status}: ${txt.slice(0, 400)}`);
  }
  const reader = res.body.getReader();
  const dec = new TextDecoder();
  let buf = '';
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buf += dec.decode(value, { stream: true });
    let idx;
    while ((idx = buf.indexOf('\n')) !== -1) {
      const line = buf.slice(0, idx).trim();
      buf = buf.slice(idx + 1);
      if (!line.startsWith('data:')) continue;
      const payload = line.slice(5).trim();
      if (payload === '[DONE]') return;
      try {
        const json = JSON.parse(payload);
        const delta = json.choices?.[0]?.delta?.content;
        if (delta) yield delta;
      } catch { /* skip malformed */ }
    }
  }
}

async function* streamGoogle({ key, model, messages, temperature, signal }) {
  const contents = messages
    .filter((m) => m.role !== 'system')
    .map((m) => ({ role: m.role === 'assistant' ? 'model' : 'user', parts: [{ text: m.content }] }));
  const systemInstruction = messages.filter((m) => m.role === 'system').map((m) => m.content).join('\n') || undefined;

  const res = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/${model}:streamGenerateContent?alt=sse&key=${key}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ contents, systemInstruction: systemInstruction ? { parts: [{ text: systemInstruction }] } : undefined, generationConfig: { temperature } }),
      signal,
    }
  );
  if (!res.ok) throw new Error(`Gemini ${res.status}: ${(await res.text()).slice(0, 400)}`);
  const reader = res.body.getReader();
  const dec = new TextDecoder();
  let buf = '';
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buf += dec.decode(value, { stream: true });
    let idx;
    while ((idx = buf.indexOf('\n')) !== -1) {
      const line = buf.slice(0, idx).trim();
      buf = buf.slice(idx + 1);
      if (!line.startsWith('data:')) continue;
      const payload = line.slice(5).trim();
      try {
        const json = JSON.parse(payload);
        const text = json.candidates?.[0]?.content?.parts?.map((p) => p.text || '').join('') || '';
        if (text) yield text;
      } catch { /* skip */ }
    }
  }
}

async function* streamAnthropic({ key, model, messages, temperature, signal }) {
  const system = messages.filter((m) => m.role === 'system').map((m) => m.content).join('\n');
  const rest = messages.filter((m) => m.role !== 'system').map((m) => ({ role: m.role, content: m.content }));
  const res = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-api-key': key,
      'anthropic-version': '2023-06-01',
    },
    body: JSON.stringify({ model, system: system || undefined, messages: rest, max_tokens: 2048, temperature, stream: true }),
    signal,
  });
  if (!res.ok) throw new Error(`Anthropic ${res.status}: ${(await res.text()).slice(0, 400)}`);
  const reader = res.body.getReader();
  const dec = new TextDecoder();
  let buf = '';
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buf += dec.decode(value, { stream: true });
    let idx;
    while ((idx = buf.indexOf('\n')) !== -1) {
      const line = buf.slice(0, idx).trim();
      buf = buf.slice(idx + 1);
      if (!line.startsWith('data:')) continue;
      const payload = line.slice(5).trim();
      try {
        const json = JSON.parse(payload);
        if (json.type === 'content_block_delta' && json.delta?.text) yield json.delta.text;
      } catch { /* skip */ }
    }
  }
}

module.exports = { streamChat, pickProvider };
