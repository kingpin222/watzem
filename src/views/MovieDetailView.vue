<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import {
  MediaItem,
  MediaMode,
  MediaDetails,
  CastMember,
  TVSeason,
  TVEpisode,
  CreditsResponse,
  SeasonDetailsResponse
} from '../types';
import { useApi } from '../composables/useApi';
import { useWatchlist } from '../composables/useWatchlist';
import { useHaptics } from '../composables/useHaptics';
import { useTvMode } from '../composables/useTvMode';
import { ApiCache } from '../utils/apiCache';
import {
  ArrowLeft,
  X,
  Star,
  Bookmark,
  Users,
  Clock,
  Clapperboard,
  Tv,
  Tag,
  Play,
  Info,
  BookOpen,
  ArrowUp,
  Search
} from 'lucide-vue-next';
import StreamPlayer from '../components/StreamPlayer.vue';
import { tvSpatialNav } from '../services/tvSpatialNav';

const props = withDefaults(
  defineProps<{
    item: MediaItem;
    mode: MediaMode;
    initialPlay?: boolean;
  }>(),
  {
    initialPlay: false
  }
);

const emit = defineEmits<{
  (e: 'back'): void;
  (e: 'close'): void;
  (e: 'search', query: string): void;
  (e: 'search-actor', actor: { id: number; name: string; profile_path: string | null }): void;
}>();

const {
  getImageUrl,
  getFullDetails,
  getTvDetails,
  getSeasonDetails,
  getCredits,
  getMediaImages,
  getMediaVideos
} = useApi();
const { isInWatchlist, toggleWatchlist } = useWatchlist();
const { triggerMedium, triggerLight } = useHaptics();
const { isTvMode } = useTvMode();

// TMDB Standard Genre Map for instant frame-0 genre display without waiting for network
const TMDB_GENRE_MAP: Record<number, string> = {
  28: 'Action',
  12: 'Adventure',
  16: 'Animation',
  35: 'Comedy',
  80: 'Crime',
  99: 'Documentary',
  58: 'Drama',
  10751: 'Family',
  14: 'Fantasy',
  36: 'History',
  27: 'Horror',
  10402: 'Music',
  9648: 'Mystery',
  10749: 'Romance',
  878: 'Sci-Fi',
  10770: 'TV Movie',
  53: 'Thriller',
  10752: 'War',
  37: 'Western',
  10759: 'Action & Adventure',
  10762: 'Kids',
  10763: 'News',
  10764: 'Reality',
  10765: 'Sci-Fi & Fantasy',
  10766: 'Soap',
  10767: 'Talk',
  10768: 'War & Politics'
};

const title = computed(() => props.item?.title || props.item?.name || 'Details');

// Banner uses the list poster or backdrop image with fast w780 resolution
const bannerUrl = computed(() => {
  if (!props.item) return '';
  const path = props.item.backdrop_path || props.item.poster_path;
  return getImageUrl(path, 'w780');
});

const handleBannerError = (e: Event) => {
  const img = e.target as HTMLImageElement;
  if (!img || !props.item) return;
  if (props.item.poster_path && !img.dataset.triedPoster) {
    img.dataset.triedPoster = 'true';
    img.src = getImageUrl(props.item.poster_path, 'w780');
  } else if (!img.dataset.triedFallback) {
    img.dataset.triedFallback = 'true';
    img.src = 'https://image.tmdb.org/t/p/w780/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg';
  }
};

const isSaved = computed(() => (props.item?.id ? isInWatchlist(props.item.id) : false));

// Determine whether this is a tv or movie context
const itemMode = computed<'movie' | 'tv'>(() => {
  if (!props.item) return props.mode === 'tv' ? 'tv' : 'movie';
  if (props.item.media_type === 'tv') return 'tv';
  if (props.item.media_type === 'movie') return 'movie';
  if (props.item.first_air_date && !props.item.release_date) return 'tv';
  if (props.item.name && !props.item.title) return 'tv';
  if ((props.item as any).number_of_seasons) return 'tv';
  if (details.value?.number_of_seasons) return 'tv';
  return props.mode === 'tv' ? 'tv' : 'movie';
});

// Enriched details
const details = ref<MediaDetails | null>(null);
const isLoadingDetails = ref(true);

// Cast
const cast = ref<CastMember[]>([]);
const isLoadingCast = ref(true);

// Derived display values — fallback to item if details not yet loaded
const rating = computed(() => {
  const raw = details.value?.vote_average ?? props.item?.vote_average;
  const num = typeof raw === 'string' ? parseFloat(raw) : Number(raw);
  if (!isNaN(num) && num > 0) {
    return num.toFixed(1);
  }
  return null;
});
const displayRating = computed(() => rating.value || 'NR');
const year = computed(() => {
  const d = details.value?.release_date || details.value?.first_air_date
    || props.item?.release_date || props.item?.first_air_date;
  return d ? d.substring(0, 4) : '';
});
const storyline = computed(() =>
  details.value?.overview || props.item?.overview || 'No synopsis available for this title.'
);
const tagline = computed(() => details.value?.tagline || null);
const runtime = computed(() => {
  if (details.value?.runtime) {
    const h = Math.floor(details.value.runtime / 60);
    const m = details.value.runtime % 60;
    return h > 0 ? `${h}h ${m}m` : `${m}m`;
  }
  const ep = details.value?.episode_run_time?.[0];
  if (ep) return `~${ep}m / ep`;
  return null;
});
const genres = computed(() => {
  if (details.value?.genres?.length) {
    return details.value.genres.map((g) => g.name);
  }
  if (props.item?.genre_ids?.length) {
    return props.item.genre_ids.map((id) => TMDB_GENRE_MAP[id]).filter(Boolean) as string[];
  }
  return [];
});
const keywords = computed(() => {
  const kw = details.value?.keywords;
  const arr = kw?.keywords || kw?.results || [];
  return arr.slice(0, 16).map((k) => k.name);
});

const handleKeywordClick = (kw: string) => {
  triggerLight();
  emit('search', kw);
};

const handleActorClick = (actor: CastMember) => {
  triggerLight();
  emit('search-actor', {
    id: actor.id,
    name: actor.name,
    profile_path: actor.profile_path
  });
};
const directors = computed(() => details.value?.directors ?? []);
const writers = computed(() => details.value?.writers ?? []);
const createdBy = computed(() => details.value?.created_by?.map((c) => c.name) ?? []);
const status = computed(() => details.value?.status || null);
const language = computed(() => {
  const lang = details.value?.original_language;
  if (!lang) return null;
  try {
    return new Intl.DisplayNames(['en'], { type: 'language' }).of(lang) || lang.toUpperCase();
  } catch {
    return lang.toUpperCase();
  }
});
const networks = computed(() => details.value?.networks?.map((n) => n.name) ?? []);

