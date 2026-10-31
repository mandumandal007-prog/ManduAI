const router = require('express').Router();
const { z } = require('zod');
const requireAuth = require('../middleware/auth');
const config = require('../config');

router.post('/', requireAuth, async (req, res, next) => {
  try {
    const { query, count = 6 } = z.object({ query: z.string().min(2), count: z.number().optional() }).parse(req.body);

    if (config.search.tavily) {
      const r = await fetch('https://api.tavily.com/search', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ api_key: config.search.tavily, query, max_results: count, include_answer: true }),
      });
      if (!r.ok) return res.status(r.status).json({ error: (await r.text()).slice(0, 300) });
      const j = await r.json();
      return res.json({
        answer: j.answer || null,
        results: (j.results || []).map((x) => ({ title: x.title, url: x.url, snippet: x.content })),
      });
    }

    if (config.search.serper) {
      const r = await fetch('https://google.serper.dev/search', {
        method: 'POST',
        headers: { 'X-API-KEY': config.search.serper, 'Content-Type': 'application/json' },
        body: JSON.stringify({ q: query, num: count }),
      });
      if (!r.ok) return res.status(r.status).json({ error: (await r.text()).slice(0, 300) });
      const j = await r.json();
      return res.json({
        answer: j.answerBox?.answer || j.answerBox?.snippet || null,
        results: (j.organic || []).map((x) => ({ title: x.title, url: x.link, snippet: x.snippet })),
      });
    }

    if (config.search.brave) {
      const r = await fetch(`https://api.search.brave.com/res/v1/web/search?q=${encodeURIComponent(query)}&count=${count}`, {
        headers: { 'X-Subscription-Token': config.search.brave, Accept: 'application/json' },
      });
      if (!r.ok) return res.status(r.status).json({ error: (await r.text()).slice(0, 300) });
      const j = await r.json();
      return res.json({
        answer: null,
        results: (j.web?.results || []).map((x) => ({ title: x.title, url: x.url, snippet: x.description })),
      });
    }

    return res.status(400).json({ error: 'No search provider configured. Set TAVILY_API_KEY, SERPER_API_KEY, or BRAVE_SEARCH_API_KEY.' });
  } catch (e) { next(e); }
});

module.exports = router;
