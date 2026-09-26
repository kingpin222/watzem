import express, { Request, Response } from 'express';
import cors from 'cors';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { YifyService } from './services/yifyService.js';
import { TorrentStreamService } from './services/torrentStreamService.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Find project root directory reliably whether executing from tsx (server/src) or node (dist-server)
const findProjectRoot = (startDir: string): string => {
  let curr = startDir;
  for (let i = 0; i < 4; i++) {
    if (fs.existsSync(path.join(curr, 'package.json'))) {
      return curr;
    }
    const parent = path.dirname(curr);
    if (parent === curr) break;
    curr = parent;
  }
  if (fs.existsSync(path.resolve(process.cwd(), 'dist'))) {
    return process.cwd();
  }
  return path.resolve(startDir, '..');
};

const projectRoot = findProjectRoot(__dirname);
const clientDistPath = path.resolve(projectRoot, 'dist');

const app = express();
const PORT = process.env.PORT || 2727;

app.use(cors());
app.use(express.json());

// Health check
app.get('/api/health', (_req: Request, res: Response) => {
  res.json({ status: 'ok', time: new Date().toISOString() });
});

// Browse & Discover Media (movies / tv)
app.get('/api/media', async (req: Request, res: Response) => {
  try {
    const params = req.query as Record<string, string>;
    const data = await YifyService.getMediaList(params);
    res.setHeader('Cache-Control', 'public, max-age=900, stale-while-revalidate=1800');
    res.json(data);
  } catch (error) {
    console.error('Error fetching media:', error);
    res.status(500).json({ error: 'Failed to fetch media list' });
  }
});

// Search
app.get('/api/search', async (req: Request, res: Response) => {
  try {
    const query = String(req.query.q || '');
    const mode = (req.query.mode as 'movie' | 'tv') || 'movie';
    const page = parseInt(String(req.query.page || '1'), 10);

    if (!query) {
      return res.status(400).json({ error: 'Search query "q" is required' });
    }

    const data = await YifyService.search(query, mode, page);
    res.setHeader('Cache-Control', 'public, max-age=600, stale-while-revalidate=1800');
    res.json(data);
  } catch (error) {
    console.error('Error searching media:', error);
    res.status(500).json({ error: 'Failed to perform search' });
  }
});

// Torrents & Magnet Links
app.get('/api/torrents', async (req: Request, res: Response) => {
  try {
    const name = String(req.query.name || '');
    const year = req.query.year ? String(req.query.year) : '';
    const mode = (req.query.mode as 'movie' | 'tv') || 'movie';
    const quality = String(req.query.quality || 'all');

    if (!name) {
      return res.status(400).json({ error: 'Movie/TV name is required' });
    }

    const data = await YifyService.getTorrents(name, year, mode, quality);
    res.setHeader('Cache-Control', 'public, max-age=1800, stale-while-revalidate=3600');
    res.json(data);
  } catch (error) {
    console.error('Error fetching torrents:', error);
    res.status(500).json({ error: 'Failed to fetch torrents' });
  }
});

