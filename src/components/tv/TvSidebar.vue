<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { 
  Film, 
  Tv, 
  Search, 
  Bookmark, 
  Heart,
  Clock,
  Compass,
  Sparkles,
  Download
} from 'lucide-vue-next';
import { MediaMode, TabKey } from '../../types';
import WatzemLogo from '../WatzemLogo.vue';
import { useWatchlist } from '../../composables/useWatchlist';
import { useTvMode } from '../../composables/useTvMode';
import { useAppUpdate } from '../../composables/useAppUpdate';

defineProps<{
  currentTab: TabKey;
  mode: MediaMode;
}>();

const emit = defineEmits<{
  (e: 'navigate', tab: TabKey): void;
  (e: 'update:mode', mode: MediaMode): void;
  (e: 'open-support'): void;
}>();

const { watchlist } = useWatchlist();
const { isTvMode } = useTvMode();
const { updateAvailable } = useAppUpdate();

const isExpanded = ref(false);
const currentTime = ref('');

let timeInterval: any = null;

const updateClock = () => {
  const now = new Date();
  currentTime.value = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

onMounted(() => {
  updateClock();
  timeInterval = setInterval(updateClock, 10000);
});

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval);
});

const handleItemClick = (tab: TabKey, targetMode?: MediaMode) => {
  if (targetMode) {
    emit('update:mode', targetMode);
  }
  emit('navigate', tab);
  // Auto collapse on touch devices
  if (typeof window !== 'undefined' && window.matchMedia('(pointer: coarse)').matches) {
    isExpanded.value = false;
  }
};
</script>

<template>
  <aside 
    class="tv-sidebar" 
    :class="{ expanded: isExpanded }"
    @mouseenter="isExpanded = true"
    @mouseleave="isExpanded = false"
    @focusin="isExpanded = true"
    @focusout="isExpanded = false"
  >
    <!-- Background Blur & Gradient -->
    <div class="sidebar-backdrop"></div>

    <!-- Top Brand Logo -->
    <div 
      class="sidebar-brand" 
      tabindex="0" 
      role="button" 
      title="WATZEM Home"
      @click="handleItemClick('home', 'movie')"
    >
      <WatzemLogo size="sm" :mode="mode" />
      <span class="brand-tv-pill" v-if="isTvMode">TV</span>
    </div>

    <!-- Navigation List (Unique, Mutually Exclusive Items) -->
    <nav class="sidebar-nav" role="navigation">
      <!-- 1. Search -->
      <button
        class="tv-nav-item"
        :class="{ active: currentTab === 'search' }"
        tabindex="0"
        role="button"
        title="Search"
        @click="handleItemClick('search')"
      >
        <div class="icon-wrap">
          <Search :size="21" :stroke-width="currentTab === 'search' ? 2.5 : 2" />
        </div>
        <span class="label">Search</span>
        <div class="active-indicator" v-if="currentTab === 'search'"></div>
      </button>

      <!-- 2. Movies (Home / Movie Catalog) -->
      <button
        class="tv-nav-item"
        :class="{ active: currentTab === 'home' && mode === 'movie' }"
        tabindex="0"
        role="button"
        title="Movies"
        @click="handleItemClick('home', 'movie')"
      >
        <div class="icon-wrap">
          <Film :size="21" :stroke-width="currentTab === 'home' && mode === 'movie' ? 2.5 : 2" />
        </div>
        <span class="label">Movies</span>
        <div class="active-indicator" v-if="currentTab === 'home' && mode === 'movie'"></div>
      </button>

      <!-- 3. TV Series (TV Shows Catalog) -->
      <button
        class="tv-nav-item"
        :class="{ active: currentTab === 'home' && mode === 'tv' }"
        tabindex="0"
        role="button"
        title="TV Series"
        @click="handleItemClick('home', 'tv')"
      >
        <div class="icon-wrap cyan-glow">
          <Tv :size="21" :stroke-width="currentTab === 'home' && mode === 'tv' ? 2.5 : 2" />
        </div>
        <span class="label">TV Series</span>
        <div class="active-indicator cyan-bg" v-if="currentTab === 'home' && mode === 'tv'"></div>
      </button>

      <!-- 4. Explore (Categories, Networks, Franchises, Eras) -->
      <button
        class="tv-nav-item"
        :class="{ active: currentTab === 'explore' }"
        tabindex="0"
        role="button"
        title="Explore"
        @click="handleItemClick('explore')"
      >
        <div class="icon-wrap purple-glow">
          <Compass :size="21" :stroke-width="currentTab === 'explore' ? 2.5 : 2" />
        </div>
        <span class="label">Explore</span>
        <div class="active-indicator purple-bg" v-if="currentTab === 'explore'"></div>
      </button>

      <!-- 5. Watchlist -->
      <button
        class="tv-nav-item"
        :class="{ active: currentTab === 'watchlist' }"
        tabindex="0"
        role="button"
        title="Watchlist"
        @click="handleItemClick('watchlist')"
      >
        <div class="icon-wrap">
          <Bookmark :size="21" :stroke-width="currentTab === 'watchlist' ? 2.5 : 2" />
          <span v-if="watchlist.length > 0" class="badge-dot"></span>
        </div>
        <span class="label">Watchlist</span>
        <span v-if="watchlist.length > 0" class="badge-count">
          {{ watchlist.length > 99 ? '99+' : watchlist.length }}
        </span>
        <div class="active-indicator" v-if="currentTab === 'watchlist'"></div>
      </button>
    </nav>

    <!-- Bottom Meta Area: Update, Support & Clock -->
    <div class="sidebar-footer">
      <!-- In-App Version Update / Info Button -->
      <button
        class="tv-nav-item update-btn"
        :class="{ active: currentTab === 'update', 'has-update': updateAvailable }"
        tabindex="0"
        role="button"
        :title="updateAvailable ? 'New Update Available' : 'App Updates & Info'"
        @click="handleItemClick('update')"
      >
        <div class="icon-wrap">
          <Sparkles v-if="updateAvailable" :size="19" class="badge-sparkle" />
          <Download v-else :size="19" class="update-icon" />
          <span v-if="updateAvailable" class="pulse-update-dot"></span>
        </div>
        <span class="label">{{ updateAvailable ? 'Update App' : 'Updates' }}</span>
        <div class="active-indicator" v-if="currentTab === 'update'"></div>
      </button>

      <button
        class="tv-nav-item support-btn"
        :class="{ active: currentTab === 'tips' }"
        tabindex="0"
        role="button"
        title="Support WATZEM"
        @click="handleItemClick('tips')"
      >
        <div class="icon-wrap">
          <Heart :size="19" :stroke-width="2.2" class="support-heart" />
        </div>
        <span class="label">Support & Tips</span>
      </button>

      <div class="sidebar-clock" v-if="currentTime">
        <Clock :size="13" class="clock-icon" />
        <span class="clock-text">{{ currentTime }}</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.tv-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: calc(78px + var(--safe-left, 0px));
  z-index: 1000;
  display: flex;
  flex-direction: column;
  padding: 1.25rem 0.5rem 1rem calc(0.5rem + var(--safe-left, 0px));
  transition: width 0.28s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.28s ease;
  user-select: none;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

