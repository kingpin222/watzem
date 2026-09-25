<script setup lang="ts">
import { ref } from 'vue';
import { useAndroidApp } from '../composables/useAndroidApp';
import { useHaptics } from '../composables/useHaptics';
import {
  Download,
  X,
  Smartphone,
  CheckCircle2,
  Tv,
  Zap
} from 'lucide-vue-next';
import WatzemLogo from './WatzemLogo.vue';

const {
  isAndroidWeb,
  shouldShowPrompt,
  dismissPrompt,
  downloadApk,
  getApkDownloadUrl
} = useAndroidApp();

const { triggerLight, triggerMedium } = useHaptics();

const isDownloading = ref(false);

const handleDownload = () => {
  triggerMedium();
  isDownloading.value = true;
  downloadApk();
  setTimeout(() => {
    isDownloading.value = false;
  }, 3000);
};

const handleDismiss = () => {
  triggerLight();
  dismissPrompt();
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
  <!-- Show only if visiting via an Android browser (non-APK) and prompt is active -->
  <Teleport to="body">
    <Transition name="modal-pop">
      <div
        v-if="isAndroidWeb && shouldShowPrompt"
        class="apk-popup-backdrop"
        @click.self="handleDismiss"
      >
        <!-- 80% Screen Modal Container with the Loading Matrix Background -->
        <div class="apk-popup-card" role="dialog" aria-modal="true" aria-label="Download WATZEM Android App">
          
          <!-- Animated Loading Background: Scrolling Movie Posters Matrix -->
          <div class="loading-matrix-bg" aria-hidden="true">
            <div class="matrix-stage">
              <!-- Row 1 (Scroll Left) -->
              <div class="matrix-row scroll-left">
                <div class="matrix-track">
                  <div
                    v-for="(movie, idx) in [...row1Posters, ...row1Posters]"
                    :key="`r1-${idx}`"
                    class="poster-cell"
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

              <!-- Row 2 (Scroll Right) -->
              <div class="matrix-row scroll-right">
                <div class="matrix-track">
                  <div
                    v-for="(movie, idx) in [...row2Posters, ...row2Posters]"
                    :key="`r2-${idx}`"
                    class="poster-cell"
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

              <!-- Row 3 (Scroll Left) -->
              <div class="matrix-row scroll-left-slow">
                <div class="matrix-track">
                  <div
                    v-for="(movie, idx) in [...row3Posters, ...row3Posters]"
                    :key="`r3-${idx}`"
                    class="poster-cell"
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

            <!-- Deep Cinematic Vignette Overlays -->
            <div class="matrix-vignette"></div>
            <div class="matrix-spotlight"></div>
          </div>

          <!-- Foreground Popup Content -->
          <div class="popup-inner-content">
            <!-- Top Bar: Badge & Dismiss Button -->
            <div class="popup-header-bar">
              <div class="android-tag-pill">
                <Smartphone :size="12" />
                <span>Android App</span>
              </div>
              <button
                class="popup-close-btn"
                aria-label="Close"
                title="Continue in browser"
                @click="handleDismiss"
              >
                <X :size="18" />
              </button>
            </div>

            <!-- Center Showcase -->
            <div class="popup-center-body">
              <div class="brand-avatar-wrap">
                <div class="brand-avatar-glow"></div>
                <img src="/icon-192.png" alt="WATZEM" class="brand-avatar-img" />
                <span class="android-active-badge">
                  <Smartphone :size="11" />
                </span>
              </div>

              <div class="logo-wrap">
                <WatzemLogo size="md" mode="movie" />
              </div>

              <h2 class="popup-headline">Experience WATZEM at its Best</h2>
              <p class="popup-subtext">
                Install our official Android app for a smoother, ad-free cinema experience.
              </p>

              <!-- Feature Highlights -->
              <div class="feature-bullets-box">
                <div class="feature-row">
                  <span class="feature-icon-dot"><Zap :size="13" /></span>
                  <span class="feature-text">Ad-Free Native Video Player</span>
                </div>
                <div class="feature-row">
                  <span class="feature-icon-dot"><Tv :size="13" /></span>
                  <span class="feature-text">Picture-in-Picture & Background Play</span>
                </div>
                <div class="feature-row">
                  <span class="feature-icon-dot"><CheckCircle2 :size="13" /></span>
                  <span class="feature-text">Faster 1080p & 4K Streaming</span>
                </div>
              </div>
            </div>

            <!-- Bottom CTA Section -->
            <div class="popup-footer-actions">
              <a
                :href="getApkDownloadUrl()"
                class="download-cta-btn"
                :class="{ loading: isDownloading }"
                download="WATZEM.apk"
                @click="handleDownload"
              >
                <Download :size="17" class="btn-dl-icon" />
                <span class="btn-label">{{ isDownloading ? 'Starting Download...' : 'Download APK (3.9 MB)' }}</span>
                <span class="btn-shimmer"></span>
              </a>

              <button class="continue-web-btn" @click="handleDismiss">
                Continue to Web Version
              </button>

              <div class="security-caption">
                Free & safe APK • Direct install for Android 7.0+
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* Fullscreen Backdrop */
.apk-popup-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(3, 4, 7, 0.85);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

/* 80% Screen Modal Card */
.apk-popup-card {
  position: relative;
  width: min(88vw, 440px);
  height: 80vh;
  max-height: 720px;
  min-height: 520px;
  border-radius: 26px;
  overflow: hidden;
  background: #08090e;
  border: 1px solid rgba(99, 184, 188, 0.35);
  box-shadow:
    0 25px 60px -15px rgba(0, 0, 0, 0.95),
    0 0 35px -5px rgba(99, 184, 188, 0.22);
  display: flex;
  flex-direction: column;
}

/* Background Loading Matrix (Movie Posters Animated Scrolling) */
.loading-matrix-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 1;
}

