<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { MediaItem, MediaMode } from '../types';
import { useApi } from '../composables/useApi';
import { useWatchlist } from '../composables/useWatchlist';
import { useHaptics } from '../composables/useHaptics';
import { useHeroBackground } from '../composables/useHeroBackground';
import {
  Star,
  Play,
  Info,
  Bookmark,
  ChevronLeft,
  ChevronRight,
  Sparkles
} from 'lucide-vue-next';

const props = defineProps<{
  items?: MediaItem[];
  item?: MediaItem;
  mode: MediaMode;
}>();

const emit = defineEmits<{
  (e: 'select', item: MediaItem): void;
  (e: 'play', item: MediaItem): void;
}>();

const { getImageUrl } = useApi();
const { isInWatchlist, toggleWatchlist } = useWatchlist();
const { triggerMedium, triggerLight } = useHaptics();
const { setHeroBackdrop, clearHeroBackdrop } = useHeroBackground();

// Display items candidate pool - limited strictly to 10 movie/tv items with valid images only
const displayItems = computed<MediaItem[]>(() => {
  const source = props.items && props.items.length > 0
    ? props.items
    : props.item
      ? [props.item]
      : [];

  // Filter for items with valid movie/tv images only (backdrop or poster)
  const withImages = source.filter((m) => Boolean(m.backdrop_path || m.poster_path));
  const pool = withImages.length > 0 ? withImages : source;

  // Enforce strict limit of 10 items only
  return pool.slice(0, 10);
});

const currentIndex = ref(0);
const isPaused = ref(false);
const scrollParallaxY = ref(0);
const rotationTimer = ref<ReturnType<typeof setInterval> | null>(null);
const ROTATION_INTERVAL_MS = 6500;
const transitionKey = ref(0);

// Active featured item
const activeItem = computed<MediaItem | null>(() => {
  if (displayItems.value.length === 0) return null;
  const idx = Math.min(currentIndex.value, displayItems.value.length - 1);
  return displayItems.value[idx];
});

const title = computed(() => {
  if (!activeItem.value) return 'Featured';
  return activeItem.value.title || activeItem.value.name || 'Featured';
});

const heroImgFailed = ref(false);

const backdropUrl = computed(() => {
  if (!activeItem.value) return '';
  if (heroImgFailed.value) {
    if (activeItem.value.poster_path && activeItem.value.backdrop_path) {
      return getImageUrl(activeItem.value.poster_path, 'w780');
    }
    return 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1280&q=80';
  }
  const path = activeItem.value.backdrop_path || activeItem.value.poster_path;
  return getImageUrl(path, 'w1280');
});

const handleBackdropError = () => {
  heroImgFailed.value = true;
};

watch(activeItem, () => {
  heroImgFailed.value = false;
});

watch(backdropUrl, (newUrl) => {
  if (newUrl) {
    setHeroBackdrop(newUrl);
  } else {
    clearHeroBackdrop();
  }
}, { immediate: true });

const heroItemMode = computed<'movie' | 'tv'>(() => {
  if (!activeItem.value) return props.mode === 'tv' ? 'tv' : 'movie';
  if (activeItem.value.media_type === 'tv') return 'tv';
  if (activeItem.value.media_type === 'movie') return 'movie';
  if (activeItem.value.first_air_date && !activeItem.value.release_date) return 'tv';
  if (activeItem.value.name && !activeItem.value.title) return 'tv';
  return props.mode === 'tv' ? 'tv' : 'movie';
});

const rating = computed(() => {
  if (!activeItem.value || !activeItem.value.vote_average) return null;
  const avg = Number(activeItem.value.vote_average);
  return !isNaN(avg) && avg > 0 ? avg.toFixed(1) : null;
});

const displayRating = computed(() => rating.value || 'NR');

const year = computed(() => {
  if (!activeItem.value) return '';
  const d = activeItem.value.release_date || activeItem.value.first_air_date;
  return d ? d.substring(0, 4) : '';
});

const isSaved = computed(() => {
  if (!activeItem.value) return false;
  return isInWatchlist(activeItem.value.id);
});

const nextSlide = (userInitiated = false) => {
  if (displayItems.value.length <= 1) return;
  if (userInitiated) triggerLight();
  currentIndex.value = (currentIndex.value + 1) % displayItems.value.length;
  transitionKey.value++;
  if (userInitiated) restartTimer();
};

const prevSlide = () => {
  if (displayItems.value.length <= 1) return;
  triggerLight();
  currentIndex.value =
    (currentIndex.value - 1 + displayItems.value.length) % displayItems.value.length;
  transitionKey.value++;
  restartTimer();
};

