<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { MediaItem, MediaMode, MediaDetails, CastMember, TVSeason, TVEpisode, CreditsResponse, SeasonDetailsResponse } from '../types';
import { useApi } from '../composables/useApi';
import { useWatchlist } from '../composables/useWatchlist';
import { useHaptics } from '../composables/useHaptics';
import { ApiCache } from '../utils/apiCache';
import {
  X,
  Star,
  Bookmark,
  Users,
  Clock,
  Clapperboard,
  Globe,
  DollarSign,
  Tv,
  Tag,
  Play
} from 'lucide-vue-next';
import StreamPlayer from './StreamPlayer.vue';

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
  (e: 'close'): void;
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

// TMDB Standard Genre Map for instant frame-0 genre display without waiting for network
const TMDB_GENRE_MAP: Record<number, string> = {
  28: 'Action',
  12: 'Adventure',
  16: 'Animation',
  35: 'Comedy',
  80: 'Crime',
  99: 'Documentary',
  18: 'Drama',
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

const title = computed(() => props.item.title || props.item.name || 'Details');
// Banner uses the list poster or backdrop image with fast w780 resolution
const bannerUrl = computed(() => {
  const path = props.item.backdrop_path || props.item.poster_path;
  return getImageUrl(path, 'w780');
});

const handleBannerError = (e: Event) => {
  const img = e.target as HTMLImageElement;
  if (!img) return;
  if (props.item.poster_path && !img.dataset.triedPoster) {
    img.dataset.triedPoster = 'true';
    img.src = getImageUrl(props.item.poster_path, 'w780');
  } else if (!img.dataset.triedFallback) {
    img.dataset.triedFallback = 'true';
    img.src = 'https://image.tmdb.org/t/p/w780/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg';
  }
};

const isSaved = computed(() => isInWatchlist(props.item.id));

// Determine whether this is a tv or movie context
const itemMode = computed<'movie' | 'tv'>(() => {
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
  const raw = details.value?.vote_average ?? props.item.vote_average;
  const num = typeof raw === 'string' ? parseFloat(raw) : Number(raw);
  if (!isNaN(num) && num > 0) {
    return num.toFixed(1);
  }
  return null;
});
const displayRating = computed(() => rating.value || 'NR');
const voteCount = computed(() => {
  const c = details.value?.vote_count ?? props.item.vote_count;
  return c ? c.toLocaleString() : null;
});
const year = computed(() => {
  const d = details.value?.release_date || details.value?.first_air_date
    || props.item.release_date || props.item.first_air_date;
  return d ? d.substring(0, 4) : '';
});
const storyline = computed(() =>
  details.value?.overview || props.item.overview || 'No synopsis available for this title.'
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
  if (props.item.genre_ids?.length) {
    return props.item.genre_ids.map((id) => TMDB_GENRE_MAP[id]).filter(Boolean) as string[];
  }
  return [];
});
const keywords = computed(() => {
  const kw = details.value?.keywords;
  const arr = kw?.keywords || kw?.results || [];
  return arr.slice(0, 8).map((k) => k.name);
});
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
const initialBackdrop = bannerUrl.value || getImageUrl(props.item.backdrop_path || props.item.poster_path, 'w1280');
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
    // Preload the following image so it decodes smoothly before transitioning
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

const handleModalScroll = (e: Event) => {
  const el = e.target as HTMLElement;
  if (!el) return;
  if (el.scrollTop > 50) {
    stopSlideshow();
  } else if (!slideTimer && slideImages.value.length > 1) {
    startSlideshow();
  }
};

