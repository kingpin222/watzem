<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { tvSpatialNav } from '../services/tvSpatialNav';
import { MediaItem, MediaMode } from '../types';
import { useApi } from '../composables/useApi';
import { useHaptics } from '../composables/useHaptics';
import { useInfiniteScroll } from '../composables/useInfiniteScroll';
import MovieCard from '../components/MovieCard.vue';
import {
  Tv,
  MonitorPlay,
  Film,
  Radio,
  Layers,
  Zap,
  Compass,
  Palette,
  Laugh,
  ShieldAlert,
  Users,
  Wand2,
  Skull,
  Search,
  Heart,
  Rocket,
  Eye,
  Shield,
  Sparkles,
  Crosshair,
  Gauge,
  Crown,
  Target,
  Moon,
  Binary,
  Camera,
  Leaf,
  Castle,
  ArrowLeft,
  FolderOpen,
  ChevronDown,
  Loader2,
  Calendar,
  History,
  Clock
} from 'lucide-vue-next';

const props = defineProps<{
  mode: MediaMode;
}>();

const emit = defineEmits<{
  (e: 'select', item: MediaItem): void;
  (e: 'update:mode', mode: MediaMode): void;
}>();

const { getMediaList, getCollection } = useApi();
const { triggerLight } = useHaptics();

// Current active category title or null (showing browse menu)
const activeCategoryTitle = ref<string | null>(null);
const categoryItems = ref<MediaItem[]>([]);
const isLoadingCategory = ref(false);

// Category Pagination State
const categoryPage = ref(1);
const categoryTotalPages = ref(1);
const isLoadingMoreCategory = ref(false);
const activeCategoryFetcher = ref<((page: number) => Promise<{ results: MediaItem[]; total_pages?: number }>) | null>(null);

// Category Context & Year Filter State
interface CategoryContext {
  type: 'network' | 'genre' | 'collection' | 'studio' | 'year';
  rawItem: any;
  baseTitle: string;
}

const activeCategoryContext = ref<CategoryContext | null>(null);
const selectedFilterYear = ref<string>('all');
const rawCollectionParts = ref<MediaItem[]>([]);

// Networks / Streaming Providers with Lucide icons
const networks = [
  { id: 213, name: 'Netflix', icon: MonitorPlay, provider: 8, network: 213 },
  { id: 49, name: 'HBO / Max', icon: Film, provider: 1899, network: 49 },
  { id: 2739, name: 'Disney+', icon: Castle, provider: 337, network: 2739 },
  { id: 1024, name: 'Prime Video', icon: Tv, provider: 9, network: 1024 },
  { id: 2552, name: 'Apple TV+', icon: Radio, provider: 350, network: 2552 },
  { id: 453, name: 'Hulu', icon: Layers, provider: 15, network: 453 },
  { id: 4330, name: 'Paramount+', icon: Compass, provider: 531, network: 4330 },
  { id: 3353, name: 'Peacock', icon: Sparkles, provider: 386, network: 3353 }
];

// Popular Genres with Lucide icons
const genres = [
  { id: 28, tvId: 10759, name: 'Action', icon: Zap },
  { id: 12, tvId: 10759, name: 'Adventure', icon: Compass },
  { id: 16, tvId: 16, name: 'Animation', icon: Palette },
  { id: 35, tvId: 35, name: 'Comedy', icon: Laugh },
  { id: 80, tvId: 80, name: 'Crime', icon: ShieldAlert },
  { id: 99, tvId: 99, name: 'Documentary', icon: Users },
  { id: 18, tvId: 18, name: 'Drama', icon: Wand2 },
  { id: 10751, tvId: 10751, name: 'Family', icon: Heart },
  { id: 14, tvId: 10765, name: 'Fantasy', icon: Sparkles },
  { id: 36, tvId: 18, name: 'History', icon: Crown },
  { id: 27, tvId: 9648, name: 'Horror', icon: Skull },
  { id: 9648, tvId: 9648, name: 'Mystery', icon: Search },
  { id: 10749, tvId: 10749, name: 'Romance', icon: Heart },
  { id: 878, tvId: 10765, name: 'Sci-Fi', icon: Rocket },
  { id: 53, tvId: 9648, name: 'Thriller', icon: Eye },
  { id: 10752, tvId: 10768, name: 'War', icon: Shield },
  { id: 37, tvId: 37, name: 'Western', icon: Crosshair },
  { id: 10402, tvId: 10402, name: 'Music', icon: Gauge }
];

