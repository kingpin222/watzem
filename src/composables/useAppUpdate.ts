import { ref, computed } from 'vue';
import { Capacitor } from '@capacitor/core';
import { App as CapApp } from '@capacitor/app';
import { API_SERVER_URL, UPDATE_SERVER_URL, getUpdateEndpoint } from '../config/api';

export interface AppUpdateInfo {
  latestVersion: string;
  versionCode: number;
  minSupportedVersion: string;
  releaseDate: string;
  title: string;
  releaseNotes: string[];
  downloadUrl: string;
  fileSize: string;
  mandatory: boolean;
}

// Global shared state across components
const updateInfo = ref<AppUpdateInfo | null>(null);
const updateAvailable = ref(false);
const isChecking = ref(false);
const showUpdateModal = ref(false);
const isDownloading = ref(false);
const currentVersion = ref('1.0.0');
const currentBuild = ref(1);
const error = ref<string | null>(null);
const lastChecked = ref<number | null>(null);

function compareSemver(v1: string, v2: string): number {
  const parts1 = v1.replace(/^v/i, '').split('.').map(n => parseInt(n, 10) || 0);
  const parts2 = v2.replace(/^v/i, '').split('.').map(n => parseInt(n, 10) || 0);
  const maxLen = Math.max(parts1.length, parts2.length);

  for (let i = 0; i < maxLen; i++) {
    const num1 = parts1[i] ?? 0;
    const num2 = parts2[i] ?? 0;
    if (num1 > num2) return 1;
    if (num1 < num2) return -1;
  }
  return 0;
}

export function useAppUpdate() {
  const isNative = computed(() => {
    try {
      return Capacitor.isNativePlatform();
    } catch {
      return false;
    }
  });

  /**
   * Initializes local version from Capacitor App plugin
   */
  const initAppInfo = async () => {
    try {
      if (isNative.value) {
        const info = await CapApp.getInfo();
        currentVersion.value = info.version || '1.0.0';
        currentBuild.value = parseInt(info.build || '1', 10) || 1;
      }
    } catch (e) {
      console.warn('Failed to retrieve native app info:', e);
    }
  };

  /**
   * Check for updates against the backend API
   */
  const checkForUpdate = async (options: { manual?: boolean; force?: boolean } = {}) => {
    const { manual = false, force = false } = options;

    // Check test flag ?test_update=1
    const urlParams = typeof window !== 'undefined' ? new URLSearchParams(window.location.search) : null;
    const testUpdate = urlParams ? urlParams.get('test_update') === '1' : false;

    if (isChecking.value) return;
    isChecking.value = true;
    error.value = null;

    try {
      await initAppInfo();

      // In standalone mode without an update server, skip network check
      if (!UPDATE_SERVER_URL && !API_SERVER_URL && !testUpdate) {
        isChecking.value = false;
        return;
      }

      // Query version endpoint (from Cloudflare edge / worker / raw CDN / local proxy)
      const endpoint = getUpdateEndpoint();
      const separator = endpoint.includes('?') ? '&' : '?';
      const res = await fetch(`${endpoint}${separator}t=${Date.now()}`);

      if (!res.ok) {
        throw new Error(`Server returned ${res.status}`);
      }

      const data: AppUpdateInfo = await res.json();
      updateInfo.value = data;
      lastChecked.value = Date.now();

      // Compare versions
      const hasCodeUpdate = data.versionCode > currentBuild.value;
      const hasSemverUpdate = compareSemver(data.latestVersion, currentVersion.value) > 0;
      const hasNewVersion = testUpdate || hasCodeUpdate || hasSemverUpdate;

      updateAvailable.value = hasNewVersion;

      if (hasNewVersion) {
        // If mandatory, modal must always show and cannot be skipped
        if (data.mandatory) {
          showUpdateModal.value = true;
        } else {
          const dismissedVersion = typeof sessionStorage !== 'undefined'
            ? sessionStorage.getItem('watzem_dismissed_update_version')
            : null;

          if (manual || force || dismissedVersion !== data.latestVersion) {
            showUpdateModal.value = true;
          }
        }
      } else if (manual) {
        // No update available
        showUpdateModal.value = false;
      }
    } catch (err: any) {
      console.warn('Update check failed:', err);
      error.value = err.message || 'Failed to check for updates';

      if (testUpdate) {
        // Provide mock data if offline/testing
        updateInfo.value = {
          latestVersion: '1.1.0',
          versionCode: 2,
          minSupportedVersion: '1.0.0',
          releaseDate: '2026-09-24',
          title: 'WATZEM Update Required',
          releaseNotes: [
            'Critical update required to continue streaming',
            'Smoother 1080p and 4K stream playback performance',
            'Refined player controls and background playback',
            'General security & stability updates'
          ],
          downloadUrl: '/download/app',
          fileSize: '3.9 MB',
          mandatory: true
        };
        updateAvailable.value = true;
        showUpdateModal.value = true;
      }
    } finally {
      isChecking.value = false;
    }
  };

  /**
   * Start downloading and installing the update
   */
  const startUpdate = () => {
    if (!updateInfo.value) return;
    isDownloading.value = true;

    try {
      const rawUrl = updateInfo.value.downloadUrl || '/download/app';
      const baseServer = UPDATE_SERVER_URL || API_SERVER_URL;
      const fullUrl = (rawUrl.startsWith('http://') || rawUrl.startsWith('https://'))
        ? rawUrl
        : (baseServer ? `${baseServer.replace(/\/+$/, '')}${rawUrl.startsWith('/') ? '' : '/'}${rawUrl}` : rawUrl);

      // Trigger download
      const link = document.createElement('a');
      link.href = fullUrl;
      link.setAttribute('download', `WATZEM-v${updateInfo.value.latestVersion}.apk`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      // On Android native, also attempt window.open to prompt system download manager
      if (isNative.value && typeof window !== 'undefined') {
        window.open(fullUrl, '_system');
      }

      // Reset download animation state after a brief delay
      setTimeout(() => {
        isDownloading.value = false;
      }, 4000);
    } catch (e) {
      console.error('Failed to trigger update download:', e);
      isDownloading.value = false;
    }
  };

  /**
   * Dismiss the update modal for the current session (Only for non-mandatory updates)
   */
  const dismissModal = () => {
    if (updateInfo.value?.mandatory) {
      // Mandatory updates cannot be dismissed
      return;
    }
    if (updateInfo.value?.latestVersion && typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem('watzem_dismissed_update_version', updateInfo.value.latestVersion);
    }
    showUpdateModal.value = false;
  };

  const openModal = () => {
    showUpdateModal.value = true;
  };

  return {
    updateInfo,
    updateAvailable,
    isChecking,
    showUpdateModal,
    isDownloading,
    currentVersion,
    currentBuild,
    error,
    lastChecked,
    checkForUpdate,
    startUpdate,
    dismissModal,
    openModal
  };
}
