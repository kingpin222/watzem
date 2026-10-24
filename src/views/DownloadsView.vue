<script setup lang="ts">
import { ref, computed } from 'vue';
import { MediaItem, DownloadedItem } from '../types';
import { useDownloads } from '../composables/useDownloads';
import { useApi } from '../composables/useApi';
import { useHaptics } from '../composables/useHaptics';
import {
  Download,
  Play,
  Trash2,
  HardDrive,
  Film,
  Tv,
  CheckCircle2,
  Compass,
  Magnet,
  ArrowDownToLine
} from 'lucide-vue-next';

defineProps<{
  mode?: string;
}>();

const emit = defineEmits<{
  (e: 'play', item: MediaItem): void;
  (e: 'select', item: MediaItem): void;
  (e: 'explore'): void;
}>();

const {
  downloads,
  removeDownload,
  clearAllDownloads,
  totalStorageFormatted,
  downloadActualMovieVideo,
  openInTorrentClient
} = useDownloads();
const { getImageUrl } = useApi();
const { triggerLight, triggerMedium } = useHaptics();

const filterType = ref<'all' | 'movie' | 'tv'>('all');

const filteredDownloads = computed(() => {
  if (filterType.value === 'all') return downloads.value;
  return downloads.value.filter((d) => d.mode === filterType.value);
});

const handlePlayItem = (d: DownloadedItem) => {
  triggerLight();
  emit('play', d.item);
};

const handleSelectItem = (d: DownloadedItem) => {
  triggerLight();
  emit('select', d.item);
};

const formatDate = (timestamp: number) => {
  const d = new Date(timestamp);
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
};

const handleReDownload = async (d: DownloadedItem) => {
  triggerLight();
  await downloadActualMovieVideo(d.item, d.mode, {
    season: d.season,
    episode: d.episode
  });
};

const handleOpenMagnet = async (d: DownloadedItem) => {
  triggerMedium();
  if (d.magnetUrl) {
    await openInTorrentClient(d.magnetUrl, d.item.title || d.item.name);
  }
};
</script>

