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
  ShieldCheck,
  Share2,
  Send,
  MessageCircle
} from 'lucide-vue-next';
import QRCode from 'qrcode';
import SplashPosterMatrix from '../components/SplashPosterMatrix.vue';
import { useMonetization, openExternalUrl } from '../composables/useMonetization';
import { CryptoWallet } from '../config/monetization';
import { useHaptics } from '../composables/useHaptics';
import { useDeepLinks, APP_DOWNLOAD_URL } from '../composables/useDeepLinks';

const emit = defineEmits<{
  (e: 'back'): void;
}>();

const { copyAddress, openCoffeeLink, config } = useMonetization();
const { triggerLight } = useHaptics();
const { shareApp, copyAppLink } = useDeepLinks();

// Active Support Tab: 'share' | 'coffee' | 'crypto'
const activeSupportTab = ref<'share' | 'coffee' | 'crypto'>('share');

const isCopiedAppLink = ref(false);

const handleShareApp = async () => {
  triggerLight();
  await shareApp();
};

const handleCopyAppLink = async () => {
  triggerLight();
  const ok = await copyAppLink();
  if (ok) {
    isCopiedAppLink.value = true;
    setTimeout(() => {
      isCopiedAppLink.value = false;
    }, 3000);
  }
};

const shareToSocial = (platform: 'whatsapp' | 'telegram' | 'twitter' | 'facebook') => {
  triggerLight();
  const text = 'Watch thousands of free movies and TV series in HD with zero popup ads on WATZEM!';
  const url = APP_DOWNLOAD_URL;
  let target = '';
  if (platform === 'whatsapp') {
    target = `https://api.whatsapp.com/send?text=${encodeURIComponent(text + ' ' + url)}`;
  } else if (platform === 'telegram') {
    target = `https://t.me/share/url?url=${encodeURIComponent(url)}&text=${encodeURIComponent(text)}`;
  } else if (platform === 'twitter') {
    target = `https://twitter.com/intent/tweet?text=${encodeURIComponent(text)}&url=${encodeURIComponent(url)}`;
  } else if (platform === 'facebook') {
    target = `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`;
  }
  openExternalUrl(target);
};

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
        width: 160,
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

const selectTab = (tab: 'share' | 'coffee' | 'crypto') => {
  triggerLight();
  activeSupportTab.value = tab;
};

onMounted(() => {
  generateAllQrs();
});
</script>

