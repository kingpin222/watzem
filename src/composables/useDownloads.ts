import { ref, computed } from 'vue';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { Capacitor } from '@capacitor/core';
import { MediaItem, MediaMode, DownloadedItem, TorrentItem, TorrentResponse } from '../types';
import { useHaptics } from './useHaptics';
import { useToast } from './useToast';
import { getApiBaseUrl, API_SERVER_URL } from '../config/api';

const STORAGE_KEY = 'watzem_downloads_v1';
const LEGACY_STORAGE_KEY = 'yify_downloads_v1';

const downloads = ref<DownloadedItem[]>(loadInitialDownloads());
const downloadingIds = ref<Set<string>>(new Set());

function loadInitialDownloads(): DownloadedItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY) || localStorage.getItem(LEGACY_STORAGE_KEY);
    if (!raw) return [];
    return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to load downloads from localStorage', e);
    return [];
  }
}

function persistDownloads() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(downloads.value));
  } catch (e) {
    console.error('Failed to save downloads to localStorage', e);
  }
}

function getItemKey(id: number, season?: number, episode?: number): string {
  if (season && episode) {
    return `${id}-S${season}E${episode}`;
  }
  return `${id}`;
}

async function blobToBase64(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = reject;
    reader.onload = () => {
      const result = reader.result as string;
      const base64 = result.includes(',') ? result.split(',')[1] : result;
      resolve(base64);
    };
    reader.readAsDataURL(blob);
  });
}

function triggerBrowserDownload(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.style.display = 'none';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => window.URL.revokeObjectURL(url), 3000);
}

