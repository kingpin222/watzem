<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { MediaItem, MediaMode, TorrentItem } from '../types';
import { useDownloads } from '../composables/useDownloads';
import { useHaptics } from '../composables/useHaptics';
import { useApi } from '../composables/useApi';
import {
  Download,
  HardDrive,
  Magnet,
  Trash2,
  X,
  CheckCircle2,
  Loader2,
  Sparkles,
  ArrowDownToLine,
  ExternalLink,
  Film
} from 'lucide-vue-next';

const props = defineProps<{
  item: MediaItem;
  mode: MediaMode;
  season?: number;
  episode?: number;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const {
  isDownloaded,
  getDownloadedItem,
  isDownloading,
  downloadActualMovieVideo,
  downloadToDeviceStorage,
  openInTorrentClient,
  removeDownload
} = useDownloads();

const { triggerLight, triggerMedium } = useHaptics();
const { getTorrents } = useApi();

const torrents = ref<TorrentItem[]>([]);
const isLoadingTorrents = ref(true);
const selectedTorrent = ref<TorrentItem | null>(null);

const title = computed(() => props.item.title || props.item.name || 'Movie');
const isCurrentlyDownloaded = computed(() => isDownloaded(props.item.id, props.season, props.episode));
const isCurrentlyDownloading = computed(() => isDownloading(props.item.id, props.season, props.episode));
const downloadedData = computed(() => getDownloadedItem(props.item.id, props.season, props.episode));

const cleanFileName = computed(() => {
  const t = title.value.replace(/[/\\?%*:|"<>]/g, '_');
  const d = props.item.release_date || props.item.first_air_date || '';
  const y = d ? d.substring(0, 4) : '';
  return `${t}${y ? `_${y}` : ''}.mp4`;
});

const formattedSize = computed(() => {
  if (selectedTorrent.value?.bytes) {
    const bytes = selectedTorrent.value.bytes;
    if (bytes < 1024 * 1024 * 1024) {
      return `${(bytes / (1024 * 1024)).toFixed(0)} MB`;
    }
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
  }
  return props.mode === 'tv' ? '450 MB' : '1.85 GB';
});

onMounted(async () => {
  try {
    const d = props.item.release_date || props.item.first_air_date || '';
    const year = d ? d.substring(0, 4) : '';
    const query = props.mode === 'tv' && props.season ? `${title.value} S${String(props.season).padStart(2, '0')}` : title.value;

    const data = await getTorrents(query, year, props.mode);
    if (data.hits && data.hits.length > 0) {
      torrents.value = [...data.hits].sort((a, b) => (b.seeds || 0) - (a.seeds || 0));
      selectedTorrent.value = torrents.value[0];
    }
  } catch (err) {
    console.warn('Failed to load torrent options', err);
  } finally {
    isLoadingTorrents.value = false;
  }
});

const handleDownloadActualMovie = async () => {
  triggerMedium();
  await downloadActualMovieVideo(props.item, props.mode, {
    torrent: selectedTorrent.value || undefined,
    season: props.season,
    episode: props.episode
  });
};

const handleSaveToStorage = async () => {
  triggerMedium();
  await downloadToDeviceStorage(props.item, props.mode, {
    torrent: selectedTorrent.value || undefined,
    season: props.season,
    episode: props.episode
  });
};

const handleOpenTorrentApp = async () => {
  triggerMedium();
  const magnet = selectedTorrent.value?.magnetUrl || downloadedData.value?.magnetUrl;
  if (magnet) {
    await openInTorrentClient(magnet, title.value);
  }
};

const handleDelete = () => {
  triggerLight();
  removeDownload(props.item.id, props.season, props.episode);
};
</script>

<template>
  <div class="sheet-backdrop" @click.self="emit('close')">
    <div class="sheet-modal">
      <!-- Sheet Handle -->
      <div class="sheet-handle-bar">
        <div class="sheet-pill"></div>
      </div>

      <!-- Sheet Header -->
      <div class="sheet-header">
        <div class="sheet-header-info">
          <div class="badge-row">
            <span class="sheet-badge font-mono">MOVIE VIDEO DOWNLOAD</span>
            <span v-if="selectedTorrent?.seeds" class="seed-badge font-mono">
              <Sparkles :size="11" />
              <span>{{ selectedTorrent.seeds }} SEEDS</span>
            </span>
          </div>
          <h3 class="sheet-title font-display">{{ title }}</h3>
        </div>

        <button class="sheet-close" aria-label="Close" @click="emit('close')">
          <X :size="18" />
        </button>
      </div>

      <!-- Status banner if already downloaded -->
      <div v-if="isCurrentlyDownloaded" class="downloaded-banner">
        <div class="banner-left">
          <CheckCircle2 :size="18" class="icon-check" />
          <div class="banner-text">
            <span class="banner-title">Saved in Storage</span>
            <span class="banner-file font-mono">{{ downloadedData?.fileName || cleanFileName }}</span>
          </div>
        </div>

        <button class="btn-trash-storage" title="Delete from storage" @click="handleDelete">
          <Trash2 :size="15" />
        </button>
      </div>

      <!-- Sheet Body: Options -->
      <div class="sheet-body">
        <!-- Option 1: Direct MP4 Video File Download to Storage -->
        <div class="download-option-card featured-download">
          <div class="opt-header">
            <div class="opt-icon-box amber">
              <Film :size="20" />
            </div>
            <div class="opt-meta">
              <div class="opt-title-row">
                <h4 class="opt-title">Download Movie Video (MP4)</h4>
                <span class="opt-tag amber-tag font-mono">Actual Video</span>
              </div>
              <p class="opt-desc">
                Downloads the full 1080p MP4/MKV movie video file directly into your device's Downloads storage.
              </p>
            </div>
          </div>

          <div class="opt-footer">
            <div class="file-info-badge font-mono">
              <span>Size: {{ formattedSize }}</span>
            </div>

            <button
              class="btn-action-primary"
              :disabled="isCurrentlyDownloading"
              @click="handleDownloadActualMovie"
            >
              <Loader2 v-if="isCurrentlyDownloading" :size="15" class="spin" />
              <CheckCircle2 v-else-if="isCurrentlyDownloaded" :size="15" />
              <Download v-else :size="15" />
              <span>
                {{ isCurrentlyDownloading ? 'Starting Download...' : isCurrentlyDownloaded ? 'Download Again' : 'Download MP4 Movie' }}
              </span>
            </button>
          </div>
        </div>

        <!-- Option 2: Launch in Torrent Downloader -->
        <div class="download-option-card">
          <div class="opt-header">
            <div class="opt-icon-box rose">
              <Magnet :size="20" />
            </div>
            <div class="opt-meta">
              <div class="opt-title-row">
                <h4 class="opt-title">Open in Torrent Downloader</h4>
                <span class="opt-tag rose-tag font-mono">P2P Fast</span>
              </div>
              <p class="opt-desc">
                Opens LibreTorrent, Flud, uTorrent, or Folx to download the full video file at peer-to-peer speeds.
              </p>
            </div>
          </div>

          <div class="opt-footer">
            <span class="torrent-source font-mono">
              {{ selectedTorrent?.source || 'YTS Swarm' }}
            </span>

            <button
              class="btn-action-secondary"
              :disabled="isLoadingTorrents || (!selectedTorrent && !downloadedData?.magnetUrl)"
              @click="handleOpenTorrentApp"
            >
              <ExternalLink :size="14" />
              <span>Open in Downloader</span>
            </button>
          </div>
        </div>

        <!-- Option 3: Raw Torrent File -->
        <div class="download-option-card sub-card">
          <div class="opt-header">
            <div class="opt-icon-box muted">
              <ArrowDownToLine :size="18" />
            </div>
            <div class="opt-meta">
              <div class="opt-title-row">
                <h4 class="opt-title">Save .torrent File</h4>
                <span class="opt-tag font-mono">.torrent</span>
              </div>
              <p class="opt-desc">
                Save just the raw .torrent metadata file to device storage.
              </p>
            </div>
          </div>

          <div class="opt-footer">
            <button
              class="btn-action-tertiary"
              :disabled="isCurrentlyDownloading"
              @click="handleSaveToStorage"
            >
              <ArrowDownToLine :size="13" />
              <span>Download .torrent</span>
            </button>
          </div>
        </div>

        <!-- Storage Directory Note -->
        <div class="storage-tip">
          <HardDrive :size="14" class="tip-icon" />
          <span>Files are saved directly into your device's <strong>Downloads</strong> folder.</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sheet-backdrop {
  position: fixed;
  inset: 0;
  z-index: 120;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.sheet-modal {
  width: 100%;
  max-width: 520px;
  background: #0e1017;
  border-top-left-radius: var(--radius-lg);
  border-top-right-radius: var(--radius-lg);
  border: 1px solid var(--border-subtle);
  border-bottom: none;
  box-shadow: 0 -12px 40px rgba(0, 0, 0, 0.8);
  padding: 0.75rem 1.25rem 2rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  animation: slideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.sheet-handle-bar {
  display: flex;
  justify-content: center;
  padding: 0.25rem 0;
}

.sheet-pill {
  width: 36px;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.2);
}

.sheet-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.badge-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.3rem;
}

.sheet-badge {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--accent-amber);
  background: rgba(245, 158, 11, 0.12);
  padding: 2px 6px;
  border-radius: 4px;
  letter-spacing: 0.05em;
}

.seed-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--accent-emerald);
  background: rgba(16, 185, 129, 0.12);
  padding: 2px 6px;
  border-radius: 4px;
}

