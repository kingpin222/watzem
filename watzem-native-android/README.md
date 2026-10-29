# 🍿 WATZEM Native Android (Kotlin + Jetpack Compose)

A 100% native Android application built in **Kotlin** with modern **Jetpack Compose**, **Material 3**, **AndroidX Media3 (ExoPlayer)**, and **Retrofit 2**.

---

## 📱 Features

- **🎬 Live TMDb Movie & TV Catalog**:
  - Hero carousel with high-resolution backdrops, ratings, synopsis, and quick play buttons.
  - Trending Now, Popular Blockbusters, Top Rated TV Series, Coming Soon to Theaters.
  - Genre-based carousels (Sci-Fi & Cyberpunk, High Octane Action, etc.).
- **🔍 Real-Time Debounced Search**:
  - Instant live search across movies and TV shows with 350ms debounce.
  - Trending search suggestion tags.
- **📄 Immersive Detail Screen**:
  - Ultra-wide backdrop with dynamic gradient overlay.
  - Release year, runtime, TV seasons count, content rating, and genre tags.
  - Full cast & crew carousel with circular actor photos.
  - Official YouTube trailers and teaser previews.
  - Recommended & similar titles carousel.
  - One-tap "Add to Watchlist" bookmarking.
- **▶️ Dual Streaming & Playback Engine**:
  1. **AndroidX Media3 (ExoPlayer)**:
     - Hardware-accelerated native playback for direct MP4 and HLS streams.
     - Custom amber-themed cinema controls: Play/Pause, Replay 10s, Forward 10s, timeline slider scrubber.
     - Aspect ratio toggle (`FIT` vs `ZOOM`).
     - Picture-in-Picture (PiP) support.
  2. **WATZEM Multi-Source Stream Player**:
     - Embedded native player with hardened ad-blocking and redirect trap suppression.
     - 5 switchable streaming servers: **Videasy VIP**, **VidSrc PM**, **CineSrc Pro**, **VidSrc IO**, and **VidSrcMe**.
     - Built-in TV season and episode navigator (`< Prev / Next >`).
     - Auto Picture-in-Picture on Android 12+ (API 31+).
- **⭐ Watchlist & Favorites**:
  - Local persistent storage with reactive `StateFlow` updates across screens.
  - Fast bookmark and removal actions.
- **🎨 Cinema Dark Theme**:
  - Sleek midnight obsidian (`#090C10`) background with amber/gold (`#F59E0B`) accents.
  - Edge-to-edge support with transparent system insets.
  - Adaptive for phones, foldables, tablets, and Android TV.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.1.20
- **UI Framework**: Jetpack Compose + Material 3 (Declarative UI)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern + Kotlin Coroutines & StateFlow
- **Networking**: Retrofit 2.11.0 + OkHttp 4.12.0 + Gson
- **Media Playback**: AndroidX Media3 ExoPlayer 1.4.1 (`media3-exoplayer`, `media3-ui`, `media3-exoplayer-hls`)
- **Image Loading**: Coil Compose 2.7.0 (with 150 MB disk cache & 25% memory cache)
- **Navigation**: Jetpack Navigation Compose 2.8.3
- **Build System**: Android Gradle Plugin 8.7.2 + Gradle 8.11.1

---

## 📂 Project Structure

```
watzem-native-android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/watzem/nativeapp/
│       │   │   ├── WatzemApp.kt              # Application class with Coil cache
│       │   │   ├── MainActivity.kt           # Edge-to-edge Compose container
│       │   │   ├── data/
│       │   │   │   ├── model/                # MediaItem, MediaDetails, StreamServer, etc.
│       │   │   │   ├── remote/               # Retrofit TmdbApiService & TmdbApiClient
│       │   │   │   ├── local/                # WatchlistManager (StateFlow persistence)
│       │   │   │   └── repository/           # MovieRepository
│       │   │   ├── ui/
│       │   │   │   ├── theme/                # Color, Type, Theme (Cinema dark)
│       │   │   │   ├── components/           # Cards, Hero, Chips, Trailers, ServerSheet
│       │   │   │   ├── navigation/           # Screen, BottomNavBar, WatzemNavGraph
│       │   │   │   └── screens/
│       │   │   │       ├── home/             # HomeScreen & HomeViewModel
│       │   │   │       ├── movies/           # MoviesScreen & MoviesViewModel
│       │   │   │       ├── tv/               # TvShowsScreen & TvShowsViewModel
│       │   │   │       ├── search/           # SearchScreen & SearchViewModel
│       │   │   │       ├── detail/           # DetailScreen & DetailViewModel
│       │   │   │       ├── player/           # VideoPlayerScreen & WebStreamPlayerScreen
│       │   │   │       └── watchlist/        # WatchlistScreen & WatchlistViewModel
│       │   └── res/                          # Drawables, mipmaps, strings, themes
│       └── test/                             # Unit tests for Models & Servers
├── build.gradle.kts                          # Root build script
├── settings.gradle.kts                       # Repository management & plugins
└── gradlew                                   # Gradle wrapper script
```

---

## 🚀 How to Build and Run

### 1. In Android Studio
1. Open Android Studio.
2. Select **Open** and choose the `watzem-native-android` folder.
3. Allow Gradle to sync.
4. Select an Android emulator or connected device and click **Run** (`Ctrl+R` / `Shift+F10`).

### 2. From the Command Line
```bash
cd watzem-native-android

# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test

# Install onto connected device/emulator
./gradlew installDebug
```

The compiled APK will be located at:
`watzem-native-android/app/build/outputs/apk/debug/app-debug.apk`

