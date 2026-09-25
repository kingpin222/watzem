# 🛡️ WATZEM Anonymous Update Server (Origin Cloaking)

This directory contains standalone update server solutions designed to check app versions and deliver APK updates **without exposing your real backend server IP or origin infrastructure**.

---

## 🔒 The Architecture

```
[ Android App ]
      │
      ▼ (HTTPS Request to Edge)
┌────────────────────────────────────────────────────────┐
│  CLOUDFLARE EDGE WORKER / CDN (IP Cloaked)            │
│  - Endpoint: https://update.watzem.com/api/app/version │
│  - Endpoint: https://update.watzem.com/download/app    │
└────────────────────────────────────────────────────────┘
      │
      ├─► Serves cached version.json (< 15ms latency)
      └─► 302 Redirects APK download to GitHub Release / R2 Bucket
           (Your real server is NEVER contacted or exposed!)
```

---

## 🚀 Option 1: Cloudflare Worker (Recommended & 100% Free)

- **Pros:** Completely serverless, zero hosting cost, DDoS immune, globally distributed (300+ locations), hides origin IP 100%.
- **Files:** `cloudflare-worker.js`, `wrangler.toml`

### Quick Deployment Steps:

1. **Install Wrangler** (Cloudflare CLI):
   ```bash
   cd update-server
   npm install
   ```

2. **Login to Cloudflare**:
   ```bash
   npx wrangler login
   ```

3. **Deploy with 1 command**:
   ```bash
   npx wrangler deploy
   ```

4. Cloudflare will give you a live URL, e.g.:
   `https://watzem-update-server.<your-subdomain>.workers.dev`

5. (Optional) In Cloudflare Dashboard, you can attach your custom domain under **Workers & Pages ➔ Settings ➔ Triggers ➔ Custom Domains** (e.g. `update.watzem.com`).

6. In your root `.env` or build configuration, set:
   ```env
   VITE_UPDATE_SERVER=https://watzem-update-server.<your-subdomain>.workers.dev
   ```

---

## 📦 Option 2: GitHub Releases + Raw CDN (Zero-Maintenance)

You can host your updates completely through GitHub Releases without running any servers at all:

1. Repository: `kingpin222/watzem`
2. Push your `version.json` file to the root of the repository.
3. When releasing a new version, create a GitHub Release with tag `v1.1.89` and attach `app-release.apk`.
4. Your update URL in the app will be:
   ```env
   VITE_UPDATE_SERVER=https://raw.githubusercontent.com/kingpin222/watzem/main/version.json
   ```
5. In `version.json`, set:
   ```json
   "downloadUrl": "https://github.com/kingpin222/watzem/releases/latest/download/app-release.apk"
   ```

---

## 🚇 Option 3: Standalone Microservice with Cloudflare Tunnel

If you prefer self-hosting the Node.js microservice (`standalone-server.mjs`) on a VPS or home server, use **Cloudflare Tunnel (`cloudflared`)** so no inbound ports are opened and your real IP is never exposed to DNS or scanners:

1. Start the microservice:
   ```bash
   cd update-server
   npm install
   node standalone-server.mjs
   ```

2. Expose it securely via Cloudflare Tunnel:
   ```bash
   cloudflared tunnel --url http://localhost:3131
   ```

3. Cloudflare Tunnel generates an encrypted edge tunnel URL (e.g. `https://xyz-random.trycloudflare.com` or maps to your proxied domain). Outbound traffic is proxied through Cloudflare; your origin IP remains completely hidden.
