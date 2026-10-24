<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useAppUpdate } from '../composables/useAppUpdate';
import { useHaptics } from '../composables/useHaptics';
import {
  Download,
  Sparkles,
  CheckCircle2,
  ShieldCheck,
  ArrowLeft,
  ArrowRight,
  RefreshCw,
  HardDrive,
  Calendar,
  ExternalLink,
  Check
} from 'lucide-vue-next';

const emit = defineEmits<{
  (e: 'back'): void;
}>();

const {
  updateInfo,
  updateAvailable,
  isChecking,
  isDownloading,
  downloadProgress,
  downloadBytesText,
  downloadStatusText,
  currentVersion,
  currentBuild,
  checkForUpdate,
  startUpdate,
  dismissModal
} = useAppUpdate();

const { triggerLight, triggerMedium, triggerSuccess } = useHaptics();

const isManualChecking = ref(false);
const checkSuccessMessage = ref('');

const handleUpdate = () => {
  triggerMedium();
  startUpdate();
};

const handleBack = () => {
  triggerLight();
  dismissModal();
  emit('back');
};

const handleManualCheck = async () => {
  triggerLight();
  isManualChecking.value = true;
  checkSuccessMessage.value = '';
  try {
    await checkForUpdate({ manual: true, force: true });
    if (!updateAvailable.value) {
      triggerSuccess();
      checkSuccessMessage.value = 'WATZEM is up to date!';
      setTimeout(() => {
        checkSuccessMessage.value = '';
      }, 4000);
    }
  } finally {
    isManualChecking.value = false;
  }
};

onMounted(() => {
  // If no update info yet, check automatically on page load
  if (!updateInfo.value) {
    checkForUpdate();
  }
});
</script>

