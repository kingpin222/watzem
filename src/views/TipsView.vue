<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import {
  Heart,
  Coffee,
  Copy,
  Check,
  ExternalLink,
  ArrowLeft,
  Sparkles,
  QrCode,
  ShieldCheck
} from 'lucide-vue-next';
import QRCode from 'qrcode';
import { useMonetization } from '../composables/useMonetization';
import { CryptoWallet } from '../config/monetization';
import { useHaptics } from '../composables/useHaptics';

const emit = defineEmits<{
  (e: 'back'): void;
}>();

const { copyAddress, openCoffeeLink, config } = useMonetization();
const { triggerLight } = useHaptics();

const copiedId = ref<string | null>(null);
const selectedWalletId = ref<string>(config.cryptoWallets[0]?.id || 'btc');
const qrCodes = ref<Record<string, string>>({});
const isGeneratingQr = ref(false);

const activeWallet = computed<CryptoWallet>(() => {
  return config.cryptoWallets.find(w => w.id === selectedWalletId.value) || config.cryptoWallets[0];
});

const generateAllQrs = async () => {
  isGeneratingQr.value = true;
  for (const wallet of config.cryptoWallets) {
    try {
      const url = await QRCode.toDataURL(wallet.address, {
        width: 220,
        margin: 1,
        color: {
          dark: '#08080c',
          light: '#ffffff'
        },
        errorCorrectionLevel: 'M'
      });
      qrCodes.value[wallet.id] = url;
    } catch (err) {
      console.error('Failed to generate QR for wallet', wallet.id, err);
    }
  }
  isGeneratingQr.value = false;
};

const handleCopy = async (wallet: CryptoWallet) => {
  await copyAddress(wallet);
  copiedId.value = wallet.id;
  setTimeout(() => {
    if (copiedId.value === wallet.id) {
      copiedId.value = null;
    }
  }, 3000);
};

const handleBack = () => {
  triggerLight();
  emit('back');
};

onMounted(() => {
  generateAllQrs();
});
</script>

