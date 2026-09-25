<script setup lang="ts">
import { ref, computed } from 'vue';
import { TorrentItem, MediaMode } from '../types';
import { useHaptics } from '../composables/useHaptics';
import { Magnet, HardDrive, Copy, Check, Download, Search } from 'lucide-vue-next';

const props = defineProps<{
  torrents: TorrentItem[];
  sources: string[];
  isLoading: boolean;
  mode: MediaMode;
}>();

const { copyText } = useHaptics();

const selectedQuality = ref<'all' | '1080p' | '4k' | '720p'>('all');
const copiedIndex = ref<number | null>(null);

const formatSize = (bytes: number): string => {
  if (!bytes) return '-';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + units[i];
};

const getQualityInfo = (title: string) => {
  if (!title) return { label: 'SD', cls: 'q-sd' };
  if (/2160p|4K|UHD/i.test(title)) return { label: '4K', cls: 'q-4k' };
  if (/1080p/i.test(title)) return { label: '1080P', cls: 'q-1080' };
  if (/720p/i.test(title)) return { label: '720P', cls: 'q-720' };
  return { label: 'SD', cls: 'q-sd' };
};

const filteredTorrents = computed(() => {
  if (selectedQuality.value === 'all') return props.torrents;
  if (selectedQuality.value === '4k') {
    return props.torrents.filter((t) => /2160p|4K|UHD/i.test(t.title));
  }
  if (selectedQuality.value === '1080p') {
    return props.torrents.filter((t) => /1080p/i.test(t.title));
  }
  if (selectedQuality.value === '720p') {
    return props.torrents.filter((t) => /720p/i.test(t.title));
  }
  return props.torrents;
});

const handleCopyMagnet = async (index: number, magnetUrl: string) => {
  if (!magnetUrl) return;
  const success = await copyText(magnetUrl);
  if (success) {
    copiedIndex.value = index;
    setTimeout(() => {
      if (copiedIndex.value === index) {
        copiedIndex.value = null;
      }
    }, 2500);
  }
};
</script>

<template>
  <div class="torrents-section">
    <!-- Section Top Bar -->
    <div class="torrents-header">
      <div class="title-wrap">
        <h3 class="section-title">
          <Magnet :size="16" class="magnet-symbol" />
          <span>Torrents & Magnets</span>
        </h3>
        <span class="count-badge font-mono">{{ filteredTorrents.length }}</span>
      </div>

      <!-- Quality Filter Pills -->
      <div class="quality-filter-pills">
        <button
          class="q-btn font-mono"
          :class="{ active: selectedQuality === 'all' }"
          @click="selectedQuality = 'all'"
        >
          All
        </button>
        <button
          class="q-btn font-mono"
          :class="{ active: selectedQuality === '1080p' }"
          @click="selectedQuality = '1080p'"
        >
          1080p
        </button>
        <button
          class="q-btn font-mono"
          :class="{ active: selectedQuality === '4k' }"
          @click="selectedQuality = '4k'"
        >
          4K UHD
        </button>
        <button
          class="q-btn font-mono"
          :class="{ active: selectedQuality === '720p' }"
          @click="selectedQuality = '720p'"
        >
          720p
        </button>
      </div>
    </div>

    <!-- Sources info -->
    <div v-if="sources.length > 0" class="sources-row">
      <span class="sources-label">Sources:</span>
      <span v-for="src in sources" :key="src" class="source-tag font-mono">{{ src }}</span>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="loading-state">
      <div class="spinner" :class="{ rose: mode === 'tv' }"></div>
      <span class="loading-text">Searching indexed torrent feeds...</span>
    </div>

    <!-- Empty State -->
    <div v-else-if="filteredTorrents.length === 0" class="empty-torrents">
      <div class="empty-icon">
        <Search :size="32" stroke-width="1.5" />
      </div>
      <h4>No torrents found</h4>
      <p>Try selecting a different quality or check again later.</p>
    </div>

    <!-- Torrent Cards List -->
    <div v-else class="torrents-list">
      <div
        v-for="(t, idx) in filteredTorrents"
        :key="t.hash || idx"
        class="torrent-card"
      >
        <div class="torrent-main-row">
          <span class="quality-badge font-mono" :class="getQualityInfo(t.title).cls">
            {{ getQualityInfo(t.title).label }}
          </span>
          <span class="torrent-name" :title="t.title">{{ t.title }}</span>
        </div>

        <div class="torrent-meta-row">
          <div class="stats-group">
            <span class="stat-pill seeds font-mono" title="Seeders">
              <span class="dot-seed"></span> {{ t.seeds || 0 }}
            </span>
            <span class="stat-pill peers font-mono" title="Leechers/Peers">
              <span class="dot-peer"></span> {{ t.peers || 0 }}
            </span>
            <span class="stat-pill size font-mono">
              <HardDrive :size="11" />
              <span>{{ formatSize(t.bytes) }}</span>
            </span>
            <span v-if="t.source" class="stat-pill source-pill font-mono">
              {{ t.source }}
            </span>
          </div>

          <div class="actions-group">
            <!-- Copy Magnet Button -->
            <button
              class="action-btn copy-btn"
              :class="{ copied: copiedIndex === idx }"
              title="Copy Magnet Link"
              @click="handleCopyMagnet(idx, t.magnetUrl)"
            >
              <Copy v-if="copiedIndex !== idx" :size="12" />
              <Check v-else :size="12" />
              <span v-if="copiedIndex !== idx">Copy</span>
              <span v-else>Copied</span>
            </button>

            <!-- Direct Magnet App Launch -->
            <a
              :href="t.magnetUrl"
              class="action-btn magnet-link-btn"
              :class="{ 'tv-theme': mode === 'tv' }"
              title="Open in Torrent App"
            >
              <Download :size="12" />
              <span>Open</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.torrents-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.torrents-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.section-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.count-badge {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-size: 0.65rem;
  padding: 1px 6px;
  border-radius: 4px;
}