<template>
  <div class="update-page">
    <!-- Top Bar with Back Button -->
    <header class="update-header-bar">
      <button 
        class="btn-back"
        data-nav-item="true"
        tabindex="0"
        title="Back"
        @click="handleBack"
      >
        <ArrowLeft :size="18" />
        <span>Back</span>
      </button>

      <div class="header-badge" :class="{ 'has-update': updateAvailable }">
        <Sparkles v-if="updateAvailable" :size="14" class="sparkle-anim" />
        <CheckCircle2 v-else :size="14" />
        <span>{{ updateAvailable ? 'Update Available' : 'Up to Date' }}</span>
      </div>
    </header>

    <!-- Main Fluid Content Container -->
    <main class="update-content">
      <!-- Hero Header Card -->
      <section class="update-hero-card" :class="{ 'update-ready': updateAvailable }">
        <div class="hero-glow-orb"></div>
        <div class="hero-inner">
          <div class="hero-icon-bubble">
            <img src="/icon-192.png" alt="WATZEM" class="app-icon-img" />
            <span class="update-badge-icon" v-if="updateAvailable">
              <Sparkles :size="14" />
            </span>
          </div>

          <div class="hero-text">
            <h1 class="hero-title">
              {{ updateAvailable ? (updateInfo?.title || 'New Update Available') : 'WATZEM is Up to Date' }}
            </h1>

            <div class="version-transition-row">
              <span class="v-pill current">v{{ currentVersion }} (Build {{ currentBuild }})</span>
              <template v-if="updateAvailable && updateInfo">
                <ArrowRight :size="14" class="v-arrow" />
                <span class="v-pill latest">v{{ updateInfo.latestVersion }}</span>
                <span v-if="updateInfo.mandatory" class="v-mandatory">Required</span>
              </template>
            </div>

            <p class="hero-desc">
              {{ updateAvailable
                ? 'A new official release is ready for installation. Enjoy improved streaming speeds, bug fixes, and new features.'
                : 'You are currently running the latest official version of WATZEM. No action is required.'
              }}
            </p>
          </div>
        </div>
      </section>

      <!-- Active Update Details (When update is available) -->
      <div v-if="updateAvailable && updateInfo" class="update-main-grid">
        <!-- Release Notes Card -->
        <section class="update-card release-notes-card">
          <div class="card-header">
            <div class="card-icon-wrap notes-icon-wrap">
              <Sparkles :size="22" />
            </div>
            <div>
              <h2 class="card-title">What's New in v{{ updateInfo.latestVersion }}</h2>
              <p class="card-subtitle">Official Changelog & Performance Updates</p>
            </div>
          </div>

          <!-- Specs Strip -->
          <div class="specs-strip">
            <span class="spec-pill" v-if="updateInfo.fileSize">
              <HardDrive :size="13" />
              <span>{{ updateInfo.fileSize }}</span>
            </span>
            <span class="spec-pill">
              <ShieldCheck :size="13" />
              <span>Verified APK</span>
            </span>
            <span class="spec-pill" v-if="updateInfo.releaseDate">
              <Calendar :size="13" />
              <span>{{ updateInfo.releaseDate }}</span>
            </span>
          </div>

          <!-- Notes List -->
          <ul class="notes-list">
            <li
              v-for="(note, index) in updateInfo.releaseNotes"
              :key="index"
              class="note-item"
            >
              <div class="note-bullet-dot">
                <Check :size="12" />
              </div>
              <span class="note-text">{{ note }}</span>
            </li>
          </ul>
        </section>

        <!-- Installation & Action Card -->
        <section class="update-card install-card">
          <div class="card-header">
            <div class="card-icon-wrap download-icon-wrap">
              <Download :size="22" />
            </div>
            <div>
              <h2 class="card-title">Install Update</h2>
              <p class="card-subtitle">Fast, in-place update without losing settings</p>
            </div>
          </div>

          <!-- Progress Bar (Active downloading state) -->
          <div v-if="isDownloading" class="download-progress-wrap">
            <div class="progress-labels">
              <span class="progress-status">{{ downloadStatusText || 'Downloading update...' }}</span>
              <span class="progress-pct font-mono">{{ downloadProgress }}%</span>
            </div>
            <div class="progress-track">
              <div
                class="progress-bar-fill"
                :style="{ width: `${Math.max(4, downloadProgress)}%` }"
              >
                <span class="progress-glow-dot"></span>
              </div>
            </div>
            <div v-if="downloadBytesText" class="progress-subtext font-mono">
              {{ downloadBytesText }}
            </div>
          </div>

          <!-- Actions -->
          <div v-else class="action-buttons-wrap">
            <button
              class="action-btn update-now-btn"
              data-nav-item="true"
              tabindex="0"
              @click="handleUpdate"
            >
              <Download :size="18" class="bounce-anim" />
              <span>{{ updateInfo.mandatory ? 'Install Required Update' : 'Update & Install Now' }}</span>
              <span class="shimmer-sweep"></span>
            </button>

            <button
              v-if="!updateInfo.mandatory"
              class="action-btn later-btn"
              data-nav-item="true"
              tabindex="0"
              @click="handleBack"
            >
              Remind Me Later
            </button>
          </div>

          <div class="security-guarantee">
            <ShieldCheck :size="16" class="shield-green" />
            <span>Cryptographically signed package • Seamlessly upgrades your app</span>
          </div>
        </section>
      </div>

      <!-- Up to Date State (When no update available) -->
      <section v-else class="update-card up-to-date-card">
        <div class="up-to-date-inner">
          <div class="check-icon-large">
            <CheckCircle2 :size="48" />
          </div>
          <h2 class="up-to-date-title">Your App is Up to Date</h2>
          <p class="up-to-date-text">
            You have the latest version of WATZEM installed with all security patches, active server mirrors, and streaming scrapers.
          </p>

          <button
            class="action-btn check-now-btn"
            :class="{ 'is-checking': isManualChecking || isChecking }"
            data-nav-item="true"
            tabindex="0"
            :disabled="isManualChecking || isChecking"
            @click="handleManualCheck"
          >
            <RefreshCw :size="16" :class="{ 'spin-anim': isManualChecking || isChecking }" />
            <span>{{ isManualChecking || isChecking ? 'Checking for Updates...' : 'Check for Updates Now' }}</span>
          </button>

          <p v-if="checkSuccessMessage" class="check-success-text">
            <Check :size="14" />
            <span>{{ checkSuccessMessage }}</span>
          </p>
        </div>
      </section>

      <!-- Fallback Direct Download Link -->
      <footer class="update-footer-note">
        <ExternalLink :size="14" />
        <span>Need to download the APK manually? Visit the</span>
        <a href="/download" class="download-link">APK Download Page</a>
      </footer>
    </main>
  </div>