const budget = computed(() => {
  const b = details.value?.budget;
  if (!b || b === 0) return null;
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', notation: 'compact', maximumFractionDigits: 1 }).format(b);
});
const revenue = computed(() => {
  const r = details.value?.revenue;
  if (!r || r === 0) return null;
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', notation: 'compact', maximumFractionDigits: 1 }).format(r);
});

// ── Play Media State ────────────────────────────────────────────────────────
const isPlayingMedia = ref(props.initialPlay);
const streamPlayerRef = ref<any>(null);

// ── Header Sliding Images (TMDB Backdrops) ──────────────────────────────────
const initialBackdrop = bannerUrl.value || (props.item ? getImageUrl(props.item.backdrop_path || props.item.poster_path, 'w1280') : '');
const slideImages = ref<string[]>(initialBackdrop ? [initialBackdrop] : []);
const slideIndex = ref(0);
let slideTimer: ReturnType<typeof setInterval> | null = null;

const preloadImage = (url: string) => {
  if (!url) return;
  const img = new Image();
  img.src = url;
};

const startSlideshow = () => {
  if (slideTimer) clearInterval(slideTimer);
  if (slideImages.value.length < 2) return;
  slideTimer = setInterval(() => {
    const nextIdx = (slideIndex.value + 1) % slideImages.value.length;
    const peekIdx = (nextIdx + 1) % slideImages.value.length;
    if (slideImages.value[peekIdx]) {
      preloadImage(slideImages.value[peekIdx]);
    }
    slideIndex.value = nextIdx;
  }, 4800);
};

const stopSlideshow = () => {
  if (slideTimer) {
    clearInterval(slideTimer);
    slideTimer = null;
  }
};

const handleWindowScroll = () => {
  if (window.scrollY > 100) {
    stopSlideshow();
  } else if (!slideTimer && slideImages.value.length > 1) {
    startSlideshow();
  }
};

const loadSlideImages = async () => {
  if (!props.item?.id) return;
  try {
    const imgs = await getMediaImages(props.item.id, itemMode.value);
    if (imgs && imgs.length > 0) {
      const seed = bannerUrl.value || getImageUrl(props.item.backdrop_path || props.item.poster_path, 'w1280');
      const deduped = [seed, ...imgs.filter((u) => u !== seed)].filter(Boolean);
      deduped.slice(1, 3).forEach((u) => preloadImage(u));
      slideImages.value = deduped.slice(0, 3);
      startSlideshow();
    }
  } catch {
    // Keep initial backdrop
  }
};

// ── Trailer ──────────────────────────────────────────────────────────────────
const trailerKey = ref<string | null>(null);
const showTrailerModal = ref(false);

const loadTrailer = async () => {
  if (trailerKey.value || !props.item?.id) return;
  try {
    const videos = await getMediaVideos(props.item.id, itemMode.value);
    const pick =
      videos.find((v) => v.type === 'Trailer' && v.name.toLowerCase().includes('official')) ||
      videos.find((v) => v.type === 'Trailer') ||
      videos.find((v) => v.type === 'Teaser') ||
      videos[0] ||
      null;
    trailerKey.value = pick ? pick.key : null;
  } catch {
    trailerKey.value = null;
  }
};

const openTrailer = () => {
  triggerMedium();
  if (trailerKey.value) {
    showTrailerModal.value = true;
  }
};

const closeTrailer = () => {
  triggerLight();
  showTrailerModal.value = false;
};

const trailerUrl = computed(() =>
  trailerKey.value
    ? `https://www.youtube.com/embed/${trailerKey.value}?autoplay=1&rel=0&modestbranding=1`
    : ''
);