// Release Years & Eras
export interface ReleaseYearDef {
  id: string;
  name: string;
  badge?: string;
  year?: number;
  yearGte?: number;
  yearLte?: number;
  icon: any;
}

const releaseYears: ReleaseYearDef[] = [
  { id: '2026', name: '2026', badge: 'Upcoming', year: 2026, icon: Sparkles },
  { id: '2025', name: '2025', badge: 'Latest', year: 2025, icon: Calendar },
  { id: '2024', name: '2024', badge: 'Hits', year: 2024, icon: Calendar },
  { id: '2023', name: '2023', year: 2023, icon: Calendar },
  { id: '2022', name: '2022', year: 2022, icon: Calendar },
  { id: '2021', name: '2021', year: 2021, icon: Calendar },
  { id: '2020', name: '2020', year: 2020, icon: Calendar },
  { id: '2019', name: '2019', year: 2019, icon: Calendar },
  { id: '2018', name: '2018', year: 2018, icon: Calendar },
  { id: '2015', name: '2015', year: 2015, icon: Calendar },
  { id: '2010', name: '2010', year: 2010, icon: Calendar },
  { id: '2010s', name: '2010s', badge: 'Decade', yearGte: 2010, yearLte: 2019, icon: History },
  { id: '2000s', name: '2000s', badge: 'Decade', yearGte: 2000, yearLte: 2009, icon: History },
  { id: '1990s', name: '90s', badge: 'Retro', yearGte: 1990, yearLte: 1999, icon: History },
  { id: '1980s', name: '80s', badge: 'Retro', yearGte: 1980, yearLte: 1989, icon: History },
  { id: 'classics', name: 'Classics', badge: 'Pre-80s', yearLte: 1979, icon: Clock }
];

// Popular Movie Franchises
const collections = [
  { id: 86311, name: 'The Avengers', icon: Target },
  { id: 1241, name: 'Harry Potter', icon: Sparkles },
  { id: 10, name: 'Star Wars', icon: Rocket },
  { id: 9485, name: 'Fast & Furious', icon: Gauge },
  { id: 119, name: 'Lord of the Rings', icon: Crown },
  { id: 295, name: 'Pirates of the Caribbean', icon: Skull },
  { id: 529892, name: 'Spider-Man', icon: Zap },
  { id: 263, name: 'The Dark Knight', icon: Moon },
  { id: 87096, name: 'Avatar', icon: Binary },
  { id: 8091, name: 'Alien', icon: Rocket }
];

// Studios with Lucide icons
const studios = [
  { id: 420, name: 'Marvel Studios', icon: Zap },
  { id: 3, name: 'Pixar', icon: Sparkles },
  { id: 41077, name: 'A24', icon: Camera },
  { id: 10342, name: 'Studio Ghibli', icon: Leaf },
  { id: 174, name: 'Warner Bros', icon: Film },
  { id: 2, name: 'Walt Disney', icon: Castle }
];

// Discover query builder supporting categories and secondary year filter
const buildDiscoverParams = (page: number, yearId: string) => {
  const ctx = activeCategoryContext.value;
  if (!ctx) return { api: 'discover', mode: props.mode, page };

  const params: Record<string, string | number | undefined> = {
    api: 'discover',
    mode: props.mode,
    page
  };

  if (ctx.type === 'network') {
    if (props.mode === 'movie') {
      params.provider = ctx.rawItem.provider;
    } else {
      params.network = ctx.rawItem.network;
    }
  } else if (ctx.type === 'genre') {
    params.genres = props.mode === 'movie' ? ctx.rawItem.id : ctx.rawItem.tvId;
  } else if (ctx.type === 'studio') {
    params.mode = 'movie';
    params.company = ctx.rawItem.id;
  } else if (ctx.type === 'year') {
    const yDef = ctx.rawItem as ReleaseYearDef;
    if (yDef.year) params.year = yDef.year;
    if (yDef.yearGte) params.year_gte = yDef.yearGte;
    if (yDef.yearLte) params.year_lte = yDef.yearLte;
  }

  // If a secondary year filter is selected within a non-year category
  if (ctx.type !== 'year' && yearId && yearId !== 'all') {
    const yDef = releaseYears.find((y) => y.id === yearId);
    if (yDef) {
      if (yDef.year) params.year = yDef.year;
      if (yDef.yearGte) params.year_gte = yDef.yearGte;
      if (yDef.yearLte) params.year_lte = yDef.yearLte;
    }
  }

  return params;
};