.sheet-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.25;
}

.sheet-close {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.sheet-close:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
}

/* Downloaded Banner */
.downloaded-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: var(--radius-sm);
  padding: 0.65rem 0.85rem;
}

.banner-left {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  min-width: 0;
}

.icon-check {
  color: var(--accent-emerald);
  flex-shrink: 0;
}

.banner-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.banner-title {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--text-primary);
}

.banner-file {
  font-size: 0.68rem;
  color: var(--accent-emerald);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.btn-trash-storage {
  background: transparent;
  border: none;
  color: var(--text-muted);
  padding: 6px;
  border-radius: 4px;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: all 0.15s ease;
}

.btn-trash-storage:hover {
  color: var(--accent-rose);
  background: rgba(225, 29, 72, 0.15);
}

/* Sheet Body */
.sheet-body {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.download-option-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  transition: all 0.2s ease;
}

.download-option-card:hover {
  border-color: var(--border-active);
  background: var(--bg-card-hover);
}

.opt-header {
  display: flex;
  gap: 0.75rem;
}

.opt-icon-box {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-sm);
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.opt-icon-box.amber {
  background: rgba(245, 158, 11, 0.15);
  color: var(--accent-amber);
}

.opt-icon-box.rose {
  background: rgba(225, 29, 72, 0.15);
  color: var(--accent-rose);
}

.opt-meta {
  flex: 1;
  min-width: 0;
}

.opt-title-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 2px;
}

