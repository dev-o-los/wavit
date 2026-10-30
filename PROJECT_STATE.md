# 🎵 Wavit — Project Master State & Architecture Document

> **Last Updated:** 2026-10-30  
> **Status:** Phase 1 Complete (Backend & API Engine Ready)  
> **Project Root:** `/Users/utkarsh/Documents/wavit`  
> **Maintainer / Agent Rule:** **This file must be updated after every chat session and response** to log all changes, new files, new features, and architectural decisions. Any future agent or developer must read this file first before making changes.

---

## 1. Project Overview & Identity

- **Project Name:** Wavit
- **Goal:** Modern, high-performance, open-source music player built with **Kotlin Multiplatform (KMP)** and **Ktor**, powered by YouTube Music's internal InnerTube API without requiring official API keys or user sign-in for core playback.
- **Phase 1 Goal:** Standalone, testable **Backend-only** (No UI). All business logic, InnerTube wrappers, data parsers, stream resolvers, queue controllers, and REST routes.
- **Future Phase 2 Goal:** Multiplatform UI using Compose Multiplatform (Android, Desktop, iOS) consuming this backend.

---

## 2. Architecture & Design Principles

```
┌─────────────────────────────────────────────────────────┐
│                 Client Layer (HTTP / UI)                │
│       Compose Multiplatform App / Web / cURL / CLI      │
└────────────────────────────┬────────────────────────────┘
                             │ HTTP Requests (JSON)
┌────────────────────────────▼────────────────────────────┐
│              Server Module (`:server`)                  │
│  ┌───────────────────────────────────────────────────┐  │
│  │ Application.kt (Ktor Engine, Netty, StatusPages)  │  │
│  └─────────────────────────┬─────────────────────────┘  │
│                            │ Routes                     │
│  ┌─────────────────────────▼─────────────────────────┐  │
│  │ Route Layer (/search, /song, /album, /stream, ...) │  │
│  └─────────────────────────┬─────────────────────────┘  │
│                            │ Calls                      │
│  ┌─────────────────────────▼─────────────────────────┐  │
│  │ Repository Layer (MusicRepository, SongInfoRepo)  │  │
│  └─────────────────────────┬─────────────────────────┘  │
│                            │ Coordinates                │
│  ┌─────────────────────────▼─────────────────────────┐  │
│  │ InnerTube Layer (Client, Parser, StreamResolver)  │  │
│  └───────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────┘
                             │ Uses Pure Domain Models
┌────────────────────────────▼────────────────────────────┐
│              Shared Module (`:shared`)                  │
│  ┌───────────────────────────────────────────────────┐  │
│  │  `commonMain` (Zero platform dependencies)        │  │
│  │  - Domain Models (Song, Album, Artist, StreamInfo)│  │
│  │  - Track Control Queue (PlaybackQueue, Shuffle)   │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Key Architectural Guidelines
1. **Separation of Concerns:**
   - **`shared` Module (`commonMain`):** Zero platform dependencies. No `java.*`, no `android.*`, no `io.ktor.server.*`. Pure Kotlin data classes (`@Serializable`) and data-structure algorithms (e.g., `PlaybackQueue`).
   - **`server` Module (`jvm`):** Encapsulates all network I/O, Ktor server endpoints, InnerTube client requests, stream extraction heuristics, and scraping.
2. **Resilience & Bot-Protection Bypass:**
   - `InnerTubeClient` generates and caches `visitorData` from `https://www.youtube.com/sw.js_data` to ensure YouTube's backend recognizes requests as a valid client rather than an automated bot.
   - `StreamResolver` leverages mobile client identities (`IOS`, `ANDROID_VR`) that return direct, non-throttled audio stream URLs in `adaptiveFormats` without complex cipher deciphering.
3. **In-Memory Caching:**
   - Stream URLs and song metadata use concurrent in-memory caching with TTL (10 minutes) to eliminate duplicate requests to YouTube.