const loadSlideImages = async () => {
  try {
    const imgs = await getMediaImages(props.item.id, itemMode.value);
    if (imgs && imgs.length > 0) {
      const seed = bannerUrl.value || getImageUrl(props.item.backdrop_path || props.item.poster_path, 'w1280');
      const deduped = [seed, ...imgs.filter((u) => u !== seed)].filter(Boolean);
      // Preload next backdrops
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
  if (trailerKey.value) return;
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
      streamPlayerRef.value.startPlaying(false);
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

  // Only show skeleton if no episodes are currently displayed
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

// Immediate hydration on setup
initFromCache();

const loadAll = async () => {
  if (!props.item?.id) return;

  // Hydrate from cache first
  initFromCache();

  // Background loads
  loadSlideImages();
  loadTrailer();

  if (!details.value) {
    isLoadingDetails.value = true;
  }
  if (cast.value.length === 0) {
    isLoadingCast.value = true;
  }

  // Fetch details and cast in parallel without wiping out existing rendered state
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
        // Fallback: fetch TV details directly
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

onMounted(() => loadAll());
onUnmounted(() => stopSlideshow());
watch(
  () => props.item?.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      episodesCache.value = {};
      initFromCache();
      loadAll();
    }
  }
);

const isPoppingBookmark = ref(false);

const handleBookmark = () => {
  triggerMedium();
  toggleWatchlist(props.item);
  isPoppingBookmark.value = true;
  setTimeout(() => {
    isPoppingBookmark.value = false;
  }, 400);
};

const handleClose = () => {
  triggerLight();
  emit('close');
};
</script>

<template>
  <div class="modal-backdrop" @click.self="handleClose">
    <div class="modal-sheet" @scroll.passive="handleModalScroll">

      <!-- Backdrop Banner with Sliding Images -->
      <div class="modal-banner">
        <div class="banner-slideshow">
          <!-- Permanent base backdrop to prevent any black/blank frame flashes -->
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

        <!-- Close Button -->
        <button class="modal-close-btn" aria-label="Close" @click="handleClose">
          <X :size="18" stroke-width="2.5" />
        </button>

        <!-- Banner Info Overlay -->
        <div class="banner-info">
          <div class="meta-row">
            <span class="badge-mode" :class="{ 'tv-theme': itemMode === 'tv' }">
              {{ itemMode === 'tv' ? 'TV SERIES' : 'MOVIE' }}
            </span>
            <span class="meta-rating font-mono">
              <Star :size="11" fill="currentColor" class="star-icon" />{{ displayRating }}
            </span>
            <span v-if="year" class="meta-chip font-mono">{{ year }}</span>
            <span v-if="runtime" class="meta-chip font-mono runtime-chip">
              <Clock :size="10" />{{ runtime }}
            </span>
          </div>
          <h2 class="modal-title font-display">{{ title }}</h2>
          <p v-if="tagline" class="modal-tagline">{{ tagline }}</p>

          <!-- Primary Call to Action Buttons (Watch Now, Watchlist & Watch Trailer) -->
          <div class="banner-cta-row">
            <button class="cta-watch-btn" @click="handleWatchClick">
              <Play :size="15" fill="currentColor" />
              <span>{{ itemMode === 'tv' ? `Watch S${activeSeasonNumber}:E${activeEpisodeNumber}` : 'Watch Now' }}</span>
            </button>
            <button
              class="cta-bookmark-btn"
              :class="{ saved: isSaved, 'animate-pop-bounce': isPoppingBookmark }"
              :title="isSaved ? 'Remove from Watchlist' : 'Add to Watchlist'"
              @click="handleBookmark"
            >
              <Bookmark :size="14" :fill="isSaved ? 'currentColor' : 'none'" />
              <span>{{ isSaved ? 'Saved' : 'Watchlist' }}</span>
            </button>
            <button v-if="trailerKey" class="cta-trailer-btn" @click="openTrailer">
              <Clapperboard :size="14" />
              <span>Watch Trailer</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Modal Body -->
      <div class="modal-body">

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

        <!-- TV Seasons & Episodes Guide Section -->
        <div v-if="itemMode === 'tv'" class="episodes-section">
          <div class="episodes-section-header">
            <div class="episodes-header-left">
              <Tv :size="15" class="section-icon" />
              <h4 class="section-heading font-display">Episodes</h4>
              <span v-if="episodesList.length > 0" class="episodes-count-badge font-mono">
                {{ episodesList.length }} EPISODES
              </span>
            </div>

            <!-- Season Selector Tabs -->
            <div v-if="tvSeasons.length > 1" class="season-tabs-wrap no-scrollbar">
              <button
                v-for="s in tvSeasons"
                :key="s.season_number"
                class="season-tab-btn font-mono"
                :class="{ active: s.season_number === activeSeasonNumber }"
                @click="changeSeason(s.season_number)"
              >
                Season {{ s.season_number }}
              </button>
            </div>
            <div v-else-if="tvSeasons.length === 1" class="single-season-label font-mono">
              Season {{ tvSeasons[0].season_number }}
            </div>
          </div>

          <!-- Loading Skeleton (Only shown when list is completely empty) -->
          <div v-if="isLoadingEpisodes && episodesList.length === 0" class="episodes-skeleton-list">
            <div v-for="i in 3" :key="i" class="episode-skeleton-item">
              <div class="skeleton ep-badge-skel"></div>
              <div class="ep-text-skel">
                <div class="skeleton" style="height: 14px; width: 60%; margin-bottom: 6px;"></div>
                <div class="skeleton" style="height: 11px; width: 85%;"></div>
              </div>
            </div>
          </div>

          <!-- Episodes List -->
          <div v-else-if="episodesList.length > 0" class="episodes-list no-scrollbar">
            <div
              v-for="ep in episodesList"
              :key="ep.id || ep.episode_number"
              class="episode-card"
              :class="{
                'now-playing': ep.episode_number === activeEpisodeNumber && activeSeasonNumber === currentPlayingSeason
              }"
              @click="playEpisode(activeSeasonNumber, ep.episode_number)"
            >
              <!-- Sleek High-Performance Episode Number & Play Badge (Zero image requests) -->
              <div class="ep-badge-box">
                <span class="ep-badge-num font-mono">
                  {{ ep.episode_number < 10 ? '0' + ep.episode_number : ep.episode_number }}
                </span>
                <div class="ep-badge-play">
                  <Play :size="13" fill="currentColor" />
                </div>
              </div>

              <div class="episode-details">
                <div class="ep-header-line">
                  <span class="ep-title font-display">
                    {{ ep.name || ('Episode ' + ep.episode_number) }}
                  </span>
                  <span v-if="ep.vote_average" class="ep-rating font-mono">
                    <Star :size="10" fill="currentColor" class="star-icon" />{{ ep.vote_average.toFixed(1) }}
                  </span>
                </div>
                <div class="ep-meta-line font-mono">
                  <span v-if="ep.air_date">{{ ep.air_date }}</span>
                  <span v-if="ep.air_date && ep.runtime"> · </span>
                  <span v-if="ep.runtime">{{ ep.runtime }}m</span>
                  <span
                    v-if="ep.episode_number === activeEpisodeNumber && activeSeasonNumber === currentPlayingSeason"
                    class="ep-playing-indicator"
                  >
                    <span class="pulse-dot"></span> Streaming Now
                  </span>
                </div>
                <p v-if="ep.overview" class="ep-overview">
                  {{ ep.overview }}
                </p>
              </div>

              <!-- Quick Play Action Button -->
              <div class="ep-quick-play-btn" title="Play Episode">
                <Play :size="12" fill="currentColor" />
              </div>
            </div>
          </div>

          <div v-else class="episodes-empty-state">
            <p>No episode information found for Season {{ activeSeasonNumber }}.</p>
          </div>
        </div>

        <!-- Genres -->
        <div v-if="genres.length > 0" class="genre-row">
          <span v-for="g in genres" :key="g" class="genre-chip">{{ g }}</span>
        </div>

        <!-- Storyline: Never replace with skeleton if initial overview is already available -->
        <div class="overview-box">
          <h4 class="section-heading font-display">Storyline</h4>
          <p v-if="storyline && storyline !== 'No synopsis available for this title.'" class="overview-text">{{ storyline }}</p>
          <div v-else-if="isLoadingDetails" class="skeleton" style="height:56px;border-radius:6px;"></div>
          <p v-else class="overview-text">{{ storyline }}</p>
        </div>

        <!-- Info Grid: Reserved layout rows while loading to prevent violent jumps -->
        <div class="info-grid">
          <template v-if="isLoadingDetails && !details">
            <div v-for="i in 3" :key="'info-skel-' + i" class="info-row info-skel-row">
              <div class="skeleton" style="width: 14px; height: 14px; border-radius: 3px;"></div>
              <div class="skeleton" style="width: 65px; height: 13px; border-radius: 3px;"></div>
              <div class="skeleton" style="width: 45%; height: 13px; border-radius: 3px;"></div>
            </div>
          </template>
          <template v-else>
            <!-- Director (movies) -->
            <div v-if="directors.length > 0" class="info-row">
              <Clapperboard :size="13" class="info-icon" />
              <span class="info-label">Director</span>
              <span class="info-value">{{ directors.join(', ') }}</span>
            </div>
            <!-- Creator (TV) -->
            <div v-if="createdBy.length > 0" class="info-row">
              <Clapperboard :size="13" class="info-icon" />
              <span class="info-label">Created by</span>
              <span class="info-value">{{ createdBy.join(', ') }}</span>
            </div>
            <!-- Writers -->
            <div v-if="writers.length > 0" class="info-row">
              <Tag :size="13" class="info-icon" />
              <span class="info-label">Writer</span>
              <span class="info-value">{{ writers.join(', ') }}</span>
            </div>
            <!-- Status -->
            <div v-if="status" class="info-row">
              <Star :size="13" class="info-icon" />
              <span class="info-label">Status</span>
              <span class="info-value">{{ status }}</span>
            </div>
            <!-- Language -->
            <div v-if="language" class="info-row">
              <Globe :size="13" class="info-icon" />
              <span class="info-label">Language</span>
              <span class="info-value">{{ language }}</span>
            </div>
            <!-- Network (TV) -->
            <div v-if="networks.length > 0" class="info-row">
              <Tv :size="13" class="info-icon" />
              <span class="info-label">Network</span>
              <span class="info-value">{{ networks.join(' · ') }}</span>
            </div>
            <!-- Budget (movies) -->
            <div v-if="budget" class="info-row">
              <DollarSign :size="13" class="info-icon" />
              <span class="info-label">Budget</span>
              <span class="info-value">{{ budget }}</span>
            </div>
            <!-- Revenue (movies) -->
            <div v-if="revenue" class="info-row">
              <DollarSign :size="13" class="info-icon" />
              <span class="info-label">Box Office</span>
              <span class="info-value">{{ revenue }}</span>
            </div>
            <!-- Vote count -->
            <div v-if="voteCount" class="info-row">
              <Star :size="13" class="info-icon" />
              <span class="info-label">Votes</span>
              <span class="info-value">{{ voteCount }}</span>
            </div>
          </template>
        </div>

        <!-- Keywords / Tags -->
        <div v-if="keywords.length > 0" class="keyword-section">
          <h4 class="section-heading font-display">Tags</h4>
          <div class="keyword-row">
            <span v-for="kw in keywords" :key="kw" class="keyword-chip">{{ kw }}</span>
          </div>
        </div>

        <!-- Cast -->
        <div v-if="isLoadingCast || cast.length > 0" class="cast-section">
          <h4 class="section-heading font-display">
            <Users :size="13" class="cast-heading-icon" />
            Cast
          </h4>
          <!-- Skeleton -->
          <div v-if="isLoadingCast" class="cast-scroll no-scrollbar">
            <div v-for="n in 8" :key="n" class="cast-card-skeleton skeleton"></div>
          </div>
          <!-- Actual cast -->
          <div v-else class="cast-scroll no-scrollbar">
            <div v-for="member in cast" :key="member.id" class="cast-card">
              <div class="cast-photo-wrap">
                <img
                  v-if="member.profile_path"
                  :src="getImageUrl(member.profile_path, 'w185')"
                  :alt="member.name"
                  class="cast-photo"
                  loading="lazy"
                />
                <div v-else class="cast-photo-placeholder">
                  <Users :size="20" stroke-width="1.5" />
                </div>
              </div>
              <div class="cast-info">
                <span class="cast-name">{{ member.name }}</span>
                <span v-if="member.character" class="cast-character">{{ member.character }}</span>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- YouTube Trailer Overlay -->
    <Teleport to="body">
      <Transition name="trailer-fade">
        <div v-if="showTrailerModal" class="trailer-overlay" @click.self="closeTrailer">
          <div class="trailer-frame-wrap">
            <button class="trailer-close-btn" aria-label="Close Trailer" @click="closeTrailer">
              <X :size="18" stroke-width="2.2" />
            </button>
            <iframe
              v-if="trailerUrl"
              :src="trailerUrl"
              class="trailer-iframe"
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
/* ---- backdrop & sheet ---- */
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(4, 4, 8, 0.94);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  transition: opacity 0.2s ease;
}
@media (min-width: 768px) {
  .modal-backdrop { align-items: center; padding: 1.5rem; }
}