const handleWatchClick = () => {
  triggerMedium();
  isPlayingMedia.value = true;
  setTimeout(() => {
    if (streamPlayerRef.value?.startPlaying) {
      streamPlayerRef.value.startPlaying(isTvMode.value ? true : false);
    }
    const playerWrap = document.querySelector('.player-wrapper');
    if (playerWrap) {
      playerWrap.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }, 50);
};

const handlePlayerClose = () => {
  isPlayingMedia.value = false;
};

// TV Seasons & Episodes Guide State
const activeSeasonNumber = ref(1);
const activeEpisodeNumber = ref(1);
const currentPlayingSeason = ref(1);
const tvSeasons = ref<TVSeason[]>(
  itemMode.value === 'tv'
    ? [{ id: 1, season_number: 1, name: 'Season 1', episode_count: 0, overview: '', poster_path: null }]
    : []
);
const episodesList = ref<TVEpisode[]>([]);
const episodesCache = ref<Record<number, TVEpisode[]>>({});
const isLoadingEpisodes = ref(itemMode.value === 'tv');

const loadSeasonEpisodes = async (seasonNum: number) => {
  if (!props.item?.id) return;

  // 1. Instant check from in-memory cache
  if (episodesCache.value[seasonNum] && episodesCache.value[seasonNum].length > 0) {
    episodesList.value = episodesCache.value[seasonNum];
    isLoadingEpisodes.value = false;
    return;
  }

  // 2. Instant check from persistent ApiCache
  const cached = ApiCache.get<SeasonDetailsResponse>(`season_details:${props.item.id}:${seasonNum}`);
  if (cached?.episodes && cached.episodes.length > 0) {
    episodesList.value = cached.episodes;
    episodesCache.value[seasonNum] = cached.episodes;
    isLoadingEpisodes.value = false;
    return;
  }

  if (episodesList.value.length === 0) {
    isLoadingEpisodes.value = true;
  }

  try {
    const data = await getSeasonDetails(props.item.id, seasonNum);
    const eps = data.episodes || [];
    episodesList.value = eps;
    episodesCache.value[seasonNum] = eps;
  } catch (err) {
    console.error('Failed to load season episodes:', err);
    if (!episodesCache.value[seasonNum]) {
      episodesList.value = [];
    }
  } finally {
    isLoadingEpisodes.value = false;
  }
};

const changeSeason = (seasonNum: number) => {
  triggerLight();
  activeSeasonNumber.value = seasonNum;
  loadSeasonEpisodes(seasonNum);
};

const playEpisode = (seasonNum: number, episodeNum: number) => {
  triggerMedium();
  activeSeasonNumber.value = seasonNum;
  activeEpisodeNumber.value = episodeNum;
  currentPlayingSeason.value = seasonNum;
  isPlayingMedia.value = true;
  setTimeout(() => {
    if (streamPlayerRef.value?.playEpisode) {
      streamPlayerRef.value.playEpisode(seasonNum, episodeNum);
    }
    const playerWrap = document.querySelector('.player-wrapper');
    if (playerWrap) {
      playerWrap.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }, 50);
};

const handlePlayerEpisodeChange = (payload: { season: number; episode: number }) => {
  activeSeasonNumber.value = payload.season;
  activeEpisodeNumber.value = payload.episode;
  currentPlayingSeason.value = payload.season;
  if (episodesList.value.length === 0 || activeSeasonNumber.value !== payload.season) {
    loadSeasonEpisodes(payload.season);
  }
};

/**
 * Synchronous hydration from ApiCache to render content on frame 0 with ZERO flicker
 */
const initFromCache = () => {
  if (!props.item?.id) return;
  const id = props.item.id;
  const mode = itemMode.value;

  // 1. Details
  const cachedDetails = ApiCache.get<MediaDetails>(`full_details:${mode}:${id}`);
  if (cachedDetails) {
    details.value = cachedDetails;
    isLoadingDetails.value = false;
    if (mode === 'tv') {
      const sList = (cachedDetails.seasons || []).filter((s) => s.season_number > 0);
      if (sList.length > 0) {
        tvSeasons.value = sList;
      }
    }
  }

  // 2. Cast
  const cachedCredits = ApiCache.get<CreditsResponse>(`credits:${mode}:${id}`);
  if (cachedCredits?.cast && cachedCredits.cast.length > 0) {
    cast.value = cachedCredits.cast.slice(0, 15);
    isLoadingCast.value = false;
  }

  // 3. Images
  const cachedImages = ApiCache.get<string[]>(`media_images:${mode}:${id}`);
  if (cachedImages && cachedImages.length > 0) {
    const seed = bannerUrl.value || getImageUrl(props.item.backdrop_path || props.item.poster_path, 'w1280');
    const deduped = [seed, ...cachedImages.filter((u) => u !== seed)].filter(Boolean);
    slideImages.value = deduped.slice(0, 3);
    startSlideshow();
  }

  // 4. Videos / Trailer
  const cachedVideos = ApiCache.get<{ key: string; name: string; type: string; site: string }[]>(`media_videos:${mode}:${id}`);
  if (cachedVideos) {
    const pick =
      cachedVideos.find((v) => v.type === 'Trailer' && v.name.toLowerCase().includes('official')) ||
      cachedVideos.find((v) => v.type === 'Trailer') ||
      cachedVideos.find((v) => v.type === 'Teaser') ||
      cachedVideos[0] ||
      null;
    trailerKey.value = pick ? pick.key : null;
  }

  // 5. TV Season 1 Episodes
  if (mode === 'tv') {
    const cachedSeason = ApiCache.get<SeasonDetailsResponse>(`season_details:${id}:1`);
    if (cachedSeason?.episodes && cachedSeason.episodes.length > 0) {
      episodesList.value = cachedSeason.episodes;
      episodesCache.value[1] = cachedSeason.episodes;
      isLoadingEpisodes.value = false;
    }
  }
};

initFromCache();

const loadAll = async () => {
  if (!props.item?.id) return;

  initFromCache();
  loadSlideImages();
  loadTrailer();

  if (!details.value) {
    isLoadingDetails.value = true;
  }
  if (cast.value.length === 0) {
    isLoadingCast.value = true;
  }

  const [detailsRes, creditsRes] = await Promise.allSettled([
    getFullDetails(props.item.id, itemMode.value),
    getCredits(props.item.id, itemMode.value)
  ]);

  if (detailsRes.status === 'fulfilled') {
    details.value = detailsRes.value;
    if (itemMode.value === 'tv') {
      const sList = (detailsRes.value.seasons || []).filter((s) => s.season_number > 0);
      if (sList.length > 0) {
        tvSeasons.value = sList;
        if (!activeSeasonNumber.value || !sList.some((s) => s.season_number === activeSeasonNumber.value)) {
          activeSeasonNumber.value = sList[0].season_number;
          currentPlayingSeason.value = sList[0].season_number;
        }
        loadSeasonEpisodes(activeSeasonNumber.value);
      } else {
        try {
          const tvInfo = await getTvDetails(props.item.id);
          const filtered = (tvInfo.seasons || []).filter((s) => s.season_number > 0);
          if (filtered.length > 0) {
            tvSeasons.value = filtered;
            activeSeasonNumber.value = filtered[0].season_number;
            currentPlayingSeason.value = filtered[0].season_number;
            loadSeasonEpisodes(activeSeasonNumber.value);
          }
        } catch {}
      }
    }
  } else if (itemMode.value === 'tv') {
    try {
      const tvInfo = await getTvDetails(props.item.id);
      const filtered = (tvInfo.seasons || []).filter((s) => s.season_number > 0);
      if (filtered.length > 0) {
        tvSeasons.value = filtered;
        activeSeasonNumber.value = filtered[0].season_number;
        currentPlayingSeason.value = filtered[0].season_number;
        loadSeasonEpisodes(activeSeasonNumber.value);
      }
    } catch {}
  }
  isLoadingDetails.value = false;

  if (creditsRes.status === 'fulfilled') {
    cast.value = (creditsRes.value.cast || []).slice(0, 15);
  }
  isLoadingCast.value = false;
};

const forceScrollTop = () => {
  if (typeof window !== 'undefined') {
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
    document.documentElement.scrollTop = 0;
    document.body.scrollTop = 0;
    const appEl = document.getElementById('app');
    if (appEl) appEl.scrollTop = 0;
    const mainEl = document.querySelector('.main-content');
    if (mainEl) mainEl.scrollTop = 0;
  }
};

/**
 * Full state reset whenever a new movie/show is selected
 */
const resetDetailsState = () => {
  stopSlideshow();
  isPlayingMedia.value = props.initialPlay;
  showTrailerModal.value = false;
  trailerKey.value = null;
  activeSeasonNumber.value = 1;
  activeEpisodeNumber.value = 1;
  currentPlayingSeason.value = 1;
  episodesList.value = [];
  episodesCache.value = {};
  tvSeasons.value = itemMode.value === 'tv'
    ? [{ id: 1, season_number: 1, name: 'Season 1', episode_count: 0, overview: '', poster_path: null }]
    : [];
  details.value = null;
  cast.value = [];
  isLoadingDetails.value = true;
  isLoadingCast.value = true;
  isLoadingEpisodes.value = itemMode.value === 'tv';
  slideIndex.value = 0;

  const backdrop = bannerUrl.value || (props.item ? getImageUrl(props.item.backdrop_path || props.item.poster_path, 'w1280') : '');
  slideImages.value = backdrop ? [backdrop] : [];

  forceScrollTop();
};

onMounted(() => {
  resetDetailsState();
  forceScrollTop();
  requestAnimationFrame(forceScrollTop);
  setTimeout(forceScrollTop, 50);
  setTimeout(forceScrollTop, 150);
  setTimeout(forceScrollTop, 280);
  initFromCache();
  loadAll();
  window.addEventListener('scroll', handleWindowScroll, { passive: true });
});

onUnmounted(() => {
  stopSlideshow();
  isPlayingMedia.value = false;
  showTrailerModal.value = false;
  window.removeEventListener('scroll', handleWindowScroll);
});

watch(
  () => props.item?.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      resetDetailsState();
      forceScrollTop();
      requestAnimationFrame(forceScrollTop);
      setTimeout(forceScrollTop, 50);
      setTimeout(forceScrollTop, 150);
      initFromCache();
      loadAll();
    }
  }
);

