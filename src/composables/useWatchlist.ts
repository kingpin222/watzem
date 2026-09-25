import { ref } from 'vue';
import { MediaItem } from '../types';

const STORAGE_KEY = 'watzem_watchlist_v1';
const LEGACY_STORAGE_KEY = 'yify_watchlist_v1';

// Shared reactive state
const watchlist = ref<MediaItem[]>(loadInitialWatchlist());

function loadInitialWatchlist(): MediaItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY) || localStorage.getItem(LEGACY_STORAGE_KEY);
    if (!raw) return [];
    return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to load watchlist from localStorage', e);
    return [];
  }
}

function persistWatchlist() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(watchlist.value));
  } catch (e) {
    console.error('Failed to save watchlist to localStorage', e);
  }
}

export function useWatchlist() {
  const isInWatchlist = (id: number): boolean => {
    return watchlist.value.some((item) => item.id === id);
  };

  const toggleWatchlist = (item: MediaItem): boolean => {
    const index = watchlist.value.findIndex((m) => m.id === item.id);
    if (index >= 0) {
      watchlist.value.splice(index, 1);
      persistWatchlist();
      return false; // Removed
    } else {
      watchlist.value.unshift(item);
      persistWatchlist();
      return true; // Added
    }
  };

  const removeFromWatchlist = (id: number) => {
    watchlist.value = watchlist.value.filter((m) => m.id !== id);
    persistWatchlist();
  };

  return {
    watchlist,
    isInWatchlist,
    toggleWatchlist,
    removeFromWatchlist
  };
}