<template>
  <div class="tips-page">
    <!-- Top Bar with Back Button -->
    <header class="tips-header-bar">
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

      <div class="header-badge">
        <Heart :size="14" class="heart-pulse-icon" />
        <span>Community Supported</span>
      </div>
    </header>

    <!-- Main Fluid Content Container -->
    <main class="tips-content">
      <!-- Hero Header Banner -->
      <section class="tips-hero-card">
        <div class="hero-glow-orb"></div>
        <div class="hero-inner">
          <div class="hero-icon-bubble">
            <Heart :size="32" class="hero-heart" />
          </div>
          <div class="hero-text">
            <h1 class="hero-title">Support WATZEM</h1>
            <p class="hero-desc">
              WATZEM is an independent, non-profit streaming project dedicated to cinema lovers worldwide.
              Your tips help pay for high-speed streaming scrapers, server mirrors, and ongoing updates.
            </p>
          </div>
        </div>
      </section>

      <!-- Grid of Support Channels -->
      <div class="support-grid">
        <!-- Direct Coffee Support Option -->
        <section class="support-card coffee-card" v-if="config.coffee.enabled">
          <div class="card-glow-edge"></div>
          <div class="card-header">
            <div class="card-icon-wrap coffee-icon-wrap">
              <Coffee :size="24" />
            </div>
            <div>
              <h2 class="card-title">Buy Us a Coffee</h2>
              <p class="card-subtitle">Quick card, Apple Pay, Google Pay support</p>
            </div>
          </div>

          <p class="card-body-text">
            Leave a personalized tip or note! Every coffee helps keep the servers alive and streaming without intrusive ads.
          </p>

          <button 
            class="action-btn coffee-btn"
            data-nav-item="true"
            tabindex="0"
            @click="openCoffeeLink"
          >
            <Coffee :size="18" />
            <span>{{ config.coffee.name }}</span>
            <ExternalLink :size="14" class="link-arrow" />
          </button>
        </section>

        <!-- Crypto Support Card -->
        <section class="support-card crypto-card" v-if="config.cryptoWallets.length > 0">
          <div class="card-header">
            <div class="card-icon-wrap crypto-icon-wrap">
              <Sparkles :size="24" />
            </div>
            <div>
              <h2 class="card-title">Cryptocurrency Tips</h2>
              <p class="card-subtitle">Direct, decentralized, and anonymous</p>
            </div>
          </div>

          <!-- Currency Selector Pills -->
          <div class="wallet-pills" role="tablist">
            <button
              v-for="wallet in config.cryptoWallets"
              :key="wallet.id"
              class="wallet-pill-btn"
              :class="{ active: selectedWalletId === wallet.id }"
              data-nav-item="true"
              tabindex="0"
              role="tab"
              :aria-selected="selectedWalletId === wallet.id"
              @click="selectedWalletId = wallet.id"
            >
              <span class="pill-dot" :style="{ backgroundColor: wallet.badgeColor || '#f59e0b' }"></span>
              <span class="pill-symbol">{{ wallet.symbol }}</span>
              <span class="pill-name">{{ wallet.name }}</span>
            </button>
          </div>

          <!-- Active Wallet Display -->
          <div class="wallet-details-box" v-if="activeWallet">
            <div class="qr-col">
              <div class="qr-frame" @click="handleCopy(activeWallet)">
                <img 
                  v-if="qrCodes[activeWallet.id]"
                  :src="qrCodes[activeWallet.id]"
                  :alt="`${activeWallet.symbol} QR Code`"
                  class="qr-img"
                />
                <div v-else class="qr-loading">
                  <QrCode :size="32" class="spin-slow" />
                  <span>Generating QR...</span>
                </div>
              </div>
              <span class="qr-hint">Scan with mobile wallet</span>
            </div>

            <div class="info-col">
              <div class="network-badge-row">
                <span class="network-tag">{{ activeWallet.network }}</span>
                <span class="symbol-tag" :style="{ color: activeWallet.badgeColor || '#f59e0b' }">
                  {{ activeWallet.symbol }}
                </span>
              </div>

              <div class="address-display-row" @click="handleCopy(activeWallet)">
                <code class="address-text">{{ activeWallet.address }}</code>
              </div>

              <button 
                class="action-btn copy-btn"
                :class="{ copied: copiedId === activeWallet.id }"
                data-nav-item="true"
                tabindex="0"
                @click="handleCopy(activeWallet)"
              >
                <Check v-if="copiedId === activeWallet.id" :size="18" />
                <Copy v-else :size="18" />
                <span>{{ copiedId === activeWallet.id ? 'Address Copied!' : 'Copy Wallet Address' }}</span>
              </button>
            </div>
          </div>
        </section>
      </div>

      <!-- Trust & Privacy Footnote -->
      <footer class="tips-footer-note">
        <ShieldCheck :size="16" class="note-shield" />
        <span>WATZEM never collects payment information. All donations are 100% voluntary and non-recurring.</span>
      </footer>
    </main>
  </div>
</template>

<style scoped>
/* Tips Page - 100% Fluid Layout */
.tips-page {
  width: 100%;
  max-width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-main, #07070a);
}

.tips-header-bar {
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
  background: rgba(244, 63, 94, 0.12);
  border: 1px solid rgba(244, 63, 94, 0.3);
  color: #fb7185;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.heart-pulse-icon {
  animation: heartPulse 1.8s infinite ease-in-out;
}

@keyframes heartPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

/* Content Area */
.tips-content {
  flex: 1;
  width: 100%;
  max-width: 100%;
  padding: 1.5rem max(clamp(1rem, 3vw, 3rem), var(--safe-right, 0px)) max(calc(var(--safe-bottom, 0px) + 3rem), 4rem) max(clamp(1rem, 3vw, 3rem), var(--safe-left, 0px));
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

/* Hero Card */
.tips-hero-card {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-lg, 18px);
  background: linear-gradient(135deg, rgba(30, 20, 25, 0.85) 0%, rgba(18, 14, 22, 0.95) 100%);
  border: 1px solid rgba(244, 63, 94, 0.25);
  padding: 1.75rem clamp(1.25rem, 3vw, 2.5rem);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.4);
}

