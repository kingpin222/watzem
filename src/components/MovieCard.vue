<script lang="ts">
// Module-scoped cache of loaded image URLs to prevent skeleton flicker on revisit
const loadedImagesCache = new Set<string>();
</script>

<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { MediaItem, MediaMode } from '../types';
import { useApi } from '../composables/useApi';
import { useHaptics } from '../composables/useHaptics';
import { Star, Play, Film } from 'lucide-vue-next';

const props = defineProps<{
  item: MediaItem;
  mode: MediaMode;
}>();

const emit = defineEmits<{
  (e: 'select', item: MediaItem): void;
}>();

const { getImageUrl } = useApi();
const { triggerLight } = useHaptics();

const title = computed(() => props.item.title || props.item.name || 'Untitled');

// Multi-stage fallback:
// 0: primary (poster_path w342, or backdrop_path w342 if no poster)
// 1: backdrop fallback (backdrop_path w342 or w500)
// 2: alternative resolution fallback (w500)
// 3: final failure (show placeholder)
const fallbackStage = ref(0);

const posterUrl = computed(() => {
  if (fallbackStage.value === 0) {
    if (props.item.poster_path) {
      return getImageUrl(props.item.poster_path, 'w342');
    }
    if (props.item.backdrop_path) {
      return getImageUrl(props.item.backdrop_path, 'w342');
    }
    return '';
  }
  if (fallbackStage.value === 1) {
    if (props.item.backdrop_path && props.item.backdrop_path !== props.item.poster_path) {
      return getImageUrl(props.item.backdrop_path, 'w342');
    }
    if (props.item.poster_path) {
      return getImageUrl(props.item.poster_path, 'w500');
    }
    return '';
  }
  if (fallbackStage.value === 2) {
    if (props.item.poster_path) {
      return getImageUrl(props.item.poster_path, 'w500');
    }
    if (props.item.backdrop_path) {
      return getImageUrl(props.item.backdrop_path, 'w500');
    }
    return '';
  }
  return '';
});

const cardItemMode = computed<'movie' | 'tv'>(() => {
  if (props.item.media_type === 'tv') return 'tv';
  if (props.item.media_type === 'movie') return 'movie';
  if (props.item.first_air_date && !props.item.release_date) return 'tv';
  if (props.item.name && !props.item.title) return 'tv';
  return props.mode === 'tv' ? 'tv' : 'movie';
});

const rating = computed(() => {
  const avg = Number(props.item.vote_average);
  if (!isNaN(avg) && avg > 0) {
    return avg.toFixed(1);
  }
  return null;
});

const displayRating = computed(() => rating.value || 'NR');

const isPreloaded = computed(() => Boolean(posterUrl.value && loadedImagesCache.has(posterUrl.value)));
const imgLoaded = ref(isPreloaded.value);
const shouldAnimate = ref(!isPreloaded.value);
const imgError = ref(!props.item.poster_path && !props.item.backdrop_path);
const imgRef = ref<HTMLImageElement | null>(null);

let loadTimeout: ReturnType<typeof setTimeout> | null = null;

const clearLoadTimeout = () => {
  if (loadTimeout) {
    clearTimeout(loadTimeout);
    loadTimeout = null;
  }
};

const armLoadTimeout = () => {
  clearLoadTimeout();
  if (imgLoaded.value || imgError.value || !posterUrl.value) return;
  loadTimeout = setTimeout(() => {
    if (!imgLoaded.value && !imgError.value) {
      handleImgError();
    }
  }, 6500);
};

const checkImgComplete = () => {
  if (!imgRef.value) return;
  if (imgRef.value.complete) {
    if (imgRef.value.naturalWidth > 0) {
      onImageLoad();
    } else if (imgRef.value.src) {
      handleImgError();
    }
  }
};

const onImageLoad = () => {
  clearLoadTimeout();
  imgLoaded.value = true;
  imgError.value = false;
  if (posterUrl.value) {
    loadedImagesCache.add(posterUrl.value);
  }
};

const handleImgError = () => {
  clearLoadTimeout();
  // Try fallback stages before declaring failure
  if (fallbackStage.value === 0) {
    if (props.item.backdrop_path && props.item.backdrop_path !== props.item.poster_path) {
      fallbackStage.value = 1;
      imgLoaded.value = false;
      armLoadTimeout();
      return;
    }
    if (props.item.poster_path) {
      fallbackStage.value = 2;
      imgLoaded.value = false;
      armLoadTimeout();
      return;
    }
  } else if (fallbackStage.value === 1) {
    if (props.item.poster_path) {
      fallbackStage.value = 2;
      imgLoaded.value = false;
      armLoadTimeout();
      return;
    }
  }

  // Final failure: show placeholder
  imgError.value = true;
  imgLoaded.value = false;
};