// Generic loader for network, genre, studio, or year categories
const loadCategoryData = async () => {
  if (!activeCategoryContext.value) return;
  const ctx = activeCategoryContext.value;

  if (ctx.type === 'year') {
    activeCategoryTitle.value = `${ctx.rawItem.name} ${props.mode === 'movie' ? 'Movies' : 'TV Shows'}`;
  } else if (selectedFilterYear.value !== 'all') {
    const yDef = releaseYears.find((y) => y.id === selectedFilterYear.value);
    activeCategoryTitle.value = `${ctx.baseTitle} (${yDef?.name || selectedFilterYear.value})`;
  } else {
    activeCategoryTitle.value = ctx.baseTitle;
  }

  isLoadingCategory.value = true;
  categoryPage.value = 1;
  categoryTotalPages.value = 1;

  const fetcher = async (p: number) => {
    const params = buildDiscoverParams(p, selectedFilterYear.value);
    return await getMediaList(params);
  };
  activeCategoryFetcher.value = fetcher;

  try {
    const res = await fetcher(1);
    categoryItems.value = res.results || [];
    categoryTotalPages.value = res.total_pages || 1;
  } catch (err) {
    console.error('Error fetching category media', err);
    categoryItems.value = [];
  } finally {
    isLoadingCategory.value = false;
  }
};

const openNetwork = async (net: (typeof networks)[0]) => {
  triggerLight();
  activeCategoryContext.value = { type: 'network', rawItem: net, baseTitle: net.name };
  selectedFilterYear.value = 'all';
  await loadCategoryData();
};

const openGenre = async (g: (typeof genres)[0]) => {
  triggerLight();
  activeCategoryContext.value = { type: 'genre', rawItem: g, baseTitle: g.name };
  selectedFilterYear.value = 'all';
  await loadCategoryData();
};

const openStudio = async (st: (typeof studios)[0]) => {
  triggerLight();
  activeCategoryContext.value = { type: 'studio', rawItem: st, baseTitle: st.name };
  selectedFilterYear.value = 'all';
  await loadCategoryData();
};

const openYear = async (y: ReleaseYearDef) => {
  triggerLight();
  activeCategoryContext.value = {
    type: 'year',
    rawItem: y,
    baseTitle: `${y.name} ${props.mode === 'movie' ? 'Movies' : 'TV Shows'}`
  };
  selectedFilterYear.value = y.id;
  await loadCategoryData();
};

const openCollection = async (col: (typeof collections)[0]) => {
  triggerLight();
  activeCategoryContext.value = { type: 'collection', rawItem: col, baseTitle: col.name };
  selectedFilterYear.value = 'all';
  activeCategoryTitle.value = col.name;
  isLoadingCategory.value = true;
  categoryPage.value = 1;
  categoryTotalPages.value = 1;
  activeCategoryFetcher.value = null; // Collection is a fixed finite set of films
  try {
    const res = await getCollection(col.id);
    rawCollectionParts.value = (res.parts || []).slice().sort((a, b) => (a.release_date || '').localeCompare(b.release_date || ''));
    categoryItems.value = rawCollectionParts.value;
  } catch (err) {
    console.error('Error fetching collection', err);
    categoryItems.value = [];
    rawCollectionParts.value = [];
  } finally {
    isLoadingCategory.value = false;
  }
};

// Year filtering inside Category View
const selectFilterYear = async (yearId: string) => {
  if (selectedFilterYear.value === yearId) return;
  triggerLight();
  selectedFilterYear.value = yearId;

  if (activeCategoryContext.value?.type === 'collection') {
    if (yearId === 'all') {
      categoryItems.value = rawCollectionParts.value;
    } else {
      const yDef = releaseYears.find((y) => y.id === yearId);
      categoryItems.value = rawCollectionParts.value.filter((item) => {
        const yr = parseInt((item.release_date || '').substring(0, 4), 10);
        if (isNaN(yr)) return false;
        if (yDef?.year) return yr === yDef.year;
        if (yDef?.yearGte && yDef?.yearLte) return yr >= yDef.yearGte && yr <= yDef.yearLte;
        if (yDef?.yearLte) return yr <= yDef.yearLte;
        return false;
      });
    }
    return;
  }

  if (activeCategoryContext.value?.type === 'year') {
    const yDef = releaseYears.find((y) => y.id === yearId);
    if (yDef) {
      activeCategoryContext.value.rawItem = yDef;
      activeCategoryContext.value.baseTitle = `${yDef.name} ${props.mode === 'movie' ? 'Movies' : 'TV Shows'}`;
    }
  }

  await loadCategoryData();
};

