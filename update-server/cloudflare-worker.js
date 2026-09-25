/**
 * Cloudflare Worker: WATZEM Anonymous Update Server
 * 
 * 100% Serverless Edge Architecture:
 * - Hides your real server IP and origin infrastructure completely.
 * - Global Anycast network (300+ locations) with instant response times (<20ms).
 * - Automatic DDoS protection, SSL termination, and rate limiting.
 * - Zero server maintenance or hosting costs.
 */

// Default Update Manifest (Update this or configure via Cloudflare KV / Environment Variables)
const DEFAULT_VERSION_CONFIG = {
  version: "1.1.89",
  versionCode: 91,
  minSupportedVersion: "1.0.0",
  releaseDate: "2026-09-25",
  title: "WATZEM Update Available",
  releaseNotes: [
    "Smoother 1080p and 4K stream playback performance",
    "Separated Movie & TV views for instant loading and zero lag",
    "Streamlined compact support modal with crypto and card tips",
    "General security, stability, and memory optimizations"
  ],
  // Direct download link to GitHub Releases for kingpin222/watzem
  downloadUrl: "https://github.com/kingpin222/watzem/releases/latest/download/app-release.apk",
  fileSize: "14.3 MB",
  mandatory: true
};

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, User-Agent, X-App-Version",
  "Access-Control-Max-Age": "86400"
};

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Handle preflight CORS
    if (request.method === "OPTIONS") {
      return new Response(null, { headers: CORS_HEADERS });
    }

    if (request.method !== "GET" && request.method !== "HEAD") {
      return new Response(JSON.stringify({ error: "Method not allowed" }), {
        status: 405,
        headers: { ...CORS_HEADERS, "Content-Type": "application/json" }
      });
    }

    // 1. Version Check Endpoint: /api/app/version or /version.json or /version
    if (
      url.pathname === "/api/app/version" ||
      url.pathname === "/version.json" ||
      url.pathname === "/version" ||
      url.pathname === "/"
    ) {
      // If Cloudflare KV is configured, load dynamic version, otherwise use config/env
      let versionData = DEFAULT_VERSION_CONFIG;
      if (env.WATZEM_KV) {
        try {
          const kvData = await env.WATZEM_KV.get("version_info", { type: "json" });
          if (kvData) versionData = kvData;
        } catch (e) {
          console.warn("KV fetch error, using default config", e);
        }
      }

      // Check environment overrides
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
        fileSize: versionData.fileSize || "14.3 MB",
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

    // 2. Download Redirect / Proxy Endpoint: /download/app or /download
    if (url.pathname === "/download/app" || url.pathname === "/download" || url.pathname.endsWith(".apk")) {
      const targetUrl = env.DOWNLOAD_URL || DEFAULT_VERSION_CONFIG.downloadUrl;

      // If pointing to an external CDN/GitHub/R2, redirect with HTTP 302
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
