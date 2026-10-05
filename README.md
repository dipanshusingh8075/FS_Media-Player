# FS Media Player 🎬⚡

A standalone, modern, offline media player app for Android built with **Kotlin**, **Jetpack Compose (Material 3)**, and **AndroidX Media3 (ExoPlayer)**.

---

## 🛡️ Intellectual Property & Design Originality
FS Media Player was engineered from the ground up to ensure complete trade-dress and intellectual property independence:
- **Unique Design Language**: Adopts a bespoke **Obsidian & Electric Cyan / Cyber Emerald** aesthetic with glassmorphic cards and floating HUD pills, completely distinct from older blue/white video player players (such as MX Player).
- **Material 3 Foundation**: Built entirely on Google's official Material 3 specification and Jetpack Compose.
- **Modern Gesture System**: Tailored gesture engine that handles brightness, volume, and seek scrubbing without imitating third-party proprietary designs.

---

## 🚀 Key Features

1. **MediaStore & Scoped Storage Engine**
   - Scoped Storage compliant across Android 10, 11, 12, 13, 14, and 15+.
   - Real-time reactive updates using `ContentObserver` + Kotlin `Flow`.
   - Dual view: **Folder-wise hierarchy** with video count & storage consumption, and a **Flat chronological video list**.
   - Asynchronous thumbnail rendering via Coil with `VideoFrameDecoder`.

2. **AndroidX Media3 (ExoPlayer) Core**
   - **Hardware vs Software Decoding Switch**: Seamlessly switch between hardware acceleration (GPU/VPU) and CPU software decoders (`c2.android.*` / `omx.google.*`) on-the-fly without losing playback position.
   - **Aspect Ratio Modes**: Cycle through *Fit*, *Fill*, *Zoom / Crop*, and *Fixed 16:9*.
   - **Speed Control**: Fine-grained playback rates from `0.5x` up to `2.0x`.
   - **Audio Stream Switching**: Instant switching across multiple audio tracks in multi-language videos.
   - **Subtitle Support**: Internal subtitle tracks and custom file picker to mount external `.srt` and `.vtt` subtitles.

3. **Custom Gesture Engine with HUD**
   - **Left-Screen Vertical Drag**: Adjusts display brightness cleanly via `WindowManager.LayoutParams` (no invasive system permissions).
   - **Right-Screen Vertical Drag**: Adjusts media volume with responsive level calculations.
   - **Horizontal Drag**: Real-time seek scrubbing with floating preview pill displaying target time, delta (+/- seconds), and total length.
   - **Double-Tap**: Left side double-tap seeks -10s, right side seeks +10s.
   - **One-Touch Lock**: Locks all gesture touch events to prevent accidental inputs.

4. **Background Playback & Picture-in-Picture (PiP)**
   - AndroidX `MediaSessionService` implementation (`MediaPlaybackService`) for background audio and lockscreen playback notification.
   - Picture-in-Picture (PiP) enabled with auto-entry on home press (`onUserLeaveHint`).

---

## 🏛️ Architecture Overview

The app follows **Clean Architecture** with **MVVM** and **Dagger Hilt**:
```
com.fsmediaplayer.app/
├── core/
│   ├── designsystem/          # Colors, Typography, Glassmorphism, HUD, Gesture Overlay
│   ├── model/                 # Domain & UI Models (VideoItem, VideoFolder, PlaybackState)
│   └── player/                # FSPlayerManager, RenderersFactoryProvider, MediaPlaybackService
├── data/
│   ├── repository/            # MediaStoreVideoRepositoryImpl
│   └── di/                    # Hilt Dependency Injection Modules
├── domain/
│   ├── repository/            # VideoRepository Interface
│   └── usecase/               # GetVideosUseCase, GetVideoFoldersUseCase
└── presentation/
    ├── library/               # Folders, Video list, Permissions, LibraryViewModel
    └── player/                # PlayerScreen, Dialogs (Audio, Subtitles, Speed)
```

---

## 🛠️ Tech Stack & Dependencies

- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose (BOM 2024.09.02) + Material 3
- **Media Engine**: AndroidX Media3 1.4.1 (`media3-exoplayer`, `media3-ui`, `media3-session`, `media3-extractor`)
- **Dependency Injection**: Dagger Hilt 2.51.1
- **Async & Concurrency**: Kotlin Coroutines & Reactive Flow
- **Image/Thumbnail Loading**: Coil Compose 2.7.0 with `coil-video`
