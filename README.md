# Mandu AI — All-in-One AI Assistant

Mandu AI is a modern Android AI assistant (Kotlin + Jetpack Compose + Material 3 + MVVM) with a secure Node.js backend. It supports AI chat with streaming, voice, image generation, web search, an AI agent runner, and a Connectors Hub for external services.

> **Honest note:** This repository is a complete, compiling foundation. All API integrations are real HTTP calls — no fake responses. Connectors that require OAuth will only work once **you** register OAuth apps with the respective providers and fill in `.env`. Nothing pretends to succeed; failures surface as errors.

## Repo layout
```

ManduAI/
├── android/        # Kotlin + Jetpack Compose app
├── backend/        # Node.js + Express + Prisma
├── docs/           # Installation, API setup, Connectors, APK build
├── .github/workflows/  # CI: android-build.yml, backend-check.yml
├── .env.example
├── README.md
└── LICENSE

```

## Quick start

### 1. Backend
```bash
cd backend
cp ../.env.example .env
# Edit .env — add DATABASE_URL, JWT_SECRET, and at least one AI provider key
npm install
npx prisma migrate deploy    # or: npx prisma db push
npm start                    # http://localhost:4000
```

### 2. Android

1. Open `android/` in Android Studio (Hedgehog+).
2. Edit `android/gradle.properties` → set `MANDU_BACKEND_URL=http://10.0.2.2:4000` (emulator → localhost).
3. Run the `app` configuration, or `gradle assembleDebug`.

### 3. Push to GitHub and get APK

The included workflow `.github/workflows/android-build.yml` builds a debug APK on every push to `main`. Download it from **Actions → Build Android APK → Artifacts**.

Full instructions: see `docs/APK_BUILD.md`.

## Features implemented

- Email/password auth (JWT via backend), session persistence with DataStore
- AI chat with **real SSE streaming** (OpenAI/DeepSeek/Gemini/Anthropic — pick one)
- Conversation list, create, delete
- Voice: SpeechRecognizer → text → chat → TextToSpeech
- Image generation (DALL·E / compatible endpoint)
- Web search proxy (Tavily/Serper/Brave — configure one)
- Connectors UI + backend OAuth scaffolding (Gmail, Drive, Calendar, GitHub, Notion, Slack, Telegram, Discord, etc.)
- AI Agent runner endpoint with tool-call loop + confirmation step
- Theming: light/dark, Material 3 dynamic color, gradients
- Bottom nav: Home · Chat · Explore · Connectors · Profile

## What's NOT included (by design)

- A production-verified OAuth client for each connector (you must register apps)
- Payment, SMS OTP, push notifications
- A signed release keystore (you create your own — see docs)

## License

MIT — see `LICENSE`.

</BDS:create_file>

<BDS:create_file fileName="ManduAI/.env.example">

```env
# ---------- Backend ----------
NODE_ENV=development
PORT=4000
DATABASE_URL="postgresql://user:password@localhost:5432/mandu?schema=public"
JWT_SECRET=change-me-to-a-long-random-string
JWT_EXPIRES_IN=30d
CORS_ORIGIN=*

# ---------- AI providers (set at least one) ----------
OPENAI_API_KEY=
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_DEFAULT_MODEL=gpt-4o-mini
OPENAI_IMAGE_MODEL=dall-e-3

DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_DEFAULT_MODEL=deepseek-chat

GOOGLE_AI_API_KEY=
GOOGLE_AI_DEFAULT_MODEL=gemini-1.5-flash

ANTHROPIC_API_KEY=
ANTHROPIC_DEFAULT_MODEL=claude-3-5-sonnet-latest

# Default provider used by /api/chat when client does not specify one
AI_DEFAULT_PROVIDER=openai

# ---------- Web search (set one) ----------
TAVILY_API_KEY=
SERPER_API_KEY=
BRAVE_SEARCH_API_KEY=

# ---------- Connectors (fill only the ones you want to enable) ----------
# Google (Gmail, Drive, Calendar, Contacts, Sheets)
GOOGLE_OAUTH_CLIENT_ID=
GOOGLE_OAUTH_CLIENT_SECRET=
GOOGLE_OAUTH_REDIRECT_URI=http://localhost:4000/api/connectors/google/callback

# GitHub
GITHUB_OAUTH_CLIENT_ID=
GITHUB_OAUTH_CLIENT_SECRET=
GITHUB_OAUTH_REDIRECT_URI=http://localhost:4000/api/connectors/github/callback

# Notion
NOTION_OAUTH_CLIENT_ID=
NOTION_OAUTH_CLIENT_SECRET=
NOTION_OAUTH_REDIRECT_URI=http://localhost:4000/api/connectors/notion/callback

# Slack
SLACK_OAUTH_CLIENT_ID=
SLACK_OAUTH_CLIENT_SECRET=
SLACK_OAUTH_REDIRECT_URI=http://localhost:4000/api/connectors/slack/callback

# Telegram (bot token, no OAuth)
TELEGRAM_BOT_TOKEN=

# Discord
DISCORD_OAUTH_CLIENT_ID=
DISCORD_OAUTH_CLIENT_SECRET=
DISCORD_OAUTH_REDIRECT_URI=http://localhost:4000/api/connectors/discord/callback

# Dropbox
DROPBOX_APP_KEY=
DROPBOX_APP_SECRET=
DROPBOX_OAUTH_REDIRECT_URI=http://localhost:4000/api/connectors/dropbox/callback

# OneDrive / Microsoft Graph
MS_OAUTH_CLIENT_ID=
MS_OAUTH_CLIENT_SECRET=
MS_OAUTH_REDIRECT_URI=http://localhost:4000/api/connectors/onedrive/callback

