# 🍿 WATZEM Movies & TV Mobile App

A high-performance, mobile-first Movie & TV streaming app built using **Node.js + TypeScript**, **Vue 3 + Vite + TypeScript**, and **Capacitor**, powered by the **TMDb API** and multi-source streaming engines.

---

## 📱 Features

- **Movies 🎬 & TV Shows 📺 Switcher**: Fast toggle between movie catalog and TV series with real-time category updates.
- **Dynamic Trending Hero Banner**: Highlighting top trending titles with backdrop art, ratings, and quick actions.
- **Direct Stream Player (VidSrc)**: Built-in HD streaming player with TV season and episode selection.
- **Torrents & Magnet Links Aggregator**:
  - Live indexed torrent sources (The Pirate Bay, 1337x, RuTracker, Knaben, EZTV).
  - Seeders, leechers, file size breakdown, and quality badges (4K UHD, 1080P, 720P, SD).
  - One-tap **Copy Magnet Link** with haptic feedback and confirmation badge.
  - Direct `magnet:` app launch link for mobile torrent clients (LibreTorrent, Flud, µTorrent).
- **Comprehensive Explore Catalog**:
  - **Streaming Networks**: Netflix, HBO / Max, Disney+, Prime Video, Apple TV+, Hulu, Paramount+, Peacock.
  - **Genres**: Action, Adventure, Animation, Comedy, Crime, Sci-Fi, Horror, Fantasy, etc.
  - **Movie Franchises**: The Avengers, Harry Potter, Star Wars, James Bond, Fast & Furious, John Wick, Matrix, etc.
  - **Studios**: Marvel Studios, Pixar, A24, Studio Ghibli, Warner Bros, Disney.
- **Instant Search**: Debounced search with suggestions (Deadpool, Dune, Oppenheimer, Anime, etc.).
- **Watchlist / Bookmarks**: Save favorites locally using persistent storage.
- **Mobile-First Design & Safe Area**:
  - Dark cinematic obsidian theme (`#07070a`) with glowing accent colors.
  - Bottom navigation bar designed for one-handed mobile touch.
  - Notch / Dynamic Island and gesture navigation safe-area padding.
  - Capacitor native status bar and haptic feedback support.

---

## 🛠️ Technology Stack

- **Backend**: Node.js, Express, TypeScript (`tsx`), native fetch, in-memory caching.
- **Frontend**: Vue 3 (Composition API), TypeScript, Vite.
- **Mobile Runtime**: Capacitor (`@capacitor/core`, `@capacitor/cli`, `@capacitor/status-bar`, `@capacitor/haptics`, `@capacitor/clipboard`, `@capacitor/app`).
- **Styling**: Vanilla CSS with customized design tokens and smooth micro-animations.

---

## 🚀 Getting Started

### 1. Run Development Server (Both Frontend & Backend)

```bash
npm run dev
```

- **Frontend (Vite)**: http://localhost:5173
- **Backend API**: http://localhost:2727

### 2. Build for Production

```bash
npm run build
```

This compiles both the Vue 3 frontend (`dist/`) and the TypeScript server (`dist-server/`).

### 3. Run Production Server

```bash
npm start
```

### 4. Run as a Windows Service (via node-windows)

To install the API as an auto-starting background Windows Service:

```bash
# First build the server
npm run build

# Install and start the Windows Service
npm run service:install

# To uninstall the service
npm run service:uninstall
```

---

## 📲 Capacitor Mobile Deployment

### Sync Web Assets with Capacitor

```bash
npm run build:client
npx cap sync
```

### Add Mobile Platforms

- **Android**:
  ```bash
  npm run cap:add:android
  npx cap open android
  ```
- **iOS**:
  ```bash
  npm run cap:add:ios
  npx cap open ios
  ```
