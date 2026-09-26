<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { tvSpatialNav } from '../services/tvSpatialNav';
import { MediaItem } from '../types';
import { useApi } from '../composables/useApi';
import { useHaptics } from '../composables/useHaptics';
import { useInfiniteScroll } from '../composables/useInfiniteScroll';
import {
  Flame,
  TrendingUp,
  Star,
  Tv,
  Clock,
  ChevronDown,
  Loader2
} from 'lucide-vue-next';
import MovieCard from '../components/MovieCard.vue';
import FeaturedHero from '../components/FeaturedHero.vue';

const emit = defineEmits<{
  (e: 'select', item: MediaItem): void;
  (e: 'play', item: MediaItem): void;
}>();

const { getMediaList } = useApi();
const { triggerLight } = useHaptics();

const items = ref<MediaItem[]>([]);
const isLoading = ref(true);
const page = ref(1);
const totalPages = ref(1);
const activeFilter = ref<string>('popular');

// Hero banner candidates — strictly 10 random TV images with backdrops
const heroItems = ref<MediaItem[]>([]);

const categories = [
  { key: 'popular', label: 'Popular', icon: Flame, api: 'popular' },
  { key: 'trending', label: 'Trending', icon: TrendingUp, api: 'trending', extra: { time: 'week' } },
  { key: 'imdb_top', label: 'IMDb Top', icon: Star, api: 'imdb' },
  { key: 'on_the_air', label: 'On Air', icon: Tv, api: 'on_the_air' },
  { key: 'airing_today', label: 'Airing Today', icon: Clock, api: 'airing_today' }
];

const categoryBarRef = ref<HTMLElement | null>(null);

const loadData = async (reset = true) => {
  if (reset) {
    page.value = 1;
    totalPages.value = 1;
    isLoading.value = true;
    isLoadingMore.value = false;
    items.value = [];
    heroItems.value = [];
  }

  const currentCat = categories.find((c) => c.key === activeFilter.value) || categories[0];
  const queryParams: Record<string, string | number | undefined> = {
    api: currentCat.api,
    mode: 'tv',
    page: page.value,
    sort: 'popularity.desc',
    ...(currentCat.extra || {})
  };

  try {
    const data = await getMediaList(queryParams);
    const results = (data.results || []).filter((x) => x.media_type !== 'person');

    if (reset) {
      items.value = results;

      // Select up to 10 random TV items with valid images
      const withBackdrops = results.filter((m) => Boolean(m.backdrop_path));
      const candidates = withBackdrops.length >= 10
        ? withBackdrops
        : results.filter((m) => Boolean(m.backdrop_path || m.poster_path));

      // Fisher-Yates shuffle
      const pool = [...(candidates.length > 0 ? candidates : results)];
      for (let i = pool.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [pool[i], pool[j]] = [pool[j], pool[i]];
      }

      heroItems.value = pool.slice(0, 10);
    } else {
      const existingIds = new Set(items.value.map((x) => x.id));
      const uniqueResults = results.filter((x) => !existingIds.has(x.id));
      if (uniqueResults.length > 0) {
        items.value.push(...uniqueResults);
      }
    }
    totalPages.value = data.total_pages || 1;
  } catch (error) {
    console.error('Failed to load TV series list:', error);
  } finally {
    isLoading.value = false;
  }
};

const handleSelectFilter = (catKey: string) => {
  if (activeFilter.value !== catKey) {
    triggerLight();
    activeFilter.value = catKey;
    loadData(true);
  }
};

const isLoadingMore = ref(false);

const loadMore = async () => {
  if (isLoadingMore.value || isLoading.value || page.value >= totalPages.value) return;

  const activeEl = document.activeElement as HTMLElement | null;
  const wasLoadMore = activeEl?.classList.contains('load-more-btn');
  const focusedCardId = activeEl?.getAttribute('data-card-id');
  const prevCount = items.value.length;

  isLoadingMore.value = true;
  try {
    page.value++;
    await loadData(false);
    await nextTick();

    // Maintain focus on newly loaded items or keep current focus
    if (wasLoadMore) {
      const allCards = document.querySelectorAll<HTMLElement>('.movies-grid .movie-card');
      if (allCards.length > prevCount && allCards[prevCount]) {
        tvSpatialNav.focusElement(allCards[prevCount]);
      } else {
        const loadBtn = document.querySelector<HTMLElement>('.load-more-btn');
        if (loadBtn) tvSpatialNav.focusElement(loadBtn);
      }
    } else if (focusedCardId) {
      const targetCard = document.querySelector<HTMLElement>(`[data-card-id="${focusedCardId}"]`);
      if (targetCard) {
        tvSpatialNav.focusElement(targetCard);
      }
    }
  } finally {
    isLoadingMore.value = false;
  }
};

const { sentinelRef } = useInfiniteScroll(
  loadMore,
  () => !isLoading.value && !isLoadingMore.value && page.value < totalPages.value,
  { distance: 400 }
);

onMounted(() => {
  window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
  loadData(true);
});

// Clean up references and arrays when unmounted to eliminate memory/image overload
onUnmounted(() => {
  items.value = [];
  heroItems.value = [];
});
</script>

