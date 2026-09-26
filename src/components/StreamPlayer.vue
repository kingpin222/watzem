<script lang="ts">
import { registerPlugin } from '@capacitor/core';

// Module-scoped plugin registration to ensure SystemBars and PipPlugin are registered only once
let SystemBars: any = null;
let PipPlugin: any = null;
try {
  SystemBars = registerPlugin<any>('SystemBars');
} catch {
  SystemBars = (window as any)?.Capacitor?.Plugins?.SystemBars;
}
try {
  PipPlugin = registerPlugin<any>('PipPlugin');
} catch {
  PipPlugin = (window as any)?.Capacitor?.Plugins?.PipPlugin;
}

// Multi-server automatic fallback architecture with verified active mirrors
export interface StreamServerDef {
  id: string;
  name: string;
  shortName: string;
  badge: string;
  getUrl: (id: string | number, isTv: boolean, season: number, episode: number) => string;
}
</script>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { MediaItem, MediaMode, TVSeason, TVEpisode } from '../types';
import { useApi } from '../composables/useApi';
import { useHaptics } from '../composables/useHaptics';
import { ScreenOrientation } from '@capacitor/screen-orientation';
import { StatusBar } from '@capacitor/status-bar';
import { NavigationBar } from '@hugotomazi/capacitor-navigation-bar';
import {
  Play,
  X,
  Server,
  RotateCw,
  Check,
  Maximize,
  Tv,
  PictureInPicture,
  AlertCircle
} from 'lucide-vue-next';

const props = withDefaults(
  defineProps<{
    item: MediaItem;
    mode: MediaMode;
    autoPlay?: boolean;
    initialSeason?: number;
    initialEpisode?: number;
    showInlineServer?: boolean;
  }>(),
  {
    autoPlay: true,
    initialSeason: 1,
    initialEpisode: 1,
    showInlineServer: false
  }
);

const { getImageUrl, getTvDetails, getSeasonDetails, getMediaImages, getMediaVideos, probeStreamServers } = useApi();
const { triggerLight, triggerMedium } = useHaptics();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'episode-change', payload: { season: number; episode: number }): void;
}>();

const isPlaying = ref(false);
const isFullscreen = ref(false);
const isClosing = ref(false); // true while player exit animation is running

const seasons = ref<TVSeason[]>([]);
const currentSeason = ref<number>(1);
const episodes = ref<TVEpisode[]>([]);
const currentEpisode = ref<number>(1);
const loadingTvData = ref(false);
const showEpisodesMenu = ref(false);

const isTv = computed(() => {
  return (
    props.mode === 'tv' ||
    props.item.media_type === 'tv' ||
    Boolean(props.item.first_air_date && !props.item.release_date) ||
    Boolean(props.item.name && !props.item.title) ||
    Boolean((props.item as any).number_of_seasons)
  );
});
const mediaTitle = computed(() => props.item.title || props.item.name || 'Now Playing');

// Use the cinematic backdrop image behind the play button (swapped with top banner)
const imageLoadError = ref(false);

const playerImageUrl = computed(() => {
  if (imageLoadError.value && props.item.poster_path) {
    return getImageUrl(props.item.poster_path, 'w500');
  }
  const path = props.item.backdrop_path || props.item.poster_path;
  return getImageUrl(path, 'w780');
});

const handlePlayerImageError = (e: Event) => {
  const img = e.target as HTMLImageElement;
  if (!img) return;
  if (!imageLoadError.value && props.item.poster_path) {
    imageLoadError.value = true;
    img.src = getImageUrl(props.item.poster_path, 'w500');
  } else if (!img.dataset.triedFallback) {
    img.dataset.triedFallback = 'true';
    img.src = 'https://image.tmdb.org/t/p/w780/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg';
  }
};

// Interactive pointer and scroll parallax tracking
const placeholderRef = ref<HTMLElement | null>(null);
const mouseParallaxX = ref(0);
const mouseParallaxY = ref(0);
const scrollParallaxY = ref(0);
const isHovering = ref(false);
let scrollCleanup: (() => void) | null = null;

const handleMouseMove = (e: MouseEvent) => {
  if (!placeholderRef.value) return;
  const rect = placeholderRef.value.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  const centerY = rect.top + rect.height / 2;
  const normX = (e.clientX - centerX) / (rect.width / 2);
  const normY = (e.clientY - centerY) / (rect.height / 2);
  mouseParallaxX.value = Math.max(-1, Math.min(1, normX)) * 16;
  mouseParallaxY.value = Math.max(-1, Math.min(1, normY)) * 12;
};

const handleMouseEnter = () => {
  isHovering.value = true;
};

const handleMouseLeave = () => {
  isHovering.value = false;
  mouseParallaxX.value = 0;
  mouseParallaxY.value = 0;
};

const parallaxStyle = computed(() => {
  const x = mouseParallaxX.value;
  const y = mouseParallaxY.value + scrollParallaxY.value;
  const scale = isHovering.value ? 1.14 : 1.06;
  return {
    transform: `translate3d(${x}px, ${y}px, 0) scale(${scale})`
  };
});

// ── IMDB Photo Slideshow ─────────────────────────────────────────────────────
const slideImages = ref<string[]>([]);
const slideIndex = ref(0);
let slideTimer: ReturnType<typeof setInterval> | null = null;

const startSlideshow = () => {
  if (slideTimer) clearInterval(slideTimer);
  if (slideImages.value.length < 2) return;
  slideTimer = setInterval(() => {
    slideIndex.value = (slideIndex.value + 1) % slideImages.value.length;
  }, 4000);
};

const stopSlideshow = () => {
  if (slideTimer) { clearInterval(slideTimer); slideTimer = null; }
};

const loadSlideImages = async () => {
  try {
    const imgs = await getMediaImages(props.item.id, isTv.value ? 'tv' : 'movie');
    if (imgs.length > 0) {
      // Seed with the item's own backdrop first so something shows instantly
      const seed = getImageUrl(props.item.backdrop_path || props.item.poster_path, 'w1280');
      const deduped = [seed, ...imgs.filter(u => u !== seed)].filter(Boolean);
      slideImages.value = deduped.slice(0, 12);
    } else {
      slideImages.value = [
        getImageUrl(props.item.backdrop_path || props.item.poster_path, 'w1280')
      ].filter(Boolean);
    }
    slideIndex.value = 0;
    startSlideshow();
  } catch {
    // fallback: just use the existing backdrop
    slideImages.value = [
      getImageUrl(props.item.backdrop_path || props.item.poster_path, 'w780')
    ].filter(Boolean);
  }
};

// Current slide URL (falls back to playerImageUrl while fetching)
const currentSlideUrl = computed(() =>
  slideImages.value[slideIndex.value] || playerImageUrl.value
);

// ── Trailer ──────────────────────────────────────────────────────────────────
const trailerKey = ref<string | null>(null);
const showTrailer = ref(false);

const loadTrailer = async () => {
  try {
    const videos = await getMediaVideos(props.item.id, isTv.value ? 'tv' : 'movie');
    // Priority: Official Trailer > any Trailer > Teaser
    const pick =
      videos.find(v => v.type === 'Trailer' && v.name.toLowerCase().includes('official')) ||
      videos.find(v => v.type === 'Trailer') ||
      videos.find(v => v.type === 'Teaser') ||
      videos[0] ||
      null;
    trailerKey.value = pick ? pick.key : null;
  } catch {
    trailerKey.value = null;
  }
};

const openTrailer = () => {
  if (trailerKey.value) showTrailer.value = true;
};

const closeTrailer = () => {
  showTrailer.value = false;
};

const trailerUrl = computed(() =>
  trailerKey.value
    ? `https://www.youtube.com/embed/${trailerKey.value}?autoplay=1&rel=0&modestbranding=1`
    : ''
);

const SERVERS: StreamServerDef[] = [
  {
    id: 'server10',
    name: 'Server 1',
    shortName: 'VidEasy',
    badge: 'Fast VIP',
    getUrl: (id, isTv, season, episode) => {
      const s = season || 1;
      const e = episode || 1;
      return isTv
        ? `https://player.videasy.to/tv/${id}/${s}/${e}`
        : `https://player.videasy.to/movie/${id}`;
    }
  },
  {
    id: 'server3',
    name: 'Server 2',
    shortName: 'Server 2',
    badge: 'VIP Clean',
    getUrl: (id, isTv, season, episode) => {
      return isTv
        ? `https://player.autoembed.co/embed/tv/${id}/${season}/${episode}?autoplay=1`
        : `https://player.autoembed.co/embed/movie/${id}?autoplay=1`;
    }
  },
  {
    id: 'server1',
    name: 'Server 3',
    shortName: 'Server 3',
    badge: 'HD Mirror',
    getUrl: (id, isTv, season, episode) => {
      const params = new URLSearchParams({
        primaryColor: '63b8bc',
        secondaryColor: 'a2a2a2',
        iconColor: 'eefdec',
        icons: 'vid',
        player: 'jw',
        title: 'false',
        poster: 'false',
        autoplay: 'true'
      });
      return isTv
        ? `https://vidlink.pro/tv/${id}/${season}/${episode}?${params.toString()}`
        : `https://vidlink.pro/movie/${id}?${params.toString()}`;
    }
  },
  {
    id: 'server2',
    name: 'Server 4',
    shortName: 'Server 4',
    badge: 'Fast Stream',
    getUrl: (id, isTv, season, episode) => {
      return isTv
        ? `https://vidsrc.pm/embed/tv/${id}/${season}/${episode}?autoplay=1`
        : `https://vidsrc.pm/embed/movie/${id}?autoplay=1`;
    }
  },
  {
    id: 'server5',
    name: 'Server 5',
    shortName: 'Server 5',
    badge: 'Alternative',
    getUrl: (id, isTv, season, episode) => {
      return isTv
        ? `https://vidsrc.io/embed/tv/${id}/${season}/${episode}?autoplay=1`
        : `https://vidsrc.io/embed/movie/${id}?autoplay=1`;
    }
  },
  {
    id: 'server6',
    name: 'Server 6',
    shortName: 'Server 6',
    badge: 'Multi-Source',
    getUrl: (id, isTv, season, episode) => {
      return isTv
        ? `https://multiembed.mov/?video_id=${id}&tmdb=1&s=${season}&e=${episode}&autoplay=1`
        : `https://multiembed.mov/?video_id=${id}&tmdb=1&autoplay=1`;
    }
  },
  {
    id: 'server8',
    name: 'Server 7',
    shortName: 'CineSrc',
    badge: 'VIP Auto-Skip',
    getUrl: (id, isTv, season, episode) => {
      const s = season || 1;
      const e = episode || 1;
      const params = new URLSearchParams({
        autoplay: 'true',
        color: '#f59e0b',
        autonext: 'true',
        autoskip: 'true'
      });
      return isTv
        ? `https://cinesrc.st/embed/tv/${id}?s=${s}&e=${e}&${params.toString()}`
        : `https://cinesrc.st/embed/movie/${id}?${params.toString()}`;
    }
  }
];