// Direct File Download to Device Storage (.torrent / .magnet)
app.get('/api/download/torrent', async (req: Request, res: Response) => {
  try {
    const hash = String(req.query.hash || '').trim().toUpperCase();
    const name = String(req.query.name || 'movie').replace(/[/\\?%*:|"<>]/g, '_');
    const magnet = String(req.query.magnet || '');

    if (!hash && !magnet) {
      return res.status(400).json({ error: 'Hash or magnet is required' });
    }

    if (hash) {
      try {
        const upstream = await fetch(`http://itorrents.net/torrent/${hash}.torrent`, {
          headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
          }
        });
        if (upstream.ok) {
          const buffer = await upstream.arrayBuffer();
          res.setHeader('Content-Type', 'application/x-bittorrent');
          res.setHeader('Content-Disposition', `attachment; filename="${name}.torrent"`);
          return res.send(Buffer.from(buffer));
        }
      } catch (err) {
        console.warn('Failed to fetch from itorrents, falling back to magnet file', err);
      }
    }

    // Fallback: Send downloadable .magnet file
    res.setHeader('Content-Type', 'text/plain');
    res.setHeader('Content-Disposition', `attachment; filename="${name}.magnet"`);
    res.send(magnet || `magnet:?xt=urn:btih:${hash}&dn=${encodeURIComponent(name)}`);
  } catch (error) {
    console.error('Error downloading torrent file:', error);
    res.status(500).json({ error: 'Failed to download file' });
  }
});

// Stream Actual Movie Video (MP4 / MKV) Directly to Device Storage
app.get('/api/download/video', async (req: Request, res: Response) => {
  try {
    const hash = String(req.query.hash || '').trim();
    const magnet = String(req.query.magnet || '').trim();
    const name = String(req.query.name || 'movie').trim();

    if (!hash && !magnet) {
      return res.status(400).json({ error: 'Hash or magnet is required' });
    }

    const torrentId = TorrentStreamService.formatTorrentId(magnet || hash, name);
    const torrent = await TorrentStreamService.getOrAddTorrent(torrentId, 25000);

    const movieFile = TorrentStreamService.findMovieFile(torrent);
    if (!movieFile) {
      return res.status(404).json({ error: 'No video file found in movie torrent' });
    }

    TorrentStreamService.streamVideo(movieFile, req, res, name);
  } catch (error) {
    console.error('Error streaming movie file:', error);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Failed to stream video file' });
    }
  }
});