// Auto-alternating timer
const startTimer = () => {
  stopTimer();
  rotationTimer.value = setInterval(() => {
    if (!isPaused.value && displayItems.value.length > 1) {
      nextSlide(false);
    }
  }, ROTATION_INTERVAL_MS);
};

const stopTimer = () => {
  if (rotationTimer.value) {
    clearInterval(rotationTimer.value);
    rotationTimer.value = null;
  }
};

const restartTimer = () => {
  startTimer();
};

// Candidate slide image preloading for seamless zero-flicker rotation
const preloadedBackdrops = new Set<string>();

const preloadBackdrop = (item?: MediaItem | null) => {
  if (!item) return;
  const path = item.backdrop_path || item.poster_path;
  if (!path) return;
  const url = getImageUrl(path, 'w1280');
  if (url && !preloadedBackdrops.has(url)) {
    preloadedBackdrops.add(url);
    const img = new Image();
    img.src = url;
  }
};

const preloadUpcoming = () => {
  const list = displayItems.value;
  if (list.length <= 1) return;
  const next1 = (currentIndex.value + 1) % list.length;
  const next2 = (currentIndex.value + 2) % list.length;
  preloadBackdrop(list[next1]);
  preloadBackdrop(list[next2]);
};

watch([currentIndex, displayItems], () => {
  preloadUpcoming();
}, { immediate: true });

// Parallax scrolling computation
let ticking = false;
const handleScroll = () => {
  if (!ticking) {
    window.requestAnimationFrame(() => {
      const isLandscape = window.matchMedia('(orientation: landscape)').matches;
      if (isLandscape) {
        scrollParallaxY.value = 0;
      } else {
        const scrollY = window.scrollY || document.documentElement.scrollTop;
        if (scrollY < 650) {
          scrollParallaxY.value = Math.round(scrollY * 0.38);
        } else {
          scrollParallaxY.value = Math.round(650 * 0.38);
        }
      }
      ticking = false;
    });
    ticking = true;
  }
};

const handleOrientationOrResize = () => {
  handleScroll();
};

// Touch swipe support
let touchStartX = 0;
let touchStartY = 0;

const onTouchStart = (e: TouchEvent) => {
  if (e.touches.length === 1) {
    touchStartX = e.touches[0].clientX;
    touchStartY = e.touches[0].clientY;
  }
};

const onTouchEnd = (e: TouchEvent) => {
  if (e.changedTouches.length === 1) {
    const deltaX = e.changedTouches[0].clientX - touchStartX;
    const deltaY = e.changedTouches[0].clientY - touchStartY;
    // Ensure horizontal gesture
    if (Math.abs(deltaX) > 40 && Math.abs(deltaX) > Math.abs(deltaY)) {
      if (deltaX < 0) {
        nextSlide(true);
      } else {
        prevSlide();
      }
    }
  }
};

const handleToggleBookmark = (e: Event) => {
  e.stopPropagation();
  if (activeItem.value) {
    triggerMedium();
    toggleWatchlist(activeItem.value);
  }
};

const handlePlay = (e: Event) => {
  e.stopPropagation();
  if (activeItem.value) {
    triggerLight();
    emit('play', activeItem.value);
  }
};

// Lifecycle
onMounted(() => {
  if (displayItems.value.length > 1) {
    // Pick random initial slide
    currentIndex.value = Math.floor(Math.random() * displayItems.value.length);
  }
  startTimer();
  window.addEventListener('scroll', handleScroll, { passive: true });
  window.addEventListener('resize', handleOrientationOrResize, { passive: true });
  window.addEventListener('orientationchange', handleOrientationOrResize, { passive: true });
});

onUnmounted(() => {
  stopTimer();
  clearHeroBackdrop();
  window.removeEventListener('scroll', handleScroll);
  window.removeEventListener('resize', handleOrientationOrResize);
  window.removeEventListener('orientationchange', handleOrientationOrResize);
});

// Watch for mode or items changes
watch(
  () => props.mode,
  () => {
    if (displayItems.value.length > 1) {
      currentIndex.value = Math.floor(Math.random() * displayItems.value.length);
    } else {
      currentIndex.value = 0;
    }
    transitionKey.value++;
    restartTimer();
  }
);

watch(
  () => displayItems.value.length,
  (newLen) => {
    if (newLen > 1) {
      currentIndex.value = Math.floor(Math.random() * newLen);
    } else {
      currentIndex.value = 0;
    }
    transitionKey.value++;
    restartTimer();
  }
);
</script>

