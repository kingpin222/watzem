<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { 
  Home, 
  Film, 
  Tv, 
  Search, 
  Bookmark, 
  HelpCircle,
  Clock,
  Compass
} from 'lucide-vue-next';
import { MediaMode, TabKey } from '../../types';
import WatzemLogo from '../WatzemLogo.vue';

defineProps<{
  currentTab: TabKey;
  mode: MediaMode;
}>();

const emit = defineEmits<{
  (e: 'navigate', tab: TabKey): void;
  (e: 'update:mode', mode: MediaMode): void;
  (e: 'open-support'): void;
}>();

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
    <div class="sidebar-brand" tabindex="0" role="button" @click="handleItemClick('home', 'movie')">
      <WatzemLogo size="sm" :mode="mode" />
      <span class="brand-tv-pill">TV</span>
    </div>

    <!-- Navigation List -->
    <nav class="sidebar-nav" role="navigation">
      <!-- 1. Search -->
      <button
        class="tv-nav-item"
        :class="{ active: currentTab === 'search' }"
        tabindex="0"
        role="button"
        @click="handleItemClick('search')"
      >
        <div class="icon-wrap">
          <Search :size="24" :stroke-width="currentTab === 'search' ? 2.5 : 2" />
        </div>
        <span class="label">Search</span>
        <div class="active-indicator" v-if="currentTab === 'search'"></div>
      </button>

      <!-- 2. Home / Discover -->
      <button
        class="tv-nav-item"
        :class="{ active: currentTab === 'home' && mode === 'movie' }"
        tabindex="0"
        role="button"
        @click="handleItemClick('home', 'movie')"
      >
        <div class="icon-wrap">
          <Home :size="24" :stroke-width="currentTab === 'home' && mode === 'movie' ? 2.5 : 2" />
        </div>
        <span class="label">Home</span>
        <div class="active-indicator" v-if="currentTab === 'home' && mode === 'movie'"></div>
      </button>

      <!-- 3. Explore (Categories, Networks, Franchises, Eras) -->
      <button
        class="tv-nav-item"
        :class="{ active: currentTab === 'explore' }"
        tabindex="0"
        role="button"
        @click="handleItemClick('explore')"
      >
        <div class="icon-wrap purple-glow">
          <Compass :size="24" :stroke-width="currentTab === 'explore' ? 2.5 : 2" />
        </div>
        <span class="label">Explore</span>
        <div class="active-indicator purple-bg" v-if="currentTab === 'explore'"></div>
      </button>

      <!-- 4. Movies -->
      <button
        class="tv-nav-item"
        :class="{ active: currentTab === 'home' && mode === 'movie' }"
        tabindex="0"
        role="button"
        @click="handleItemClick('home', 'movie')"
      >
        <div class="icon-wrap">
          <Film :size="24" :stroke-width="2" />
        </div>
        <span class="label">Movies</span>
      </button>

      <!-- 5. TV Series -->
      <button
        class="tv-nav-item"
        :class="{ active: currentTab === 'home' && mode === 'tv' }"
        tabindex="0"
        role="button"
        @click="handleItemClick('home', 'tv')"
      >
        <div class="icon-wrap cyan-glow">
          <Tv :size="24" :stroke-width="2" />
        </div>
        <span class="label">TV Series</span>
        <div class="active-indicator cyan-bg" v-if="currentTab === 'home' && mode === 'tv'"></div>
      </button>

      <!-- 6. Watchlist -->
      <button
        class="tv-nav-item"
        :class="{ active: currentTab === 'watchlist' }"
        tabindex="0"
        role="button"
        @click="handleItemClick('watchlist')"
      >
        <div class="icon-wrap">
          <Bookmark :size="24" :stroke-width="currentTab === 'watchlist' ? 2.5 : 2" />
        </div>
        <span class="label">Watchlist</span>
        <div class="active-indicator" v-if="currentTab === 'watchlist'"></div>
      </button>
    </nav>

    <!-- Bottom Meta Area: Clock & Support -->
    <div class="sidebar-footer">
      <button
        class="tv-nav-item support-btn"
        tabindex="0"
        role="button"
        title="Help & Info"
        @click="emit('open-support')"
      >
        <div class="icon-wrap">
          <HelpCircle :size="22" />
        </div>
        <span class="label">Help & Tips</span>
      </button>

      <div class="sidebar-clock" v-if="currentTime">
        <Clock :size="15" class="clock-icon" />
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
  width: 78px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  padding: 1.5rem 0.75rem;
  transition: width 0.28s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.28s ease;
  user-select: none;
}

