import { createApp } from 'vue';
import App from './App.vue';
import './styles/main.css';
import './styles/tv.css';
import { Capacitor } from '@capacitor/core';
import { StatusBar } from '@capacitor/status-bar';
import { NavigationBar } from '@hugotomazi/capacitor-navigation-bar';
import { ScreenOrientation } from '@capacitor/screen-orientation';
import { initAdBlocker } from './utils/adBlocker';
import { tvSpatialNav } from './services/tvSpatialNav';

// Initialize in-app Ad and Popup Blocker Engine
initAdBlocker();

// Check if running on Android TV or TV mode
const isTv = typeof window !== 'undefined' && Boolean(
  (window as any).__IS_ANDROID_TV__ ||
  document.documentElement.classList.contains('is-tv-mode') ||
  new URLSearchParams(window.location.search).get('tv') === '1' ||
  new URLSearchParams(window.location.search).get('mode') === 'tv' ||
  /android.*(tv|box|leanback)|googletv|smarttv|mibox|firetv|aftmm|aftt|aftm|shield/i.test(navigator.userAgent || '')
);

if (isTv) {
  document.documentElement.classList.add('is-tv-mode');
}

// Initialize Spatial Navigation for D-Pad, TV remotes, keyboards, and game controllers
tvSpatialNav.init();

// Disable browser/WebView automatic scroll restoration
if (typeof window !== 'undefined' && 'scrollRestoration' in window.history) {
  window.history.scrollRestoration = 'manual';
}

// Initialize Capacitor native plugins (Default: Fullscreen - No Status Bar, No Navigation Bar)
async function initCapacitor() {
  const isNative = Capacitor.isNativePlatform();
  if (isNative) {
    document.documentElement.classList.add('is-native-app');
    if (!isTv) {
      document.documentElement.style.setProperty('--safe-top', '36px');
    } else {
      document.documentElement.style.setProperty('--safe-top', '0px');
    }
  }

  try {
    await StatusBar.hide();
  } catch {}

  try {
    await NavigationBar.hide();
  } catch {}

  // Support both portrait and landscape orientation on mobile (TV is natively fixed landscape)
  if (!isTv) {
    try {
      await ScreenOrientation.unlock();
    } catch {
      try {
        if (screen.orientation && (screen.orientation as any).unlock) {
          await (screen.orientation as any).unlock();
        }
      } catch {}
    }
  }

  // Set safe-area CSS vars (Notch/Cutout safe zone on phone, flush 0px on TV)
  const syncSafeArea = () => {
    if (isTv) {
      document.documentElement.style.setProperty('--safe-top', '0px');
      document.documentElement.style.setProperty('--safe-bottom', '0px');
      document.documentElement.style.setProperty('--safe-left', '0px');
      document.documentElement.style.setProperty('--safe-right', '0px');
      return;
    }

    const isLandscape = window.matchMedia('(orientation: landscape)').matches || (window.innerWidth > window.innerHeight);
    const style = getComputedStyle(document.documentElement);

    let top = (window as any).__ANDROID_SAFE_TOP__ !== undefined
      ? `${(window as any).__ANDROID_SAFE_TOP__}px`
      : style.getPropertyValue('--safe-top').trim();
    let bottom = (window as any).__ANDROID_SAFE_BOTTOM__ !== undefined
      ? `${(window as any).__ANDROID_SAFE_BOTTOM__}px`
      : style.getPropertyValue('--safe-bottom').trim() || '0px';
    let left = (window as any).__ANDROID_SAFE_LEFT__ !== undefined
      ? `${(window as any).__ANDROID_SAFE_LEFT__}px`
      : style.getPropertyValue('--safe-left').trim() || '0px';
    let right = (window as any).__ANDROID_SAFE_RIGHT__ !== undefined
      ? `${(window as any).__ANDROID_SAFE_RIGHT__}px`
      : style.getPropertyValue('--safe-right').trim() || '0px';

    if (isNative) {
      if (isLandscape) {
        // In landscape, status bar is hidden, cutout notch is on the left/right sides
        if ((window as any).__ANDROID_SAFE_TOP__ === undefined) {
          top = '0px';
        }
      } else {
        const parsed = parseFloat(top);
        if (!parsed || parsed < 36) {
          top = '36px';
        }
        left = '0px';
        right = '0px';
        (window as any).__ANDROID_SAFE_LEFT__ = 0;
        (window as any).__ANDROID_SAFE_RIGHT__ = 0;
      }
    } else if (!isLandscape) {
      left = '0px';
      right = '0px';
    }

    document.documentElement.style.setProperty('--safe-top',    top || '0px');
    document.documentElement.style.setProperty('--safe-bottom', bottom || '0px');
    document.documentElement.style.setProperty('--safe-left',   left || '0px');
    document.documentElement.style.setProperty('--safe-right',  right || '0px');

    document.documentElement.classList.toggle('is-landscape', isLandscape);
    document.documentElement.classList.toggle('is-portrait', !isLandscape);
  };

  window.addEventListener('resize', syncSafeArea, { passive: true });
  window.addEventListener('orientationchange', syncSafeArea, { passive: true });
  try {
    ScreenOrientation.addListener('screenOrientationChange', () => {
      requestAnimationFrame(syncSafeArea);
      setTimeout(syncSafeArea, 150);
      setTimeout(syncSafeArea, 400);
    });
  } catch {}

  requestAnimationFrame(syncSafeArea);
  setTimeout(syncSafeArea, 200);
  setTimeout(syncSafeArea, 800);
}

initCapacitor();

const app = createApp(App);
app.mount('#app');
