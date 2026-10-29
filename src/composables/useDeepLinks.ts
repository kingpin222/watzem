import { ref } from 'vue';
import { Capacitor } from '@capacitor/core';
import { App as CapApp, URLOpenListenerEvent } from '@capacitor/app';
import { Share } from '@capacitor/share';
import { Clipboard } from '@capacitor/clipboard';
import { MediaItem, MediaMode } from '../types';
import { useToast } from './useToast';
import { useHaptics } from './useHaptics';
import { useApi } from './useApi';

export interface DeepLinkTarget {
  type: 'movie' | 'tv';
  id: number;
}

export const APP_DOWNLOAD_URL = 'https://github.com/kingpin222/watzem/releases/latest/download/app-release.apk';
export const APP_WEB_BASE = 'https://watzem.app';

export function getBaseUrl(): string {
  if (typeof window !== 'undefined' && window.location.hostname && !window.location.hostname.includes('localhost') && !window.location.hostname.includes('127.0.0.1')) {
    return window.location.origin;
  }
  return APP_WEB_BASE;
}

export function parseDeepLink(urlOrPath: string): DeepLinkTarget | null {
  if (!urlOrPath) return null;

  try {
    let normalized = urlOrPath.trim();

    // 1. Handle custom scheme: watzem://movie/123 or watzem://tv/123
    if (normalized.startsWith('watzem://')) {
      const rest = normalized.replace(/^watzem:\/\//, '');
      const [pathPart, queryPart] = rest.split('?');
      const segments = pathPart.split('/').filter(Boolean);

      if (segments.length >= 2) {
        const type = segments[0].toLowerCase();
        const id = parseInt(segments[1], 10);
        if ((type === 'movie' || type === 'tv') && !isNaN(id) && id > 0) {
          return { type, id };
        }
      }

      if (queryPart) {
        const params = new URLSearchParams(queryPart);
        const movieId = params.get('movie');
        if (movieId && parseInt(movieId, 10) > 0) {
          return { type: 'movie', id: parseInt(movieId, 10) };
        }
        const tvId = params.get('tv');
        if (tvId && parseInt(tvId, 10) > 0) {
          return { type: 'tv', id: parseInt(tvId, 10) };
        }
        const id = params.get('id');
        const type = params.get('type') === 'tv' ? 'tv' : 'movie';
        if (id && parseInt(id, 10) > 0) {
          return { type, id: parseInt(id, 10) };
        }
      }

      if (segments.length === 1 && !isNaN(parseInt(segments[0], 10))) {
        return { type: 'movie', id: parseInt(segments[0], 10) };
      }
    }

    // 2. Relative paths or queries: /movie/123 or ?movie=123
    if (normalized.startsWith('/') || normalized.startsWith('?')) {
      normalized = `${APP_WEB_BASE}${normalized.startsWith('/') ? '' : '/'}${normalized}`;
    }

    // 3. Full HTTP/HTTPS URLs
    const url = new URL(normalized);

    // A. Query parameters: ?movie=123 or ?tv=123 or ?id=123&type=movie
    const movieParam = url.searchParams.get('movie');
    if (movieParam && parseInt(movieParam, 10) > 0) {
      return { type: 'movie', id: parseInt(movieParam, 10) };
    }
    const tvParam = url.searchParams.get('tv');
    if (tvParam && parseInt(tvParam, 10) > 0) {
      return { type: 'tv', id: parseInt(tvParam, 10) };
    }
    const idParam = url.searchParams.get('id');
    if (idParam && parseInt(idParam, 10) > 0) {
      const type = url.searchParams.get('type') === 'tv' ? 'tv' : 'movie';
      return { type, id: parseInt(idParam, 10) };
    }

    // B. Path segments: /movie/123 or /tv/123
    const pathSegments = url.pathname.split('/').filter(Boolean);
    if (pathSegments.length >= 2) {
      const type = pathSegments[0].toLowerCase();
      const id = parseInt(pathSegments[1], 10);
      if ((type === 'movie' || type === 'tv') && !isNaN(id) && id > 0) {
        return { type, id };
      }
    }

    // C. Hash router links: #/movie/123
    if (url.hash) {
      const hashClean = url.hash.replace(/^#\/?/, '');
      const hashSegments = hashClean.split('/').filter(Boolean);
      if (hashSegments.length >= 2) {
        const type = hashSegments[0].toLowerCase();
        const id = parseInt(hashSegments[1], 10);
        if ((type === 'movie' || type === 'tv') && !isNaN(id) && id > 0) {
          return { type, id };
        }
      }
    }
  } catch (err) {
    console.warn('[useDeepLinks] Failed to parse URL as deeplink:', urlOrPath, err);
  }

  return null;
}

export function useDeepLinks() {
  const { showToast } = useToast();
  const { triggerLight, triggerSuccess } = useHaptics();
  const { getFullDetails } = useApi();
  const isResolvingDeepLink = ref(false);

  /**
   * Generic share function using Capacitor Share with Web and Clipboard fallbacks
   */
  const shareContent = async (options: {
    title: string;
    text: string;
    url: string;
    dialogTitle?: string;
    copySuccessMessage?: string;
  }): Promise<boolean> => {
    // 1. Try Native Capacitor Share Dialog (Android intent chooser)
    try {
      const canShareResult = await Share.canShare().catch(() => ({ value: false }));
      if (canShareResult?.value || typeof (navigator as any)?.share === 'function') {
        await Share.share({
          title: options.title,
          text: options.text,
          url: options.url,
          dialogTitle: options.dialogTitle || options.title
        });
        triggerLight();
        return true;
      }
    } catch (err: any) {
      if (err?.name === 'AbortError' || String(err?.message || '').toLowerCase().includes('canceled')) {
        return false;
      }
      console.warn('[useDeepLinks] Share failed, trying clipboard fallback:', err);
    }

    // 2. Fallback: Copy link to clipboard
    try {
      const copyText = options.text ? `${options.text}\n${options.url}`.trim() : options.url;
      await Clipboard.write({ string: copyText });
      triggerSuccess();
      showToast({
        title: 'Link Copied!',
        message: options.copySuccessMessage || 'Link copied to clipboard.',
        type: 'success',
        duration: 3500
      });
      return true;
    } catch (err) {
      if (navigator?.clipboard?.writeText) {
        const copyText = options.text ? `${options.text}\n${options.url}`.trim() : options.url;
        await navigator.clipboard.writeText(copyText);
        triggerSuccess();
        showToast({
          title: 'Link Copied!',
          message: options.copySuccessMessage || 'Link copied to clipboard.',
          type: 'success',
          duration: 3500
        });
        return true;
      }
    }

    return false;
  };

  /**
   * Share the WATZEM App with friends
   */
  const shareApp = async (): Promise<boolean> => {
    return shareContent({
      title: 'WATZEM - Free Movies & TV Series Stream',
      text: 'Watch thousands of movies and TV series for free in HD with zero popup ads on WATZEM!',
      url: APP_DOWNLOAD_URL,
      dialogTitle: 'Share WATZEM App',
      copySuccessMessage: 'App download link copied to clipboard!'
    });
  };

  /**
   * Copy app link to clipboard directly
   */
  const copyAppLink = async (): Promise<boolean> => {
    try {
      await Clipboard.write({ string: APP_DOWNLOAD_URL });
      triggerSuccess();
      showToast({
        title: 'App Link Copied!',
        message: 'Direct APK download link copied to clipboard.',
        type: 'success',
        duration: 3500
      });
      return true;
    } catch {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(APP_DOWNLOAD_URL);
        triggerSuccess();
        showToast({
          title: 'App Link Copied!',
          message: 'Direct APK download link copied to clipboard.',
          type: 'success',
          duration: 3500
        });
        return true;
      }
    }
    return false;
  };

  /**
   * Generate deeplink URLs for a specific movie or TV show (watzem:// custom scheme)
   */
  const getMediaLinks = (item: MediaItem, mode: MediaMode = 'movie') => {
    const type = item.media_type === 'tv' || mode === 'tv' ? 'tv' : 'movie';
    const appSchemeUrl = `watzem://${type}/${item.id}`;
    const base = getBaseUrl();
    const webUrl = `${base}/${type}/${item.id}`;

    return {
      appSchemeUrl,
      webUrl,
      shareUrl: appSchemeUrl
    };
  };

  /**
   * Share a specific Movie or TV show with deeplink
   */
  const shareMedia = async (item: MediaItem, mode: MediaMode = 'movie'): Promise<boolean> => {
    const title = item.title || item.name || 'Title';
    const year = (item.release_date || item.first_air_date || '').slice(0, 4);
    const yearStr = year ? ` (${year})` : '';
    const { appSchemeUrl, webUrl } = getMediaLinks(item, mode);

    return shareContent({
      title: `${title}${yearStr} on WATZEM`,
      text: `🎬 Watch "${title}"${yearStr} on WATZEM — 100% Free with zero ads!\n\n📱 Open in App: ${appSchemeUrl}\n🌐 Web Link: ${webUrl}\n⬇️ Download App:`,
      url: APP_DOWNLOAD_URL,
      dialogTitle: `Share "${title}"`,
      copySuccessMessage: `"${title}" deeplink (${appSchemeUrl}) copied to clipboard!`
    });
  };

  /**
   * Copy specific Movie or TV deeplink to clipboard
   */
  const copyMediaLink = async (item: MediaItem, mode: MediaMode = 'movie'): Promise<boolean> => {
    const title = item.title || item.name || 'Title';
    const { appSchemeUrl } = getMediaLinks(item, mode);

    try {
      await Clipboard.write({ string: appSchemeUrl });
      triggerSuccess();
      showToast({
        title: `"${title}" Link Copied!`,
        message: `${appSchemeUrl} copied to clipboard.`,
        type: 'success',
        duration: 3500
      });
      return true;
    } catch {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(appSchemeUrl);
        triggerSuccess();
        showToast({
          title: `"${title}" Link Copied!`,
          message: `${appSchemeUrl} copied to clipboard.`,
          type: 'success',
          duration: 3500
        });
        return true;
      }
    }
    return false;
  };

  /**
   * Resolve an incoming deeplink target and load full movie/tv details
   */
  const resolveTarget = async (
    target: DeepLinkTarget,
    onOpenMedia: (item: MediaItem) => void
  ): Promise<boolean> => {
    isResolvingDeepLink.value = true;
    try {
      const details = await getFullDetails(target.id, target.type);
      if (details && details.id) {
        const item: MediaItem = {
          id: details.id,
          title: details.title || (details as any).name,
          name: (details as any).name || details.title,
          overview: details.overview || '',
          poster_path: details.poster_path,
          backdrop_path: details.backdrop_path,
          vote_average: details.vote_average || 0,
          vote_count: details.vote_count,
          release_date: details.release_date,
          first_air_date: details.first_air_date,
          media_type: target.type
        };

        onOpenMedia(item);
        showToast({
          title: details.title || (details as any).name || 'Media Opened',
          message: 'Loaded via deeplink',
          type: 'info',
          duration: 2500
        });
        return true;
      }
    } catch (err) {
      console.error('[useDeepLinks] Failed to load media details for deeplink:', target, err);
      showToast({
        title: 'Deeplink Error',
        message: 'Could not load the requested title.',
        type: 'error',
        duration: 4000
      });
    } finally {
      isResolvingDeepLink.value = false;
    }
    return false;
  };

  /**
   * Listen for incoming deeplinks from native intent and web URL
   */
  const initDeepLinks = (onOpenMedia: (item: MediaItem) => void) => {
    const handleUrl = (url: string) => {
      const target = parseDeepLink(url);
      if (target) {
        resolveTarget(target, onOpenMedia);
      }
    };

    // 1. Cold start launch URL on native Android / iOS
    if (Capacitor.isNativePlatform()) {
      try {
        CapApp.getLaunchUrl().then((launch) => {
          if (launch?.url) {
            handleUrl(launch.url);
          }
        });

        CapApp.addListener('appUrlOpen', (event: URLOpenListenerEvent) => {
          if (event?.url) {
            handleUrl(event.url);
          }
        });
      } catch (err) {
        console.warn('[useDeepLinks] CapApp listener error:', err);
      }
    }

    // 2. Web browser: URL check on mount
    if (typeof window !== 'undefined') {
      const initialUrl = window.location.href;
      const target = parseDeepLink(initialUrl);
      if (target) {
        resolveTarget(target, onOpenMedia);
      }

      window.addEventListener('popstate', () => {
        const popTarget = parseDeepLink(window.location.href);
        if (popTarget) {
          resolveTarget(popTarget, onOpenMedia);
        }
      });
    }
  };

  return {
    shareApp,
    copyAppLink,
    shareMedia,
    copyMediaLink,
    getMediaLinks,
    parseDeepLink,
    resolveTarget,
    initDeepLinks,
    isResolvingDeepLink,
    APP_DOWNLOAD_URL
  };
}
