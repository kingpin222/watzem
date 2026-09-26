<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import { tvSpatialNav } from '../services/tvSpatialNav';
import { MediaItem, MediaMode, PersonSearchResult } from '../types';
import { useApi } from '../composables/useApi';
import { useHaptics } from '../composables/useHaptics';
import { useInfiniteScroll } from '../composables/useInfiniteScroll';
import { useRecentSearches } from '../composables/useRecentSearches';
import {
  Search,
  SearchX,
  X,
  TrendingUp,
  Clock,
  Trash2,
  ChevronDown,
  Loader2,
  Layers,
  Film,
  Tv,
  Sparkles,
  Users
} from 'lucide-vue-next';
import MovieCard from '../components/MovieCard.vue';

export type SearchCategory = 'multi' | 'movie' | 'tv';

const props = withDefaults(
  defineProps<{
    mode: MediaMode;
    initialQuery?: string;
    initialActor?: PersonSearchResult | null;
  }>(),
  {
    initialQuery: '',
    initialActor: null
  }
);

const emit = defineEmits<{
  (e: 'select', item: MediaItem): void;
}>();

const { searchMedia, getTrendingSearches, searchPerson, getActorCredits, getImageUrl } = useApi();
const { triggerLight } = useHaptics();
const { recent, push: pushRecent, remove: removeRecent, clear: clearRecent } = useRecentSearches();

const searchQuery = ref('');
const searchResults = ref<MediaItem[]>([]);
const mediaSearchResults = ref<MediaItem[]>([]);
const mediaTotalPages = ref(1);
const searchViewMode = ref<'media' | 'actor'>('media');
const isSearching = ref(false);
const hasSearched = ref(false);
const searchPage = ref(1);
const searchTotalPages = ref(1);
const isLoadingMoreSearch = ref(false);
const activeCategory = ref<SearchCategory>('multi');
const activeActor = ref<PersonSearchResult | null>(null);
const actorAllCredits = ref<MediaItem[]>([]);
let debounceTimer: ReturnType<typeof setTimeout> | null = null;

const searchCategories = [
  { key: 'multi' as const, label: 'All', icon: Layers },
  { key: 'movie' as const, label: 'Movies', icon: Film },
  { key: 'tv' as const, label: 'TV Shows', icon: Tv }
];

const filterCreditsByCategory = (credits: MediaItem[], cat: SearchCategory) => {
  if (cat === 'movie') return credits.filter((m) => m.media_type === 'movie' || (!m.first_air_date && m.title));
  if (cat === 'tv') return credits.filter((m) => m.media_type === 'tv' || Boolean(m.first_air_date || m.name));
  return credits;
};

const movieCount = computed(() => {
  const source = searchViewMode.value === 'actor' && actorAllCredits.value.length > 0
    ? actorAllCredits.value
    : mediaSearchResults.value;
  return source.filter((item) => {
    return item.media_type === 'movie' || (!item.first_air_date && !item.name);
  }).length;
});

const tvCount = computed(() => {
  const source = searchViewMode.value === 'actor' && actorAllCredits.value.length > 0
    ? actorAllCredits.value
    : mediaSearchResults.value;
  return source.filter((item) => {
    return item.media_type === 'tv' || Boolean(item.first_air_date || item.name);
  }).length;
});

const setSearchViewMode = (mode: 'media' | 'actor') => {
  if (searchViewMode.value === mode) return;
  triggerLight();
  searchViewMode.value = mode;
  searchPage.value = 1;
  if (mode === 'actor' && activeActor.value && actorAllCredits.value.length > 0) {
    const filtered = filterCreditsByCategory(actorAllCredits.value, activeCategory.value);
    searchResults.value = filtered.slice(0, 20);
    searchTotalPages.value = Math.max(1, Math.ceil(filtered.length / 20));
  } else {
    searchResults.value = mediaSearchResults.value;
    searchTotalPages.value = mediaTotalPages.value;
  }
};

// Live trending searches — populated on mount from TMDB/IMDB source
const suggestions = ref<string[]>([]);
const isLoadingTrending = ref(false);

