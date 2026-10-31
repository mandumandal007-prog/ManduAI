const router = require('express').Router();
const { z } = require('zod');
const prisma = require('../db');
const requireAuth = require('../middleware/auth');
const { streamChat } = require('../ai/providers');

// Conversations
router.get('/conversations', requireAuth, async (req, res, next) => {
  try {
    const items = await prisma.conversation.findMany({
      where: { userId: req.userId },
      orderBy: { updatedAt: 'desc' },
      select: { id: true, title: true, model: true, updatedAt: true },
    });
    res.json(items);
  } catch (e) { next(e); }
});

router.post('/conversations', requireAuth, async (req, res, next) => {
  try {
    const body = z.object({ title: z.string().optional(), model: z.string().optional() }).parse(req.body || {});
    const conv = await prisma.conversation.create({
      data: { userId: req.userId, title: body.title || 'New chat', model: body.model || 'gpt-4o-mini' },
    });
    res.json(conv);
  } catch (e) { next(e); }
});

router.patch('/conversations/:id', requireAuth, async (req, res, next) => {
  try {
    const { title, model } = req.body;
    const conv = await prisma.conversation.findFirst({ where: { id: req.params.id, userId: req.userId } });
    if (!conv) return res.status(404).json({ error: 'Not found' });
    const updated = await prisma.conversation.update({ where: { id: conv.id }, data: { title, model } });
    res.json(updated);
  } catch (e) { next(e); }
});

router.delete('/conversations/:id', requireAuth, async (req, res, next) => {
  try {
    const conv = await prisma.conversation.findFirst({ where: { id: req.params.id, userId: req.userId } });
    if (!conv) return res.status(404).json({ error: 'Not found' });
    await prisma.conversation.delete({ where: { id: conv.id } });
    res.json({ ok: true });
  } catch (e) { next(e); }
});

router.get('/conversations/:id/messages', requireAuth, async (req, res, next) => {
  try {
    const conv = await prisma.conversation.findFirst({ where: { id: req.params.id, userId: req.userId } });
    if (!conv) return res.status(404).json({ error: 'Not found' });
    const messages = await prisma.message.findMany({ where: { conversationId: conv.id }, orderBy: { createdAt: 'asc' } });
    res.json(messages);
  } catch (e) { next(e); }
});

// Streaming chat
router.post('/chat', requireAuth, async (req, res, next) => {
  try {
    const body = z.object({
      conversationId: z.string().optional(),
      provider: z.string().optional(),
      model: z.string().optional(),
      system: z.string().optional(),
      message: z.string().min(1),
    }).parse(req.body);

    // Ensure conversation
    let convId = body.conversationId;
    if (convId) {
      const found = await prisma.conversation.findFirst({ where: { id: convId, userId: req.userId } });
      if (!found) return res.status(404).json({ error: 'Conversation not found' });
    } else {
      const conv = await prisma.conversation.create({
        data: { userId: req.userId, title: body.message.slice(0, 40), model: body.model || 'gpt-4o-mini' },
      });
      convId = conv.id;
    }

    await prisma.message.create({ data: { conversationId: convId, role: 'user', content: body.message } });

    const history = await prisma.message.findMany({
      where: { conversationId: convId }, orderBy: { createdAt: 'asc' },
    });

    const messages = [];
    if (body.system) messages.push({ role: 'system', content: body.system });
    for (const m of history) messages.push({ role: m.role, content: m.content });

    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');
    res.write(`event: meta\ndata: ${JSON.stringify({ conversationId: convId })}\n\n`);

    const ac = new AbortController();
    req.on('close', () => ac.abort());

    let full = '';
    try {
      for await (const chunk of streamChat({ provider: body.provider, model: body.model, messages, signal: ac.signal })) {
        full += chunk;
        res.write(`data: ${JSON.stringify({ delta: chunk })}\n\n`);
      }
      await prisma.message.create({ data: { conversationId: convId, role: 'assistant', content: full } });
      await prisma.conversation.update({ where: { id: convId }, data: { updatedAt: new Date() } });
      res.write(`event: done\ndata: {}\n\n`);
      res.end();
    } catch (err) {
      res.write(`event: error\ndata: ${JSON.stringify({ error: err.message })}\n\n`);
      res.end();
    }
  } catch (e) { next(e); }
});

module.exports = router;
