# 🛡️ WATZEM Anonymous Update & Analytics Edge Server

This directory contains standalone update & telemetry edge server solutions designed to check app versions, deliver APK updates, and **track active users & device form-factors (Android TV, Mobile, Tablet, Desktop) anonymously without exposing your real backend server IP or infrastructure**.

---

## 🔒 The Architecture

```
[ WATZEM App (TV / Phone / Tablet) ]
      │
      ▼ (HTTPS Request to Edge)
┌────────────────────────────────────────────────────────┐
│  CLOUDFLARE EDGE WORKER / STANDALONE TUNNEL (Cloaked)  │
│  - GET  /api/app/version   -> Update manifest (<20ms)  │
│  - POST /api/telemetry/ping -> Heartbeat telemetry    │
│  - GET  /stats             -> Live analytics dashboard │
│  - GET  /download/app      -> 302 Redirect to APK      │
└────────────────────────────────────────────────────────┘
      │
      ├─► Serves cached version.json (< 15ms latency)
      ├─► Tracks active users, daily active (DAU), device breakdown
      └─► 302 Redirects APK download to GitHub Release / R2 Bucket
```

---

## 📊 Analytics & Telemetry Capabilities

Every WATZEM installation sends a non-invasive, anonymous heartbeat ping (throttled to once every 6 hours):

* **Unique Installs**: Non-reversible UUID generated on first run.
* **Device Classification**: Automatic classification into **Android TV**, **Mobile**, **Tablet**, or **Desktop**.
* **Device Models**: E.g. *Chromecast with Google TV*, *Sony Bravia 4K*, *Pixel 8*, *Galaxy S24*.
* **Active User Metrics**:
  * **DAU** (Daily Active Users in last 24h)
  * **WAU** (Weekly Active Users in last 7 days)
  * **MAU** (Monthly Active Users in last 30 days)
* **App Version Adoption**: Breakdown of users across app versions (`v1.1.251`, `v1.1.252`, etc.).
* **Geographic Distribution**: Non-identifying country code from Cloudflare edge headers.
* **Visual Web Dashboard**: Access live stats at `/stats` or `/dashboard`.

---

## 🚀 Option 1: Cloudflare Worker (Serverless & Free)

- **Pros:** Completely serverless, zero hosting cost, DDoS immune, globally distributed (300+ locations), hides origin IP 100%.
- **Files:** `cloudflare-worker.js`, `wrangler.toml`

### Deployment Steps:

1. **Install Wrangler** (Cloudflare CLI):
   ```bash
   cd update-server
   npm install
   ```

2. **Login to Cloudflare**:
   ```bash
   npx wrangler login
   ```

3. **(Optional) Create a KV namespace for telemetry persistence**:
   ```bash
   npx wrangler kv:namespace create WATZEM_KV
   ```
   Add the returned `id` to `wrangler.toml`.

4. **Deploy with 1 command**:
   ```bash
   npx wrangler deploy
   ```

5. Cloudflare will give you a live URL, e.g.:
   `https://watzem-update-server.<your-subdomain>.workers.dev`

6. In your root `.env` or build configuration, set:
   ```env
   VITE_UPDATE_SERVER=https://watzem-update-server.<your-subdomain>.workers.dev
   ```

---

## 🚇 Option 2: Standalone Microservice with Cloudflare Tunnel

If you prefer self-hosting the Node.js microservice (`standalone-server.mjs`) on a VPS or local server with local file-backed persistence:

1. **Start the microservice**:
   ```bash
   cd update-server
   npm install
   node standalone-server.mjs
   ```

2. **Expose it securely via Cloudflare Tunnel**:
   ```bash
   cloudflared tunnel --url http://localhost:3131
   ```

3. **Endpoints Available**:
   * Version check: `http://localhost:3131/api/app/version`
   * Telemetry Ping: `http://localhost:3131/api/telemetry/ping`
   * **Live Visual Dashboard**: `http://localhost:3131/stats`
   * JSON Telemetry API: `http://localhost:3131/api/telemetry/stats`
   * APK Download: `http://localhost:3131/download/app`