watch(
  () => props.initialPlay,
  (val) => {
    isPlayingMedia.value = val;
  }
);

const isPoppingBookmark = ref(false);

const handleBookmark = () => {
  if (!props.item) return;
  triggerMedium();
  toggleWatchlist(props.item);
  isPoppingBookmark.value = true;
  setTimeout(() => {
    isPoppingBookmark.value = false;
  }, 400);
};

const handleBack = () => {
  triggerLight();
  stopSlideshow();
  isPlayingMedia.value = false;
  showTrailerModal.value = false;
  emit('back');
  emit('close');
};

// ── Section Anchor Navigation for D-pad & Continuous Page Scrolling ─────────
const activeSection = ref('overview');

const scrollToSection = (sectionId: string) => {
  activeSection.value = sectionId.replace('section-', '');
  const el = document.getElementById(sectionId);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    setTimeout(() => {
      const focusable = el.querySelector<HTMLElement>('button, [tabindex="0"], a');
      if (focusable) {
        tvSpatialNav.focusElement(focusable);
      }
    }, 180);
  }
};

const scrollToTop = () => {
  activeSection.value = 'overview';
  window.scrollTo({ top: 0, left: 0, behavior: 'smooth' });
  const appEl = document.getElementById('app');
  if (appEl) appEl.scrollTo({ top: 0, behavior: 'smooth' });
  const mainEl = document.querySelector('.main-content');
  if (mainEl) mainEl.scrollTo({ top: 0, behavior: 'smooth' });
  setTimeout(() => {
    const watchBtn = document.querySelector<HTMLElement>('.cta-watch-btn');
    if (watchBtn) {
      tvSpatialNav.focusElement(watchBtn);
    }
  }, 180);
};
</script>