const loadMoreCategory = async () => {
  if (
    !activeCategoryFetcher.value ||
    isLoadingMoreCategory.value ||
    categoryPage.value >= categoryTotalPages.value
  ) {
    return;
  }
  triggerLight();

  const activeEl = document.activeElement as HTMLElement | null;
  const wasLoadMore = activeEl?.classList.contains('load-more-btn');
  const focusedCardId = activeEl?.getAttribute('data-card-id');
  const prevCount = categoryItems.value.length;

  isLoadingMoreCategory.value = true;
  try {
    const nextPage = categoryPage.value + 1;
    const res = await activeCategoryFetcher.value(nextPage);
    const newItems = res.results || [];
    if (newItems.length > 0) {
      const existingIds = new Set(categoryItems.value.map((x) => x.id));
      const uniqueResults = newItems.filter((x) => !existingIds.has(x.id));
      if (uniqueResults.length > 0) {
        categoryItems.value.push(...uniqueResults);
      }
      categoryPage.value = nextPage;
      categoryTotalPages.value = res.total_pages || categoryTotalPages.value;
    }
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
  } catch (err) {
    console.error('Error loading more category items:', err);
  } finally {
    isLoadingMoreCategory.value = false;
  }
};

const { sentinelRef: exploreSentinelRef } = useInfiniteScroll(
  loadMoreCategory,
  () => Boolean(
    activeCategoryFetcher.value &&
    !isLoadingCategory.value &&
    !isLoadingMoreCategory.value &&
    categoryPage.value < categoryTotalPages.value
  ),
  { distance: 400 }
);

const goBackToBrowse = () => {
  triggerLight();
  activeCategoryTitle.value = null;
  activeCategoryContext.value = null;
  selectedFilterYear.value = 'all';
  categoryItems.value = [];
  rawCollectionParts.value = [];
  categoryPage.value = 1;
  categoryTotalPages.value = 1;
  activeCategoryFetcher.value = null;
};

// Reactively reload category if media mode (movie/tv) changes
watch(() => props.mode, () => {
  if (activeCategoryContext.value) {
    if (activeCategoryContext.value.type === 'collection' || activeCategoryContext.value.type === 'studio') {
      goBackToBrowse();
    } else {
      if (activeCategoryContext.value.type === 'year') {
        activeCategoryContext.value.baseTitle = `${activeCategoryContext.value.rawItem.name} ${props.mode === 'movie' ? 'Movies' : 'TV Shows'}`;
      }
      loadCategoryData();
    }
  }
});

onMounted(() => {
  const unregister = tvSpatialNav.registerBackHandler(() => {
    if (activeCategoryTitle.value) {
      goBackToBrowse();
      return true;
    }
    return false;
  });

  onUnmounted(() => {
    unregister();
  });
});
</script>

