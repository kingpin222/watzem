import { createApp } from 'vue';
import App from './App.vue';
import './styles/main.css';
import { StatusBar, Style } from '@capacitor/status-bar';
import { ScreenOrientation } from '@capacitor/screen-orientation';
import { initAdBlocker } from './utils/adBlocker';

// Initialize in-app Ad and Popup Blocker Engine
initAdBlocker();

// Disable browser/WebView automatic scroll restoration
if (typeof window !== 'undefined' && 'scrollRestoration' in window.history) {
  window.history.scrollRestoration = 'manual';
}

// Initialize Capacitor native plugins (Default app lock: Portrait)
async function initCapacitor() {
  try {
    await StatusBar.setStyle({ style: Style.Dark });
    await StatusBar.setBackgroundColor({ color: '#07070a' });
  } catch {
    // Running in web browser or preview
  }

  try {
    await ScreenOrientation.lock({ orientation: 'portrait' });
  } catch {
    try {
      if (screen.orientation && (screen.orientation as any).lock) {
        await (screen.orientation as any).lock('portrait');
      }
    } catch {}
  }

  // Freeze safe-area CSS vars as static values so that StatusBar.hide()/show()
  // during player open/close does NOT cause the header padding to change and jump.
  // We read the live env() value once on startup and lock it permanently.
  requestAnimationFrame(() => {
    const style = getComputedStyle(document.documentElement);
    const top    = style.getPropertyValue('--safe-top').trim()    || '0px';
    const bottom = style.getPropertyValue('--safe-bottom').trim() || '0px';
    const left   = style.getPropertyValue('--safe-left').trim()   || '0px';
    const right  = style.getPropertyValue('--safe-right').trim()  || '0px';
    // Re-stamp as plain px values so they are immune to env() live updates
    document.documentElement.style.setProperty('--safe-top',    top);
    document.documentElement.style.setProperty('--safe-bottom', bottom);
    document.documentElement.style.setProperty('--safe-left',   left);
    document.documentElement.style.setProperty('--safe-right',  right);
  });
}

initCapacitor();

const app = createApp(App);
app.mount('#app');
