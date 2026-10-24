<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { Capacitor } from '@capacitor/core';
import { App as CapApp } from '@capacitor/app';
import { SplashScreen as CapSplashScreen } from '@capacitor/splash-screen';
import { MediaItem, MediaMode, TabKey, PersonSearchResult } from './types';
import TopHeader from './components/TopHeader.vue';
import BottomNav from './components/BottomNav.vue';
import MovieDetailView from './views/MovieDetailView.vue';
import UpdateView from './views/UpdateView.vue';
import TipsView from './views/TipsView.vue';
import ToastContainer from './components/ToastContainer.vue';
import SplashScreen from './components/SplashScreen.vue';
import MovieView from './views/MovieView.vue';
import TvView from './views/TvView.vue';
import ExploreView from './views/ExploreView.vue';
import SearchView from './views/SearchView.vue';
import WatchlistView from './views/WatchlistView.vue';
import ApkDownloadView from './views/ApkDownloadView.vue';
import { useAppUpdate } from './composables/useAppUpdate';
import { useHeroBackground } from './composables/useHeroBackground';
import { useTvMode } from './composables/useTvMode';
import { useMonetization } from './composables/useMonetization';
import { tvSpatialNav } from './services/tvSpatialNav';
import { watch } from 'vue';
import TvSidebar from './components/tv/TvSidebar.vue';

// App global state
const showSplash = ref(true);
const currentMode = ref<MediaMode>('movie');
const currentTab = ref<TabKey>('home');
const previousTab = ref<TabKey>('home');
const selectedItem = ref<MediaItem | null>(null);
const initialPlayState = ref(false);
const { checkForUpdate, updateInfo, showUpdateModal } = useAppUpdate();
const { activeHeroBackdrop, isHeroActive } = useHeroBackground();
const { isTvMode } = useTvMode();

// Reactive Landscape / Widescreen Detection
const isLandscape = ref(false);

const checkIsLandscape = (): boolean => {
  if (typeof window === 'undefined') return false;
  if (window.screen?.orientation?.type) {
    return window.screen.orientation.type.startsWith('landscape');
  }
  return window.matchMedia('(orientation: landscape)').matches;
};

const updateOrientation = () => {
  const land = checkIsLandscape();
  isLandscape.value = land;
  if (!land && typeof document !== 'undefined') {
    document.documentElement.style.setProperty('--safe-left', '0px');
    document.documentElement.style.setProperty('--safe-right', '0px');
  }
};

let mql: MediaQueryList | null = null;
let mqlListener: ((e: MediaQueryListEvent) => void) | null = null;
let screenOrientationListener: (() => void) | null = null;

const showSidebar = computed(() => {
  return (isTvMode.value || isLandscape.value) && 
    currentTab.value !== 'download' && 
    currentTab.value !== 'details' &&
    currentTab.value !== 'update' &&
    currentTab.value !== 'tips';
});

// Android Web Detection & Redirection
const isNative = Capacitor.isNativePlatform();
const isAndroid = typeof navigator !== 'undefined' && /android/i.test(navigator.userAgent || '');
const urlParams = typeof window !== 'undefined' ? new URLSearchParams(window.location.search) : null;
const testAndroid = urlParams ? (urlParams.get('test_android') === '1' || urlParams.get('android') === '1') : false;
const isAndroidWeb = (isAndroid || testAndroid) && !isNative;
const pathname = typeof window !== 'undefined' ? window.location.pathname.replace(/\/$/, '') : '';

// 1. Direct navigation routes
if (pathname === '/download') {
  currentTab.value = 'download';
  showSplash.value = false;
} else if (pathname === '/update') {
  currentTab.value = 'update';
  showSplash.value = false;
} else if (pathname === '/tips' || pathname === '/support') {
  currentTab.value = 'tips';
  showSplash.value = false;
}
// 2. If Android user detected on the web (and NOT already using the native APK): STRICT LOCK
else if (isAndroidWeb) {
  currentTab.value = 'download';
  showSplash.value = false;
  if (typeof window !== 'undefined' && window.location.pathname !== '/download') {
    window.history.replaceState(null, '', '/download');
  }
}

// Watch modal trigger states and seamlessly navigate to full pages instead of popups
const { isSupportModalOpen } = useMonetization();
watch(showUpdateModal, (isOpen) => {
  if (isOpen) {
    handleNavigate('update');
  }
});
watch(isSupportModalOpen, (isOpen) => {
  if (isOpen) {
    handleNavigate('tips');
    isSupportModalOpen.value = false;
  }
});