<template>
  <div class="movie-detail-page">
    <!-- Sticky / Floating Navigation Controls over Header -->
    <header class="page-floating-nav">
      <button class="circle-nav-btn back-btn" aria-label="Go Back" @click="handleBack">
        <ArrowLeft :size="20" stroke-width="2.5" />
      </button>
      <div class="nav-actions">
        <button
          class="circle-nav-btn bookmark-btn"
          :class="{ saved: isSaved, 'animate-pop-bounce': isPoppingBookmark }"
          :title="isSaved ? 'Remove from Watchlist' : 'Add to Watchlist'"
          @click="handleBookmark"
        >
          <Bookmark :size="18" :fill="isSaved ? 'currentColor' : 'none'" />
        </button>
      </div>
    </header>

    <!-- Cinematic Hero Banner -->
    <div class="page-banner">
      <div class="banner-slideshow">
        <img
          v-if="bannerUrl"
          :src="bannerUrl"
          :alt="title"
          class="banner-base-img"
          @error="handleBannerError"
        />
        <img
          v-for="(src, i) in slideImages"
          :key="src"
          :src="src"
          :alt="title"
          class="banner-slide-img"
          :class="{ active: i === slideIndex }"
        />
      </div>
      <div class="banner-gradient"></div>

      <!-- Banner Info Overlay -->
      <div class="banner-info-wrap">
        <div class="banner-info-content">
          <div class="meta-row">
            <span class="badge-mode" :class="{ 'tv-theme': itemMode === 'tv' }">
              {{ itemMode === 'tv' ? 'TV SERIES' : 'MOVIE' }}
            </span>
            <span class="meta-rating font-mono">
              <Star :size="12" fill="currentColor" class="star-icon" />{{ displayRating }}
            </span>
            <span v-if="year" class="meta-chip font-mono">{{ year }}</span>
            <span v-if="runtime" class="meta-chip font-mono runtime-chip">
              <Clock :size="11" />{{ runtime }}
            </span>
          </div>
          <h1 class="page-title font-display">{{ title }}</h1>
          <p v-if="tagline" class="page-tagline">{{ tagline }}</p>

          <!-- Primary Call to Action Buttons -->
          <div class="banner-cta-row">
            <button class="cta-watch-btn" @click="handleWatchClick">
              <Play :size="16" fill="currentColor" />
              <span>{{ itemMode === 'tv' ? `Watch S${activeSeasonNumber}:E${activeEpisodeNumber}` : 'Watch Now' }}</span>
            </button>
            <button
              class="cta-bookmark-btn"
              :class="{ saved: isSaved, 'animate-pop-bounce': isPoppingBookmark }"
              :title="isSaved ? 'Remove from Watchlist' : 'Add to Watchlist'"
              @click="handleBookmark"
            >
              <Bookmark :size="15" :fill="isSaved ? 'currentColor' : 'none'" />
              <span>{{ isSaved ? 'Saved' : 'Watchlist' }}</span>
            </button>
            <button v-if="trailerKey" class="cta-trailer-btn" @click="openTrailer">
              <Clapperboard :size="15" />
              <span>Watch Trailer</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Page Body Container -->
    <div class="page-content-container">

      <!-- Stream Player (Mounted when playing) -->
      <div v-if="isPlayingMedia" class="player-wrapper">
        <div class="now-playing-bar">
          <div class="now-playing-label">
            <span class="pulse-playing-dot"></span>
            <span class="font-display">Now Playing: {{ itemMode === 'tv' ? `Season ${activeSeasonNumber}, Episode ${activeEpisodeNumber}` : title }}</span>
          </div>
          <button class="stop-playing-btn" title="Close Video" @click="handlePlayerClose">
            <X :size="13" />
            <span>Close Video</span>
          </button>
        </div>
        <StreamPlayer
          ref="streamPlayerRef"
          :item="item"
          :mode="itemMode"
          :auto-play="true"
          :initial-season="activeSeasonNumber"
          :initial-episode="activeEpisodeNumber"
          @episode-change="handlePlayerEpisodeChange"
          @close="handlePlayerClose"
        />
      </div>

      <!-- Section Anchor Navigation Bar for D-Pad & Quick Jump -->
      <nav class="detail-anchors-bar no-scrollbar" aria-label="Details Sections">
        <button
          class="detail-anchor-link font-mono"
          :class="{ active: activeSection === 'overview' }"
          @click="scrollToSection('section-overview')"
        >
          <BookOpen :size="13" />
          <span>Story</span>
        </button>

        <button
          v-if="cast.length"
          class="detail-anchor-link font-mono"
          :class="{ active: activeSection === 'cast' }"
          @click="scrollToSection('section-cast')"
        >
          <Users :size="13" />
          <span>Cast</span>
        </button>

        <button
          class="detail-anchor-link font-mono"
          :class="{ active: activeSection === 'details' }"
          @click="scrollToSection('section-details')"
        >
          <Info :size="13" />
          <span>Details</span>
        </button>

        <button
          v-if="keywords.length"
          class="detail-anchor-link font-mono"
          :class="{ active: activeSection === 'keywords' }"
          @click="scrollToSection('section-keywords')"
        >
          <Tag :size="13" />
          <span>Keywords</span>
        </button>

        <button
          v-if="itemMode === 'tv' && episodesList.length"
          class="detail-anchor-link font-mono"
          :class="{ active: activeSection === 'episodes' }"
          @click="scrollToSection('section-episodes')"
        >
          <Tv :size="13" />
          <span>Episodes</span>
        </button>
      </nav>

      <!-- Genres Row -->
      <div v-if="genres.length" class="genre-row">
        <span v-for="g in genres" :key="g" class="genre-chip font-mono">
          {{ g }}
        </span>
      </div>

      <!-- Storyline / Overview Section with Anchor -->
      <section id="section-overview" class="overview-box detail-section-card" tabindex="0" role="region" aria-label="Storyline overview">
        <div class="section-anchor-header">
          <div class="anchor-header-title">
            <BookOpen :size="14" class="section-icon" />
            <h2 class="section-heading font-display">Overview</h2>
          </div>
          <button
            v-if="cast.length"
            class="section-jump-link font-mono"
            title="Jump to Cast"
            @click="scrollToSection('section-cast')"
          >
            <span>Cast</span>
            <span class="jump-arrow">↓</span>
          </button>
        </div>
        <p class="overview-text">{{ storyline }}</p>
      </section>

      <!-- Cast Section with Anchor & D-Pad Focusable Cards -->
      <section id="section-cast" class="cast-section detail-section-card">
        <div class="section-anchor-header">
          <div class="anchor-header-title">
            <Users :size="14" class="cast-heading-icon" />
            <h2 class="section-heading font-display">Top Cast</h2>
          </div>
          <button
            class="section-jump-link font-mono"
            title="Jump to Details"
            @click="scrollToSection('section-details')"
          >
            <span>Details</span>
            <span class="jump-arrow">↓</span>
          </button>
        </div>
        <div v-if="isLoadingCast && cast.length === 0" class="cast-scroll no-scrollbar">
          <div v-for="i in 6" :key="i" class="cast-card-skeleton skeleton"></div>
        </div>
        <div v-else-if="cast.length" class="cast-scroll no-scrollbar">
          <button
            v-for="actor in cast"
            :key="actor.id"
            class="cast-card"
            tabindex="0"
            role="button"
            :title="`Search movies starring ${actor.name}`"
            :aria-label="`Search movies starring ${actor.name}`"
            @click="handleActorClick(actor)"
          >
            <div class="cast-photo-wrap">
              <img
                v-if="actor.profile_path"
                :src="getImageUrl(actor.profile_path, 'w185')"
                :alt="actor.name"
                class="cast-photo"
                loading="lazy"
              />
              <div v-else class="cast-photo-placeholder font-mono">
                {{ actor.name.slice(0, 2).toUpperCase() }}
              </div>
              <div class="cast-search-overlay">
                <Search :size="14" class="cast-search-icon" />
              </div>
            </div>
            <div class="cast-info">
              <span class="cast-name">{{ actor.name }}</span>
              <span v-if="actor.character" class="cast-character">{{ actor.character }}</span>
            </div>
          </button>
        </div>
        <p v-else class="text-xs text-muted">No cast information available.</p>
      </section>

      <!-- Detailed Info Grid with Anchor & D-Pad Focusable Rows -->
      <section id="section-details" class="info-section detail-section-card">
        <div class="section-anchor-header">
          <div class="anchor-header-title">
            <Info :size="14" class="section-icon" />
            <h2 class="section-heading font-display">Media Details</h2>
          </div>
          <button
            class="section-jump-link font-mono"
            :title="keywords.length ? 'Jump to Keywords' : (itemMode === 'tv' ? 'Jump to Episodes' : 'Back to Top')"
            @click="scrollToSection(keywords.length ? 'section-keywords' : (itemMode === 'tv' ? 'section-episodes' : 'section-overview'))"
          >
            <span>{{ keywords.length ? 'Keywords' : (itemMode === 'tv' ? 'Episodes' : 'Top') }}</span>
            <span class="jump-arrow">{{ keywords.length || itemMode === 'tv' ? '↓' : '↑' }}</span>
          </button>
        </div>
        <div class="info-grid">
          <div v-if="directors.length" class="info-row info-row-focusable" tabindex="0">
            <span class="info-label font-mono">Director</span>
            <span class="info-value">{{ directors.join(', ') }}</span>
          </div>
          <div v-if="writers.length" class="info-row info-row-focusable" tabindex="0">
            <span class="info-label font-mono">Writers</span>
            <span class="info-value">{{ writers.join(', ') }}</span>
          </div>
          <div v-if="createdBy.length" class="info-row info-row-focusable" tabindex="0">
            <span class="info-label font-mono">Creator</span>
            <span class="info-value">{{ createdBy.join(', ') }}</span>
          </div>
          <div v-if="status" class="info-row info-row-focusable" tabindex="0">
            <span class="info-label font-mono">Status</span>
            <span class="info-value">{{ status }}</span>
          </div>
          <div v-if="language" class="info-row info-row-focusable" tabindex="0">
            <span class="info-label font-mono">Language</span>
            <span class="info-value">{{ language }}</span>
          </div>
          <div v-if="budget" class="info-row info-row-focusable" tabindex="0">
            <span class="info-label font-mono">Budget</span>
            <span class="info-value font-mono">{{ budget }}</span>
          </div>
          <div v-if="revenue" class="info-row info-row-focusable" tabindex="0">
            <span class="info-label font-mono">Box Office</span>
            <span class="info-value font-mono">{{ revenue }}</span>
          </div>
          <div v-if="networks.length" class="info-row info-row-focusable" tabindex="0">
            <span class="info-label font-mono">Network</span>
            <span class="info-value">{{ networks.join(', ') }}</span>
          </div>
        </div>
      </section>

      <!-- Keywords Section with Anchor & D-Pad Focusable Chips -->
      <section v-if="keywords.length" id="section-keywords" class="keyword-section detail-section-card">
        <div class="section-anchor-header">
          <div class="anchor-header-title">
            <Tag :size="13" class="section-icon" />
            <h2 class="section-heading font-display">Keywords</h2>
          </div>
          <button
            class="section-jump-link font-mono"
            :title="itemMode === 'tv' ? 'Jump to Episodes' : 'Back to Top'"
            @click="scrollToSection(itemMode === 'tv' ? 'section-episodes' : 'section-overview')"
          >
            <span>{{ itemMode === 'tv' ? 'Episodes' : 'Top' }}</span>
            <span class="jump-arrow">{{ itemMode === 'tv' ? '↓' : '↑' }}</span>
          </button>
        </div>
        <div class="keyword-row">
          <button
            v-for="kw in keywords"
            :key="kw"
            class="keyword-chip font-mono"
            tabindex="0"
            :title="`Search titles for #${kw}`"
            @click="handleKeywordClick(kw)"
          >
            <Search :size="11" class="kw-search-icon" />
            <span>#{{ kw }}</span>
          </button>
        </div>
      </section>

      <!-- TV Seasons & Episodes Guide Section with Anchor (At the bottom of the page) -->
      <section v-if="itemMode === 'tv'" id="section-episodes" class="episodes-section detail-section-card">
        <div class="episodes-section-header">
          <div class="episodes-header-left">
            <Tv :size="16" class="section-icon" />
            <h2 class="section-heading font-display">Episodes</h2>
            <span v-if="episodesList.length > 0" class="episodes-count-badge font-mono">
              {{ episodesList.length }} EPISODES
            </span>
          </div>

          <!-- Quick Jump to Top -->
          <button class="section-jump-link font-mono" title="Back to Top" @click="scrollToTop">
            <span>Top</span>
            <span class="jump-arrow">↑</span>
          </button>

          <!-- Season Selector Tabs -->
          <div v-if="tvSeasons.length > 1" class="season-tabs-wrap no-scrollbar">
            <button
              v-for="s in tvSeasons"
              :key="s.id"
              class="season-tab-btn"
              :class="{ active: s.season_number === activeSeasonNumber }"
              @click="changeSeason(s.season_number)"
            >
              {{ s.name || `Season ${s.season_number}` }}
            </button>
          </div>
          <div v-else-if="tvSeasons.length === 1" class="single-season-label font-mono">
            {{ tvSeasons[0].name || 'Season 1' }}
          </div>
        </div>

        <!-- Episodes List -->
        <div v-if="episodesList.length > 0" class="episodes-list">
          <div
            v-for="ep in episodesList"
            :key="ep.id"
            class="episode-card"
            :class="{ 'now-playing': isPlayingMedia && currentPlayingSeason === activeSeasonNumber && activeEpisodeNumber === ep.episode_number }"
            tabindex="0"
            role="button"
            @click="playEpisode(activeSeasonNumber, ep.episode_number)"
          >
            <!-- Lightweight Episode Badge / Play Action -->
            <div class="ep-badge-box">
              <span class="ep-badge-num font-mono">E{{ ep.episode_number }}</span>
              <div class="ep-badge-play">
                <Play :size="18" fill="currentColor" />
              </div>
            </div>

            <!-- Episode Details -->
            <div class="episode-details">
              <div class="ep-header-line">
                <span class="ep-title font-display">{{ ep.name || `Episode ${ep.episode_number}` }}</span>
                <span v-if="ep.vote_average && ep.vote_average > 0" class="ep-rating font-mono">
                  <Star :size="9" fill="currentColor" />{{ ep.vote_average.toFixed(1) }}
                </span>
              </div>
              <div class="ep-meta-line font-mono">
                <span v-if="ep.air_date">{{ ep.air_date }}</span>
                <span v-if="ep.runtime" class="ep-runtime">• {{ ep.runtime }}m</span>
                <div v-if="isPlayingMedia && currentPlayingSeason === activeSeasonNumber && activeEpisodeNumber === ep.episode_number" class="ep-playing-indicator">
                  <span class="pulse-dot"></span>
                  <span>PLAYING</span>
                </div>
              </div>
              <p v-if="ep.overview" class="ep-overview">{{ ep.overview }}</p>
            </div>

            <!-- Quick Play Button -->
            <button class="ep-quick-play-btn" title="Play Episode">
              <Play :size="13" fill="currentColor" />
            </button>
          </div>
        </div>

        <!-- Skeleton Loading for Episodes -->
        <div v-else-if="isLoadingEpisodes" class="episodes-skeleton-list">
          <div v-for="i in 5" :key="i" class="episode-skeleton-item skeleton">
            <div class="ep-badge-skel skeleton"></div>
            <div class="ep-text-skel">
              <div class="skel-line w-40 mb-1"></div>
              <div class="skel-line w-20"></div>
            </div>
          </div>
        </div>

        <div v-else class="episodes-empty-state">
          <span>No episode information found for Season {{ activeSeasonNumber }}.</span>
        </div>
      </section>

      <!-- Back to Top Footer Anchor -->
      <div class="detail-bottom-anchor-wrap">
        <button class="detail-back-to-top-btn font-mono" @click="scrollToTop">
          <ArrowUp :size="15" />
          <span>Back to Top</span>
        </button>
      </div>
    </div>

    <!-- YouTube Trailer Modal Overlay -->
    <Teleport to="body">
      <Transition name="trailer-fade">
        <div v-if="showTrailerModal" class="trailer-overlay" @click.self="closeTrailer">
          <div class="trailer-frame-wrap">
            <button class="trailer-close-btn" aria-label="Close Trailer" @click="closeTrailer">
              <X :size="20" />
            </button>
            <iframe
              v-if="trailerUrl"
              :src="trailerUrl"
              title="Official Trailer"
              class="trailer-iframe"
              sandbox="allow-scripts allow-same-origin allow-forms allow-presentation"
              allow="autoplay; encrypted-media; fullscreen"
              allowfullscreen
            ></iframe>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.movie-detail-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  background: var(--bg-surface);
  color: var(--text-primary);
  overflow-x: hidden;
}

