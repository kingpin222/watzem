import { ref } from 'vue';
import { Clipboard } from '@capacitor/clipboard';
import { Browser } from '@capacitor/browser';
import { Capacitor } from '@capacitor/core';
import { useToast } from './useToast';
import { useHaptics } from './useHaptics';
import { MONETIZATION_CONFIG, CryptoWallet } from '../config/monetization';

const isSupportModalOpen = ref(false);

/**
 * Open external URL reliably across Android, iOS, and Web.
 * Uses Capacitor Browser (Chrome Custom Tabs / Safari View Controller) on native,
 * and reliable fallback mechanisms on desktop/mobile web.
 */
export async function openExternalUrl(url: string) {
  if (!url) return;

  // 1. Native mobile (Android APK & iOS)
  if (Capacitor.isNativePlatform()) {
    try {
      await Browser.open({
        url,
        windowName: '_system',
        toolbarColor: '#0f1017'
      });
      return;
    } catch (err) {
      console.warn('[useMonetization] Capacitor Browser.open error, trying web fallback:', err);
    }
  }

  // 2. Web browser: Anchor click with _blank
  try {
    const a = document.createElement('a');
    a.href = url;
    a.target = '_blank';
    a.rel = 'noopener noreferrer';
    a.setAttribute('data-allow-popup', 'true');
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      if (document.body.contains(a)) {
        document.body.removeChild(a);
      }
    }, 200);
    return;
  } catch (err) {
    console.warn('[useMonetization] Anchor click fallback failed:', err);
  }

  // 3. Fallback window.open
  try {
    if (typeof window !== 'undefined') {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  } catch (e) {
    console.error('[useMonetization] window.open failed:', e);
  }
}

export function useMonetization() {
  const { showToast } = useToast();
  const { triggerSuccess, triggerLight } = useHaptics();

  const openSupportModal = () => {
    triggerLight();
    isSupportModalOpen.value = true;
  };

  const closeSupportModal = () => {
    isSupportModalOpen.value = false;
  };

  const copyAddress = async (wallet: CryptoWallet) => {
    try {
      await Clipboard.write({ string: wallet.address });
      triggerSuccess();
      showToast({
        title: `${wallet.symbol} Address Copied!`,
        message: `${wallet.network}: ${wallet.address.slice(0, 10)}...${wallet.address.slice(-6)}`,
        type: 'success',
        duration: 3500
      });
    } catch {
      // Fallback for desktop browsers
      try {
        await navigator.clipboard.writeText(wallet.address);
        triggerSuccess();
        showToast({
          title: `${wallet.symbol} Address Copied!`,
          type: 'success',
          duration: 3000
        });
      } catch {
        showToast({
          title: 'Failed to copy address',
          type: 'error'
        });
      }
    }
  };

  const openCoffeeLink = async () => {
    triggerLight();
    await openExternalUrl(MONETIZATION_CONFIG.coffee.url);
  };

  return {
    isSupportModalOpen,
    openSupportModal,
    closeSupportModal,
    copyAddress,
    openCoffeeLink,
    openExternalUrl,
    config: MONETIZATION_CONFIG
  };
}