<template>
  <div class="tips-page">
    <!-- Animated Poster Background Matrix from Splash Screen -->
    <SplashPosterMatrix spotlight="rose" dimLevel="deep" />

    <!-- Top Bar with Back Button -->
    <header class="tips-header-bar">
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

      <div class="header-badge">
        <Heart :size="13" class="heart-pulse-icon" />
        <span>Community Supported</span>
      </div>
    </header>

    <!-- Main Fluid & Compact Content Container -->
    <main class="tips-content">
      <div class="tips-container">
        <!-- Single Unified Master Support Card (No scattered cards) -->
        <div class="tips-unified-card">
          <div class="card-glow-edge"></div>

          <!-- Header Section -->
          <div class="card-header-cluster">
            <div class="hero-icon-bubble">
              <Heart :size="22" class="hero-heart" />
            </div>
            <div class="hero-text">
              <h1 class="card-main-title">Support WATZEM</h1>
              <p class="hero-desc">
                100% free cinema streaming. Voluntary support helps fund scrapers, mirrors, and active updates.
              </p>
            </div>
          </div>

          <!-- Segmented Tab Switcher -->
          <div class="support-tab-bar" role="tablist">
            <button
              class="tab-pill-btn"
              :class="{ active: activeSupportTab === 'share' }"
              data-nav-item="true"
              tabindex="0"
              role="tab"
              :aria-selected="activeSupportTab === 'share'"
              @click="selectTab('share')"
            >
              <Share2 :size="13" />
              <span>Share App</span>
              <span class="pill-badge-free">Free</span>
            </button>

            <button
              v-if="config.coffee.enabled"
              class="tab-pill-btn"
              :class="{ active: activeSupportTab === 'coffee' }"
              data-nav-item="true"
              tabindex="0"
              role="tab"
              :aria-selected="activeSupportTab === 'coffee'"
              @click="selectTab('coffee')"
            >
              <Coffee :size="13" />
              <span>Coffee</span>
            </button>

            <button
              v-if="config.cryptoWallets.length > 0"
              class="tab-pill-btn"
              :class="{ active: activeSupportTab === 'crypto' }"
              data-nav-item="true"
              tabindex="0"
              role="tab"
              :aria-selected="activeSupportTab === 'crypto'"
              @click="selectTab('crypto')"
            >
              <Sparkles :size="13" />
              <span>Crypto</span>
            </button>
          </div>

          <div class="card-divider"></div>

          <!-- Tab 1: Share App (Free Community Support) -->
          <div v-if="activeSupportTab === 'share'" class="tab-content-pane">
            <p class="pane-desc">
              Share WATZEM with your friends, family, and movie groups! Every new viewer keeps our free servers alive.
            </p>

            <div class="share-actions-row">
              <button 
                class="action-btn share-primary-btn"
                data-nav-item="true"
                tabindex="0"
                @click="handleShareApp"
              >
                <Share2 :size="15" />
                <span>Share WATZEM App</span>
              </button>

              <button 
                class="action-btn share-copy-btn"
                :class="{ copied: isCopiedAppLink }"
                data-nav-item="true"
                tabindex="0"
                @click="handleCopyAppLink"
              >
                <Check v-if="isCopiedAppLink" :size="15" />
                <Copy v-else :size="15" />
                <span>{{ isCopiedAppLink ? 'Link Copied!' : 'Copy Link' }}</span>
              </button>
            </div>

            <!-- Social Quick Share Shortcuts -->
            <div class="social-share-chips">
              <span class="social-chips-label">Quick Share:</span>
              <div class="chips-row">
                <button class="social-chip-btn whatsapp" title="Share via WhatsApp" @click="shareToSocial('whatsapp')">
                  <MessageCircle :size="12" />
                  <span>WhatsApp</span>
                </button>
                <button class="social-chip-btn telegram" title="Share via Telegram" @click="shareToSocial('telegram')">
                  <Send :size="12" />
                  <span>Telegram</span>
                </button>
                <button class="social-chip-btn twitter" title="Share on X" @click="shareToSocial('twitter')">
                  <span>X</span>
                </button>
                <button class="social-chip-btn facebook" title="Share on Facebook" @click="shareToSocial('facebook')">
                  <span>Facebook</span>
                </button>
              </div>
            </div>
          </div>

          <!-- Tab 2: Buy Us a Coffee -->
          <div v-else-if="activeSupportTab === 'coffee'" class="tab-content-pane">
            <p class="pane-desc">
              Buy us a coffee to keep development and scraper maintenance going strong. Supports Credit Card, Apple Pay, and Google Pay.
            </p>

            <button 
              class="action-btn coffee-btn"
              data-nav-item="true"
              tabindex="0"
              @click="openCoffeeLink"
            >
              <Coffee :size="16" />
              <span>{{ config.coffee.name }}</span>
              <ExternalLink :size="13" class="link-arrow" />
            </button>
          </div>

          <!-- Tab 3: Cryptocurrency Tips -->
          <div v-else-if="activeSupportTab === 'crypto'" class="tab-content-pane">
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
              </button>
            </div>

            <!-- Active Wallet Display -->
            <div class="wallet-details-box" v-if="activeWallet">
              <div class="qr-col">
                <div class="qr-frame" @click="handleCopy(activeWallet)" title="Click to copy address">
                  <img 
                    v-if="qrCodes[activeWallet.id]"
                    :src="qrCodes[activeWallet.id]"
                    :alt="`${activeWallet.symbol} QR Code`"
                    class="qr-img"
                  />
                  <div v-else class="qr-loading">
                    <QrCode :size="22" class="spin-slow" />
                    <span>Generating QR...</span>
                  </div>
                </div>
                <span class="qr-hint">Scan with mobile wallet</span>
              </div>

              <div class="info-col">
                <div class="network-badge-row">
                  <span class="network-tag">{{ activeWallet.network }}</span>
                  <span class="symbol-tag" :style="{ color: activeWallet.badgeColor || '#f59e0b' }">
                    {{ activeWallet.name }} ({{ activeWallet.symbol }})
                  </span>
                </div>

                <div class="address-display-row" @click="handleCopy(activeWallet)" title="Click to copy">
                  <code class="address-text">{{ activeWallet.address }}</code>
                </div>

                <button 
                  class="action-btn copy-btn"
                  :class="{ copied: copiedId === activeWallet.id }"
                  data-nav-item="true"
                  tabindex="0"
                  @click="handleCopy(activeWallet)"
                >
                  <Check v-if="copiedId === activeWallet.id" :size="15" />
                  <Copy v-else :size="15" />
                  <span>{{ copiedId === activeWallet.id ? 'Address Copied!' : 'Copy Wallet Address' }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Trust & Privacy Footnote -->
        <footer class="tips-footer-note">
          <ShieldCheck :size="14" class="note-shield" />
          <span>WATZEM never collects payment info • Voluntary & non-recurring</span>
        </footer>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* Page Layout */
.tips-page {
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
.tips-header-bar {
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
  background: rgba(244, 63, 94, 0.12);
  border: 1px solid rgba(244, 63, 94, 0.3);
  color: #fb7185;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 700;
}

.heart-pulse-icon {
  animation: heartPulse 1.8s infinite ease-in-out;
}

@keyframes heartPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.22); }
}