<template>
  <div class="explore-view">
    <!-- Active Category Selected (Filtered Grid View) -->
    <div v-if="activeCategoryTitle" class="category-detail-view">
      <div class="cat-header">
        <button class="btn-back" @click="goBackToBrowse">
          <ArrowLeft :size="16" />
          <span>Categories</span>
        </button>
        <h2 class="cat-title font-display">{{ activeCategoryTitle }}</h2>
        <span class="count-tag font-mono">{{ categoryItems.length }} titles</span>
      </div>

      <!-- Category Year Filter Strip -->
      <div class="cat-year-filter-strip">
        <button
          class="cat-year-chip"
          :class="{ active: selectedFilterYear === 'all', 'tv-theme': mode === 'tv' }"
          @click="selectFilterYear('all')"
        >
          <span>All Years</span>
        </button>
        <button
          v-for="y in releaseYears"
          :key="y.id"
          class="cat-year-chip"
          :class="{ active: selectedFilterYear === y.id, 'tv-theme': mode === 'tv' }"
          @click="selectFilterYear(y.id)"
        >
          <span>{{ y.name }}</span>
          <span v-if="y.badge" class="chip-badge">{{ y.badge }}</span>
        </button>
      </div>

      <!-- Loading skeleton -->
      <div v-if="isLoadingCategory" class="movies-grid">
        <div v-for="n in 6" :key="n" class="skeleton card-skeleton"></div>
      </div>

      <!-- Category movies grid -->
      <template v-else-if="categoryItems.length > 0">
        <div class="movies-grid">
          <MovieCard
            v-for="item in categoryItems"
            :key="item.id"
            :item="item"
            :mode="mode"
            @select="emit('select', $event)"
          />
        </div>

        <!-- Infinite Scroll Sentinel & Loading Indicator (Button stays permanently mounted) -->
        <div
          v-if="categoryPage < categoryTotalPages"
          ref="exploreSentinelRef"
          class="load-more-wrap"
        >
          <button
            class="load-more-btn"
            :class="{ 'tv-theme': mode === 'tv', 'is-loading': isLoadingMoreCategory }"
            :aria-busy="isLoadingMoreCategory"
            @click="loadMoreCategory"
          >
            <div v-if="isLoadingMoreCategory" class="loading-indicator-inline">
              <Loader2 :size="18" class="spin" />
              <span>Loading more titles...</span>
            </div>
            <div v-else class="load-more-content">
              <span>Load More</span>
              <ChevronDown :size="16" />
            </div>
          </button>
        </div>
      </template>

      <div v-else class="empty-state">
        <div class="empty-icon">
          <FolderOpen :size="36" stroke-width="1.5" />
        </div>
        <p>No titles found for this category and year.</p>
      </div>
    </div>

    <!-- Main Browse Categories Grid -->
    <div v-else class="browse-sections">
      <!-- Mode Switcher Pill Bar (Movies vs TV Series) -->
      <div class="explore-mode-bar">
        <button
          type="button"
          class="explore-mode-chip"
          :class="{ active: mode === 'movie' }"
          :aria-pressed="mode === 'movie'"
          @click="emit('update:mode', 'movie')"
        >
          <Film :size="15" />
          <span>Movies</span>
        </button>
        <button
          type="button"
          class="explore-mode-chip tv-theme"
          :class="{ active: mode === 'tv' }"
          :aria-pressed="mode === 'tv'"
          @click="emit('update:mode', 'tv')"
        >
          <Tv :size="15" />
          <span>TV Series</span>
        </button>
      </div>

      <!-- Quick Year Filter Pill Strip -->
      <section class="explore-section quick-years-section">
        <div class="section-header-row">
          <div class="title-with-icon">
            <Calendar :size="17" class="title-icon" :class="{ 'tv-theme': mode === 'tv' }" />
            <h3 class="section-title font-display">Browse by Year</h3>
          </div>
          <span class="section-subtitle">Jump to release era</span>
        </div>
        <div class="quick-years-scroll">
          <button
            v-for="y in releaseYears"
            :key="y.id"
            class="quick-year-pill"
            :class="{ 'tv-theme': mode === 'tv' }"
            @click="openYear(y)"
          >
            <component :is="y.icon" :size="13" class="pill-icon" />
            <span class="pill-year">{{ y.name }}</span>
            <span v-if="y.badge" class="pill-badge">{{ y.badge }}</span>
          </button>
        </div>
      </section>

      <!-- Streaming Networks -->
      <section class="explore-section">
        <div class="section-header-row">
          <div class="title-with-icon">
            <MonitorPlay :size="17" class="title-icon" :class="{ 'tv-theme': mode === 'tv' }" />
            <h3 class="section-title font-display">Streaming & Networks</h3>
          </div>
        </div>
        <div class="cards-grid">
          <button
            v-for="net in networks"
            :key="net.id"
            class="explore-btn"
            @click="openNetwork(net)"
          >
            <div class="explore-icon-wrap">
              <component :is="net.icon" :size="18" />
            </div>
            <span class="explore-name">{{ net.name }}</span>
          </button>
        </div>
      </section>

      <!-- Genres -->
      <section class="explore-section">
        <div class="section-header-row">
          <div class="title-with-icon">
            <Zap :size="17" class="title-icon" :class="{ 'tv-theme': mode === 'tv' }" />
            <h3 class="section-title font-display">Popular Genres</h3>
          </div>
        </div>
        <div class="cards-grid">
          <button
            v-for="g in genres"
            :key="g.id"
            class="explore-btn"
            @click="openGenre(g)"
          >
            <div class="explore-icon-wrap">
              <component :is="g.icon" :size="18" />
            </div>
            <span class="explore-name">{{ g.name }}</span>
          </button>
        </div>
      </section>

      <!-- Release Years & Eras Grid -->
      <section class="explore-section">
        <div class="section-header-row">
          <div class="title-with-icon">
            <History :size="17" class="title-icon" :class="{ 'tv-theme': mode === 'tv' }" />
            <h3 class="section-title font-display">Release Years & Eras</h3>
          </div>
          <span class="count-tag font-mono">{{ releaseYears.length }} eras</span>
        </div>
        <div class="cards-grid">
          <button
            v-for="y in releaseYears"
            :key="y.id"
            class="explore-btn year-btn"
            :class="{ 'tv-theme': mode === 'tv' }"
            @click="openYear(y)"
          >
            <div class="explore-icon-wrap year-icon-wrap" :class="{ 'tv-theme': mode === 'tv' }">
              <component :is="y.icon" :size="18" />
            </div>
            <div class="year-name-wrap">
              <span class="explore-name">{{ y.name }}</span>
              <span v-if="y.badge" class="year-badge font-mono">{{ y.badge }}</span>
            </div>
          </button>
        </div>
      </section>

      <!-- Movie Franchises & Collections -->
      <section v-if="mode === 'movie'" class="explore-section">
        <div class="section-header-row">
          <div class="title-with-icon">
            <Target :size="17" class="title-icon" />
            <h3 class="section-title font-display">Movie Franchises</h3>
          </div>
        </div>
        <div class="cards-grid">
          <button
            v-for="col in collections"
            :key="col.id"
            class="explore-btn"
            @click="openCollection(col)"
          >
            <div class="explore-icon-wrap">
              <component :is="col.icon" :size="18" />
            </div>
            <span class="explore-name">{{ col.name }}</span>
          </button>
        </div>
      </section>

      <!-- Studios -->
      <section v-if="mode === 'movie'" class="explore-section">
        <div class="section-header-row">
          <div class="title-with-icon">
            <Film :size="17" class="title-icon" />
            <h3 class="section-title font-display">Film Studios</h3>
          </div>
        </div>
        <div class="cards-grid">
          <button
            v-for="st in studios"
            :key="st.id"
            class="explore-btn"
            @click="openStudio(st)"
          >
            <div class="explore-icon-wrap">
              <component :is="st.icon" :size="18" />
            </div>
            <span class="explore-name">{{ st.name }}</span>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* Explore View - 100% Fluid */
