<script setup lang="ts">
import { useToast } from '../composables/useToast';
import { CheckCircle2, AlertCircle, HardDrive, X } from 'lucide-vue-next';

const { toasts, dismissToast } = useToast();
</script>

<template>
  <div class="toast-container" aria-live="polite">
    <TransitionGroup name="toast-anim" tag="div" class="toast-stack">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast-pill"
        :class="t.type || 'info'"
        @click="dismissToast(t.id)"
      >
        <div class="toast-icon-wrapper">
          <CheckCircle2 v-if="t.type === 'success'" :size="16" class="icon-success" />
          <AlertCircle v-else-if="t.type === 'warning' || t.type === 'error'" :size="16" class="icon-warn" />
          <HardDrive v-else :size="16" class="icon-info" />
        </div>

        <div class="toast-content">
          <span class="toast-title">{{ t.title }}</span>
          <span v-if="t.message" class="toast-msg">{{ t.message }}</span>
        </div>

        <button class="toast-close" aria-label="Close notification" @click.stop="dismissToast(t.id)">
          <X :size="13" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: calc(max(var(--safe-top, 0px), env(safe-area-inset-top, 0px)) + var(--header-height) + 12px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  width: 92%;
  max-width: 440px;
  pointer-events: none;
}

.toast-stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.toast-pill {
  pointer-events: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  background: rgba(18, 20, 29, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 10px 30px -5px rgba(0, 0, 0, 0.6), 0 0 1px 1px rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all 0.2s ease;
}

.toast-pill.success {
  border-color: rgba(16, 185, 129, 0.4);
  box-shadow: 0 10px 30px -5px rgba(16, 185, 129, 0.2);
}

.toast-pill.warning {
  border-color: rgba(245, 158, 11, 0.4);
}

.toast-pill.error {
  border-color: rgba(244, 63, 94, 0.4);
}

.toast-icon-wrapper {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-success {
  color: var(--accent-emerald);
}

.icon-warn {
  color: var(--accent-rose);
}

.icon-info {
  color: var(--accent-amber);
}

.toast-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.toast-title {
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.3;
}

.toast-msg {
  font-size: 0.72rem;
  color: var(--text-muted);
  line-height: 1.3;
  margin-top: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.toast-close {
  background: transparent;
  border: none;
  color: var(--text-muted);
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.15s ease;
}

.toast-close:hover {
  opacity: 1;
}

/* Animations */
.toast-anim-enter-active,
.toast-anim-leave-active {
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.toast-anim-enter-from {
  opacity: 0;
  transform: translateY(-16px) scale(0.95);
}

.toast-anim-leave-to {
  opacity: 0;
  transform: translateY(-12px) scale(0.95);
}
</style>
