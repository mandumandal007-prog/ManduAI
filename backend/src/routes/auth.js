const router = require('express').Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { z } = require('zod');
const prisma = require('../db');
const config = require('../config');
const requireAuth = require('../middleware/auth');

const creds = z.object({
  email: z.string().email(),
  password: z.string().min(6).max(120),
  name: z.string().min(1).max(80).optional(),
});

function sign(user) {
  return jwt.sign({ sub: user.id, email: user.email }, config.jwt.secret, {
    expiresIn: config.jwt.expiresIn,
  });
}

router.post('/register', async (req, res, next) => {
  try {
    const data = creds.parse(req.body);
    const exists = await prisma.user.findUnique({ where: { email: data.email } });
    if (exists) return res.status(409).json({ error: 'Email already registered' });
    const passwordHash = await bcrypt.hash(data.password, 10);
    const user = await prisma.user.create({
      data: { email: data.email, passwordHash, name: data.name || null },
    });
    res.json({ token: sign(user), user: { id: user.id, email: user.email, name: user.name } });
  } catch (e) {
    next(e);
  }
});

router.post('/login', async (req, res, next) => {
  try {
    const data = creds.parse(req.body);
    const user = await prisma.user.findUnique({ where: { email: data.email } });
    if (!user) return res.status(401).json({ error: 'Invalid credentials' });
    const ok = await bcrypt.compare(data.password, user.passwordHash);
    if (!ok) return res.status(401).json({ error: 'Invalid credentials' });
    res.json({ token: sign(user), user: { id: user.id, email: user.email, name: user.name } });
  } catch (e) {
    next(e);
  }
});

router.get('/me', requireAuth, async (req, res, next) => {
  try {
    const user = await prisma.user.findUnique({ where: { id: req.userId } });
    if (!user) return res.status(404).json({ error: 'Not found' });
    res.json({ id: user.id, email: user.email, name: user.name });
  } catch (e) {
    next(e);
  }
});

module.exports = router;
