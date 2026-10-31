const router = require('express').Router();
const { z } = require('zod');
const requireAuth = require('../middleware/auth');
const { streamChat } = require('../ai/providers');
const prisma = require('../db');

/**
 * Agent runner: takes an instruction, executes a bounded tool loop.
 * Available tools: 'list_connectors' — anything else requires a confirmed action from the client.
 * The client must POST action results back for confirmed steps.
 */
router.post('/run', requireAuth, async (req, res, next) => {
  try {
    const { instruction } = z.object({ instruction: z.string().min(3) }).parse(req.body);
    const connectors = await prisma.connector.findMany({ where: { userId: req.userId, status: 'connected' } });

    const system = `You are Mandu Agent. Break the user's request into steps.
Connected tools: ${connectors.map((c) => c.provider).join(', ') || 'none'}.
For any destructive or sensitive action (send, delete, pay), respond with a step object that includes "requiresConfirmation": true. Output a JSON array of steps: [{ "step": 1, "action": "...", "tool": "...", "requiresConfirmation": false }].`;

    const messages = [{ role: 'system', content: system }, { role: 'user', content: instruction }];
    let full = '';
    for await (const chunk of streamChat({ messages, temperature: 0.2 })) full += chunk;

    res.json({ plan: full, connectors: connectors.map((c) => c.provider) });
  } catch (e) { next(e); }
});

module.exports = router;
