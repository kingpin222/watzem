import WebTorrent, { Torrent, TorrentFile } from 'webtorrent';
import { Request, Response } from 'express';

const TRACKERS = [
  'udp://tracker.opentrackr.org:1337/announce',
  'udp://open.stealth.si:80/announce',
  'udp://tracker.torrent.eu.org:451/announce',
  'udp://tracker.bittor.pw:1337/announce',
  'udp://explodie.org:6969/announce',
  'udp://tracker.cyberia.is:6969/announce',
  'udp://tracker.moeking.me:6969/announce',
  'http://p4p.arenabg.com:1337/announce',
  'udp://9.rarbg.me:2780/announce',
  'udp://9.rarbg.to:2710/announce',
  'udp://9.rarbg.to:2730/announce'
];

let client: InstanceType<typeof WebTorrent> | null = null;

function getClient(): InstanceType<typeof WebTorrent> {
  if (!client) {
    client = new WebTorrent({
      maxConns: 60
    });
    client.on('error', (err: unknown) => {
      console.error('WebTorrent client error:', err);
    });
  }
  return client;
}

export class TorrentStreamService {
  /**
   * Format torrent ID with tracker announce list if hash only
   */
  static formatTorrentId(hashOrMagnet: string, name?: string): string {
    const trimmed = hashOrMagnet.trim();
    if (trimmed.startsWith('magnet:')) {
      return trimmed;
    }
    const cleanHash = trimmed.toUpperCase();
    const dn = name ? `&dn=${encodeURIComponent(name)}` : '';
    const tr = TRACKERS.map((t) => `&tr=${encodeURIComponent(t)}`).join('');
    return `magnet:?xt=urn:btih:${cleanHash}${dn}${tr}`;
  }

  /**
   * Get an existing torrent or add it to the client
   */
  static async getOrAddTorrent(torrentId: string, timeoutMs = 25000): Promise<Torrent> {
    const wt = getClient();
    const existing = await wt.get(torrentId);
    if (existing) {
      if (existing.ready) return existing;
      return new Promise<Torrent>((resolve) => {
        existing.once('ready', () => resolve(existing));
      });
    }

    return new Promise<Torrent>((resolve, reject) => {
      let settled = false;
      const timer = setTimeout(async () => {
        if (settled) return;
        settled = true;
        const tor = await wt.get(torrentId);
        if (tor && tor.files && tor.files.length > 0) {
          resolve(tor);
        } else {
          reject(new Error('Timed out connecting to peers for movie torrent'));
        }
      }, timeoutMs);

      try {
        const tor = wt.add(torrentId, {
          announce: TRACKERS,
          destroyStoreOnDestroy: true
        } as any);

        tor.once('ready', () => {
          if (settled) return;
          settled = true;
          clearTimeout(timer);
          resolve(tor);
        });

        tor.once('error', (err: unknown) => {
          if (settled) return;
          settled = true;
          clearTimeout(timer);
          reject(err);
        });
      } catch (err) {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        reject(err);
      }
    });
  }

  /**
   * Locate the largest video file in the torrent
   */
  static findMovieFile(torrent: Torrent): TorrentFile | null {
    if (!torrent.files || torrent.files.length === 0) return null;

    const videoExts = ['.mp4', '.mkv', '.avi', '.webm', '.mov', '.m4v'];
    const videoFiles = torrent.files.filter((f: TorrentFile) => {
      const lower = f.name.toLowerCase();
      return videoExts.some((ext) => lower.endsWith(ext));
    });

    if (videoFiles.length > 0) {
      return videoFiles.reduce((prev: TorrentFile, curr: TorrentFile) =>
        curr.length > prev.length ? curr : prev
      );
    }

    return torrent.files.reduce((prev: TorrentFile, curr: TorrentFile) =>
      curr.length > prev.length ? curr : prev
    );
  }

  /**
   * Stream the actual video file directly to the client as an MP4 attachment
   */
  static streamVideo(
    videoFile: TorrentFile,
    req: Request,
    res: Response,
    customTitle?: string
  ): void {
    const rawName = customTitle || videoFile.name;
    const cleanBaseName = rawName.replace(/[/\\?%*:|"<>]/g, '_').replace(/\.[^/.]+$/, '');
    const extension = videoFile.name.split('.').pop()?.toLowerCase() || 'mp4';
    const finalFilename = `${cleanBaseName}.${extension}`;

    const contentType = extension === 'mkv' ? 'video/x-matroska' : extension === 'webm' ? 'video/webm' : 'video/mp4';

    const totalSize = videoFile.length;
    const range = req.headers.range;

    if (range) {
      const parts = range.replace(/bytes=/, '').split('-');
      const start = parseInt(parts[0], 10);
      const end = parts[1] ? parseInt(parts[1], 10) : totalSize - 1;
      const chunkSize = end - start + 1;

      res.status(206);
      res.setHeader('Content-Range', `bytes ${start}-${end}/${totalSize}`);
      res.setHeader('Accept-Ranges', 'bytes');
      res.setHeader('Content-Length', chunkSize);
      res.setHeader('Content-Type', contentType);
      res.setHeader('Content-Disposition', `attachment; filename="${finalFilename}"`);

      const stream = videoFile.createReadStream({ start, end });
      stream.pipe(res);
    } else {
      res.status(200);
      res.setHeader('Content-Length', totalSize);
      res.setHeader('Accept-Ranges', 'bytes');
      res.setHeader('Content-Type', contentType);
      res.setHeader('Content-Disposition', `attachment; filename="${finalFilename}"`);

      const stream = videoFile.createReadStream();
      stream.pipe(res);
    }
  }

  /**
   * Get metadata info about the torrent
   */
  static async getTorrentStatus(hashOrMagnet: string) {
    const wt = getClient();
    const torrentId = this.formatTorrentId(hashOrMagnet);
    const tor = await wt.get(torrentId);
    if (!tor) return null;

    const movieFile = tor.ready ? this.findMovieFile(tor) : null;

    return {
      ready: tor.ready,
      progress: tor.progress,
      downloadSpeed: tor.downloadSpeed,
      numPeers: tor.numPeers,
      fileName: movieFile?.name,
      fileSize: movieFile?.length
    };
  }
}
