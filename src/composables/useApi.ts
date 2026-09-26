import {
  MediaListResponse,
  TorrentResponse,
  TVDetailsResponse,
  SeasonDetailsResponse,
  CreditsResponse,
  MediaDetails,
  MediaMode
} from '../types';
import { ApiCache, CACHE_TTLS } from '../utils/apiCache';
import { TmdbClient } from '../services/tmdbClient';

export function useApi() {
  /**
   * Helper to format TMDB image URLs
   */
  const getImageUrl = (
    path: string | null | undefined,
    size: 'w185' | 'w342' | 'w500' | 'w780' | 'w1280' | 'original' = 'w342'
  ): string => {
    return TmdbClient.getImageUrl(path, size);
  };

  /**
   * Get list of media (popular, trending, discover, top_rated/imdb...) with cache
   * Runs directly against TMDB API without requiring any server backend.
   */
  const getMediaList = async (
    params: Record<string, string | number | undefined>,
    forceRefresh = false
  ): Promise<MediaListResponse> => {
    const searchParams = new URLSearchParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        searchParams.append(key, String(value));
      }
    }
    const queryString = searchParams.toString();
    const cacheKey = `media_list:${queryString}`;

    if (forceRefresh) {
      ApiCache.invalidate(cacheKey);
    }

    return ApiCache.fetchWithCache(
      cacheKey,
      () => TmdbClient.getMediaList(params),
      CACHE_TTLS.MEDIA_LIST
    );
  };

  /**
   * Search movies, TV shows, or both with cache directly from TMDB
   */
  const searchMedia = async (
    query: string,
    mode: MediaMode | 'multi' | 'all' = 'multi',
    page = 1
  ): Promise<MediaListResponse> => {
    const cacheKey = `search:${mode}:${query.toLowerCase().trim()}:${page}`;

    return ApiCache.fetchWithCache(
      cacheKey,
      () => TmdbClient.searchMedia(query, mode, page),
      CACHE_TTLS.SEARCH
    );
  };

  /**
   * Search for an actor / person with cache
   */
  const searchPerson = async (query: string) => {
    const trimmed = query.trim().toLowerCase();
    const cacheKey = `person_search:${trimmed}`;
    return ApiCache.fetchWithCache(
      cacheKey,
      () => TmdbClient.searchPerson(trimmed),
      CACHE_TTLS.SEARCH
    );
  };

  /**
   * Get all movies and TV credits for an actor with cache
   */
  const getActorCredits = async (personId: number | string) => {
    const cacheKey = `actor_credits:${personId}`;
    return ApiCache.fetchWithCache(
      cacheKey,
      () => TmdbClient.getActorCredits(personId),
      CACHE_TTLS.CREDITS
    );
  };

  /**
   * Search torrents and magnet links with cache
   */
  const getTorrents = async (
    _name: string,
    _year: string | number = '',
    _mode: MediaMode = 'movie',
    _quality = 'all'
  ): Promise<TorrentResponse> => {
    return TmdbClient.getTorrents();
  };

  /**
   * Get TV Show details with cache
   */
  const getTvDetails = async (id: number | string): Promise<TVDetailsResponse> => {
    const cacheKey = `tv_details:${id}`;

    return ApiCache.fetchWithCache(
      cacheKey,
      () => TmdbClient.getTvDetails(id),
      CACHE_TTLS.TV_DETAILS
    );
  };

  /**
   * Get TV Season details with episodes with cache
   */
  const getSeasonDetails = async (
    id: number | string,
    seasonNumber: number | string
  ): Promise<SeasonDetailsResponse> => {
    const cacheKey = `season_details:${id}:${seasonNumber}`;

    return ApiCache.fetchWithCache(
      cacheKey,
      () => TmdbClient.getSeasonDetails(id, seasonNumber),
      CACHE_TTLS.SEASON_DETAILS
    );
  };

  /**
   * Get Collection parts with cache
   */
  const getCollection = async (id: number | string): Promise<MediaListResponse> => {
    const cacheKey = `collection:${id}`;

    return ApiCache.fetchWithCache(
      cacheKey,
      () => TmdbClient.getCollection(id),
      CACHE_TTLS.COLLECTION
    );
  };

  /**
   * Get cast & credits for a movie or TV show with cache
   */
  const getCredits = async (id: number | string, mode: MediaMode = 'movie'): Promise<CreditsResponse> => {
    const cacheKey = `credits:${mode}:${id}`;

    return ApiCache.fetchWithCache(
      cacheKey,
      () => TmdbClient.getCredits(id, mode),
      CACHE_TTLS.CREDITS
    );
  };

  /**
   * Get full enriched media details from TMDB with cache
   */
  const getFullDetails = async (id: number | string, mode: MediaMode = 'movie'): Promise<MediaDetails> => {
    const cacheKey = `full_details:${mode}:${id}`;

    return ApiCache.fetchWithCache(
      cacheKey,
      () => TmdbClient.getFullDetails(id, mode),
      CACHE_TTLS.DETAILS
    );
  };

  /**
   * Get backdrop and poster images for a movie or TV show from TMDB
   */
  const getMediaImages = async (
    id: number | string,
    mode: MediaMode = 'movie'
  ): Promise<string[]> => {
    const cacheKey = `media_images:${mode}:${id}`;

    return ApiCache.fetchWithCache(
      cacheKey,
      () => TmdbClient.getMediaImages(id, mode),
      CACHE_TTLS.DETAILS
    );
  };

  /**
   * Get YouTube trailer key for a movie or TV show from TMDB
   */
  const getMediaVideos = async (
    id: number | string,
    mode: MediaMode = 'movie'
  ): Promise<{ key: string; name: string; type: string; site: string }[]> => {
    const cacheKey = `media_videos:${mode}:${id}`;

    return ApiCache.fetchWithCache(
      cacheKey,
      () => TmdbClient.getMediaVideos(id, mode),
      CACHE_TTLS.DETAILS
    );
  };

  /**
   * Get live trending search titles with cache
   */
  const getTrendingSearches = async (): Promise<string[]> => {
    const cacheKey = 'trending_searches';

    return ApiCache.fetchWithCache(
      cacheKey,
      () => TmdbClient.getTrendingSearches(),
      CACHE_TTLS.TRENDING_SEARCHES
    );
  };

  /**
   * Standalone stream servers probe without backend server dependency
   */
  const probeStreamServers = async (
    _id: number | string,
    _mode: MediaMode = 'movie',
    _season = 1,
    _episode = 1
  ): Promise<{
    bestServerIndex: number;
    bestServerId: string;
    bestServerName: string;
    servers: Array<{ index: number; id: string; name: string; ok: boolean; latency: number }>;
  }> => {
    return TmdbClient.probeStreamServers();
  };

  /**
   * Clear or invalidate cached data
   */
  const clearCache = () => ApiCache.clear();
  const invalidateCache = (prefix: string) => ApiCache.invalidate(prefix);

  return {
    getImageUrl,
    getMediaList,
    searchMedia,
    getTorrents,
    getTvDetails,
    getSeasonDetails,
    getCollection,
    getCredits,
    getFullDetails,
    getMediaImages,
    getMediaVideos,
    getTrendingSearches,
    searchPerson,
    getActorCredits,
    probeStreamServers,
    clearCache,
    invalidateCache
  };
}