watch(() => props.item.id, () => {
  fallbackStage.value = 0;
  imgError.value = !props.item.poster_path && !props.item.backdrop_path;
  const inCache = Boolean(posterUrl.value && loadedImagesCache.has(posterUrl.value));
  imgLoaded.value = inCache;
  shouldAnimate.value = !inCache;
  if (!inCache && posterUrl.value) armLoadTimeout();
  nextTick(() => {
    checkImgComplete();
  });
});

watch(posterUrl, (newUrl) => {
  if (!newUrl) {
    imgLoaded.value = false;
    imgError.value = true;
    clearLoadTimeout();
    return;
  }
  const inCache = loadedImagesCache.has(newUrl);
  imgLoaded.value = inCache;
  shouldAnimate.value = !inCache;
  imgError.value = false;
  if (!inCache) armLoadTimeout();
  nextTick(() => {
    checkImgComplete();
  });
});

onMounted(() => {
  if (!imgLoaded.value && !imgError.value && posterUrl.value) {
    armLoadTimeout();
  }
  nextTick(() => {
    checkImgComplete();
  });
});

onUnmounted(() => {
  clearLoadTimeout();
});

const releaseYear = computed(() => {
  const date = props.item.release_date || props.item.first_air_date;
  return date ? date.substring(0, 4) : '';
});

const handleClick = () => {
  triggerLight();
  emit('select', props.item);
};
</script>

<template>
  <div class="movie-card" @click="handleClick">
    <div class="poster-container">
      <!-- Loading Skeleton with Smooth Crossfade & Pulsing Icon (only if there is an image to load) -->
      <div
        v-if="posterUrl && !imgError"
        class="poster-skeleton"
        :class="{ 'skeleton-fade': imgLoaded }"
      >
        <div class="skeleton-shimmer"></div>
        <div class="skeleton-icon-wrap" :class="{ 'tv-theme': cardItemMode === 'tv' }">
          <Film :size="20" class="pulse-icon" />
        </div>
      </div>

      <!-- Poster Image with Blur-Up & Scale Settle Reveal -->
      <img
        v-if="posterUrl && !imgError"
        ref="imgRef"
        :src="posterUrl"
        :alt="title"
        decoding="async"
        class="poster-img"
        :class="{
          loaded: imgLoaded,
          'has-reveal-effect': shouldAnimate
        }"
        @load="onImageLoad"
        @error="handleImgError"
      />

      <!-- One-time Luminous Gleam Sweep on Image Reveal -->
      <div
        v-if="shouldAnimate && imgLoaded && !imgError"
        class="poster-gleam"
      ></div>

      <!-- Fallback when no image or image failed -->
      <div v-if="imgError || !posterUrl" class="poster-placeholder">
        <Film :size="32" :stroke-width="1.5" />
        <span class="placeholder-text">{{ title }}</span>
      </div>

      <!-- Play Action Overlay -->
      <div class="play-overlay">
        <div class="play-btn-circle" :class="{ 'tv-theme': cardItemMode === 'tv' }">
          <Play :size="18" fill="currentColor" />
        </div>
      </div>

      <!-- Top Badges -->
      <div class="badges-top">
        <div class="badge-group-left">
          <span class="badge-type font-mono" :class="{ 'tv-theme': cardItemMode === 'tv' }">
            {{ cardItemMode === 'tv' ? 'TV' : 'MOVIE' }}
          </span>
          <span v-if="releaseYear" class="badge-year font-mono" :class="{ 'tv-theme': cardItemMode === 'tv' }">
            {{ releaseYear }}
          </span>
        </div>
        <span class="badge-rating font-mono">
          <Star :size="10" fill="currentColor" class="star-icon" />
          <span>{{ displayRating }}</span>
        </span>
      </div>
    </div>

    <!-- Title Info -->
    <div class="card-info">
      <h3 class="card-title">{{ title }}</h3>
    </div>
  </div>
</template>

<style scoped>
.movie-card {
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.28s cubic-bezier(0.16, 1, 0.3, 1), border-color 0.25s ease, box-shadow 0.28s cubic-bezier(0.16, 1, 0.3, 1);
  user-select: none;
}

.movie-card:active {
  transform: scale(0.96) translateY(0);
  transition: transform 0.12s ease;
}

@media (hover: hover) {
  .movie-card:hover {
    transform: translateY(-5px) scale(1.02);
    border-color: var(--border-glow);
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.5), 0 0 20px rgba(245, 158, 11, 0.15);
  }
  .movie-card:hover .play-overlay {
    opacity: 1;
  }
  .movie-card:hover .poster-img.loaded {
    transform: scale(1.04);
  }
}