/* In portrait orientation on non-TV devices, sidebar must be completely hidden */
@media (orientation: portrait) {
  html:not(.is-tv-mode) .tv-sidebar,
  :root:not(.is-tv-mode) .tv-sidebar {
    display: none !important;
  }
}

.tv-sidebar::-webkit-scrollbar {
  display: none;
}

.tv-sidebar.expanded {
  width: calc(230px + var(--safe-left, 0px));
  box-shadow: 24px 0 60px rgba(0, 0, 0, 0.88);
}

.sidebar-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(10, 10, 15, 0.94);
  backdrop-filter: blur(28px);
  -webkit-backdrop-filter: blur(28px);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  z-index: -1;
  pointer-events: none;
}

/* Brand header */
.sidebar-brand {
  position: relative;
  display: flex;
  align-items: center;
  cursor: pointer;
  outline: none;
  border-radius: 12px;
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  flex-shrink: 0;
}

.tv-sidebar:not(.expanded) .sidebar-brand {
  justify-content: center;
  padding: 0.5rem 0 1.25rem;
  width: 100%;
}

.tv-sidebar:not(.expanded) .sidebar-brand :deep(.brand-text-col) {
  display: none !important;
}

.tv-sidebar:not(.expanded) .brand-tv-pill {
  display: none !important;
}

.tv-sidebar.expanded .sidebar-brand {
  justify-content: flex-start;
  padding: 0.5rem 0.65rem 1.25rem;
  gap: 0.5rem;
}

.tv-sidebar.expanded .sidebar-brand :deep(.brand-text-col) {
  display: flex !important;
  opacity: 1;
  animation: brandTextFade 0.2s ease-out;
}

.tv-sidebar.expanded .brand-tv-pill {
  display: inline-block !important;
  opacity: 1;
  transform: translateX(0);
}