/* Floating Navigation Controls */
.page-floating-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 60;
  padding: 0.5rem max(1rem, var(--safe-right, 0px)) 0.5rem max(1rem, var(--safe-left, 0px));
  display: flex;
  align-items: center;
  justify-content: space-between;
  pointer-events: none;
  transition: padding 0.2s ease;
}

.circle-nav-btn {
  pointer-events: auto;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: rgba(14, 14, 22, 0.88);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: var(--text-primary);
  display: grid;
  place-items: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.65);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  user-select: none;
}

@media (hover: hover) and (pointer: fine) {
  .circle-nav-btn:hover {
    background: rgba(26, 26, 38, 0.95);
    border-color: rgba(255, 255, 255, 0.3);
  }
}

.circle-nav-btn.bookmark-btn.saved {
  color: #fbbf24;
  border-color: rgba(245, 158, 11, 0.4);
}

/* Banner */
.page-banner {
  position: relative;
  width: 100%;
  height: 380px;
  background: radial-gradient(circle at center, #1b1b29 0%, #0d0d16 65%, #07070a 100%);
  overflow: hidden;
  contain: paint;
}

@media (min-width: 768px) {
  .page-banner {
    height: 460px;
  }
}

.banner-slideshow {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #08080c;
}

.banner-base-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 25%;
  z-index: 0;
}

