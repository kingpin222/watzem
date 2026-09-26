import {
  MediaItem,
  MediaListResponse,
  TorrentResponse,
  TVDetailsResponse,
  SeasonDetailsResponse,
  CreditsResponse,
  MediaDetails,
  MediaMode
} from '../types';

export const TMDB_API_KEY = '4ef0d7355d9ffb5151e987764708ce96';
export const TMDB_BASE_URL = 'https://api.themoviedb.org/3';
export const TMDB_IMAGE_BASE = 'https://image.tmdb.org/t/p';

// In-flight deduplication map so identical parallel queries share one network request
const inFlightRequests = new Map<string, Promise<unknown>>();

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
      'Accept': 'application/json, text/plain, */*'
    }
  });

  if (!res.ok) {
    throw new Error(`TMDB HTTP ${res.status}: ${res.statusText} (${url})`);
  }

  return (await res.json()) as T;
}

export class TmdbClient {
  /**
   * Helper to format TMDB image URLs
   */
  static getImageUrl(
    path: string | null | undefined,
    size: 'w185' | 'w342' | 'w500' | 'w780' | 'w1280' | 'original' = 'w342'
  ): string {
    if (!path || typeof path !== 'string') return '';
    const trimmed = path.trim();
    if (!trimmed) return '';
    if (trimmed.startsWith('http://') || trimmed.startsWith('https://') || trimmed.startsWith('data:')) {
      return trimmed;
    }
    const cleanPath = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
    return `${TMDB_IMAGE_BASE}/${size}${cleanPath}`;
  }

