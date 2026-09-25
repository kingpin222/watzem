<script setup lang="ts">
import { computed } from 'vue';
import { TabKey, MediaMode } from '../types';
import { useWatchlist } from '../composables/useWatchlist';
import { useHaptics } from '../composables/useHaptics';
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
</script>

<template>
  <nav class="bottom-nav">
    <div class="nav-inner">
      <!-- Home Tab -->
      <button
        class="nav-tab"
        :class="{ active: currentTab === 'home', 'tv-theme': isTv }"
        @click="handleSelect('home')"
      >
        <div class="icon-wrap">
          <Home :size="20" stroke-width="2.2" />
        </div>
        <span class="tab-label">Home</span>
        <span v-if="currentTab === 'home'" class="active-dot"></span>
      </button>

      <!-- Explore Tab -->
      <button
        class="nav-tab"
        :class="{ active: currentTab === 'explore', 'tv-theme': isTv }"
        @click="handleSelect('explore')"
      >
        <div class="icon-wrap">
          <Compass :size="20" stroke-width="2.2" />
        </div>
        <span class="tab-label">Explore</span>
        <span v-if="currentTab === 'explore'" class="active-dot"></span>
      </button>

      <!-- Search Tab -->
      <button
        class="nav-tab"
        :class="{ active: currentTab === 'search', 'tv-theme': isTv }"
        @click="handleSelect('search')"
      >
        <div class="icon-wrap">
          <Search :size="20" stroke-width="2.2" />
        </div>
        <span class="tab-label">Search</span>
        <span v-if="currentTab === 'search'" class="active-dot"></span>
      </button>

      <!-- Watchlist Tab -->
      <button
        class="nav-tab"
        :class="{ active: currentTab === 'watchlist', 'tv-theme': isTv }"
        @click="handleSelect('watchlist')"
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
  background: rgba(10, 10, 15, 0.92);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid var(--border-subtle);
  padding-bottom: var(--safe-bottom);
  transition: background 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
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

@media (orientation: landscape) and (max-height: 550px) {
  .bottom-nav {
    padding-bottom: 0;
  }
}

.nav-inner {
  max-width: 600px;
  margin: 0 auto;
  height: var(--nav-height);
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 0 max(0.5rem, var(--safe-left, 0px));
  padding-right: max(0.5rem, var(--safe-right, 0px));
}

@media (orientation: landscape) and (max-height: 550px) {
  .nav-inner {
    height: 50px;
  }
}

.nav-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  height: 100%;
  color: var(--text-muted);
  transition: color 0.25s cubic-bezier(0.16, 1, 0.3, 1), transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
}

.nav-tab:active {
  transform: scale(0.92);
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
