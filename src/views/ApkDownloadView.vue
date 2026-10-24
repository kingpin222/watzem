<script setup lang="ts">
import { ref } from 'vue';
import { useHaptics } from '../composables/useHaptics';
import WatzemLogo from '../components/WatzemLogo.vue';
import {
  Download,
  Smartphone,
  ShieldCheck,
  CheckCircle2,
  Tv,
  Zap,
  Sparkles
} from 'lucide-vue-next';

import { openExternalUrl } from '../composables/useMonetization';

const { triggerMedium } = useHaptics();

const isDownloading = ref(false);
const downloadUrl = 'https://github.com/kingpin222/watzem/releases/latest/download/app-release.apk';

const handleDownload = async () => {
  triggerMedium();
  isDownloading.value = true;
  await openExternalUrl(downloadUrl);
  setTimeout(() => {
    isDownloading.value = false;
  }, 3500);
};

// Curated high-res poster artworks for the animated loading background matrix
const row1Posters = [
  { title: 'Dune: Part Two', path: '/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg' },
  { title: 'Oppenheimer', path: '/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg' },
  { title: 'Avengers: Endgame', path: '/or06FN3Dka5tukK1e9sl16pB3iy.jpg' },
  { title: 'The Dark Knight', path: '/qJ2tW6WMUDux911r6m7haRef0WH.jpg' },
  { title: 'Spider-Man: No Way Home', path: '/1g0dhYtq4irTY1GPXvft6k4YLjm.jpg' },
  { title: 'Interstellar', path: '/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg' },
];

const row2Posters = [
  { title: 'Gladiator II', path: '/2cxhvwyEwRlysAmRH4iodkvo0z5.jpg' },
  { title: 'Deadpool & Wolverine', path: '/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg' },
  { title: 'The Batman', path: '/74xTEgt7R36Fpooo50r9T25onhq.jpg' },
  { title: 'Top Gun: Maverick', path: '/62HCnUTziyWcpDaBO2i1DX17ljH.jpg' },
  { title: 'Blade Runner 2049', path: '/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg' },
  { title: 'Dune', path: '/d5NXSklXo0qyIYkgV94XAgMIckC.jpg' },
];

const row3Posters = [
  { title: 'Spider-Man: Across the Spider-Verse', path: '/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg' },
  { title: 'Stranger Things', path: '/49WJfeN0moxb9IPfGn8AIqMGskD.jpg' },
  { title: 'Breaking Bad', path: '/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg' },
  { title: 'Game of Thrones', path: '/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg' },
  { title: 'Avengers: Infinity War', path: '/7WsyChQLEftFiDOVTGkv3hFpyyt.jpg' },
  { title: 'House of the Dragon', path: '/7QMsOTMUswlwxJP0rTTZfmz2tX2.jpg' },
];

const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement;
  if (img && !img.dataset.failed) {
    img.dataset.failed = 'true';
    img.src = 'https://image.tmdb.org/t/p/w342/8b8R8l88Qje9dn9OE8PY05Nxl1X.jpg';
  }
};
</script>

