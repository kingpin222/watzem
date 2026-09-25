<script setup lang="ts">
import { useAppUpdate } from '../composables/useAppUpdate';
import { useHaptics } from '../composables/useHaptics';
import {
  Download,
  Sparkles,
  CheckCircle2,
  ShieldCheck,
  X,
  ArrowRight,
  RefreshCw,
  HardDrive
} from 'lucide-vue-next';

const {
  updateInfo,
  showUpdateModal,
  isDownloading,
  downloadProgress,
  downloadBytesText,
  downloadStatusText,
  currentVersion,
  startUpdate,
  dismissModal
} = useAppUpdate();

const { triggerLight, triggerMedium } = useHaptics();

const handleUpdate = () => {
  triggerMedium();
  startUpdate();
};

const handleDismiss = () => {
  triggerLight();
  dismissModal();
};
</script>

<template>
  <Teleport to="body">
    <div
      v-if="showUpdateModal && updateInfo"
      class="update-modal-backdrop"
      @click.self="!updateInfo.mandatory && handleDismiss()"
    >
      <div class="update-modal-card" role="dialog" aria-modal="true" aria-labelledby="update-title">
        <!-- Close button if not mandatory -->
        <button
          v-if="!updateInfo.mandatory"
          class="close-modal-btn"
          aria-label="Close"
          @click="handleDismiss"
        >
          <X :size="18" />
        </button>

        <!-- Header Icon & Glow -->
        <div class="update-icon-wrap">
          <div class="icon-glow-ring"></div>
          <img src="/icon-192.png" alt="WATZEM" class="app-icon-img" />
          <span class="update-badge-icon">
            <RefreshCw :size="12" class="spin-icon" />
          </span>
        </div>

        <!-- Headline & Version Transition -->
        <h2 id="update-title" class="update-headline">
          {{ updateInfo.title || 'Update Available' }}
        </h2>

        <div class="version-transition-pill">
          <span class="v-tag old">v{{ currentVersion }}</span>
          <ArrowRight :size="13" class="v-arrow" />
          <span class="v-tag new">v{{ updateInfo.latestVersion }}</span>
          <span v-if="updateInfo.mandatory" class="mandatory-badge">Required</span>
        </div>

        <!-- Specs Strip -->
        <div class="update-specs-strip">
          <span class="spec-pill">
            <HardDrive :size="12" />
            <span>{{ updateInfo.fileSize || '3.9 MB' }}</span>
          </span>
          <span class="spec-pill">
            <ShieldCheck :size="12" />
            <span>Verified APK</span>
          </span>
          <span v-if="updateInfo.releaseDate" class="spec-pill">
            <CheckCircle2 :size="12" />
            <span>{{ updateInfo.releaseDate }}</span>
          </span>
        </div>

        <!-- Release Notes Box -->
        <div class="release-notes-box">
          <div class="notes-header">
            <Sparkles :size="14" class="notes-icon" />
            <span>What's New in v{{ updateInfo.latestVersion }}</span>
          </div>
          <ul class="notes-list">
            <li
              v-for="(note, index) in updateInfo.releaseNotes"
              :key="index"
              class="note-item"
            >
              <span class="bullet">•</span>
              <span class="note-text">{{ note }}</span>
            </li>
          </ul>
        </div>

        <!-- In-App Download Progress Container -->
        <div v-if="isDownloading" class="download-progress-container">
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

        <!-- Action Buttons -->
        <div v-else class="update-actions">
          <button
            class="update-now-btn"
            @click="handleUpdate"
          >
            <Download :size="18" class="btn-icon bounce-anim" />
            <span>{{ updateInfo.mandatory ? 'Install Required Update' : 'Update & Install Now' }}</span>
            <span class="shimmer-sweep"></span>
          </button>

          <button
            v-if="!updateInfo.mandatory"
            class="update-later-btn"
            @click="handleDismiss"
          >
            Remind Me Later
          </button>
        </div>

        <!-- Safe Security Note -->
        <div class="safe-tag">
          <ShieldCheck :size="12" />
          <span>Official package • Installs smoothly over your existing app</span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.update-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(8, 8, 12, 0.78);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.25rem;
  animation: fadeIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.update-modal-card {
  position: relative;
  width: 100%;
  max-width: 400px;
  background: rgba(30, 26, 16, 0.94);
  border: 1px solid rgba(250, 204, 21, 0.32);
  border-radius: 24px;
  padding: 1.75rem 1.4rem;
  box-shadow:
    0 24px 60px -15px rgba(0, 0, 0, 0.8),
    0 0 40px -5px rgba(250, 204, 21, 0.25);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  animation: popIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes popIn {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(12px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.close-modal-btn {
  position: absolute;
  top: 1rem;
  right: 1rem;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #d1d5db;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.18s ease;
}

.close-modal-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
}

/* Icon & Badge */
.update-icon-wrap {
  position: relative;
  width: 62px;
  height: 62px;
  margin-bottom: 0.9rem;
}

.app-icon-img {
  width: 100%;
  height: 100%;
  border-radius: 16px;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.25);
  box-shadow: 0 8px 24px rgba(234, 179, 8, 0.3);
  position: relative;
  z-index: 2;
}

.icon-glow-ring {
  position: absolute;
  inset: -6px;
  border-radius: 22px;
  background: radial-gradient(circle, rgba(250, 204, 21, 0.6) 0%, transparent 70%);
  filter: blur(8px);
  z-index: 1;
}

.update-badge-icon {
  position: absolute;
  bottom: -4px;
  right: -4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #facc15;
  color: #1a1600;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #231f13;
  z-index: 3;
}

.spin-icon {
  animation: slowSpin 8s linear infinite;
}

@keyframes slowSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Headline */
.update-headline {
  font-size: 1.35rem;
  font-weight: 850;
  letter-spacing: -0.02em;
  color: #ffffff;
  margin: 0 0 0.5rem 0;
  line-height: 1.25;
}

/* Version Pill */
.version-transition-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(250, 204, 21, 0.2);
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  margin-bottom: 0.85rem;
}