/* Quality Pills */
.quality-filter-pills {
  display: flex;
  gap: 0.25rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  padding: 2px;
  border-radius: 8px;
}

.q-btn {
  padding: 0.25rem 0.55rem;
  font-size: 0.65rem;
  color: var(--text-muted);
  border-radius: 6px;
  transition: all 0.2s ease;
}

.q-btn.active {
  background: var(--accent-amber);
  color: #07070a;
  font-weight: 700;
}

/* Sources */
.sources-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.sources-label {
  font-size: 0.68rem;
  color: var(--text-muted);
}

.source-tag {
  background: rgba(168, 85, 247, 0.15);
  border: 1px solid rgba(168, 85, 247, 0.3);
  color: #c084fc;
  font-size: 0.62rem;
  padding: 1px 5px;
  border-radius: 4px;
}

/* States */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  gap: 0.75rem;
}

.loading-text {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.empty-torrents {
  text-align: center;
  padding: 2rem 1rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.empty-icon {
  font-size: 1.8rem;
  opacity: 0.4;
  margin-bottom: 0.5rem;
}

.empty-torrents h4 {
  font-size: 0.88rem;
  color: var(--text-primary);
  margin-bottom: 0.2rem;
}

.empty-torrents p {
  font-size: 0.75rem;
  color: var(--text-muted);
}

/* Torrent list */
.torrents-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.torrent-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  padding: 0.65rem 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  transition: border-color 0.2s ease;
}

.torrent-card:hover {
  border-color: var(--border-active);
}

.torrent-main-row {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
}

.quality-badge {
  font-size: 0.62rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  text-transform: uppercase;
  flex-shrink: 0;
}

.q-4k {
  background: linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
  color: #fff;
}

.q-1080 {
  background: var(--accent-amber);
  color: #07070a;
}

.q-720 {
  background: var(--accent-cyan);
  color: #07070a;
}

.q-sd {
  background: var(--text-muted);
  color: #fff;
}

.torrent-name {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--text-primary);
  line-height: 1.35;
  word-break: break-all;
}

.torrent-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding-top: 0.4rem;
  border-top: 1px solid var(--border-subtle);
}

.stats-group {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: var(--bg-surface);
  border-radius: 4px;
  padding: 2px 5px;
  font-size: 0.62rem;
}

.stat-pill.seeds {
  color: var(--accent-emerald);
}

.dot-seed {
  width: 5px;
  height: 5px;
  background: var(--accent-emerald);
  border-radius: 50%;
}

.stat-pill.peers {
  color: var(--text-secondary);
}

.dot-peer {
  width: 5px;
  height: 5px;
  background: var(--text-muted);
  border-radius: 50%;
}

.stat-pill.size {
  color: var(--accent-blue);
}

.source-pill {
  color: var(--text-muted);
  text-transform: uppercase;
}

.actions-group {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.35rem 0.6rem;
  border-radius: 6px;
  text-decoration: none;
  transition: all 0.15s ease;
}

.copy-btn {
  background: var(--bg-surface-elevated);
  border: 1px solid var(--border-subtle);
  color: var(--text-secondary);
}

.copy-btn.copied {
  background: var(--accent-amber);
  color: #07070a;
  border-color: var(--accent-amber);
}

.magnet-link-btn {
  background: linear-gradient(135deg, var(--accent-amber), #d97706);
  color: #07070a;
}

.magnet-link-btn.tv-theme {
  background: linear-gradient(135deg, var(--accent-rose), #e11d48);
  color: #fff;
}
</style>
