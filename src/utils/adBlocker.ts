/**
 * Comprehensive In-App Ad & Popup Blocker Engine
 * Powered by AdGuard Filter Lists (AdGuard DNS Filter, Base Filter, Popups & Annoyances Filter)
 * 
 * Neutralizes rogue ad popups, window.open calls, synthetic link clicks,
 * tracking pixels, ad script injections, cookie sync networks, and clickjacking overlays.
 */

// ── AdGuard Blocked Domains List ─────────────────────────────────────────────
// Compiled from AdGuard DNS Filter, AdGuard Base Filter, and AdGuard Annoyances
export const ADGUARD_BLOCKED_DOMAINS = [
  // ── Google AdSense & DoubleClick Ads ──
  'googlesyndication.com',
  'googleadservices.com',
  'doubleclick.net',
  'google.com/pagead',
  'adsbygoogle',
  'adtrafficquality.google',

  // ── Video Ad Networks, Exchanges & Cookie Sync (Connatix, 3Lift, Adform, etc.) ──
  'connatix.com',
  'cds.connatix.com',
  'cd.connatix.com',
  'cks.connatix.com',
  'capi.connatix.com',
  'cnx_usync',
  'adscore.com',
  'c1.adscore.com',
  '3lift.com',
  'eb2.3lift.com',
  'triplelift.net',
  'adform.net',
  'c1.adform.net',
  'bidswitch.net',
  'x.bidswitch.net',
  'bidgx.com',
  'srv.bidgx.com',
  'bidgency.com',
  'criteo.com',
  'criteo.net',
  'ssp-sync.criteo.com',
  'bidr.io',
  'match.prod.bidr.io',
  'doubleverify.com',
  'moatads.com',
  'scorecardresearch.com',
  'sportradarserving.com',
  'spotxchange.com',
  'spotx.tv',
  'teads.tv',
  'springserve.com',
  'aniview.com',
  'primis.tech',
  'vidoomy.com',
  'targetvideo.com',
  'streamrail.net',
  'innity.com',
  'synacor.com',
  'kargo.com',

  // ── Aggressive Popups, Popunders & Redirects (AdGuard Popups / Annoyances) ──
  'popads.net',
  'popcash.net',
  'popmyads.com',
  'propellerads.com',
  'propellerclick.com',
  'exoclick.com',
  'syndication.exoclick.com',
  'trafficjunky.com',
  'syndication.trafficjunky.com',
  'realsrv.com',
  'syndication.realsrv.com',
  'juicyads.com',
  'adcash.com',
  'clickadu.com',
  'monetag.com',
  'hilltopads.net',
  'hilltopads.com',
  'galaksion.com',
  'richpush.co',
  'richads.co',
  'admaven.com',
  'yllix.com',
  'adoperator.com',
  'bidvertiser.com',
  'traffichunt.com',
  'clicksor.com',
  'infolinks.com',
  'revenuehits.com',
  'onclickperformance.com',
  'clksite.com',
  'deloton.com',
  'zeroredirect.com',
  'tsyndicate.com',
  'trafficstars.com',
  'ad-delivery.net',
  'adtrue.com',
  'clickthreat.net',
  'histats.com',
  'bet365.com',
  '1xbet.com',
  'directlink.me',
  'pushnotifications.co',
  'zoneid.info',
  'adsterra.com',
  'ad-score.com',
  'ad-center.com',
  'clickfunnels.com',
  'clickbank.net',
  'popmonetizer.com',
  'trafficfactory.biz',
  'ero-advertising.com',
  'leadbolt.com',
  'adsmol.com',
  'adsupply.com',
  'adzerk.net',
  'advertserve.com',
  'a-ads.com',
  'clarium.host',
  'bidgear.com',
  'adtng.com',
  'plugrush.com',
  'adtouch.me',
  'trafficforce.com',
  'trafficsignals.com',
  'smartbanner.com',
  'onclickalgo.com',
  'clickadilla.com',
  'evadav.com',
  'pushwoosh.com',
  'onesignal.com',

  // ── General Ad Networks, Telemetry & Web Trackers (AdGuard Base & DNS Filter) ──
  'adnxs.com',
  'appnexus.com',
  'cpmstar.com',
  'mgid.com',
  'taboola.com',
  'outbrain.com',
  'pubmatic.com',
  'openx.net',
  'rubiconproject.com',
  'casalemedia.com',
  'contextweb.com',
  'yieldmo.com',
  'smartadserver.com',
  'adpushup.com',
  'adkernel.com',
  'sonobi.com',
  'sovrn.com',
  'lijit.com',
  'ezoic.com',
  'revcontent.com',
  'zedo.com',
  'exponential.com',
  'conversantmedia.com',
  'media.net',
  'sharethrough.com',
  'undertone.com',
  'gumgum.com',
  'inmobi.com',
  'smaato.net',
  'quantserve.com',
  'chartbeat.com',
  'hotjar.com',
  'clarity.ms',
  'adtechus.com',
  'advertising.com',
  'adroll.com',
  'adblade.com',
  'zergnet.com',
  'serving-sys.com',
  'flashtalking.com',
  'sizmek.com',
  'integralads.com',
  'iasds.com',
  'creativecdn.com',

  // ── Anti-DevTools & Anti-Debugging Script Networks ──────────────────────────
  'disable-devtool',
  'disabledevtool',
  'anti-debug',
  'antidebug'
];

