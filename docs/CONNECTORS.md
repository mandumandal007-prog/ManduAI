# Connectors — Configuration Guide

Every connector requires a real OAuth app registered with the provider. **The app will not pretend to be connected until the OAuth exchange with the provider actually succeeds.**

## Add a new OAuth app
1. Register an OAuth app on the provider's developer console.
2. Set the redirect URI to `https://<your-backend>/api/connectors/<provider>/callback`.
3. Copy Client ID and Client Secret into `.env` (see `.env.example` for exact variable names).
4. Restart the backend.

## Provider quick-links

| Provider | Console | Scopes you need |
|---|---|---|
| Google (Gmail/Drive/Calendar/Contacts/Sheets) | https://console.cloud.google.com/apis/credentials | gmail.readonly, drive, calendar, contacts.readonly, spreadsheets |
| GitHub | https://github.com/settings/developers | repo, read:user |
| Notion | https://www.notion.so/my-integrations | read_content, insert_content |
| Slack | https://api.slack.com/apps | channels:read, chat:write |
| Discord | https://discord.com/developers/applications | identify, guilds |
| Dropbox | https://www.dropbox.com/developers/apps | files.content.write, files.content.read |
| OneDrive (Microsoft Graph) | https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps | Files.ReadWrite, offline_access |
| Telegram | @BotFather → /newbot | (no OAuth; paste bot token into `TELEGRAM_BOT_TOKEN`) |
| Custom REST | n/a | Provide the base URL in the app's connector settings |

## Flow
1. User taps **Connect** → app `POST /api/connectors/:provider/authorize`
2. Backend returns `{ url }` → app opens `url` in Chrome Custom Tabs
3. Provider redirects to your backend's `/callback`
4. Backend exchanges code → stores tokens in the DB → user returns to the app
5. App refreshes `/api/connectors` and shows the new status

## Security
- Access and refresh tokens live **only** in the database.
- The Android client only ever sees `{ provider, status, metadata }`.