.sidebar-brand:focus-visible,
.sidebar-brand.tv-focused {
  transform: scale(1.08);
  outline: 2px solid #f59e0b;
  outline-offset: 4px;
}

.brand-tv-pill {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #000;
  font-size: 0.62rem;
  font-weight: 900;
  padding: 0.12rem 0.38rem;
  border-radius: 4px;
  letter-spacing: 0.05em;
  opacity: 0;
  transform: translateX(-6px);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

/* Navigation List */
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  flex: 1;
}

.tv-nav-item {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  background: transparent;
  border: none;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.65);
  font-family: inherit;
  font-size: 0.95rem;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  white-space: nowrap;
  outline: none;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.2s ease;
  user-select: none;
}

/* Collapsed: Perfectly Centered Icons */
.tv-sidebar:not(.expanded) .tv-nav-item {
  justify-content: center;
  padding: 0.65rem 0;
  gap: 0;
}

.tv-sidebar:not(.expanded) .tv-nav-item .icon-wrap {
  margin: 0 auto;
}

.tv-sidebar:not(.expanded) .tv-nav-item .label,
.tv-sidebar:not(.expanded) .tv-nav-item .badge-count {
  display: none !important;
}

/* Expanded: Left-aligned with Labels */
.tv-sidebar.expanded .tv-nav-item {
  justify-content: flex-start;
  padding: 0.65rem 0.85rem;
  gap: 0.85rem;
}

.tv-sidebar.expanded .tv-nav-item .icon-wrap {
  margin: 0;
}

.tv-sidebar.expanded .tv-nav-item .label {
  display: inline-block !important;
  opacity: 1;
  transform: translateX(0);
  animation: brandTextFade 0.2s ease-out;
}

.tv-nav-item .icon-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1), color 0.2s ease;
}

.tv-nav-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
}

.tv-nav-item.active {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
}

.tv-nav-item.active .icon-wrap {
  color: #f59e0b;
}

.tv-nav-item.active .icon-wrap.cyan-glow {
  color: #06b6d4;
}

.tv-nav-item.active .icon-wrap.purple-glow {
  color: #c084fc;
}

.tv-nav-item .active-indicator {
  position: absolute;
  left: 0;
  top: 18%;
  bottom: 18%;
  width: 3.5px;
  background: #f59e0b;
  border-radius: 0 4px 4px 0;
  box-shadow: 0 0 10px rgba(245, 158, 11, 0.85);
}

.tv-nav-item .active-indicator.cyan-bg {
  background: #06b6d4;
  box-shadow: 0 0 10px rgba(6, 182, 212, 0.85);
}

.tv-nav-item .active-indicator.purple-bg {
  background: #c084fc;
  box-shadow: 0 0 10px rgba(192, 132, 252, 0.85);
}

/* Watchlist Badges */
.badge-dot {
  position: absolute;
  top: -1px;
  right: -1px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f43f5e;
  box-shadow: 0 0 6px rgba(244, 63, 94, 0.8);
}

.tv-sidebar.expanded .badge-dot {
  display: none;
}

.badge-count {
  margin-left: auto;
  background: rgba(244, 63, 94, 0.22);
  color: #fb7185;
  border: 1px solid rgba(244, 63, 94, 0.4);
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.1rem 0.45rem;
  border-radius: 999px;
  animation: brandTextFade 0.2s ease-out;
}

/* D-pad and Keyboard TV Focus Ring */
.tv-nav-item:focus-visible,
.tv-nav-item.tv-focused {
  background: linear-gradient(90deg, rgba(245, 158, 11, 0.25), rgba(245, 158, 11, 0.08));
  color: #ffffff;
  outline: 3px solid #f59e0b !important;
  outline-offset: 2px;
  box-shadow: 0 0 25px rgba(245, 158, 11, 0.65);
  z-index: 10;
}

.tv-sidebar:not(.expanded) .tv-nav-item:focus-visible,
.tv-sidebar:not(.expanded) .tv-nav-item.tv-focused {
  transform: scale(1.08);
}

.tv-sidebar.expanded .tv-nav-item:focus-visible,
.tv-sidebar.expanded .tv-nav-item.tv-focused {
  transform: scale(1.04) translateX(4px);
}

.tv-nav-item:focus-visible .icon-wrap,
.tv-nav-item.tv-focused .icon-wrap {
  color: #f59e0b;
  transform: scale(1.12);
}

.tv-nav-item:focus-visible .icon-wrap.cyan-glow,
.tv-nav-item.tv-focused .icon-wrap.cyan-glow {
  color: #06b6d4;
}

