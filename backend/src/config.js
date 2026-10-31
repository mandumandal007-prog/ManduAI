require('dotenv').config();

const required = ['JWT_SECRET', 'DATABASE_URL'];
for (const k of required) {
  if (!process.env[k]) {
    console.warn(`[config] Missing env ${k}. Some features will fail.`);
  }
}

module.exports = {
  env: process.env.NODE_ENV || 'development',
  port: Number(process.env.PORT || 4000),
  corsOrigin: process.env.CORS_ORIGIN || '*',
  jwt: {
    secret: process.env.JWT_SECRET || 'dev-only-secret-change-me',
    expiresIn: process.env.JWT_EXPIRES_IN || '30d',
  },
  ai: {
    defaultProvider: process.env.AI_DEFAULT_PROVIDER || 'openai',
    openai: {
      key: process.env.OPENAI_API_KEY || '',
      baseUrl: process.env.OPENAI_BASE_URL || 'https://api.openai.com/v1',
      model: process.env.OPENAI_DEFAULT_MODEL || 'gpt-4o-mini',
      imageModel: process.env.OPENAI_IMAGE_MODEL || 'dall-e-3',
    },
    deepseek: {
      key: process.env.DEEPSEEK_API_KEY || '',
      baseUrl: process.env.DEEPSEEK_BASE_URL || 'https://api.deepseek.com',
      model: process.env.DEEPSEEK_DEFAULT_MODEL || 'deepseek-chat',
    },
    google: {
      key: process.env.GOOGLE_AI_API_KEY || '',
      model: process.env.GOOGLE_AI_DEFAULT_MODEL || 'gemini-1.5-flash',
    },
    anthropic: {
      key: process.env.ANTHROPIC_API_KEY || '',
      model: process.env.ANTHROPIC_DEFAULT_MODEL || 'claude-3-5-sonnet-latest',
    },
  },
  search: {
    tavily: process.env.TAVILY_API_KEY || '',
    serper: process.env.SERPER_API_KEY || '',
    brave: process.env.BRAVE_SEARCH_API_KEY || '',
  },
};
