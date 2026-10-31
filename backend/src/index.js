const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const config = require('./config');

const authRoutes = require('./routes/auth');
const chatRoutes = require('./routes/chat');
const imageRoutes = require('./routes/images');
const searchRoutes = require('./routes/search');
const connectorRoutes = require('./routes/connectors');
const agentRoutes = require('./routes/agents');

const app = express();
app.use(cors({ origin: config.corsOrigin }));
app.use(express.json({ limit: '10mb' }));
app.use(morgan('dev'));

app.get('/health', (_req, res) => res.json({ ok: true, service: 'mandu-backend' }));

app.use('/api/auth', authRoutes);
app.use('/api', chatRoutes);
app.use('/api/images', imageRoutes);
app.use('/api/search', searchRoutes);
app.use('/api/connectors', connectorRoutes);
app.use('/api/agents', agentRoutes);

// Error handler
app.use((err, _req, res, _next) => {
  console.error('[error]', err);
  res.status(err.status || 500).json({ error: err.message || 'Internal error' });
});

app.listen(config.port, () => {
  console.log(`Mandu backend listening on :${config.port}`);
});