const activeServerIndex = ref(0);
const isProbingServers = ref(false);
const serverLatencies = ref<Record<number, number>>({});
const serverStatusMap = ref<Record<number, boolean>>({});
const userManuallySelectedServer = ref(false);
const recommendedServerIndex = ref<number | null>(null);

// Automated server availability probe on mount / item change:
// Tests candidate servers concurrently and sets the verified working server as default
const probeAndSetDefaultServer = async () => {
  if (!props.item || !props.item.id) return;
  isProbingServers.value = true;
  try {
    const s = currentSeason.value || 1;
    const e = currentEpisode.value || 1;
    const result = await probeStreamServers(props.item.id, isTv.value ? 'tv' : 'movie', s, e);

    if (result && Array.isArray(result.servers)) {
      result.servers.forEach((srv) => {
        serverLatencies.value[srv.index] = srv.latency;
        serverStatusMap.value[srv.index] = srv.ok;
      });
    }

    if (typeof result.bestServerIndex === 'number' && result.bestServerIndex >= 0 && result.bestServerIndex < SERVERS.length) {
      recommendedServerIndex.value = result.bestServerIndex;
      // If user has not manually switched server during this session, use the verified server as default
      if (!userManuallySelectedServer.value) {
        activeServerIndex.value = result.bestServerIndex;
        console.log(`[StreamPlayer] Tested and set verified default server: ${SERVERS[result.bestServerIndex].name} (${result.bestServerName})`);
      }
    }
  } catch (err) {
    console.warn('[StreamPlayer] Server probe error:', err);
  } finally {
    isProbingServers.value = false;
  }
};

const fallbackNotice = ref('');
let fallbackNoticeTimer: any = null;
let connectionWatchdogTimer: any = null;
let streamGraceTimer: any = null;

const isStreamLoading = ref(false);
const isIframeLoaded = ref(false);
const isStreamHealthy = ref(false);
const showServerMenu = ref(false);

// Auto-hide player buttons on inactivity (3.5s timeout)
const showControls = ref(true);
let controlsInactivityTimer: any = null;

const resetControlsTimeout = () => {
  showControls.value = true;
  if (controlsInactivityTimer) {
    clearTimeout(controlsInactivityTimer);
    controlsInactivityTimer = null;
  }
  // Keep controls visible while server or episode menu dropdown is open
  if (showServerMenu.value || showEpisodesMenu.value) {
    return;
  }
  if (isPlaying.value) {
    controlsInactivityTimer = setTimeout(() => {
      if (!showServerMenu.value && !showEpisodesMenu.value) {
        showControls.value = false;
      }
    }, 3500);
  }
};

const handleUserActivity = () => {
  resetControlsTimeout();
};

const toggleControls = () => {
  if (!showControls.value) {
    resetControlsTimeout();
  } else {
    // If a menu is open, clicking empty background closes it first
    if (showServerMenu.value || showEpisodesMenu.value) {
      showServerMenu.value = false;
      showEpisodesMenu.value = false;
      resetControlsTimeout();
    } else {
      showControls.value = false;
      if (controlsInactivityTimer) {
        clearTimeout(controlsInactivityTimer);
        controlsInactivityTimer = null;
      }
    }
  }
};

// Keep controls visible while menus are open, resume countdown on close
watch([showServerMenu, showEpisodesMenu], ([serverOpen, epOpen]) => {
  if (serverOpen || epOpen) {
    showControls.value = true;
    if (controlsInactivityTimer) {
      clearTimeout(controlsInactivityTimer);
      controlsInactivityTimer = null;
    }
  } else {
    resetControlsTimeout();
  }
});

// Smart failure memory & exhaustion tracking
const failedServerIndices = ref<Set<number>>(new Set());
const allServersExhausted = ref(false);

const activeServer = computed(() => SERVERS[activeServerIndex.value] || SERVERS[0]);

const showFallbackNotice = (msg: string) => {
  fallbackNotice.value = msg;
  resetControlsTimeout();
  if (fallbackNoticeTimer) clearTimeout(fallbackNoticeTimer);
  fallbackNoticeTimer = setTimeout(() => {
    fallbackNotice.value = '';
  }, 4500);
};

const clearWatchdog = () => {
  if (connectionWatchdogTimer) {
    clearTimeout(connectionWatchdogTimer);
    connectionWatchdogTimer = null;
  }
  if (streamGraceTimer) {
    clearTimeout(streamGraceTimer);
    streamGraceTimer = null;
  }
  isStreamLoading.value = false;
};

/**
 * Intelligent next server selection:
 * 1. Excludes servers that already failed in the current session
 * 2. Prioritizes candidate mirrors verified online via probe (sorted by lowest latency)
 * 3. Falls back to next untried server in natural order
 * 4. Returns null when all 7 servers have been exhausted
 */
const getNextBestServerIndex = (currentIndex: number): number | null => {
  const untriedIndices: number[] = [];
  for (let i = 0; i < SERVERS.length; i++) {
    if (!failedServerIndices.value.has(i)) {
      untriedIndices.push(i);
    }
  }

  if (untriedIndices.length === 0) {
    return null;
  }

  // Find untried servers that were verified online by probe
  const verifiedOnline = untriedIndices.filter(
    (idx) => serverStatusMap.value[idx] === true
  );

  if (verifiedOnline.length > 0) {
    verifiedOnline.sort((a, b) => {
      const latA = serverLatencies.value[a] ?? 9999;
      const latB = serverLatencies.value[b] ?? 9999;
      return latA - latB;
    });
    return verifiedOnline[0];
  }

  // Otherwise pick next available untried server
  const nextInSequence = untriedIndices.find((idx) => idx > currentIndex);
  if (nextInSequence !== undefined) {
    return nextInSequence;
  }

  return untriedIndices[0];
};

/**
 * Smart automatic fallback:
 * Switches to the next verified/untried server or enters exhausted state
 */
const triggerNextFallback = (reason = 'Server unresponsive') => {
  clearWatchdog();
  const prevIdx = activeServerIndex.value;
  const prevName = SERVERS[prevIdx]?.name || `Server ${prevIdx + 1}`;
  failedServerIndices.value.add(prevIdx);

  const nextIdx = getNextBestServerIndex(prevIdx);

  if (nextIdx === null) {
    allServersExhausted.value = true;
    showFallbackNotice(`All ${SERVERS.length} servers attempted • Select manually or retry`);
    console.warn(`[StreamPlayer] All servers exhausted for this item (${reason}).`);
    return;
  }

  allServersExhausted.value = false;
  activeServerIndex.value = nextIdx;
  const nextName = SERVERS[nextIdx].name;
  showFallbackNotice(`Auto-switched to ${nextName} (${prevName}: ${reason})`);
  console.info(`[StreamPlayer] Auto-switched from ${prevName} to ${nextName} (${reason})`);

  if (isPlaying.value) {
    startWatchdog();
  }
};

/**
 * Reset all failed server states and restart from Server 1
 */
const retryAllServers = () => {
  failedServerIndices.value.clear();
  allServersExhausted.value = false;
  userManuallySelectedServer.value = false;
  activeServerIndex.value = 0;
  showFallbackNotice(`Retrying ${SERVERS[0].name}...`);
  if (isPlaying.value) {
    startWatchdog();
  }
};

const selectServer = (idx: number) => {
  if (idx < 0 || idx >= SERVERS.length) return;
  userManuallySelectedServer.value = true;
  failedServerIndices.value.delete(idx);
  allServersExhausted.value = false;
  clearWatchdog();
  activeServerIndex.value = idx;
  showServerMenu.value = false;
  showFallbackNotice(`Selected ${SERVERS[idx].name}`);
  triggerLight();
  if (isPlaying.value) {
    startWatchdog();
  }
};

const nextServerManual = () => {
  triggerMedium();
  const nextIdx = (activeServerIndex.value + 1) % SERVERS.length;
  selectServer(nextIdx);
};

