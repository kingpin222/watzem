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

export interface MediaListResponse {
  page: number;
  results: MediaItem[];
  total_pages: number;
  total_results: number;
  parts?: MediaItem[]; // Collections return parts
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
}

export interface SeasonDetailsResponse {
  id: number;
  season_number: number;
  name: string;
  episodes: TVEpisode[];
}