const performSearch = async (query: string, page = 1) => {
  const trimmed = query.trim();
  if (trimmed.length < 2) {
    searchResults.value = [];
    mediaSearchResults.value = [];
    activeActor.value = null;
    actorAllCredits.value = [];
    searchViewMode.value = 'media';
    hasSearched.value = false;
    isSearching.value = false;
    searchPage.value = 1;
    searchTotalPages.value = 1;
    mediaTotalPages.value = 1;
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
    if (page === 1) {
      // 1. Parallel lookup for actor and media
      const [personRes, mediaRes] = await Promise.all([
        searchPerson(trimmed),
        searchMedia(trimmed, activeCategory.value, 1)
      ]);

      const normalize = (str: string) =>
        str.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();

      const cleanQuery = normalize(trimmed);
      const personName = personRes ? normalize(personRes.name) : '';

      // 2. Filter & store raw media results
      const rawMedia = (mediaRes.results || []).filter((item) => item.media_type !== 'person');
      mediaSearchResults.value = rawMedia;
      mediaTotalPages.value = mediaRes.total_pages || 1;

      // 3. Check if any media item has an exact/prominent title match with the search query
      const hasExactMediaTitleMatch = rawMedia.some((item) => {
        const title = normalize(item.title || item.name || '');
        return title === cleanQuery || title.startsWith(`${cleanQuery}:`) || title.startsWith(`${cleanQuery} `);
      });

      // 4. Strict actor match detection:
      // An actor query is only when the query matches the actor's full name,
      // NOT a random single word that merely appears in an actor's name (e.g. "Logan", "Barbie", "Creed")!
      const isMultiWord = cleanQuery.split(/\s+/).length >= 2;
      const isExactActorName = Boolean(
        personRes && (
          personName === cleanQuery ||
          (isMultiWord && (personName.startsWith(cleanQuery) || cleanQuery.startsWith(personName)))
        )
      );

      // 5. Fetch actor filmography if valid actor candidate found
      if (personRes && (isExactActorName || (personRes.popularity || 0) >= 3.0)) {
        activeActor.value = personRes;
        try {
          const credits = await getActorCredits(personRes.id);
          actorAllCredits.value = credits;
        } catch {
          actorAllCredits.value = [];
        }
      } else {
        activeActor.value = null;
        actorAllCredits.value = [];
      }

      // 6. View mode decision:
      // Prioritize actor view ONLY if the query is an exact actor name AND does NOT conflict with a prominent media title!
      // (e.g. "Tom Cruise" -> actor mode)
      // (e.g. "Logan" or "Barbie" or "Creed" -> media mode, showing the movie)
      if (isExactActorName && !hasExactMediaTitleMatch && actorAllCredits.value.length > 0) {
        searchViewMode.value = 'actor';
        const filtered = filterCreditsByCategory(actorAllCredits.value, activeCategory.value);
        searchResults.value = filtered.slice(0, 20);
        searchTotalPages.value = Math.max(1, Math.ceil(filtered.length / 20));
        pushRecent(personRes!.name);
      } else {
        searchViewMode.value = 'media';
        searchResults.value = rawMedia;
        searchTotalPages.value = mediaTotalPages.value;
        if (rawMedia.length > 0) pushRecent(trimmed);
      }
      searchPage.value = 1;
    } else {
      // Pagination for page > 1
      if (searchViewMode.value === 'actor' && actorAllCredits.value.length > 0) {
        const filtered = filterCreditsByCategory(actorAllCredits.value, activeCategory.value);
        const start = (page - 1) * 20;
        const nextBatch = filtered.slice(start, start + 20);
        const existingIds = new Set(searchResults.value.map((x) => `${x.media_type}-${x.id}`));
        const unique = nextBatch.filter((x) => !existingIds.has(`${x.media_type}-${x.id}`));
        if (unique.length > 0) {
          searchResults.value.push(...unique);
        }
        searchPage.value = page;
        searchTotalPages.value = Math.max(1, Math.ceil(filtered.length / 20));
      } else {
        const res = await searchMedia(trimmed, activeCategory.value, page);
        const filtered = (res.results || []).filter((item) => item.media_type !== 'person');
        const existingIds = new Set(mediaSearchResults.value.map((x) => x.id));
        const uniqueResults = filtered.filter((x) => !existingIds.has(x.id));
        if (uniqueResults.length > 0) {
          mediaSearchResults.value.push(...uniqueResults);
          searchResults.value.push(...uniqueResults);
        }
        searchPage.value = page;
        searchTotalPages.value = res.total_pages || 1;
        mediaTotalPages.value = res.total_pages || 1;
      }
    }
  } catch (err) {
    console.error('Search error:', err);
    if (page === 1) searchResults.value = [];
  } finally {
    isSearching.value = false;
    isLoadingMoreSearch.value = false;
  }
};