// Real-time Movie Download / Stream Status
app.get('/api/download/status', async (req: Request, res: Response) => {
  try {
    const hash = String(req.query.hash || '');
    const magnet = String(req.query.magnet || '');
    const status = await TorrentStreamService.getTorrentStatus(magnet || hash);
    res.json(status || { ready: false, message: 'Torrent not yet active' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to retrieve download status' });
  }
});

// TV Show Details
app.get('/api/tv/details', async (req: Request, res: Response) => {
  try {
    const id = String(req.query.id || '');
    if (!id) {
      return res.status(400).json({ error: 'TV Show ID is required' });
    }
    const data = await YifyService.getTvDetails(id);
    res.setHeader('Cache-Control', 'public, max-age=7200, stale-while-revalidate=86400');
    res.json(data);
  } catch (error) {
    console.error('Error fetching TV details:', error);
    res.status(500).json({ error: 'Failed to fetch TV details' });
  }
});

// TV Show Season Details
app.get('/api/tv/season', async (req: Request, res: Response) => {
  try {
    const id = String(req.query.id || '');
    const season = String(req.query.season || '1');
    if (!id) {
      return res.status(400).json({ error: 'TV Show ID is required' });
    }
    const data = await YifyService.getSeasonDetails(id, season);
    res.setHeader('Cache-Control', 'public, max-age=7200, stale-while-revalidate=86400');
    res.json(data);
  } catch (error) {
    console.error('Error fetching season details:', error);
    res.status(500).json({ error: 'Failed to fetch season details' });
  }
});

// Collection details
app.get('/api/collection', async (req: Request, res: Response) => {
  try {
    const id = String(req.query.id || '');
    if (!id) {
      return res.status(400).json({ error: 'Collection ID is required' });
    }
    const data = await YifyService.getCollection(id);
    res.setHeader('Cache-Control', 'public, max-age=21600, stale-while-revalidate=86400');
    res.json(data);
  } catch (error) {
    console.error('Error fetching collection:', error);
    res.status(500).json({ error: 'Failed to fetch collection' });
  }
});

// Cast & Credits for a movie or TV show
app.get('/api/credits', async (req: Request, res: Response) => {
  try {
    const id = String(req.query.id || '');
    const mode = (req.query.mode as 'movie' | 'tv') || 'movie';
    if (!id) {
      return res.status(400).json({ error: 'Media ID is required' });
    }
    const data = await YifyService.getCredits(id, mode);
    res.setHeader('Cache-Control', 'public, max-age=7200, stale-while-revalidate=86400');
    res.json(data);
  } catch (error) {
    console.error('Error fetching credits:', error);
    res.status(500).json({ error: 'Failed to fetch credits' });
  }
});

// Full enriched details (genres, tagline, runtime, IMDB id, directors, writers, keywords, networks)
app.get('/api/details', async (req: Request, res: Response) => {
  try {
    const id = String(req.query.id || '');
    const mode = (req.query.mode as 'movie' | 'tv') || 'movie';
    if (!id) {
      return res.status(400).json({ error: 'Media ID is required' });
    }
    const data = await YifyService.getFullDetails(id, mode);
    res.setHeader('Cache-Control', 'public, max-age=7200, stale-while-revalidate=86400');
    res.json(data);
  } catch (error) {
    console.error('Error fetching full details:', error);
    res.status(500).json({ error: 'Failed to fetch media details' });
  }
});

// Stream Server Health & Availability Probe
// Concurrently tests stream providers for this item and returns the optimal/verified server
app.get('/api/stream/probe', async (req: Request, res: Response) => {
  try {
    const id = String(req.query.id || '');
    const mode = (req.query.mode as 'movie' | 'tv') || 'movie';
    const season = parseInt(String(req.query.season || '1'), 10);
    const episode = parseInt(String(req.query.episode || '1'), 10);
    const isTv = mode === 'tv' || Boolean(req.query.season || req.query.episode);

    if (!id) {
      return res.status(400).json({ error: 'Media ID is required' });
    }

    const candidateServers = [
      {
        index: 0,
        id: 'server3',
        name: 'Server 1',
        url: isTv
          ? `https://player.autoembed.co/embed/tv/${id}/${season}/${episode}`
          : `https://player.autoembed.co/embed/movie/${id}`
      },
      {
        index: 1,
        id: 'server1',
        name: 'Server 2',
        url: isTv
          ? `https://vidlink.pro/tv/${id}/${season}/${episode}`
          : `https://vidlink.pro/movie/${id}`
      },
      {
        index: 2,
        id: 'server2',
        name: 'Server 3',
        url: isTv
          ? `https://vidsrc.pm/embed/tv/${id}/${season}/${episode}`
          : `https://vidsrc.pm/embed/movie/${id}`
      },
      {
        index: 3,
        id: 'server4',
        name: 'Server 4',
        url: isTv
          ? `https://www.2embed.cc/embedtv/${id}?s=${season}&e=${episode}`
          : `https://www.2embed.cc/embed/${id}`
      },
      {
        index: 4,
        id: 'server5',
        name: 'Server 5',
        url: isTv
          ? `https://vidsrc.io/embed/tv/${id}/${season}/${episode}`
          : `https://vidsrc.io/embed/movie/${id}`
      },
      {
        index: 5,
        id: 'server6',
        name: 'Server 6',
        url: isTv
          ? `https://multiembed.mov/?video_id=${id}&tmdb=1&s=${season}&e=${episode}`
          : `https://multiembed.mov/?video_id=${id}&tmdb=1`
      },
      {
        index: 6,
        id: 'server8',
        name: 'Server 7 (CineSrc)',
        url: isTv
          ? `https://cinesrc.st/embed/tv/${id}?s=${season || 1}&e=${episode || 1}&color=%23f59e0b&autoskip=true&autonext=true`
          : `https://cinesrc.st/embed/movie/${id}?color=%23f59e0b&autoskip=true`
      },
      {
        index: 7,
        id: 'server9',
        name: 'Server 8 (Nxsha)',
        url: isTv
          ? `https://web.nxsha.app/embed/tv/${id}/${season || 1}/${episode || 1}`
          : `https://web.nxsha.app/embed/movie/${id}`
      },
      {
        index: 8,
        id: 'server10',
        name: 'Server 9 (VidEasy)',
        url: isTv
          ? `https://player.videasy.to/tv/${id}/${season || 1}/${episode || 1}`
          : `https://player.videasy.to/movie/${id}`
      }
    ];

    const probePromises = candidateServers.map(async (server) => {
      const start = Date.now();
      try {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 2800);
        const upstream = await fetch(server.url, {
          method: 'GET',
          headers: {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
          },
          signal: controller.signal
        });
        clearTimeout(timeout);
        const latency = Date.now() - start;
        const isOk = upstream.status === 200;
        return {
          index: server.index,
          id: server.id,
          name: server.name,
          ok: isOk,
          status: upstream.status,
          latency
        };
      } catch (err: any) {
        return {
          index: server.index,
          id: server.id,
          name: server.name,
          ok: false,
          status: 0,
          latency: Date.now() - start,
          error: err.name === 'AbortError' ? 'timeout' : 'network_error'
        };
      }
    });

    const results = await Promise.all(probePromises);
    const workingServers = results.filter((s) => s.ok);

    // Prefer Server 2 (Vidlink) if online and responsive (<1500ms), as it provides fast, direct playback.
    // Otherwise pick the lowest latency working server.
    let best = workingServers.find((s) => s.index === 1 && s.latency < 1500);
    if (!best && workingServers.length > 0) {
      best = [...workingServers].sort((a, b) => a.latency - b.latency)[0];
    }

    const bestIndex = best ? best.index : 0;
    const bestId = best ? best.id : 'server3';
    const bestName = best ? best.name : 'Server 1';

    res.setHeader('Cache-Control', 'public, max-age=300, stale-while-revalidate=600');
    res.json({
      bestServerIndex: bestIndex,
      bestServerId: bestId,
      bestServerName: bestName,
      servers: results
    });
  } catch (error) {
    console.error('Error probing stream servers:', error);
    res.status(500).json({ error: 'Failed to probe stream servers' });
  }
});

// TMDB trailers & videos (YouTube keys) for Watch Trailer feature
app.get('/api/videos', async (req: Request, res: Response) => {
  try {
    const id = String(req.query.id || '');
    const mode = (req.query.mode as 'movie' | 'tv') || 'movie';
    if (!id) return res.status(400).json({ error: 'Media ID is required' });

    const TMDB_API_KEY = '4ef0d7355d9ffb5151e987764708ce96';
    const url = `https://api.themoviedb.org/3/${mode}/${id}/videos?api_key=${TMDB_API_KEY}&language=en-US`;
    const response = await fetch(url);
    if (!response.ok) throw new Error(`TMDB videos error: ${response.status}`);
    const data = await response.json();

    res.setHeader('Cache-Control', 'public, max-age=86400, stale-while-revalidate=604800');
    res.json(data);
  } catch (error) {
    console.error('Error fetching media videos:', error);
    res.status(500).json({ error: 'Failed to fetch media videos' });
  }
});

// TMDB backdrop & poster images for the slideshow background in the player
app.get('/api/images', async (req: Request, res: Response) => {
  try {
    const id = String(req.query.id || '');
    const mode = (req.query.mode as 'movie' | 'tv') || 'movie';
    if (!id) return res.status(400).json({ error: 'Media ID is required' });

    const TMDB_API_KEY = '4ef0d7355d9ffb5151e987764708ce96';
    const url = `https://api.themoviedb.org/3/${mode}/${id}/images?api_key=${TMDB_API_KEY}&include_image_language=en,null`;
    const response = await fetch(url);
    if (!response.ok) throw new Error(`TMDB images error: ${response.status}`);
    const data = await response.json() as { backdrops?: object[]; posters?: object[] };

    res.setHeader('Cache-Control', 'public, max-age=86400, stale-while-revalidate=604800');
    res.json(data);
  } catch (error) {
    console.error('Error fetching media images:', error);
    res.status(500).json({ error: 'Failed to fetch media images' });
  }
});

// Trending search titles (live from TMDB trending/all/day — same source as IMDB)
app.get('/api/trending-searches', async (_req: Request, res: Response) => {
  try {
    const titles = await YifyService.getTrendingSearches();
    res.setHeader('Cache-Control', 'public, max-age=1800, stale-while-revalidate=3600');
    res.json({ titles });
  } catch (error) {
    console.error('Error fetching trending searches:', error);
    res.status(500).json({ error: 'Failed to fetch trending searches' });
  }
});

// App Version & In-App Update Info (Dynamically reads from version.json)
app.get('/api/app/version', (_req: Request, res: Response) => {
  res.setHeader('Cache-Control', 'no-cache');
  const versionFile = path.resolve(projectRoot, 'version.json');
  if (fs.existsSync(versionFile)) {
    try {
      const data = JSON.parse(fs.readFileSync(versionFile, 'utf-8'));
      return res.json({
        latestVersion: data.version || '1.1.1',
        versionCode: Number(data.versionCode) || 3,
        minSupportedVersion: data.minSupportedVersion || '1.0.0',
        releaseDate: data.releaseDate || new Date().toISOString().split('T')[0],
        title: data.title || 'WATZEM Update Required',
        releaseNotes: data.releaseNotes || [
          'Critical update required to continue streaming',
          'Smoother 1080p and 4K stream playback performance',
          'Refined player controls and background playback',
          'General security & stability updates'
        ],
        downloadUrl: data.downloadUrl || '/download/app',
        fileSize: data.fileSize || '3.9 MB',
        mandatory: data.mandatory !== undefined ? data.mandatory : true
      });
    } catch (err) {
      console.warn('Failed to parse version.json, serving fallback version', err);
    }
  }

  // Fallback
  res.json({
    latestVersion: '1.1.1',
    versionCode: 3,
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
  });
});

// Download APK installer
app.get('/download/app', (_req: Request, res: Response) => {
  const apkPath = path.resolve(projectRoot, 'public/install.apk');
  res.setHeader('Content-Type', 'application/vnd.android.package-archive');
  res.setHeader('Content-Disposition', 'attachment; filename="install.apk"');
  res.sendFile(apkPath);
});

// Serve frontend dist with immutable asset caching
app.use(express.static(clientDistPath, {
  maxAge: '7d',
  index: false,
  setHeaders: (res, filePath) => {
    if (filePath.endsWith('.html')) {
      res.setHeader('Cache-Control', 'no-cache');
    }
  }
}));

// Set dist as the main view on load (redirects Android web visitors to /download)
app.get('/', (req: Request, res: Response) => {
  const userAgent = String(req.headers['user-agent'] || '');
  if (/android/i.test(userAgent)) {
    return res.redirect(302, '/download');
  }
  const indexPath = path.join(clientDistPath, 'index.html');
  if (fs.existsSync(indexPath)) {
    res.setHeader('Cache-Control', 'no-cache');
    res.sendFile(indexPath);
  } else {
    res.status(404).send('Frontend build (dist) not found. Please run "npm run build".');
  }
});

// Dedicated APK download landing page route
app.get('/download', (_req: Request, res: Response) => {
  const indexPath = path.join(clientDistPath, 'index.html');
  if (fs.existsSync(indexPath)) {
    res.setHeader('Cache-Control', 'no-cache');
    res.sendFile(indexPath);
  } else {
    res.status(404).send('Frontend build (dist) not found. Please run "npm run build".');
  }
});

// Client-side SPA fallback for non-API routes
app.get('*', (req: Request, res: Response) => {
  if (req.path.startsWith('/api') || req.path.startsWith('/download/app')) {
    return res.status(404).json({ error: 'Endpoint not found' });
  }
  const indexPath = path.join(clientDistPath, 'index.html');
  if (fs.existsSync(indexPath)) {
    res.setHeader('Cache-Control', 'no-cache');
    res.sendFile(indexPath);
  } else {
    res.status(404).send('Frontend build (dist) not found. Please run "npm run build".');
  }
});

app.listen(PORT, () => {
  console.log(`🍿 WATZEM Server running at http://localhost:${PORT}`);
  console.log(`📁 Serving dist as main view from: ${clientDistPath}`);
});