</template>

<style scoped>
/* Update Page - 100% Fluid Layout */
.update-page {
  width: 100%;
  max-width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-main, #07070a);
}

.update-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: max(var(--safe-top, 0px), env(safe-area-inset-top, 0px), 0.75rem) max(clamp(1rem, 3vw, 3rem), var(--safe-right, 0px)) 0.75rem max(clamp(1rem, 3vw, 3rem), var(--safe-left, 0px));
  background: rgba(10, 10, 16, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-subtle, rgba(255, 255, 255, 0.08));
  position: sticky;
  top: 0;
  z-index: 50;
  gap: 0.75rem;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: var(--text-primary, #ffffff);
  font-size: 0.85rem;
  font-weight: 700;
  padding: 8px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-back:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.25);
  transform: translateX(-2px);
}

.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #34d399;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.header-badge.has-update {
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.4);
  color: #fbbf24;
}

.sparkle-anim {
  animation: sparkleTwinkle 1.8s infinite ease-in-out;
}

@keyframes sparkleTwinkle {
  0%, 100% { transform: scale(1) rotate(0deg); opacity: 0.8; }
  50% { transform: scale(1.25) rotate(15deg); opacity: 1; }
}

/* Content Area */
.update-content {
  flex: 1;
  width: 100%;
  max-width: 100%;
  padding: 1.5rem max(clamp(1rem, 3vw, 3rem), var(--safe-right, 0px)) max(calc(var(--safe-bottom, 0px) + 3rem), 4rem) max(clamp(1rem, 3vw, 3rem), var(--safe-left, 0px));
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

/* Hero Card */
.update-hero-card {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-lg, 18px);
  background: linear-gradient(135deg, rgba(20, 24, 34, 0.85) 0%, rgba(12, 14, 22, 0.95) 100%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  padding: 1.75rem clamp(1.25rem, 3vw, 2.5rem);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.4);
}

.update-hero-card.update-ready {
  background: linear-gradient(135deg, rgba(32, 26, 18, 0.9) 0%, rgba(18, 15, 12, 0.95) 100%);
  border-color: rgba(245, 158, 11, 0.35);
}

.hero-glow-orb {
  position: absolute;
  top: -40px;
  right: -40px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.3) 0%, transparent 70%);
  filter: blur(45px);
  pointer-events: none;
}

.hero-inner {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.hero-icon-bubble {
  position: relative;
  width: 68px;
  height: 68px;
  border-radius: 18px;
  overflow: visible;
  flex-shrink: 0;
}

.app-icon-img {
  width: 100%;
  height: 100%;
  border-radius: 18px;
  object-fit: cover;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}

.update-badge-icon {
  position: absolute;
  bottom: -4px;
  right: -4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f59e0b;
  color: #07070a;
  display: grid;
  place-items: center;
  border: 2px solid #07070a;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.5);
}

.hero-title {
  font-size: clamp(1.4rem, 2.5vw, 2rem);
  font-weight: 800;
  color: #ffffff;
  margin-bottom: 0.35rem;
  letter-spacing: -0.02em;
}

.version-transition-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 0.5rem;
}

.v-pill {
  font-size: 0.76rem;
  font-weight: 700;
  padding: 3px 9px;
  border-radius: 999px;
}

.v-pill.current {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.v-pill.latest {
  background: rgba(245, 158, 11, 0.2);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.45);
}