.matrix-stage {
  position: absolute;
  top: -30%;
  left: -40%;
  width: 180%;
  height: 160%;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  transform: rotate(-10deg) scale(1.08);
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

.poster-cell {
  width: 95px;
  height: 142px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: #12131c;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.7);
}

.poster-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: brightness(0.65) contrast(1.1);
}

/* Animations */
.scroll-left .matrix-track {
  animation: marqueeLeft 32s linear infinite;
}

.scroll-right .matrix-track {
  animation: marqueeRight 40s linear infinite;
}

.scroll-left-slow .matrix-track {
  animation: marqueeLeft 48s linear infinite;
}

@keyframes marqueeLeft {
  0% { transform: translate3d(0, 0, 0); }
  100% { transform: translate3d(-50%, 0, 0); }
}

@keyframes marqueeRight {
  0% { transform: translate3d(-50%, 0, 0); }
  100% { transform: translate3d(0, 0, 0); }
}

/* Vignette & Radial Spotlight Overlays */
.matrix-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    circle at center,
    rgba(8, 9, 14, 0.82) 0%,
    rgba(8, 9, 14, 0.94) 55%,
    #08090e 100%
  );
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
}

.matrix-spotlight {
  position: absolute;
  top: 15%;
  left: 50%;
  transform: translateX(-50%);
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(99, 184, 188, 0.22) 0%,
    rgba(245, 158, 11, 0.12) 45%,
    transparent 70%
  );
  filter: blur(35px);
  pointer-events: none;
}

/* Foreground Content */
.popup-inner-content {
  position: relative;
  z-index: 5;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 1.25rem 1.4rem;
}

/* Header Bar */
.popup-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.android-tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: rgba(61, 220, 132, 0.15);
  border: 1px solid rgba(61, 220, 132, 0.35);
  color: #3ddc84;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.22rem 0.6rem;
  border-radius: 999px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.popup-close-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #9ca3af;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.popup-close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

/* Center Showcase */
.popup-center-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 0.5rem 0;
}

.brand-avatar-wrap {
  position: relative;
  width: 64px;
  height: 64px;
  margin-bottom: 0.75rem;
}

.brand-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 16px;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.6);
  position: relative;
  z-index: 2;
}

.brand-avatar-glow {
  position: absolute;
  inset: -6px;
  background: radial-gradient(circle, rgba(99, 184, 188, 0.6) 0%, transparent 70%);
  filter: blur(10px);
  z-index: 1;
}

.android-active-badge {
  position: absolute;
  bottom: -4px;
  right: -4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #3ddc84;
  color: #07070a;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #08090e;
  z-index: 3;
}

.logo-wrap {
  margin-bottom: 0.4rem;
}

.popup-headline {
  font-size: 1.15rem;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.02em;
  margin: 0.1rem 0 0.35rem 0;
}

.popup-subtext {
  font-size: 0.78rem;
  color: #9ca3af;
  line-height: 1.4;
  margin: 0 0 1rem 0;
  max-width: 280px;
}

/* Feature Bullets Card */
.feature-bullets-box {
  width: 100%;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 0.75rem 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  backdrop-filter: blur(10px);
}

.feature-row {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  text-align: left;
}

.feature-icon-dot {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: rgba(99, 184, 188, 0.16);
  color: #63b8bc;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-text {
  font-size: 0.78rem;
  font-weight: 600;
  color: #e5e7eb;
}

/* Footer Actions */
.popup-footer-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.6rem;
  padding-top: 0.5rem;
}

.download-cta-btn {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  background: linear-gradient(135deg, #63b8bc 0%, #357c7f 100%);
  color: #07070a;
  font-size: 0.9rem;
  font-weight: 800;
  padding: 0.85rem 1.25rem;
  border-radius: 14px;
  text-decoration: none;
  box-shadow:
    0 8px 24px rgba(99, 184, 188, 0.4),
    inset 0 1px 1px rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.25);
  overflow: hidden;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.download-cta-btn:active {
  transform: scale(0.97);
  box-shadow: 0 4px 12px rgba(99, 184, 188, 0.3);
}

.download-cta-btn.loading {
  opacity: 0.85;
  pointer-events: none;
}

.btn-dl-icon {
  transition: transform 0.2s ease;
}

.download-cta-btn:hover .btn-dl-icon {
  transform: translateY(2px);
}

.btn-shimmer {
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

.continue-web-btn {
  background: transparent;
  border: none;
  color: #9ca3af;
  font-size: 0.78rem;
  font-weight: 500;
  cursor: pointer;
  padding: 0.35rem 0.75rem;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.continue-web-btn:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.06);
}

.security-caption {
  font-size: 0.68rem;
  color: #6b7280;
  text-align: center;
}

/* Modal Pop Transitions */
.modal-pop-enter-active,
.modal-pop-leave-active {
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-pop-enter-from,
.modal-pop-leave-to {
  opacity: 0;
}

.modal-pop-enter-from .apk-popup-card,
.modal-pop-leave-to .apk-popup-card {
  transform: scale(0.92) translateY(24px);
  opacity: 0;
}

.modal-pop-enter-active .apk-popup-card,
.modal-pop-leave-active .apk-popup-card {
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
</style>