const markStreamHealthy = () => {
  isStreamHealthy.value = true;
  isStreamLoading.value = false;
  if (connectionWatchdogTimer) {
    clearTimeout(connectionWatchdogTimer);
    connectionWatchdogTimer = null;
  }
  if (streamGraceTimer) {
    clearTimeout(streamGraceTimer);
    streamGraceTimer = null;
  }
  if (serverStatusMap.value[activeServerIndex.value] === undefined) {
    serverStatusMap.value[activeServerIndex.value] = true;
  }
};

/**
 * Smart Two-Phase Watchdog:
 * Phase 1: 10s window to establish connection & receive HTML frame load
 * Phase 2: 7s post-load buffer grace period for media playback signals
 */
const startWatchdog = () => {
  clearWatchdog();
  if (!isPlaying.value) return;

  isStreamLoading.value = true;
  isIframeLoaded.value = false;
  isStreamHealthy.value = false;
  const currentIndex = activeServerIndex.value;

  // Phase 1: Connection timeout
  connectionWatchdogTimer = setTimeout(() => {
    if (activeServerIndex.value === currentIndex && isPlaying.value && !isIframeLoaded.value) {
      console.warn(`[StreamPlayer] Connection timeout (10s) on ${SERVERS[currentIndex].name}`);
      triggerNextFallback('Connection timed out (10s)');
    }
  }, 10000);
};

const handleIframeLoad = () => {
  isIframeLoaded.value = true;
  if (connectionWatchdogTimer) {
    clearTimeout(connectionWatchdogTimer);
    connectionWatchdogTimer = null;
  }

  const currentIndex = activeServerIndex.value;

  // Phase 2: Post-load buffer grace period (7 seconds)
  if (streamGraceTimer) clearTimeout(streamGraceTimer);
  streamGraceTimer = setTimeout(() => {
    if (
      activeServerIndex.value === currentIndex &&
      isPlaying.value &&
      !isStreamHealthy.value &&
      isStreamLoading.value
    ) {
      console.warn(`[StreamPlayer] Stream buffering stalled on ${SERVERS[currentIndex].name}`);
      triggerNextFallback('Stream buffering stalled');
    } else {
      isStreamLoading.value = false;
    }
  }, 7000);
};

const handleIframeError = () => {
  triggerNextFallback('Stream failed to load');
};

const handleWindowMessage = (event: MessageEvent) => {
  try {
    const data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
    if (!data) return;

    // CineSrc specific integration
    if (event.origin && event.origin.includes('cinesrc.st')) {
      if (data.type === 'cinesrc:nextepisode' && data.season && data.episode) {
        currentSeason.value = Number(data.season);
        currentEpisode.value = Number(data.episode);
        emit('episode-change', { season: currentSeason.value, episode: currentEpisode.value });
        return;
      }
      if (data.type === 'cinesrc:close') {
        stopAndClose();
        return;
      }
      if (data.type === 'cinesrc:error') {
        triggerNextFallback('CineSrc playback error');
        return;
      }
      if (data.type === 'cinesrc:ready' || data.type === 'cinesrc:playing') {
        markStreamHealthy();
        return;
      }
    }

    const eventType = String(data.type || data.event || data.status || '').toLowerCase();

    // Positive playback signals (stream confirmed playing & working)
    const isSuccessSignal =
      eventType === 'playing' ||
      eventType === 'play' ||
      eventType === 'timeupdate' ||
      eventType === 'ready' ||
      eventType === 'player:ready' ||
      eventType === 'jwplayer.ready' ||
      eventType === 'firstframe' ||
      eventType === 'media_data' ||
      data.status === 'playing';

    if (isSuccessSignal) {
      markStreamHealthy();
      return;
    }

    // Explicit error signals
    const isErrorSignal =
      eventType === 'error' ||
      eventType === 'player_error' ||
      eventType === 'media_error' ||
      eventType === 'hlserror' ||
      data.error === true;

    if (isErrorSignal) {
      triggerNextFallback('Stream error reported by player');
    }
  } catch {}
};

// Computed Stream URL using automatic server routing
const streamUrl = computed(() => {
  const current = SERVERS[activeServerIndex.value] || SERVERS[0];
  return current.getUrl(props.item.id, isTv.value, currentSeason.value, currentEpisode.value);
});

const loadEpisodesForSeason = async (sn: number) => {
  try {
    const data = await getSeasonDetails(props.item.id, sn);
    episodes.value = data.episodes || [];
  } catch (err) {
    console.error('Failed to load season episodes', err);
    episodes.value = [];
  }
};

const handleSeasonChange = async (event: Event) => {
  const target = event.target as HTMLSelectElement;
  currentSeason.value = parseInt(target.value, 10);
  currentEpisode.value = 1;
  fallbackNotice.value = '';
  triggerLight();
  emit('episode-change', { season: currentSeason.value, episode: currentEpisode.value });
  await loadEpisodesForSeason(currentSeason.value);
};

const handleEpisodeSelect = (epNum: number) => {
  currentEpisode.value = epNum;
  fallbackNotice.value = '';
  triggerLight();
  emit('episode-change', { season: currentSeason.value, episode: currentEpisode.value });
  startPlaying(true);
};

const playEpisode = async (seasonNum: number, epNum: number) => {
  if (currentSeason.value !== seasonNum) {
    currentSeason.value = seasonNum;
    await loadEpisodesForSeason(seasonNum);
  }
  currentEpisode.value = epNum;
  fallbackNotice.value = '';
  triggerLight();
  emit('episode-change', { season: currentSeason.value, episode: currentEpisode.value });
  startPlaying(true);
};

defineExpose({
  playEpisode,
  startPlaying: (fullscreen = true) => startPlaying(fullscreen),
  openTrailer,
  trailerKey,
  isPlaying,
  currentSeason,
  currentEpisode,
  seasons,
  episodes,
  activeServerIndex,
  selectServer
});

const startPlaying = async (fullscreen = true) => {
  triggerMedium();
  isPlaying.value = true;
  startWatchdog();
  if (fullscreen) {
    enterFullscreen();
  }
};

const enterFullscreen = async () => {
  isFullscreen.value = true;

  // 1. Lock device orientation to landscape
  try {
    await ScreenOrientation.lock({ orientation: 'landscape' });
  } catch {
    try {
      if (screen.orientation && (screen.orientation as any).lock) {
        await (screen.orientation as any).lock('landscape');
      }
    } catch {}
  }

  // 2. Hide status bar and navigation bar for true immersive full screen
  try {
    await SystemBars.setImmersiveFullscreen();
  } catch {}

  try {
    await StatusBar.hide();
  } catch {}

  try {
    await NavigationBar.hide();
  } catch {}

  // 3. Request DOM Fullscreen
  try {
    const elem = document.documentElement;
    if (elem.requestFullscreen && !document.fullscreenElement) {
      await elem.requestFullscreen();
    }
  } catch {
    // WebKit / iOS fallback
  }
};

const stopAndClose = async () => {
  if (isClosing.value) return;
  isClosing.value = true;
  triggerLight();
  clearWatchdog();
  allServersExhausted.value = false;
  showServerMenu.value = false;

  // Hide the fullscreen overlay first (instant)
  isFullscreen.value = false;

  // 1. Restore device orientation back to portrait
  try {
    await ScreenOrientation.lock({ orientation: 'portrait' });
  } catch {
    try {
      if (screen.orientation && (screen.orientation as any).lock) {
        await (screen.orientation as any).lock('portrait');
      }
    } catch {}
  }

  // 2. Maintain permanent fullscreen (keep status bar and navigation bar hidden)
  try {
    await SystemBars.exitImmersiveFullscreen();
  } catch {}

  try {
    await StatusBar.hide();
  } catch {}

  try {
    await NavigationBar.hide();
  } catch {}

  // 3. Exit DOM Fullscreen
  try {
    if (document.exitFullscreen && document.fullscreenElement) {
      await document.exitFullscreen();
    }
  } catch {}

  // 4. Wait for the orientation/viewport to actually return to portrait (height >= width)
  //    before swapping the DOM back to placeholder and emitting close.
  //    This prevents the main page underneath from suffering layout shift or visual snap.
  await new Promise<void>((resolve) => {
    if (window.innerHeight >= window.innerWidth) {
      requestAnimationFrame(() => requestAnimationFrame(() => resolve()));
      return;
    }
    let resolved = false;
    const finish = () => {
      if (!resolved) {
        resolved = true;
        window.removeEventListener('resize', onResize);
        window.removeEventListener('orientationchange', onResize);
        requestAnimationFrame(() => requestAnimationFrame(() => resolve()));
      }
    };
    const onResize = () => {
      if (window.innerHeight >= window.innerWidth) {
        finish();
      }
    };
    window.addEventListener('resize', onResize, { passive: true });
    window.addEventListener('orientationchange', onResize, { passive: true });
    setTimeout(finish, 260);
  });

  if (fallbackNoticeTimer) clearTimeout(fallbackNoticeTimer);
  isPlaying.value = false;
  isClosing.value = false;
  emit('close');
};

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && (isFullscreen.value || isPlaying.value)) {
    stopAndClose();
  }
};

// Picture-in-Picture (PiP) State & Handling
const isPipSupported = ref(false);
const isInPipMode = ref(false);
let pipListener: any = null;

const checkPipSupport = async () => {
  if (PipPlugin?.isPipSupported) {
    try {
      const res = await PipPlugin.isPipSupported();
      isPipSupported.value = Boolean(res?.supported);
    } catch {
      isPipSupported.value = false;
    }
  } else if (typeof document !== 'undefined') {
    isPipSupported.value = Boolean(
      'pictureInPictureEnabled' in document ||
      'documentPictureInPicture' in window
    );
  }
};

