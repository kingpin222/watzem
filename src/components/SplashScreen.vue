<script setup lang="ts">
import { ref, onMounted } from 'vue';
import WatzemLogo from './WatzemLogo.vue';

const emit = defineEmits<{
  (e: 'finish'): void;
}>();

const isDismissing = ref(false);
const progress = ref(0);

// Curated verified 100% working high-res poster artworks for the animated matrix rows
const row1Posters = [
  { title: 'Dune: Part Two', path: '/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg' },
  { title: 'Oppenheimer', path: '/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg' },
  { title: 'Avengers: Endgame', path: '/or06FN3Dka5tukK1e9sl16pB3iy.jpg' },
  { title: 'The Dark Knight', path: '/qJ2tW6WMUDux911r6m7haRef0WH.jpg' },
  { title: 'Spider-Man: No Way Home', path: '/1g0dhYtq4irTY1GPXvft6k4YLjm.jpg' },
  { title: 'Interstellar', path: '/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg' },
  { title: 'Inception', path: '/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg' },
  { title: 'Avatar: The Way of Water', path: '/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg' },
];

const row2Posters = [
  { title: 'Gladiator II', path: '/2cxhvwyEwRlysAmRH4iodkvo0z5.jpg' },
  { title: 'Deadpool & Wolverine', path: '/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg' },
  { title: 'The Batman', path: '/74xTEgt7R36Fpooo50r9T25onhq.jpg' },
  { title: 'Top Gun: Maverick', path: '/62HCnUTziyWcpDaBO2i1DX17ljH.jpg' },
  { title: 'Blade Runner 2049', path: '/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg' },
  { title: 'Dune', path: '/d5NXSklXo0qyIYkgV94XAgMIckC.jpg' },
  { title: 'Fight Club', path: '/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg' },
  { title: 'Pulp Fiction', path: '/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg' },
];

const row3Posters = [
  { title: 'Spider-Man: Across the Spider-Verse', path: '/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg' },
  { title: 'Stranger Things', path: '/49WJfeN0moxb9IPfGn8AIqMGskD.jpg' },
  { title: 'Breaking Bad', path: '/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg' },
  { title: 'Game of Thrones', path: '/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg' },
  { title: 'Avengers: Infinity War', path: '/7WsyChQLEftFiDOVTGkv3hFpyyt.jpg' },
  { title: 'House of the Dragon', path: '/7QMsOTMUswlwxJP0rTTZfmz2tX2.jpg' },
  { title: 'Lord of the Rings', path: '/rCzpDGLbOoPwLjy3OAm5NUPOTrC.jpg' },
  { title: 'Star Wars', path: '/6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg' },
];

const row4Posters = [
  { title: 'Fast & Furious', path: '/fiVW06jE7z9YnO4trhaMEdclSiC.jpg' },
  { title: 'The Matrix', path: '/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg' },
  { title: 'Mad Max: Fury Road', path: '/hA2ple9q4qnwxp3hKVNhroipsir.jpg' },
  { title: 'Forrest Gump', path: '/saHP97rTPS5eLmrLQEcANmKrsFl.jpg' },
  { title: 'Joker', path: '/udDclJoHjfjb8Ekgsd4FDteOkCU.jpg' },
  { title: 'The Godfather', path: '/3bhkrj58Vtu7enYsRolD1fZdja1.jpg' },
  { title: 'Whiplash', path: '/7fn624j5lj3xTme2SgiLCeuedmO.jpg' },
  { title: 'The Shawshank Redemption', path: '/9cqNxx0GxF0bflZmeSMuL5tnGzr.jpg' },
];

const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement;
  if (img && !img.dataset.failed) {
    img.dataset.failed = 'true';
    // Fallback to verified Dune 2 poster
    img.src = 'https://image.tmdb.org/t/p/w342/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg';
  }
};

const dismiss = () => {
  if (isDismissing.value) return;
  isDismissing.value = true;
  setTimeout(() => {
    emit('finish');
  }, 450);
};