const savedScrollPositions = ref<Record<string, number>>({});

const scrollToTop = () => {
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

const openDetail = (item: MediaItem, autoPlay = false) => {
  if (currentTab.value !== 'details') {
    previousTab.value = currentTab.value;
    savedScrollPositions.value[currentTab.value] =
      window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0;
  }
  selectedItem.value = item;
  initialPlayState.value = autoPlay;
  currentTab.value = 'details';
  scrollToTop();
  requestAnimationFrame(scrollToTop);
};

const closeDetail = () => {
  const targetTab = previousTab.value || 'home';
  const targetScroll = savedScrollPositions.value[targetTab] || 0;
  currentTab.value = targetTab;
  selectedItem.value = null;
  initialPlayState.value = false;
  setTimeout(() => {
    window.scrollTo({ top: targetScroll, left: 0, behavior: 'instant' });
    document.documentElement.scrollTop = targetScroll;
    document.body.scrollTop = targetScroll;
  }, 20);
};

const keywordSearchQuery = ref('');
const initialSearchActor = ref<PersonSearchResult | null>(null);

const handleSearchFromDetail = (keyword: string) => {
  if (currentTab.value === 'details') {
    previousTab.value = 'home';
  }
  selectedItem.value = null;
  initialPlayState.value = false;
  initialSearchActor.value = null;
  keywordSearchQuery.value = keyword;
  currentTab.value = 'search';
  scrollToTop();
  requestAnimationFrame(scrollToTop);
};

const handleActorSearchFromDetail = (actor: { id: number; name: string; profile_path: string | null }) => {
  if (currentTab.value === 'details') {
    previousTab.value = 'home';
  }
  selectedItem.value = null;
  initialPlayState.value = false;
  initialSearchActor.value = {
    id: actor.id,
    name: actor.name,
    profile_path: actor.profile_path,
    known_for_department: 'Acting',
    popularity: 10
  };
  keywordSearchQuery.value = actor.name;
  currentTab.value = 'search';
  scrollToTop();
  requestAnimationFrame(scrollToTop);
};

const handleAfterEnter = () => {
  if (currentTab.value === 'details') {
    scrollToTop();
    requestAnimationFrame(scrollToTop);
  }
};

// Accurately determine if the selected item is a TV show or movie
const selectedItemMode = computed<'movie' | 'tv'>(() => {
  if (!selectedItem.value) return currentMode.value;
  if (selectedItem.value.media_type === 'tv') return 'tv';
  if (selectedItem.value.media_type === 'movie') return 'movie';
  if (selectedItem.value.first_air_date && !selectedItem.value.release_date) return 'tv';
  if (selectedItem.value.name && !selectedItem.value.title) return 'tv';
  return currentMode.value;
});

const activeComponent = computed(() => {
  switch (currentTab.value) {
    case 'home':
      return currentMode.value === 'tv' ? TvView : MovieView;
    case 'explore':
      return ExploreView;
    case 'search':
      return SearchView;
    case 'watchlist':
      return WatchlistView;
    case 'download':
      return ApkDownloadView;
    case 'details':
      return MovieDetailView;
    case 'update':
      return UpdateView;
    case 'tips':
      return TipsView;
    default:
      return currentMode.value === 'tv' ? TvView : MovieView;
  }
});

const handleBackFromSubpage = () => {
  if (currentTab.value === 'details') {
    closeDetail();
  } else {
    const target = previousTab.value && previousTab.value !== 'update' && previousTab.value !== 'tips'
      ? previousTab.value
      : 'home';
    handleNavigate(target);
  }
};

const handleNavigate = (tab: TabKey) => {
  if (isAndroidWeb) {
    currentTab.value = 'download';
    return;
  }
  if (currentTab.value === 'details') {
    previousTab.value = tab;
    selectedItem.value = null;
    initialPlayState.value = false;
  } else {
    savedScrollPositions.value[currentTab.value] =
      window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0;
  }
  if (tab !== 'search') {
    keywordSearchQuery.value = '';
    initialSearchActor.value = null;
  }
  currentTab.value = tab;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const handleModeChange = (newMode: MediaMode) => {
  if (currentMode.value !== newMode) {
    currentMode.value = newMode;
    savedScrollPositions.value = {};
    scrollToTop();
    requestAnimationFrame(scrollToTop);
  }
};

// Capacitor Native Android Hardware Back Button & Splash Screen Support
onMounted(() => {
  // Hide native splash screen if running in Capacitor
  try {
    CapSplashScreen.hide();
  } catch (err) {
    // Ignore in web browser
  }

  // Handle native Android hardware back button
  try {
    CapApp.addListener('backButton', () => {
      // If a mandatory update is active, back button exits the app rather than bypassing the update
      if (updateInfo.value?.mandatory && currentTab.value === 'update') {
        CapApp.exitApp();
        return;
      }
      if (currentTab.value === 'details' || currentTab.value === 'update' || currentTab.value === 'tips') {
        handleBackFromSubpage();
      } else if (currentTab.value !== 'home') {
        currentTab.value = 'home';
      } else {
        CapApp.exitApp();
      }
    });
  } catch (err) {
    // Ignore in web browser
  }

  // Register D-pad back navigation for Android TV remotes
  tvSpatialNav.registerBackHandler(() => {
    if (currentTab.value === 'details' || currentTab.value === 'update' || currentTab.value === 'tips') {
      handleBackFromSubpage();
      return true;
    }
    if (currentTab.value !== 'home') {
      handleNavigate('home');
      return true;
    }
    return false;
  });

  // Setup orientation change tracking for fluid landscape sidebar layout
  updateOrientation();
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', updateOrientation, { passive: true });

    const onOrientationChange = () => {
      updateOrientation();
      requestAnimationFrame(updateOrientation);
      setTimeout(updateOrientation, 100);
    };
    window.addEventListener('orientationchange', onOrientationChange, { passive: true });

    if (window.matchMedia) {
      mql = window.matchMedia('(orientation: landscape)');
      mqlListener = (e: MediaQueryListEvent) => {
        isLandscape.value = e.matches;
      };
      if (mql.addEventListener) {
        mql.addEventListener('change', mqlListener);
      } else if ((mql as any).addListener) {
        (mql as any).addListener(mqlListener);
      }
    }

    if (window.screen?.orientation) {
      screenOrientationListener = () => {
        updateOrientation();
      };
      window.screen.orientation.addEventListener('change', screenOrientationListener);
    }
  }

  // Check for in-app version update if running on native platform or test query
  try {
    if (isNative || (urlParams && urlParams.get('test_update') === '1')) {
      checkForUpdate();
    }
  } catch (err) {
    console.warn('Auto update check failed:', err);
  }
});

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', updateOrientation);
    window.removeEventListener('orientationchange', updateOrientation);
    if (mql && mqlListener) {
      if (mql.removeEventListener) {
        mql.removeEventListener('change', mqlListener);
      } else if ((mql as any).removeListener) {
        (mql as any).removeListener(mqlListener);
      }
    }
    if (window.screen?.orientation && screenOrientationListener) {
      window.screen.orientation.removeEventListener('change', screenOrientationListener);
    }
  }
});
</script>