const triggerPip = async () => {
  triggerLight();
  if (PipPlugin?.enterPip) {
    try {
      const res = await PipPlugin.enterPip();
      if (res?.success) {
        isInPipMode.value = true;
        return;
      }
    } catch (err) {
      console.warn('Native PiP failed:', err);
    }
  }

  // Web fallback: Document Picture-in-Picture API
  if (typeof window !== 'undefined' && 'documentPictureInPicture' in window) {
    try {
      const pipWindow = await (window as any).documentPictureInPicture.requestWindow({
        width: 640,
        height: 360,
      });
      [...document.styleSheets].forEach((styleSheet) => {
        try {
          const cssRules = [...styleSheet.cssRules].map((rule) => rule.cssText).join('');
          const style = document.createElement('style');
          style.textContent = cssRules;
          pipWindow.document.head.appendChild(style);
        } catch {}
      });
      const activeIframe = document.querySelector('.fullscreen-iframe') || document.querySelector('.stream-iframe');
      if (activeIframe) {
        const parent = activeIframe.parentElement;
        pipWindow.document.body.appendChild(activeIframe);
        pipWindow.addEventListener('pagehide', () => {
          isInPipMode.value = false;
          if (parent && activeIframe) {
            parent.appendChild(activeIframe);
          }
        });
        isInPipMode.value = true;
      }
    } catch (e) {
      console.warn('Document PiP failed:', e);
    }
  }
};

watch([isPlaying, isFullscreen], ([playing]) => {
  if (PipPlugin?.setAutoPip) {
    try {
      PipPlugin.setAutoPip({ enabled: playing });
    } catch {}
  }
  if (playing) {
    resetControlsTimeout();
  } else {
    showControls.value = true;
    if (controlsInactivityTimer) {
      clearTimeout(controlsInactivityTimer);
      controlsInactivityTimer = null;
    }
  }
});

onMounted(async () => {
  window.addEventListener('keydown', handleKeyDown);
  window.addEventListener('message', handleWindowMessage);
  window.addEventListener('mousemove', handleUserActivity, { passive: true });
  window.addEventListener('touchstart', handleUserActivity, { passive: true });

  checkPipSupport();
  if (PipPlugin?.addListener) {
    try {
      pipListener = await PipPlugin.addListener('pipChange', (data: { inPip: boolean }) => {
        isInPipMode.value = Boolean(data?.inPip);
      });
    } catch {}
  }

  // Scroll parallax listener
  const sheet = placeholderRef.value?.closest('.modal-sheet');
  const scrollTarget = sheet || window;
  const handleScroll = () => {
    if (!placeholderRef.value) return;
    const rect = placeholderRef.value.getBoundingClientRect();
    const windowH = window.innerHeight;
    const relativePos = (rect.top + rect.height / 2) - (windowH / 2);
    scrollParallaxY.value = Math.round(relativePos * -0.12);
  };
  scrollTarget.addEventListener('scroll', handleScroll, { passive: true });
  scrollCleanup = () => scrollTarget.removeEventListener('scroll', handleScroll);
  handleScroll();

  if (props.initialSeason) {
    currentSeason.value = props.initialSeason;
  }
  if (props.initialEpisode) {
    currentEpisode.value = props.initialEpisode;
  }

  if (isTv.value) {
    loadingTvData.value = true;
    try {
      const details = await getTvDetails(props.item.id);
      seasons.value = (details.seasons || []).filter((s) => s.season_number > 0);
      if (seasons.value.length > 0) {
        if (props.initialSeason && seasons.value.some((s) => s.season_number === props.initialSeason)) {
          currentSeason.value = props.initialSeason;
        } else {
          currentSeason.value = seasons.value[0].season_number;
        }
        await loadEpisodesForSeason(currentSeason.value);
        if (props.initialEpisode && episodes.value.some((ep) => ep.episode_number === props.initialEpisode)) {
          currentEpisode.value = props.initialEpisode;
        }
      }
    } catch (err) {
      console.error('Error fetching TV details:', err);
    } finally {
      loadingTvData.value = false;
    }
  }

  // Proactively test which server has a working stream and set as default
  await probeAndSetDefaultServer();

  if (props.autoPlay) {
    startPlaying(true);
  }

  // Load IMDB/TMDB photo slideshow and trailer in the background
  loadSlideImages();
  loadTrailer();
});

onUnmounted(async () => {
  stopSlideshow();
  clearWatchdog();
  window.removeEventListener('keydown', handleKeyDown);
  window.removeEventListener('message', handleWindowMessage);
  window.removeEventListener('mousemove', handleUserActivity);
  window.removeEventListener('touchstart', handleUserActivity);
  if (controlsInactivityTimer) {
    clearTimeout(controlsInactivityTimer);
    controlsInactivityTimer = null;
  }
  if (scrollCleanup) scrollCleanup();
  if (fallbackNoticeTimer) clearTimeout(fallbackNoticeTimer);
  if (pipListener) {
    try {
      pipListener.remove();
    } catch {}
  }
  if (PipPlugin?.setAutoPip) {
    try {
      PipPlugin.setAutoPip({ enabled: false });
    } catch {}
  }

  // Guarantee portrait mode and keep fullscreen when player unmounts
  try {
    await ScreenOrientation.lock({ orientation: 'portrait' });
  } catch {}
  try {
    await StatusBar.hide();
  } catch {}
});

watch(
  () => props.item.id,
  async () => {
    stopAndClose();
    clearWatchdog();
    failedServerIndices.value.clear();
    allServersExhausted.value = false;
    userManuallySelectedServer.value = false;
    recommendedServerIndex.value = null;
    serverLatencies.value = {};
    serverStatusMap.value = {};
    activeServerIndex.value = 0;
    fallbackNotice.value = '';
    currentSeason.value = 1;
    currentEpisode.value = 1;
    await probeAndSetDefaultServer();
    if (props.autoPlay) {
      startPlaying(true);
    }
  }
);

watch([currentSeason, currentEpisode], () => {
  failedServerIndices.value.clear();
  allServersExhausted.value = false;
  probeAndSetDefaultServer();
});
</script>

