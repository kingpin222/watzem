/**
 * apiCache — High-performance two-tiered client-side cache (L1 Memory + L2 SessionStorage)
 * with concurrent in-flight Promise deduplication.
 */

interface CacheEntry<T> {
  data: T;
  timestamp: number;
  ttl: number; // in milliseconds
}

// In-Memory L1 Cache
const memoryCache = new Map<string, CacheEntry<unknown>>();

// In-flight pending requests deduplication map
const inFlightRequests = new Map<string, Promise<unknown>>();

// Helper to safely access sessionStorage
function getFromSession<T>(key: string): CacheEntry<T> | null {
  if (typeof window === 'undefined' || !window.sessionStorage) return null;
  try {
    const raw = window.sessionStorage.getItem(`api_cache_${key}`);
    if (!raw) return null;
    return JSON.parse(raw) as CacheEntry<T>;
  } catch {
    return null;
  }
}

function saveToSession<T>(key: string, entry: CacheEntry<T>): void {
  if (typeof window === 'undefined' || !window.sessionStorage) return;
  try {
    window.sessionStorage.setItem(`api_cache_${key}`, JSON.stringify(entry));
  } catch {
    // Storage quota might be exceeded, clear older cached api items
    try {
      for (let i = 0; i < window.sessionStorage.length; i++) {
        const k = window.sessionStorage.key(i);
        if (k && k.startsWith('api_cache_')) {
          window.sessionStorage.removeItem(k);
          break;
        }
      }
    } catch {
      // Ignore
    }
  }
}

/**
 * Standard default TTLs (in milliseconds) by category
 */
export const CACHE_TTLS = {
  SEARCH: 5 * 60 * 1000,           // 5 minutes
  MEDIA_LIST: 15 * 60 * 1000,      // 15 minutes
  TRENDING_SEARCHES: 30 * 60 * 1000,// 30 minutes
  TORRENTS: 20 * 60 * 1000,        // 20 minutes
  DETAILS: 60 * 60 * 1000,         // 1 hour (24 hours in L2)
  CREDITS: 60 * 60 * 1000,         // 1 hour
  TV_DETAILS: 60 * 60 * 1000,      // 1 hour
  SEASON_DETAILS: 60 * 60 * 1000,  // 1 hour
  COLLECTION: 4 * 60 * 60 * 1000   // 4 hours
};

export class ApiCache {
  /**
   * Get cached data if valid from L1 (memory) or L2 (sessionStorage)
   */
  static get<T>(key: string): T | null {
    const now = Date.now();

    // 1. Check L1 Memory Cache
    const memEntry = memoryCache.get(key) as CacheEntry<T> | undefined;
    if (memEntry) {
      if (now - memEntry.timestamp < memEntry.ttl) {
        return memEntry.data;
      }
      memoryCache.delete(key);
    }

    // 2. Check L2 SessionStorage Cache
    const sessionEntry = getFromSession<T>(key);
    if (sessionEntry) {
      if (now - sessionEntry.timestamp < sessionEntry.ttl) {
        // Hydrate L1 from L2
        memoryCache.set(key, sessionEntry);
        return sessionEntry.data;
      }
      try {
        window.sessionStorage.removeItem(`api_cache_${key}`);
      } catch {
        // Ignore
      }
    }

    return null;
  }

  /**
   * Set cached data into both L1 (Memory) and L2 (SessionStorage)
   */
  static set<T>(key: string, data: T, ttl: number = CACHE_TTLS.MEDIA_LIST): void {
    const entry: CacheEntry<T> = {
      data,
      timestamp: Date.now(),
      ttl
    };

    // Keep memory cache under 300 entries
    if (memoryCache.size > 300) {
      const oldest = memoryCache.keys().next().value;
      if (oldest) memoryCache.delete(oldest);
    }

    memoryCache.set(key, entry as CacheEntry<unknown>);
    saveToSession(key, entry);
  }

  /**
   * Execute or reuse a fetch request with caching and in-flight deduplication
   */
  static async fetchWithCache<T>(
    cacheKey: string,
    fetcher: () => Promise<T>,
    ttl: number = CACHE_TTLS.MEDIA_LIST
  ): Promise<T> {
    // 1. Check if already cached
    const cached = ApiCache.get<T>(cacheKey);
    if (cached !== null) {
      return cached;
    }

    // 2. Check if identical request is currently in-flight
    if (inFlightRequests.has(cacheKey)) {
      return (await inFlightRequests.get(cacheKey)) as T;
    }

    // 3. Initiate new request and track in-flight
    const requestPromise = (async () => {
      try {
        const result = await fetcher();
        // Do not cache empty arrays (prevents caching failed/transient empty results)
        if (!Array.isArray(result) || result.length > 0) {
          ApiCache.set(cacheKey, result, ttl);
        }
        return result;
      } finally {
        inFlightRequests.delete(cacheKey);
      }
    })();

    inFlightRequests.set(cacheKey, requestPromise);
    return await requestPromise;
  }

  /**
   * Invalidate specific key or prefix
   */
  static invalidate(keyOrPrefix: string): void {
    for (const k of memoryCache.keys()) {
      if (k === keyOrPrefix || k.startsWith(keyOrPrefix)) {
        memoryCache.delete(k);
      }
    }
    if (typeof window !== 'undefined' && window.sessionStorage) {
      try {
        for (let i = window.sessionStorage.length - 1; i >= 0; i--) {
          const k = window.sessionStorage.key(i);
          if (k && (k === `api_cache_${keyOrPrefix}` || k.startsWith(`api_cache_${keyOrPrefix}`))) {
            window.sessionStorage.removeItem(k);
          }
        }
      } catch {
        // Ignore
      }
    }
  }

  /**
   * Clear all cached data
   */
  static clear(): void {
    memoryCache.clear();
    inFlightRequests.clear();
    if (typeof window !== 'undefined' && window.sessionStorage) {
      try {
        for (let i = window.sessionStorage.length - 1; i >= 0; i--) {
          const k = window.sessionStorage.key(i);
          if (k && k.startsWith('api_cache_')) {
            window.sessionStorage.removeItem(k);
          }
        }
      } catch {
        // Ignore
      }
    }
  }
}