<template>
  <div
    v-if="activeItem"
    class="featured-hero"
    @mouseenter="isPaused = true"
    @mouseleave="isPaused = false"
    @touchstart.passive="onTouchStart"
    @touchend="onTouchEnd"
    @click="emit('select', activeItem)"
  >
    <!-- Ambient Dynamic Glow Aura -->
    <div
      class="hero-ambient-glow"
      :class="{ 'tv-glow': heroItemMode === 'tv' }"
    ></div>

    <!-- Parallax Backdrop Container -->
    <div
      class="backdrop-parallax-viewport"
      :style="{ transform: `translate3d(0, ${scrollParallaxY}px, 0)` }"
    >
      <Transition name="hero-crossfade">
        <div :key="activeItem.id" class="backdrop-slide">
          <img
            v-if="backdropUrl"
            :src="backdropUrl"
            :alt="title"
            class="backdrop-img ken-burns"
            @error="handleBackdropError"
          />
        </div>
      </Transition>

      <!-- Multi-stage cinematic gradients for seamless blend -->
      <div class="gradient-overlay-bottom"></div>
      <div class="gradient-overlay-left"></div>
      <div class="gradient-overlay-top"></div>
    </div>

    <!-- Navigation Arrows for Desktop / Hover -->
    <button
      v-if="displayItems.length > 1"
      class="hero-nav-arrow arrow-left"
      aria-label="Previous featured"
      @click.stop="prevSlide"
    >
      <ChevronLeft :size="20" />
    </button>
    <button
      v-if="displayItems.length > 1"
      class="hero-nav-arrow arrow-right"
      aria-label="Next featured"
      @click.stop="nextSlide(true)"
    >
      <ChevronRight :size="20" />
    </button>

    <!-- Morphing Content Area with Smooth Text Morph Transition -->
    <div class="hero-content">
      <Transition name="hero-morph" mode="out-in">
        <div :key="activeItem.id" class="hero-text-block">
          <div class="hero-meta">
            <span class="badge-featured" :class="{ 'tv-theme': heroItemMode === 'tv' }">
              <Sparkles :size="11" class="badge-sparkle" />
              <span>{{ heroItemMode === 'tv' ? 'TV SERIES' : 'FEATURED MOVIE' }}</span>
            </span>
            <span v-if="year" class="meta-year font-mono">{{ year }}</span>
            <span class="meta-rating font-mono">
              <Star :size="11" fill="currentColor" class="star" />
              <span>{{ displayRating }}</span>
            </span>
          </div>

          <h1 class="hero-title font-display">{{ title }}</h1>
          <p class="hero-desc">{{ activeItem.overview || 'Stream in high quality with fast playback.' }}</p>
        </div>
      </Transition>

      <!-- Action Buttons & Alternating Pill Indicators -->
      <div class="hero-bottom-bar">
        <div class="hero-actions">
          <button class="btn-play" :class="{ 'tv-theme': heroItemMode === 'tv' }" @click="handlePlay">
            <Play :size="16" fill="currentColor" />
            <span>Watch Now</span>
          </button>

          <button class="btn-details" @click.stop="emit('select', activeItem)">
            <Info :size="15" />
            <span>Details</span>
          </button>

          <button
            class="btn-bookmark"
            :class="{ active: isSaved }"
            :title="isSaved ? 'Remove from Watchlist' : 'Add to Watchlist'"
            aria-label="Save to watchlist"
            @click="handleToggleBookmark"
          >
            <Bookmark :size="15" :fill="isSaved ? 'currentColor' : 'none'" />
            <span>{{ isSaved ? 'Saved' : 'Watchlist' }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.featured-hero {
  position: relative;
  width: 100%;
  height: 420px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background-color: transparent;
  user-select: none;
}

@media (min-width: 768px) and (min-height: 551px) {
  .featured-hero {
    height: 500px;
  }
}

@media (min-width: 1024px) and (min-height: 551px) {
  .featured-hero {
    height: 580px;
  }
}

@media (orientation: landscape) {
  .featured-hero {
    height: clamp(220px, 66vh, 360px);
  }
}

/* Ambient glow */
.hero-ambient-glow {
  position: absolute;
  top: -20%;
  left: 10%;
  width: 80%;
  height: 80%;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.18) 0%, transparent 70%);
  filter: blur(50px);
  pointer-events: none;
  z-index: 0;
  transition: all 0.8s ease;
}