<template>
  <div class="stream-player-container">

    <!-- Stream Server Selector Row (Only shown if showInlineServer is enabled) -->
    <div v-if="showInlineServer && !isPlaying" class="server-picker-row">
      <div class="server-picker-label">
        <Server :size="13" class="server-icon" />
        <span>Server</span>
      </div>
      <div class="server-pills-scroller no-scrollbar">
        <button
          v-for="(s, idx) in SERVERS"
          :key="s.id"
          class="server-pill"
          :class="{ active: idx === activeServerIndex }"
          @click="selectServer(idx)"
          :title="s.badge"
        >
          <span class="pill-dot"></span>
          <span>{{ s.name }}</span>
          <span class="pill-badge">{{ s.badge }}</span>
        </button>
      </div>
    </div>

    <!-- Inline Video Aspect Box -->
    <div class="video-aspect-box">
      <!-- Placeholder with Parallax Backdrop (when not playing) -->
      <transition name="player-fade">
      <div
        v-if="!isPlaying"
        ref="placeholderRef"
        class="player-placeholder"
        @click="startPlaying(true)"
        @mousemove="handleMouseMove"
        @mouseenter="handleMouseEnter"
        @mouseleave="handleMouseLeave"
      >
        <!-- Parallax Background Layer (IMDB/TMDB Photo Slideshow) -->
        <div class="parallax-bg-wrapper">
          <!-- Ambient blurred backdrop (always current slide) -->
          <img
            :src="currentSlideUrl"
            :alt="mediaTitle"
            class="parallax-bg-blur"
            loading="lazy"
          />
          <!-- Slide stack: each image fades in/out as slideIndex changes -->
          <template v-if="slideImages.length > 0">
            <img
              v-for="(src, i) in slideImages"
              :key="src"
              :src="src"
              :alt="mediaTitle"
              class="slide-bg-img"
              :class="{ active: i === slideIndex }"
              :style="i === slideIndex ? parallaxStyle : {}"
              loading="lazy"
            />
          </template>
          <!-- Fallback single image if slideshow not loaded yet -->
          <img
            v-else-if="playerImageUrl"
            :src="playerImageUrl"
            :alt="mediaTitle"
            class="parallax-bg-img"
            :style="parallaxStyle"
            loading="lazy"
            @error="handlePlayerImageError"
          />
          <div class="parallax-cinematic-gradient"></div>
          <div class="parallax-ambient-glow" :class="{ 'tv-theme': isTv }"></div>
        </div>

        <!-- Play Button + Watch Trailer -->
        <div class="placeholder-content">
          <div class="play-pulse-btn" :class="{ 'tv-theme': isTv }">
            <Play :size="32" fill="currentColor" />
          </div>
          <!-- Watch Trailer button — shown only when a YouTube trailer is found -->
          <Transition name="trailer-btn-fade">
            <button
              v-if="trailerKey"
              class="watch-trailer-btn"
              @click.stop="openTrailer"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19.615 3.184c-3.604-.246-11.631-.245-15.23 0C.488 3.45.029 5.804 0 12c.029 6.185.484 8.549 4.385 8.816 3.6.245 11.626.246 15.23 0C23.512 20.55 23.971 18.196 24 12c-.029-6.185-.484-8.549-4.385-8.816zM9 16V8l8 4-8 4z"/>
              </svg>
              Watch Trailer
            </button>
          </Transition>
        </div>
      </div>
      </transition>

      <!-- Inline Video with Server Controls and Exit button -->
      <transition name="player-fade">
      <div
        v-if="isPlaying"
        class="inline-player-wrap"
        :class="{ 'in-pip-mode': isInPipMode, 'controls-hidden': !showControls }"
        @pointermove="handleUserActivity"
        @touchstart.passive="handleUserActivity"
      >
        <!-- Top Touch/Tap Wake Zone (Inline) -->
        <div
          class="inline-touch-wake-zone"
          :class="{ 'is-active': !showControls }"
          @click.stop="toggleControls"
        ></div>

        <iframe
          :key="streamUrl"
          :src="streamUrl"
          class="stream-iframe"
          allow="autoplay; encrypted-media; fullscreen; picture-in-picture; accelerometer; gyroscope"
          allowfullscreen
          referrerpolicy="no-referrer-when-downgrade"
          @load="handleIframeLoad"
          @error="handleIframeError"
        ></iframe>

        <!-- Exhausted Servers Overlay (Inline) -->
        <transition name="hint-fade">
          <div v-if="allServersExhausted" class="exhausted-screen-overlay">
            <div class="exhausted-card">
              <AlertCircle :size="28" class="exhausted-card-icon" />
              <h4 class="exhausted-card-title">All 7 Servers Attempted</h4>
              <p class="exhausted-card-desc">No active stream responded across current mirrors for this title.</p>
              <div class="exhausted-btn-row">
                <button class="exhausted-retry-btn" @click.stop="retryAllServers">
                  <RotateCw :size="13" />
                  Retry Server 1
                </button>
                <button class="exhausted-pick-btn" @click.stop="showServerMenu = true">
                  <Server :size="13" />
                  Choose Server
                </button>
              </div>
            </div>
          </div>
        </transition>

        <!-- Top Inline Controls -->
        <div class="inline-top-bar" @click.stop="resetControlsTimeout">
          <!-- Server Switcher & Next Server Button (Only shown if showInlineServer enabled) -->
          <div v-if="showInlineServer" class="server-quick-controls">
            <button
              class="player-server-btn"
              :class="{ loading: isStreamLoading }"
              :title="'Server: ' + activeServer.name + ' (Tap to switch)'"
              aria-label="Select Server"
              @click.stop="showServerMenu = !showServerMenu"
            >
              <Server :size="14" />
              <span
                v-if="serverStatusMap[activeServerIndex] !== undefined"
                class="server-header-dot"
                :class="serverStatusMap[activeServerIndex] ? 'online' : 'offline'"
                :title="serverStatusMap[activeServerIndex] ? 'Stream Verified' : 'Stream issue'"
              ></span>
            </button>
            <button
              class="player-next-btn"
              title="Switch to Next Server"
              aria-label="Switch to Next Server"
              @click.stop="nextServerManual"
            >
              <RotateCw :size="13" :class="{ spin: isStreamLoading }" />
            </button>
          </div>

          <div class="inline-top-actions">
            <!-- Picture in Picture Button -->
            <button
              v-if="isPipSupported"
              class="player-pip-btn"
              title="Picture in Picture"
              aria-label="Picture in Picture"
              @click.stop="triggerPip"
            >
              <PictureInPicture :size="14" />
            </button>
            <button
              class="player-fullscreen-btn"
              title="Fullscreen Theater Mode"
              @click.stop="enterFullscreen"
            >
              <Maximize :size="14" />
            </button>
            <!-- Exit Button (X) -->
            <button
              class="player-exit-btn"
              aria-label="Exit Player"
              title="Exit Player"
              @click="stopAndClose"
            >
              <X :size="16" stroke-width="2.2" />
            </button>
          </div>
        </div>

        <!-- Server Dropdown Menu (Inline) -->
        <transition name="menu-fade">
          <div v-if="showServerMenu" class="player-server-menu" @click.stop>
            <div class="menu-title">Select Stream Server</div>
            <button
              v-for="(s, idx) in SERVERS"
              :key="s.id"
              class="menu-server-item"
              :class="{ active: idx === activeServerIndex }"
              @click="selectServer(idx)"
            >
              <div class="menu-item-info">
                <span
                  class="server-health-dot"
                  :class="{
                    online: serverStatusMap[idx] === true,
                    offline: serverStatusMap[idx] === false,
                    testing: isProbingServers && serverStatusMap[idx] === undefined
                  }"
                  :title="serverStatusMap[idx] === true ? 'Verified Stream Online' : (serverStatusMap[idx] === false ? 'Server Offline / Unavailable' : 'Testing server...')"
                ></span>
                <span class="item-name">{{ s.name }}</span>
                <span class="item-badge">{{ s.badge }}</span>
                <span v-if="idx === recommendedServerIndex" class="item-verified-tag">Default</span>
              </div>
              <div class="menu-item-meta">
                <span v-if="serverLatencies[idx]" class="item-latency font-mono">{{ serverLatencies[idx] }}ms</span>
                <Check v-if="idx === activeServerIndex" :size="14" class="item-check" />
              </div>
            </button>
          </div>
        </transition>

        <!-- Inline Fallback Notification -->
        <transition name="hint-fade">
          <div v-if="fallbackNotice" class="fallback-notice-pill inline-hint">
            <span class="fallback-dot"></span>
            <span>{{ fallbackNotice }}</span>
          </div>
        </transition>
      </div>
      </transition>
    </div>

    <!-- FULLSCREEN THEATER: Video Frame with Server Switcher & Exit -->
    <Teleport to="body">
      <div
        v-if="isFullscreen && isPlaying"
        class="fullscreen-theater-overlay force-landscape"
        :class="{ 'in-pip-mode': isInPipMode, 'controls-hidden': !showControls }"
        @pointermove="handleUserActivity"
        @touchstart.passive="handleUserActivity"
      >
        <!-- Top Touch/Tap Wake Zone (Fullscreen) -->
        <div
          class="theater-touch-wake-zone"
          :class="{ 'is-active': !showControls }"
          @click.stop="toggleControls"
        ></div>

        <!-- Fallback notice pill (only appears during server switch / error fallback) -->
        <transition name="hint-fade">
          <div v-if="fallbackNotice" class="fallback-notice-pill">
            <span class="fallback-dot"></span>
            <span>{{ fallbackNotice }}</span>
          </div>
        </transition>

        <!-- Floating Theater Top Controls Header -->
        <div class="theater-top-controls" @click.stop="resetControlsTimeout">
          <!-- Server Switcher & Next Server Button -->
          <div class="theater-server-pill-wrap">
            <button
              class="theater-server-btn"
              :class="{ loading: isStreamLoading }"
              :title="'Server: ' + activeServer.name + ' (Tap to switch)'"
              aria-label="Select Server"
              @click.stop="showServerMenu = !showServerMenu"
            >
              <Server :size="16" />
              <span
                v-if="serverStatusMap[activeServerIndex] !== undefined"
                class="server-header-dot"
                :class="serverStatusMap[activeServerIndex] ? 'online' : 'offline'"
              ></span>
            </button>

            <!-- TV Episode Selector Button (Theater Mode) -->
            <button
              v-if="isTv && episodes.length > 0"
              class="theater-episodes-btn"
              :title="'Season ' + currentSeason + ' Episode ' + currentEpisode"
              aria-label="Episodes"
              @click.stop="showEpisodesMenu = !showEpisodesMenu; showServerMenu = false"
            >
              <Tv :size="15" />
              <span>S{{ currentSeason }}:E{{ currentEpisode }}</span>
            </button>

            <button
              class="theater-next-btn"
              title="Switch to Next Server"
              aria-label="Switch to Next Server"
              @click.stop="nextServerManual"
            >
              <RotateCw :size="16" :class="{ spin: isStreamLoading }" />
            </button>
          </div>

          <!-- Floating Theater Top Right Actions (PiP & Close) -->
          <div class="theater-top-actions">
            <button
              v-if="isPipSupported"
              class="floating-theater-pip-btn"
              aria-label="Picture in Picture"
              title="Picture in Picture"
              @click.stop="triggerPip"
            >
              <PictureInPicture :size="17" />
            </button>

            <!-- Exit Button (X) -->
            <button
              class="floating-theater-exit-btn"
              aria-label="Exit Player"
              title="Exit Player"
              @click.stop="stopAndClose"
            >
              <X :size="18" stroke-width="2.2" />
            </button>
          </div>
        </div>

        <!-- Theater Server Menu Dropdown -->
        <transition name="menu-fade">
          <div v-if="showServerMenu" class="theater-server-menu" @click.stop>
            <div class="menu-title">Select Stream Server</div>
            <div class="menu-items-list">
              <button
                v-for="(s, idx) in SERVERS"
                :key="s.id"
                class="menu-server-item"
                :class="{ active: idx === activeServerIndex }"
                @click="selectServer(idx)"
              >
                <div class="menu-item-info">
                  <span
                    class="server-health-dot"
                    :class="{
                      online: serverStatusMap[idx] === true,
                      offline: serverStatusMap[idx] === false,
                      testing: isProbingServers && serverStatusMap[idx] === undefined
                    }"
                  ></span>
                  <span class="item-name">{{ s.name }}</span>
                  <span class="item-badge">{{ s.badge }}</span>
                  <span v-if="idx === recommendedServerIndex" class="item-verified-tag">Default</span>
                </div>
                <div class="menu-item-meta">
                  <span v-if="serverLatencies[idx]" class="item-latency font-mono">{{ serverLatencies[idx] }}ms</span>
                  <Check v-if="idx === activeServerIndex" :size="16" class="item-check" />
                </div>
              </button>
            </div>
          </div>
        </transition>

        <!-- Theater Episodes Dropdown Menu -->
        <transition name="menu-fade">
          <div v-if="showEpisodesMenu" class="theater-episodes-menu" @click.stop>
            <div class="theater-episodes-header">
              <div class="menu-title">Episodes — Season {{ currentSeason }}</div>
              <select
                v-if="seasons.length > 1"
                class="theater-season-select font-mono"
                :value="currentSeason"
                @change="handleSeasonChange"
              >
                <option v-for="s in seasons" :key="s.season_number" :value="s.season_number">
                  Season {{ s.season_number }}
                </option>
              </select>
            </div>
            <div class="theater-episodes-grid no-scrollbar">
              <button
                v-for="ep in episodes"
                :key="ep.episode_number"
                class="theater-ep-pill font-mono"
                :class="{ active: ep.episode_number === currentEpisode }"
                @click="handleEpisodeSelect(ep.episode_number); showEpisodesMenu = false"
              >
                E{{ ep.episode_number < 10 ? '0' + ep.episode_number : ep.episode_number }}
              </button>
            </div>
          </div>
        </transition>

        <!-- Video Frame -->
        <div class="fullscreen-video-frame" @click="toggleControls">
          <iframe
            :key="streamUrl"
            :src="streamUrl"
            class="fullscreen-iframe"
            allow="autoplay; encrypted-media; fullscreen; picture-in-picture; accelerometer; gyroscope"
            allowfullscreen
            referrerpolicy="no-referrer-when-downgrade"
            @load="handleIframeLoad"
            @error="handleIframeError"
          ></iframe>

          <!-- Exhausted Servers Overlay (Theater Mode) -->
          <transition name="hint-fade">
            <div v-if="allServersExhausted" class="exhausted-screen-overlay" @click.stop>
              <div class="exhausted-card">
                <AlertCircle :size="36" class="exhausted-card-icon" />
                <h3 class="exhausted-card-title">All 7 Mirrors Attempted</h3>
                <p class="exhausted-card-desc">None of the available streaming mirrors responded in time for this title.</p>
                <div class="exhausted-btn-row">
                  <button class="exhausted-retry-btn" @click.stop="retryAllServers">
                    <RotateCw :size="15" />
                    Retry Server 1
                  </button>
                  <button class="exhausted-pick-btn" @click.stop="showServerMenu = true">
                    <Server :size="15" />
                    Choose Mirror
                  </button>
                </div>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </Teleport>

    <!-- YouTube Trailer Overlay -->
    <Teleport to="body">
      <Transition name="trailer-fade">
        <div v-if="showTrailer" class="trailer-overlay" @click.self="closeTrailer">
          <div class="trailer-frame-wrap">
            <button class="trailer-close-btn" aria-label="Close Trailer" @click="closeTrailer">
              <X :size="18" stroke-width="2.2" />
            </button>
            <iframe
              v-if="showTrailer"
              :src="trailerUrl"
              class="trailer-iframe"
              allow="autoplay; encrypted-media; fullscreen"
              allowfullscreen
              frameborder="0"
            />
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.stream-player-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 0.85rem;
  /* Isolate this subtree from causing page-level reflows */
  contain: layout;
}

