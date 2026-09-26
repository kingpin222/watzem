<script setup lang="ts">
import { computed } from 'vue';
import { MediaMode, TabKey } from '../types';
import { useHaptics } from '../composables/useHaptics';
import { useAppUpdate } from '../composables/useAppUpdate';
import { Film, Tv, Sparkles, Heart } from 'lucide-vue-next';
import WatzemLogo from './WatzemLogo.vue';
import { useMonetization } from '../composables/useMonetization';

const props = defineProps<{
  mode: MediaMode;
  currentTab: TabKey;
}>();

const emit = defineEmits<{
  (e: 'update:mode', mode: MediaMode): void;
  (e: 'navigate', tab: TabKey): void;
}>();

const { triggerLight } = useHaptics();
const { updateAvailable, openModal } = useAppUpdate();
const { openSupportModal } = useMonetization();

const isMovie = computed(() => props.mode === 'movie');

const handleModeChange = (newMode: MediaMode) => {
  if (props.mode !== newMode) {
    triggerLight();
    emit('update:mode', newMode);
  }
};
</script>

<template>
  <header class="top-header">
    <div class="header-content">
      <!-- Logo Brand -->
      <div class="brand" @click="emit('navigate', 'home')">
        <WatzemLogo size="sm" :mode="mode" />
      </div>

      <!-- Mode Switcher Pill -->
      <div class="mode-pill-wrap">
        <div class="mode-pill">
          <div class="pill-thumb" :class="{ 'is-tv': !isMovie }"></div>
          <button
            class="pill-btn"
            :class="{ active: isMovie }"
            @click="handleModeChange('movie')"
          >
            <Film :size="13" />
            <span>Movies</span>
          </button>
          <button
            class="pill-btn"
            :class="{ active: !isMovie }"
            @click="handleModeChange('tv')"
          >
            <Tv :size="13" />
            <span>TV</span>
          </button>
        </div>
      </div>

      <!-- Actions Area -->
      <div class="header-actions">
        <!-- In-App Version Update Available Button -->
        <button
          v-if="updateAvailable"
          class="update-badge-btn"
          aria-label="Update Available"
          title="New version available"
          @click="openModal"
        >
          <Sparkles :size="13" class="badge-sparkle" />
          <span class="badge-text">Update</span>
          <span class="pulsing-dot"></span>
        </button>

        <!-- Support / Tip Jar Button -->
        <button
          class="support-btn"
          aria-label="Support the app"
          title="Support WATZEM"
          @click="openSupportModal"
        >
          <Heart :size="15" :stroke-width="2.2" class="support-heart" />
          <span class="support-text">Tip</span>
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.top-header {
  position: sticky;
  top: 0;
  z-index: 50;
  width: 100%;
  background: rgba(7, 7, 10, 0.78);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-bottom: 1px solid var(--border-subtle);
  padding-top: var(--safe-top);
  transition: padding-top 0.25s ease;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--header-height);
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 max(1rem, var(--safe-left, 0px));
  padding-right: max(1rem, var(--safe-right, 0px));
  gap: 0.75rem;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}

.logo-icon {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--accent-amber), #d97706);
  color: #07070a;
  display: grid;
  place-items: center;
  box-shadow: 0 2px 12px var(--accent-amber-glow);
  transition: all 0.3s ease;
}

