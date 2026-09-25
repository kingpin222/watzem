/**
 * Mobile App API Configuration
 * The application operates in 100% Standalone Client Mode (Serverless):
 * - Direct TMDB REST API integration for movies, TV series, search, credits & details.
 * - Direct client-side stream embed resolution for video players.
 * - No custom backend server or proxy required for the APK to run.
 */

export const STANDALONE_CLIENT_MODE = true;

export const API_SERVER_URL = (import.meta.env.VITE_API_SERVER as string) || '';

/**
 * Dedicated Update Server URL (e.g. Cloudflare Worker, Raw CDN, or Edge Proxy)
 * Setting this ensures the client queries updates without exposing the real backend server IP.
 */
export const UPDATE_SERVER_URL = (import.meta.env.VITE_UPDATE_SERVER as string) || '';

export function getApiBaseUrl(): string {
  if (API_SERVER_URL) {
    return `${API_SERVER_URL}/api`;
  }
  return '/api';
}

export function getUpdateEndpoint(): string {
  if (UPDATE_SERVER_URL) {
    if (UPDATE_SERVER_URL.endsWith('.json')) {
      return UPDATE_SERVER_URL;
    }
    const clean = UPDATE_SERVER_URL.replace(/\/+$/, '');
    return `${clean}/api/app/version`;
  }
  const apiBase = getApiBaseUrl();
  return `${apiBase}/app/version`;
}