<template>
  <div class="apk-download-page">
    <!-- Animated Loading Background: Scrolling Movie Matrix -->
    <div class="matrix-background-layer" aria-hidden="true">
      <div class="matrix-stage">
        <div class="matrix-row scroll-left">
          <div class="matrix-track">
            <div
              v-for="(movie, idx) in [...row1Posters, ...row1Posters]"
              :key="`r1-${idx}`"
              class="poster-card"
            >
              <img
                :src="`https://image.tmdb.org/t/p/w342${movie.path}`"
                :alt="movie.title"
                class="poster-img"
                @error="handleImageError"
              />
            </div>
          </div>
        </div>

        <div class="matrix-row scroll-right">
          <div class="matrix-track">
            <div
              v-for="(movie, idx) in [...row2Posters, ...row2Posters]"
              :key="`r2-${idx}`"
              class="poster-card"
            >
              <img
                :src="`https://image.tmdb.org/t/p/w342${movie.path}`"
                :alt="movie.title"
                class="poster-img"
                @error="handleImageError"
              />
            </div>
          </div>
        </div>

        <div class="matrix-row scroll-left-slow">
          <div class="matrix-track">
            <div
              v-for="(movie, idx) in [...row3Posters, ...row3Posters]"
              :key="`r3-${idx}`"
              class="poster-card"
            >
              <img
                :src="`https://image.tmdb.org/t/p/w342${movie.path}`"
                :alt="movie.title"
                class="poster-img"
                @error="handleImageError"
              />
            </div>
          </div>
        </div>
      </div>

      <div class="matrix-vignette-overlay"></div>
      <div class="ambient-glow-center"></div>
    </div>

    <!-- Clean Compact Content -->
    <div class="page-content-wrapper">
      <!-- Minimalist Header -->
      <header class="download-nav-header">
        <WatzemLogo size="sm" mode="movie" />
      </header>

      <!-- Compact Glass Showcase Card -->
      <main class="compact-hero-card">
        <!-- App Icon -->
        <div class="app-icon-wrap">
          <div class="icon-glow"></div>
          <img src="/icon-192.png" alt="WATZEM" class="app-icon-img" />
          <span class="icon-status-dot">
            <Smartphone :size="10" />
          </span>
        </div>

        <!-- Headline & Clean One-Liner Subtitle -->
        <h1 class="hero-title">WATZEM <span class="gradient-text">Android</span></h1>
        <p class="hero-subtext">Install the official APK to stream on Android</p>

        <!-- Compact Specs Row -->
        <div class="compact-specs">
          <span class="spec-chip"><CheckCircle2 :size="11" /> v1.0</span>
          <span class="spec-chip"><CheckCircle2 :size="11" /> 7.3 MB</span>
          <span class="spec-chip"><CheckCircle2 :size="11" /> Android 7+</span>
          <span class="spec-chip"><ShieldCheck :size="11" /> Verified</span>
        </div>

        <!-- Download Action -->
        <div class="action-wrap">
          <a
            :href="downloadUrl"
            class="compact-download-btn"
            :class="{ loading: isDownloading }"
            download="WATZEM.apk"
            @click="handleDownload"
          >
            <Download :size="18" class="dl-icon" />
            <span class="dl-text">{{ isDownloading ? 'Downloading APK...' : 'Download APK (7.3 MB)' }}</span>
            <span class="shimmer-sweep"></span>
          </a>
        </div>

        <!-- Clean 3-Point Highlights -->
        <div class="compact-features-grid">
          <div class="feature-item">
            <span class="item-icon yellow"><Zap :size="14" /></span>
            <span class="item-label">Ad-Free Native Player</span>
          </div>
          <div class="feature-item">
            <span class="item-icon amber"><Tv :size="14" /></span>
            <span class="item-label">PiP & Background Play</span>
          </div>
          <div class="feature-item">
            <span class="item-icon gold"><Sparkles :size="14" /></span>
            <span class="item-label">Ultra-Fast 1080p / 4K</span>
          </div>
        </div>

        <!-- Compact 3-Step Install Flow -->
        <div class="compact-steps-box">
          <div class="step-cell">
            <span class="step-index">1</span>
            <span class="step-text">Download APK</span>
          </div>
          <span class="step-arrow">→</span>
          <div class="step-cell">
            <span class="step-index">2</span>
            <span class="step-text">Open File</span>
          </div>
          <span class="step-arrow">→</span>
          <div class="step-cell">
            <span class="step-index">3</span>
            <span class="step-text">Install</span>
          </div>
        </div>

        <!-- Safe Footer Note -->
        <div class="safe-note">
          <ShieldCheck :size="11" />
          <span>Safe & virus-free • No special permissions required</span>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.apk-download-page {
  position: relative;
  min-height: 100vh;
  width: 100%;
  background: radial-gradient(circle at 50% 12%, #383014 0%, #221e14 45%, #141310 100%);
  color: #ffffff;
  overflow-x: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

/* Background Matrix */
.matrix-background-layer {
  position: fixed;
  inset: 0;
  overflow: hidden;
  z-index: 1;
  pointer-events: none;
}

.matrix-stage {
  position: absolute;
  top: -25%;
  left: -30%;
  width: 160%;
  height: 150%;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  transform: rotate(-9deg) scale(1.05);
  transform-origin: center center;
}

.matrix-row {
  display: flex;
  overflow: hidden;
  width: 100%;
}

.matrix-track {
  display: flex;
  gap: 0.85rem;
  width: max-content;
  will-change: transform;
}

.poster-card {
  width: 95px;
  height: 142px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: #232014;
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
}

.poster-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: brightness(0.92) contrast(1.05);
}

.scroll-left .matrix-track {
  animation: marqueeLeft 36s linear infinite;
}

.scroll-right .matrix-track {
  animation: marqueeRight 44s linear infinite;
}

.scroll-left-slow .matrix-track {
  animation: marqueeLeft 52s linear infinite;
}

@keyframes marqueeLeft {
  0% { transform: translate3d(0, 0, 0); }
  100% { transform: translate3d(-50%, 0, 0); }
}

@keyframes marqueeRight {
  0% { transform: translate3d(-50%, 0, 0); }
  100% { transform: translate3d(0, 0, 0); }
}

.matrix-vignette-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    circle at 50% 30%,
    rgba(56, 48, 20, 0.48) 0%,
    rgba(34, 30, 20, 0.72) 58%,
    rgba(20, 19, 16, 0.88) 100%
  );
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
}

.ambient-glow-center {
  position: absolute;
  top: 38%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 520px;
  height: 520px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(250, 204, 21, 0.35) 0%, rgba(234, 179, 8, 0.15) 40%, transparent 70%);
  filter: blur(65px);
}

/* Page Content Wrapper */
.page-content-wrapper {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 440px;
  display: flex;
  flex-direction: column;
}

/* Minimalist Nav */
.download-nav-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.85rem;
  padding: 0 0.25rem;
}

