<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import {
  Heart,
  Coffee,
  Copy,
  Check,
  ExternalLink,
  X,
  Zap,
  QrCode
} from 'lucide-vue-next';
import QRCode from 'qrcode';
import { useMonetization } from '../composables/useMonetization';
import { CryptoWallet } from '../config/monetization';

const {
  isSupportModalOpen,
  closeSupportModal,
  copyAddress,
  openCoffeeLink,
  config
} = useMonetization();

const copiedId = ref<string | null>(null);
const selectedWalletId = ref<string>(config.cryptoWallets[0]?.id || 'btc');
const qrCodes = ref<Record<string, string>>({});
const isGeneratingQr = ref(false);

const activeWallet = computed(() => {
  return config.cryptoWallets.find(w => w.id === selectedWalletId.value) || config.cryptoWallets[0];
});

const generateAllQrs = async () => {
  isGeneratingQr.value = true;
  for (const wallet of config.cryptoWallets) {
    try {
      const url = await QRCode.toDataURL(wallet.address, {
        width: 180,
        margin: 1,
        color: {
          dark: '#0a0f1d',
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

onMounted(() => {
  generateAllQrs();
});

watch(isSupportModalOpen, (isOpen) => {
  if (isOpen && Object.keys(qrCodes.value).length === 0) {
    generateAllQrs();
  }
});

const handleCopy = async (wallet: CryptoWallet) => {
  await copyAddress(wallet);
  copiedId.value = wallet.id;
  setTimeout(() => {
    if (copiedId.value === wallet.id) {
      copiedId.value = null;
    }
  }, 2200);
};

const formatAddress = (address: string) => {
  if (!address) return '';
  if (address.length <= 16) return address;
  return `${address.slice(0, 8)}...${address.slice(-8)}`;
};
</script>

<template>
  <Teleport to="body">
    <Transition name="support-modal">
      <div v-if="isSupportModalOpen" class="support-modal-backdrop" @click.self="closeSupportModal">
        <div class="support-modal-dialog">
          <!-- Header -->
          <div class="modal-header">
            <div class="header-left">
              <div class="header-icon-box">
                <Heart :size="16" class="heart-icon" />
              </div>
              <div>
                <h2 class="modal-title font-display">Support WATZEM</h2>
                <p class="modal-subtitle">Keeps streaming fast, free, and ad-free.</p>
              </div>
            </div>
            <button class="modal-close-btn" aria-label="Close" @click="closeSupportModal">
              <X :size="16" />
            </button>
          </div>

          <!-- Quick Support (Buy Me a Coffee) -->
          <button
            v-if="config.coffee.enabled"
            class="coffee-btn"
            @click="openCoffeeLink"
          >
            <div class="coffee-icon-wrap">
              <Coffee :size="14" />
            </div>
            <div class="coffee-text-col">
              <span class="coffee-title font-display">Buy Me a Coffee</span>
              <span class="coffee-sub">Support via Cards, Apple Pay, Google Pay</span>
            </div>
            <ExternalLink :size="12" class="coffee-arrow" />
          </button>

          <!-- Crypto Tip Jar -->
          <div class="crypto-card">
            <!-- Tabs -->
            <div class="crypto-header-row">
              <div class="crypto-label">
                <Zap :size="12" class="zap-icon" />
                <span class="font-display">Crypto Tip Jar</span>
              </div>
              <div class="coin-pill-group" role="tablist">
                <button
                  v-for="wallet in config.cryptoWallets"
                  :key="wallet.id"
                  class="coin-pill font-mono"
                  :class="{ active: selectedWalletId === wallet.id }"
                  :style="selectedWalletId === wallet.id ? {
                    borderColor: `${wallet.badgeColor || '#f59e0b'}88`,
                    backgroundColor: `${wallet.badgeColor || '#f59e0b'}18`,
                    color: wallet.badgeColor || '#f59e0b'
                  } : {}"
                  @click="selectedWalletId = wallet.id"
                  role="tab"
                  :aria-selected="selectedWalletId === wallet.id"
                >
                  {{ wallet.symbol }}
                </button>
              </div>
            </div>

            <!-- Active Coin Content -->
            <div v-if="activeWallet" class="crypto-content-row">
              <!-- QR Code -->
              <div class="qr-col">
                <div class="qr-box">
                  <img
                    v-if="qrCodes[activeWallet.id]"
                    :src="qrCodes[activeWallet.id]"
                    :alt="`${activeWallet.name} QR Code`"
                    class="qr-img"
                    width="84"
                    height="84"
                  />
                  <div v-else class="qr-loading">
                    <QrCode :size="20" />
                  </div>
                </div>
                <span class="network-badge font-mono">{{ activeWallet.network }}</span>
              </div>

              <!-- Address & Copy -->
              <div class="details-col">
                <div class="coin-meta">
                  <span class="coin-name font-display">{{ activeWallet.name }}</span>
                  <span
                    class="coin-chip font-mono"
                    :style="{ color: activeWallet.badgeColor || '#f59e0b', backgroundColor: `${activeWallet.badgeColor || '#f59e0b'}18` }"
                  >
                    {{ activeWallet.symbol }}
                  </span>
                </div>

                <!-- Truncated Address Box -->
                <div class="address-box font-mono" :title="activeWallet.address" @click="handleCopy(activeWallet)">
                  <span class="address-text">{{ formatAddress(activeWallet.address) }}</span>
                </div>

                <!-- Copy CTA -->
                <button
                  class="copy-btn font-display"
                  :class="{ copied: copiedId === activeWallet.id }"
                  @click="handleCopy(activeWallet)"
                >
                  <Check v-if="copiedId === activeWallet.id" :size="13" class="check-icon" />
                  <Copy v-else :size="13" />
                  <span>{{ copiedId === activeWallet.id ? 'Copied to Clipboard' : 'Copy Address' }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.support-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.78);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.support-modal-dialog {
  position: relative;
  width: 100%;
  max-width: 380px;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  background: #0e121b;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  padding: 1rem;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.85), 0 0 30px rgba(245, 158, 11, 0.05);
  animation: modalEnter 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalEnter {
  from {
    opacity: 0;
    transform: scale(0.96) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* Header */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.header-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: rgba(244, 63, 94, 0.15);
  border: 1px solid rgba(244, 63, 94, 0.3);
  display: grid;
  place-items: center;
  color: #f43f5e;
  flex-shrink: 0;
}

.modal-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
}

.modal-subtitle {
  font-size: 0.72rem;
  color: var(--text-secondary);
  line-height: 1.25;
  margin-top: 1px;
}

.modal-close-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: var(--text-secondary);
  display: grid;
  place-items: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.modal-close-btn:hover {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

/* Buy Me a Coffee Action Button */
.coffee-btn {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.55rem 0.75rem;
  border-radius: 12px;
  border: 1px solid rgba(255, 221, 0, 0.18);
  background: rgba(255, 221, 0, 0.04);
  cursor: pointer;
  transition: all 0.18s ease;
  text-align: left;
  width: 100%;
}

.coffee-btn:hover {
  background: rgba(255, 221, 0, 0.09);
  border-color: rgba(255, 221, 0, 0.35);
  transform: translateY(-1px);
}

.coffee-icon-wrap {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(255, 221, 0, 0.15);
  color: #ffdd00;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.coffee-text-col {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.coffee-title {
  font-size: 0.8rem;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
}

.coffee-sub {
  font-size: 0.64rem;
  color: var(--text-muted);
  line-height: 1.2;
  margin-top: 1px;
}

.coffee-arrow {
  color: var(--text-muted);
  flex-shrink: 0;
  transition: transform 0.18s ease;
}

.coffee-btn:hover .coffee-arrow {
  transform: translate(2px, -2px);
  color: #ffdd00;
}

/* Crypto Card */
.crypto-card {
  background: rgba(255, 255, 255, 0.025);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.crypto-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.crypto-label {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.76rem;
  font-weight: 700;
  color: var(--text-secondary);
}

.zap-icon {
  color: #f59e0b;
}

.coin-pill-group {
  display: flex;
  gap: 0.3rem;
}

.coin-pill {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 0.68rem;
  font-weight: 700;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.15s ease;
}

.coin-pill:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.15);
}

/* Crypto Content Row */
.crypto-content-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.qr-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
  flex-shrink: 0;
}

.qr-box {
  background: #ffffff;
  padding: 4px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-img {
  display: block;
  border-radius: 4px;
  width: 84px;
  height: 84px;
}

.qr-loading {
  width: 84px;
  height: 84px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.network-badge {
  font-size: 0.58rem;
  color: var(--text-muted);
  background: rgba(255, 255, 255, 0.04);
  padding: 1px 5px;
  border-radius: 4px;
  max-width: 84px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}

.details-col {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  flex: 1;
  min-width: 0;
}

.coin-meta {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.coin-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: #fff;
}

.coin-chip {
  font-size: 0.6rem;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 4px;
}

.address-box {
  background: rgba(0, 0, 0, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 0.35rem 0.55rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.address-box:hover {
  background: rgba(0, 0, 0, 0.65);
  border-color: rgba(255, 255, 255, 0.18);
}

.address-text {
  font-size: 0.68rem;
  color: rgba(255, 255, 255, 0.8);
  display: block;
  user-select: all;
  letter-spacing: -0.01em;
}

.copy-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  padding: 0.45rem 0.65rem;
  border-radius: 8px;
  font-size: 0.74rem;
  font-weight: 600;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
  cursor: pointer;
  transition: all 0.15s ease;
}

.copy-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.copy-btn.copied {
  background: rgba(16, 185, 129, 0.18) !important;
  border-color: rgba(52, 211, 153, 0.4) !important;
  color: #34d399 !important;
}

.check-icon {
  color: #34d399;
}

/* Transitions */
.support-modal-enter-active,
.support-modal-leave-active {
  transition: opacity 0.2s ease;
}

.support-modal-enter-from,
.support-modal-leave-to {
  opacity: 0;
}

@media (orientation: landscape) {
  .support-modal-backdrop {
    padding: 0.5rem 1rem;
  }
  .support-modal-dialog {
    max-height: 92vh;
    overflow-y: auto;
    padding: 0.75rem 1rem;
    gap: 0.5rem;
  }
}
</style>