.modal-sheet {
  width: 100%;
  max-width: 600px;
  max-height: 92vh;
  background: var(--bg-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  border: 1px solid var(--border-subtle);
  border-bottom: none;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain;
  box-shadow: 0 -10px 40px rgba(0, 0, 0, 0.9);
  padding-bottom: calc(var(--safe-bottom) + 1.25rem);
}
@media (min-width: 768px) {
  .modal-sheet {
    max-width: 780px;
    max-height: 88vh;
    border-radius: var(--radius-xl);
    border-bottom: 1px solid var(--border-subtle);
    box-shadow: 0 25px 60px rgba(0, 0, 0, 0.85);
    padding-bottom: 1.5rem;
  }
}

/* Modal Transition integration */
:deep(.modal-fade-enter-active) .modal-sheet,
:deep(.modal-fade-leave-active) .modal-sheet,
.modal-fade-enter-active .modal-sheet,
.modal-fade-leave-active .modal-sheet {
  transition: transform 0.28s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.28s ease;
}

:deep(.modal-fade-enter-from),
.modal-fade-enter-from {
  opacity: 0;
}
:deep(.modal-fade-leave-to),
.modal-fade-leave-to {
  opacity: 0;
}

:deep(.modal-fade-enter-from) .modal-sheet,
.modal-fade-enter-from .modal-sheet {
  transform: translateY(100%);
  opacity: 0.3;
}
:deep(.modal-fade-leave-to) .modal-sheet,
.modal-fade-leave-to .modal-sheet {
  transform: translateY(100%);
  opacity: 0;
}

@media (min-width: 768px) {
  :deep(.modal-fade-enter-from) .modal-sheet,
  .modal-fade-enter-from .modal-sheet {
    transform: scale(0.95) translateY(15px);
    opacity: 0;
  }
  :deep(.modal-fade-leave-to) .modal-sheet,
  .modal-fade-leave-to .modal-sheet {
    transform: scale(0.96) translateY(15px);
    opacity: 0;
  }
}

/* ---- banner with sliding images ---- */
.modal-banner {
  position: relative;
  width: 100%;
  height: 290px;
  background: radial-gradient(circle at center, #1b1b29 0%, #0d0d16 65%, #07070a 100%);
  flex-shrink: 0;
  overflow: hidden;
  contain: paint;
}
@media (min-width: 768px) { .modal-banner { height: 340px; } }

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
  background: linear-gradient(to top, var(--bg-surface) 0%, rgba(14, 14, 21, 0.45) 50%, rgba(0, 0, 0, 0.65) 100%);
}

.modal-close-btn {
  position: absolute;
  top: 1rem;
  right: 1rem;
  z-index: 10;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(18, 18, 26, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: var(--text-primary);
  display: grid;
  place-items: center;
  cursor: pointer;
}

@media (hover: hover) and (pointer: fine) {
  .modal-close-btn:hover {
    background: rgba(239, 68, 68, 0.75);
  }
}

.banner-info {
  position: absolute;
  bottom: 1rem;
  left: 1.25rem;
  right: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  z-index: 3;
}

.meta-row { display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap; }

.badge-mode {
  font-size: 0.6rem; font-weight: 800; padding: 2px 6px;
  border-radius: 4px; background: var(--accent-amber); color: #07070a;
  letter-spacing: 0.5px;
}
.badge-mode.tv-theme { background: var(--accent-rose); color: #fff; }

.meta-chip {
  font-size: 0.7rem; color: var(--text-secondary);
  display: flex; align-items: center; gap: 3px;
}
.meta-rating {
  font-size: 0.72rem; font-weight: 700; color: #fbbf24;
  display: flex; align-items: center; gap: 3px;
}
.star-icon { color: #fbbf24; }

.modal-title {
  font-size: 1.35rem; font-weight: 800; line-height: 1.2;
  color: var(--text-primary); text-shadow: 0 2px 8px rgba(0,0,0,0.9);
}
.modal-tagline {
  font-size: 0.75rem; font-style: italic;
  color: rgba(255,255,255,0.7); margin-top: 0.1rem;
}

/* Call to action buttons in header */
.banner-cta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 0.35rem;
  flex-wrap: wrap;
}

.cta-watch-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 999px;
  background: var(--accent-amber);
  color: #07070a;
  font-size: 0.8rem;
  font-weight: 800;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.4);
}

@media (hover: hover) and (pointer: fine) {
  .cta-watch-btn:hover {
    box-shadow: 0 6px 18px rgba(245, 158, 11, 0.55);
    filter: brightness(1.08);
  }
}

.cta-watch-btn:active {
  transform: scale(0.98);
}

.cta-trailer-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(28, 28, 40, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.22);
  color: #ffffff;
  font-size: 0.8rem;
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
  gap: 5px;
  padding: 8px 13px;
  border-radius: 999px;
  background: rgba(24, 24, 34, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.78rem;
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

/* ---- body ---- */
.modal-body {
  padding: 1rem 1.25rem 2rem;
  display: flex; flex-direction: column; gap: 1.15rem;
}

.player-wrapper {
  margin-bottom: 0.5rem;
}

/* Now playing bar */
.now-playing-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  background: rgba(14, 14, 22, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.12);
  margin-bottom: 8px;
}

.now-playing-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
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
  width: 34px;
  height: 34px;
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

/* genres */
.genre-row { display: flex; gap: 0.4rem; flex-wrap: wrap; }
.genre-chip {
  font-size: 0.68rem; font-weight: 600;
  padding: 3px 10px; border-radius: var(--radius-full);
  background: rgba(255,255,255,0.07);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
}

/* overview box */
.overview-box {
  display: flex; flex-direction: column; gap: 0.4rem;
}
.section-heading {
  font-size: 0.82rem; font-weight: 700; color: var(--text-primary);
  display: flex; align-items: center; gap: 0.35rem;
  text-transform: uppercase; letter-spacing: 0.05em;
}
.overview-text {
  font-size: 0.82rem; line-height: 1.65; color: var(--text-secondary);
}

/* info grid */
.info-grid {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.info-row {
  display: flex; align-items: flex-start; gap: 0.55rem;
  padding: 0.5rem 0.9rem;
  border-bottom: 1px solid var(--border-subtle);
  font-size: 0.78rem;
}
.info-row:last-child { border-bottom: none; }
.info-skel-row {
  align-items: center;
  gap: 0.75rem;
  min-height: 38px;
}
.info-icon { color: var(--accent-amber); margin-top: 2px; flex-shrink: 0; }
.info-label {
  color: var(--text-secondary); font-weight: 600;
  min-width: 78px; flex-shrink: 0;
}
.info-value { color: var(--text-primary); line-height: 1.4; }

/* keywords */
.keyword-section { display: flex; flex-direction: column; gap: 0.5rem; }
.keyword-row { display: flex; gap: 0.35rem; flex-wrap: wrap; }
.keyword-chip {
  font-size: 0.65rem; font-weight: 500;
  padding: 2px 8px; border-radius: var(--radius-full);
  background: rgba(245,158,11,0.08);
  border: 1px solid rgba(245,158,11,0.22);
  color: var(--accent-amber);
}

/* cast */
.cast-section { display: flex; flex-direction: column; gap: 0.65rem; }
.cast-heading-icon { color: var(--accent-amber); }
.cast-scroll { display: flex; gap: 0.7rem; overflow-x: auto; padding-bottom: 0.4rem; }
.cast-card-skeleton { flex-shrink: 0; width: 70px; height: 106px; border-radius: var(--radius-sm); }
.cast-card {
  flex-shrink: 0; display: flex; flex-direction: column;
  align-items: center; gap: 0.4rem; width: 70px; cursor: default;
}
.cast-photo-wrap {
  width: 62px; height: 62px; border-radius: 50%;
  overflow: hidden; border: 2px solid var(--border-subtle);
  background: var(--bg-card); flex-shrink: 0;
}
@media (hover: hover) and (pointer: fine) {
  .cast-card:hover .cast-photo-wrap { border-color: var(--accent-amber); }
}
.cast-photo { width: 100%; height: 100%; object-fit: cover; object-position: top; }
.cast-photo-placeholder {
  width: 100%; height: 100%;
  display: grid; place-items: center;
  color: var(--text-muted); background: var(--bg-card);
}
.cast-info {
  display: flex; flex-direction: column;
  align-items: center; gap: 0.1rem; text-align: center; width: 100%;
}
.cast-name {
  font-size: 0.68rem; font-weight: 600; color: var(--text-primary);
  line-height: 1.25; overflow: hidden;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
.cast-character {
  font-size: 0.61rem; color: var(--text-muted); line-height: 1.2;
  overflow: hidden;
  display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical;
}

/* Episodes & Seasons Guide Section */
.episodes-section {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
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
  padding: 0.35rem 0.8rem;
  border-radius: 999px;
  font-size: 0.74rem;
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
  padding: 0.65rem;
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
  width: 42px;
  height: 42px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ep-badge-num {
  font-size: 0.8rem;
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
  width: 32px;
  height: 32px;
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
  gap: 0.2rem;
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
  font-size: 0.85rem;
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
  font-size: 0.68rem;
  color: var(--accent-amber);
  flex-shrink: 0;
}

.ep-meta-line {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.68rem;
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
  font-size: 0.72rem;
  color: var(--text-secondary);
  line-height: 1.35;
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
  width: 42px;
  height: 42px;
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
  font-size: 0.8rem;
}
</style>