<template>
  <div class="downloads-view">
    <!-- Header -->
    <div class="downloads-header">
      <div class="header-top">
        <div>
          <h2 class="downloads-title font-display">Play Later</h2>
          <span class="downloads-sub">Downloaded for offline streaming</span>
        </div>

        <button
          v-if="downloads.length > 0"
          class="btn-clear-all"
          title="Clear all downloads"
          @click="clearAllDownloads"
        >
          <Trash2 :size="14" />
          <span>Clear All</span>
        </button>
      </div>

      <!-- Storage Info Card -->
      <div v-if="downloads.length > 0" class="storage-card">
        <div class="storage-info">
          <HardDrive :size="16" class="storage-icon" />
          <span class="storage-label">Device Storage Used:</span>
          <span class="storage-value font-mono">{{ totalStorageFormatted }}</span>
        </div>
        <span class="storage-count font-mono">{{ downloads.length }} title{{ downloads.length === 1 ? '' : 's' }}</span>
      </div>

      <!-- Filter Pills -->
      <div v-if="downloads.length > 0" class="filter-pills">
        <button
          class="pill-opt"
          :class="{ active: filterType === 'all' }"
          @click="filterType = 'all'"
        >
          All ({{ downloads.length }})
        </button>
        <button
          class="pill-opt"
          :class="{ active: filterType === 'movie' }"
          @click="filterType = 'movie'"
        >
          <Film :size="12" />
          <span>Movies</span>
        </button>
        <button
          class="pill-opt"
          :class="{ active: filterType === 'tv' }"
          @click="filterType = 'tv'"
        >
          <Tv :size="12" />
          <span>TV Shows</span>
        </button>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="downloads.length === 0" class="empty-downloads">
      <div class="empty-icon-box">
        <Download :size="48" :stroke-width="1.5" />
      </div>
      <h3 class="empty-title">No offline downloads yet</h3>
      <p class="empty-text">
        Tap <strong>"Play Later"</strong> on any movie or TV series to save it for smooth offline streaming on the go.
      </p>
      <button class="btn-discover" @click="emit('explore')">
        <Compass :size="16" />
        <span>Find Movies to Download</span>
      </button>
    </div>

    <!-- Downloaded Items List -->
    <div v-else class="downloads-list">
      <div
        v-for="d in filteredDownloads"
        :key="d.id + (d.season ? `-S${d.season}E${d.episode}` : '')"
        class="download-card"
        @click="handleSelectItem(d)"
      >
        <!-- Poster Thumbnail -->
        <div class="card-poster">
          <img
            v-if="d.item.poster_path"
            :src="getImageUrl(d.item.poster_path, 'w185')"
            :alt="d.item.title || d.item.name"
            class="poster-img"
            @error="(e: any) => e.target.style.display = 'none'"
          />
          <div v-else class="poster-fallback">
            <Film :size="24" />
          </div>

          <div class="play-overlay" @click.stop="handlePlayItem(d)">
            <div class="play-circle">
              <Play :size="14" fill="currentColor" />
            </div>
          </div>
        </div>

        <!-- Details Info -->
        <div class="card-details">
          <div class="title-row">
            <h4 class="item-title font-display">{{ d.item.title || d.item.name }}</h4>
            <span v-if="d.season && d.episode" class="ep-badge font-mono">
              S{{ d.season }}:E{{ d.episode }}
            </span>
          </div>

          <div class="meta-row">
            <span class="status-badge font-mono">
              <CheckCircle2 :size="12" class="check-icon" />
              <span>In Storage</span>
            </span>
            <span class="size-pill font-mono">{{ d.formattedSize }}</span>
            <span class="date-txt">{{ formatDate(d.downloadedAt) }}</span>
          </div>

          <div v-if="d.fileName" class="file-path-row font-mono">
            <HardDrive :size="11" class="fp-icon" />
            <span class="file-name-text">{{ d.fileName }}</span>
          </div>

          <!-- Quick Action Buttons -->
          <div class="card-actions">
            <button class="btn-play-offline" @click.stop="handlePlayItem(d)">
              <Play :size="13" fill="currentColor" />
              <span>Watch</span>
            </button>

            <div class="right-actions-row">
              <button
                class="btn-sub-action"
                title="Save file to device storage"
                @click.stop="handleReDownload(d)"
              >
                <ArrowDownToLine :size="13" />
              </button>

              <button
                v-if="d.magnetUrl"
                class="btn-sub-action magnet-action"
                title="Open in Torrent Downloader (LibreTorrent / Flud / uTorrent)"
                @click.stop="handleOpenMagnet(d)"
              >
                <Magnet :size="13" />
              </button>

              <button
                class="btn-delete-download"
                title="Delete from device"
                @click.stop="removeDownload(d.id, d.season, d.episode)"
              >
                <Trash2 :size="13" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Downloads View - 100% Fluid */
.downloads-view {
  width: 100%;
  max-width: 100%;
  padding: 1.25rem max(clamp(1rem, 2.5vw, 2.5rem), var(--safe-left, 0px)) 3rem;
  padding-right: max(clamp(1rem, 2.5vw, 2.5rem), var(--safe-right, 0px));
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

@media (min-width: 768px) {
  .downloads-view {
    padding: 1.75rem max(clamp(1.5rem, 3vw, 3rem), var(--safe-left, 0px)) 4rem;
    padding-right: max(clamp(1.5rem, 3vw, 3rem), var(--safe-right, 0px));
  }
}

.downloads-header {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--border-subtle);
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.downloads-title {
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--text-primary);
}

.downloads-sub {
  font-size: 0.78rem;
  color: var(--text-muted);
}

.btn-clear-all {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.35rem 0.65rem;
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;
}

.btn-clear-all:hover {
  background: rgba(225, 29, 72, 0.2);
  border-color: rgba(225, 29, 72, 0.4);
  color: #fff;
}

.storage-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  padding: 0.6rem 0.85rem;
}

.storage-info {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.75rem;
}

.storage-icon {
  color: var(--accent-amber);
}

.storage-label {
  color: var(--text-muted);
}

