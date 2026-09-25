import { MediaItem, MediaListResponse, TorrentResponse, TVDetailsResponse, SeasonDetailsResponse } from '../types.js';

const TMDB_API_KEY = '4ef0d7355d9ffb5151e987764708ce96';
const TMDB_BASE_URL = 'https://api.themoviedb.org/3';

// In-memory cache with individual expiration timestamps
interface CacheEntry<T> {
  data: T;
  expiresAt: number;
}

const cache = new Map<string, CacheEntry<unknown>>();
const inFlightRequests = new Map<string, Promise<unknown>>();
const MAX_CACHE_SIZE = 1000;

// Standard Server TTLs (ms)
export const SERVER_CACHE_TTLS = {
  SEARCH: 10 * 60 * 1000,           // 10 minutes
  MEDIA_LIST: 15 * 60 * 1000,       // 15 minutes
  TORRENTS: 30 * 60 * 1000,         // 30 minutes
  DETAILS: 2 * 60 * 60 * 1000,      // 2 hours
  CREDITS: 2 * 60 * 60 * 1000,      // 2 hours
  TV_DETAILS: 2 * 60 * 60 * 1000,   // 2 hours
  SEASON_DETAILS: 2 * 60 * 60 * 1000,// 2 hours
  COLLECTION: 6 * 60 * 60 * 1000,   // 6 hours
  TRENDING: 30 * 60 * 1000          // 30 minutes
};

function getCached<T>(key: string): T | null {
  const entry = cache.get(key);
  if (!entry) return null;
  if (Date.now() > entry.expiresAt) {
    cache.delete(key);
    return null;
  }
  return entry.data as T;
}

function setCache<T>(key: string, data: T, ttlMs: number = SERVER_CACHE_TTLS.MEDIA_LIST) {
  // Prune oldest if cache capacity reached
  if (cache.size >= MAX_CACHE_SIZE) {
    const oldestKey = cache.keys().next().value;
    if (oldestKey) cache.delete(oldestKey);
  }
  cache.set(key, { data, expiresAt: Date.now() + ttlMs });
}

// Background cleanup of expired entries every 5 minutes
const cleanupTimer = setInterval(() => {
  const now = Date.now();
  for (const [key, entry] of cache.entries()) {
    if (now > entry.expiresAt) {
      cache.delete(key);
    }
  }
}, 5 * 60 * 1000);
if (cleanupTimer.unref) cleanupTimer.unref();

async function fetchWithDedup<T>(key: string, fetcher: () => Promise<T>): Promise<T> {
  if (inFlightRequests.has(key)) {
    return (await inFlightRequests.get(key)) as T;
  }
  const promise = (async () => {
    try {
      return await fetcher();
    } finally {
      inFlightRequests.delete(key);
    }
  })();
  inFlightRequests.set(key, promise);
  return await promise;
}

async function requestJson<T>(url: string): Promise<T> {
  const res = await fetch(url, {
    headers: {
      'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148',
      'Accept': 'application/json, text/plain, */*'
    }
  });

  if (!res.ok) {
    throw new Error(`Upstream request failed: ${res.status} ${res.statusText}`);
  }

  return (await res.json()) as T;
}