.poster-container {
  position: relative;
  width: 100%;
  aspect-ratio: 2 / 3;
  background: radial-gradient(circle at 50% 35%, #181926 0%, #0d0e15 100%);
  overflow: hidden;
}

/* Skeleton with smooth fade-out and ambient shimmer */
.poster-skeleton {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    145deg,
    #13141f 0%,
    #181a28 50%,
    #11121c 100%
  );
  opacity: 1;
  pointer-events: none;
  transition: opacity 0.4s cubic-bezier(0.16, 1, 0.3, 1), visibility 0.4s ease;
  overflow: hidden;
}

.poster-skeleton.skeleton-fade {
  opacity: 0;
  visibility: hidden;
}

.skeleton-shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.04) 45%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.04) 55%,
    transparent 100%
  );
  background-size: 200% 100%;
  animation: posterShimmer 1.8s infinite cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes posterShimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.skeleton-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.07);
  display: grid;
  place-items: center;
  color: rgba(245, 158, 11, 0.45);
  box-shadow: 0 0 20px rgba(245, 158, 11, 0.08);
}

.skeleton-icon-wrap.tv-theme {
  color: rgba(244, 63, 94, 0.45);
  box-shadow: 0 0 20px rgba(244, 63, 94, 0.08);
}

.pulse-icon {
  animation: skeletonPulse 1.8s ease-in-out infinite;
}

@keyframes skeletonPulse {
  0%, 100% {
    transform: scale(0.92);
    opacity: 0.4;
  }
  50% {
    transform: scale(1.06);
    opacity: 0.85;
  }
}

/* Poster Image with Blur-Up and Scale Reveal */
.poster-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  z-index: 1;
}

/* If already cached, reveal immediately without re-animating */
.poster-img.loaded:not(.has-reveal-effect) {
  opacity: 1;
  transform: scale(1);
}

/* Progressive cinematic blur-up effect on network load */
.poster-img.has-reveal-effect {
  filter: blur(12px) brightness(0.85);
  transform: scale(1.06);
  transition:
    opacity 0.45s cubic-bezier(0.16, 1, 0.3, 1),
    filter 0.5s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.poster-img.loaded.has-reveal-effect {
  opacity: 1;
  filter: blur(0px) brightness(1);
  transform: scale(1);
}

/* Single-sweep luminous gleam when movie icon loads */
.poster-gleam {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 3;
  background: linear-gradient(
    115deg,
    transparent 30%,
    rgba(255, 255, 255, 0.16) 50%,
    transparent 70%
  );
  animation: gleamReveal 0.85s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes gleamReveal {
  0% {
    transform: translateX(-100%);
    opacity: 0;
  }
  20% {
    opacity: 1;
  }
  100% {
    transform: translateX(100%);
    opacity: 0;
  }
}

.poster-placeholder {
  position: absolute;
  inset: 0;
  z-index: 1;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  color: var(--text-muted);
  text-align: center;
  background: var(--bg-surface);
}

.placeholder-text {
  font-size: 0.75rem;
  margin-top: 0.5rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Play button overlay */
.play-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.play-btn-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--accent-amber);
  color: #07070a;
  display: grid;
  place-items: center;
  box-shadow: 0 4px 16px var(--accent-amber-glow);
  transform: scale(0.9);
  transition: transform 0.2s ease;
}

.play-btn-circle.tv-theme {
  background: var(--accent-rose);
  box-shadow: 0 4px 16px var(--accent-rose-glow);
  color: #fff;
}

.play-btn-circle svg {
  margin-left: 2px;
}

/* Top badges */
.badges-top {
  position: absolute;
  top: 6px;
  left: 6px;
  right: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 4px;
  pointer-events: none;
  z-index: 4;
}

.badge-group-left {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
}

.badge-type {
  background: var(--accent-amber);
  color: #07070a;
  font-size: 0.58rem;
  font-weight: 800;
  padding: 2px 5px;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.6);
  letter-spacing: 0.5px;
}

.badge-type.tv-theme {
  background: var(--accent-rose);
  color: #fff;
}

.badge-year {
  background: rgba(14, 14, 20, 0.82);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: var(--text-secondary);
  font-size: 0.6rem;
  font-weight: 700;
  padding: 2px 5px;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.4);
}

.badge-rating {
  background: rgba(7, 7, 10, 0.85);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  color: var(--text-primary);
  font-size: 0.65rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 3px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.star-icon {
  color: #fbbf24;
  font-size: 0.75rem;
}

/* Info */
.card-info {
  padding: 0.6rem 0.65rem 0.7rem;
}

.card-title {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: -0.2px;
}
</style>