.storage-value {
  color: var(--text-primary);
  font-weight: 700;
}

.storage-count {
  font-size: 0.7rem;
  color: var(--text-secondary);
  background: var(--bg-surface-elevated);
  padding: 1px 6px;
  border-radius: 4px;
}

.filter-pills {
  display: flex;
  gap: 0.4rem;
}

.pill-opt {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.35rem 0.75rem;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  color: var(--text-secondary);
  transition: all 0.2s ease;
}

.pill-opt.active {
  background: var(--accent-amber);
  color: #07070a;
  font-weight: 700;
}

/* Empty State */
.empty-downloads {
  text-align: center;
  padding: 4rem 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.empty-icon-box {
  color: var(--text-muted);
  opacity: 0.4;
  margin-bottom: 0.5rem;
}

.empty-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-primary);
}

.empty-text {
  font-size: 0.82rem;
  color: var(--text-secondary);
  max-width: 320px;
  line-height: 1.5;
}

.btn-discover {
  margin-top: 0.85rem;
  padding: 0.7rem 1.4rem;
  background: var(--accent-amber);
  color: #07070a;
  font-size: 0.85rem;
  font-weight: 700;
  border-radius: var(--radius-full);
  box-shadow: 0 4px 16px var(--accent-amber-glow);
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

/* Downloads List - Fluid Grid */
.downloads-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

@media (min-width: 768px) {
  .downloads-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
  }
}

@media (min-width: 1440px) {
  .downloads-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 1.25rem;
  }
}

.download-card {
  display: flex;
  gap: 0.85rem;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.download-card:hover {
  border-color: var(--border-active);
  background: var(--bg-card-hover);
}

.card-poster {
  position: relative;
  width: 72px;
  height: 104px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--bg-surface);
  flex-shrink: 0;
}

.poster-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.poster-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--text-muted);
}

.play-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: grid;
  place-items: center;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.download-card:hover .play-overlay {
  opacity: 1;
}

.play-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--accent-amber);
  color: #07070a;
  display: grid;
  place-items: center;
}

.play-circle svg {
  margin-left: 2px;
}

.card-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.5rem;
}

.item-title {
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ep-badge {
  background: var(--accent-rose);
  color: #fff;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 4px;
  white-space: nowrap;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin: 0.35rem 0;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 0.68rem;
  color: var(--accent-emerald);
  font-weight: 600;
}

.check-icon {
  color: var(--accent-emerald);
}

.size-pill {
  font-size: 0.68rem;
  color: var(--text-secondary);
  background: var(--bg-surface);
  padding: 1px 5px;
  border-radius: 4px;
}

.date-txt {
  font-size: 0.68rem;
  color: var(--text-muted);
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-top: 0.2rem;
}

.btn-play-offline {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: var(--accent-amber);
  color: #07070a;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.4rem 0.85rem;
  border-radius: var(--radius-sm);
}

.file-path-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.68rem;
  color: var(--accent-emerald);
  background: rgba(16, 185, 129, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
  margin-bottom: 0.3rem;
  width: fit-content;
  max-width: 100%;
}

.fp-icon {
  flex-shrink: 0;
}

.file-name-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.right-actions-row {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.btn-sub-action {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-sub-action:hover {
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.35);
  color: var(--accent-amber);
}

.btn-sub-action.magnet-action:hover {
  background: rgba(225, 29, 72, 0.15);
  border-color: rgba(225, 29, 72, 0.35);
  color: var(--accent-rose);
}

.btn-delete-download {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  color: var(--text-muted);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-delete-download:hover {
  background: rgba(225, 29, 72, 0.2);
  border-color: rgba(225, 29, 72, 0.4);
  color: #fff;
}

@media (orientation: landscape) {
  .downloads-view {
    padding: 1rem max(clamp(1.25rem, 2.5vw, 2.5rem), var(--safe-left, 0px)) 3rem;
    padding-right: max(clamp(1.25rem, 2.5vw, 2.5rem), var(--safe-right, 0px));
    gap: 0.85rem;
  }
  .downloads-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
    gap: 0.85rem;
  }
}
</style>
