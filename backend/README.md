# Mandu Backend

Express + Prisma + JWT. All AI calls, OAuth callbacks, and file/API proxying happen here so the Android app never holds a secret.

## Run
```bash
cp ../.env.example .env
# fill DATABASE_URL, JWT_SECRET, one AI provider key
npm install
npx prisma db push
npm start
```

## Endpoints

| Method ↕▾ | Path ↕▾ | Auth ↕▾ | Description ↕▾ |
|---|---|---|---|
| −POST | /api/auth/register | – | Create account |
| −POST | /api/auth/login | – | Login, returns JWT |
| −GET | /api/auth/me | JWT | Current user |
| −GET | /api/conversations | JWT | List |
| −POST | /api/conversations | JWT | Create |
| −PATCH | /api/conversations/:id | JWT | Rename |
| −DELETE | /api/conversations/:id | JWT | Delete |
| −GET | /api/conversations/:id/messages | JWT | History |
| −POST | /api/chat | JWT | SSE stream. Body: { conversationId?, provider?, model?, system?, message } |
| −POST | /api/images/generate | JWT | DALL·E-style image |
| −POST | /api/search | JWT | Web search (Tavily/Serper/Brave) |
| −GET | /api/connectors/catalog | JWT | All providers |
| −GET | /api/connectors | JWT | User's connections |
| −POST | /api/connectors/:provider/authorize | JWT | Get OAuth URL |
| −GET | /api/connectors/:provider/callback | – | OAuth redirect handler |
| −DELETE | /api/connectors/:provider | JWT | Disconnect |
| −POST | /api/agents/run | JWT | Generate an agent plan |
⚙

## Security

- Passwords hashed with bcrypt.
- JWT signed with `JWT_SECRET`.
- Access/refresh tokens stored server-side (Prisma `Connector`), never returned to the client.
- Do NOT commit `.env`.

</BDS:create_file>

<!-- ============================ DOCS ============================ -->

<BDS:create_file fileName="ManduAI/docs/INSTALLATION.md">

```markdown
# Installation

## Prerequisites
- Node.js ≥ 18
- Android Studio Hedgehog or newer, JDK 17
- A PostgreSQL database (Supabase, Neon, RDS, or local)

## Backend
```bash
cd backend
cp ../.env.example .env
# Edit .env — required: DATABASE_URL, JWT_SECRET; choose at least one AI provider key
npm install
npx prisma db push
npm start

The server prints `Mandu backend listening on :4000`. Test: `curl http://localhost:4000/health`.

## Android

1. Open the `android/` folder in Android Studio.
2. Edit `android/gradle.properties`:

- `MANDU_BACKEND_URL=http://10.0.2.2:4000` for the emulator
- `MANDU_BACKEND_URL=https://your-host` for a real device (must be HTTPS)
3. Sync Gradle, then Run ▶.

## Emulator ↔ host networking

| Where the app runs ↕▾ | Use this backend URL ↕▾ |
|---|---|
| −Android emulator | `http://10.0.2.2:4000` |
| −Physical device on the same Wi-Fi | `http://<your-computer-LAN-IP>:4000` |
| −Deployed backend | `https://your-host` |
⚙

</BDS:create_file>

<BDS:create_file fileName="ManduAI/docs/API_SETUP.md">

```markdown
# API Setup

## AI providers (choose at least one)

### OpenAI
1. https://platform.openai.com/api-keys → create key
2. `.env`: `OPENAI_API_KEY=sk-...`, optionally change `OPENAI_DEFAULT_MODEL`
3. Image generation uses this key too.

### DeepSeek
1. https://platform.deepseek.com → API keys
2. `.env`: `DEEPSEEK_API_KEY=...`
3. `.env`: `AI_DEFAULT_PROVIDER=deepseek`

### Google Gemini
1. https://aistudio.google.com/app/apikey
2. `.env`: `GOOGLE_AI_API_KEY=...`

### Anthropic
1. https://console.anthropic.com/settings/keys
2. `.env`: `ANTHROPIC_API_KEY=...`

## Web search (choose one)
- **Tavily** → https://tavily.com (recommended for AI)
- **Serper** → https://serper.dev
- **Brave Search** → https://brave.com/search/api/

Set the matching key in `.env`. The app will show a clear error if none is configured.

## Notes
- The Android app never sees these keys — it calls `/api/chat`, `/api/search`, `/api/images/generate` on your backend.
- If you rotate a key, just restart the backend.

