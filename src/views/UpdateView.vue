<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useAppUpdate } from '../composables/useAppUpdate';
import { useHaptics } from '../composables/useHaptics';
import SplashPosterMatrix from '../components/SplashPosterMatrix.vue';
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
  if (!updateInfo.value) {
    checkForUpdate();
  }
});
</script>

<template>
  <div class="update-page">
    <!-- Animated Poster Background Matrix from Splash Screen -->
    <SplashPosterMatrix spotlight="amber" dimLevel="deep" />

    <!-- Top Bar with Back Button -->
    <header class="update-header-bar">
      <button 
        class="btn-back"
        data-nav-item="true"
        tabindex="0"
        title="Back"
        @click="handleBack"
      >
        <ArrowLeft :size="16" />
        <span>Back</span>
      </button>

      <div class="header-badge" :class="{ 'has-update': updateAvailable }">
        <Sparkles v-if="updateAvailable" :size="13" class="sparkle-anim" />
        <CheckCircle2 v-else :size="13" />
        <span>{{ updateAvailable ? 'Update Available' : 'Up to Date' }}</span>
      </div>
    </header>

    <!-- Main Fluid & Compact Content Container -->
    <main class="update-content">
      <div class="update-container">
        <!-- Single Unified Master Card (No scattered cards) -->
        <div class="update-unified-card" :class="{ 'has-update': updateAvailable }">
          <div class="card-glow-edge" :class="updateAvailable ? 'glow-amber' : 'glow-emerald'"></div>

          <!-- Card Header Section -->
          <div class="card-header-cluster">
            <div class="app-icon-wrap">
              <img src="/icon-192.png" alt="WATZEM" class="app-icon-img" />
              <span class="status-dot" :class="updateAvailable ? 'dot-amber' : 'dot-emerald'">
                <Sparkles v-if="updateAvailable" :size="10" />
                <Check v-else :size="10" />
              </span>
            </div>

            <div class="header-details">
              <h1 class="card-main-title">
                {{ updateAvailable ? (updateInfo?.title || 'New Update Available') : 'WATZEM is Up to Date' }}
              </h1>

              <div class="version-pill-row">
                <span class="v-pill current">v{{ currentVersion }} (Build {{ currentBuild }})</span>
                <template v-if="updateAvailable && updateInfo">
                  <ArrowRight :size="12" class="v-arrow" />
                  <span class="v-pill latest">v{{ updateInfo.latestVersion }}</span>
                  <span v-if="updateInfo.mandatory" class="v-mandatory">Required</span>
                </template>
                <span v-else class="v-pill current-ok">
                  <CheckCircle2 :size="11" /> Latest Official Build
                </span>
              </div>
            </div>
          </div>

          <div class="card-divider"></div>

          <!-- When Update is Available: Release Notes & Actions in same card -->
          <div v-if="updateAvailable && updateInfo" class="card-body-section">
            <!-- Specs Strip -->
            <div class="specs-strip">
              <span class="spec-pill" v-if="updateInfo.fileSize">
                <HardDrive :size="12" />
                <span>{{ updateInfo.fileSize }}</span>
              </span>
              <span class="spec-pill">
                <ShieldCheck :size="12" />
                <span>Verified APK</span>
              </span>
              <span class="spec-pill" v-if="updateInfo.releaseDate">
                <Calendar :size="12" />
                <span>{{ updateInfo.releaseDate }}</span>
              </span>
            </div>

            <!-- Changelog Section -->
            <div class="changelog-area" v-if="updateInfo.releaseNotes?.length">
              <span class="changelog-heading">What's New in v{{ updateInfo.latestVersion }}:</span>
              <ul class="notes-list">
                <li
                  v-for="(note, index) in updateInfo.releaseNotes"
                  :key="index"
                  class="note-item"
                >
                  <div class="note-bullet">
                    <Check :size="10" />
                  </div>
                  <span class="note-text">{{ note }}</span>
                </li>
              </ul>
            </div>

            <!-- Active Downloading State -->
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

            <!-- Ready to Install Action Buttons -->
            <div v-else class="action-buttons-wrap">
              <button
                class="action-btn update-now-btn"
                data-nav-item="true"
                tabindex="0"
                @click="handleUpdate"
              >
                <Download :size="16" class="bounce-anim" />
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
              <ShieldCheck :size="13" class="shield-green" />
              <span>Signed APK package • Seamless in-place upgrade</span>
            </div>
          </div>

          <!-- When Up to Date: Clean Single-Card Body -->
          <div v-else class="up-to-date-body">
            <p class="up-to-date-desc">
              You are running the latest version. All streaming scrapers, server mirrors, and security patches are operational.
            </p>

            <button
              class="action-btn check-now-btn"
              :class="{ 'is-checking': isManualChecking || isChecking }"
              data-nav-item="true"
              tabindex="0"
              :disabled="isManualChecking || isChecking"
              @click="handleManualCheck"
            >
              <RefreshCw :size="14" :class="{ 'spin-anim': isManualChecking || isChecking }" />
              <span>{{ isManualChecking || isChecking ? 'Checking for Updates...' : 'Check for Updates' }}</span>
            </button>

            <p v-if="checkSuccessMessage" class="check-success-text">
              <Check :size="13" />
              <span>{{ checkSuccessMessage }}</span>
            </p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* Page Layout */
.update-page {
  position: relative;
  width: 100%;
  max-width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-main, #07070a);
  overflow-x: hidden;
}

