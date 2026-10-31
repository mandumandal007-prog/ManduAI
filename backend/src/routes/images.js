const router = require('express').Router();
const { z } = require('zod');
const requireAuth = require('../middleware/auth');
const config = require('../config');

router.post('/generate', requireAuth, async (req, res, next) => {
  try {
    const { prompt, size = '1024x1024', n = 1 } = z.object({
      prompt: z.string().min(2),
      size: z.string().optional(),
      n: z.number().int().min(1).max(4).optional(),
    }).parse(req.body);

    if (!config.ai.openai.key) {
      return res.status(400).json({ error: 'OPENAI_API_KEY required for image generation' });
    }

    const r = await fetch(`${config.ai.openai.baseUrl}/images/generations`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${config.ai.openai.key}` },
      body: JSON.stringify({ model: config.ai.openai.imageModel, prompt, size, n }),
    });
    if (!r.ok) {
      const t = await r.text();
      return res.status(r.status).json({ error: t.slice(0, 400) });
    }
    const json = await r.json();
    res.json({ images: (json.data || []).map((d) => ({ url: d.url, b64: d.b64_json })) });
  } catch (e) { next(e); }
});

module.exports = router;