/* Compact Hero Card */
.compact-hero-card {
  background: rgba(38, 34, 20, 0.82);
  backdrop-filter: blur(28px);
  -webkit-backdrop-filter: blur(28px);
  border: 1px solid rgba(250, 204, 21, 0.35);
  border-radius: 22px;
  padding: 1.6rem 1.35rem;
  box-shadow:
    0 24px 60px -15px rgba(0, 0, 0, 0.5),
    0 0 35px -5px rgba(250, 204, 21, 0.25);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

/* App Icon */
.app-icon-wrap {
  position: relative;
  width: 58px;
  height: 58px;
  margin-bottom: 0.75rem;
}

.app-icon-img {
  width: 100%;
  height: 100%;
  border-radius: 15px;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.25);
  box-shadow: 0 8px 20px rgba(202, 138, 4, 0.3);
  position: relative;
  z-index: 2;
}

.icon-glow {
  position: absolute;
  inset: -6px;
  background: radial-gradient(circle, rgba(250, 204, 21, 0.65) 0%, transparent 70%);
  filter: blur(10px);
  z-index: 1;
}

.icon-status-dot {
  position: absolute;
  bottom: -3px;
  right: -3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #facc15;
  color: #1a1600;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #262214;
  z-index: 3;
}

/* Typography */
.hero-title {
  font-size: 1.35rem;
  font-weight: 850;
  letter-spacing: -0.02em;
  margin: 0 0 0.25rem 0;
  line-height: 1.2;
}

.gradient-text {
  background: linear-gradient(135deg, #fef08a 0%, #facc15 50%, #eab308 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtext {
  font-size: 0.8rem;
  color: #fef9c3;
  margin: 0 0 0.85rem 0;
  line-height: 1.35;
}

/* Specs */
.compact-specs {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  flex-wrap: wrap;
  margin-bottom: 1.1rem;
}

.spec-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.68rem;
  font-weight: 600;
  color: #fef08a;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(250, 204, 21, 0.22);
  padding: 0.18rem 0.5rem;
  border-radius: 6px;
}

.spec-chip svg {
  color: #facc15;
}

/* Action Area */
.action-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1.1rem;
}

.compact-download-btn {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  background: linear-gradient(135deg, #facc15 0%, #eab308 100%);
  color: #1a1600;
  padding: 0.82rem 1.25rem;
  border-radius: 14px;
  text-decoration: none;
  font-size: 0.92rem;
  font-weight: 850;
  letter-spacing: -0.01em;
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow:
    0 8px 24px rgba(234, 179, 8, 0.45),
    inset 0 1px 1px rgba(255, 255, 255, 0.5);
  overflow: hidden;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.compact-download-btn:active {
  transform: scale(0.97);
  box-shadow: 0 4px 12px rgba(234, 179, 8, 0.35);
}

.compact-download-btn.loading {
  opacity: 0.85;
  pointer-events: none;
}

.dl-icon {
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.compact-download-btn:hover .dl-icon {
  transform: translateY(2px);
}

.shimmer-sweep {
  position: absolute;
  top: 0;
  left: -75%;
  width: 50%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  transform: skewX(-20deg);
  animation: shimmer 3.5s infinite;
}

@keyframes shimmer {
  0%, 75% { left: -75%; }
  100% { left: 125%; }
}

/* Compact 3 Highlights */
.compact-features-grid {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(250, 204, 21, 0.18);
  border-radius: 12px;
  padding: 0.65rem 0.85rem;
  margin-bottom: 0.85rem;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}

.item-icon {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-icon.yellow {
  background: rgba(234, 179, 8, 0.22);
  color: #facc15;
}

.item-icon.amber {
  background: rgba(245, 158, 11, 0.22);
  color: #fbbf24;
}

.item-icon.gold {
  background: rgba(202, 138, 4, 0.24);
  color: #fde047;
}

.item-label {
  font-size: 0.76rem;
  font-weight: 600;
  color: #fef9c3;
}

/* Compact 3-Step Flow */
.compact-steps-box {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(20, 18, 12, 0.55);
  border: 1px solid rgba(250, 204, 21, 0.16);
  border-radius: 10px;
  padding: 0.55rem 0.75rem;
  margin-bottom: 0.85rem;
}

.step-cell {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.step-index {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #facc15;
  color: #1a1600;
  font-size: 0.68rem;
  font-weight: 850;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.step-text {
  font-size: 0.7rem;
  font-weight: 600;
  color: #fef9c3;
}

.step-arrow {
  font-size: 0.75rem;
  color: #fef08a;
}

/* Safe Note */
.safe-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  font-size: 0.66rem;
  color: #fef08a;
}

.safe-note svg {
  color: #facc15;
}

@media (orientation: landscape) {
  .apk-download-page {
    padding: 0.75rem max(1.25rem, var(--safe-left, 0px));
    padding-right: max(1.25rem, var(--safe-right, 0px));
    align-items: flex-start;
  }
  .page-content-wrapper {
    max-width: 580px;
    margin: 0.5rem auto;
  }
}
</style>