.tv-sidebar.expanded {
  width: 230px;
  box-shadow: 20px 0 60px rgba(0, 0, 0, 0.85);
}

.sidebar-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(10, 10, 15, 0.88);
  backdrop-filter: blur(28px);
  -webkit-backdrop-filter: blur(28px);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  z-index: -1;
}

/* Brand header */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.4rem 1.75rem;
  cursor: pointer;
  outline: none;
}

.sidebar-brand:focus-visible,
.sidebar-brand.tv-focused {
  transform: scale(1.08);
}

.brand-tv-pill {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #000;
  font-size: 0.65rem;
  font-weight: 900;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
  letter-spacing: 0.05em;
  opacity: 0;
  transform: translateX(-6px);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.tv-sidebar.expanded .brand-tv-pill {
  opacity: 1;
  transform: translateX(0);
}

/* Navigation List */
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
}

.tv-nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 1rem;
  width: 100%;
  padding: 0.75rem 0.85rem;
  background: transparent;
  border: none;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.65);
  font-family: inherit;
  font-size: 1rem;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  white-space: nowrap;
  outline: none;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.2s ease;
}

.tv-nav-item .icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1), color 0.2s ease;
}

.tv-nav-item .label {
  opacity: 0;
  transform: translateX(-8px);
  transition: opacity 0.2s ease, transform 0.2s ease;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tv-sidebar.expanded .tv-nav-item .label {
  opacity: 1;
  transform: translateX(0);
}

.tv-nav-item.active {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
}

.tv-nav-item.active .icon-wrap {
  color: #f59e0b;
}

.tv-nav-item .active-indicator {
  position: absolute;
  left: 0;
  top: 20%;
  bottom: 20%;
  width: 4px;
  background: #f59e0b;
  border-radius: 0 4px 4px 0;
  box-shadow: 0 0 10px rgba(245, 158, 11, 0.8);
}

.tv-nav-item.active .icon-wrap.purple-glow {
  color: #c084fc;
}

.tv-nav-item .active-indicator.cyan-bg {
  background: #06b6d4;
  box-shadow: 0 0 10px rgba(6, 182, 212, 0.8);
}

.tv-nav-item .active-indicator.purple-bg {
  background: #c084fc;
  box-shadow: 0 0 10px rgba(192, 132, 252, 0.8);
}

/* D-pad and Keyboard TV Focus Ring */
.tv-nav-item:focus-visible,
.tv-nav-item.tv-focused {
  background: linear-gradient(90deg, rgba(245, 158, 11, 0.25), rgba(245, 158, 11, 0.08));
  color: #ffffff;
  outline: 3px solid #f59e0b !important;
  outline-offset: 2px;
  box-shadow: 0 0 25px rgba(245, 158, 11, 0.65);
  transform: scale(1.08) translateX(4px);
  z-index: 10;
}

.tv-nav-item:focus-visible .icon-wrap,
.tv-nav-item.tv-focused .icon-wrap {
  color: #f59e0b;
  transform: scale(1.15);
}

.tv-nav-item:focus-visible .icon-wrap.cyan-glow,
.tv-nav-item.tv-focused .icon-wrap.cyan-glow {
  color: #06b6d4;
}

.tv-nav-item:focus-visible .icon-wrap.purple-glow,
.tv-nav-item.tv-focused .icon-wrap.purple-glow {
  color: #c084fc;
}

/* Footer Clock and Support */
.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: auto;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 1rem;
}

.sidebar-clock {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0.85rem;
  color: rgba(255, 255, 255, 0.4);
  font-size: 0.85rem;
  font-variant-numeric: tabular-nums;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.tv-sidebar.expanded .sidebar-clock {
  opacity: 1;
}

.clock-icon {
  opacity: 0.7;
}
</style>