export class YifyService {
  /**
   * Fetch movies or TV shows list (popular, trending, discover, top_rated/imdb, etc.)
   * Directly uses official TMDb / IMDb Top lists instead of yify proxy
   */
  static async getMediaList(params: Record<string, string | number | undefined>): Promise<MediaListResponse> {
    const searchParams = new URLSearchParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        searchParams.append(key, String(value));
      }
    }

    const cacheKey = `list:${searchParams.toString()}`;
    const cached = getCached<MediaListResponse>(cacheKey);
    if (cached) return cached;

    return fetchWithDedup(cacheKey, async () => {
      const apiType = String(params.api || 'popular');
      const mode = (String(params.mode || 'movie')) as 'movie' | 'tv';
      const page = parseInt(String(params.page || '1'), 10) || 1;

      try {
        // 1. IMDb Top List (or top_rated requested by user)
        if (apiType === 'imdb' || apiType === 'imdb_top' || apiType === 'top_rated') {
          if (mode === 'movie') {
            // Curated IMDb Top 250 list on TMDB (List ID: 634)
            const listUrl = `${TMDB_BASE_URL}/list/634?api_key=${TMDB_API_KEY}&page=${page}`;
            const raw = await requestJson<{ items?: MediaItem[]; results?: MediaItem[]; item_count?: number }>(listUrl);
            const items = (raw.items || raw.results || []).map((item) => ({
              ...item,
              media_type: 'movie' as const
            }));
            const result: MediaListResponse = {
              page,
              results: items,
              total_pages: Math.ceil((raw.item_count || 250) / 20),
              total_results: raw.item_count || 250
            };
            setCache(cacheKey, result, SERVER_CACHE_TTLS.MEDIA_LIST);
            return result;
          } else {
            // TV Top Rated mirroring IMDb Top TV (filtered by high vote count to eliminate obscure entries)
            const tvUrl = `${TMDB_BASE_URL}/discover/tv?api_key=${TMDB_API_KEY}&sort_by=vote_average.desc&vote_count.gte=1000&page=${page}`;
            const raw = await requestJson<MediaListResponse>(tvUrl);
            const items = (raw.results || []).map((item) => ({
              ...item,
              media_type: 'tv' as const
            }));
            const result: MediaListResponse = {
              page: raw.page || page,
              results: items,
              total_pages: raw.total_pages || 1,
              total_results: raw.total_results || 0
            };
            setCache(cacheKey, result, SERVER_CACHE_TTLS.MEDIA_LIST);
            return result;
          }
        }

        // 2. Popular (Mirrors IMDb MovieMeter popularity)
        if (apiType === 'popular') {
          const url = `${TMDB_BASE_URL}/${mode}/popular?api_key=${TMDB_API_KEY}&page=${page}`;
          const raw = await requestJson<MediaListResponse>(url);
          const result: MediaListResponse = {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: mode })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
          setCache(cacheKey, result, SERVER_CACHE_TTLS.MEDIA_LIST);
          return result;
        }

        // 3. Trending
        if (apiType === 'trending') {
          const timeWindow = String(params.time || 'week');
          const url = `${TMDB_BASE_URL}/trending/${mode}/${timeWindow}?api_key=${TMDB_API_KEY}&page=${page}`;
          const raw = await requestJson<MediaListResponse>(url);
          const result: MediaListResponse = {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: mode })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
          setCache(cacheKey, result, SERVER_CACHE_TTLS.MEDIA_LIST);
          return result;
        }

        // 4. In Theaters (now_playing)
        if (apiType === 'now_playing') {
          const url = `${TMDB_BASE_URL}/movie/now_playing?api_key=${TMDB_API_KEY}&page=${page}`;
          const raw = await requestJson<MediaListResponse>(url);
          const result: MediaListResponse = {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: 'movie' as const })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
          setCache(cacheKey, result, SERVER_CACHE_TTLS.MEDIA_LIST);
          return result;
        }

        // 5. Upcoming
        if (apiType === 'upcoming') {
          const url = `${TMDB_BASE_URL}/movie/upcoming?api_key=${TMDB_API_KEY}&page=${page}`;
          const raw = await requestJson<MediaListResponse>(url);
          const result: MediaListResponse = {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: 'movie' as const })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
          setCache(cacheKey, result, SERVER_CACHE_TTLS.MEDIA_LIST);
          return result;
        }

        // 6. On Air (TV)
        if (apiType === 'on_the_air') {
          const url = `${TMDB_BASE_URL}/tv/on_the_air?api_key=${TMDB_API_KEY}&page=${page}`;
          const raw = await requestJson<MediaListResponse>(url);
          const result: MediaListResponse = {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: 'tv' as const })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
          setCache(cacheKey, result, SERVER_CACHE_TTLS.MEDIA_LIST);
          return result;
        }

        // 7. Airing Today (TV)
        if (apiType === 'airing_today') {
          const url = `${TMDB_BASE_URL}/tv/airing_today?api_key=${TMDB_API_KEY}&page=${page}`;
          const raw = await requestJson<MediaListResponse>(url);
          const result: MediaListResponse = {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: 'tv' as const })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
          setCache(cacheKey, result, SERVER_CACHE_TTLS.MEDIA_LIST);
          return result;
        }

        // 8. Discover (genres, networks, providers)
        if (apiType === 'discover') {
          const qs = new URLSearchParams({
            api_key: TMDB_API_KEY,
            page: String(page),
            sort_by: String(params.sort || 'popularity.desc')
          });
          if (params.genres) qs.append('with_genres', String(params.genres));
          if (params.provider) {
            qs.append('with_watch_providers', String(params.provider));
            qs.append('watch_region', 'US');
          }
          if (params.network) qs.append('with_networks', String(params.network));
          if (params.company) qs.append('with_companies', String(params.company));
          if (params.year) {
            if (mode === 'movie') qs.append('primary_release_year', String(params.year));
            else qs.append('first_air_date_year', String(params.year));
          }
          if (params.year_gte) {
            const dateKey = mode === 'movie' ? 'primary_release_date.gte' : 'first_air_date.gte';
            qs.append(dateKey, `${params.year_gte}-01-01`);
          }
          if (params.year_lte) {
            const dateKey = mode === 'movie' ? 'primary_release_date.lte' : 'first_air_date.lte';
            qs.append(dateKey, `${params.year_lte}-12-31`);
          }

          const url = `${TMDB_BASE_URL}/discover/${mode}?${qs.toString()}`;
          const raw = await requestJson<MediaListResponse>(url);
          const result: MediaListResponse = {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: mode })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
          setCache(cacheKey, result, SERVER_CACHE_TTLS.MEDIA_LIST);
          return result;
        }

        // 9. Generic endpoint fallback
        const fallbackUrl = `${TMDB_BASE_URL}/${mode}/${apiType}?api_key=${TMDB_API_KEY}&page=${page}`;
        const raw = await requestJson<MediaListResponse>(fallbackUrl);
        const result: MediaListResponse = {
          page: raw.page || page,
          results: (raw.results || []).map((item) => ({ ...item, media_type: mode })),
          total_pages: raw.total_pages || 1,
          total_results: raw.total_results || 0
        };
        setCache(cacheKey, result, SERVER_CACHE_TTLS.MEDIA_LIST);
        return result;
      } catch (err) {
        console.warn(`Direct TMDb/IMDb request failed for api="${apiType}":`, err);
        return { page, results: [], total_pages: 1, total_results: 0 };
      }
    });
  }

  /**
   * Search movies or TV shows
   */
  static async search(query: string, mode: 'movie' | 'tv' = 'movie', page = 1): Promise<MediaListResponse> {
    const cacheKey = `search:${mode}:${query.toLowerCase().trim()}:${page}`;
    const cached = getCached<MediaListResponse>(cacheKey);
    if (cached) return cached;

    return fetchWithDedup(cacheKey, async () => {
      try {
        const url = `${TMDB_BASE_URL}/search/${mode}?api_key=${TMDB_API_KEY}&query=${encodeURIComponent(query)}&page=${page}`;
        const raw = await requestJson<MediaListResponse>(url);
        const result: MediaListResponse = {
          page: raw.page || page,
          results: (raw.results || []).map((item) => ({ ...item, media_type: mode })),
          total_pages: raw.total_pages || 1,
          total_results: raw.total_results || 0
        };
        setCache(cacheKey, result, SERVER_CACHE_TTLS.SEARCH);
        return result;
      } catch (err) {
        console.warn('Direct search failed:', err);
        return { page, results: [], total_pages: 1, total_results: 0 };
      }
    });
  }

  /**
   * Search torrents and magnet links for a given title
   */
  static async getTorrents(
    name: string,
    year: string | number = '',
    mode: 'movie' | 'tv' = 'movie',
    quality = 'all'
  ): Promise<TorrentResponse> {
    const cacheKey = `torrents:${mode}:${name.toLowerCase().trim()}:${year}:${quality}`;
    const cached = getCached<TorrentResponse>(cacheKey);
    if (cached) return cached;

    return fetchWithDedup(cacheKey, async () => {
      // Return empty torrent hits cleanly without external scraping
      const emptyData: TorrentResponse = { hits: [], total: 0, sources: [] };
      setCache(cacheKey, emptyData, SERVER_CACHE_TTLS.TORRENTS);
      return emptyData;
    });
  }

  /**
   * Get TV show details including available seasons
   */
  static async getTvDetails(id: number | string): Promise<TVDetailsResponse> {
    const cacheKey = `tv_details:${id}`;
    const cached = getCached<TVDetailsResponse>(cacheKey);
    if (cached) return cached;

    return fetchWithDedup(cacheKey, async () => {
      try {
        const url = `${TMDB_BASE_URL}/tv/${id}?api_key=${TMDB_API_KEY}`;
        const raw = await requestJson<{
          id: number;
          name: string;
          overview: string;
          number_of_seasons: number;
          number_of_episodes: number;
          seasons?: Array<{
            id: number;
            name: string;
            overview: string;
            season_number: number;
            episode_count: number;
            poster_path: string | null;
          }>;
        }>(url);
        const data: TVDetailsResponse = {
          id: raw.id,
          name: raw.name,
          overview: raw.overview,
          number_of_seasons: raw.number_of_seasons,
          number_of_episodes: raw.number_of_episodes,
          seasons: raw.seasons || []
        };
        setCache(cacheKey, data, SERVER_CACHE_TTLS.TV_DETAILS);
        return data;
      } catch (err) {
        console.warn('Direct tv_details failed:', err);
        throw err;
      }
    });
  }

  /**
   * Get TV show episodes for a specific season
   */
  static async getSeasonDetails(id: number | string, seasonNumber: number | string): Promise<SeasonDetailsResponse> {
    const cacheKey = `season_details:${id}:${seasonNumber}`;
    const cached = getCached<SeasonDetailsResponse>(cacheKey);
    if (cached) return cached;

    return fetchWithDedup(cacheKey, async () => {
      try {
        const url = `${TMDB_BASE_URL}/tv/${id}/season/${seasonNumber}?api_key=${TMDB_API_KEY}`;
        const raw = await requestJson<{
          id: number;
          season_number: number;
          name: string;
          episodes?: Array<{
            id: number;
            episode_number: number;
            name: string;
            overview: string;
            still_path: string | null;
            vote_average: number;
          }>;
        }>(url);
        const data: SeasonDetailsResponse = {
          id: raw.id,
          season_number: raw.season_number,
          name: raw.name,
          episodes: raw.episodes || []
        };
        setCache(cacheKey, data, SERVER_CACHE_TTLS.SEASON_DETAILS);
        return data;
      } catch (err) {
        console.warn('Direct season_details failed:', err);
        throw err;
      }
    });
  }

  /**
   * Get collection parts (e.g. Marvel, Harry Potter, Star Wars)
   */
  static async getCollection(id: number | string): Promise<MediaListResponse> {
    const cacheKey = `collection:${id}`;
    const cached = getCached<MediaListResponse>(cacheKey);
    if (cached) return cached;

    return fetchWithDedup(cacheKey, async () => {
      try {
        const url = `${TMDB_BASE_URL}/collection/${id}?api_key=${TMDB_API_KEY}`;
        const raw = await requestJson<{
          id: number;
          name: string;
          parts?: MediaItem[];
        }>(url);
        const parts = (raw.parts || []).map((p) => ({ ...p, media_type: 'movie' as const }));
        const data: MediaListResponse = {
          page: 1,
          results: parts,
          parts: parts,
          total_pages: 1,
          total_results: parts.length
        };
        setCache(cacheKey, data, SERVER_CACHE_TTLS.COLLECTION);
        return data;
      } catch (err) {
        console.warn('Direct collection failed:', err);
        throw err;
      }
    });
  }

  /**
   * Get cast & crew credits for a movie or TV show directly from TMDB
   */
  static async getCredits(id: number | string, mode: 'movie' | 'tv' = 'movie'): Promise<unknown> {
    const cacheKey = `credits:${mode}:${id}`;
    const cached = getCached<unknown>(cacheKey);
    if (cached) return cached;

    return fetchWithDedup(cacheKey, async () => {
      // TMDB credits endpoint - uses the same public API key used for images
      const TMDB_API_KEY = '4ef0d7355d9ffb5151e987764708ce96';
      const url = `https://api.themoviedb.org/3/${mode}/${id}/credits?api_key=${TMDB_API_KEY}&language=en-US`;
      const data = await requestJson<unknown>(url);
      setCache(cacheKey, data, SERVER_CACHE_TTLS.CREDITS);
      return data;
    });
  }

  /**
   * Get full enriched media details from TMDB (genres, tagline, runtime, IMDB ID,
   * director, writers, keywords, networks, budget, revenue, etc.)
   */
  static async getFullDetails(id: number | string, mode: 'movie' | 'tv' = 'movie'): Promise<unknown> {
    const cacheKey = `full_details:${mode}:${id}`;
    const cached = getCached<unknown>(cacheKey);
    if (cached) return cached;

    return fetchWithDedup(cacheKey, async () => {
      const TMDB_API_KEY = '4ef0d7355d9ffb5151e987764708ce96';
      const append = 'keywords,external_ids,credits';
      const url = `https://api.themoviedb.org/3/${mode}/${id}?api_key=${TMDB_API_KEY}&language=en-US&append_to_response=${append}`;
      const raw = await requestJson<Record<string, unknown>>(url);

      // Inject director and writer arrays into the top-level response for convenience
      const credits = raw.credits as { crew?: Array<{ name: string; job: string }> } | undefined;
      const crew = credits?.crew || [];
      const directors = crew.filter((c) => c.job === 'Director').map((c) => c.name);
      const writers = crew
        .filter((c) => ['Screenplay', 'Writer', 'Story', 'Creator'].includes(c.job))
        .map((c) => c.name)
        .slice(0, 3);

      const result = { ...raw, directors, writers };
      setCache(cacheKey, result, SERVER_CACHE_TTLS.DETAILS);
      return result;
    });
  }

  /**
   * Get current trending search titles mirroring IMDB MovieMeter.
   * Fetches TMDB movie/popular (MovieMeter equivalent) and tv/popular in parallel,
   * interleaves them, and returns the top 12 titles.
   */
  static async getTrendingSearches(): Promise<string[]> {
    const cacheKey = 'trending_searches';
    const cached = getCached<string[]>(cacheKey);
    if (cached) return cached;

    return fetchWithDedup(cacheKey, async () => {
      const TMDB_API_KEY = '4ef0d7355d9ffb5151e987764708ce96';
      const [movies, shows] = await Promise.all([
        requestJson<{ results: Array<{ title?: string }> }>(
          `https://api.themoviedb.org/3/movie/popular?api_key=${TMDB_API_KEY}&language=en-US&page=1`
        ),
        requestJson<{ results: Array<{ name?: string }> }>(
          `https://api.themoviedb.org/3/tv/popular?api_key=${TMDB_API_KEY}&language=en-US&page=1`
        )
      ]);

      // Interleave: movie, show, movie, show... for variety
      const movieTitles = (movies.results || []).map((r) => r.title || '').filter(Boolean);
      const showTitles  = (shows.results  || []).map((r) => r.name  || '').filter(Boolean);
      const merged: string[] = [];
      const len = Math.max(movieTitles.length, showTitles.length);
      for (let i = 0; i < len && merged.length < 12; i++) {
        if (movieTitles[i]) merged.push(movieTitles[i]);
        if (merged.length < 12 && showTitles[i]) merged.push(showTitles[i]);
      }

      setCache(cacheKey, merged, SERVER_CACHE_TTLS.TRENDING);
      return merged;
    });
  }
}