export function useDownloads() {
  const { triggerSuccess, triggerLight, copyText } = useHaptics();
  const { showToast } = useToast();

  const isDownloaded = (id: number, season?: number, episode?: number): boolean => {
    return downloads.value.some((d) => {
      if (d.id !== id) return false;
      if (season && episode) {
        return d.season === season && d.episode === episode;
      }
      return true;
    });
  };

  const getDownloadedItem = (id: number, season?: number, episode?: number): DownloadedItem | undefined => {
    return downloads.value.find((d) => {
      if (d.id !== id) return false;
      if (season && episode) {
        return d.season === season && d.episode === episode;
      }
      return true;
    });
  };

  const isDownloading = (id: number, season?: number, episode?: number): boolean => {
    return downloadingIds.value.has(getItemKey(id, season, episode));
  };

  /**
   * Directly downloads .torrent file to device storage & downloads folder
   */
  const downloadToDeviceStorage = async (
    item: MediaItem,
    mode: MediaMode,
    options?: {
      torrent?: TorrentItem;
      season?: number;
      episode?: number;
    }
  ): Promise<{ success: boolean; fileName?: string; magnetUrl?: string }> => {
    const season = options?.season || 1;
    const episode = options?.episode || 1;
    const isTvShow = mode === 'tv' || item.media_type === 'tv';
    const key = getItemKey(item.id, isTvShow ? season : undefined, isTvShow ? episode : undefined);

    downloadingIds.value.add(key);
    triggerLight();

    const title = item.title || item.name || 'Movie';
    const cleanTitle = title.replace(/[/\\?%*:|"<>]/g, '_');
    const rawDate = item.release_date || item.first_air_date || '';
    const year = rawDate ? rawDate.substring(0, 4) : '';

    let selectedTorrent = options?.torrent;

    // If no specific torrent was provided, query API for best torrent
    if (!selectedTorrent) {
      if (!API_SERVER_URL) {
        downloadingIds.value.delete(key);
        showToast({ title: 'Downloads unavailable in standalone mode', type: 'info' });
        return { success: false };
      }
      try {
        const apiBase = getApiBaseUrl();
        const queryName = isTvShow ? `${title} S${String(season).padStart(2, '0')}` : title;
        const res = await fetch(`${apiBase}/torrents?name=${encodeURIComponent(queryName)}&year=${year}&mode=${mode}`);
        if (res.ok) {
          const data: TorrentResponse = await res.json();
          if (data.hits && data.hits.length > 0) {
            // Sort by seeds descending
            const sorted = [...data.hits].sort((a, b) => (b.seeds || 0) - (a.seeds || 0));
            selectedTorrent = sorted[0];
          }
        }
      } catch (err) {
        console.warn('Failed to fetch torrent list for storage download', err);
      }
    }

    const hash = selectedTorrent?.hash || '';
    const magnetUrl = selectedTorrent?.magnetUrl || '';
    const ext = hash ? 'torrent' : 'magnet';
    const fileName = `${cleanTitle}${year ? `_${year}` : ''}${isTvShow ? `_S${season}E${episode}` : ''}.${ext}`;
    let storagePath = 'Downloads folder';

    try {
      // Request download from server
      const apiBase = getApiBaseUrl();
      const downloadEndpoint = `${apiBase}/download/torrent?hash=${encodeURIComponent(hash)}&name=${encodeURIComponent(
        cleanTitle
      )}&magnet=${encodeURIComponent(magnetUrl)}`;

      const fileRes = await fetch(downloadEndpoint);
      if (!fileRes.ok) {
        throw new Error(`Download endpoint returned ${fileRes.status}`);
      }

      const fileBlob = await fileRes.blob();

      // If Capacitor Native platform (Android / iOS), save file to Documents
      if (Capacitor.isNativePlatform()) {
        try {
          const base64 = await blobToBase64(fileBlob);
          await Filesystem.writeFile({
            path: `Movies/${fileName}`,
            data: base64,
            directory: Directory.Documents,
            recursive: true
          });

          const uriRes = await Filesystem.getUri({
            directory: Directory.Documents,
            path: `Movies/${fileName}`
          });
          storagePath = uriRes.uri;
        } catch (nativeFsErr) {
          console.warn('Filesystem.writeFile failed, falling back to browser trigger', nativeFsErr);
        }
      }

      // Always trigger system download to ensure file is placed into user's Downloads directory
      triggerBrowserDownload(fileBlob, fileName);

      // Determine file size representation
      const sizeBytes = selectedTorrent?.bytes || (isTvShow ? 450 * 1024 * 1024 : 1.8 * 1024 * 1024 * 1024);
      const formattedSize =
        sizeBytes < 1024 * 1024 * 1024
          ? `${(sizeBytes / (1024 * 1024)).toFixed(0)} MB`
          : `${(sizeBytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;

      const streamUrl = isTvShow
        ? `https://vidsrc.mov/embed/tv/${item.id}/${season}/${episode}`
        : `https://vidsrc.mov/embed/movie/${item.id}`;

      // Remove existing duplicate if present
      downloads.value = downloads.value.filter((d) => {
        if (d.id !== item.id) return true;
        if (isTvShow) return !(d.season === season && d.episode === episode);
        return false;
      });

      const newDownload: DownloadedItem = {
        id: item.id,
        item,
        mode,
        season: isTvShow ? season : undefined,
        episode: isTvShow ? episode : undefined,
        downloadedAt: Date.now(),
        sizeBytes,
        formattedSize,
        status: 'completed',
        streamUrl,
        torrentHash: hash,
        magnetUrl,
        fileName,
        storagePath,
        seeds: selectedTorrent?.seeds,
        downloadType: 'storage_file'
      };

      downloads.value.unshift(newDownload);
      persistDownloads();
      await triggerSuccess();

      showToast({
        title: 'Downloaded to Storage',
        message: `Saved ${fileName} to device storage.`,
        type: 'success'
      });

      return { success: true, fileName, magnetUrl };
    } catch (error) {
      console.error('Error during device storage download:', error);
      showToast({
        title: 'Download Failed',
        message: 'Could not write file to device storage.',
        type: 'error'
      });
      return { success: false };
    } finally {
      downloadingIds.value.delete(key);
    }
  };

  /**
   * Launch native torrent client via magnet link & copy to clipboard
   */
  const openInTorrentClient = async (magnetUrl: string, title?: string): Promise<boolean> => {
    if (!magnetUrl) {
      showToast({
        title: 'Magnet Link Not Available',
        message: 'No magnet source found for this title.',
        type: 'warning'
      });
      return false;
    }

    try {
      // Copy magnet link to clipboard for convenience
      await copyText(magnetUrl);

      // Attempt to launch device torrent client (Flud, LibreTorrent, uTorrent, Folx)
      window.location.href = magnetUrl;

      showToast({
        title: 'Torrent Client Opened',
        message: title ? `Opening torrent client for ${title}` : 'Magnet link copied & opened.',
        type: 'success'
      });
      return true;
    } catch (err) {
      console.error('Failed to open magnet link', err);
      showToast({
        title: 'Magnet Copied',
        message: 'Magnet link copied to clipboard. Paste into your torrent app.',
        type: 'info'
      });
      return false;
    }
  };

  /**
   * Downloads the ACTUAL movie video file (MP4 / MKV) directly to device storage
   */
  const downloadActualMovieVideo = async (
    item: MediaItem,
    mode: MediaMode,
    options?: {
      torrent?: TorrentItem;
      season?: number;
      episode?: number;
    }
  ): Promise<{ success: boolean; fileName?: string; magnetUrl?: string }> => {
    const season = options?.season || 1;
    const episode = options?.episode || 1;
    const isTvShow = mode === 'tv' || item.media_type === 'tv';
    const key = getItemKey(item.id, isTvShow ? season : undefined, isTvShow ? episode : undefined);

    downloadingIds.value.add(key);
    triggerLight();

    const title = item.title || item.name || 'Movie';
    const cleanTitle = title.replace(/[/\\?%*:|"<>]/g, '_');
    const rawDate = item.release_date || item.first_air_date || '';
    const year = rawDate ? rawDate.substring(0, 4) : '';

    let selectedTorrent = options?.torrent;

    // Find best torrent if not supplied
    if (!selectedTorrent) {
      if (!API_SERVER_URL) {
        showToast({ title: 'Video downloads require an active server', type: 'info' });
        return { success: false };
      }
      try {
        const apiBase = getApiBaseUrl();
        const queryName = isTvShow ? `${title} S${String(season).padStart(2, '0')}` : title;
        const res = await fetch(`${apiBase}/torrents?name=${encodeURIComponent(queryName)}&year=${year}&mode=${mode}`);
        if (res.ok) {
          const data: TorrentResponse = await res.json();
          if (data.hits && data.hits.length > 0) {
            const sorted = [...data.hits].sort((a, b) => (b.seeds || 0) - (a.seeds || 0));
            selectedTorrent = sorted[0];
          }
        }
      } catch (err) {
        console.warn('Failed to fetch torrent list for video download', err);
      }
    }

    const hash = selectedTorrent?.hash || '';
    const magnetUrl = selectedTorrent?.magnetUrl || '';
    const fileName = `${cleanTitle}${year ? `_${year}` : ''}${isTvShow ? `_S${season}E${episode}` : ''}.mp4`;

    try {
      const apiBase = getApiBaseUrl();
      const videoDownloadUrl = `${apiBase}/download/video?hash=${encodeURIComponent(hash)}&name=${encodeURIComponent(
        cleanTitle
      )}&magnet=${encodeURIComponent(magnetUrl)}`;

      // Trigger real browser / device download of the MP4 file
      const a = document.createElement('a');
      a.href = videoDownloadUrl;
      a.download = fileName;
      a.style.display = 'none';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);

      const sizeBytes = selectedTorrent?.bytes || (isTvShow ? 450 * 1024 * 1024 : 1.8 * 1024 * 1024 * 1024);
      const formattedSize =
        sizeBytes < 1024 * 1024 * 1024
          ? `${(sizeBytes / (1024 * 1024)).toFixed(0)} MB`
          : `${(sizeBytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;

      const streamUrl = isTvShow
        ? `https://vidsrc.mov/embed/tv/${item.id}/${season}/${episode}`
        : `https://vidsrc.mov/embed/movie/${item.id}`;

      // Register downloaded video in list
      downloads.value = downloads.value.filter((d) => {
        if (d.id !== item.id) return true;
        if (isTvShow) return !(d.season === season && d.episode === episode);
        return false;
      });

      const newDownload: DownloadedItem = {
        id: item.id,
        item,
        mode,
        season: isTvShow ? season : undefined,
        episode: isTvShow ? episode : undefined,
        downloadedAt: Date.now(),
        sizeBytes,
        formattedSize,
        status: 'completed',
        streamUrl,
        torrentHash: hash,
        magnetUrl,
        fileName,
        storagePath: 'Downloads Folder',
        seeds: selectedTorrent?.seeds,
        downloadType: 'actual_video'
      };

      downloads.value.unshift(newDownload);
      persistDownloads();
      await triggerSuccess();

      showToast({
        title: 'Downloading Movie (MP4)',
        message: `Saving ${fileName} (${formattedSize}) directly to device storage.`,
        type: 'success'
      });

      return { success: true, fileName, magnetUrl };
    } catch (error) {
      console.error('Error initiating movie download:', error);
      showToast({
        title: 'Movie Download Failed',
        message: 'Could not start movie video download.',
        type: 'error'
      });
      return { success: false };
    } finally {
      downloadingIds.value.delete(key);
    }
  };

  /**
   * Default start download action (downloads actual movie MP4)
   */
  const startDownload = async (
    item: MediaItem,
    mode: MediaMode,
    season = 1,
    episode = 1
  ): Promise<void> => {
    await downloadActualMovieVideo(item, mode, { season, episode });
  };

  /**
   * Remove downloaded item and free up storage
   */
  const removeDownload = async (id: number, season?: number, episode?: number) => {
    triggerLight();
    const itemToDelete = downloads.value.find((d) => {
      if (d.id !== id) return false;
      if (season && episode) return d.season === season && d.episode === episode;
      return true;
    });

    if (itemToDelete && itemToDelete.fileName && Capacitor.isNativePlatform()) {
      try {
        await Filesystem.deleteFile({
          path: `Movies/${itemToDelete.fileName}`,
          directory: Directory.Documents
        });
      } catch {
        // File may have been removed manually, ignore
      }
    }

    downloads.value = downloads.value.filter((d) => {
      if (d.id !== id) return true;
      if (season && episode) {
        return !(d.season === season && d.episode === episode);
      }
      return false;
    });
    persistDownloads();

    showToast({
      title: 'Removed from Downloads',
      message: 'Item deleted from your download list.',
      type: 'info'
    });
  };

  const clearAllDownloads = () => {
    triggerLight();
    downloads.value = [];
    persistDownloads();
    showToast({
      title: 'Downloads Cleared',
      message: 'All downloaded items cleared.',
      type: 'info'
    });
  };

  const totalStorageFormatted = computed(() => {
    const totalBytes = downloads.value.reduce((acc, d) => acc + (d.sizeBytes || 0), 0);
    if (totalBytes === 0) return '0 MB';
    if (totalBytes < 1024 * 1024 * 1024) {
      return `${(totalBytes / (1024 * 1024)).toFixed(0)} MB`;
    }
    return `${(totalBytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
  });

  return {
    downloads,
    isDownloaded,
    getDownloadedItem,
    isDownloading,
    startDownload,
    downloadActualMovieVideo,
    downloadToDeviceStorage,
    openInTorrentClient,
    removeDownload,
    clearAllDownloads,
    totalStorageFormatted
  };
}
