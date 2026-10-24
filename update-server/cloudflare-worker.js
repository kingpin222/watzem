/**
 * Cloudflare Worker: WATZEM Anonymous Update & Telemetry Server
 * 
 * 100% Serverless Edge Architecture:
 * - Hides your real server IP and origin infrastructure completely.
 * - Global Anycast network (300+ locations) with instant response times (<20ms).
 * - Tracks unique devices, form factors (TV, Mobile, Tablet), active users anonymously.
 * - Zero server maintenance or hosting costs.
 */

// Default Update Manifest
const DEFAULT_VERSION_CONFIG = {
  version: "1.1.251",
  versionCode: 253,
  minSupportedVersion: "1.0.0",
  releaseDate: "2026-09-25",
  title: "WATZEM Update Available",
  releaseNotes: [
    "Smoother 1080p and 4K stream playback performance",
    "Refined player controls and background playback",
    "General security & stability updates"
  ],
  downloadUrl: "https://github.com/kingpin222/watzem/releases/latest/download/app-release.apk",
  fileSize: "4.03 MB",
  mandatory: true
};

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, HEAD, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, User-Agent, X-App-Version, CF-Connecting-IP",
  "Access-Control-Max-Age": "86400"
};

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Handle preflight CORS
    if (request.method === "OPTIONS") {
      return new Response(null, { headers: CORS_HEADERS });
    }

    // ── 1. Telemetry Ping Endpoint (POST /api/telemetry/ping or /api/ping) ──
    if (
      request.method === "POST" &&
      (url.pathname === "/api/telemetry/ping" || url.pathname === "/api/ping")
    ) {
      try {
        const payload = await request.json();
        const installId = payload?.installId;
        if (!installId) {
          return new Response(JSON.stringify({ error: "installId required" }), {
            status: 400,
            headers: { ...CORS_HEADERS, "Content-Type": "application/json" }
          });
        }

        const country = request.headers.get("cf-ipcountry") || "Unknown";
        const now = new Date().toISOString();

        // If Cloudflare KV is bound (e.g. env.WATZEM_KV)
        if (env.WATZEM_KV) {
          const key = `device:${installId}`;
          const existingStr = await env.WATZEM_KV.get(key);
          const existing = existingStr ? JSON.parse(existingStr) : { firstSeen: now, pings: 0 };

          const record = {
            ...existing,
            installId,
            deviceType: payload.deviceType || "mobile",
            model: payload.model || "Generic",
            platform: payload.platform || "android",
            osVersion: payload.osVersion || "Unknown",
            appVersion: payload.appVersion || "1.0.0",
            versionCode: payload.versionCode || 0,
            country,
            lastSeen: now,
            pings: (existing.pings || 0) + 1
          };

          // Store for 60 days
          await env.WATZEM_KV.put(key, JSON.stringify(record), { expirationTtl: 60 * 24 * 60 * 60 });
        }

        return new Response(JSON.stringify({ ok: true, ack: now }), {
          status: 200,
          headers: { ...CORS_HEADERS, "Content-Type": "application/json" }
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), {
          status: 500,
          headers: { ...CORS_HEADERS, "Content-Type": "application/json" }
        });
      }
    }

    // ── 2. Telemetry Stats API / Dashboard (GET /stats or /api/stats) ────────
    if (url.pathname === "/api/telemetry/stats" || url.pathname === "/api/stats") {
      let stats = {
        totalInstalls: 0,
        byDeviceType: { tv: 0, mobile: 0, tablet: 0, desktop: 0 },
        notice: env.WATZEM_KV ? "KV bound" : "Bind WATZEM_KV in wrangler.toml to persist records"
      };

      if (env.WATZEM_KV) {
        try {
          const list = await env.WATZEM_KV.list({ prefix: "device:" });
          stats.totalInstalls = list.keys.length;
        } catch {}
      }

      return new Response(JSON.stringify(stats, null, 2), {
        status: 200,
        headers: { ...CORS_HEADERS, "Content-Type": "application/json" }
      });
    }

    // ── 3. App Version Check Endpoint ───────────────────────────────────────
    if (
      url.pathname === "/api/app/version" ||
      url.pathname === "/version.json" ||
      url.pathname === "/version" ||
      url.pathname === "/"
    ) {
      let versionData = DEFAULT_VERSION_CONFIG;
      if (env.WATZEM_KV) {
        try {
          const kvData = await env.WATZEM_KV.get("version_info", { type: "json" });
          if (kvData) versionData = kvData;
        } catch (e) {
          console.warn("KV fetch error, using default config", e);
        }
      }

      if (env.LATEST_VERSION) versionData.version = env.LATEST_VERSION;
      if (env.VERSION_CODE) versionData.versionCode = parseInt(env.VERSION_CODE, 10);
      if (env.DOWNLOAD_URL) versionData.downloadUrl = env.DOWNLOAD_URL;

      const responsePayload = {
        latestVersion: versionData.version,
        versionCode: Number(versionData.versionCode) || 1,
        minSupportedVersion: versionData.minSupportedVersion || "1.0.0",
        releaseDate: versionData.releaseDate,
        title: versionData.title || "WATZEM Update Available",
        releaseNotes: versionData.releaseNotes || ["Performance and stability updates"],
        downloadUrl: versionData.downloadUrl || "/download/app",
        fileSize: versionData.fileSize || "4.03 MB",
        mandatory: versionData.mandatory !== undefined ? versionData.mandatory : true
      };

      return new Response(JSON.stringify(responsePayload, null, 2), {
        status: 200,
        headers: {
          ...CORS_HEADERS,
          "Content-Type": "application/json; charset=utf-8",
          "Cache-Control": "public, max-age=60, stale-while-revalidate=300"
        }
      });
    }

    // ── 4. Download Redirect: /download/app or /download ────────────────────
    if (url.pathname === "/download/app" || url.pathname === "/download" || url.pathname.endsWith(".apk")) {
      const targetUrl = env.DOWNLOAD_URL || DEFAULT_VERSION_CONFIG.downloadUrl;
      if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
        return Response.redirect(targetUrl, 302);
      }

      return new Response(JSON.stringify({ error: "Download URL not configured" }), {
        status: 404,
        headers: { ...CORS_HEADERS, "Content-Type": "application/json" }
      });
    }

    return new Response(JSON.stringify({ error: "Not Found" }), {
      status: 404,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" }
    });
  }
};