.explore-view {
  width: 100%;
  max-width: 100%;
  padding: 1.25rem max(clamp(1rem, 2.5vw, 2.5rem), var(--safe-left, 0px)) 3rem;
  padding-right: max(clamp(1rem, 2.5vw, 2.5rem), var(--safe-right, 0px));
  display: flex;
  flex-direction: column;
}

@media (min-width: 768px) {
  .explore-view {
    padding: 1.75rem max(clamp(1.5rem, 3vw, 3rem), var(--safe-left, 0px)) 4rem;
    padding-right: max(clamp(1.5rem, 3vw, 3rem), var(--safe-right, 0px));
  }
}

.browse-sections {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.explore-mode-bar {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.15rem 0;
}

.explore-mode-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.45rem 1.15rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  user-select: none;
}

.explore-mode-chip:hover {
  color: var(--text-primary);
  border-color: rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.05);
}

.explore-mode-chip.active {
  color: #0b0f19;
  background: #f59e0b;
  border-color: #f59e0b;
  box-shadow: 0 2px 10px rgba(245, 158, 11, 0.3);
}

.explore-mode-chip.tv-theme.active {
  background: #06b6d4;
  border-color: #06b6d4;
  color: #000000;
  box-shadow: 0 2px 10px rgba(6, 182, 212, 0.35);
}

.explore-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.section-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.title-icon {
  color: var(--accent-amber);
  transition: color 0.2s ease;
}

.title-icon.tv-theme {
  color: var(--accent-rose);
}

.section-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-primary);
}

.section-subtitle {
  font-size: 0.72rem;
  color: var(--text-muted);
  font-weight: 500;
}

/* Quick Years Pill Carousel */
.quick-years-section {
  gap: 0.65rem;
}

.quick-years-scroll {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  overflow-x: auto;
  padding: 2px 0 6px;
  margin: 0 -0.25rem;
  padding-left: 0.25rem;
  padding-right: 0.25rem;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}

.quick-years-scroll::-webkit-scrollbar {
  display: none;
}