/* Main Content Area */
.tips-content {
  position: relative;
  z-index: 10;
  flex: 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.25rem max(clamp(1rem, 3vw, 2.5rem), var(--safe-right, 0px)) max(calc(var(--safe-bottom, 0px) + 2rem), 2.5rem) max(clamp(1rem, 3vw, 2.5rem), var(--safe-left, 0px));
}

.tips-container {
  width: 100%;
  max-width: 540px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* Single Unified Master Card */
.tips-unified-card {
  position: relative;
  border-radius: 18px;
  background: rgba(14, 14, 22, 0.82);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(244, 63, 94, 0.2);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.45);
  padding: 1.25rem 1.35rem;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.card-glow-edge {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(244, 63, 94, 0.8), transparent);
}

/* Header Cluster */
.card-header-cluster {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.hero-icon-bubble {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f43f5e, #e11d48);
  display: grid;
  place-items: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(244, 63, 94, 0.45);
}

.card-main-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.01em;
  margin-bottom: 0.15rem;
}

.hero-desc {
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.68);
  line-height: 1.4;
}

/* Tab Bar */
.support-tab-bar {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background: rgba(8, 8, 14, 0.65);
  padding: 4px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.tab-pill-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  padding: 7px 10px;
  border-radius: 9px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.65);
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-pill-btn:hover {
  color: #ffffff;
}

.tab-pill-btn.active {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.pill-badge-free {
  font-size: 0.6rem;
  font-weight: 800;
  text-transform: uppercase;
  background: rgba(52, 211, 153, 0.2);
  color: #34d399;
  padding: 1px 5px;
  border-radius: 4px;
}

.card-divider {
  width: 100%;
  height: 1px;
  background: rgba(255, 255, 255, 0.07);
}

/* Tab Content Pane */
.tab-content-pane {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.pane-desc {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.72);
  line-height: 1.45;
}

/* Action Buttons */
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

.share-actions-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.45rem;
}

@media (min-width: 440px) {
  .share-actions-row {
    grid-template-columns: 1.3fr 1fr;
  }
}

