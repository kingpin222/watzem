<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import { MediaItem, MediaMode } from '../types';
import { useApi } from '../composables/useApi';
import { useHaptics } from '../composables/useHaptics';
import { useInfiniteScroll } from '../composables/useInfiniteScroll';
import { useRecentSearches } from '../composables/useRecentSearches';
import { Search, SearchX, X, TrendingUp, Clock, Trash2, ChevronDown, Loader2 } from 'lucide-vue-next';
import MovieCard from '../components/MovieCard.vue';

const props = defineProps<{
  mode: MediaMode;
}>();

const emit = defineEmits<{
  (e: 'select', item: MediaItem): void;
}>();

const { searchMedia, getTrendingSearches } = useApi();
const { triggerLight } = useHaptics();
const { recent, push: pushRecent, remove: removeRecent, clear: clearRecent } = useRecentSearches();

const searchQuery = ref('');
const searchResults = ref<MediaItem[]>([]);
const isSearching = ref(false);
const hasSearched = ref(false);
const searchPage = ref(1);
const searchTotalPages = ref(1);
const isLoadingMoreSearch = ref(false);
let debounceTimer: ReturnType<typeof setTimeout> | null = null;

// Live trending searches — populated on mount from TMDB/IMDB source
const suggestions = ref<string[]>([]);
const isLoadingTrending = ref(false);

const performSearch = async (query: string, page = 1) => {
  const trimmed = query.trim();
  if (trimmed.length < 2) {
    searchResults.value = [];
    hasSearched.value = false;
    isSearching.value = false;
    searchPage.value = 1;
    searchTotalPages.value = 1;
    return;
  }

  if (page === 1) {
    isSearching.value = true;
    searchPage.value = 1;
  } else {
    isLoadingMoreSearch.value = true;
  }
  hasSearched.value = true;

  try {
    const res = await searchMedia(trimmed, props.mode, page);
    const filtered = (res.results || []).filter((item) => item.media_type !== 'person');
    if (page === 1) {
      searchResults.value = filtered;
      // Save to recent only when we get at least 1 result
      if (filtered.length > 0) pushRecent(trimmed);
    } else {
      searchResults.value.push(...filtered);
    }
    searchPage.value = page;
    searchTotalPages.value = res.total_pages || 1;
  } catch (err) {
    console.error('Search error:', err);
    if (page === 1) searchResults.value = [];
  } finally {
    isSearching.value = false;
    isLoadingMoreSearch.value = false;
  }
};

const loadMoreSearch = async () => {
  if (isSearching.value || isLoadingMoreSearch.value || searchPage.value >= searchTotalPages.value) {
    return;
  }
  triggerLight();
  await performSearch(searchQuery.value, searchPage.value + 1);
};

const { sentinelRef: searchSentinelRef } = useInfiniteScroll(
  loadMoreSearch,
  () => Boolean(
    !isSearching.value &&
    !isLoadingMoreSearch.value &&
    searchQuery.value.trim().length >= 2 &&
    searchPage.value < searchTotalPages.value
  ),
  { distance: 400 }
);

const handleInput = () => {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    performSearch(searchQuery.value);
  }, 400);
};

const clearSearch = () => {
  triggerLight();
  searchQuery.value = '';
  searchResults.value = [];
  hasSearched.value = false;
  searchPage.value = 1;
  searchTotalPages.value = 1;
};

const pickSuggestion = (term: string) => {
  triggerLight();
  searchQuery.value = term;
  performSearch(term);
};

const pickRecent = (term: string) => {
  triggerLight();
  searchQuery.value = term;
  performSearch(term);
};

const handleRemoveRecent = (term: string) => {
  triggerLight();
  removeRecent(term);
};

const handleClearRecent = () => {
  triggerLight();
  clearRecent();
};

onMounted(async () => {
  isLoadingTrending.value = true;
  try {
    suggestions.value = await getTrendingSearches();
  } catch {
    suggestions.value = [];
  } finally {
    isLoadingTrending.value = false;
  }
});