/* Server Picker Row (Pre-play) */
.server-picker-row {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.2rem 0.2rem 0.4rem 0.2rem;
  overflow-x: auto;
}

.server-picker-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.6px;
  flex-shrink: 0;
}

.server-icon {
  color: var(--accent-amber);
}

.server-pills-scroller {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  overflow-x: auto;
}

.server-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0.32rem 0.65rem;
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-size: 0.74rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.server-pill:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-primary);
  border-color: rgba(255, 255, 255, 0.2);
}

.server-pill.active {
  background: rgba(245, 158, 11, 0.15);
  border-color: var(--accent-amber);
  color: #fff;
  font-weight: 700;
  box-shadow: 0 0 12px rgba(245, 158, 11, 0.25);
}

.pill-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #64748b;
  transition: background 0.2s ease;
}

.server-pill.active .pill-dot {
  background: #10b981;
  box-shadow: 0 0 6px #10b981;
}

.pill-badge {
  font-size: 0.62rem;
  padding: 1px 5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 0.3px;
}

.server-pill.active .pill-badge {
  background: rgba(245, 158, 11, 0.3);
  color: #fef08a;
}

/* TV Selector */
.tv-selector-bar {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  padding-bottom: 0.25rem;
}

.season-picker {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.picker-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.season-select {
  background: var(--bg-card);
  color: var(--text-primary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  padding: 0.35rem 0.65rem;
  font-size: 0.78rem;
  font-weight: 600;
  outline: none;
  cursor: pointer;
}

.episodes-scroller {
  overflow-x: auto;
  padding-bottom: 4px;
}

.episodes-list {
  display: flex;
  gap: 0.4rem;
}

.ep-pill {
  padding: 0.35rem 0.65rem;
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.ep-pill.active {
  background: var(--accent-rose);
  border-color: var(--accent-rose);
  color: #fff;
  font-weight: 700;
}

/* Video Aspect Box */
.video-aspect-box {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #050508;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.65);
  /* Contain reflows to this box only */
  contain: layout style;
  /* Prevent any dimension change from propagating upward */
  will-change: contents;
}

.player-placeholder {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  user-select: none;
}

/* Parallax Background Wrapper */
.parallax-bg-wrapper {
  position: absolute;
  inset: -25px; /* Overhang to allow parallax movement without edge clipping */
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
  background: radial-gradient(circle at center, #1b1b29 0%, #0d0d16 65%, #07070a 100%);
}

.parallax-bg-blur {
  position: absolute;
  inset: -20px;
  width: calc(100% + 40px);
  height: calc(100% + 40px);
  object-fit: cover;
  filter: blur(28px) brightness(0.38) saturate(1.4);
  transform: scale(1.1);
  pointer-events: none;
}

.parallax-bg-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 25%;
  filter: brightness(0.6) contrast(1.18) saturate(1.2);
  transition: transform 0.22s cubic-bezier(0.16, 1, 0.3, 1), filter 0.35s ease;
  will-change: transform;
}

.player-placeholder:hover .parallax-bg-img {
  filter: brightness(0.72) contrast(1.24) saturate(1.3);
}

/* Photo Slideshow: stacked images that crossfade and Ken-Burns zoom */
.slide-bg-img {
  position: absolute;
  inset: -25px;
  width: calc(100% + 50px);
  height: calc(100% + 50px);
  object-fit: cover;
  object-position: center 25%;
  filter: brightness(0.6) contrast(1.18) saturate(1.2);
  opacity: 0;
  transition: opacity 1.2s ease, filter 0.35s ease;
  will-change: opacity, transform;
  animation: none;
}

.slide-bg-img.active {
  opacity: 1;
  animation: kenBurns 4.2s ease-in-out forwards;
}

@keyframes kenBurns {
  0%   { transform: scale(1.06) translate(0%, 0%); }
  100% { transform: scale(1.14) translate(-1.5%, -1%); }
}

.player-placeholder:hover .slide-bg-img.active {
  filter: brightness(0.72) contrast(1.24) saturate(1.3);
}


/* Vignette Gradients */
.parallax-cinematic-gradient {
  position: absolute;
  inset: 0;
  background: 
    radial-gradient(ellipse at center, rgba(7, 7, 10, 0.32) 0%, rgba(7, 7, 10, 0.88) 85%),
    linear-gradient(180deg, rgba(7, 7, 10, 0.35) 0%, rgba(7, 7, 10, 0.9) 100%);
  pointer-events: none;
}

.parallax-ambient-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, rgba(245, 158, 11, 0.24) 0%, transparent 65%);
  pointer-events: none;
  opacity: 0.75;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.parallax-ambient-glow.tv-theme {
  background: radial-gradient(circle at center, rgba(244, 63, 94, 0.24) 0%, transparent 65%);
}

.player-placeholder:hover .parallax-ambient-glow {
  opacity: 1;
  transform: scale(1.1);
}

/* ==========================================================================
   TV CRT Screen Effects
   ========================================================================== */
.tv-screen-effects {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
  overflow: hidden;
  border-radius: inherit;
}

/* Horizontal Scanlines */
.tv-scanlines {
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    0deg,
    rgba(0, 0, 0, 0.38) 0px,
    rgba(0, 0, 0, 0.38) 1.5px,
    transparent 1.5px,
    transparent 3px
  );
  opacity: 0.65;
  mix-blend-mode: multiply;
  pointer-events: none;
}

/* Moving Phosphor Scanline Beam */
.tv-scanline-beam {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 22%;
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(255, 255, 255, 0.05) 50%,
    rgba(245, 158, 11, 0.08) 75%,
    transparent 100%
  );
  opacity: 0.85;
  animation: tvBeamSweep 6.5s linear infinite;
  pointer-events: none;
}

@keyframes tvBeamSweep {
  0% {
    transform: translateY(-100%);
  }
  100% {
    transform: translateY(600%);
  }
}

/* Glass Reflection Highlight */
.tv-glass-glare {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    130deg,
    rgba(255, 255, 255, 0.09) 0%,
    rgba(255, 255, 255, 0.02) 28%,
    transparent 45%
  );
  pointer-events: none;
}

/* Vignette / CRT Screen Curvature */
.tv-vignette {
  position: absolute;
  inset: 0;
  box-shadow: inset 0 0 32px rgba(0, 0, 0, 0.88);
  background: radial-gradient(ellipse at center, transparent 55%, rgba(0, 0, 0, 0.55) 100%);
  pointer-events: none;
}