.hero-glow-orb {
  position: absolute;
  top: -40px;
  right: -40px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(244, 63, 94, 0.35) 0%, transparent 70%);
  filter: blur(40px);
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
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f43f5e, #e11d48);
  display: grid;
  place-items: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 8px 24px rgba(244, 63, 94, 0.45);
}

.hero-title {
  font-size: clamp(1.4rem, 2.5vw, 2rem);
  font-weight: 800;
  color: #ffffff;
  margin-bottom: 0.35rem;
  letter-spacing: -0.02em;
}

.hero-desc {
  font-size: 0.88rem;
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.55;
  max-width: 850px;
}

/* Support Grid */
.support-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

@media (min-width: 1024px) {
  .support-grid {
    grid-template-columns: 1fr 1.6fr;
  }
}

/* Common Support Card */
.support-card {
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

.coffee-icon-wrap {
  background: rgba(245, 158, 11, 0.16);
  border: 1px solid rgba(245, 158, 11, 0.35);
  color: #f59e0b;
}

.crypto-icon-wrap {
  background: rgba(6, 182, 212, 0.16);
  border: 1px solid rgba(6, 182, 212, 0.35);
  color: #06b6d4;
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

.card-body-text {
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.5;
}

/* Action Buttons */
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

.coffee-btn {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #07070a;
  box-shadow: 0 4px 18px rgba(245, 158, 11, 0.4);
  margin-top: auto;
}

.coffee-btn:hover {
  filter: brightness(1.1);
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(245, 158, 11, 0.55);
}

.copy-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #ffffff;
}

.copy-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.35);
}

.copy-btn.copied {
  background: rgba(16, 185, 129, 0.2);
  border-color: rgba(16, 185, 129, 0.5);
  color: #34d399;
}

/* Wallet Pills */
.wallet-pills {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.wallet-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.75);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
}

.wallet-pill-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.wallet-pill-btn.active {
  background: rgba(245, 158, 11, 0.18);
  border-color: var(--accent-amber, #f59e0b);
  color: #fbbf24;
  box-shadow: 0 0 14px rgba(245, 158, 11, 0.25);
}

.pill-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.pill-symbol {
  font-weight: 800;
}

.pill-name {
  font-size: 0.72rem;
  opacity: 0.7;
}

/* Wallet Details Box */
.wallet-details-box {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
  background: rgba(8, 8, 14, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 1.25rem;
}

@media (min-width: 640px) {
  .wallet-details-box {
    grid-template-columns: 200px 1fr;
    align-items: center;
  }
}

.qr-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.qr-frame {
  width: 190px;
  height: 190px;
  background: #ffffff;
  border-radius: 12px;
  padding: 8px;
  display: grid;
  place-items: center;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.5);
  transition: transform 0.2s ease;
}

.qr-frame:hover {
  transform: scale(1.03);
}

.qr-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  color: #08080c;
  font-size: 0.75rem;
  font-weight: 600;
}

.qr-hint {
  font-size: 0.7rem;
  color: var(--text-muted, rgba(255, 255, 255, 0.5));
}

.info-col {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.network-badge-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.network-tag {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 3px 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.85);
}

.symbol-tag {
  font-size: 0.82rem;
  font-weight: 800;
}

.address-display-row {
  background: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  padding: 0.75rem;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.address-display-row:hover {
  border-color: rgba(245, 158, 11, 0.5);
}

.address-text {
  font-family: var(--font-mono, monospace);
  font-size: 0.82rem;
  color: #fbbf24;
  word-break: break-all;
  line-height: 1.45;
  user-select: all;
}

/* Footer Note */
.tips-footer-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 1rem;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.78rem;
  text-align: center;
}

.note-shield {
  color: #10b981;
  flex-shrink: 0;
}

@media (max-width: 600px) {
  .hero-inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
}
</style>