<template>
  <div class="tv-view">
    <!-- Featured Hero Section with 10 Random TV Images -->
    <FeaturedHero
      v-if="heroItems.length > 0 && !isLoading"
      :items="heroItems"
      mode="tv"
      @select="emit('select', $event)"
      @play="emit('play', $event)"
    />

    <!-- Hero Skeleton when loading -->
    <div v-else-if="isLoading" class="skeleton hero-skeleton"></div>

    <!-- Category Pills Bar -->
    <div ref="categoryBarRef" class="category-bar no-scrollbar">
      <button
        v-for="cat in categories"
        :key="cat.key"
        class="cat-chip tv-theme"
        :class="{ active: activeFilter === cat.key }"
        @click="handleSelectFilter(cat.key)"
      >
        <component :is="cat.icon" :size="13" />
        <span>{{ cat.label }}</span>
      </button>
    </div>

    <!-- Media Grid -->
    <div class="content-section">
      <div class="section-title-row">
        <h2 class="section-heading font-display">Popular Series</h2>
        <span class="count-tag font-mono">{{ items.length }} titles</span>
      </div>

      <!-- Loading Skeletons -->
      <div v-if="isLoading && items.length === 0" class="movies-grid">
        <div v-for="n in 6" :key="n" class="skeleton card-skeleton"></div>
      </div>

      <!-- Actual TV Grid -->
      <div v-else class="movies-grid">
        <MovieCard
          v-for="m in items"
          :key="m.id"
          :item="m"
          mode="tv"
          @select="emit('select', $event)"
        />
      </div>

      <!-- Infinite Scroll Trigger Sentinel & Loading State (Button stays permanently mounted) -->
      <div
        v-if="items.length > 0 && page < totalPages"
        ref="sentinelRef"
        class="load-more-wrap"
      >
        <button
          class="load-more-btn tv-theme"
          :class="{ 'is-loading': isLoadingMore }"
          :aria-busy="isLoadingMore"
          @click="loadMore"
        >
          <div v-if="isLoadingMore" class="loading-indicator-inline">
            <Loader2 :size="18" class="spin" />
            <span>Loading more series...</span>
          </div>
          <div v-else class="load-more-content">
            <span>Load More</span>
            <ChevronDown :size="16" />
          </div>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tv-view {
  display: flex;
  flex-direction: column;
  padding-bottom: 2rem;
}

.hero-skeleton {
  width: 100%;
  height: 420px;
  border-radius: 0;
}

@media (min-width: 768px) and (min-height: 551px) {
  .hero-skeleton {
    height: 500px;
  }
}

@media (min-width: 1024px) and (min-height: 551px) {
  .hero-skeleton {
    height: 580px;
  }
}

@media (orientation: landscape) and (max-height: 550px) {
  .hero-skeleton {
    height: clamp(240px, 72vh, 320px);
  }
}

/* Category bar */
.category-bar {
  display: flex;
  gap: 0.5rem;
  overflow-x: auto;
  padding: 0.85rem max(1rem, var(--safe-left, 0px));
  padding-right: max(1rem, var(--safe-right, 0px));
  background: rgba(14, 14, 21, 0.72);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-subtle);
  position: sticky;
  top: calc(var(--safe-top) + var(--header-height));
  z-index: 40;
}

@media (orientation: landscape) and (max-height: 550px) {
  .category-bar {
    padding: 0.5rem max(1rem, var(--safe-left, 0px));
    padding-right: max(1rem, var(--safe-right, 0px));
  }
}

.cat-chip {
  padding: 0.4rem 0.85rem;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.cat-chip.active.tv-theme {
  background: var(--accent-rose);
  color: #fff;
  border-color: var(--accent-rose);
}

/* Content */
.content-section {
  padding: 1.25rem max(1rem, var(--safe-left, 0px));
  padding-right: max(1rem, var(--safe-right, 0px));
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

@media (min-width: 768px) and (min-height: 551px) {
  .content-section {
    padding: 1.75rem max(1.5rem, var(--safe-left, 0px));
    padding-right: max(1.5rem, var(--safe-right, 0px));
  }
}

@media (orientation: landscape) and (max-height: 550px) {
  .content-section {
    padding: 1rem max(1rem, var(--safe-left, 0px));
    padding-right: max(1rem, var(--safe-right, 0px));
    gap: 0.75rem;
  }
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-heading {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text-primary);
}

.count-tag {
  font-size: 0.68rem;
  color: var(--text-muted);
  background: var(--bg-card);
  padding: 2px 6px;
  border-radius: 4px;
}

/* Movies/Series Grid */
.movies-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

@media (min-width: 480px) {
  .movies-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 1rem;
  }
}

@media (min-width: 768px) and (min-height: 551px) {
  .movies-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 1.25rem;
  }
}

@media (orientation: landscape) and (max-height: 550px) {
  .movies-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 0.85rem;
  }
}

@media (min-width: 1024px) {
  .movies-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 1.25rem;
  }
}

@media (min-width: 1280px) {
  .movies-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 1.5rem;
  }
}

.card-skeleton {
  aspect-ratio: 2 / 3;
  border-radius: var(--radius-md);
}

/* Load More */
.load-more-wrap {
  display: flex;
  justify-content: center;
  margin-top: 1rem;
}

.load-more-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  font-size: 0.82rem;
  font-weight: 600;
  padding: 0.65rem 1.4rem;
  border-radius: var(--radius-full);
  transition: all 0.2s ease;
}

.load-more-btn.tv-theme:hover {
  border-color: var(--accent-rose);
}

.load-more-btn:disabled,
.load-more-btn.is-loading {
  opacity: 0.85;
  cursor: wait;
}

.load-more-content,
.loading-indicator-inline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  color: var(--accent-amber);
  font-size: 0.85rem;
  font-weight: 500;
  padding: 0.65rem 1.4rem;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
}

.loading-indicator.tv-theme {
  color: var(--accent-rose);
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