.hero-ambient-glow.tv-glow {
  background: radial-gradient(circle, rgba(244, 63, 94, 0.18) 0%, transparent 70%);
}

/* Parallax Viewport & Slides */
.backdrop-parallax-viewport {
  position: absolute;
  inset: -15% 0 -5% 0;
  z-index: 1;
  will-change: transform;
  pointer-events: none;
  background: transparent;
}

.backdrop-slide {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  background: transparent;
}

.backdrop-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 25%;
}

/* Ken Burns slow cinematic drift effect */
.ken-burns {
  animation: kenburns 14s cubic-bezier(0.25, 1, 0.5, 1) infinite alternate;
}

@keyframes kenburns {
  0% {
    transform: scale(1.02) translate3d(0, 0, 0);
  }
  50% {
    transform: scale(1.08) translate3d(-1.2%, -0.8%, 0);
  }
  100% {
    transform: scale(1.05) translate3d(1.2%, 0.8%, 0);
  }
}

/* Dual-Image Crossfade Transitions */
.hero-crossfade-enter-active,
.hero-crossfade-leave-active {
  transition: opacity 0.9s cubic-bezier(0.4, 0, 0.2, 1), transform 0.9s cubic-bezier(0.4, 0, 0.2, 1);
}

.hero-crossfade-enter-from {
  opacity: 0;
  transform: scale(1.06);
}

.hero-crossfade-leave-to {
  opacity: 0;
  transform: scale(1);
}

/* Multi-stage cinematic gradients */
.gradient-overlay-bottom {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    rgba(7, 7, 10, 0.1) 0%,
    rgba(7, 7, 10, 0.35) 45%,
    rgba(7, 7, 10, 0.85) 80%,
    var(--bg-main) 100%
  );
}

.gradient-overlay-left {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to right,
    rgba(7, 7, 10, 0.85) 0%,
    rgba(7, 7, 10, 0.4) 45%,
    transparent 80%
  );
}

.gradient-overlay-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(
    to bottom,
    rgba(7, 7, 10, 0.75) 0%,
    transparent 100%
  );
}

/* Navigation Arrows (Desktop / Hover) */
.hero-nav-arrow {
  position: absolute;
  top: 45%;
  transform: translateY(-50%);
  z-index: 5;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(14, 14, 21, 0.6);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  display: none;
  place-items: center;
  cursor: pointer;
  transition: all 0.25s ease;
  opacity: 0.65;
}

@media (min-width: 768px) and (min-height: 551px) {
  .featured-hero:hover .hero-nav-arrow {
    display: grid;
  }
}

.hero-nav-arrow:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-50%) scale(1.08);
}

.arrow-left {
  left: 1.25rem;
}

.arrow-right {
  right: 1.25rem;
}

/* Hero Content Area */
.hero-content {
  position: relative;
  z-index: 2;
  padding: 1.25rem max(1.25rem, var(--safe-left, 0px)) 1.25rem;
  padding-right: max(1.25rem, var(--safe-right, 0px));
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-width: 960px;
}

@media (min-width: 768px) and (min-height: 551px) {
  .hero-content {
    padding: 2.25rem max(2rem, var(--safe-left, 0px)) 1.85rem;
    padding-right: max(2rem, var(--safe-right, 0px));
    gap: 0.9rem;
  }
}

@media (orientation: landscape) {
  .hero-content {
    padding: 0.75rem max(1.25rem, var(--safe-left, 0px)) 0.75rem;
    padding-right: max(1.25rem, var(--safe-right, 0px));
    gap: 0.4rem;
    max-width: 100%;
  }
  .hero-text-block {
    gap: 0.25rem;
  }
  .hero-title {
    font-size: clamp(1.2rem, 3.2vw, 1.85rem);
    line-height: 1.15;
  }
  .hero-desc {
    font-size: 0.8rem;
    line-height: 1.35;
    -webkit-line-clamp: 2;
    max-width: 680px;
  }
  .hero-bottom-bar {
    margin-top: 0.25rem;
    gap: 0.6rem;
  }
  .btn-play {
    padding: 8px 14px;
    font-size: 0.8rem;
    border-radius: 999px;
  }
  .btn-details {
    padding: 7px 12px;
    font-size: 0.78rem;
    border-radius: 999px;
  }
  .btn-bookmark {
    padding: 7px 11px;
    font-size: 0.78rem;
    border-radius: 999px;
  }
}