const handleSelectCategory = (cat: SearchCategory) => {
  if (activeCategory.value === cat) return;
  triggerLight();
  activeCategory.value = cat;
  if (searchViewMode.value === 'actor' && actorAllCredits.value.length > 0) {
    const filtered = filterCreditsByCategory(actorAllCredits.value, cat);
    searchResults.value = filtered.slice(0, 20);
    searchPage.value = 1;
    searchTotalPages.value = Math.max(1, Math.ceil(filtered.length / 20));
  } else if (searchQuery.value.trim().length >= 2) {
    performSearch(searchQuery.value, 1);
  }
};

const loadMoreSearch = async () => {
  if (isSearching.value || isLoadingMoreSearch.value || searchPage.value >= searchTotalPages.value) {
    return;
  }
  triggerLight();

  const activeEl = document.activeElement as HTMLElement | null;
  const wasLoadMore = activeEl?.classList.contains('load-more-btn');
  const focusedCardId = activeEl?.getAttribute('data-card-id');
  const prevCount = searchResults.value.length;

  await performSearch(searchQuery.value, searchPage.value + 1);
  await nextTick();

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
  mediaSearchResults.value = [];
  activeActor.value = null;
  actorAllCredits.value = [];
  searchViewMode.value = 'media';
  hasSearched.value = false;
  searchPage.value = 1;
  searchTotalPages.value = 1;
  mediaTotalPages.value = 1;
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

const searchActorDirectly = async (actor: PersonSearchResult) => {
  activeActor.value = actor;
  searchQuery.value = actor.name;
  searchViewMode.value = 'actor';
  isSearching.value = true;
  hasSearched.value = true;
  searchPage.value = 1;
  try {
    const [credits, mediaRes] = await Promise.all([
      getActorCredits(actor.id),
      searchMedia(actor.name, activeCategory.value, 1)
    ]);
    actorAllCredits.value = credits;
    mediaSearchResults.value = (mediaRes.results || []).filter((x) => x.media_type !== 'person');
    mediaTotalPages.value = mediaRes.total_pages || 1;

    const filtered = filterCreditsByCategory(credits, activeCategory.value);
    searchResults.value = filtered.slice(0, 20);
    searchTotalPages.value = Math.max(1, Math.ceil(filtered.length / 20));
    pushRecent(actor.name);
  } catch (err) {
    console.error('Failed to load actor filmography:', err);
    searchResults.value = [];
  } finally {
    isSearching.value = false;
  }
};

const clearActorSpotlight = () => {
  triggerLight();
  activeActor.value = null;
  actorAllCredits.value = [];
  searchViewMode.value = 'media';
  searchResults.value = mediaSearchResults.value;
  searchTotalPages.value = mediaTotalPages.value;
  searchPage.value = 1;
};

onMounted(async () => {
  if (props.initialActor) {
    searchActorDirectly(props.initialActor);
  } else if (props.initialQuery && props.initialQuery.trim()) {
    searchQuery.value = props.initialQuery.trim();
    performSearch(props.initialQuery.trim(), 1);
  }
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
  () => props.initialActor,
  (newActor) => {
    if (newActor) {
      searchActorDirectly(newActor);
    }
  }
);

watch(
  () => props.initialQuery,
  (newQ) => {
    if (newQ && newQ.trim() && !props.initialActor) {
      searchQuery.value = newQ.trim();
      performSearch(newQ.trim(), 1);
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
          placeholder="Search movies, TV shows, anime, titles..."
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

      <!-- Category Filter Pills (All / Movies / TV Shows) -->
      <div class="search-category-bar no-scrollbar">
        <button
          v-for="cat in searchCategories"
          :key="cat.key"
          type="button"
          class="search-cat-chip"
          :class="{
            active: activeCategory === cat.key,
            'cat-multi': cat.key === 'multi',
            'cat-movie': cat.key === 'movie',
            'cat-tv': cat.key === 'tv'
          }"
          :aria-pressed="activeCategory === cat.key"
          @click="handleSelectCategory(cat.key)"
        >
          <component :is="cat.icon" :size="14" class="cat-chip-icon" />
          <span>{{ cat.label }}</span>
          <span
            v-if="hasSearched && !isSearching && searchResults.length > 0 && activeCategory === 'multi'"
            class="cat-chip-badge"
          >
            {{ cat.key === 'multi' ? searchResults.length : cat.key === 'movie' ? movieCount : tvCount }}
          </span>
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
      <!-- Actor Spotlight Banner (When search query matches an actor) -->
      <div v-if="activeActor" class="actor-spotlight-card">
        <div class="actor-avatar-wrap">
          <img
            v-if="activeActor.profile_path"
            :src="getImageUrl(activeActor.profile_path, 'w185')"
            :alt="activeActor.name"
            class="actor-avatar-img"
          />
          <div v-else class="actor-avatar-placeholder font-mono">
            {{ activeActor.name.slice(0, 2).toUpperCase() }}
          </div>
          <div class="actor-badge-star">
            <Sparkles :size="12" />
          </div>
        </div>
        <div class="actor-details">
          <div class="actor-tag-pill font-mono">
            <Users :size="11" />
            <span>{{ searchViewMode === 'actor' ? 'Actor Filmography' : 'Actor Match' }}</span>
          </div>
          <h2 class="actor-name font-display">{{ activeActor.name }}</h2>
          <p class="actor-summary">
            Starring in {{ movieCount }} movies and {{ tvCount }} TV series
          </p>
          <!-- Quick switch button if media search results exist -->
          <button
            v-if="searchViewMode === 'actor' && mediaSearchResults.length > 0"
            type="button"
            class="actor-switch-pill font-mono"
            @click="setSearchViewMode('media')"
          >
            <Film :size="11" />
            <span>View {{ mediaSearchResults.length }} title{{ mediaSearchResults.length === 1 ? '' : 's' }}</span>
          </button>
          <button
            v-else-if="searchViewMode === 'media'"
            type="button"
            class="actor-switch-pill highlight font-mono"
            @click="setSearchViewMode('actor')"
          >
            <Sparkles :size="11" />
            <span>View filmography ({{ actorAllCredits.length }} titles)</span>
          </button>
        </div>
        <button
          type="button"
          class="actor-spotlight-close"
          title="Clear actor filter"
          @click="clearActorSpotlight"
        >
          <X :size="16" />
        </button>
      </div>

      <div class="results-meta">
        <div v-if="!isSearching" class="results-meta-left font-mono">
          <!-- View Mode Switcher if both actor and media results exist -->
          <div v-if="activeActor && mediaSearchResults.length > 0" class="view-mode-toggle">
            <button
              class="toggle-tab font-mono"
              :class="{ active: searchViewMode === 'media' }"
              type="button"
              @click="setSearchViewMode('media')"
            >
              <Film :size="12" />
              <span>Titles ({{ mediaSearchResults.length }})</span>
            </button>
            <button
              class="toggle-tab font-mono"
              :class="{ active: searchViewMode === 'actor' }"
              type="button"
              @click="setSearchViewMode('actor')"
            >
              <Users :size="12" />
              <span>{{ activeActor.name }} ({{ actorAllCredits.length }})</span>
            </button>
          </div>

          <span v-else class="results-count">
            {{ searchViewMode === 'actor' ? `Filmography (${searchResults.length} titles)` : `Found ${searchResults.length} result${searchResults.length === 1 ? '' : 's'}` }}
          </span>

          <span v-if="activeCategory === 'multi' && searchResults.length > 0" class="results-breakdown">
            <span class="breakdown-tag movie">🎬 {{ movieCount }} Movies</span>
            <span class="breakdown-tag tv">📺 {{ tvCount }} TV Shows</span>
          </span>
        </div>
        <span v-else class="results-count font-mono">{{ activeActor ? `Loading ${activeActor.name}'s movies...` : 'Searching titles...' }}</span>
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
            :key="`${item.media_type || 'm'}-${item.id}`"
            :item="item"
            :mode="(item.media_type as MediaMode) || (item.name ? 'tv' : 'movie')"
            @select="emit('select', $event)"
          />
        </div>

        <!-- Infinite Scroll Sentinel & Loading Indicator (Button stays permanently mounted) -->
        <div
          v-if="searchPage < searchTotalPages"
          ref="searchSentinelRef"
          class="load-more-wrap"
        >
          <button
            class="load-more-btn"
            :class="{ 'tv-theme': activeCategory === 'tv', 'is-loading': isLoadingMoreSearch }"
            :aria-busy="isLoadingMoreSearch"
            @click="loadMoreSearch"
          >
            <div v-if="isLoadingMoreSearch" class="loading-indicator-inline">
              <Loader2 :size="18" class="spin" />
              <span>Loading more results...</span>
            </div>
            <div v-else class="load-more-content">
              <span>Load More</span>
              <ChevronDown :size="16" />
            </div>
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
  .search-category-bar {
    max-width: 640px;
    margin: 0.65rem auto 0;
  }
  .suggestions-section {
    max-width: 640px;
    margin: 0 auto;
    width: 100%;
  }
}

.search-category-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 0.65rem;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding: 2px 4px;
}

.search-cat-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.42rem 0.95rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  white-space: nowrap;
  user-select: none;
}

.search-cat-chip:hover {
  color: var(--text-primary);
  border-color: rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.05);
}