.logo-icon.is-tv {
  background: linear-gradient(135deg, var(--accent-rose), #e11d48);
  box-shadow: 0 2px 12px var(--accent-rose-glow);
}

.brand-title {
  font-size: 1.15rem;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: var(--text-primary);
}

.brand-accent {
  background: linear-gradient(135deg, var(--accent-amber), #f59e0b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  transition: all 0.3s ease;
}

.brand-accent.is-tv {
  background: linear-gradient(135deg, var(--accent-rose), #f43f5e);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* Mode pill */
.mode-pill-wrap {
  flex: 0 1 170px;
  min-width: 136px;
  display: flex;
  justify-content: center;
}

.mode-pill {
  position: relative;
  display: flex;
  width: 100%;
  background: var(--bg-surface-elevated);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  padding: 3px;
}

.pill-thumb {
  position: absolute;
  top: 3px;
  bottom: 3px;
  left: 3px;
  width: calc(50% - 3px);
  background: var(--accent-amber);
  border-radius: var(--radius-full);
  box-shadow: 0 2px 8px var(--accent-amber-glow);
  transition: transform 0.28s cubic-bezier(0.34, 1.4, 0.5, 1), background 0.25s ease;
  z-index: 1;
}

.pill-thumb.is-tv {
  transform: translateX(100%);
  background: var(--accent-rose);
  box-shadow: 0 2px 8px var(--accent-rose-glow);
}

.pill-btn {
  position: relative;
  z-index: 2;
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0.36rem 0.25rem;
  color: var(--text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  text-align: center;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: color 0.2s ease;
  user-select: none;
}

.pill-btn.active {
  color: #07070a;
  font-weight: 700;
}


/* Header Actions Group */
.header-actions {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  flex-shrink: 0;
}

/* Support / Tip Jar Button - aligned to system design tokens */
.support-btn {
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0 0.75rem;
  border-radius: var(--radius-full);
  background: var(--bg-surface-elevated);
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  font-size: 0.76rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  user-select: none;
  flex-shrink: 0;
}

.support-btn:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-active);
  transform: translateY(-1px);
}

.support-btn:active {
  transform: scale(0.96);
}

.support-heart {
  display: block;
  color: #f43f5e;
  flex-shrink: 0;
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.support-btn:hover .support-heart {
  transform: scale(1.15);
}

.support-text {
  line-height: 1;
  font-weight: 600;
  letter-spacing: -0.2px;
}

/* In-App Update Badge Button */
.update-badge-btn {
  height: 32px;
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  background: rgba(250, 204, 21, 0.15);
  border: 1px solid rgba(250, 204, 21, 0.4);
  color: #facc15;
  padding: 0 0.75rem;
  border-radius: var(--radius-full);
  font-size: 0.74rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 0 12px rgba(250, 204, 21, 0.2);
  user-select: none;
  flex-shrink: 0;
}

.update-badge-btn:hover {
  background: rgba(250, 204, 21, 0.25);
  border-color: rgba(250, 204, 21, 0.6);
  transform: translateY(-1px);
}

.update-badge-btn:active {
  transform: scale(0.96);
}

.badge-sparkle {
  color: #facc15;
  animation: sparkleTwinkle 2s ease infinite;
}

@keyframes sparkleTwinkle {
  0%, 100% { transform: scale(1); opacity: 0.9; }
  50% { transform: scale(1.2); opacity: 1; }
}

.pulsing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #facc15;
  box-shadow: 0 0 8px #facc15;
  animation: pulseDot 1.8s infinite;
}

@keyframes pulseDot {
  0% { transform: scale(0.95); opacity: 0.7; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.7; }
}

/* Mobile responsive optimizations for top buttons & safe zones */
@media (max-width: 480px) {
  .header-content {
    padding: 0 max(0.75rem, var(--safe-left, 0px));
    padding-right: max(0.75rem, var(--safe-right, 0px));
    gap: 0.45rem;
  }

  .mode-pill-wrap {
    flex: 0 1 146px;
    min-width: 130px;
  }

  .pill-btn {
    font-size: 0.72rem;
    gap: 3px;
    padding: 0.32rem 0.15rem;
  }

  .support-btn {
    height: 32px;
    padding: 0 0.6rem;
    font-size: 0.72rem;
    gap: 4px;
  }

  .update-badge-btn {
    height: 32px;
    padding: 0 0.55rem;
    font-size: 0.72rem;
    gap: 4px;
  }
}

@media (max-width: 370px) {
  .header-content {
    padding: 0 0.5rem;
    gap: 0.35rem;
  }

  .mode-pill-wrap {
    flex: 0 1 125px;
    min-width: 118px;
  }

  .pill-btn span {
    font-size: 0.68rem;
  }

  .support-text {
    display: none;
  }

  .support-btn {
    width: 32px;
    padding: 0;
    justify-content: center;
  }

  .update-badge-btn .badge-text {
    display: none;
  }

  .update-badge-btn {
    width: 32px;
    padding: 0;
    justify-content: center;
  }
}
</style>
