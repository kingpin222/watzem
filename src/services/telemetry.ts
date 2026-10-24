import { Device, DeviceInfo } from '@capacitor/device';
import { getTelemetryEndpoint } from '../config/api';
import versionData from '../../version.json';

export type DeviceCategory = 'tv' | 'mobile' | 'tablet' | 'desktop';

export interface TelemetryPayload {
  installId: string;
  deviceType: DeviceCategory;
  model: string;
  manufacturer: string;
  platform: string;
  osVersion: string;
  appVersion: string;
  versionCode: number;
  screenWidth: number;
  screenHeight: number;
  pixelRatio: number;
  language: string;
  timestamp: string;
}

const STORAGE_KEY_INSTALL_ID = 'watzem_anonymous_install_id';
const STORAGE_KEY_LAST_PING = 'watzem_last_telemetry_ping_ts';
const STORAGE_KEY_LAST_VERSION = 'watzem_last_telemetry_version';
const PING_INTERVAL_MS = 6 * 60 * 60 * 1000; // 6 hours

/**
 * Get or generate an anonymous, non-reversible install UUID
 */
export function getAnonymousInstallId(): string {
  try {
    let id = localStorage.getItem(STORAGE_KEY_INSTALL_ID);
    if (!id || id.length < 10) {
      if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        id = crypto.randomUUID();
      } else {
        id = 'watzem_' + Math.random().toString(36).substring(2, 15) + '_' + Date.now().toString(36);
      }
      localStorage.setItem(STORAGE_KEY_INSTALL_ID, id);
    }
    return id;
  } catch {
    return 'watzem_session_' + Date.now().toString(36);
  }
}

/**
 * Detect device form-factor (TV, Mobile, Tablet, Desktop)
 */
export function detectDeviceCategory(info?: DeviceInfo | null): DeviceCategory {
  if (typeof window === 'undefined') return 'mobile';

  // 1. Android TV indicator check
  const isTvClass = document.documentElement.classList.contains('is-tv-mode') || 
                    document.documentElement.classList.contains('is-dpad-active');
  const ua = (navigator.userAgent || '').toLowerCase();
  const isTvUa = ua.includes('googletv') || ua.includes('android tv') || ua.includes('smarttv') || 
                 ua.includes('leanback') || ua.includes('aft') || ua.includes('bravia') || ua.includes('tizen');

  if (isTvClass || isTvUa) {
    return 'tv';
  }

  // 2. Mobile vs Tablet vs Desktop detection
  const width = window.innerWidth || (window.screen ? window.screen.width : 360);
  const height = window.innerHeight || (window.screen ? window.screen.height : 640);
  const minDim = Math.min(width, height);
  const maxDim = Math.max(width, height);

  const isAndroidOrIos = (info?.operatingSystem === 'android' || info?.operatingSystem === 'ios') || 
                         /android|iphone|ipad|ipod/i.test(ua);

  if (isAndroidOrIos) {
    // Typical tablet: shortest dimension >= 600px
    if (minDim >= 600 && maxDim >= 960) {
      return 'tablet';
    }
    return 'mobile';
  }

  // Desktop browser / web
  if (width >= 1024) {
    return 'desktop';
  }

  return 'mobile';
}

/**
 * Send an anonymous heartbeat ping to the telemetry server
 */
export async function sendTelemetryPing(force = false): Promise<boolean> {
  try {
    const endpoint = getTelemetryEndpoint();
    if (!endpoint) {
      return false;
    }

    const appVer = String(versionData.version || '1.1.251');
    const versionCode = Number(versionData.versionCode) || 253;

    // Throttle check
    const now = Date.now();
    const lastPing = Number(localStorage.getItem(STORAGE_KEY_LAST_PING) || '0');
    const lastVer = localStorage.getItem(STORAGE_KEY_LAST_VERSION);

    if (!force && lastVer === appVer && now - lastPing < PING_INTERVAL_MS) {
      return false; // Already pinged recently
    }

    // Gather device info safely
    let info: DeviceInfo | null = null;
    try {
      info = await Device.getInfo();
    } catch {
      // Fallback if plugin is unavailable
    }

    const payload: TelemetryPayload = {
      installId: getAnonymousInstallId(),
      deviceType: detectDeviceCategory(info),
      model: info?.model || 'Unknown',
      manufacturer: info?.manufacturer || 'Generic',
      platform: info?.platform || (navigator.userAgent.includes('Android') ? 'android' : 'web'),
      osVersion: info?.osVersion || 'Unknown',
      appVersion: appVer,
      versionCode,
      screenWidth: window.screen ? window.screen.width : window.innerWidth,
      screenHeight: window.screen ? window.screen.height : window.innerHeight,
      pixelRatio: window.devicePixelRatio || 1,
      language: navigator.language || 'en',
      timestamp: new Date().toISOString()
    };

    // Use sendBeacon if available, otherwise fetch
    const body = JSON.stringify(payload);
    let sent = false;

    if (typeof navigator !== 'undefined' && navigator.sendBeacon) {
      try {
        sent = navigator.sendBeacon(endpoint, new Blob([body], { type: 'application/json' }));
      } catch {
        sent = false;
      }
    }

    if (!sent) {
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body,
        keepalive: true
      });
      sent = res.ok;
    }

    localStorage.setItem(STORAGE_KEY_LAST_PING, String(now));
    localStorage.setItem(STORAGE_KEY_LAST_VERSION, appVer);
    return true;
  } catch {
    // Telemetry must never crash or affect user experience
    return false;
  }
}

/**
 * Initialize telemetry on app start (deferred by 2.5s to let critical UI load first)
 */
export function initTelemetry() {
  if (typeof window === 'undefined') return;

  setTimeout(() => {
    sendTelemetryPing().catch(() => {});
  }, 2500);
}