.tv-nav-item:focus-visible .icon-wrap.purple-glow,
.tv-nav-item.tv-focused .icon-wrap.purple-glow {
  color: #c084fc;
}

/* Footer Support, Update & Clock */
.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  margin-top: auto;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 0.65rem;
  flex-shrink: 0;
}

.update-btn {
  color: var(--text-secondary);
}

.update-btn.has-update {
  color: #facc15;
}

.update-btn:hover,
.update-btn:focus-visible,
.update-btn.tv-focused {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.update-btn.has-update:hover,
.update-btn.has-update:focus-visible,
.update-btn.has-update.tv-focused {
  background: rgba(250, 204, 21, 0.14);
  color: #fef08a;
}

.badge-sparkle {
  color: #facc15;
  animation: sparkleTwinkle 2s ease infinite;
}

.pulse-update-dot {
  position: absolute;
  top: 1px;
  right: 1px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #facc15;
  box-shadow: 0 0 8px #facc15;
  animation: pulseUpdateDot 1.8s infinite;
}

@keyframes pulseUpdateDot {
  0% { transform: scale(0.95); opacity: 0.7; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.7; }
}

@keyframes sparkleTwinkle {
  0%, 100% { transform: scale(1); opacity: 0.9; }
  50% { transform: scale(1.2); opacity: 1; }
}

.support-heart {
  color: #f43f5e;
  transition: transform 0.2s ease;
}

.tv-nav-item.support-btn:hover .support-heart,
.tv-nav-item.support-btn:focus-visible .support-heart,
.tv-nav-item.support-btn.tv-focused .support-heart {
  transform: scale(1.18);
}

.sidebar-clock {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  padding: 0.2rem 0.5rem;
  color: rgba(255, 255, 255, 0.45);
  font-size: 0.78rem;
  font-variant-numeric: tabular-nums;
  font-family: inherit;
  user-select: none;
}

.tv-sidebar:not(.expanded) .sidebar-clock .clock-icon {
  display: none;
}

.tv-sidebar.expanded .sidebar-clock {
  justify-content: flex-start;
  padding: 0.2rem 0.85rem;
  font-size: 0.82rem;
}

.clock-icon {
  opacity: 0.7;
}

@keyframes brandTextFade {
  from { opacity: 0; transform: translateX(-6px); }
  to { opacity: 1; transform: translateX(0); }
}

/* Responsive Compact Landscape Scaling */
@media (max-height: 680px), (orientation: landscape) and (max-height: 720px) {
  .tv-sidebar {
    padding: 0.65rem 0.4rem 0.65rem calc(0.4rem + var(--safe-left, 0px));
  }
  .sidebar-brand {
    padding: 0.2rem 0 0.55rem;
  }
  .tv-sidebar:not(.expanded) .sidebar-brand {
    padding: 0.2rem 0 0.55rem;
  }
  .tv-sidebar.expanded .sidebar-brand {
    padding: 0.2rem 0.6rem 0.55rem;
  }
  .sidebar-nav {
    gap: 0.25rem;
  }
  .tv-nav-item {
    font-size: 0.85rem;
    border-radius: 10px;
  }
  .tv-sidebar:not(.expanded) .tv-nav-item {
    padding: 0.45rem 0;
  }
  .tv-sidebar.expanded .tv-nav-item {
    padding: 0.45rem 0.65rem;
    gap: 0.65rem;
  }
  .tv-nav-item .icon-wrap {
    min-width: 22px;
    width: 22px;
    height: 22px;
  }
  .sidebar-footer {
    padding-top: 0.4rem;
    gap: 0.25rem;
  }
  .sidebar-clock {
    padding: 0.15rem 0.4rem;
    font-size: 0.72rem;
  }
}

@media (max-height: 480px) {
  .tv-sidebar {
    padding: 0.35rem 0.3rem 0.35rem calc(0.3rem + var(--safe-left, 0px));
  }
  .sidebar-brand {
    padding: 0.1rem 0 0.35rem;
  }
  .tv-sidebar:not(.expanded) .sidebar-brand {
    padding: 0.1rem 0 0.35rem;
  }
  .sidebar-nav {
    gap: 0.15rem;
  }
  .tv-nav-item {
    padding: 0.32rem 0.25rem;
  }
  .tv-sidebar:not(.expanded) .tv-nav-item {
    padding: 0.32rem 0;
  }
  .tv-sidebar.expanded .tv-nav-item {
    padding: 0.32rem 0.5rem;
    gap: 0.5rem;
  }
  .sidebar-clock {
    display: none;
  }
}
</style>

