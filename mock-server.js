const express = require('express');
const app = express();
const port = 8080;

app.use(express.json());

// In-memory storage for user settings
let userSettings = {
  dailyGoal: 10000,
  notifications: true,
  theme: 'dark',
  units: 'steps'
};

// Middleware for authentication check
const requireAuth = (req, res, next) => {
  const auth = req.headers.authorization;
  if (!auth) {
    return res.status(401).json({ error: 'Unauthorized', message: 'Authentication required' });
  }
  
  // Validate token - reject expired/invalid tokens for stricter testing
  if (auth.includes('expired') || auth.includes('invalid') || auth === '') {
    return res.status(401).json({ error: 'Unauthorized', message: 'Invalid or expired token' });
  }
  
  next();
};

// Mock Firebase Firestore endpoints
app.get('/api/steps', requireAuth, (req, res) => {
  // Test for invalid params
  if (req.query.invalid) {
    return res.status(400).json({ error: 'Bad Request', message: 'Invalid parameters' });
  }
  
  // Use the requested date or default to today
  const requestedDate = req.query.date || new Date().toISOString().split('T')[0];
  
  res.json({
    steps: 12500,
    date: requestedDate
  });
});

app.post('/api/steps/sync', requireAuth, (req, res) => {
  console.log('Sync request:', req.body);
  
  // Validate request - check if fields exist, not just truthiness (0 is valid)
  if (req.body.steps === undefined || req.body.timestamp === undefined) {
    return res.status(400).json({ error: 'Bad Request', message: 'Missing required fields' });
  }
  
  res.json({ 
    success: true, 
    message: 'Successfully synced steps data to backend',
    data: { steps: req.body.steps, synced_at: new Date().toISOString() }
  });
});

app.post('/api/backup', requireAuth, (req, res) => {
  console.log('Backup request:', req.body);
  
  // Validate request
  if (!req.body.data) {
    return res.status(400).json({ error: 'Bad Request', message: 'Data field is required' });
  }
  
  res.json({ 
    success: true, 
    message: 'User data has been successfully backed up to cloud storage',
    backupId: 'backup_' + Date.now()
  });
});

app.get('/api/settings', requireAuth, (req, res) => {
  res.json(userSettings);
});

app.put('/api/settings', requireAuth, (req, res) => {
  console.log('Settings update:', req.body);
  
  // Validate settings payload - check types
  const allowedSettings = {
    dailyGoal: 'number',
    notifications: 'boolean',
    theme: 'string',
    units: 'string'
  };
  
  // Check for invalid field types
  for (const [key, value] of Object.entries(req.body)) {
    if (allowedSettings[key]) {
      const expectedType = allowedSettings[key];
      if (typeof value !== expectedType) {
        return res.status(400).json({ 
          error: 'Bad Request', 
          message: `Invalid type for ${key}: expected ${expectedType}, got ${typeof value}` 
        });
      }
    }
  }
  
  // Update settings
  if (req.body.dailyGoal !== undefined) userSettings.dailyGoal = req.body.dailyGoal;
  if (req.body.notifications !== undefined) userSettings.notifications = req.body.notifications;
  if (req.body.theme !== undefined) userSettings.theme = req.body.theme;
  if (req.body.units !== undefined) userSettings.units = req.body.units;
  
  res.json({ 
    success: true, 
    message: 'User settings have been successfully updated',
    settings: userSettings
  });
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok', server: 'mock-firebase' });
});

const server = app.listen(port, () => {
  console.log(`Mock Firebase server running on http://localhost:${port}`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('Server shutting down...');
  server.close(() => {
    console.log('Server shut down');
    process.exit(0);
  });
});