watch(
  () => props.mode,
  () => {
    if (searchQuery.value.trim().length >= 2) {
      performSearch(searchQuery.value);
    }
  }
);
</script>

<template>
  <div class="search-view">
    <!-- Search Bar Input -->
    <div class="search-input-wrapper">
      <div class="search-field">
        <Search class="search-icon" :size="18" stroke-width="2.2" />

        <input
          v-model="searchQuery"
          type="text"
          :placeholder="mode === 'movie' ? 'Search movies by title, actor...' : 'Search TV shows, series...'"
          class="search-input"
          @input="handleInput"
        />

        <button
          v-if="searchQuery"
          class="clear-btn"
          aria-label="Clear search"
          @click="clearSearch"
        >
          <X :size="16" stroke-width="2.5" />
        </button>
      </div>
    </div>

    <!-- Suggested Search Tags (if no query yet) -->
    <div v-if="!hasSearched" class="suggestions-section">

      <!-- Recent Searches -->
      <div v-if="recent.length > 0" class="recents-block">
        <div class="recents-header">
          <h3 class="suggestions-title">
            <Clock :size="13" />
            <span>Recent</span>
          </h3>
          <button class="clear-all-btn" title="Clear all recent searches" @click="handleClearRecent">
            <Trash2 :size="12" />
            <span>Clear all</span>
          </button>
        </div>
        <div class="recents-list">
          <button
            v-for="term in recent"
            :key="term"
            class="recent-item"
            @click="pickRecent(term)"
          >
            <Clock :size="13" class="recent-clock" />
            <span class="recent-text">{{ term }}</span>
            <span
              class="recent-remove"
              role="button"
              :aria-label="`Remove ${term}`"
              @click.stop="handleRemoveRecent(term)"
            >
              <X :size="12" />
            </span>
          </button>
        </div>
      </div>

      <!-- Trending Searches -->
      <div class="trending-block">
        <h3 class="suggestions-title">
          <TrendingUp :size="14" />
          <span>Trending Now</span>
        </h3>
        <!-- Loading skeleton pills -->
        <div v-if="isLoadingTrending" class="tags-cloud">
          <span v-for="n in 8" :key="n" class="tag-skeleton skeleton"></span>
        </div>
        <!-- Live trending titles -->
        <div v-else class="tags-cloud">
          <button
            v-for="s in suggestions"
            :key="s"
            class="tag-btn font-mono"
            @click="pickSuggestion(s)"
          >
            <TrendingUp :size="11" />
            <span>{{ s }}</span>
          </button>
        </div>
      </div>

    </div>

    <!-- Results Section -->
    <div v-else class="results-section">
      <div class="results-meta">
        <span v-if="!isSearching" class="results-count font-mono">
          Found {{ searchResults.length }} result{{ searchResults.length === 1 ? '' : 's' }}
        </span>
        <span v-else class="results-count font-mono">Searching...</span>
      </div>

      <!-- Loading skeleton -->
      <div v-if="isSearching" class="movies-grid">
        <div v-for="n in 6" :key="n" class="skeleton card-skeleton"></div>
      </div>

      <!-- Results Grid & Sentinel -->
      <template v-else-if="searchResults.length > 0">
        <div class="movies-grid">
          <MovieCard
            v-for="item in searchResults"
            :key="item.id"
            :item="item"
            :mode="mode"
            @select="emit('select', $event)"
          />
        </div>

        <!-- Infinite Scroll Sentinel & Loading Indicator -->
        <div
          v-if="searchPage < searchTotalPages"
          ref="searchSentinelRef"
          class="load-more-wrap"
        >
          <div v-if="isLoadingMoreSearch" class="loading-indicator" :class="{ 'tv-theme': mode === 'tv' }">
            <Loader2 :size="18" class="spin" />
            <span>Loading more results...</span>
          </div>
          <button
            v-else
            class="load-more-btn"
            :class="{ 'tv-theme': mode === 'tv' }"
            @click="loadMoreSearch"
          >
            <span>Load More</span>
            <ChevronDown :size="16" />
          </button>
        </div>
      </template>

      <!-- No Results State -->
      <div v-else class="no-results">
        <div class="no-results-icon">
          <SearchX :size="40" stroke-width="1.5" />
        </div>
        <h3>No matches for "{{ searchQuery }}"</h3>
        <p>Check your spelling or try searching for another title.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-view {
  padding: 1rem max(1rem, var(--safe-left, 0px)) 3rem;
  padding-right: max(1rem, var(--safe-right, 0px));
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

@media (min-width: 768px) {
  .search-view {
    padding: 1.75rem max(1.5rem, var(--safe-left, 0px)) 4rem;
    padding-right: max(1.5rem, var(--safe-right, 0px));
  }
}

.search-input-wrapper {
  position: sticky;
  top: calc(var(--safe-top) + var(--header-height));
  z-index: 30;
  background: var(--bg-main);
  padding-bottom: 0.5rem;
  width: 100%;
}

@media (min-width: 768px) {
  .search-field {
    max-width: 640px;
    margin: 0 auto;
  }
  .suggestions-section {
    max-width: 640px;
    margin: 0 auto;
    width: 100%;
  }
}

.search-field {
  position: relative;
  display: flex;
  align-items: center;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 0 0.85rem;
  transition: border-color 0.2s ease;
}

.search-field:focus-within {
  border-color: var(--accent-amber);
  box-shadow: 0 0 0 1px var(--accent-amber);
}

.search-icon {
  color: var(--text-muted);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  padding: 0.75rem 0.65rem;
  color: var(--text-primary);
  font-size: 0.88rem;
  font-family: inherit;
}

.search-input::placeholder {
  color: var(--text-muted);
}

.clear-btn {
  color: var(--text-muted);
  display: grid;
  place-items: center;
  padding: 4px;
}

/* Suggestions */
.suggestions-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.recents-block,
.trending-block {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.recents-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.suggestions-title {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.clear-all-btn {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.7rem;
  font-weight: 500;
  color: var(--text-muted);
  padding: 3px 6px;
  border-radius: var(--radius-sm);
  transition: color 0.2s ease;
}
.clear-all-btn:hover {
  color: var(--accent-rose);
}

/* Recent items list */
.recents-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.62rem 0.9rem;
  border-bottom: 1px solid var(--border-subtle);
  text-align: left;
  width: 100%;
  transition: background 0.15s ease;
}
.recent-item:last-child { border-bottom: none; }
.recent-item:hover { background: rgba(255,255,255,0.04); }

.recent-clock { color: var(--text-muted); flex-shrink: 0; }

.recent-text {
  flex: 1;
  font-size: 0.83rem;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-remove {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: var(--text-muted);
  flex-shrink: 0;
  transition: color 0.15s ease, background 0.15s ease;
}
.recent-remove:hover {
  color: var(--text-primary);
  background: rgba(255,255,255,0.1);
}

.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tag-btn {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.45rem 0.8rem;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 0.75rem;
  transition: all 0.2s ease;
}

.tag-btn:hover {
  border-color: var(--accent-amber);
  color: var(--text-primary);
}

.tag-skeleton {
  display: inline-block;
  height: 32px;
  width: 90px;
  border-radius: var(--radius-sm);
}
.tag-skeleton:nth-child(2n) { width: 70px; }
.tag-skeleton:nth-child(3n) { width: 110px; }

/* Results */
.results-section {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.results-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.results-count {
  font-size: 0.72rem;
  color: var(--text-muted);
}

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
    gap: 1.5rem;
  }
}

.card-skeleton {
  aspect-ratio: 2 / 3;
  border-radius: var(--radius-md);
}

.no-results {
  text-align: center;
  padding: 3rem 1rem;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.no-results-icon {
  font-size: 2.2rem;
  margin-bottom: 0.5rem;
  opacity: 0.4;
}

.no-results h3 {
  font-size: 0.95rem;
  color: var(--text-primary);
  margin-bottom: 0.25rem;
}

.no-results p {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  margin-top: 1.5rem;
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

.load-more-btn:hover {
  border-color: var(--accent-amber);
}

.load-more-btn.tv-theme:hover {
  border-color: var(--accent-rose);
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