<template>
  <div 
    class="app-container" 
    :class="{ 
      'is-download-mode': currentTab === 'download', 
      'is-details-mode': currentTab === 'details',
      'has-sidebar': showSidebar 
    }"
  >
    <!-- Animated Splash Screen with Movie Tiles Matrix (Only for standard web/app navigation) -->
    <Transition name="splash-fade">
      <SplashScreen
        v-if="showSplash && currentTab !== 'download'"
        @finish="showSplash = false"
      />
    </Transition>

    <!-- Ambient ambient glow effect -->
    <div class="bg-ambient-glow"></div>

    <!-- Active Hero Expanded Body Background -->
    <div
      class="body-hero-backdrop-wrap"
      :class="{ visible: isHeroActive && currentTab === 'home' }"
    >
      <Transition name="hero-body-fade">
        <div
          v-if="isHeroActive && currentTab === 'home' && activeHeroBackdrop"
          :key="activeHeroBackdrop"
          class="body-hero-backdrop-img"
          :style="{ backgroundImage: `url(${activeHeroBackdrop})` }"
        ></div>
      </Transition>
      <div class="body-hero-gradient-overlay"></div>
      <div
        class="body-hero-ambient-tint"
        :class="{ 'tv-tint': currentMode === 'tv' }"
      ></div>
    </div>

    <!-- Global Toast Notifications -->
    <ToastContainer />

    <!-- Left Sidebar Navigation (Active on Android TV and all Landscape screen sizes) -->
    <TvSidebar
      v-if="showSidebar"
      :current-tab="currentTab"
      :mode="currentMode"
      @navigate="handleNavigate"
      @update:mode="handleModeChange"
      @open-support="handleNavigate('tips')"
    />

    <!-- Header (Hidden when Sidebar is active, on APK download page, and on Fullscreen pages) -->
    <TopHeader
      v-if="!showSidebar && currentTab !== 'download' && currentTab !== 'details' && currentTab !== 'update' && currentTab !== 'tips'"
      :mode="currentMode"
      :current-tab="currentTab"
      @update:mode="handleModeChange"
      @navigate="handleNavigate"
    />

    <!-- Main View Content Area -->
    <main
      class="main-content"
      :class="{
        'download-page-main': currentTab === 'download',
        'details-page-main': currentTab === 'details' || currentTab === 'update' || currentTab === 'tips'
      }"
    >
      <Transition name="view-fade" mode="out-in" @after-enter="handleAfterEnter">
        <KeepAlive :max="4" :exclude="['MovieDetailView', 'MovieView', 'TvView', 'UpdateView', 'TipsView']">
          <component
            :is="activeComponent"
            :key="currentTab === 'details' ? (selectedItem?.id || 'details') : currentTab === 'home' ? `home-${currentMode}` : currentTab"
            :mode="currentTab === 'details' ? selectedItemMode : currentMode"
            :item="selectedItem"
            :initial-play="initialPlayState"
            :initial-query="currentTab === 'search' ? keywordSearchQuery : ''"
            :initial-actor="currentTab === 'search' ? initialSearchActor : null"
            @select="openDetail($event, false)"
            @play="openDetail($event, false)"
            @explore="handleNavigate('home')"
            @update:mode="currentMode = $event"
            @back="handleBackFromSubpage"
            @close="handleBackFromSubpage"
            @search="handleSearchFromDetail"
            @search-actor="handleActorSearchFromDetail"
          />
        </KeepAlive>
      </Transition>
    </main>

    <!-- Mobile Bottom Navigation Bar (Hidden when Sidebar is active, on APK download page, or in Details/Update/Tips view) -->
    <BottomNav
      v-if="!showSidebar && currentTab !== 'download' && currentTab !== 'details' && currentTab !== 'update' && currentTab !== 'tips'"
      :current-tab="currentTab"
      :mode="currentMode"
      @navigate="handleNavigate"
    />
  </div>