// Safely convert any URL argument (primitive string, URL object, TrustedScriptURL, or custom object) to string
export const toUrlString = (url: any): string => {
  if (!url) return '';
  if (typeof url === 'string') return url;
  try {
    if (typeof url.toString === 'function') {
      return url.toString();
    }
  } catch (_) {}
  return String(url || '');
};

// Verify if a URL or resource belongs to legitimate essential Google services (reCAPTCHA, fonts)
export const isGoogleAdSense = (_url: any): boolean => false;

// Check if a URL matches any domain in AdGuard's blocklist
export const isAdGuardBlocked = (url: any): boolean => {
  const lower = toUrlString(url).toLowerCase();
  if (!lower) return false;
  // Whitelist essential fonts, reCAPTCHA, and verified community support/VPN domains
  if (
    lower.includes('recaptcha') ||
    lower.includes('fonts.googleapis') ||
    lower.includes('fonts.gstatic') ||
    lower.includes('buymeacoffee.com') ||
    lower.includes('ko-fi.com') ||
    lower.includes('nordvpn')
  ) {
    return false;
  }

  for (let i = 0; i < ADGUARD_BLOCKED_DOMAINS.length; i++) {
    if (lower.includes(ADGUARD_BLOCKED_DOMAINS[i])) {
      return true;
    }
  }
  return false;
};

// Harmless dummy window returned when ad scripts attempt window.open
const createDummyWindow = () => ({
  closed: true,
  length: 0,
  opener: null,
  parent: typeof window !== 'undefined' ? window : null,
  top: typeof window !== 'undefined' ? window : null,
  window: null,
  name: '',
  location: { href: 'about:blank', replace: () => {}, reload: () => {}, assign: () => {} },
  document: { write: () => {}, open: () => {}, close: () => {} },
  focus: () => {},
  blur: () => {},
  close: () => {},
  postMessage: () => {},
  addEventListener: () => {},
  removeEventListener: () => {},
  dispatchEvent: () => false
});

let isInitialized = false;

