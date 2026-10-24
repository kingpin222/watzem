/**
 * Standalone Anonymous Update & Telemetry Server (Node.js / Express)
 * 
 * Run this on a separate, dedicated VPS or container behind Cloudflare Tunnel (cloudflared).
 * Tracks active devices, form-factors (TV, Mobile, Tablet, Desktop), and app versions anonymously.
 */

import express from 'express';
import cors from 'cors';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3131;
const VERSION_FILE = path.join(__dirname, 'version.json');
const APK_FILE = path.join(__dirname, 'install.apk');
const TELEMETRY_FILE = path.join(__dirname, 'telemetry-db.json');

app.use(cors());
app.use(express.json());

// Hide server signatures for security
app.disable('x-powered-by');

// In-Memory Telemetry Database with Debounced File Persistence
let telemetryData = { devices: {} };
if (fs.existsSync(TELEMETRY_FILE)) {
  try {
    telemetryData = JSON.parse(fs.readFileSync(TELEMETRY_FILE, 'utf-8'));
    if (!telemetryData.devices) telemetryData.devices = {};
  } catch (err) {
    console.error('Failed to parse telemetry-db.json, starting fresh:', err);
    telemetryData = { devices: {} };
  }
}

let saveTimeout = null;
function saveTelemetryDebounced() {
  if (saveTimeout) clearTimeout(saveTimeout);
  saveTimeout = setTimeout(() => {
    try {
      fs.writeFileSync(TELEMETRY_FILE, JSON.stringify(telemetryData, null, 2), 'utf-8');
    } catch (e) {
      console.error('Error saving telemetry database:', e);
    }
  }, 3000);
}

// Health check
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'watzem-update-edge' });
});

// ── 1. Telemetry Ping Endpoint ─────────────────────────────────────────────
app.post(['/api/telemetry/ping', '/api/ping'], (req, res) => {
  const {
    installId,
    deviceType,
    model,
    manufacturer,
    platform,
    osVersion,
    appVersion,
    versionCode,
    screenWidth,
    screenHeight,
    language
  } = req.body || {};

  if (!installId || typeof installId !== 'string') {
    return res.status(400).json({ error: 'installId is required' });
  }

  const country = req.headers['cf-ipcountry'] || req.headers['x-country'] || 'Unknown';
  const now = new Date().toISOString();

  const existing = telemetryData.devices[installId] || {
    firstSeen: now,
    pings: 0
  };

  telemetryData.devices[installId] = {
    ...existing,
    installId,
    deviceType: deviceType || 'mobile',
    model: model || 'Generic',
    manufacturer: manufacturer || 'Unknown',
    platform: platform || 'android',
    osVersion: osVersion || 'Unknown',
    appVersion: appVersion || '1.0.0',
    versionCode: Number(versionCode) || 0,
    screenWidth: Number(screenWidth) || 0,
    screenHeight: Number(screenHeight) || 0,
    language: language || 'en',
    country,
    lastSeen: now,
    pings: (existing.pings || 0) + 1
  };

  saveTelemetryDebounced();
  res.json({ ok: true, ack: now });
});

// ── 2. Telemetry Statistics Calculation ───────────────────────────────────
function computeTelemetryStats() {
  const now = Date.now();
  const DAY_MS = 24 * 60 * 60 * 1000;

  const devicesList = Object.values(telemetryData.devices);
  const totalInstalls = devicesList.length;

  let active24h = 0;
  let active7d = 0;
  let active30d = 0;

  const byDeviceType = { tv: 0, mobile: 0, tablet: 0, desktop: 0 };
  const byVersion = {};
  const byModel = {};
  const byCountry = {};

  for (const d of devicesList) {
    const lastSeenTime = new Date(d.lastSeen).getTime();
    const diff = now - lastSeenTime;

    if (diff <= DAY_MS) active24h++;
    if (diff <= 7 * DAY_MS) active7d++;
    if (diff <= 30 * DAY_MS) active30d++;

    // Device breakdown
    const dt = d.deviceType || 'mobile';
    byDeviceType[dt] = (byDeviceType[dt] || 0) + 1;

    // Version breakdown
    const v = d.appVersion || 'Unknown';
    byVersion[v] = (byVersion[v] || 0) + 1;

    // Model breakdown
    const m = d.model || 'Unknown';
    byModel[m] = (byModel[m] || 0) + 1;

    // Country breakdown
    const c = d.country || 'Unknown';
    byCountry[c] = (byCountry[c] || 0) + 1;
  }

  return {
    totalInstalls,
    active24h,
    active7d,
    active30d,
    byDeviceType,
    byVersion,
    topModels: Object.entries(byModel).sort((a, b) => b[1] - a[1]).slice(0, 10),
    topCountries: Object.entries(byCountry).sort((a, b) => b[1] - a[1]).slice(0, 10)
  };
}