.v-mandatory {
  font-size: 0.65rem;
  font-weight: 800;
  text-transform: uppercase;
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.4);
  padding: 2px 6px;
  border-radius: 4px;
  letter-spacing: 0.5px;
}

.v-arrow {
  color: rgba(255, 255, 255, 0.4);
}

.hero-desc {
  font-size: 0.88rem;
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.55;
  max-width: 850px;
}

/* Main Grid */
.update-main-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

@media (min-width: 1024px) {
  .update-main-grid {
    grid-template-columns: 1.4fr 1fr;
  }
}

/* Common Card */
.update-card {
  position: relative;
  border-radius: var(--radius-lg, 18px);
  background: var(--bg-card, rgba(16, 16, 24, 0.85));
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--border-subtle, rgba(255, 255, 255, 0.1));
  padding: 1.5rem clamp(1.25rem, 2.5vw, 2rem);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.3);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.card-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.notes-icon-wrap {
  background: rgba(245, 158, 11, 0.16);
  border: 1px solid rgba(245, 158, 11, 0.35);
  color: #f59e0b;
}

.download-icon-wrap {
  background: rgba(16, 185, 129, 0.16);
  border: 1px solid rgba(16, 185, 129, 0.35);
  color: #10b981;
}

.card-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #ffffff;
}

.card-subtitle {
  font-size: 0.76rem;
  color: var(--text-muted, rgba(255, 255, 255, 0.5));
}

/* Specs Strip */
.specs-strip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.spec-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.75);
  font-size: 0.75rem;
  font-weight: 600;
}

/* Notes List */
.notes-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.note-item {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.45;
}

.note-bullet-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(245, 158, 11, 0.18);
  color: #f59e0b;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  margin-top: 1px;
}

/* Progress Bar */
.download-progress-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  background: rgba(8, 8, 14, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 1.25rem;
}

.progress-labels {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.82rem;
  font-weight: 700;
  color: #ffffff;
}

.progress-pct {
  color: #f59e0b;
}

.progress-track {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
  position: relative;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #f59e0b, #10b981);
  border-radius: 999px;
  position: relative;
  transition: width 0.25s ease;
}

.progress-subtext {
  font-size: 0.72rem;
  color: var(--text-muted, rgba(255, 255, 255, 0.5));
}

/* Actions */
.action-buttons-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.65rem;
  padding: 12px 22px;
  border-radius: 999px;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  width: 100%;
}

.update-now-btn {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #07070a;
  box-shadow: 0 4px 18px rgba(245, 158, 11, 0.4);
}

.update-now-btn:hover {
  filter: brightness(1.1);
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(245, 158, 11, 0.55);
}

.later-btn {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.75);
}

.later-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.check-now-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #ffffff;
  max-width: 300px;
}

.check-now-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.35);
}

.security-guarantee {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.76rem;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 0.25rem;
}

.shield-green {
  color: #10b981;
  flex-shrink: 0;
}

/* Up to Date Card */
.up-to-date-card {
  align-items: center;
  text-align: center;
  padding: 3rem 1.5rem;
}

.up-to-date-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  max-width: 520px;
}

.check-icon-large {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(16, 185, 129, 0.15);
  border: 1px solid rgba(16, 185, 129, 0.35);
  color: #10b981;
  display: grid;
  place-items: center;
}

.up-to-date-title {
  font-size: 1.35rem;
  font-weight: 800;
  color: #ffffff;
}

.up-to-date-text {
  font-size: 0.88rem;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.55;
}

.check-success-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #34d399;
  font-size: 0.82rem;
  font-weight: 700;
}

.spin-anim {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Footer Note */
.update-footer-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 1rem;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.78rem;
  text-align: center;
  flex-wrap: wrap;
}

.download-link {
  color: #f59e0b;
  text-decoration: underline;
  cursor: pointer;
}

@media (max-width: 600px) {
  .hero-inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
}
</style>
