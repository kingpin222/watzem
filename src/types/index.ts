export interface MediaItem {
  id: number;
  title?: string;
  name?: string;
  original_title?: string;
  original_name?: string;
  overview: string;
  poster_path: string | null;
  backdrop_path: string | null;
  vote_average: number;
  vote_count?: number;
  release_date?: string;
  first_air_date?: string;
  media_type?: 'movie' | 'tv' | 'person';
  genre_ids?: number[];
  popularity?: number;
}

export interface MediaDetailsGenre {
  id: number;
  name: string;
}

export interface MediaDetailsNetwork {
  id: number;
  name: string;
  logo_path: string | null;
}

export interface MediaDetailsCreator {
  id: number;
  name: string;
}

export interface MediaDetails {
  id: number;
  imdb_id?: string;
  title?: string;
  name?: string;
  original_title?: string;
  original_name?: string;
  overview?: string;
  tagline?: string;
  poster_path: string | null;
  backdrop_path: string | null;
  vote_average: number;
  vote_count?: number;
  release_date?: string;
  first_air_date?: string;
  runtime?: number;
  episode_run_time?: number[];
  genres?: MediaDetailsGenre[];
  status?: string;
  original_language?: string;
  budget?: number;
  revenue?: number;
  popularity?: number;
  number_of_seasons?: number;
  number_of_episodes?: number;
  seasons?: TVSeason[];
  networks?: MediaDetailsNetwork[];
  created_by?: MediaDetailsCreator[];
  keywords?: { keywords?: Array<{ id: number; name: string }>; results?: Array<{ id: number; name: string }> };
  external_ids?: { imdb_id?: string };
  // Crew injected server-side
  directors?: string[];
  writers?: string[];
}

export interface MediaListResponse {
  page: number;
  results: MediaItem[];
  total_pages: number;
  total_results: number;
  parts?: MediaItem[];
}

export interface TorrentItem {
  title: string;
  seeds: number;
  peers: number;
  bytes: number;
  magnetUrl: string;
  hash: string;
  source: string;
}

export interface TorrentResponse {
  hits: TorrentItem[];
  sources: string[];
  total: number;
}

export interface TVSeason {
  id: number;
  name: string;
  overview: string;
  season_number: number;
  episode_count: number;
  poster_path: string | null;
}

export interface TVDetailsResponse {
  id: number;
  name: string;
  overview: string;
  number_of_seasons: number;
  number_of_episodes: number;
  seasons: TVSeason[];
}

export interface TVEpisode {
  id: number;
  episode_number: number;
  name: string;
  overview: string;
  still_path: string | null;
  vote_average: number;
  air_date?: string;
  runtime?: number;
}

export interface SeasonDetailsResponse {
  id: number;
  season_number: number;
  name: string;
  episodes: TVEpisode[];
}

export type MediaMode = 'movie' | 'tv';

export type TabKey = 'home' | 'explore' | 'search' | 'watchlist' | 'download' | 'details';

export interface CastMember {
  id: number;
  name: string;
  character: string;
  profile_path: string | null;
  order: number;
}

export interface CreditsResponse {
  id: number;
  cast: CastMember[];
}

export interface PersonSearchResult {
  id: number;
  name: string;
  profile_path: string | null;
  known_for_department?: string;
  popularity?: number;
}


export interface DownloadedItem {
  id: number;
  item: MediaItem;
  mode: MediaMode;
  season?: number;
  episode?: number;
  downloadedAt: number;
  sizeBytes: number;
  formattedSize: string;
  status: 'completed' | 'downloading' | 'failed';
  streamUrl: string;
  torrentHash?: string;
  magnetUrl?: string;
  fileName?: string;
  fileQuality?: string;
  storagePath?: string;
  seeds?: number;
  downloadType?: 'actual_video' | 'storage_file' | 'torrent_app' | 'play_later';
}

export interface CategoryOption {
  key: string;
  label: string;
  icon: string;
  type: string;
  params?: Record<string, string | number>;
}