.search-cat-chip.active {
  color: #0b0f19;
  background: #f59e0b;
  border-color: #f59e0b;
  box-shadow: 0 2px 10px rgba(245, 158, 11, 0.3);
}

.search-cat-chip.active.cat-tv {
  background: #f43f5e;
  border-color: #f43f5e;
  color: #ffffff;
  box-shadow: 0 2px 10px rgba(244, 63, 94, 0.35);
}

.cat-chip-icon {
  flex-shrink: 0;
}

.cat-chip-badge {
  font-size: 0.7rem;
  padding: 0.1rem 0.35rem;
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.2);
  font-weight: 700;
  font-family: monospace;
}

.search-cat-chip.active.cat-tv .cat-chip-badge {
  background: rgba(255, 255, 255, 0.25);
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

/* Actor Spotlight Banner */
.actor-spotlight-card {
  display: flex;
  align-items: center;
  gap: 1.1rem;
  padding: 1rem 1.25rem;
  margin-bottom: 0.35rem;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.12) 0%, rgba(14, 14, 21, 0.92) 100%);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: var(--radius-lg);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45);
}

.actor-avatar-wrap {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.5);
}

.actor-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  object-position: top;
  border: 2px solid var(--accent-amber);
}

.actor-avatar-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--bg-surface-elevated);
  border: 2px solid var(--accent-amber);
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--accent-amber);
}

