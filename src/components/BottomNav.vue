<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { TabKey, MediaMode } from '../types';
import { useWatchlist } from '../composables/useWatchlist';
import { useHaptics } from '../composables/useHaptics';
import { tvSpatialNav } from '../services/tvSpatialNav';
import { Home, Compass, Search, Bookmark } from 'lucide-vue-next';

const props = defineProps<{
  currentTab: TabKey;
  mode: MediaMode;
}>();

const emit = defineEmits<{
  (e: 'navigate', tab: TabKey): void;
}>();

const { watchlist } = useWatchlist();
const { triggerLight } = useHaptics();

const isTv = computed(() => props.mode === 'tv');

const handleSelect = (tab: TabKey) => {
  if (props.currentTab !== tab) {
    triggerLight();
    emit('navigate', tab);
  }
};

const tabOrder: TabKey[] = ['home', 'explore', 'search', 'watchlist'];
const tabButtons = ref<Record<string, HTMLElement | null>>({});

const setTabRef = (key: TabKey, el: any) => {
  if (el) {
    tabButtons.value[key] = el as HTMLElement;
  }
};

const handleKeyDown = (e: KeyboardEvent, current: TabKey) => {
  const currentIndex = tabOrder.indexOf(current);
  if (currentIndex === -1) return;

  const key = e.key;
  const keyCode = e.keyCode || e.which;

  const isLeft = key === 'ArrowLeft' || keyCode === 21 || keyCode === 37;
  const isRight = key === 'ArrowRight' || keyCode === 22 || keyCode === 39;
  const isUp = key === 'ArrowUp' || keyCode === 19 || keyCode === 38;
  const isDown = key === 'ArrowDown' || keyCode === 20 || keyCode === 40;
  const isEnter = key === 'Enter' || key === ' ' || keyCode === 23 || keyCode === 13 || keyCode === 66;

  if (isLeft) {
    e.preventDefault();
    e.stopPropagation();
    const prevKey = tabOrder[(currentIndex - 1 + tabOrder.length) % tabOrder.length];
    const el = tabButtons.value[prevKey];
    if (el) {
      tvSpatialNav.focusElement(el);
    }
  } else if (isRight) {
    e.preventDefault();
    e.stopPropagation();
    const nextKey = tabOrder[(currentIndex + 1) % tabOrder.length];
    const el = tabButtons.value[nextKey];
    if (el) {
      tvSpatialNav.focusElement(el);
    }
  } else if (isUp) {
    e.preventDefault();
    e.stopPropagation();
    tvSpatialNav.navigate('up');
  } else if (isDown) {
    e.preventDefault();
    e.stopPropagation();
    // Stay inside bottom nav
  } else if (isEnter) {
    e.preventDefault();
    e.stopPropagation();
    handleSelect(current);
  }
};

onMounted(() => {
  if (document.documentElement.classList.contains('is-dpad-active')) {
    const el = tabButtons.value[props.currentTab];
    if (el) {
      tvSpatialNav.focusElement(el);
    }
  }
});
</script>

<template>
  <nav class="bottom-nav">
    <div class="nav-inner">
      <!-- Home Tab -->
      <button
        :ref="(el) => setTabRef('home', el)"
        type="button"
        class="nav-tab"
        :class="{ active: currentTab === 'home', 'tv-theme': isTv }"
        aria-label="Home"
        tabindex="0"
        data-tv-focusable="true"
        data-tab-key="home"
        @click="handleSelect('home')"
        @keydown="handleKeyDown($event, 'home')"
      >
        <div class="icon-wrap">
          <Home :size="20" stroke-width="2.2" />
        </div>
        <span class="tab-label">Home</span>
        <span v-if="currentTab === 'home'" class="active-dot"></span>
      </button>

      <!-- Explore Tab -->
      <button
        :ref="(el) => setTabRef('explore', el)"
        type="button"
        class="nav-tab"
        :class="{ active: currentTab === 'explore', 'tv-theme': isTv }"
        aria-label="Explore"
        tabindex="0"
        data-tv-focusable="true"
        data-tab-key="explore"
        @click="handleSelect('explore')"
        @keydown="handleKeyDown($event, 'explore')"
      >
        <div class="icon-wrap">
          <Compass :size="20" stroke-width="2.2" />
        </div>
        <span class="tab-label">Explore</span>
        <span v-if="currentTab === 'explore'" class="active-dot"></span>
      </button>

      <!-- Search Tab -->
      <button
        :ref="(el) => setTabRef('search', el)"
        type="button"
        class="nav-tab"
        :class="{ active: currentTab === 'search', 'tv-theme': isTv }"
        aria-label="Search"
        tabindex="0"
        data-tv-focusable="true"
        data-tab-key="search"
        @click="handleSelect('search')"
        @keydown="handleKeyDown($event, 'search')"
      >
        <div class="icon-wrap">
          <Search :size="20" stroke-width="2.2" />
        </div>
        <span class="tab-label">Search</span>
        <span v-if="currentTab === 'search'" class="active-dot"></span>
      </button>

      <!-- Watchlist Tab -->
      <button
        :ref="(el) => setTabRef('watchlist', el)"
        type="button"
        class="nav-tab"
        :class="{ active: currentTab === 'watchlist', 'tv-theme': isTv }"
        aria-label="Watchlist"
        tabindex="0"
        data-tv-focusable="true"
        data-tab-key="watchlist"
        @click="handleSelect('watchlist')"
        @keydown="handleKeyDown($event, 'watchlist')"
      >
        <div class="icon-wrap">
          <Bookmark :size="20" stroke-width="2.2" />
          <span v-if="watchlist.length > 0" class="badge-count">
            {{ watchlist.length > 99 ? '99+' : watchlist.length }}
          </span>
        </div>
        <span class="tab-label">Watchlist</span>
        <span v-if="currentTab === 'watchlist'" class="active-dot"></span>
      </button>
    </div>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 50;
  background: rgba(10, 10, 15, 0.94);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid var(--border-subtle);
  padding-bottom: max(var(--safe-bottom, 0px), env(safe-area-inset-bottom, 0px));
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.45);
  transition: background 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
}

