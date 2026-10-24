import { Capacitor } from '@capacitor/core';
import versionData from '../../version.json';

const UMAMI_PIXEL_BASE_URL = 'https://cloud.umami.is/p/IJ9X3JipP';
const STORAGE_KEY_UID = 'watzem_anonymous_uid';
const STORAGE_KEY_FIRST_SEEN = 'watzem_first_seen';
const STORAGE_KEY_LAST_PING = 'watzem_last_pixel_ping';

export interface DeviceTelemetryInfo {
  deviceType: 'tv' | 'mobile' | 'tablet' | 'desktop';
  platform: string;
  version: string;
  resolution: string;
  isNewUser: boolean;
  uid: string;
}

/**
 * Returns or generates a persistent anonymous UUID for unique device counting.
 */
export function getOrCreateAnonymousId(): { uid: string; isNewUser: boolean } {
  if (typeof window === 'undefined') {
    return { uid: 'server', isNewUser: false };
  }

  try {
    let uid = localStorage.getItem(STORAGE_KEY_UID);
    let isNewUser = false;

    if (!uid) {
      if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        uid = crypto.randomUUID();
      } else {
        uid = 'wz_' + Math.random().toString(36).substring(2, 15) + Date.now().toString(36);
      }
      localStorage.setItem(STORAGE_KEY_UID, uid);
      localStorage.setItem(STORAGE_KEY_FIRST_SEEN, new Date().toISOString());
      isNewUser = true;
    }

    return { uid, isNewUser };
  } catch {
    return { uid: 'anonymous', isNewUser: false };
  }
}

/**
 * Detects whether the current device is a TV, tablet, mobile phone, or desktop.
 */
export function detectDeviceType(): 'tv' | 'mobile' | 'tablet' | 'desktop' {
  if (typeof window === 'undefined') return 'desktop';

  // 1. Android TV / Smart TV detection
  const isTv = Boolean(
    (window as any).__IS_ANDROID_TV__ ||
    document.documentElement.classList.contains('is-tv-mode') ||
    new URLSearchParams(window.location.search).get('tv') === '1' ||
    new URLSearchParams(window.location.search).get('mode') === 'tv' ||
    /android.*(tv|box|leanback)|googletv|smarttv|mibox|firetv|aftmm|aftt|aftm|shield/i.test(navigator.userAgent || '')
  );
  if (isTv) return 'tv';

  const width = Math.min(window.innerWidth || 0, window.screen?.width || 0);
  const isTouch = Boolean(
    'ontouchstart' in window ||
    navigator.maxTouchPoints > 0 ||
    (navigator as any).msMaxTouchPoints > 0
  );

  // 2. Tablet detection
  if (isTouch && width >= 600) {
    return 'tablet';
  }

  // 3. Mobile detection
  if (isTouch || width < 600 || /android|iphone|ipad|ipod|mobile/i.test(navigator.userAgent || '')) {
    return 'mobile';
  }

  // 4. Desktop / Web
  return 'desktop';
}

/**
 * Sends a lightweight pixel ping to Umami Cloud with device metadata query parameters.
 */
export function trackUmamiPixel(extraParams: Record<string, string> = {}) {
  if (typeof window === 'undefined') return;

  try {
    const { uid, isNewUser } = getOrCreateAnonymousId();
    const deviceType = detectDeviceType();
    const platform = Capacitor.getPlatform() || 'web';
    const version = versionData.version || '1.1.251';
    const resolution = `${window.screen?.width || window.innerWidth}x${window.screen?.height || window.innerHeight}`;

    const url = new URL(UMAMI_PIXEL_BASE_URL);
    url.searchParams.set('uid', uid);
    url.searchParams.set('device', deviceType);
    url.searchParams.set('platform', platform);
    url.searchParams.set('version', version);
    url.searchParams.set('res', resolution);
    if (isNewUser) {
      url.searchParams.set('new_user', '1');
    }

    // Attach any extra custom parameters (e.g. page, action)
    for (const [key, val] of Object.entries(extraParams)) {
      if (val) url.searchParams.set(key, val);
    }

    // Cache buster
    url.searchParams.set('_t', Date.now().toString());

    // Fire standard Image pixel request (guarantees execution across WebView, mobile, TV, and web)
    const pixel = new Image(1, 1);
    pixel.src = url.toString();
    pixel.referrerPolicy = 'no-referrer-when-downgrade';

    try {
      localStorage.setItem(STORAGE_KEY_LAST_PING, Date.now().toString());
    } catch {}
  } catch (err) {
    // Fail silently without interrupting app or player execution
    console.debug('[UmamiPixel] Ping skipped:', err);
  }
}

/**
 * Initializes the Umami pixel tracking on app launch.
 * Pings immediately, and sets up page view tracking for route changes.
 */
export function initUmamiPixel() {
  if (typeof window === 'undefined') return;

  // Initial startup ping
  trackUmamiPixel({ event: 'app_launch' });

  // Optional: Listen for URL hash/path changes to track view transitions
  window.addEventListener('hashchange', () => {
    trackUmamiPixel({ page: window.location.hash || '#home' });
  }, { passive: true });
}
