import { ref, computed } from 'vue';

const isTvModeState = ref<boolean>(false);

function detectTv(): boolean {
  if (typeof window === 'undefined') return false;

  // 1. Injected global from Android TV MainActivity
  if ((window as any).__IS_ANDROID_TV__ === true) {
    return true;
  }

  // 2. Already tagged on root element
  if (document.documentElement.classList.contains('is-tv-mode')) {
    return true;
  }

  // 3. Explicit URL parameter for testing: ?tv=1 or ?mode=tv
  try {
    const params = new URLSearchParams(window.location.search);
    if (params.get('tv') === '1' || params.get('mode') === 'tv') {
      return true;
    }
  } catch {}

  // 4. Stored preference
  try {
    if (localStorage.getItem('watzem_force_tv_mode') === 'true') {
      return true;
    }
  } catch {}

  // 5. User-Agent TV heuristics (Android TV, Google TV, MiBox, FireTV, Shield, etc.)
  const ua = navigator.userAgent || '';
  if (/android.*(tv|box|leanback)|googletv|smarttv|mibox|firetv|aftmm|aftt|aftm|shield/i.test(ua)) {
    return true;
  }

  return false;
}

// Initial detection
if (typeof window !== 'undefined') {
  isTvModeState.value = detectTv();
  if (isTvModeState.value) {
    document.documentElement.classList.add('is-tv-mode');
  }
}

export function useTvMode() {
  const isTvMode = computed(() => isTvModeState.value);

  const setTvMode = (val: boolean) => {
    isTvModeState.value = val;
    if (typeof window !== 'undefined') {
      if (val) {
        document.documentElement.classList.add('is-tv-mode');
        try { localStorage.setItem('watzem_force_tv_mode', 'true'); } catch {}
      } else {
        document.documentElement.classList.remove('is-tv-mode');
        try { localStorage.removeItem('watzem_force_tv_mode'); } catch {}
      }
    }
  };

  const toggleTvMode = () => {
    setTvMode(!isTvModeState.value);
  };

  return {
    isTvMode,
    setTvMode,
    toggleTvMode
  };
}