</template>

<style scoped>
/* App Container & Sidebar Push Offset - 100% Fluid Layout */
.app-container {
  width: 100%;
  max-width: 100%;
  min-height: 100vh;
  position: relative;
}

/* Sidebar push offset & adjustments: ONLY active in landscape or TV mode */
@media (orientation: landscape) {
  .app-container.has-sidebar {
    padding-left: calc(78px + var(--safe-left, 0px));
  }

  .app-container.has-sidebar.is-details-mode {
    padding-left: 0 !important;
  }

  .has-sidebar .main-content {
    padding-bottom: 2rem !important;
  }

  /* Symmetrical padding for all views inside sidebar layout */
  .has-sidebar .content-section,
  .has-sidebar .hero-content,
  .has-sidebar .explore-view,
  .has-sidebar .search-view,
  .has-sidebar .watchlist-view,
  .has-sidebar .downloads-view,
  .has-sidebar .category-bar {
    padding-left: 1.25rem !important;
  }

  /* Fix sticky headers pinning to screen top when TopHeader is hidden in landscape/sidebar */
  .has-sidebar :deep(.category-bar),
  .has-sidebar :deep(.search-input-wrapper) {
    top: 0 !important;
  }
}

/* TV Mode explicit support (for 10-foot UI on smart TVs) */
:root.is-tv-mode .app-container.has-sidebar,
html.is-tv-mode .app-container.has-sidebar {
  padding-left: calc(78px + var(--safe-left, 0px));
}

:root.is-tv-mode .app-container.has-sidebar.is-details-mode,
html.is-tv-mode .app-container.has-sidebar.is-details-mode {
  padding-left: 0 !important;
}

:root.is-tv-mode .has-sidebar .main-content,
html.is-tv-mode .has-sidebar .main-content {
  padding-bottom: 2rem !important;
}

:root.is-tv-mode .has-sidebar .content-section,
:root.is-tv-mode .has-sidebar .hero-content,
:root.is-tv-mode .has-sidebar .explore-view,
:root.is-tv-mode .has-sidebar .search-view,
:root.is-tv-mode .has-sidebar .watchlist-view,
:root.is-tv-mode .has-sidebar .downloads-view,
:root.is-tv-mode .has-sidebar .category-bar,
html.is-tv-mode .has-sidebar .content-section,
html.is-tv-mode .has-sidebar .hero-content,
html.is-tv-mode .has-sidebar .explore-view,
html.is-tv-mode .has-sidebar .search-view,
html.is-tv-mode .has-sidebar .watchlist-view,
html.is-tv-mode .has-sidebar .downloads-view,
html.is-tv-mode .has-sidebar .category-bar {
  padding-left: 1.25rem !important;
}