4. **Structured Error Handling:**
   - StatusPages plugin catches exceptions and converts them into standardized JSON error responses with proper HTTP status codes.

---

## 3. Complete File & Directory Manifest

### 3.1 Root Configuration
| File Path | Description |
|---|---|
| [`settings.gradle.kts`](file:///Users/utkarsh/Documents/wavit/settings.gradle.kts) | Declares project name `wavit`, includes `:shared` and `:server` modules, and configures Maven Central & JitPack repositories. |
| [`build.gradle.kts`](file:///Users/utkarsh/Documents/wavit/build.gradle.kts) | Root buildscript applying Kotlin 2.1.0 Multiplatform, JVM, Serialization, and Ktor 3.0.3 plugins. |
| [`gradle.properties`](file:///Users/utkarsh/Documents/wavit/gradle.properties) | JVM memory settings (`-Xmx2048m`), UTF-8 encoding, and IPv4 preference. |
| [`gradlew`](file:///Users/utkarsh/Documents/wavit/gradlew) / [`gradlew.bat`](file:///Users/utkarsh/Documents/wavit/gradlew.bat) | Gradle wrapper binaries (Gradle 8.11.1). |
| [`test-api.sh`](file:///Users/utkarsh/Documents/wavit/test-api.sh) | Automated curl test script testing all 11 core API endpoints with colored status outputs. |
| [`PROJECT_STATE.md`](file:///Users/utkarsh/Documents/wavit/PROJECT_STATE.md) | **This file.** The living source of truth for the entire project. |

### 3.2 Shared Module (`:shared`) — Multiplatform Domain
| File Path | Description |
|---|---|
| [`shared/build.gradle.kts`](file:///Users/utkarsh/Documents/wavit/shared/build.gradle.kts) | KMP configuration targeting `jvm()` with `kotlinx-serialization-json` and `kotlinx-coroutines-core`. |
| [`shared/src/commonMain/kotlin/com/wavit/models/Song.kt`](file:///Users/utkarsh/Documents/wavit/shared/src/commonMain/kotlin/com/wavit/models/Song.kt) | Core playable song model: `videoId`, `title`, `artist`, `durationMillis`, `thumbnailUrl`, `albumId`, `albumName`, `isExplicit`, `isVideo`. |
| [`shared/src/commonMain/kotlin/com/wavit/models/ArtistRef.kt`](file:///Users/utkarsh/Documents/wavit/shared/src/commonMain/kotlin/com/wavit/models/ArtistRef.kt) | Credited artist reference with `name` and YouTube `browseId` channel link. |
| [`shared/src/commonMain/kotlin/com/wavit/models/Album.kt`](file:///Users/utkarsh/Documents/wavit/shared/src/commonMain/kotlin/com/wavit/models/Album.kt) | Full album model: `browseId`, `title`, `artist`, `year`, `thumbnailUrl`, `description`, and `tracks: List<Song>`. |
| [`shared/src/commonMain/kotlin/com/wavit/models/ArtistPage.kt`](file:///Users/utkarsh/Documents/wavit/shared/src/commonMain/kotlin/com/wavit/models/ArtistPage.kt) | Artist landing page: `browseId`, `name`, `description`, `thumbnailUrl`, `subscriberCountText`, `topSongs: List<Song>`, `sections: List<HomeShelf>`. |
| [`shared/src/commonMain/kotlin/com/wavit/models/SearchResult.kt`](file:///Users/utkarsh/Documents/wavit/shared/src/commonMain/kotlin/com/wavit/models/SearchResult.kt) | Search result model + `SearchFilter` enum (`ALL`, `SONGS`, `VIDEOS`, `ALBUMS`, `ARTISTS`, `PLAYLISTS`) and `BrowseItem`. |
| [`shared/src/commonMain/kotlin/com/wavit/models/HomeShelf.kt`](file:///Users/utkarsh/Documents/wavit/shared/src/commonMain/kotlin/com/wavit/models/HomeShelf.kt) | Home & browse models: `HomeShelf`, `ShelfItem`, `HomeFeed`, `MoodGenre`, `MoodGenreSection`. |
| [`shared/src/commonMain/kotlin/com/wavit/models/StreamInfo.kt`](file:///Users/utkarsh/Documents/wavit/shared/src/commonMain/kotlin/com/wavit/models/StreamInfo.kt) | Direct audio stream metadata: `videoId`, `url`, `mimeType`, `bitrateKbps`, `format` (`m4a`/`opus`), `durationSeconds`, `loudnessDb`. |
| [`shared/src/commonMain/kotlin/com/wavit/models/SongInfo.kt`](file:///Users/utkarsh/Documents/wavit/shared/src/commonMain/kotlin/com/wavit/models/SongInfo.kt) | Extended watch metadata (`views`, `likes`, `releaseDate`) and synced lyrics models (`LyricLine`, `Lyrics`). |
| [`shared/src/commonMain/kotlin/com/wavit/queue/PlaybackQueue.kt`](file:///Users/utkarsh/Documents/wavit/shared/src/commonMain/kotlin/com/wavit/queue/PlaybackQueue.kt) | Pure data-level queue manager handling `next()`, `previous()`, `playNext()`, `addToEnd()`, `appendAutoplayTracks()`, `setShuffle()`, `setRepeatMode()`. |
| [`shared/src/commonTest/kotlin/com/wavit/queue/PlaybackQueueTest.kt`](file:///Users/utkarsh/Documents/wavit/shared/src/commonTest/kotlin/com/wavit/queue/PlaybackQueueTest.kt) | Complete unit test suite verifying queue navigation, wrapping, repeat modes, and autoplay insertions. |

### 3.3 Server Module (`:server`) — Ktor Backend & InnerTube Engine
| File Path | Description |
|---|---|
| [`server/build.gradle.kts`](file:///Users/utkarsh/Documents/wavit/server/build.gradle.kts) | Configures Ktor Netty Server, Ktor CIO Client, ContentNegotiation, CORS, StatusPages, CallLogging, and Jsoup. |
| [`server/src/main/resources/logback.xml`](file:///Users/utkarsh/Documents/wavit/server/src/main/resources/logback.xml) | Console logging configuration with thread and class formatting. |
| [`server/src/main/kotlin/com/wavit/Application.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/Application.kt) | Server entry point on port 8080 (configurable via `PORT` env). Installs plugins and registers all route modules. |
| [`server/src/main/kotlin/com/wavit/innertube/InnerTubeClient.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/innertube/InnerTubeClient.kt) | Low-level HTTP client executing requests against YouTube's `/youtubei/v1` endpoints with visitor ID generation. |
| [`server/src/main/kotlin/com/wavit/innertube/InnerTubeParser.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/innertube/InnerTubeParser.kt) | Depth-first JSON parser extracting clean domain objects from raw InnerTube renderer hierarchies. |
| [`server/src/main/kotlin/com/wavit/innertube/StreamResolver.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/innertube/StreamResolver.kt) | Resolves direct streaming URLs using mobile client identities (`IOS`, `ANDROID_VR`) with quality selection and caching. |
| [`server/src/main/kotlin/com/wavit/repository/MusicRepository.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/repository/MusicRepository.kt) | High-level business repository providing unified API for search, albums, artists, radio, home, and streaming. |
| [`server/src/main/kotlin/com/wavit/repository/SongInfoRepository.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/repository/SongInfoRepository.kt) | Web scraper extracting view counts, likes, and official release dates from YouTube watch pages using Jsoup. |
| [`server/src/main/kotlin/com/wavit/routes/SearchRoutes.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/routes/SearchRoutes.kt) | Endpoints: `GET /search`, `GET /search/suggestions`. |
| [`server/src/main/kotlin/com/wavit/routes/SongRoutes.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/routes/SongRoutes.kt) | Endpoints: `GET /song/{id}`, `GET /song/{id}/info`, `GET /song/{id}/related`, `GET /song/{id}/lyrics`. |
| [`server/src/main/kotlin/com/wavit/routes/AlbumRoutes.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/routes/AlbumRoutes.kt) | Endpoint: `GET /album/{id}`. |
| [`server/src/main/kotlin/com/wavit/routes/ArtistRoutes.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/routes/ArtistRoutes.kt) | Endpoint: `GET /artist/{id}`. |
| [`server/src/main/kotlin/com/wavit/routes/StreamRoutes.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/routes/StreamRoutes.kt) | Endpoint: `GET /stream/{id}`. |
| [`server/src/main/kotlin/com/wavit/routes/HomeRoutes.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/routes/HomeRoutes.kt) | Endpoints: `GET /home`, `GET /explore`. |
| [`server/src/main/kotlin/com/wavit/routes/QueueRoutes.kt`](file:///Users/utkarsh/Documents/wavit/server/src/main/kotlin/com/wavit/routes/QueueRoutes.kt) | Endpoints: `GET /queue`, `POST /queue/set`, `POST /queue/next`, `POST /queue/previous`, `POST /queue/add`, `POST /queue/shuffle`, `POST /queue/repeat`. |

---

## 4. API Endpoint Documentation

Base URL: `http://localhost:8080`

### 4.1 Root / Health Check
- **Endpoint:** `GET /`
- **Description:** Returns server metadata, version, health status, and full endpoint directory.
- **Response Example:**
```json
{
  "service": "Wavit Music Backend API",
  "version": "1.0.0",
  "status": "healthy",
  "endpoints": [ ... ]
}
```

---

### 4.2 Search
#### 1. Full Search
- **Endpoint:** `GET /search`
- **Query Parameters:**
  - `q` (string, required): Search query (e.g., `blinding lights`).
  - `filter` (string, optional): One of `all` (default), `songs`, `videos`, `albums`, `artists`, `playlists`.
- **Response Model:** `SearchResult`
- **Response Example:**
```json
{
  "query": "blinding lights",
  "filter": "SONGS",
  "songs": [
    {
      "videoId": "4NRXx6U8ABQ",
      "title": "Blinding Lights",
      "artist": "The Weeknd",
      "artists": [
        { "name": "The Weeknd", "browseId": "UC0WP5P-ufpRfjbNrmOWwLBQ" }
      ],
      "albumId": "MPREb_3fK8...",
      "albumName": "After Hours",
      "thumbnailUrl": "https://lh3.googleusercontent.com/...",
      "durationText": "3:20",
      "isExplicit": false,
      "isVideo": false
    }
  ],
  "albums": [],
  "artists": [],
  "playlists": [],
  "continuationToken": null
}
```

#### 2. Search Suggestions (Typeahead)
- **Endpoint:** `GET /search/suggestions`
- **Query Parameters:**
  - `q` (string, required): Partial query (e.g., `blind`).
- **Response Example:**
```json
{
  "query": "blind",
  "suggestions": [
    "blinding lights",
    "blinding lights the weeknd",
    "blinded by the light",
    "blind guardian"
  ]
}
```

---

### 4.3 Song Details & Metadata
#### 1. Core Song Metadata
- **Endpoint:** `GET /song/{id}`
- **Path Parameters:**
  - `id` (string, required): 11-character YouTube `videoId` (e.g., `4NRXx6U8ABQ`).
- **Response Model:** `Song`

#### 2. Extended Song Info (Views, Likes, Release Date)
- **Endpoint:** `GET /song/{id}/info`
- **Path Parameters:**
  - `id` (string, required): YouTube `videoId`.
- **Response Example:**
```json
{
  "videoId": "4NRXx6U8ABQ",
  "title": "Blinding Lights",
  "artist": "The Weeknd",
  "views": "1.4B",
  "likes": "12.5M",
  "releaseDate": "2019-11-29",
  "description": "Provided to YouTube by Universal Music Group..."
}
```

#### 3. Related / Radio Tracks (Autoplay Seed)
- **Endpoint:** `GET /song/{id}/related`
- **Path Parameters:**
  - `id` (string, required): YouTube `videoId`.
- **Response Example:**
```json
{
  "tracks": [
    {
      "videoId": "XXYlFuWEuKI",
      "title": "Save Your Tears",
      "artist": "The Weeknd",
      "thumbnailUrl": "https://lh3.googleusercontent.com/...",
      "durationText": "3:35"
    }
  ]
}
```

#### 4. Lyrics
- **Endpoint:** `GET /song/{id}/lyrics`
- **Path Parameters:**
  - `id` (string, required): YouTube `videoId`.
- **Response Example:**
```json
{
  "videoId": "4NRXx6U8ABQ",
  "plainLyrics": "Yeah\nI've been tryna call\nI've been on my own for long enough...",
  "syncedLyrics": [
    { "timeMs": 14200, "text": "Yeah" },
    { "timeMs": 28400, "text": "I've been tryna call" }
  ],
  "source": "YouTube Transcript"
}
```

---

### 4.4 Album & Artist Details
#### 1. Album Details
- **Endpoint:** `GET /album/{id}`
- **Path Parameters:**
  - `id` (string, required): Album `browseId` starting with `MPREb...`
- **Response Example:**
```json
{
  "browseId": "MPREb_...",
  "title": "After Hours",
  "artist": "The Weeknd",
  "year": "2020",
  "thumbnailUrl": "https://lh3.googleusercontent.com/...",
  "description": "After Hours is the fourth studio album...",
  "tracks": [ ... ],
  "trackCount": 14
}
```

#### 2. Artist Profile
- **Endpoint:** `GET /artist/{id}`
- **Path Parameters:**
  - `id` (string, required): Artist `browseId` starting with `UC...`
- **Response Example:**
```json
{
  "browseId": "UC0WP5P-ufpRfjbNrmOWwLBQ",
  "name": "The Weeknd",
  "description": "Canadian singer, songwriter, and record producer...",
  "thumbnailUrl": "https://lh3.googleusercontent.com/...",
  "topSongs": [ ... ],
  "sections": [
    {
      "title": "Albums",
      "items": [ ... ]
    }
  ]
}
```

---

### 4.5 Audio Stream Resolution
- **Endpoint:** `GET /stream/{id}`
- **Path Parameters:**
  - `id` (string, required): YouTube `videoId`.
- **Query Parameters:**
  - `maxKbps` (int, optional): Maximum bitrate cap (e.g. `128`, `256`). Defaults to highest available.
- **Response Model:** `StreamInfo`
- **Response Example:**
```json
{
  "videoId": "4NRXx6U8ABQ",
  "url": "https://rr2---sn-4g5ednls.googlevideo.com/videoplayback?expire=...",
  "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
  "bitrateKbps": 128,
  "durationSeconds": 200,
  "format": "m4a",
  "loudnessDb": -7.2,
  "expiresAtMs": 1793335500000
}
```

---

### 4.6 Home & Explore Feeds
#### 1. Home Feed
- **Endpoint:** `GET /home`
- **Response Example:**
```json
{
  "shelves": [
    {
      "title": "Quick Picks",
      "subtitle": "Start radio from a song",
      "items": [
        {
          "title": "Blinding Lights",
          "subtitle": "The Weeknd",
          "thumbnailUrl": "https://...",
          "videoId": "4NRXx6U8ABQ",
          "browseId": null
        }
      ]
    }
  ],
  "continuation": "4wING..."
}
```

#### 2. Explore (Moods & Genres)
- **Endpoint:** `GET /explore`
- **Response Example:**
```json
{
  "sections": [
    {
      "title": "Moods & Moments",
      "items": [
        { "title": "Chill", "browseId": "FEmusic_moods_and_genres_category_..." },
        { "title": "Workout", "browseId": "FEmusic_moods_and_genres_category_..." }
      ]
    }
  ]
}
```

---

### 4.7 Playback Queue Management
Stateful queue endpoints for data-level player control:
- `GET /queue`: Returns current queue snapshot.
- `POST /queue/set`: Sets the active queue. Body: `{"songs": [...], "startIndex": 0}`.
- `POST /queue/next`: Resolves and advances to the next track.
- `POST /queue/previous`: Resolves and steps back to the previous track.
- `POST /queue/add`: Adds a track to the queue. Body: `{"song": {...}, "playNext": true}`.
- `POST /queue/shuffle`: Enables/disables shuffle. Body: `{"enabled": true}`.
- `POST /queue/repeat`: Sets repeat mode. Body: `{"mode": "OFF" | "ALL" | "ONE"}`.

---

## 5. InnerTube & Googlevideo Protocol Guide

### Why InnerTube?
YouTube Music's web app (`music.youtube.com`) and mobile apps communicate via an internal JSON protocol at `/youtubei/v1`. Unlike the public YouTube Data API v3:
- It requires no developer API key or billing account.
- It returns complete music shelves, albums, track numbers, and radio mixes.

### Bot Prevention & Visitor Data
When an unauthenticated HTTP client calls InnerTube repeatedly, Google triggers a bot verification wall. Wavit prevents this via:
1. Fetching `https://www.youtube.com/sw.js_data` before making player or browse calls.
2. Parsing the Base64 protobuf string matching `Cg[A-Za-z0-9_%-]{40,}`.
3. Injecting this token as `context.client.visitorData` in every payload.

### Direct Streaming URL Extraction
The YouTube `/player` endpoint behavior depends on the client identity passed:
- `WEB_REMIX` / `WEB`: Google often applies an obfuscated signature cipher (`signatureCipher`) requiring JavaScript deobfuscation.
- `IOS` (`com.google.ios.youtube/21.26.4`) & `ANDROID_VR`: Google sends direct HTTPS URLs in `streamingData.adaptiveFormats` without signature cipher.
- Wavit's `StreamResolver` targets `IOS` first to obtain direct URLs directly playable by standard media players.

---

## 6. Testing & Operational Commands

### 6.1 Run Shared Multiplatform Tests
```bash
cd /Users/utkarsh/Documents/wavit
./gradlew :shared:jvmTest
```

### 6.2 Start the Ktor Backend Server
```bash
cd /Users/utkarsh/Documents/wavit
./gradlew :server:run
```

### 6.3 Execute Automated Verification Suite
In a separate terminal window:
```bash
cd /Users/utkarsh/Documents/wavit
./test-api.sh
```

---

## 7. Change Log & Activity History

### [2026-10-30] — Session 4: Android Emulator Setup & SDK Config
- **Added `local.properties`:** Configured `sdk.dir=/Users/utkarsh/Library/Android/sdk` so Gradle and Android Studio resolve the local Android SDK.
- **Documented Emulator Operations:** Documented dual-terminal operational procedure (running Ktor backend alongside Android emulator) and how the Android loopback IP `10.0.2.2:8080` transparently bridges requests from the emulator to the host Mac's backend.

### [2026-10-30] — Session 3: Phase 2 Step 2 — Android Jetpack Compose Frontend
- **Configured Android Target & Module:** Added `androidTarget()` to `:shared` and created `:androidApp` with AGP 8.7.3, Jetpack Compose Material 3, Navigation Compose, Coil, and Media3 ExoPlayer.
- **Created Universal KMP Repository:** Added `WavitRepository` interface and `WavitApiRepository` client in `shared/commonMain` to cleanly bridge the UI to the KMP backend.
- **Implemented Search Screen (`SearchScreen.kt` & `SearchViewModel.kt`):**
  - Auto-focusing pinned top search bar with keyboard auto-open via `FocusRequester` and `LocalSoftwareKeyboardController`.
  - IME Search action triggering asynchronous queries via coroutines.
  - Scrollable `LazyColumn` showing title, artist, duration, and album artwork with `Coil`.
  - Loading, error, and empty result state handling.
- **Implemented Player Screen (`PlayerScreen.kt` & `PlayerViewModel.kt`):**
  - Prominent square artwork with drop shadow.
  - Autoplay triggered upon screen arrival via `LaunchedEffect`.
  - Real-time seek `Slider` with progress tracking (`300ms` polling) and smooth user-drag handling.
  - Large circular Play/Pause toggle button in sync with ExoPlayer playback state.
  - Clean `AndroidViewModel` lifecycle management ensuring ExoPlayer release without memory leaks.
- **Created Navigation & Host (`Screen.kt`, `WavitNavHost.kt`, `MainActivity.kt`):** URL-safe argument encoding/decoding and edge-to-edge Compose rendering.

### [2026-10-30] — Session 2: Live InnerTube API Verification ("Attention" by Charlie Puth)
- **Live Search Verification:** Tested real YouTube Music search with query `"attention by charlie puth"`.
  - Returned **20 candidates**.
  - Top match: **"Attention" by Charlie Puth** (Album: *Attention*, Duration: `3:32`, YouTube video ID: `vxUBYHz_q1I`).
  - Confirmed availability and track resolution.
- **Audio Stream Analysis:**
  - Evaluated streaming formats via mobile and web client identities (`IOS`, `WEB_REMIX`).
  - Extracted audio format `itag 140` (`audio/mp4; codecs="mp4a.40.2"`) at **131 kbps**, loudness `-7.2 dB`.
  - Documented recent YouTube server-side changes (SABR / Server ABR streams and client headers).

### [2026-10-30] — Session 1: Repository Extraction & Phase 1 Backend Build
- **Analyzed:** Cloned and audited full BitChord codebase (`619` files). Separated ~90 UI-only files from core backend logic.
- **Created Multiplatform Project Structure:** Created root Gradle build, `:shared` module, and `:server` module.
- **Created Domain Models in `commonMain`:** `Song`, `ArtistRef`, `Album`, `ArtistPage`, `SearchResult`, `HomeShelf`, `StreamInfo`, `SongInfo`, `Lyrics`.
- **Built Track Control Engine in `commonMain`:** Implemented `PlaybackQueue` supporting index tracking, next/prev, play-next, autoplay tier, shuffle, and repeat modes.
- **Built Unit Test Suite:** Implemented `PlaybackQueueTest` verifying queue operations.
- **Built InnerTube Layer in `:server`:**
  - `InnerTubeClient`: Visitor token generator, browse, search, and player requests.
  - `InnerTubeParser`: Deep-first parser for search, carousels, albums, and transcripts.
  - `StreamResolver`: Direct googlevideo audio extractor with in-memory TTL caching.
  - `SongInfoRepository`: Jsoup scraper for views, likes, and release dates.
  - `MusicRepository`: High-level orchestrator.
- **Built REST Server & Routes:** 17 REST endpoints in Ktor on Netty with CORS, StatusPages, ContentNegotiation.
- **Created Verification Script:** Created `test-api.sh` for automated endpoint testing.
- **Created State File:** Created `PROJECT_STATE.md` as the permanent source of truth for the project.

---

## 8. Roadmap & Next Phases

- [ ] **Phase 2 — Multiplatform UI (Compose Multiplatform):**
  - Android application shell with `MediaSession` and background audio playback.
  - Desktop client (macOS/Windows/Linux) using Compose for Desktop.
  - MiniPlayer and Full Player motion layout.
- [ ] **Phase 3 — Offline Downloads & Local Media:**
  - Chunked range-based downloader writing `.m4a` with ID3/MP4 metadata tagging.
  - Local device audio scanning.
- [ ] **Phase 4 — Synced Lyrics UI:**
  - Word-by-word active line karaoke animation.

