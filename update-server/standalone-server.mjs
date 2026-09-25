/**
 * Standalone Anonymous Update Server (Node.js / Express)
 * 
 * Run this on a separate, dedicated VPS or container behind Cloudflare Tunnel (cloudflared).
 * Your primary movie server IP is NEVER exposed.
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

app.use(cors());
app.use(express.json());

// Hide server signatures for security
app.disable('x-powered-by');

// Health check
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'watzem-update-edge' });
});

// App Version Check Endpoint
app.get(['/api/app/version', '/version.json', '/version'], (_req, res) => {
  res.setHeader('Cache-Control', 'public, max-age=60, stale-while-revalidate=300');

  if (fs.existsSync(VERSION_FILE)) {
    try {
      const data = JSON.parse(fs.readFileSync(VERSION_FILE, 'utf-8'));
      return res.json({
        latestVersion: data.version || '1.1.89',
        versionCode: Number(data.versionCode) || 91,
        minSupportedVersion: data.minSupportedVersion || '1.0.0',
        releaseDate: data.releaseDate || new Date().toISOString().split('T')[0],
        title: data.title || 'WATZEM Update Available',
        releaseNotes: data.releaseNotes || [
          'Smoother 1080p and 4K stream playback performance',
          'Separated Movie & TV views for instant loading and zero lag',
          'Streamlined compact support modal with crypto and card tips',
          'General security, stability, and memory optimizations'
        ],
        downloadUrl: data.downloadUrl || '/download/app',
        fileSize: data.fileSize || '14.3 MB',
        mandatory: data.mandatory !== undefined ? data.mandatory : true
      });
    } catch (e) {
      console.error('Failed to parse version.json:', e);
    }
  }

  // Fallback defaults
  res.json({
    latestVersion: '1.1.89',
    versionCode: 91,
    minSupportedVersion: '1.0.0',
    releaseDate: '2026-09-25',
    title: 'WATZEM Update Available',
    releaseNotes: ['General security, stability, and playback improvements'],
    downloadUrl: '/download/app',
    fileSize: '14.3 MB',
    mandatory: true
  });
});

// APK Download Route
app.get(['/download/app', '/download', '/install.apk'], (_req, res) => {
  // If local APK file is present, serve it
  if (fs.existsSync(APK_FILE)) {
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', 'attachment; filename="WATZEM.apk"');
    return res.sendFile(APK_FILE);
  }

  // Otherwise check if version.json points to external downloadUrl
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
  console.log(`🚀 WATZEM Anonymous Update Microservice running on port ${PORT}`);
  console.log(`📡 Endpoints:`);
  console.log(`   - Version check: http://localhost:${PORT}/api/app/version`);
  console.log(`   - APK Download:  http://localhost:${PORT}/download/app`);
});