/* Glassmorphic Sticky Header */
.update-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: max(var(--safe-top, 0px), env(safe-area-inset-top, 0px), 0.6rem) max(clamp(1rem, 3vw, 2.5rem), var(--safe-right, 0px)) 0.6rem max(clamp(1rem, 3vw, 2.5rem), var(--safe-left, 0px));
  background: rgba(7, 7, 10, 0.78);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  position: sticky;
  top: 0;
  z-index: 50;
  gap: 0.75rem;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: var(--text-primary, #ffffff);
  font-size: 0.82rem;
  font-weight: 700;
  padding: 6px 14px;
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
  gap: 5px;
  padding: 4px 10px;
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #34d399;
  border-radius: 999px;
  font-size: 0.72rem;
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
  50% { transform: scale(1.2) rotate(15deg); opacity: 1; }
}

/* Main Content Area */
.update-content {
  position: relative;
  z-index: 10;
  flex: 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.25rem max(clamp(1rem, 3vw, 2.5rem), var(--safe-right, 0px)) max(calc(var(--safe-bottom, 0px) + 2rem), 2.5rem) max(clamp(1rem, 3vw, 2.5rem), var(--safe-left, 0px));
}

.update-container {
  width: 100%;
  max-width: 520px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* Single Unified Card */
.update-unified-card {
  position: relative;
  border-radius: 18px;
  background: rgba(14, 14, 22, 0.82);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.45);
  padding: 1.25rem 1.35rem;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.update-unified-card.has-update {
  background: linear-gradient(135deg, rgba(26, 20, 14, 0.85) 0%, rgba(14, 14, 22, 0.9) 100%);
  border-color: rgba(245, 158, 11, 0.28);
}

.card-glow-edge {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
}

.glow-amber {
  background: linear-gradient(90deg, transparent, rgba(245, 158, 11, 0.75), transparent);
}

.glow-emerald {
  background: linear-gradient(90deg, transparent, rgba(16, 185, 129, 0.75), transparent);
}

/* Card Header Cluster */
.card-header-cluster {
  display: flex;
  align-items: center;
  gap: 0.9rem;
}

.app-icon-wrap {
  position: relative;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.app-icon-img {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  object-fit: cover;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.45);
}

.status-dot {
  position: absolute;
  bottom: -3px;
  right: -3px;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  border: 2px solid #07070a;
}

.dot-amber {
  background: #f59e0b;
  color: #07070a;
  box-shadow: 0 0 8px rgba(245, 158, 11, 0.6);
}

.dot-emerald {
  background: #10b981;
  color: #ffffff;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.6);
}

.header-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
  min-width: 0;
}

.card-main-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.01em;
  line-height: 1.25;
}

.version-pill-row {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.v-pill {
  font-size: 0.7rem;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.v-pill.current {
  background: rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.v-pill.current-ok {
  background: rgba(16, 185, 129, 0.15);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.v-pill.latest {
  background: rgba(245, 158, 11, 0.18);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.4);
}

.v-mandatory {
  font-size: 0.6rem;
  font-weight: 800;
  text-transform: uppercase;
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.4);
  padding: 1px 5px;
  border-radius: 4px;
  letter-spacing: 0.4px;
}

.v-arrow {
  color: rgba(255, 255, 255, 0.35);
}

.card-divider {
  width: 100%;
  height: 1px;
  background: rgba(255, 255, 255, 0.07);
}

/* Card Body Sections */
.card-body-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* Specs Strip */
.specs-strip {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.spec-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.7rem;
  font-weight: 600;
}

/* Changelog Area */
.changelog-area {
  background: rgba(6, 6, 10, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 10px;
  padding: 0.75rem 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.changelog-heading {
  font-size: 0.72rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.notes-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-height: 160px;
  overflow-y: auto;
}

.note-item {
  display: flex;
  align-items: flex-start;
  gap: 0.45rem;
}

.note-bullet {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background: rgba(16, 185, 129, 0.15);
  color: #34d399;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.note-text {
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.35;
}

/* Downloading Progress */
.download-progress-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 0.4rem 0;
}

.progress-labels {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.progress-status {
  font-size: 0.76rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
}

.progress-pct {
  font-size: 0.8rem;
  font-weight: 800;
  color: #fbbf24;
}

.progress-track {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
  position: relative;
}

.progress-bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #f59e0b, #ec4899);
  transition: width 0.25s ease;
  position: relative;
}

.progress-glow-dot {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 8px;
  background: #ffffff;
  box-shadow: 0 0 8px #ffffff;
  opacity: 0.8;
}

.progress-subtext {
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.5);
  text-align: right;
}

/* Action Buttons */
.action-buttons-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  padding: 9px 16px;
  border-radius: 999px;
  font-size: 0.82rem;
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
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.35);
}

.update-now-btn:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.later-btn {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.75);
}

.later-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.bounce-anim {
  animation: bounceDown 1.5s infinite;
}

@keyframes bounceDown {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(3px); }
}

.shimmer-sweep {
  position: absolute;
  top: 0;
  left: -100%;
  width: 60%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: shimmerSweep 2.5s infinite;
}

@keyframes shimmerSweep {
  0% { left: -100%; }
  60%, 100% { left: 160%; }
}

.security-guarantee {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 0.68rem;
  color: rgba(255, 255, 255, 0.45);
  text-align: center;
}

.shield-green {
  color: #10b981;
}

/* Up-to-Date Single Card Body */
.up-to-date-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.75rem;
  padding: 0.5rem 0.25rem 0.25rem;
}

.up-to-date-desc {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.65);
  line-height: 1.45;
  max-width: 400px;
}

.check-now-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #ffffff;
  max-width: 250px;
}

.check-now-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.28);
}

.check-now-btn:disabled {
  opacity: 0.7;
  cursor: wait;
}

.spin-anim {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.check-success-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #34d399;
  font-size: 0.74rem;
  font-weight: 600;
}
</style>