.actor-badge-star {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--accent-amber);
  color: #07070a;
  display: grid;
  place-items: center;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.6);
}

.actor-details {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}

.actor-tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--accent-amber);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.actor-name {
  font-size: 1.25rem;
  font-weight: 800;
  color: #ffffff;
  line-height: 1.2;
}

.actor-summary {
  font-size: 0.78rem;
  color: var(--text-secondary);
}

.actor-spotlight-close {
  margin-left: auto;
  align-self: flex-start;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: var(--text-muted);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.2s ease;
  flex-shrink: 0;
  cursor: pointer;
}

.actor-spotlight-close:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.25);
}

.actor-switch-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  margin-top: 0.45rem;
  padding: 0.3rem 0.75rem;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-size: 0.7rem;
  font-weight: 600;
  cursor: pointer;
  width: fit-content;
  transition: all 0.2s ease;
}

.actor-switch-pill:hover {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.3);
}

.actor-switch-pill.highlight {
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.4);
  color: var(--accent-amber);
}

.actor-switch-pill.highlight:hover {
  background: rgba(245, 158, 11, 0.25);
  border-color: var(--accent-amber);
}

.view-mode-toggle {
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  padding: 2px;
  gap: 2px;
}

.toggle-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.28rem 0.65rem;
  border-radius: calc(var(--radius-sm) - 2px);
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toggle-tab.active {
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
}

.toggle-tab:hover:not(.active) {
  color: var(--text-secondary);
}

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
  flex-wrap: wrap;
  gap: 0.5rem;
}

.results-meta-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.results-count {
  font-size: 0.72rem;
  color: var(--text-muted);
}

.results-breakdown {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.72rem;
}

.breakdown-tag {
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--border-subtle);
  font-weight: 500;
}

.breakdown-tag.movie {
  color: #f59e0b;
  border-color: rgba(245, 158, 11, 0.3);
}

.breakdown-tag.tv {
  color: #f43f5e;
  border-color: rgba(244, 63, 94, 0.3);
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
