<script setup lang="ts">
import { ref, computed } from 'vue';
import { MediaItem, MediaMode } from '../types';
import { useWatchlist } from '../composables/useWatchlist';
import { useHaptics } from '../composables/useHaptics';
import { Bookmark, Compass } from 'lucide-vue-next';
import MovieCard from '../components/MovieCard.vue';

defineProps<{
  mode: MediaMode;
}>();

const emit = defineEmits<{
  (e: 'select', item: MediaItem): void;
  (e: 'explore'): void;
}>();

const { watchlist } = useWatchlist();
const { triggerLight } = useHaptics();

const filterType = ref<'all' | 'movie' | 'tv'>('all');

const filteredItems = computed(() => {
  if (filterType.value === 'all') return watchlist.value;
  return watchlist.value.filter((item) => {
    const isTv = item.media_type === 'tv' || !item.release_date;
    return filterType.value === 'tv' ? isTv : !isTv;
  });
});

const setFilter = (type: 'all' | 'movie' | 'tv') => {
  triggerLight();
  filterType.value = type;
};
</script>

<template>
  <div class="watchlist-view">
    <div class="watchlist-header">
      <div>
        <h2 class="watchlist-title font-display">My Watchlist</h2>
        <span class="watchlist-sub">Saved movies and TV series</span>
      </div>

      <!-- Type Filter -->
      <div v-if="watchlist.length > 0" class="filter-pills">
        <button
          class="pill-opt"
          :class="{ active: filterType === 'all' }"
          @click="setFilter('all')"
        >
          All ({{ watchlist.length }})
        </button>
        <button
          class="pill-opt"
          :class="{ active: filterType === 'movie' }"
          @click="setFilter('movie')"
        >
          Movies
        </button>
        <button
          class="pill-opt"
          :class="{ active: filterType === 'tv' }"
          @click="setFilter('tv')"
        >
          TV
        </button>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="watchlist.length === 0" class="empty-watchlist">
      <div class="empty-icon-box">
        <Bookmark :size="48" :stroke-width="1.5" />
      </div>
      <h3 class="empty-title">Your Watchlist is empty</h3>
      <p class="empty-text">
        Tap the bookmark icon on any movie or series to save it for quick access anytime.
      </p>
      <button class="btn-discover" @click="emit('explore')">
        <Compass :size="16" />
        <span>Explore Movies & Shows</span>
      </button>
    </div>

    <!-- Watchlist Grid -->
    <div v-else class="watchlist-content">
      <div class="movies-grid">
        <MovieCard
          v-for="item in filteredItems"
          :key="item.id"
          :item="item"
          :mode="item.media_type === 'tv' ? 'tv' : 'movie'"
          @select="emit('select', $event)"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Watchlist View - 100% Fluid */
.watchlist-view {
  width: 100%;
  max-width: 100%;
  padding: 1.25rem max(clamp(1rem, 2.5vw, 2.5rem), var(--safe-left, 0px)) 3rem;
  padding-right: max(clamp(1rem, 2.5vw, 2.5rem), var(--safe-right, 0px));
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

@media (min-width: 768px) {
  .watchlist-view {
    padding: 1.75rem max(clamp(1.5rem, 3vw, 3rem), var(--safe-left, 0px)) 4rem;
    padding-right: max(clamp(1.5rem, 3vw, 3rem), var(--safe-right, 0px));
  }
}

.watchlist-header {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--border-subtle);
}

.watchlist-title {
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--text-primary);
}

.watchlist-sub {
  font-size: 0.78rem;
  color: var(--text-muted);
}

.filter-pills {
  display: flex;
  gap: 0.4rem;
}

.pill-opt {
  padding: 0.35rem 0.75rem;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  color: var(--text-secondary);
  transition: all 0.2s ease;
}

.pill-opt.active {
  background: var(--accent-amber);
  color: #07070a;
  font-weight: 700;
}

/* Empty state */
.empty-watchlist {
  text-align: center;
  padding: 4rem 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.empty-icon-box {
  color: var(--text-muted);
  opacity: 0.4;
  margin-bottom: 0.5rem;
}

.empty-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-primary);
}

.empty-text {
  font-size: 0.82rem;
  color: var(--text-secondary);
  max-width: 320px;
  line-height: 1.5;
}

.btn-discover {
  margin-top: 0.85rem;
  padding: 0.7rem 1.4rem;
  background: var(--accent-amber);
  color: #07070a;
  font-size: 0.85rem;
  font-weight: 700;
  border-radius: var(--radius-full);
  box-shadow: 0 4px 16px var(--accent-amber-glow);
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

/* Grid - Fluid Auto-Scaling */
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

@media (min-width: 768px) {
  .movies-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 1.25rem;
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
    gap: 1.35rem;
  }
}

@media (min-width: 1536px) {
  .movies-grid {
    grid-template-columns: repeat(7, minmax(0, 1fr));
    gap: 1.4rem;
  }
}

@media (min-width: 1920px) {
  .movies-grid {
    grid-template-columns: repeat(8, minmax(0, 1fr));
    gap: 1.5rem;
  }
}

@media (min-width: 2560px) {
  .movies-grid {
    grid-template-columns: repeat(10, minmax(0, 1fr));
    gap: 1.6rem;
  }
}

@media (orientation: landscape) {
  .watchlist-view {
    padding: 1rem max(clamp(1.25rem, 2.5vw, 2.5rem), var(--safe-left, 0px)) 3rem;
    padding-right: max(clamp(1.25rem, 2.5vw, 2.5rem), var(--safe-right, 0px));
    gap: 0.85rem;
  }
  .watchlist-header {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 0.5rem;
  }
  .movies-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 0.85rem;
  }
}

@media (orientation: landscape) and (min-width: 900px) {
  .movies-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 1rem;
  }
}

@media (orientation: landscape) and (min-width: 1200px) {
  .movies-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 1.15rem;
  }
}

@media (orientation: landscape) and (min-width: 1536px) {
  .movies-grid {
    grid-template-columns: repeat(7, minmax(0, 1fr));
  }
}

@media (orientation: landscape) and (min-width: 1920px) {
  .movies-grid {
    grid-template-columns: repeat(8, minmax(0, 1fr));
  }
}
</style>
