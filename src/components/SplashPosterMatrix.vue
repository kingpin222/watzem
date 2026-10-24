<script setup lang="ts">
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
    img.src = 'https://image.tmdb.org/t/p/w342/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg';
  }
};

defineProps<{
  spotlight?: 'amber' | 'rose' | 'emerald' | 'subtle';
  dimLevel?: 'medium' | 'deep';
}>();
</script>

<template>
  <div class="poster-matrix-bg" :class="[dimLevel || 'medium', `spotlight-${spotlight || 'amber'}`]" aria-hidden="true">
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
  </div>
</template>

<style scoped>
.poster-matrix-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  background: #07070a;
  overflow: hidden;
  pointer-events: none;
  user-select: none;
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
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.7);
  transform: translateZ(0);
}

@media (min-width: 768px) {
  .poster-tile {
    width: 135px;
    height: 202px;
  }
}

.poster-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: brightness(0.8) contrast(1.05);
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
    rgba(7, 7, 10, 0.72) 0%,
    rgba(7, 7, 10, 0.88) 55%,
    #07070a 95%
  );
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  pointer-events: none;
}

.deep .cinematic-vignette {
  background: radial-gradient(
    circle at center,
    rgba(7, 7, 10, 0.82) 0%,
    rgba(7, 7, 10, 0.94) 55%,
    #07070a 98%
  );
  backdrop-filter: blur(7px);
  -webkit-backdrop-filter: blur(7px);
}

.center-spotlight-pulse {
  position: absolute;
  top: 30%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 520px;
  height: 520px;
  border-radius: 50%;
  filter: blur(55px);
  animation: spotlightPulse 4s ease-in-out infinite alternate;
  pointer-events: none;
}

.spotlight-amber .center-spotlight-pulse {
  background: radial-gradient(
    circle,
    rgba(245, 158, 11, 0.22) 0%,
    rgba(244, 63, 94, 0.12) 45%,
    transparent 70%
  );
}

.spotlight-emerald .center-spotlight-pulse {
  background: radial-gradient(
    circle,
    rgba(16, 185, 129, 0.22) 0%,
    rgba(59, 130, 246, 0.12) 45%,
    transparent 70%
  );
}

.spotlight-rose .center-spotlight-pulse {
  background: radial-gradient(
    circle,
    rgba(244, 63, 94, 0.22) 0%,
    rgba(168, 85, 247, 0.12) 45%,
    transparent 70%
  );
}

.spotlight-subtle .center-spotlight-pulse {
  background: radial-gradient(
    circle,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(120, 119, 198, 0.06) 45%,
    transparent 70%
  );
}

@keyframes spotlightPulse {
  0% {
    transform: translate(-50%, -50%) scale(0.85);
    opacity: 0.6;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.15);
    opacity: 1;
  }
}
</style>