onMounted(() => {
  // Animate progress bar across 2.2 seconds
  const startTime = Date.now();
  const duration = 2200;

  const interval = setInterval(() => {
    const elapsed = Date.now() - startTime;
    const p = Math.min(100, Math.round((elapsed / duration) * 100));
    progress.value = p;

    if (elapsed >= duration) {
      clearInterval(interval);
      dismiss();
    }
  }, 35);
});
</script>

<template>
  <div
    class="splash-screen-container"
    :class="{ 'is-dismissing': isDismissing }"
    @click="dismiss"
  >
    <!-- Background Animated Tilted Posters Matrix -->
    <div class="posters-matrix-viewport">
      <div class="posters-matrix-stage">
        <!-- Row 1 (Scroll Left) -->
        <div class="matrix-row scroll-left-fast">
          <div class="matrix-track">
            <div
              v-for="(movie, idx) in [...row1Posters, ...row1Posters]"
              :key="`r1-${idx}`"
              class="poster-tile"
            >
              <img
                :src="`https://image.tmdb.org/t/p/w342${movie.path}`"
                :alt="movie.title"
                loading="eager"
                class="poster-img"
                @error="handleImageError"
              />
            </div>
          </div>
        </div>

        <!-- Row 2 (Scroll Right) -->
        <div class="matrix-row scroll-right-medium">
          <div class="matrix-track">
            <div
              v-for="(movie, idx) in [...row2Posters, ...row2Posters]"
              :key="`r2-${idx}`"
              class="poster-tile"
            >
              <img
                :src="`https://image.tmdb.org/t/p/w342${movie.path}`"
                :alt="movie.title"
                loading="eager"
                class="poster-img"
                @error="handleImageError"
              />
            </div>
          </div>
        </div>

        <!-- Row 3 (Scroll Left) -->
        <div class="matrix-row scroll-left-slow">
          <div class="matrix-track">
            <div
              v-for="(movie, idx) in [...row3Posters, ...row3Posters]"
              :key="`r3-${idx}`"
              class="poster-tile"
            >
              <img
                :src="`https://image.tmdb.org/t/p/w342${movie.path}`"
                :alt="movie.title"
                loading="eager"
                class="poster-img"
                @error="handleImageError"
              />
            </div>
          </div>
        </div>

        <!-- Row 4 (Scroll Right) -->
        <div class="matrix-row scroll-right-fast">
          <div class="matrix-track">
            <div
              v-for="(movie, idx) in [...row4Posters, ...row4Posters]"
              :key="`r4-${idx}`"
              class="poster-tile"
            >
              <img
                :src="`https://image.tmdb.org/t/p/w342${movie.path}`"
                :alt="movie.title"
                loading="eager"
                class="poster-img"
                @error="handleImageError"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Cinematic Vignette & Ambient Radial Glow Overlays -->
    <div class="cinematic-vignette"></div>
    <div class="center-spotlight-pulse"></div>

    <!-- Centerpiece Brand Stage -->
    <div class="splash-center-content">
      <div class="logo-hero-cluster">
        <WatzemLogo size="xl" animated show-tagline />
      </div>

      <!-- High-tech Progress Bar & Status -->
      <div class="splash-bottom-bar">
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: `${progress}%` }">
            <span class="progress-spark"></span>
          </div>
        </div>

        <div class="status-row font-mono">
          <span class="status-msg">UNLIMITED MOVIES & TV</span>
          <span class="status-pct">{{ progress }}%</span>
        </div>

        <span class="tap-hint font-mono">Tap anywhere to start</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.splash-screen-container {
  position: fixed;
  inset: 0;
  z-index: 999999;
  background: #07070a;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  user-select: none;
  cursor: pointer;
  transition: opacity 0.45s cubic-bezier(0.16, 1, 0.3, 1), transform 0.45s cubic-bezier(0.16, 1, 0.3, 1);
}