.banner-slide-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 25%;
  opacity: 0;
  transition: opacity 0.4s ease-in-out;
  z-index: 0;
}

.banner-slide-img.active {
  opacity: 1;
  z-index: 1;
}

.banner-gradient {
  position: absolute;
  inset: 0;
  z-index: 2;
  background: linear-gradient(
    to top,
    var(--bg-surface) 0%,
    rgba(14, 14, 21, 0.65) 45%,
    rgba(0, 0, 0, 0.5) 80%,
    rgba(0, 0, 0, 0.75) 100%
  );
}

/* Banner Info Wrap */
.banner-info-wrap {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 3;
  padding: 1.5rem 1.25rem;
}

.banner-info-content {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.badge-mode {
  font-size: 0.62rem;
  font-weight: 800;
  padding: 2px 7px;
  border-radius: 4px;
  background: var(--accent-amber);
  color: #07070a;
  letter-spacing: 0.5px;
}
.badge-mode.tv-theme {
  background: var(--accent-rose);
  color: #fff;
}

.meta-chip {
  font-size: 0.72rem;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 3px;
}
.meta-rating {
  font-size: 0.76rem;
  font-weight: 700;
  color: #fbbf24;
  display: flex;
  align-items: center;
  gap: 4px;
}
.star-icon {
  color: #fbbf24;
}

.page-title {
  font-size: 1.6rem;
  font-weight: 800;
  line-height: 1.18;
  color: var(--text-primary);
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.9);
}

@media (min-width: 768px) {
  .page-title {
    font-size: 2.2rem;
  }
}

.page-tagline {
  font-size: 0.8rem;
  font-style: italic;
  color: rgba(255, 255, 255, 0.75);
  margin-top: 0.1rem;
}

/* Call to Action Buttons */
.banner-cta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 0.5rem;
  flex-wrap: wrap;
}

.cta-watch-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 999px;
  background: var(--accent-amber);
  color: #07070a;
  font-size: 0.86rem;
  font-weight: 800;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.4);
}

@media (hover: hover) and (pointer: fine) {
  .cta-watch-btn:hover {
    box-shadow: 0 6px 20px rgba(245, 158, 11, 0.6);
    filter: brightness(1.08);
  }
}

.cta-watch-btn:active {
  transform: scale(0.98);
}

.cta-trailer-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 16px;
  border-radius: 999px;
  background: rgba(28, 28, 40, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.22);
  color: #ffffff;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
}

@media (hover: hover) and (pointer: fine) {
  .cta-trailer-btn:hover {
    background: rgba(45, 45, 60, 0.95);
    border-color: rgba(255, 255, 255, 0.4);
  }
}

.cta-bookmark-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 15px;
  border-radius: 999px;
  background: rgba(24, 24, 34, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
}

@media (hover: hover) and (pointer: fine) {
  .cta-bookmark-btn:hover {
    background: rgba(38, 38, 52, 0.95);
    color: #fff;
  }
}

.cta-bookmark-btn.saved {
  background: rgba(245, 158, 11, 0.2);
  border-color: rgba(245, 158, 11, 0.5);
  color: #fbbf24;
}

/* Page Body Container */
.page-content-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 1.25rem 1.25rem 4rem;
  display: flex;
  flex-direction: column;
  gap: 1.35rem;
}

/* Player wrapper */
.player-wrapper {
  margin-bottom: 0.5rem;
}

.now-playing-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 14px;
  border-radius: var(--radius-sm);
  background: rgba(14, 14, 22, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.12);
  margin-bottom: 8px;
}

.now-playing-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--text-primary);
}

.pulse-playing-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.85);
}

.stop-playing-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.35);
  color: #f87171;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
}

@media (hover: hover) and (pointer: fine) {
  .stop-playing-btn:hover {
    background: rgba(239, 68, 68, 0.25);
    color: #fca5a5;
  }
}

/* Episodes Section */
.episodes-section {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  padding: 1rem 0;
  border-top: 1px solid var(--border-subtle);
}

.episodes-section-header {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

@media (min-width: 640px) {
  .episodes-section-header {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}

.episodes-header-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.episodes-header-left .section-icon {
  color: #38bdf8;
}

.section-heading {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 0.35rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.episodes-count-badge {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.12);
  color: #38bdf8;
  border: 1px solid rgba(56, 189, 248, 0.25);
  letter-spacing: 0.05em;
}

.season-tabs-wrap {
  display: flex;
  gap: 0.4rem;
  overflow-x: auto;
  padding-bottom: 2px;
}

.season-tab-btn {
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--text-secondary);
  cursor: pointer;
  white-space: nowrap;
}

@media (hover: hover) and (pointer: fine) {
  .season-tab-btn:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #fff;
  }
}

.season-tab-btn.active {
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.25), rgba(99, 184, 188, 0.35));
  border-color: #38bdf8;
  color: #fff;
  font-weight: 700;
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.3);
}

.single-season-label {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.episodes-list {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.episode-card {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.75rem 0.9rem;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  cursor: pointer;
  content-visibility: auto;
  contain-intrinsic-size: 0 76px;
  contain: layout style paint;
}

@media (hover: hover) and (pointer: fine) {
  .episode-card:hover {
    background: rgba(255, 255, 255, 0.07);
    border-color: rgba(56, 189, 248, 0.4);
  }
}

.episode-card.now-playing {
  background: rgba(56, 189, 248, 0.08);
  border-color: #38bdf8;
  box-shadow: 0 0 16px rgba(56, 189, 248, 0.18);
}

.ep-badge-box {
  position: relative;
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ep-badge-num {
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--text-secondary);
}

.ep-badge-play {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-amber);
  opacity: 0;
}

@media (hover: hover) and (pointer: fine) {
  .episode-card:hover .ep-badge-box {
    border-color: var(--accent-amber);
    background: rgba(245, 158, 11, 0.15);
  }

  .episode-card:hover .ep-badge-num {
    opacity: 0;
  }

  .episode-card:hover .ep-badge-play {
    opacity: 1;
  }
}

.episode-card.now-playing .ep-badge-box {
  border-color: #38bdf8;
  background: rgba(56, 189, 248, 0.2);
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.35);
}

.episode-card.now-playing .ep-badge-num {
  opacity: 0;
}

.episode-card.now-playing .ep-badge-play {
  opacity: 1;
  color: #38bdf8;
}

.ep-quick-play-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--text-muted);
  flex-shrink: 0;
  align-self: center;
}

@media (hover: hover) and (pointer: fine) {
  .episode-card:hover .ep-quick-play-btn {
    background: var(--accent-amber);
    border-color: var(--accent-amber);
    color: #07070a;
  }
}