:root.is-tv-mode .has-sidebar :deep(.category-bar),
:root.is-tv-mode .has-sidebar :deep(.search-input-wrapper),
html.is-tv-mode .has-sidebar :deep(.category-bar),
html.is-tv-mode .has-sidebar :deep(.search-input-wrapper) {
  top: 0 !important;
}

/* In portrait orientation on non-TV devices, sidebar margin MUST NEVER apply */
@media (orientation: portrait) {
  html:not(.is-tv-mode) .app-container,
  html:not(.is-tv-mode) .app-container.has-sidebar {
    padding-left: 0 !important;
  }

  html:not(.is-tv-mode) .has-sidebar .content-section,
  html:not(.is-tv-mode) .has-sidebar .hero-content,
  html:not(.is-tv-mode) .has-sidebar .explore-view,
  html:not(.is-tv-mode) .has-sidebar .search-view,
  html:not(.is-tv-mode) .has-sidebar .watchlist-view,
  html:not(.is-tv-mode) .has-sidebar .downloads-view,
  html:not(.is-tv-mode) .has-sidebar .category-bar {
    padding-left: unset;
  }
}

.main-content {
  flex: 1;
  position: relative;
  z-index: 10;
  padding-bottom: calc(var(--nav-height) + var(--safe-bottom) + 0.5rem);
}

.main-content.download-page-main {
  padding-bottom: 0;
}

@media (min-width: 768px) and (min-height: 551px) {
  .main-content {
    padding-bottom: calc(var(--nav-height) + 2.5rem);
  }
  .main-content.download-page-main {
    padding-bottom: 0;
  }
}

@media (orientation: landscape) {
  .main-content {
    padding-bottom: calc(var(--nav-height) + var(--safe-bottom) + 0.35rem);
  }
}

/* Splash Screen Smooth Fade Out */
.splash-fade-leave-active {
  transition: opacity 0.5s cubic-bezier(0.16, 1, 0.3, 1), transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}
.splash-fade-leave-to {
  opacity: 0;
  transform: scale(1.08);
}

/* Active Hero Expanded Body Background */
.body-hero-backdrop-wrap {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 1;
  overflow: hidden;
  opacity: 0;
  transition: opacity 1s cubic-bezier(0.4, 0, 0.2, 1);
  contain: strict;
}

.body-hero-backdrop-wrap.visible {
  opacity: 1;
}

.body-hero-backdrop-img {
  position: absolute;
  top: -15%;
  left: -15%;
  width: 130%;
  height: 130%;
  background-size: cover;
  background-position: center 20%;
  background-repeat: no-repeat;
  filter: blur(50px) saturate(1.4) brightness(0.42);
  transform: scale(1.08);
  will-change: transform, opacity;
  animation: heroBodyDrift 24s ease-in-out infinite alternate;
}

@keyframes heroBodyDrift {
  0% {
    transform: scale(1.08) translate3d(0, 0, 0);
  }
  50% {
    transform: scale(1.14) translate3d(-1.5%, -1%, 0);
  }
  100% {
    transform: scale(1.10) translate3d(1.2%, 0.8%, 0);
  }
}

.hero-body-fade-enter-active,
.hero-body-fade-leave-active {
  transition: opacity 1.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.hero-body-fade-enter-from,
.hero-body-fade-leave-to {
  opacity: 0;
}

.body-hero-gradient-overlay {
  position: absolute;
  inset: 0;
  background: 
    linear-gradient(
      to bottom,
      rgba(7, 7, 10, 0.15) 0%,
      rgba(7, 7, 10, 0.45) 30%,
      rgba(7, 7, 10, 0.82) 65%,
      var(--bg-main) 100%
    ),
    radial-gradient(
      ellipse 90% 70% at 50% 15%,
      transparent 0%,
      rgba(7, 7, 10, 0.35) 60%,
      var(--bg-main) 100%
    );
}

.body-hero-ambient-tint {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 520px;
  background: radial-gradient(circle at 50% 0%, rgba(245, 158, 11, 0.14) 0%, transparent 75%);
  transition: background 0.8s ease;
  pointer-events: none;
}

.body-hero-ambient-tint.tv-tint {
  background: radial-gradient(circle at 50% 0%, rgba(244, 63, 94, 0.14) 0%, transparent 75%);
}
</style>