/* Broadcast HUD Badges */
.tv-hud-top {
  position: absolute;
  top: 0.75rem;
  left: 0.85rem;
  right: 0.85rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  pointer-events: none;
  z-index: 2;
}

.tv-hud-bottom {
  position: absolute;
  bottom: 0.75rem;
  right: 0.85rem;
  display: flex;
  align-items: center;
  pointer-events: none;
  z-index: 2;
}

.tv-hud-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 7px;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  font-size: 0.62rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.live-badge {
  color: var(--accent-amber);
  border-color: rgba(245, 158, 11, 0.35);
}

.live-badge .rec-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-rose);
  box-shadow: 0 0 6px var(--accent-rose);
  animation: recBlink 1.4s infinite ease-in-out;
}

@keyframes recBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.2; }
}

.tech-badge {
  color: #94a3b8;
}

.channel-badge {
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.58rem;
}

/* Subtle CRT Flicker on Hover */
.player-placeholder:hover .tv-screen-effects {
  animation: tvFlicker 0.15s infinite alternate;
}

@keyframes tvFlicker {
  0% { opacity: 0.96; }
  100% { opacity: 1; }
}

/* Foreground Interactive Content */
.placeholder-content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.85rem;
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.player-placeholder:hover .placeholder-content {
  transform: translateY(-2px);
}

.play-pulse-btn {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--accent-amber);
  color: #07070a;
  display: grid;
  place-items: center;
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 0 24px rgba(245, 158, 11, 0.45), 0 8px 20px rgba(0, 0, 0, 0.5);
}

.play-pulse-btn.tv-theme {
  background: var(--accent-rose);
  color: #fff;
  box-shadow: 0 0 24px rgba(244, 63, 94, 0.45), 0 8px 20px rgba(0, 0, 0, 0.5);
}

.player-placeholder:hover .play-pulse-btn {
  transform: scale(1.12);
  box-shadow: 0 0 35px rgba(245, 158, 11, 0.75), 0 10px 25px rgba(0, 0, 0, 0.6);
}

.player-placeholder:hover .play-pulse-btn.tv-theme {
  box-shadow: 0 0 35px rgba(244, 63, 94, 0.75), 0 10px 25px rgba(0, 0, 0, 0.6);
}

.player-placeholder:active .play-pulse-btn {
  transform: scale(0.96);
}

.placeholder-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  text-align: center;
  padding: 0 1rem;
}

.main-cta {
  font-size: 1rem;
  font-weight: 800;
  color: var(--text-primary);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.8);
  letter-spacing: 0.2px;
}

.sub-cta {
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.75);
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.9);
}

/* Inline Player Wrap */
.inline-player-wrap {
  position: relative;
  width: 100%;
  height: 100%;
}

.stream-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #000;
}

.player-exit-btn {
  width: 32px;
  height: 32px;
  box-sizing: border-box;
  border-radius: 50%;
  display: grid;
  place-items: center;
  padding: 0;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.8);
  opacity: 0.7;
  cursor: pointer;
  transition: opacity 0.25s ease, background 0.25s ease, border-color 0.25s ease, transform 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}

.player-exit-btn:hover,
.player-exit-btn:active {
  opacity: 1;
  background: rgba(14, 14, 20, 0.85);
  border-color: rgba(255, 255, 255, 0.35);
  color: #ffffff;
  transform: scale(1.08);
}

/* Inline Top Controls Bar */
.inline-top-bar {
  position: absolute;
  top: 10px;
  left: 10px;
  right: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  z-index: 35;
  pointer-events: none;
  opacity: 1;
  transform: translateY(0);
  transition: opacity 0.35s cubic-bezier(0.16, 1, 0.3, 1), transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

.inline-top-bar > * {
  pointer-events: auto;
}

.inline-player-wrap.controls-hidden .inline-top-bar {
  opacity: 0;
  transform: translateY(-8px);
  pointer-events: none !important;
}

.inline-player-wrap.controls-hidden .inline-top-bar > * {
  pointer-events: none !important;
}

/* Touch wake zone for inline player */
.inline-touch-wake-zone {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 52px;
  z-index: 34;
  pointer-events: none;
}

.inline-player-wrap.controls-hidden .inline-touch-wake-zone {
  pointer-events: auto;
}

.server-quick-controls {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
}

.player-server-btn {
  width: 32px;
  height: 32px;
  box-sizing: border-box;
  border-radius: 50%;
  position: relative;
  display: grid;
  place-items: center;
  padding: 0;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}

.player-server-btn:hover {
  background: rgba(245, 158, 11, 0.25);
  border-color: var(--accent-amber);
  color: #fff;
}

.player-server-btn .server-header-dot {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 6px;
  height: 6px;
  margin: 0;
}

.player-next-btn {
  width: 32px;
  height: 32px;
  box-sizing: border-box;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}

.player-next-btn:hover {
  background: rgba(245, 158, 11, 0.25);
  border-color: var(--accent-amber);
  color: #fff;
  transform: rotate(90deg);
}

.player-next-btn:active {
  transform: scale(0.92);
}

.inline-top-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.player-fullscreen-btn,
.player-pip-btn {
  width: 32px;
  height: 32px;
  box-sizing: border-box;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.85);
  display: grid;
  place-items: center;
  padding: 0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.player-fullscreen-btn:hover,
.player-pip-btn:hover {
  background: rgba(245, 158, 11, 0.25);
  border-color: var(--accent-amber);
  color: #fff;
}


.player-next-btn:active {
  transform: scale(0.92);
}

/* Inline Server Menu Dropdown */
.player-server-menu {
  position: absolute;
  top: 46px;
  left: 10px;
  z-index: 40;
  background: rgba(14, 14, 22, 0.94);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: var(--radius-md);
  padding: 6px;
  min-width: 220px;
  max-width: 280px;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.75);
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.menu-title {
  font-size: 0.68rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 5px 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 3px;
}

.menu-server-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.76rem;
  font-weight: 500;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
  width: 100%;
}

.menu-server-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.menu-server-item.active {
  background: rgba(245, 158, 11, 0.18);
  color: #fef08a;
  font-weight: 700;
}

.item-badge {
  font-size: 0.62rem;
  padding: 1px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.6);
  white-space: nowrap;
}

.menu-server-item.active .item-badge {
  background: rgba(245, 158, 11, 0.3);
  color: #fef08a;
}

.item-check {
  color: #10b981;
  flex-shrink: 0;
}

.server-health-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  flex-shrink: 0;
}
.server-health-dot.online {
  background: #10b981;
  box-shadow: 0 0 7px rgba(16, 185, 129, 0.8);
}
.server-health-dot.offline {
  background: #ef4444;
  box-shadow: 0 0 5px rgba(239, 68, 68, 0.6);
}
.server-health-dot.testing {
  background: #f59e0b;
  animation: pulseHealthDot 1.2s infinite ease-in-out;
}
@keyframes pulseHealthDot {
  0%, 100% { opacity: 0.35; transform: scale(0.85); }
  50% { opacity: 1; transform: scale(1.2); }
}

.server-header-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-left: 2px;
  flex-shrink: 0;
}
.server-header-dot.online {
  background: #10b981;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.85);
}
.server-header-dot.offline {
  background: #ef4444;
}

.item-verified-tag {
  font-size: 0.58rem;
  font-weight: 700;
  text-transform: uppercase;
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(16, 185, 129, 0.2);
  color: #34d399;
  border: 1px solid rgba(52, 211, 153, 0.4);
  letter-spacing: 0.4px;
}

.menu-item-info {
  display: flex;
  align-items: center;
  gap: 7px;
}

.menu-item-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}

.item-latency {
  font-size: 0.65rem;
  color: rgba(255, 255, 255, 0.45);
}

/* Fullscreen Theater Top Controls */
.fullscreen-theater-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  background: #000;
  overflow: hidden;
  transition: cursor 0.2s ease;
}

.fullscreen-theater-overlay.controls-hidden {
  cursor: none;
}

/* Touch wake zone to capture tap near top when theater buttons are hidden */
.theater-touch-wake-zone {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 80px;
  z-index: 99998;
  pointer-events: none;
}

.fullscreen-theater-overlay.controls-hidden .theater-touch-wake-zone {
  pointer-events: auto;
}

