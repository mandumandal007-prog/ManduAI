const router = require('express').Router();
const crypto = require('crypto');
const prisma = require('../db');
const requireAuth = require('../middleware/auth');

// Provider catalog. Fill in real client IDs in .env to enable OAuth.
const PROVIDERS = [
  { id: 'google',   label: 'Google (Gmail/Drive/Calendar/Contacts/Sheets)', scopes: ['https://www.googleapis.com/auth/gmail.readonly', 'https://www.googleapis.com/auth/drive', 'https://www.googleapis.com/auth/calendar', 'https://www.googleapis.com/auth/contacts.readonly', 'https://www.googleapis.com/auth/spreadsheets'], envPrefix: 'GOOGLE_OAUTH', authUrl: 'https://accounts.google.com/o/oauth2/v2/auth', tokenUrl: 'https://oauth2.googleapis.com/token' },
  { id: 'github',   label: 'GitHub', scopes: ['repo', 'read:user'], envPrefix: 'GITHUB_OAUTH', authUrl: 'https://github.com/login/oauth/authorize', tokenUrl: 'https://github.com/login/oauth/access_token' },
  { id: 'notion',   label: 'Notion', scopes: [], envPrefix: 'NOTION_OAUTH', authUrl: 'https://api.notion.com/v1/oauth/authorize', tokenUrl: 'https://api.notion.com/v1/oauth/token' },
  { id: 'slack',    label: 'Slack',  scopes: ['channels:read', 'chat:write'], envPrefix: 'SLACK_OAUTH', authUrl: 'https://slack.com/oauth/v2/authorize', tokenUrl: 'https://slack.com/api/oauth.v2.access' },
  { id: 'discord',  label: 'Discord', scopes: ['identify', 'guilds'], envPrefix: 'DISCORD_OAUTH', authUrl: 'https://discord.com/oauth2/authorize', tokenUrl: 'https://discord.com/api/oauth2/token' },
  { id: 'dropbox',  label: 'Dropbox', scopes: [], envPrefix: 'DROPBOX', authUrl: 'https://www.dropbox.com/oauth2/authorize', tokenUrl: 'https://api.dropboxapi.com/oauth2/token' },
  { id: 'onedrive', label: 'OneDrive', scopes: ['Files.ReadWrite', 'offline_access'], envPrefix: 'MS_OAUTH', authUrl: 'https://login.microsoftonline.com/common/oauth2/v2.0/authorize', tokenUrl: 'https://login.microsoftonline.com/common/oauth2/v2.0/token' },
  { id: 'telegram', label: 'Telegram (bot)', scopes: [], envPrefix: 'TELEGRAM_BOT', authUrl: null, tokenUrl: null },
  { id: 'custom',   label: 'Custom REST API', scopes: [], envPrefix: null, authUrl: null, tokenUrl: null },
];

router.get('/catalog', requireAuth, (_req, res) => {
  res.json(PROVIDERS.map(({ id, label }) => ({ id, label })));
});

router.get('/', requireAuth, async (req, res, next) => {
  try {
    const rows = await prisma.connector.findMany({ where: { userId: req.userId } });
    res.json(rows.map((r) => ({ provider: r.provider, status: r.status, metadata: r.metadata, updatedAt: r.updatedAt })));
  } catch (e) { next(e); }
});

// Begin OAuth: return the authorize URL for the client to open in a browser
router.post('/:provider/authorize', requireAuth, async (req, res, next) => {
  try {
    const p = PROVIDERS.find((x) => x.id === req.params.provider);
    if (!p) return res.status(404).json({ error: 'Unknown provider' });
    if (!p.authUrl || !p.envPrefix) return res.status(400).json({ error: `${p.label} does not support OAuth start. Configure manually.` });
    const clientId = process.env[`${p.envPrefix}_CLIENT_ID`] || process.env[`${p.envPrefix}_APP_KEY`];
    const redirect = process.env[`${p.envPrefix}_REDIRECT_URI`];
    if (!clientId || !redirect) {
      return res.status(400).json({ error: `${p.label} OAuth not configured on the server. Set ${p.envPrefix}_CLIENT_ID and ${p.envPrefix}_REDIRECT_URI.` });
    }
    const state = crypto.randomBytes(24).toString('hex');
    // Persist pending connection with state
    await prisma.connector.upsert({
      where: { userId_provider: { userId: req.userId, provider: p.id } },
      create: { userId: req.userId, provider: p.id, status: 'pending', metadata: { state } },
      update: { status: 'pending', metadata: { state } },
    });
    const params = new URLSearchParams({
      client_id: clientId, redirect_uri: redirect, response_type: 'code', state,
      scope: p.scopes.join(' '),
    });
    res.json({ url: `${p.authUrl}?${params.toString()}` });
  } catch (e) { next(e); }
});

// OAuth callback (server-to-server) — exchanges code for token
router.get('/:provider/callback', async (req, res, next) => {
  try {
    const p = PROVIDERS.find((x) => x.id === req.params.provider);
    if (!p) return res.status(404).send('Unknown provider');
    const { code, state } = req.query;
    if (!code) return res.status(400).send('Missing code');
    // Find user by pending state
    const candidates = await prisma.connector.findMany({ where: { provider: p.id, status: 'pending' } });
    const match = candidates.find((c) => c.metadata?.state === state);
    if (!match) return res.status(400).send('Invalid state');

    const clientId = process.env[`${p.envPrefix}_CLIENT_ID`] || process.env[`${p.envPrefix}_APP_KEY`];
    const clientSecret = process.env[`${p.envPrefix}_CLIENT_SECRET`] || process.env[`${p.envPrefix}_APP_SECRET`];
    const redirect = process.env[`${p.envPrefix}_REDIRECT_URI`];
    if (!clientId || !clientSecret) return res.status(500).send('OAuth client not configured');

    const body = new URLSearchParams({
      client_id: clientId, client_secret: clientSecret, code: String(code), redirect_uri: redirect, grant_type: 'authorization_code',
    });
    const tokenRes = await fetch(p.tokenUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded', Accept: 'application/json' },
      body,
    });
    if (!tokenRes.ok) {
      await prisma.connector.update({ where: { id: match.id }, data: { status: 'error' } });
      return res.status(400).send('Token exchange failed');
    }
    const t = await tokenRes.json();
    await prisma.connector.update({
      where: { id: match.id },
      data: {
        status: 'connected',
        accessToken: t.access_token || null,
        refreshToken: t.refresh_token || null,
        expiresAt: t.expires_in ? new Date(Date.now() + t.expires_in * 1000) : null,
        metadata: { scope: t.scope || null },
      },
    });
    res.send('<html><body style="font-family:sans-serif;padding:40px"><h2>Connected ✅</h2><p>You can close this tab and return to Mandu AI.</p></body></html>');
  } catch (e) { next(e); }
});

router.delete('/:provider', requireAuth, async (req, res, next) => {
  try {
    await prisma.connector.deleteMany({ where: { userId: req.userId, provider: req.params.provider } });
    res.json({ ok: true });
  } catch (e) { next(e); }
});

module.exports = router;
