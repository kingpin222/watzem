import { createApp } from 'vue';
import App from './App.vue';
import './styles/main.css';
import { Capacitor } from '@capacitor/core';
import { StatusBar } from '@capacitor/status-bar';
import { NavigationBar } from '@hugotomazi/capacitor-navigation-bar';
import { ScreenOrientation } from '@capacitor/screen-orientation';
import { initAdBlocker } from './utils/adBlocker';
import { spatialNav } from './services/spatialNav';

// Initialize in-app Ad and Popup Blocker Engine
initAdBlocker();

// Initialize Spatial Navigation Engine for Android TV, Google TV & D-pad remotes
spatialNav.init();

// Disable browser/WebView automatic scroll restoration
if (typeof window !== 'undefined' && 'scrollRestoration' in window.history) {
  window.history.scrollRestoration = 'manual';
}

// Initialize Capacitor native plugins (Default: Fullscreen - No Status Bar, No Navigation Bar)
async function initCapacitor() {
  const isNative = Capacitor.isNativePlatform();
  if (isNative) {
    document.documentElement.classList.add('is-native-app');
    document.documentElement.style.setProperty('--safe-top', '0px');
  }

  try {
    await StatusBar.hide();
  } catch {}

  try {
    await NavigationBar.hide();
  } catch {}

  const isTv = Boolean(
    (window as any).__IS_ANDROID_TV__ ||
    document.documentElement.classList.contains('is-tv-mode') ||
    /android.*(tv|box)|googletv|smarttv|mibox|firetv/i.test(navigator.userAgent || '')
  );

  // On standard mobile devices lock portrait; on Android TV keep natural landscape
  if (!isTv) {
    try {
      await ScreenOrientation.lock({ orientation: 'portrait' });
    } catch {
      try {
        if (screen.orientation && (screen.orientation as any).lock) {
          await (screen.orientation as any).lock('portrait');
        }
      } catch {}
    }
  }

  // Set safe-area CSS vars flush to the top
  const syncSafeArea = () => {
    const style = getComputedStyle(document.documentElement);
    const top = (window as any).__ANDROID_SAFE_TOP__ ? `${(window as any).__ANDROID_SAFE_TOP__}px` : '0px';
    const bottom = style.getPropertyValue('--safe-bottom').trim() || '0px';
    const left   = style.getPropertyValue('--safe-left').trim()   || '0px';
    const right  = style.getPropertyValue('--safe-right').trim()  || '0px';

    document.documentElement.style.setProperty('--safe-top',    top);
    document.documentElement.style.setProperty('--safe-bottom', bottom);
    document.documentElement.style.setProperty('--safe-left',   left);
    document.documentElement.style.setProperty('--safe-right',  right);
  };

  requestAnimationFrame(syncSafeArea);
  setTimeout(syncSafeArea, 200);
  setTimeout(syncSafeArea, 800);
}

initCapacitor();

const app = createApp(App);
app.mount('#app');