// ── 3. JSON Stats API ─────────────────────────────────────────────────────
app.get(['/api/telemetry/stats', '/api/stats'], (_req, res) => {
  res.setHeader('Cache-Control', 'no-cache');
  res.json(computeTelemetryStats());
});

// ── 4. Web Visual Dashboard ────────────────────────────────────────────────
app.get(['/stats', '/dashboard'], (_req, res) => {
  const stats = computeTelemetryStats();
  const tvPct = stats.totalInstalls ? Math.round((stats.byDeviceType.tv / stats.totalInstalls) * 100) : 0;
  const mobilePct = stats.totalInstalls ? Math.round((stats.byDeviceType.mobile / stats.totalInstalls) * 100) : 0;
  const tabletPct = stats.totalInstalls ? Math.round((stats.byDeviceType.tablet / stats.totalInstalls) * 100) : 0;

  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>WATZEM Analytics Dashboard</title>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
    :root {
      --bg: #09090e;
      --card: #12121b;
      --border: rgba(255,255,255,0.08);
      --accent: #f59e0b;
      --text: #ffffff;
      --text-dim: rgba(255,255,255,0.6);
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background: var(--bg);
      color: var(--text);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      padding: 2rem 1rem;
      min-height: 100vh;
    }
    .container { max-width: 1080px; margin: 0 auto; display: flex; flex-direction: column; gap: 1.5rem; }
    header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem; border-bottom: 1px solid var(--border); padding-bottom: 1rem; }
    h1 { font-size: 1.6rem; font-weight: 800; display: flex; align-items: center; gap: 0.5rem; }
    .badge { font-size: 0.75rem; background: rgba(245, 158, 11, 0.2); color: var(--accent); padding: 4px 10px; border-radius: 999px; font-weight: 700; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; }
    .card { background: var(--card); border: 1px solid var(--border); border-radius: 14px; padding: 1.25rem; display: flex; flex-direction: column; gap: 0.5rem; }
    .card-label { font-size: 0.8rem; font-weight: 700; text-transform: uppercase; color: var(--text-dim); letter-spacing: 0.5px; }
    .card-value { font-size: 2.2rem; font-weight: 900; color: #fff; }
    .card-sub { font-size: 0.75rem; color: #10b981; font-weight: 600; }
    .tables-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1.5rem; }
    table { width: 100%; border-collapse: collapse; font-size: 0.85rem; margin-top: 0.5rem; }
    th { text-align: left; padding: 8px; color: var(--text-dim); border-bottom: 1px solid var(--border); }
    td { padding: 8px; border-bottom: 1px solid rgba(255,255,255,0.04); }
    .bar-wrap { width: 100%; height: 8px; background: rgba(255,255,255,0.06); border-radius: 999px; overflow: hidden; display: flex; margin-top: 0.5rem; }
    .bar-tv { background: #3b82f6; width: ${tvPct}%; }
    .bar-mobile { background: #10b981; width: ${mobilePct}%; }
    .bar-tablet { background: #ec4899; width: ${tabletPct}%; }
    .legend { display: flex; gap: 1rem; font-size: 0.78rem; margin-top: 0.5rem; flex-wrap: wrap; }
    .dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <h1>🍿 WATZEM Analytics <span class="badge">Live</span></h1>
      <span style="font-size:0.8rem; color:var(--text-dim);">Auto-updates on page refresh</span>
    </header>

    <div class="grid">
      <div class="card">
        <span class="card-label">Total Unique Devices</span>
        <span class="card-value">${stats.totalInstalls.toLocaleString()}</span>
        <span class="card-sub">All-time anonymous installs</span>
      </div>
      <div class="card">
        <span class="card-label">Daily Active (24h)</span>
        <span class="card-value">${stats.active24h.toLocaleString()}</span>
        <span class="card-sub">Active today</span>
      </div>
      <div class="card">
        <span class="card-label">Weekly Active (7d)</span>
        <span class="card-value">${stats.active7d.toLocaleString()}</span>
        <span class="card-sub">Active past week</span>
      </div>
      <div class="card">
        <span class="card-label">Monthly Active (30d)</span>
        <span class="card-value">${stats.active30d.toLocaleString()}</span>
        <span class="card-sub">Active past month</span>
      </div>
    </div>

    <div class="card">
      <span class="card-label">Device Classification Breakdown</span>
      <div class="bar-wrap">
        <div class="bar-tv" title="TV: ${stats.byDeviceType.tv}"></div>
        <div class="bar-mobile" title="Mobile: ${stats.byDeviceType.mobile}"></div>
        <div class="bar-tablet" title="Tablet: ${stats.byDeviceType.tablet}"></div>
      </div>
      <div class="legend">
        <span><span class="dot" style="background:#3b82f6;"></span> Android TV: <b>${stats.byDeviceType.tv}</b> (${tvPct}%)</span>
        <span><span class="dot" style="background:#10b981;"></span> Mobile: <b>${stats.byDeviceType.mobile}</b> (${mobilePct}%)</span>
        <span><span class="dot" style="background:#ec4899;"></span> Tablet: <b>${stats.byDeviceType.tablet}</b> (${tabletPct}%)</span>
      </div>
    </div>

    <div class="tables-grid">
      <div class="card">
        <span class="card-label">Top App Versions</span>
        <table>
          <thead><tr><th>Version</th><th>Users</th></tr></thead>
          <tbody>
            ${Object.entries(stats.byVersion).map(([ver, count]) => `<tr><td>v${ver}</td><td><b>${count}</b></td></tr>`).join('') || '<tr><td colspan="2">No data yet</td></tr>'}
          </tbody>
        </table>
      </div>

      <div class="card">
        <span class="card-label">Top Device Models</span>
        <table>
          <thead><tr><th>Model</th><th>Count</th></tr></thead>
          <tbody>
            ${stats.topModels.map(([mod, count]) => `<tr><td>${mod}</td><td><b>${count}</b></td></tr>`).join('') || '<tr><td colspan="2">No data yet</td></tr>'}
          </tbody>
        </table>
      </div>
    </div>
  </div>
</body>
</html>`;
  res.setHeader('Content-Type', 'text/html');
  res.send(html);
});

// ── 5. App Version Check Endpoint ──────────────────────────────────────────
app.get(['/api/app/version', '/version.json', '/version'], (_req, res) => {
  res.setHeader('Cache-Control', 'public, max-age=60, stale-while-revalidate=300');

  if (fs.existsSync(VERSION_FILE)) {
    try {
      const data = JSON.parse(fs.readFileSync(VERSION_FILE, 'utf-8'));
      return res.json({
        latestVersion: data.version || '1.1.251',
        versionCode: Number(data.versionCode) || 253,
        minSupportedVersion: data.minSupportedVersion || '1.0.0',
        releaseDate: data.releaseDate || new Date().toISOString().split('T')[0],
        title: data.title || 'WATZEM Update Available',
        releaseNotes: data.releaseNotes || [
          'Smoother 1080p and 4K stream playback performance',
          'Refined player controls and background playback',
          'General security & stability updates'
        ],
        downloadUrl: data.downloadUrl || '/download/app',
        fileSize: data.fileSize || '4.03 MB',
        mandatory: data.mandatory !== undefined ? data.mandatory : true
      });
    } catch (e) {
      console.error('Failed to parse version.json:', e);
    }
  }

  // Fallback defaults
  res.json({
    latestVersion: '1.1.251',
    versionCode: 253,
    minSupportedVersion: '1.0.0',
    releaseDate: '2026-09-25',
    title: 'WATZEM Update Available',
    releaseNotes: ['General security, stability, and playback improvements'],
    downloadUrl: '/download/app',
    fileSize: '4.03 MB',
    mandatory: true
  });
});

// ── 6. APK Download Route ──────────────────────────────────────────────────
app.get(['/download/app', '/download', '/install.apk'], (_req, res) => {
  if (fs.existsSync(APK_FILE)) {
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', 'attachment; filename="WATZEM.apk"');
    return res.sendFile(APK_FILE);
  }

  if (fs.existsSync(VERSION_FILE)) {
    try {
      const data = JSON.parse(fs.readFileSync(VERSION_FILE, 'utf-8'));
      if (data.downloadUrl && (data.downloadUrl.startsWith('http://') || data.downloadUrl.startsWith('https://'))) {
        return res.redirect(302, data.downloadUrl);
      }
    } catch (e) {
      console.error(e);
    }
  }

  res.status(404).json({ error: 'APK installer not found' });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 WATZEM Anonymous Update & Telemetry Server running on port ${PORT}`);
  console.log(`📡 Endpoints:`);
  console.log(`   - Telemetry Ping:   http://localhost:${PORT}/api/telemetry/ping`);
  console.log(`   - Live Dashboard:   http://localhost:${PORT}/stats`);
  console.log(`   - Version check:    http://localhost:${PORT}/api/app/version`);
  console.log(`   - APK Download:     http://localhost:${PORT}/download/app`);
});
