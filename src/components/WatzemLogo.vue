<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    size?: 'sm' | 'md' | 'lg' | 'xl';
    mode?: 'movie' | 'tv';
    animated?: boolean;
    showTagline?: boolean;
  }>(),
  {
    size: 'md',
    mode: 'movie',
    animated: false,
    showTagline: false
  }
);

const isTv = computed(() => props.mode === 'tv');

const iconDimensions = computed(() => {
  switch (props.size) {
    case 'sm':
      return { width: 28, height: 28, iconSize: 16 };
    case 'md':
      return { width: 34, height: 34, iconSize: 20 };
    case 'lg':
      return { width: 56, height: 56, iconSize: 32 };
    case 'xl':
      return { width: 76, height: 76, iconSize: 44 };
    default:
      return { width: 34, height: 34, iconSize: 20 };
  }
});
</script>

<template>
  <div
    class="watzem-brand-wrapper"
    :class="[
      `size-${size}`,
      { 'is-tv': isTv, 'is-animated': animated }
    ]"
  >
    <!-- Geometric Prism Logo Mark -->
    <div
      class="logo-mark"
      :style="{ width: `${iconDimensions.width}px`, height: `${iconDimensions.height}px` }"
    >
      <svg
        viewBox="0 0 48 48"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        class="logo-svg"
      >
        <defs>
          <linearGradient :id="`watzem-bg-${size}`" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse">
            <stop offset="0%" :stop-color="isTv ? '#e11d48' : '#f59e0b'" />
            <stop offset="100%" :stop-color="isTv ? '#be123c' : '#d97706'" />
          </linearGradient>
          <linearGradient :id="`watzem-accent-${size}`" x1="12" y1="12" x2="36" y2="36" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#ffffff" stop-opacity="0.95" />
            <stop offset="100%" stop-color="#ffffff" stop-opacity="0.7" />
          </linearGradient>
          <linearGradient :id="`watzem-neon-${size}`" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stop-color="#fbbf24" />
            <stop offset="50%" stop-color="#f59e0b" />
            <stop offset="100%" stop-color="#f43f5e" />
          </linearGradient>
        </defs>

        <!-- Base Rounded Plate -->
        <rect
          x="3"
          y="3"
          width="42"
          height="42"
          rx="12"
          :fill="`url(#watzem-bg-${size})`"
          class="mark-base"
        />

        <!-- Glass Sheen Corner Glow -->
        <rect
          x="3.5"
          y="3.5"
          width="41"
          height="41"
          rx="11.5"
          stroke="rgba(255, 255, 255, 0.35)"
          stroke-width="1"
        />

        <!-- Futuristic 'W' + Cinematic Play Chevron Motif -->
        <!-- Left Wing of W -->
        <path
          d="M12 16L16.5 30L20 19L23 23L21.5 28"
          stroke="white"
          stroke-width="3"
          stroke-linecap="round"
          stroke-linejoin="round"
          class="w-stem"
        />

        <!-- Right Side Play Crest Chevron -->
        <path
          d="M25 18L35 24L25 30Z"
          fill="white"
          class="play-chevron"
        />

        <!-- Radiant Lens Sparkle -->
        <circle cx="34" cy="14" r="2.2" fill="#fff" class="lens-sparkle" />
      </svg>
      <div class="logo-aura"></div>
    </div>

    <!-- Typography Block -->
    <div class="brand-text-col">
      <div class="brand-title font-display">
        <span class="watz-text">WATZ</span><span class="em-text" :class="{ 'is-tv': isTv }">EM</span>
      </div>
      <div v-if="showTagline" class="brand-sub font-mono">
        <span>CINEMA STREAM</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.watzem-brand-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  user-select: none;
}

/* Size Variants */
.size-sm {
  gap: 0.45rem;
}
.size-sm .brand-title {
  font-size: 1.05rem;
  letter-spacing: -0.4px;
}
.size-sm .brand-sub {
  font-size: 0.55rem;
}

.size-md {
  gap: 0.6rem;
}
.size-md .brand-title {
  font-size: 1.25rem;
  letter-spacing: -0.5px;
}
.size-md .brand-sub {
  font-size: 0.65rem;
}

.size-lg {
  gap: 0.85rem;
}
.size-lg .brand-title {
  font-size: 2.2rem;
  letter-spacing: -1px;
}
.size-lg .brand-sub {
  font-size: 0.72rem;
  letter-spacing: 2.5px;
}

.size-xl {
  flex-direction: column;
  gap: 1.2rem;
  text-align: center;
}
.size-xl .brand-title {
  font-size: 3.2rem;
  letter-spacing: -1.5px;
}
.size-xl .brand-sub {
  font-size: 0.82rem;
  letter-spacing: 3.5px;
  margin-top: 0.25rem;
}

/* Logo Mark */
.logo-mark {
  position: relative;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.logo-svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.45));
}

.logo-aura {
  position: absolute;
  inset: -4px;
  border-radius: 16px;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.45) 0%, transparent 70%);
  z-index: -1;
  opacity: 0.8;
  filter: blur(8px);
  transition: all 0.3s ease;
}

.is-tv .logo-aura {
  background: radial-gradient(circle, rgba(244, 63, 94, 0.5) 0%, transparent 70%);
}

/* Typography */
.brand-text-col {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-weight: 800;
  line-height: 1;
  color: #ffffff;
  display: flex;
  align-items: center;
}

.watz-text {
  color: #ffffff;
}

.em-text {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 60%, #ea580c 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  transition: all 0.3s ease;
}

.em-text.is-tv {
  background: linear-gradient(135deg, #fb7185 0%, #f43f5e 60%, #be123c 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.brand-sub {
  font-weight: 700;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  margin-top: 2px;
}

/* Animations when is-animated */
.is-animated .logo-mark {
  animation: logoBreath 3s ease-in-out infinite alternate;
}

.is-animated .lens-sparkle {
  animation: sparkleGlint 2.2s infinite ease-in-out;
}

.is-animated .em-text {
  background-size: 200% auto;
  animation: emShimmer 3s linear infinite;
}

@keyframes logoBreath {
  0% {
    transform: scale(1);
  }
  100% {
    transform: scale(1.05);
  }
}

@keyframes sparkleGlint {
  0%, 100% {
    opacity: 0.6;
    transform: scale(0.9);
  }
  50% {
    opacity: 1;
    transform: scale(1.25);
  }
}

@keyframes emShimmer {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}
</style>