  /**
   * Fetch movies or TV shows list (popular, trending, discover, top_rated/imdb, etc.)
   * Runs directly from the client against TMDB API.
   */
  static async getMediaList(
    params: Record<string, string | number | undefined>
  ): Promise<MediaListResponse> {
    const searchParams = new URLSearchParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        searchParams.append(key, String(value));
      }
    }

    const dedupKey = `list:${searchParams.toString()}`;

    return fetchWithDedup(dedupKey, async () => {
      const apiType = String(params.api || 'popular');
      const mode = (String(params.mode || 'movie')) as 'movie' | 'tv';
      const page = parseInt(String(params.page || '1'), 10) || 1;

      try {
        // 1. IMDb Top List (or top_rated requested by user)
        if (apiType === 'imdb' || apiType === 'imdb_top' || apiType === 'top_rated') {
          if (mode === 'movie') {
            try {
              // Curated IMDb Top 250 list on TMDB (List ID: 634)
              const listUrl = `${TMDB_BASE_URL}/list/634?api_key=${TMDB_API_KEY}&page=${page}`;
              const raw = await requestJson<{ items?: MediaItem[]; results?: MediaItem[]; item_count?: number }>(listUrl);
              const items = (raw.items || raw.results || []).map((item) => ({
                ...item,
                media_type: 'movie' as const
              }));
              return {
                page,
                results: items,
                total_pages: Math.ceil((raw.item_count || 250) / 20),
                total_results: raw.item_count || 250
              };
            } catch {
              // Fallback to top_rated movie
              const fallbackUrl = `${TMDB_BASE_URL}/movie/top_rated?api_key=${TMDB_API_KEY}&page=${page}`;
              const raw = await requestJson<MediaListResponse>(fallbackUrl);
              return {
                page: raw.page || page,
                results: (raw.results || []).map((item) => ({ ...item, media_type: 'movie' as const })),
                total_pages: raw.total_pages || 1,
                total_results: raw.total_results || 0
              };
            }
          } else {
            // TV Top Rated mirroring IMDb Top TV (filtered by high vote count)
            const tvUrl = `${TMDB_BASE_URL}/discover/tv?api_key=${TMDB_API_KEY}&sort_by=vote_average.desc&vote_count.gte=1000&page=${page}`;
            const raw = await requestJson<MediaListResponse>(tvUrl);
            return {
              page: raw.page || page,
              results: (raw.results || []).map((item) => ({ ...item, media_type: 'tv' as const })),
              total_pages: raw.total_pages || 1,
              total_results: raw.total_results || 0
            };
          }
        }

        // 2. Popular
        if (apiType === 'popular') {
          const url = `${TMDB_BASE_URL}/${mode}/popular?api_key=${TMDB_API_KEY}&page=${page}`;
          const raw = await requestJson<MediaListResponse>(url);
          return {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: mode })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
        }

        // 3. Trending
        if (apiType === 'trending') {
          const timeWindow = String(params.time || 'week');
          const url = `${TMDB_BASE_URL}/trending/${mode}/${timeWindow}?api_key=${TMDB_API_KEY}&page=${page}`;
          const raw = await requestJson<MediaListResponse>(url);
          return {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: mode })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
        }

        // 4. In Theaters (now_playing)
        if (apiType === 'now_playing') {
          const url = `${TMDB_BASE_URL}/movie/now_playing?api_key=${TMDB_API_KEY}&page=${page}`;
          const raw = await requestJson<MediaListResponse>(url);
          return {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: 'movie' as const })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
        }

        // 5. Upcoming
        if (apiType === 'upcoming') {
          const url = `${TMDB_BASE_URL}/movie/upcoming?api_key=${TMDB_API_KEY}&page=${page}`;
          const raw = await requestJson<MediaListResponse>(url);
          return {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: 'movie' as const })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
        }

        // 6. On Air (TV)
        if (apiType === 'on_the_air') {
          const url = `${TMDB_BASE_URL}/tv/on_the_air?api_key=${TMDB_API_KEY}&page=${page}`;
          const raw = await requestJson<MediaListResponse>(url);
          return {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: 'tv' as const })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
        }

        // 7. Airing Today (TV)
        if (apiType === 'airing_today') {
          const url = `${TMDB_BASE_URL}/tv/airing_today?api_key=${TMDB_API_KEY}&page=${page}`;
          const raw = await requestJson<MediaListResponse>(url);
          return {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: 'tv' as const })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
        }

        // 8. Discover (genres, networks, studios, year)
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
          return {
            page: raw.page || page,
            results: (raw.results || []).map((item) => ({ ...item, media_type: mode })),
            total_pages: raw.total_pages || 1,
            total_results: raw.total_results || 0
          };
        }

        // 9. Generic endpoint fallback
        const fallbackUrl = `${TMDB_BASE_URL}/${mode}/${apiType}?api_key=${TMDB_API_KEY}&page=${page}`;
        const raw = await requestJson<MediaListResponse>(fallbackUrl);
        return {
          page: raw.page || page,
          results: (raw.results || []).map((item) => ({ ...item, media_type: mode })),
          total_pages: raw.total_pages || 1,
          total_results: raw.total_results || 0
        };
      } catch (err) {
        console.warn(`Direct TMDb request failed for api="${apiType}":`, err);
        return { page, results: [], total_pages: 1, total_results: 0 };
      }
    });
  }

  /**
   * Search movies or TV shows directly from TMDB
   */
  static async searchMedia(
    query: string,
    mode: MediaMode = 'movie',
    page = 1
  ): Promise<MediaListResponse> {
    const dedupKey = `search:${mode}:${query.toLowerCase().trim()}:${page}`;

    return fetchWithDedup(dedupKey, async () => {
      try {
        const url = `${TMDB_BASE_URL}/search/${mode}?api_key=${TMDB_API_KEY}&query=${encodeURIComponent(query)}&page=${page}`;
        const raw = await requestJson<MediaListResponse>(url);
        return {
          page: raw.page || page,
          results: (raw.results || []).map((item) => ({ ...item, media_type: mode })),
          total_pages: raw.total_pages || 1,
          total_results: raw.total_results || 0
        };
      } catch (err) {
        console.warn('Direct search failed:', err);
        return { page, results: [], total_pages: 1, total_results: 0 };
      }
    });
  }

  /**
   * Get TV show details including season list
   */
  static async getTvDetails(id: number | string): Promise<TVDetailsResponse> {
    const dedupKey = `tv_details:${id}`;

    return fetchWithDedup(dedupKey, async () => {
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

      return {
        id: raw.id,
        name: raw.name,
        overview: raw.overview,
        number_of_seasons: raw.number_of_seasons,
        number_of_episodes: raw.number_of_episodes,
        seasons: raw.seasons || []
      };
    });
  }

  /**
   * Get TV show episodes for a specific season
   */
  static async getSeasonDetails(
    id: number | string,
    seasonNumber: number | string
  ): Promise<SeasonDetailsResponse> {
    const dedupKey = `season_details:${id}:${seasonNumber}`;

    return fetchWithDedup(dedupKey, async () => {
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
          air_date?: string;
          runtime?: number;
        }>;
      }>(url);

      return {
        id: raw.id,
        season_number: raw.season_number,
        name: raw.name,
        episodes: raw.episodes || []
      };
    });
  }

  /**
   * Get collection parts (e.g., Marvel cinematic universe, Harry Potter, etc.)
   */
  static async getCollection(id: number | string): Promise<MediaListResponse> {
    const dedupKey = `collection:${id}`;

    return fetchWithDedup(dedupKey, async () => {
      const url = `${TMDB_BASE_URL}/collection/${id}?api_key=${TMDB_API_KEY}`;
      const raw = await requestJson<{
        id: number;
        name: string;
        parts?: MediaItem[];
      }>(url);

      const parts = (raw.parts || []).map((p) => ({ ...p, media_type: 'movie' as const }));
      return {
        page: 1,
        results: parts,
        parts: parts,
        total_pages: 1,
        total_results: parts.length
      };
    });
  }

  /**
   * Get cast & crew credits for a movie or TV show
   */
  static async getCredits(id: number | string, mode: MediaMode = 'movie'): Promise<CreditsResponse> {
    const dedupKey = `credits:${mode}:${id}`;

    return fetchWithDedup(dedupKey, async () => {
      const url = `${TMDB_BASE_URL}/${mode}/${id}/credits?api_key=${TMDB_API_KEY}&language=en-US`;
      const raw = await requestJson<{ id: number; cast?: any[] }>(url);
      return {
        id: raw.id,
        cast: raw.cast || []
      };
    });
  }

  /**
   * Get full enriched media details from TMDB (genres, runtime, IMDB ID, directors, writers, etc.)
   */
  static async getFullDetails(id: number | string, mode: MediaMode = 'movie'): Promise<MediaDetails> {
    const dedupKey = `full_details:${mode}:${id}`;

    return fetchWithDedup(dedupKey, async () => {
      const append = 'keywords,external_ids,credits';
      const url = `${TMDB_BASE_URL}/${mode}/${id}?api_key=${TMDB_API_KEY}&language=en-US&append_to_response=${append}`;
      const raw = await requestJson<any>(url);

      // Extract director and writer arrays
      const credits = raw.credits as { crew?: Array<{ name: string; job: string }> } | undefined;
      const crew = credits?.crew || [];
      const directors = crew.filter((c) => c.job === 'Director').map((c) => c.name);
      const writers = crew
        .filter((c) => ['Screenplay', 'Writer', 'Story', 'Creator'].includes(c.job))
        .map((c) => c.name)
        .slice(0, 3);

      const imdbId = raw.imdb_id || raw.external_ids?.imdb_id || '';

      return {
        ...raw,
        imdb_id: imdbId,
        directors,
        writers
      } as MediaDetails;
    });
  }

  /**
   * Get backdrop and poster image URLs directly from TMDB
   */
  static async getMediaImages(id: number | string, mode: MediaMode = 'movie'): Promise<string[]> {
    const dedupKey = `images:${mode}:${id}`;

    return fetchWithDedup(dedupKey, async () => {
      try {
        const url = `${TMDB_BASE_URL}/${mode}/${id}/images?api_key=${TMDB_API_KEY}&include_image_language=en,null`;
        const data = await requestJson<{ backdrops?: { file_path: string }[]; posters?: { file_path: string }[] }>(url);
        const backdrops = (data.backdrops || []).slice(0, 10).map((b) => `${TMDB_IMAGE_BASE}/w1280${b.file_path}`);
        const posters = (data.posters || []).slice(0, 5).map((p) => `${TMDB_IMAGE_BASE}/w780${p.file_path}`);
        return [...backdrops, ...posters].filter(Boolean);
      } catch (err) {
        console.warn('Direct TMDB images fetch failed:', err);
        return [];
      }
    });
  }

  /**
   * Get YouTube trailer video key directly from TMDB
   */
  static async getMediaVideos(
    id: number | string,
    mode: MediaMode = 'movie'
  ): Promise<{ key: string; name: string; type: string; site: string }[]> {
    const dedupKey = `videos:${mode}:${id}`;

    return fetchWithDedup(dedupKey, async () => {
      try {
        const url = `${TMDB_BASE_URL}/${mode}/${id}/videos?api_key=${TMDB_API_KEY}&language=en-US`;
        const data = await requestJson<{ results?: { key: string; name: string; type: string; site: string }[] }>(url);
        return (data.results || []).filter((v) => v.site === 'YouTube');
      } catch (err) {
        console.warn('Direct TMDB videos fetch failed:', err);
        return [];
      }
    });
  }

  /**
   * Get current trending search titles from TMDB (interleaving movie & tv popular)
   */
  static async getTrendingSearches(): Promise<string[]> {
    const dedupKey = 'trending_searches';

    return fetchWithDedup(dedupKey, async () => {
      try {
        const [movies, shows] = await Promise.all([
          requestJson<{ results: Array<{ title?: string }> }>(
            `${TMDB_BASE_URL}/movie/popular?api_key=${TMDB_API_KEY}&language=en-US&page=1`
          ),
          requestJson<{ results: Array<{ name?: string }> }>(
            `${TMDB_BASE_URL}/tv/popular?api_key=${TMDB_API_KEY}&language=en-US&page=1`
          )
        ]);

        const movieTitles = (movies.results || []).map((r) => r.title || '').filter(Boolean);
        const showTitles = (shows.results || []).map((r) => r.name || '').filter(Boolean);
        const merged: string[] = [];
        const len = Math.max(movieTitles.length, showTitles.length);
        for (let i = 0; i < len && merged.length < 12; i++) {
          if (movieTitles[i]) merged.push(movieTitles[i]);
          if (merged.length < 12 && showTitles[i]) merged.push(showTitles[i]);
        }
        return merged;
      } catch (err) {
        console.warn('Trending searches fetch failed:', err);
        return [];
      }
    });
  }

  /**
   * Standalone stream servers probe (returns candidates immediately without server dependency)
   */
  static probeStreamServers(): {
    bestServerIndex: number;
    bestServerId: string;
    bestServerName: string;
    servers: Array<{ index: number; id: string; name: string; ok: boolean; latency: number }>;
  } {
    return {
      bestServerIndex: 0,
      bestServerId: 'server10',
      bestServerName: 'Server 1',
      servers: [
        { index: 0, id: 'server10', name: 'Server 1', ok: true, latency: 45 },
        { index: 1, id: 'server3', name: 'Server 2', ok: true, latency: 60 },
        { index: 2, id: 'server1', name: 'Server 3', ok: true, latency: 85 },
        { index: 3, id: 'server2', name: 'Server 4', ok: true, latency: 95 },
        { index: 4, id: 'server5', name: 'Server 5', ok: true, latency: 140 },
        { index: 5, id: 'server6', name: 'Server 6', ok: true, latency: 170 },
        { index: 6, id: 'server8', name: 'Server 7', ok: true, latency: 80 }
      ]
    };
  }

  /**
   * Standalone empty torrent response
   */
  static getTorrents(): TorrentResponse {
    return { hits: [], total: 0, sources: [] };
  }
}
