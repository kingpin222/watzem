import { ref, computed } from 'vue';
import { Capacitor } from '@capacitor/core';

const isDismissed = ref(
  typeof sessionStorage !== 'undefined'
    ? sessionStorage.getItem('watzem_apk_prompt_dismissed') === 'true'
    : false
);

const showInstallGuide = ref(false);

export function useAndroidApp() {
  const isAndroidDevice = computed(() => {
    if (typeof window === 'undefined' || typeof navigator === 'undefined') return false;
    // Allow URL query ?test_android=1 or localStorage for easy previewing/testing in dev
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('test_android') === '1' || urlParams.get('android') === '1') {
      return true;
    }
    if (typeof localStorage !== 'undefined' && localStorage.getItem('simulate_android_web') === '1') {
      return true;
    }
    return /android/i.test(navigator.userAgent || '');
  });

  const isNative = computed(() => {
    try {
      return Capacitor.isNativePlatform();
    } catch {
      return false;
    }
  });

  // Specifically: using Android device AND not running inside native APK
  const isAndroidWeb = computed(() => {
    return isAndroidDevice.value && !isNative.value;
  });

  const shouldShowPrompt = computed(() => {
    return isAndroidWeb.value && !isDismissed.value;
  });

  const dismissPrompt = () => {
    isDismissed.value = true;
    try {
      sessionStorage.setItem('watzem_apk_prompt_dismissed', 'true');
    } catch {
      // Ignore
    }
  };

  const reopenPrompt = () => {
    isDismissed.value = false;
  };

  const getApkDownloadUrl = () => {
    // Both /download/app and /install.apk point to the standalone APK installer
    return '/download/app';
  };

  const downloadApk = () => {
    const link = document.createElement('a');
    link.href = getApkDownloadUrl();
    link.setAttribute('download', 'WATZEM.apk');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showInstallGuide.value = true;
  };

  return {
    isAndroidDevice,
    isNative,
    isAndroidWeb,
    shouldShowPrompt,
    showInstallGuide,
    dismissPrompt,
    reopenPrompt,
    getApkDownloadUrl,
    downloadApk
  };
}