export function initAdBlocker(): void {
  if (isInitialized || typeof window === 'undefined') return;
  isInitialized = true;

  // ── 0. Anti-DisableDevTool & Anti-Debugging Neutralizer ─────────────────────
  try {
    const dummyDevTool = {
      isRunning: false,
      isDevToolOpened: () => false,
      config: {},
      ondevtoolopen: () => {},
      close: () => {},
      init: () => {},
      version: '0.3.8'
    };
    try {
      Object.defineProperty(window, 'DisableDevtool', {
        get: () => dummyDevTool,
        set: () => {},
        configurable: false
      });
    } catch (_) {
      (window as any).DisableDevtool = dummyDevTool;
    }

    // Defeat Function("debugger") and constructor("debugger") infinite loops
    const origFunc = window.Function;
    const safeFunc = function (this: any, ...args: any[]) {
      if (args.length > 0) {
        const last = args[args.length - 1];
        if (typeof last === 'string' && last.includes('debugger')) {
          args[args.length - 1] = last.replace(/debugger\s*;?/g, '/* debugger neutralized */');
        }
      }
      return origFunc.apply(this, args);
    };
    safeFunc.prototype = origFunc.prototype;
    window.Function = safeFunc as any;
    Function.prototype.constructor = safeFunc as any;

    // Prevent rogue scripts from spamming console.clear()
    console.clear = () => {};

    // Protect DevTools keyboard shortcuts (F12, Ctrl+Shift+I, Cmd+Option+I, etc.)
    window.addEventListener('keydown', (e: KeyboardEvent) => {
      const key = e.key || '';
      const isF12 = key === 'F12' || e.keyCode === 123;
      const isDevShortcut = (e.ctrlKey || e.metaKey) && (e.shiftKey || e.altKey) && (
        key === 'I' || key === 'i' ||
        key === 'J' || key === 'j' ||
        key === 'C' || key === 'c' ||
        e.keyCode === 73 || e.keyCode === 74 || e.keyCode === 67
      );
      const isViewSource = (e.ctrlKey || e.metaKey) && (key === 'U' || key === 'u' || e.keyCode === 85);

      if (isF12 || isDevShortcut || isViewSource) {
        e.stopImmediatePropagation();
        return true;
      }
    }, true);

    // Protect Right-Click / Context Menu Inspect
    window.addEventListener('contextmenu', (e: MouseEvent) => {
      e.stopImmediatePropagation();
    }, true);
  } catch (_) {}

  // ── 0.1 AdGuard Scriptlet Stubs (Neutralize Ad Network Globals) ──────────────
  try {
    (window as any).adscore = (window as any).adscore || { check: () => {}, init: () => {} };
    (window as any).cnxProxyTask = (window as any).cnxProxyTask || (() => {});
    (window as any).connatix = (window as any).connatix || {};
    (window as any).aclib = (window as any).aclib || { runPop: () => {}, runAutoTag: () => {} };
  } catch (_) {}

  // ── 1. AdGuard Network Request Blocker (fetch & XMLHttpRequest) ──────────────
  try {
    const originalFetch = window.fetch;
    window.fetch = function (input: RequestInfo | URL, _init?: RequestInit): Promise<Response> {
      const url = typeof input === 'string' ? input : (input instanceof Request ? input.url : String(input));
      if (isAdGuardBlocked(url)) {
        console.warn('[AdGuard] Blocked ad fetch request:', url);
        return Promise.resolve(new Response(null, {
          status: 204,
          statusText: 'No Content',
          headers: new Headers({ 'Content-Type': 'text/plain' })
        }));
      }
      return originalFetch.apply(window, arguments as any);
    };
  } catch (err) {
    console.debug('[AdGuard] Fetch interceptor skipped:', err);
  }

  try {
    const originalXHR = window.XMLHttpRequest;
    const originalXHROpen = originalXHR.prototype.open;
    const originalXHRSend = originalXHR.prototype.send;

    originalXHR.prototype.open = function (method: string, url: string | URL, ...rest: any[]) {
      const urlStr = String(url);
      (this as any).__isAdBlocked = isAdGuardBlocked(urlStr);
      (this as any).__adBlockedUrl = urlStr;
      if ((this as any).__isAdBlocked) {
        return;
      }
      return originalXHROpen.apply(this, [method, url, ...rest] as any);
    };

    originalXHR.prototype.send = function (...args: any[]) {
      if ((this as any).__isAdBlocked) {
        console.warn('[AdGuard] Blocked ad XHR request:', (this as any).__adBlockedUrl);
        try {
          Object.defineProperty(this, 'readyState', { value: 4, writable: true });
          Object.defineProperty(this, 'status', { value: 204, writable: true });
          Object.defineProperty(this, 'statusText', { value: 'No Content', writable: true });
          Object.defineProperty(this, 'responseText', { value: '', writable: true });
          Object.defineProperty(this, 'response', { value: '', writable: true });
        } catch (_) {}
        setTimeout(() => {
          this.dispatchEvent(new Event('readystatechange'));
          this.dispatchEvent(new Event('load'));
          this.dispatchEvent(new Event('loadend'));
        }, 0);
        return;
      }
      return originalXHRSend.apply(this, args as any);
    };
  } catch (err) {
    console.debug('[AdGuard] XHR interceptor skipped:', err);
  }

  // ── 2. AdGuard DOM Element Sanitizers (Script & Image Tracking Pixels) ───────
  try {
    const scriptSrcDesc = Object.getOwnPropertyDescriptor(HTMLScriptElement.prototype, 'src');
    if (scriptSrcDesc && scriptSrcDesc.set) {
      const origScriptSet = scriptSrcDesc.set;
      Object.defineProperty(HTMLScriptElement.prototype, 'src', {
        set: function (url: any) {
          if (isAdGuardBlocked(url)) {
            console.warn('[AdGuard] Blocked dynamic ad script injection:', toUrlString(url));
            return origScriptSet.call(this, 'data:text/javascript;charset=utf-8,');
          }
          return origScriptSet.call(this, url);
        },
        get: scriptSrcDesc.get,
        configurable: true,
        enumerable: true
      });
    }
  } catch (_) {}

  try {
    const imageSrcDesc = Object.getOwnPropertyDescriptor(HTMLImageElement.prototype, 'src');
    if (imageSrcDesc && imageSrcDesc.set) {
      const origImageSet = imageSrcDesc.set;
      Object.defineProperty(HTMLImageElement.prototype, 'src', {
        set: function (url: any) {
          if (isAdGuardBlocked(url)) {
            console.warn('[AdGuard] Neutralized ad tracking pixel:', toUrlString(url));
            return origImageSet.call(this, 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7');
          }
          return origImageSet.call(this, url);
        },
        get: imageSrcDesc.get,
        configurable: true,
        enumerable: true
      });
    }
  } catch (_) {}

  // ── 3. Global Window.open Popup Interceptor ──────────────────────────────────
  try {
    const originalOpen = window.open;
    const safeOpen = function (url?: string | URL, target?: string, features?: string) {
      const urlStr = url ? String(url) : '';
      if (
        urlStr.startsWith('magnet:') ||
        urlStr.startsWith('blob:') ||
        urlStr.includes('/api/download') ||
        urlStr.includes('buymeacoffee.com') ||
        urlStr.includes('ko-fi.com') ||
        urlStr.includes('nordvpn')
      ) {
        return originalOpen ? originalOpen.call(window, url as any, target, features) : createDummyWindow();
      }
      console.warn('[AdGuard] Neutralized popup window.open ->', urlStr);
      return createDummyWindow();
    };
    try {
      (safeOpen as any).toString = () => 'function open() { [native code] }';
    } catch (_) {}
    (window as any).open = safeOpen;
  } catch (err) {
    console.debug('[AdGuard] Unable to override window.open:', err);
  }

  // Intercept scareware / fake alert popups
  try {
    const originalAlert = window.alert;
    window.alert = function (msg?: any) {
      const str = String(msg || '').toLowerCase();
      if (str.includes('virus') || str.includes('infected') || str.includes('clean now') || str.includes('update flash') || str.includes('threat detected')) {
        console.warn('[AdGuard] Blocked scareware alert dialog:', msg);
        return;
      }
      return originalAlert ? originalAlert.call(window, msg) : undefined;
    };
  } catch (_) {}

  // ── 4. Intercept Programmatic Anchor Clicks (Ad Popups) ──────────────────────
  try {
    const originalAnchorClick = HTMLAnchorElement.prototype.click;
    HTMLAnchorElement.prototype.click = function () {
      const target = this.getAttribute('target');
      const href = this.getAttribute('href') || '';
      const isAllowed =
        this.hasAttribute('data-allow-popup') ||
        href.includes('buymeacoffee.com') ||
        href.includes('ko-fi.com') ||
        href.includes('nordvpn');

      if (isAllowed) {
        return originalAnchorClick.apply(this, arguments as any);
      }

      if ((target === '_blank' || (!href.startsWith('/') && !href.startsWith(window.location.origin))) && !isAllowed) {
        if (!isGoogleAdSense(href) && (href.startsWith('javascript:') || isAdGuardBlocked(href))) {
          console.warn('[AdGuard] Blocked programmatic ad anchor click to:', href);
          return;
        }
      }
      return originalAnchorClick.apply(this, arguments as any);
    };
  } catch (_) {}

  // ── 5. Capture-Phase Document Click Guard (Stops Click Hijacking) ────────────
  document.addEventListener(
    'click',
    (e: MouseEvent) => {
      let target = e.target as HTMLElement | null;
      while (target && target !== document.body && target !== document.documentElement) {
        if (target.tagName === 'A') {
          const anchor = target as HTMLAnchorElement;
          const href = anchor.getAttribute('href') || '';
          const targetAttr = anchor.getAttribute('target');
          const isAllowed =
            anchor.hasAttribute('data-allow-popup') ||
            href.includes('buymeacoffee.com') ||
            href.includes('ko-fi.com') ||
            href.includes('nordvpn');

          if (isAllowed || isGoogleAdSense(href)) {
            return;
          }

          if (isAdGuardBlocked(href) || (targetAttr === '_blank' && !isAllowed && !href.startsWith('magnet:') && !href.includes('yts.mx'))) {
            e.preventDefault();
            e.stopImmediatePropagation();
            console.warn('[AdGuard] Blocked suspicious ad click ->', href);
            return false;
          }
        }
        target = target.parentElement;
      }
    },
    true
  );

  // ── 6. MutationObserver (Pruning Injected Ad Elements) ───────────────────────
  try {
    const observer = new MutationObserver((mutations) => {
      for (const mutation of mutations) {
        for (let i = 0; i < mutation.addedNodes.length; i++) {
          const node = mutation.addedNodes[i] as HTMLElement;
          if (!node || !node.tagName) continue;

          // Check for dynamically injected ad script tags
          if (node.tagName === 'SCRIPT') {
            const script = node as HTMLScriptElement;
            const src = (script.src || '').toLowerCase();
            if (isGoogleAdSense(src)) continue;
            if (isAdGuardBlocked(src)) {
              console.warn('[AdGuard] Pruned ad script tag:', script.src);
              script.remove();
              continue;
            }
          }

          // Check for invisible popunder iframe overlays
          if (node.tagName === 'IFRAME') {
            const iframe = node as HTMLIFrameElement;
            const iframeId = (iframe.id || '').toLowerCase();
            const iframeName = (iframe.name || '').toLowerCase();
            const iframeSrc = (iframe.src || '').toLowerCase();

            if (
              iframe.classList.contains('fullscreen-iframe') ||
              iframe.classList.contains('stream-iframe') ||
              iframeId.startsWith('aswift_') ||
              iframeId.startsWith('google_ads_') ||
              iframeName.startsWith('aswift_') ||
              iframeName.startsWith('google_ads_') ||
              isGoogleAdSense(iframeSrc) ||
              isGoogleAdSense(iframeId) ||
              isGoogleAdSense(iframeName)
            ) {
              continue;
            }

            const style = window.getComputedStyle(iframe);
            const isZeroSize =
              parseInt(style.width, 10) <= 2 ||
              parseInt(style.height, 10) <= 2 ||
              style.opacity === '0' ||
              style.display === 'none' ||
              style.visibility === 'hidden';

            if (isZeroSize || isAdGuardBlocked(iframeSrc)) {
              console.warn('[AdGuard] Removed hidden popunder iframe:', iframe.src);
              iframe.remove();
              continue;
            }
          }

          // Check for transparent clickjacking overlays
          if (node.tagName === 'DIV' || node.tagName === 'A' || node.tagName === 'SPAN') {
            const el = node as HTMLElement;
            const className = (el.className || '').toString().toLowerCase();
            const id = (el.id || '').toString().toLowerCase();
            const isAppElement =
              className.includes('stream-') ||
              className.includes('player-') ||
              className.includes('modal') ||
              className.includes('trailer') ||
              className.includes('video') ||
              className.includes('nav') ||
              className.includes('header') ||
              id.includes('app') ||
              el.hasAttribute('data-allow-popup');

            if (!isAppElement && !isGoogleAdSense(className) && !isGoogleAdSense(id)) {
              try {
                const style = window.getComputedStyle(el);
                const isFixed = style.position === 'fixed' || style.position === 'absolute';
                const zIndex = parseInt(style.zIndex, 10) || 0;
                const isFullViewport =
                  (parseInt(style.width, 10) >= window.innerWidth * 0.8 && parseInt(style.height, 10) >= window.innerHeight * 0.8) ||
                  (style.top === '0px' && style.bottom === '0px' && style.left === '0px' && style.right === '0px');
                const isTransparent = style.opacity === '0' || style.backgroundColor.includes('rgba(0, 0, 0, 0)') || style.backgroundColor === 'transparent';

                if (isFixed && isFullViewport && isTransparent && zIndex > 100) {
                  console.warn('[AdGuard] Removed transparent clickjacking overlay:', el);
                  el.remove();
                  continue;
                }
              } catch (_) {}
            }
          }
        }
      }
    });

    if (document.documentElement) {
      observer.observe(document.documentElement, { childList: true, subtree: true });
    }
  } catch (err) {
    console.debug('[AdGuard] MutationObserver skipped:', err);
  }

  // ── 7. AdGuard Cosmetic Filter (CSS Element Hiding) ─────────────────────────
  try {
    const styleEl = document.createElement('style');
    styleEl.id = 'adguard-cosmetic-filter';
    styleEl.textContent = `
      [class*="popunder"],
      [id*="popunder"],
      [class*="popup-ad"],
      [id*="popup-ad"],
      [class*="ad-banner"],
      .adsbygoogle,
      [data-ad-client],
      [class*="ad-overlay"]:not(.player-click-shield) {
        display: none !important;
        visibility: hidden !important;
        pointer-events: none !important;
      }
    `;
    (document.head || document.documentElement).appendChild(styleEl);
  } catch (_) {}

  // ── 8. Anti-Frame Busting & Tab Focus Guard ─────────────────────────────────
  try {
    let isUserNavigating = false;
    document.addEventListener('click', (e) => {
      let t = e.target as HTMLElement | null;
      while (t && t !== document.body) {
        if (t.tagName === 'A' && (t as HTMLAnchorElement).href.startsWith(window.location.origin)) {
          isUserNavigating = true;
          setTimeout(() => { isUserNavigating = false; }, 1000);
          break;
        }
        t = t.parentElement;
      }
    }, true);

    window.addEventListener('beforeunload', (e) => {
      const hasPlayer = document.querySelector('.stream-iframe, .fullscreen-iframe');
      if (hasPlayer && !isUserNavigating) {
        e.preventDefault();
        e.returnValue = '';
        return '';
      }
    });

    window.addEventListener('blur', () => {
      const hasPlayer = document.querySelector('.stream-iframe, .fullscreen-iframe');
      if (hasPlayer && document.activeElement && document.activeElement.tagName === 'IFRAME') {
        setTimeout(() => {
          try { window.focus(); } catch (_) {}
        }, 50);
      }
    });
  } catch (_) {}

  console.info('[AdGuard] Engine Initialized: Filter lists, network interceptors, and popup protections active.');
}