.quick-year-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.42rem 0.85rem;
  border-radius: var(--radius-full);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  font-size: 0.76rem;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  flex-shrink: 0;
}

.quick-year-pill:hover {
  background: var(--bg-card-hover);
  border-color: var(--accent-amber);
  transform: translateY(-1px);
}

.quick-year-pill.tv-theme:hover {
  border-color: var(--accent-rose);
}

.pill-icon {
  color: var(--accent-amber);
  transition: color 0.2s ease;
}

.quick-year-pill.tv-theme .pill-icon {
  color: var(--accent-rose);
}

.pill-badge {
  font-size: 0.6rem;
  font-family: var(--font-mono, monospace);
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(245, 158, 11, 0.15);
  color: var(--accent-amber);
  font-weight: 700;
}

.quick-year-pill.tv-theme .pill-badge {
  background: rgba(244, 63, 94, 0.15);
  color: var(--accent-rose);
}

/* Fluid Auto-Filling Cards Grid */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: clamp(0.65rem, 1.2vw, 1rem);
}

.explore-btn {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.75rem 1rem;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  text-align: left;
  transition: all 0.2s ease;
  cursor: pointer;
}

.explore-btn:hover {
  border-color: var(--accent-amber);
  background: var(--bg-card-hover);
}

.explore-btn.tv-theme:hover {
  border-color: var(--accent-rose);
}

.explore-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--bg-surface-elevated);
  border: 1px solid var(--border-subtle);
  color: var(--accent-amber);
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.explore-icon-wrap.tv-theme {
  color: var(--accent-rose);
}

.explore-name {
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Release Year Cards in Grid */
.year-name-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-width: 0;
  gap: 0.4rem;
}

.year-badge {
  font-size: 0.62rem;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.06);
  color: var(--text-muted);
  font-weight: 600;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.year-btn:hover .year-badge {
  background: rgba(245, 158, 11, 0.18);
  color: var(--accent-amber);
}

.year-btn.tv-theme:hover .year-badge {
  background: rgba(244, 63, 94, 0.18);
  color: var(--accent-rose);
}

/* Category detail view */
.category-detail-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.cat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--border-subtle);
  gap: 0.75rem;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
  padding: 0.4rem 0.65rem;
  border-radius: 6px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.btn-back:hover {
  color: var(--text-primary);
  border-color: var(--border-active);
}

.cat-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.count-tag {
  font-size: 0.65rem;
  color: var(--text-muted);
  background: var(--bg-card);
  padding: 2px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}

/* Category Year Filter Strip */
.cat-year-filter-strip {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  overflow-x: auto;
  padding-bottom: 0.5rem;
  margin-bottom: 0.25rem;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}

.cat-year-filter-strip::-webkit-scrollbar {
  display: none;
}

.cat-year-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.38rem 0.75rem;
  border-radius: var(--radius-full);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-size: 0.74rem;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.18s ease;
  flex-shrink: 0;
}

.cat-year-chip:hover {
  color: var(--text-primary);
  border-color: var(--border-active);
}

.cat-year-chip.active {
  background: rgba(245, 158, 11, 0.18);
  border-color: rgba(245, 158, 11, 0.6);
  color: #fbbf24;
  box-shadow: 0 0 12px rgba(245, 158, 11, 0.2);
}

.cat-year-chip.active.tv-theme {
  background: rgba(244, 63, 94, 0.18);
  border-color: rgba(244, 63, 94, 0.6);
  color: #fb7185;
  box-shadow: 0 0 12px rgba(244, 63, 94, 0.2);
}

.chip-badge {
  font-size: 0.58rem;
  font-family: var(--font-mono, monospace);
  padding: 1px 4px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.08);
  color: inherit;
}

/* Movies Grid - Fluid Auto-Scaling */
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
  .explore-view {
    padding: 1rem max(clamp(1.25rem, 2.5vw, 2.5rem), var(--safe-left, 0px)) 3rem;
    padding-right: max(clamp(1.25rem, 2.5vw, 2.5rem), var(--safe-right, 0px));
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

.card-skeleton {
  aspect-ratio: 2 / 3;
  border-radius: var(--radius-md);
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: var(--text-muted);
}

.empty-icon {
  margin-bottom: 0.5rem;
  opacity: 0.5;
  display: flex;
  justify-content: center;
}

/* Load More in Categories */
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
  cursor: pointer;
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