.episode-card.now-playing .ep-quick-play-btn {
  background: #38bdf8;
  border-color: #38bdf8;
  color: #07070a;
}

.episode-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
  min-width: 0;
}

.ep-header-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.ep-title {
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ep-rating {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 0.7rem;
  color: var(--accent-amber);
  flex-shrink: 0;
}

.ep-meta-line {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.7rem;
  color: var(--text-muted);
}

.ep-playing-indicator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #38bdf8;
  font-weight: 700;
  margin-left: auto;
}

.ep-playing-indicator .pulse-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #38bdf8;
  box-shadow: 0 0 6px #38bdf8;
}

.ep-overview {
  font-size: 0.75rem;
  color: var(--text-secondary);
  line-height: 1.4;
  margin: 0;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.episodes-skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.episode-skeleton-item {
  display: flex;
  gap: 0.85rem;
  padding: 0.6rem;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  align-items: center;
}

.ep-badge-skel {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.ep-text-skel {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.episodes-empty-state {
  padding: 1.5rem;
  text-align: center;
  color: var(--text-muted);
  font-size: 0.82rem;
}

/* Genres */
.genre-row {
  display: flex;
  gap: 0.45rem;
  flex-wrap: wrap;
}
.genre-chip {
  font-size: 0.72rem;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
}

/* Overview Box */
.overview-box {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}
.overview-text {
  font-size: 0.88rem;
  line-height: 1.7;
  color: var(--text-secondary);
}

/* Info Grid */
.info-grid {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.info-row {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.6rem 1rem;
  border-bottom: 1px solid var(--border-subtle);
  font-size: 0.82rem;
}
.info-row:last-child {
  border-bottom: none;
}
.info-label {
  color: var(--text-secondary);
  font-weight: 600;
  min-width: 90px;
  flex-shrink: 0;
}
.info-value {
  color: var(--text-primary);
  line-height: 1.4;
}

/* Keywords */
.keyword-section {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}
.keyword-row {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}
.keyword-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.72rem;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.24);
  color: var(--accent-amber);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  user-select: none;
}

.keyword-chip:hover {
  background: rgba(245, 158, 11, 0.18);
  border-color: rgba(245, 158, 11, 0.45);
  color: #fbbf24;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.2);
}

.keyword-chip:active {
  transform: scale(0.96);
}

.kw-search-icon {
  opacity: 0.7;
  flex-shrink: 0;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.keyword-chip:hover .kw-search-icon {
  opacity: 1;
  transform: scale(1.1);
}

/* Cast */
.cast-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.cast-heading-icon {
  color: var(--accent-amber);
}
.cast-scroll {
  display: flex;
  gap: 0.85rem;
  overflow-x: auto;
  padding-bottom: 0.5rem;
}
.cast-card-skeleton {
  flex-shrink: 0;
  width: 72px;
  height: 110px;
  border-radius: var(--radius-sm);
}
.cast-card {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
  width: 76px;
  cursor: pointer;
  background: transparent;
  border: none;
  padding: 2px;
  outline: none;
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  user-select: none;
}
.cast-photo-wrap {
  position: relative;
  width: 66px;
  height: 66px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--border-subtle);
  background: var(--bg-card);
  flex-shrink: 0;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
@media (hover: hover) and (pointer: fine) {
  .cast-card:hover {
    transform: translateY(-3px);
  }
  .cast-card:hover .cast-photo-wrap {
    border-color: var(--accent-amber);
    box-shadow: 0 4px 14px var(--accent-amber-glow);
  }
  .cast-card:hover .cast-search-overlay {
    opacity: 1;
  }
  .cast-card:hover .cast-name {
    color: var(--accent-amber);
  }
}
.cast-card:active {
  transform: scale(0.95);
}
.cast-search-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: grid;
  place-items: center;
  color: var(--accent-amber);
  opacity: 0;
  transition: opacity 0.2s ease;
  backdrop-filter: blur(2px);
  border-radius: 50%;
}
.cast-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: top;
}
.cast-photo-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--text-muted);
  background: var(--bg-card);
}
.cast-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
  text-align: center;
  width: 100%;
}
.cast-name {
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.25;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.cast-character {
  font-size: 0.62rem;
  color: var(--text-muted);
  line-height: 1.2;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

/* Trailer Modal Overlay */
.trailer-overlay {
  position: fixed;
  inset: 0;
  z-index: 100000;
  background: rgba(0, 0, 0, 0.94);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.trailer-frame-wrap {
  position: relative;
  width: 100%;
  max-width: 820px;
  aspect-ratio: 16 / 9;
  background: #000;
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.15);
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
  background: rgba(18, 18, 26, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
}

@media (hover: hover) and (pointer: fine) {
  .trailer-close-btn:hover {
    background: rgba(239, 68, 68, 0.7);
  }
}

.trailer-fade-enter-active,
.trailer-fade-leave-active {
  transition: opacity 0.2s ease;
}

.trailer-fade-enter-from,
.trailer-fade-leave-to {
  opacity: 0;
}

/* ── Section Anchor Navigation Bar & Jump Links ────────────────────────────── */
.detail-anchors-bar {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.85rem 0 1.25rem 0;
  overflow-x: auto;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 1.25rem;
}

.detail-anchor-link {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.5rem 1.05rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 9999px;
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  user-select: none;
}

.detail-anchor-link:hover,
.detail-anchor-link.active {
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.5);
  color: #fbbf24;
}

.section-anchor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.85rem;
  padding-bottom: 0.4rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.anchor-header-title {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.section-jump-link {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.32rem 0.7rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  color: var(--text-muted);
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.section-jump-link:hover {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
  border-color: rgba(245, 158, 11, 0.4);
}

.jump-arrow {
  color: #fbbf24;
  font-size: 0.75rem;
}

.info-row-focusable {
  cursor: pointer;
  border-radius: 6px;
  padding: 0.45rem 0.6rem;
  margin: -0.2rem -0.4rem;
  transition: background 0.2s ease;
}

.info-row-focusable:hover {
  background: rgba(255, 255, 255, 0.04);
}

.detail-bottom-anchor-wrap {
  display: flex;
  justify-content: center;
  padding: 2.5rem 0 1.5rem 0;
}

.detail-back-to-top-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.7rem 1.6rem;
  background: rgba(26, 26, 38, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 9999px;
  color: var(--text-primary);
  font-size: 0.82rem;
  font-weight: 600;
  letter-spacing: 0.03em;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.5);
  transition: all 0.2s ease;
  user-select: none;
}

.detail-back-to-top-btn:hover {
  background: rgba(245, 158, 11, 0.2);
  border-color: rgba(245, 158, 11, 0.6);
  color: #fbbf24;
}
</style>