.opt-title {
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--text-primary);
}

.opt-tag {
  font-size: 0.62rem;
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-secondary);
  padding: 1px 5px;
  border-radius: 4px;
}

.opt-tag.rose-tag {
  background: rgba(225, 29, 72, 0.15);
  color: var(--accent-rose);
}

.opt-desc {
  font-size: 0.75rem;
  color: var(--text-secondary);
  line-height: 1.35;
}

.opt-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding-top: 0.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.file-info-badge {
  font-size: 0.72rem;
  color: var(--text-muted);
}

.torrent-source {
  font-size: 0.7rem;
  color: var(--text-muted);
}

.btn-action-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--accent-amber);
  color: #07070a;
  font-size: 0.78rem;
  font-weight: 700;
  padding: 0.45rem 0.95rem;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-action-primary:hover:not(:disabled) {
  box-shadow: 0 4px 14px var(--accent-amber-glow);
  transform: translateY(-1px);
}

.btn-action-secondary {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: rgba(225, 29, 72, 0.15);
  border: 1px solid rgba(225, 29, 72, 0.35);
  color: #fff;
  font-size: 0.78rem;
  font-weight: 700;
  padding: 0.45rem 0.95rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-action-secondary:hover:not(:disabled) {
  background: rgba(225, 29, 72, 0.3);
  border-color: rgba(225, 29, 72, 0.6);
  transform: translateY(-1px);
}

.btn-action-tertiary {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-action-tertiary:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.featured-download {
  border-color: rgba(245, 158, 11, 0.35);
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.06) 0%, rgba(18, 20, 29, 0.9) 100%);
}

.opt-tag.amber-tag {
  background: rgba(245, 158, 11, 0.18);
  color: var(--accent-amber);
  font-weight: 700;
}

.sub-card {
  opacity: 0.85;
}

.opt-icon-box.muted {
  background: rgba(255, 255, 255, 0.06);
  color: var(--text-muted);
}

.storage-tip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.72rem;
  color: var(--text-muted);
  background: rgba(255, 255, 255, 0.03);
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-sm);
}

.tip-icon {
  color: var(--accent-amber);
  flex-shrink: 0;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (orientation: landscape) {
  .sheet-modal {
    max-height: 90vh;
    overflow-y: auto;
    padding: 0.5rem max(1.25rem, var(--safe-left, 0px)) 1.25rem;
    padding-right: max(1.25rem, var(--safe-right, 0px));
    gap: 0.65rem;
  }
}
</style>