.splash-screen-container.is-dismissing {
  opacity: 0;
  transform: scale(1.06);
  pointer-events: none;
}

/* Tilted Matrix of Posters */
.posters-matrix-viewport {
  position: absolute;
  inset: -50% -30%;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.posters-matrix-stage {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  width: 180%;
  transform: rotate(-10deg) scale(1.15);
  transform-origin: center center;
}

.matrix-row {
  display: flex;
  overflow: hidden;
  width: 100%;
}

.matrix-track {
  display: flex;
  gap: 1.1rem;
  width: max-content;
  will-change: transform;
}

/* Individual Poster Tile */
.poster-tile {
  width: 110px;
  height: 165px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  background: #14141e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.7);
  transform: translateZ(0);
}

@media (min-width: 768px) {
  .poster-tile {
    width: 140px;
    height: 210px;
  }
}

.poster-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: brightness(0.85) contrast(1.05);
}

/* Marquee Scrolling Animations */
.scroll-left-fast .matrix-track {
  animation: marqueeLeft 38s linear infinite;
}

.scroll-right-medium .matrix-track {
  animation: marqueeRight 48s linear infinite;
}

.scroll-left-slow .matrix-track {
  animation: marqueeLeft 58s linear infinite;
}

.scroll-right-fast .matrix-track {
  animation: marqueeRight 42s linear infinite;
}

@keyframes marqueeLeft {
  0% {
    transform: translate3d(0, 0, 0);
  }
  100% {
    transform: translate3d(-50%, 0, 0);
  }
}

@keyframes marqueeRight {
  0% {
    transform: translate3d(-50%, 0, 0);
  }
  100% {
    transform: translate3d(0, 0, 0);
  }
}

/* Vignette & Ambient Radial Lighting */
.cinematic-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    circle at center,
    rgba(7, 7, 10, 0.7) 0%,
    rgba(7, 7, 10, 0.88) 55%,
    #07070a 95%
  );
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  pointer-events: none;
}

.center-spotlight-pulse {
  position: absolute;
  width: 460px;
  height: 460px;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(245, 158, 11, 0.28) 0%,
    rgba(244, 63, 94, 0.15) 45%,
    transparent 70%
  );
  filter: blur(45px);
  animation: spotlightPulse 3s ease-in-out infinite alternate;
  pointer-events: none;
}

@keyframes spotlightPulse {
  0% {
    transform: scale(0.85);
    opacity: 0.6;
  }
  100% {
    transform: scale(1.15);
    opacity: 1;
  }
}

/* Center Content & Branding */
.splash-center-content {
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2.8rem;
  padding: 2rem;
  max-width: 480px;
  width: 100%;
}

.logo-hero-cluster {
  animation: heroLogoEntrance 1.1s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes heroLogoEntrance {
  0% {
    opacity: 0;
    transform: scale(0.82) translateY(18px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* Progress & Status Bar */
.splash-bottom-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  max-width: 260px;
  animation: barFadeIn 0.8s 0.2s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes barFadeIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.progress-track {
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 9999px;
  overflow: hidden;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #f59e0b, #f43f5e);
  border-radius: 9999px;
  position: relative;
  transition: width 0.05s linear;
  box-shadow: 0 0 12px rgba(245, 158, 11, 0.8);
}

.progress-spark {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  background: #ffffff;
  border-radius: 50%;
  box-shadow: 0 0 8px #ffffff, 0 0 16px #f59e0b;
}

.status-row {
  display: flex;
  justify-content: space-between;
  width: 100%;
  font-size: 0.65rem;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 1px;
}

.status-msg {
  font-weight: 700;
}

.status-pct {
  color: var(--accent-amber);
  font-weight: 700;
}

.tap-hint {
  font-size: 0.6rem;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  margin-top: 0.5rem;
  animation: pulseHint 2s infinite ease-in-out;
}

@keyframes pulseHint {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.8;
  }
}
</style>