.theater-top-controls {
  position: absolute;
  top: 8px;
  left: max(var(--safe-left, 0px), 12px);
  right: max(var(--safe-right, 0px), 12px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  z-index: 100000;
  pointer-events: none;
  opacity: 1;
  transform: translateY(0);
  transition: opacity 0.35s cubic-bezier(0.16, 1, 0.3, 1), transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

.theater-top-controls > * {
  pointer-events: auto;
}

.fullscreen-theater-overlay.controls-hidden .theater-top-controls {
  opacity: 0;
  transform: translateY(-12px);
  pointer-events: none !important;
}

.fullscreen-theater-overlay.controls-hidden .theater-top-controls > * {
  pointer-events: none !important;
}

.theater-server-pill-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.theater-top-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.theater-server-btn {
  width: 36px;
  height: 36px;
  box-sizing: border-box;
  border-radius: 50%;
  position: relative;
  display: grid;
  place-items: center;
  padding: 0;
  background: rgba(14, 14, 20, 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition: opacity 0.25s ease, background 0.25s ease, border-color 0.25s ease, transform 0.2s ease;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.theater-server-btn:hover,
.theater-server-btn:active {
  background: rgba(22, 22, 32, 0.92);
  border-color: rgba(245, 158, 11, 0.6);
  color: #ffffff;
  transform: scale(1.05);
}

.theater-server-btn .server-header-dot {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 6px;
  height: 6px;
  margin: 0;
}

.theater-next-btn {
  width: 36px;
  height: 36px;
  box-sizing: border-box;
  border-radius: 50%;
  display: grid;
  place-items: center;
  padding: 0;
  background: rgba(14, 14, 20, 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.88);
  cursor: pointer;
  transition: opacity 0.25s ease, background 0.25s ease, border-color 0.25s ease, transform 0.2s ease;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.theater-next-btn:hover,
.theater-next-btn:active {
  background: rgba(22, 22, 32, 0.92);
  border-color: rgba(245, 158, 11, 0.6);
  color: #ffffff;
  transform: scale(1.05);
}

.theater-episodes-btn {
  height: 36px;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(14, 14, 20, 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(99, 184, 188, 0.4);
  color: #a5f3fc;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.theater-episodes-btn:hover {
  background: rgba(22, 22, 32, 0.92);
  border-color: #63b8bc;
}

/* Theater Server Menu Dropdown */
.theater-server-menu {
  position: absolute;
  top: 48px;
  left: max(var(--safe-left, 0px), 12px);
  z-index: 100002;
  background: rgba(14, 14, 22, 0.96);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: var(--radius-md);
  padding: 6px;
  min-width: 220px;
  max-width: 280px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.9);
  display: flex;
  flex-direction: column;
  gap: 3px;
}

/* Theater Episodes Menu Dropdown */
.theater-episodes-menu {
  position: absolute;
  top: 48px;
  left: max(var(--safe-left, 0px), 80px);
  z-index: 100002;
  background: rgba(14, 14, 22, 0.96);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(99, 184, 188, 0.3);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  min-width: 280px;
  max-width: 360px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.9);
}

.theater-episodes-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.theater-season-select {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  border-radius: 6px;
  font-size: 0.72rem;
  padding: 3px 8px;
  outline: none;
}

.theater-episodes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(48px, 1fr));
  gap: 6px;
  max-height: 180px;
  overflow-y: auto;
  padding: 2px;
}

.theater-ep-pill {
  padding: 6px 4px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.75rem;
  font-weight: 600;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.theater-ep-pill:hover {
  background: rgba(99, 184, 188, 0.2);
  border-color: rgba(99, 184, 188, 0.6);
  color: #fff;
}

.theater-ep-pill.active {
  background: linear-gradient(135deg, #63b8bc, #38bdf8);
  border-color: #38bdf8;
  color: #07070a;
  font-weight: 700;
}

.menu-items-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Rotation and Spin Animations */
.rotated {
  transform: rotate(180deg);
  transition: transform 0.2s ease;
}

.spin {
  animation: serverSpin 1.2s linear infinite;
}

@keyframes serverSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.fallback-notice-pill.inline-hint {
  top: auto;
  bottom: 12px;
}

/* FULLSCREEN THEATER OVERLAY */
.fullscreen-theater-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

/* Landscape Mode: In portrait orientation, rotate to full landscape view */
@media screen and (orientation: portrait) {
  .fullscreen-theater-overlay.force-landscape {
    width: 100vh;
    height: 100vw;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%) rotate(90deg);
    transform-origin: center center;
  }
}

/* Floating Exit Button & PiP Button in Theater Mode */
.floating-theater-exit-btn,
.floating-theater-pip-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  padding: 0;
  background: rgba(14, 14, 20, 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.85);
  opacity: 0.75;
  cursor: pointer;
  box-sizing: border-box;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  transition: opacity 0.25s ease, background 0.25s ease, border-color 0.25s ease, transform 0.2s ease;
}

.floating-theater-exit-btn:hover,
.floating-theater-exit-btn:active,
.floating-theater-pip-btn:hover,
.floating-theater-pip-btn:active {
  opacity: 1;
  background: rgba(22, 22, 32, 0.92);
  border-color: rgba(245, 158, 11, 0.6);
  color: #ffffff;
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.5);
}

@media (min-width: 768px) {
  .theater-top-controls {
    top: 10px;
    left: max(calc(var(--safe-left, 0px) + 20px), 20px);
    right: max(calc(var(--safe-right, 0px) + 20px), 20px);
  }

  .theater-server-btn,
  .theater-next-btn,
  .floating-theater-exit-btn,
  .floating-theater-pip-btn {
    width: 40px;
    height: 40px;
  }

  .theater-episodes-btn {
    height: 40px;
    font-size: 0.82rem;
  }
}

/* Picture-in-Picture (PiP) Window Display Mode */
.in-pip-mode .inline-top-bar,
.in-pip-mode .theater-top-controls,
.in-pip-mode .floating-theater-exit-btn,
.in-pip-mode .floating-theater-pip-btn,
.in-pip-mode .fallback-notice-pill,
.in-pip-mode .player-server-menu,
.in-pip-mode .theater-server-menu,
.in-pip-mode .theater-episodes-menu {
  display: none !important;
}

.inline-player-wrap.in-pip-mode,
.fullscreen-theater-overlay.in-pip-mode {
  position: fixed !important;
  inset: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  z-index: 99999 !important;
  background: #000 !important;
  padding: 0 !important;
  margin: 0 !important;
  transform: none !important;
}

.in-pip-mode .fullscreen-video-frame,
.in-pip-mode .stream-iframe,
.in-pip-mode .fullscreen-iframe {
  width: 100% !important;
  height: 100% !important;
  border: none !important;
  position: absolute !important;
  inset: 0 !important;
}

.fullscreen-video-frame {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fullscreen-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #000;
}

/* Automatic Fallback Notice Toast */
.fallback-notice-pill {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100001;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: rgba(14, 14, 20, 0.82);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(245, 158, 11, 0.45);
  border-radius: 999px;
  color: #fef08a;
  font-size: 0.76rem;
  font-weight: 600;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.6);
  pointer-events: none;
}

.fallback-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f59e0b;
  box-shadow: 0 0 8px #f59e0b;
}

.hint-fade-enter-active,
.hint-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.hint-fade-enter-from,
.hint-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -10px);
}

/* Player placeholder <-> inline iframe crossfade */
.player-fade-enter-active {
  transition: opacity 0.25s ease 0.05s;
}
.player-fade-leave-active {
  transition: opacity 0.15s ease;
  /* Keep the leaving element in-place so it doesn't collapse the box */
  position: absolute;
  inset: 0;
}
.player-fade-enter-from,
.player-fade-leave-to {
  opacity: 0;
}

/* Watch Trailer pill button */
.watch-trailer-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.22);
  color: rgba(255, 255, 255, 0.92);
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.3px;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.18s ease, color 0.2s ease;
  box-shadow: 0 2px 14px rgba(0, 0, 0, 0.4);
}

.watch-trailer-btn svg {
  color: #ff0000;
  flex-shrink: 0;
}

.watch-trailer-btn:hover,
.watch-trailer-btn:active {
  background: rgba(255, 0, 0, 0.18);
  border-color: rgba(255, 80, 80, 0.5);
  color: #fff;
  transform: scale(1.05);
}

.trailer-btn-fade-enter-active { transition: opacity 0.4s ease 0.3s, transform 0.4s ease 0.3s; }
.trailer-btn-fade-leave-active { transition: opacity 0.2s ease; }
.trailer-btn-fade-enter-from,
.trailer-btn-fade-leave-to { opacity: 0; transform: translateY(6px); }

/* Trailer fullscreen overlay */
.trailer-overlay {
  position: fixed;
  inset: 0;
  z-index: 99998;
  background: rgba(0, 0, 0, 0.88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: max(var(--safe-top, 0px), 16px) max(var(--safe-right, 0px), 16px)
           max(var(--safe-bottom, 0px), 16px) max(var(--safe-left, 0px), 16px);
}

.trailer-frame-wrap {
  position: relative;
  width: 100%;
  max-width: 900px;
  aspect-ratio: 16 / 9;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.8);
  background: #000;
}

.trailer-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.trailer-close-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 10;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.85);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.18s ease;
}

.trailer-close-btn:hover {
  background: rgba(220, 30, 30, 0.55);
  color: #fff;
  transform: scale(1.1);
}

.trailer-fade-enter-active { transition: opacity 0.25s ease; }
.trailer-fade-leave-active  { transition: opacity 0.2s ease;  }
.trailer-fade-enter-from,
.trailer-fade-leave-to { opacity: 0; }

/* Exhausted Mirrors Overlay */
.exhausted-screen-overlay {
  position: absolute;
  inset: 0;
  background: rgba(10, 10, 15, 0.9);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
  padding: 1.5rem;
}

.exhausted-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.5rem;
  max-width: 320px;
}

.exhausted-card-icon {
  color: #f59e0b;
  margin-bottom: 0.25rem;
}

.exhausted-card-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-main, #ffffff);
  margin: 0;
  letter-spacing: -0.01em;
}

.exhausted-card-desc {
  font-size: 0.82rem;
  color: var(--text-muted, #9ca3af);
  line-height: 1.45;
  margin: 0 0 0.5rem 0;
}

.exhausted-btn-row {
  display: flex;
  gap: 0.65rem;
  margin-top: 0.25rem;
}

.exhausted-retry-btn,
.exhausted-pick-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.55rem 0.95rem;
  border-radius: 9999px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.exhausted-retry-btn {
  background: #f59e0b;
  color: #000000;
  border: none;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25);
}

.exhausted-retry-btn:hover {
  background: #d97706;
  transform: translateY(-1px);
}

.exhausted-pick-btn {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.exhausted-pick-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  transform: translateY(-1px);
}
</style>