.v-tag {
  font-size: 0.75rem;
  font-weight: 700;
}

.v-tag.old {
  color: #9ca3af;
}

.v-arrow {
  color: #facc15;
}

.v-tag.new {
  color: #fef08a;
  background: rgba(250, 204, 21, 0.16);
  padding: 0.1rem 0.45rem;
  border-radius: 6px;
}

.mandatory-badge {
  font-size: 0.65rem;
  font-weight: 850;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  background: rgba(239, 68, 68, 0.22);
  border: 1px solid rgba(239, 68, 68, 0.45);
  color: #fca5a5;
  padding: 0.12rem 0.45rem;
  border-radius: 999px;
  margin-left: 0.2rem;
}

/* Specs */
.update-specs-strip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  flex-wrap: wrap;
  margin-bottom: 1.1rem;
}

.spec-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.68rem;
  font-weight: 600;
  color: #fef08a;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(250, 204, 21, 0.15);
  padding: 0.18rem 0.5rem;
  border-radius: 6px;
}

.spec-pill svg {
  color: #facc15;
}

/* Release Notes */
.release-notes-box {
  width: 100%;
  background: rgba(20, 18, 12, 0.6);
  border: 1px solid rgba(250, 204, 21, 0.18);
  border-radius: 14px;
  padding: 0.85rem 1rem;
  text-align: left;
  margin-bottom: 1.25rem;
  max-height: 160px;
  overflow-y: auto;
}

.notes-header {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.78rem;
  font-weight: 750;
  color: #fef08a;
  margin-bottom: 0.5rem;
}

.notes-icon {
  color: #facc15;
}

.notes-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.note-item {
  display: flex;
  align-items: flex-start;
  gap: 0.45rem;
  font-size: 0.74rem;
  color: #e5e7eb;
  line-height: 1.35;
}

.bullet {
  color: #facc15;
  font-weight: 800;
}

/* Action Buttons */
.update-actions {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  margin-bottom: 0.85rem;
}

.update-now-btn {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  background: linear-gradient(135deg, #facc15 0%, #eab308 100%);
  color: #1a1600;
  padding: 0.85rem 1.25rem;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  font-size: 0.95rem;
  font-weight: 850;
  letter-spacing: -0.01em;
  box-shadow:
    0 8px 24px rgba(234, 179, 8, 0.45),
    inset 0 1px 1px rgba(255, 255, 255, 0.5);
  cursor: pointer;
  overflow: hidden;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.update-now-btn:active {
  transform: scale(0.97);
  box-shadow: 0 4px 12px rgba(234, 179, 8, 0.35);
}

.update-now-btn.loading {
  opacity: 0.85;
  cursor: wait;
}

.btn-icon.bounce-anim {
  animation: subtleBounce 2s ease infinite;
}

@keyframes subtleBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-2px); }
}

.shimmer-sweep {
  position: absolute;
  top: 0;
  left: -75%;
  width: 50%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.45), transparent);
  transform: skewX(-20deg);
  animation: shimmer 3.5s infinite;
}

@keyframes shimmer {
  0%, 75% { left: -75%; }
  100% { left: 125%; }
}

.update-later-btn {
  background: transparent;
  border: none;
  color: #9ca3af;
  font-size: 0.78rem;
  font-weight: 600;
  padding: 0.35rem;
  cursor: pointer;
  transition: color 0.15s ease;
}

.update-later-btn:hover {
  color: #ffffff;
}

/* Safe Tag */
.safe-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  font-size: 0.66rem;
  color: #fef08a;
  opacity: 0.85;
}

.safe-tag svg {
  color: #facc15;
}

/* In-App Download Progress Container */
.download-progress-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(234, 179, 8, 0.25);
  border-radius: 14px;
  padding: 0.85rem 1rem;
  animation: fadeInProgress 0.25s ease forwards;
}

@keyframes fadeInProgress {
  from { opacity: 0; transform: scale(0.97); }
  to { opacity: 1; transform: scale(1); }
}

.progress-labels {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.76rem;
}

.progress-status {
  color: #f3f4f6;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.progress-pct {
  color: #eab308;
  font-weight: 700;
  font-size: 0.82rem;
}

.progress-track {
  width: 100%;
  height: 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 999px;
  overflow: hidden;
  position: relative;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #ca8a04, #eab308, #fef08a);
  border-radius: 999px;
  transition: width 0.15s ease-out;
  position: relative;
  box-shadow: 0 0 12px rgba(234, 179, 8, 0.6);
}

.progress-glow-dot {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 0 6px #ffffff;
}

.progress-subtext {
  font-size: 0.68rem;
  color: #9ca3af;
  text-align: right;
}
</style>
