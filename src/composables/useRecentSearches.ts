/**
 * useRecentSearches — persists recent search queries to localStorage.
 * Stores up to MAX_ITEMS unique queries, newest first.
 */
import { ref } from 'vue';

const STORAGE_KEY = 'recent_searches';
const MAX_ITEMS = 10;

// Shared reactive state across all component instances
const recent = ref<string[]>(load());

function load(): string[] {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
  } catch {
    return [];
  }
}

function save(items: string[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch {
    // ignore
  }
}

export function useRecentSearches() {
  /** Push a query to the top of the list (deduplicates, trims to MAX_ITEMS) */
  const push = (query: string) => {
    const q = query.trim();
    if (!q || q.length < 2) return;
    // Remove existing occurrence (case-insensitive), then prepend
    const next = [q, ...recent.value.filter((r) => r.toLowerCase() !== q.toLowerCase())].slice(
      0,
      MAX_ITEMS
    );
    recent.value = next;
    save(next);
  };

  /** Remove a single query */
  const remove = (query: string) => {
    const next = recent.value.filter((r) => r !== query);
    recent.value = next;
    save(next);
  };

  /** Clear all recent searches */
  const clear = () => {
    recent.value = [];
    save([]);
  };

  return {
    recent,
    push,
    remove,
    clear
  };
}