.share-primary-btn {
  background: linear-gradient(135deg, #a855f7, #7c3aed);
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(168, 85, 247, 0.3);
}

.share-primary-btn:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.share-copy-btn {
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: #ffffff;
}

.share-copy-btn:hover {
  background: rgba(255, 255, 255, 0.12);
}

.share-copy-btn.copied {
  background: rgba(16, 185, 129, 0.2);
  border-color: rgba(16, 185, 129, 0.45);
  color: #34d399;
}

/* Social Quick Share Chips */
.social-share-chips {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  flex-wrap: wrap;
  padding-top: 0.35rem;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.social-chips-label {
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: rgba(255, 255, 255, 0.4);
}

.chips-row {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.social-chip-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.09);
  color: rgba(255, 255, 255, 0.75);
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
}

.social-chip-btn:hover {
  transform: translateY(-1px);
  color: #ffffff;
}

.social-chip-btn.whatsapp:hover {
  background: rgba(37, 211, 102, 0.18);
  border-color: rgba(37, 211, 102, 0.4);
  color: #4ade80;
}

.social-chip-btn.telegram:hover {
  background: rgba(0, 136, 204, 0.18);
  border-color: rgba(0, 136, 204, 0.4);
  color: #38bdf8;
}

.social-chip-btn.twitter:hover {
  background: rgba(29, 155, 240, 0.18);
  border-color: rgba(29, 155, 240, 0.4);
  color: #60a5fa;
}

.social-chip-btn.facebook:hover {
  background: rgba(24, 119, 242, 0.18);
  border-color: rgba(24, 119, 242, 0.4);
  color: #818cf8;
}

/* Coffee Btn */
.coffee-btn {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #07070a;
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.35);
}

.coffee-btn:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

/* Wallet Pills */
.wallet-pills {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.wallet-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.74rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s ease;
}

.wallet-pill-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.wallet-pill-btn.active {
  background: rgba(245, 158, 11, 0.18);
  border-color: #f59e0b;
  color: #fbbf24;
  box-shadow: 0 0 10px rgba(245, 158, 11, 0.25);
}

.pill-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.pill-symbol {
  font-weight: 800;
}

/* Wallet Details Box */
.wallet-details-box {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.85rem;
  background: rgba(7, 7, 12, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 12px;
  padding: 0.85rem;
}

@media (min-width: 480px) {
  .wallet-details-box {
    grid-template-columns: 120px 1fr;
    align-items: center;
  }
}

.qr-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
}

.qr-frame {
  width: 110px;
  height: 110px;
  background: #ffffff;
  border-radius: 9px;
  padding: 5px;
  display: grid;
  place-items: center;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.5);
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
  gap: 0.3rem;
  color: #08080c;
  font-size: 0.68rem;
  font-weight: 600;
}

.qr-hint {
  font-size: 0.64rem;
  color: rgba(255, 255, 255, 0.45);
}

.info-col {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.network-badge-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.network-tag {
  font-size: 0.62rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.85);
}

.symbol-tag {
  font-size: 0.74rem;
  font-weight: 700;
}

.address-display-row {
  background: rgba(0, 0, 0, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 0.5rem;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.address-display-row:hover {
  border-color: rgba(245, 158, 11, 0.5);
}

.address-text {
  font-family: var(--font-mono, monospace);
  font-size: 0.72rem;
  color: #fbbf24;
  word-break: break-all;
  line-height: 1.35;
  user-select: all;
  display: block;
}

.copy-btn {
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: #ffffff;
}

.copy-btn:hover {
  background: rgba(255, 255, 255, 0.12);
}

.copy-btn.copied {
  background: rgba(16, 185, 129, 0.2);
  border-color: rgba(16, 185, 129, 0.45);
  color: #34d399;
}

/* Footer Note */
.tips-footer-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0.4rem;
  color: rgba(255, 255, 255, 0.4);
  font-size: 0.7rem;
  text-align: center;
}

.note-shield {
  color: #10b981;
  flex-shrink: 0;
}
</style>