/* Morph Transition for Title, Meta, and Desc */
.hero-text-block {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.hero-morph-enter-active,
.hero-morph-leave-active {
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.hero-morph-enter-from {
  opacity: 0;
  transform: translateY(16px) scale(0.97);
  filter: blur(5px);
}

.hero-morph-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.99);
  filter: blur(3px);
}

/* Meta */
.hero-meta {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.badge-featured {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--accent-amber);
  color: #07070a;
  font-size: 0.62rem;
  font-weight: 800;
  letter-spacing: 0.6px;
  padding: 2px 8px;
  border-radius: 4px;
  box-shadow: 0 2px 8px var(--accent-amber-glow);
}

.badge-featured.tv-theme {
  background: var(--accent-rose);
  box-shadow: 0 2px 8px var(--accent-rose-glow);
  color: #fff;
}

.badge-sparkle {
  animation: pulse 2s ease infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.2); opacity: 0.8; }
}

.meta-year {
  font-size: 0.75rem;
  color: var(--text-secondary);
  font-weight: 500;
}

.meta-rating {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 2px;
}

.meta-rating .star {
  color: #fbbf24;
}

/* Title & Description */
.hero-title {
  font-size: clamp(1.6rem, 3.8vw, 3rem);
  font-weight: 800;
  line-height: 1.12;
  color: var(--text-primary);
  text-shadow: 0 2px 14px rgba(0, 0, 0, 0.85);
  letter-spacing: -0.02em;
}

.hero-desc {
  font-size: clamp(0.8rem, 1.25vw, 0.95rem);
  color: var(--text-secondary);
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 680px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.7);
}

/* Bottom Bar */
.hero-bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 0.5rem;
}

/* Actions */
.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.btn-play {
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
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@media (hover: hover) and (pointer: fine) {
  .btn-play:hover {
    box-shadow: 0 6px 20px rgba(245, 158, 11, 0.6);
    filter: brightness(1.08);
  }
}

.btn-play:active {
  transform: scale(0.98);
}

.btn-play.tv-theme {
  background: var(--accent-rose);
  box-shadow: 0 4px 16px var(--accent-rose-glow);
  color: #fff;
}

.btn-details {
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
  transition: all 0.2s ease;
}

@media (hover: hover) and (pointer: fine) {
  .btn-details:hover {
    background: rgba(45, 45, 60, 0.95);
    border-color: rgba(255, 255, 255, 0.4);
  }
}

.btn-details:active {
  transform: scale(0.98);
}

.btn-bookmark {
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
  transition: all 0.2s ease;
}

@media (hover: hover) and (pointer: fine) {
  .btn-bookmark:hover {
    background: rgba(38, 38, 52, 0.95);
    color: #fff;
  }
}

.btn-bookmark:active {
  transform: scale(0.98);
}

.btn-bookmark.active {
  background: rgba(245, 158, 11, 0.2);
  border-color: rgba(245, 158, 11, 0.5);
  color: #fbbf24;
}

/* Indicators Cluster & Random Shuffle */
.hero-indicators-cluster {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.btn-shuffle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-shuffle:hover {
  color: var(--text-primary);
  border-color: var(--accent-amber);
  background: rgba(245, 158, 11, 0.18);
  transform: rotate(180deg);
}

.btn-shuffle.tv-theme:hover {
  border-color: var(--accent-rose);
  background: rgba(244, 63, 94, 0.18);
}

/* Morphing Pills */
.morph-indicators {
  display: flex;
  align-items: center;
  gap: 6px;
}

.morph-pill {
  position: relative;
  height: 6px;
  width: 6px;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.25);
  border: none;
  cursor: pointer;
  padding: 0;
  transition: width 0.45s cubic-bezier(0.16, 1, 0.3, 1),
              background-color 0.3s ease,
              opacity 0.3s ease;
  overflow: hidden;
}

.morph-pill:hover {
  background: rgba(255, 255, 255, 0.5);
}

/* Active Morph State: Expands from 6px circle to 26px pill */
.morph-pill.active {
  width: 26px;
  background: rgba(255, 255, 255, 0.25);
}

/* Fill animation indicating slide countdown */
.progress-fill {
  position: absolute;
  inset: 0;
  background: var(--accent-amber);
  border-radius: 9999px;
  transform-origin: left center;
  animation: fillProgress 6.5s linear forwards;
}

.progress-fill.tv-theme {
  background: var(--accent-rose);
}

.morph-pill.paused .progress-fill {
  animation-play-state: paused;
}

@keyframes fillProgress {
  0% {
    transform: scaleX(0);
  }
  100% {
    transform: scaleX(1);
  }
}
</style>