.nav-inner {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-around;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
  height: var(--nav-height, 64px);
  padding: 0 max(0.5rem, var(--safe-left, 0px));
  padding-right: max(0.5rem, var(--safe-right, 0px));
  box-sizing: border-box;
}

@media (min-width: 768px) and (min-height: 551px) {
  .bottom-nav {
    left: 50%;
    right: auto;
    transform: translateX(-50%);
    bottom: 1.25rem;
    width: min(540px, calc(100vw - 2rem));
    border-radius: var(--radius-full);
    border: 1px solid var(--border-active);
    box-shadow: 0 12px 35px rgba(0, 0, 0, 0.7);
    padding-bottom: 0;
  }
}

@media (orientation: landscape) {
  .bottom-nav {
    padding-bottom: 0;
  }
  .nav-inner {
    height: var(--nav-height, 52px);
    max-width: 500px;
    padding: 0 max(1.25rem, var(--safe-left, 0px));
    padding-right: max(1.25rem, var(--safe-right, 0px));
  }
  .nav-tab {
    gap: 2px;
  }
  .tab-label {
    font-size: 0.68rem;
  }
}

.nav-tab {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  height: 100%;
  background: transparent;
  border: none;
  outline: none;
  cursor: pointer;
  padding: 4px 0;
  color: var(--text-muted);
  transition: color 0.25s cubic-bezier(0.16, 1, 0.3, 1), transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}

.nav-tab:active {
  transform: scale(0.92);
}

/* D-Pad & Keyboard Remote Focus Styling */
.nav-tab:focus-visible,
.nav-tab.tv-focused {
  outline: 2.5px solid var(--accent-amber) !important;
  outline-offset: -2px !important;
  border-radius: var(--radius-md);
  background: rgba(245, 158, 11, 0.16);
  box-shadow: 0 0 18px rgba(245, 158, 11, 0.45), inset 0 0 8px rgba(245, 158, 11, 0.2);
  color: var(--accent-amber);
  transform: translateY(-2px) scale(1.06);
}

.nav-tab.tv-theme:focus-visible,
.nav-tab.tv-theme.tv-focused {
  outline: 2.5px solid var(--accent-rose) !important;
  background: rgba(244, 63, 94, 0.16);
  box-shadow: 0 0 18px rgba(244, 63, 94, 0.45), inset 0 0 8px rgba(244, 63, 94, 0.2);
  color: var(--accent-rose);
}

.nav-tab:focus-visible .icon-wrap,
.nav-tab.tv-focused .icon-wrap {
  transform: scale(1.15) translateY(-1px);
}

.nav-tab:focus-visible .tab-label,
.nav-tab.tv-focused .tab-label {
  font-weight: 700;
  color: #fff;
}

.icon-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.tab-label {
  font-size: 0.65rem;
  font-weight: 500;
  letter-spacing: 0.1px;
}

.nav-tab.active {
  color: var(--accent-amber);
}

.nav-tab.active.tv-theme {
  color: var(--accent-rose);
}

.nav-tab.active .icon-wrap {
  transform: translateY(-2px) scale(1.1);
  animation: tabBounce 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes tabBounce {
  0% { transform: translateY(0) scale(0.9); }
  50% { transform: translateY(-3px) scale(1.18); }
  100% { transform: translateY(-2px) scale(1.1); }
}

.active-dot {
  position: absolute;
  bottom: 4px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 8px currentColor;
  animation: dotFade 0.25s ease;
}

@keyframes dotFade {
  from { opacity: 0; transform: scale(0); }
  to { opacity: 1; transform: scale(1); }
}

.badge-count {
  position: absolute;
  top: -4px;
  right: -8px;
  background: var(--accent-rose);
  color: #fff;
  font-size: 0.58rem;
  font-weight: 700;
  padding: 1px 4px;
  border-radius: var(--radius-full);
  min-width: 15px;
  text-align: center;
  border: 1px solid var(--bg-main);
}

.badge-count.emerald {
  background: var(--accent-emerald);
}
</style>
