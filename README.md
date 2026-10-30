# AV Music Player

**AV Music Player** is an open-source Android media player, built with Kotlin, Jetpack Compose and Material 3 Expressive. It is designed around a clean, local-first experience for browsing and playing music and videos on your device.

> **Project status:** Feature-rich development build. The current branch includes local media discovery, audio/video playback, library and player UI, playlists, queue and video playback controls. It still needs hands-on validation on real devices before it should be treated as production-ready.

## Product direction

- Kotlin and Jetpack Compose.
- Material 3 Expressive styling using stable AndroidX APIs.
- Dynamic color on Android 12 and newer, with coordinated fallback colors.
- System light/dark appearance by default.
- Local-first behavior; use Android's media and document pickers rather than requesting broad storage access by default.
- Playback prototype: official VideoLAN LibVLC 3.x artifact, behind an app-owned `PlaybackEngine` interface.
- Future distribution goals: GitHub Releases, F-Droid and Google Play, subject to licensing and packaging review.

## Current implementation

- Kotlin, Jetpack Compose and Material 3 Expressive design system with lavender light/dark palettes, optional dynamic color, system theme mode and branded splash/launcher artwork.
- Local MediaStore discovery for audio and video, including permission handling, search, rescan, recently played history and in-memory LRU artwork/thumbnail caching.
- Four primary destinations: Home, Music, Videos and Library. Music supports Songs, Albums, Artists and Playlists views; user-created playlists are stored locally and support adding/removing tracks.
- Mini player with current artwork and play/pause, plus a Now Playing view with seek, shuffle, repeat, next/previous, playback speed, queue management and external timed-lyrics loading with current-line highlighting and seek-on-tap.
- Video playback through LibVLC with immersive controls, tap/double-tap seek, brightness/volume gestures, orientation preference, playback speed, audio/subtitle track selection, external subtitle selection, subtitle delay, picture-in-picture, aspect preference, resume position and hardware-decoder preference.
- Settings for theme, dynamic color, library rescan, player appearance, screen-awake behavior and video playback preferences. Media rows expose Play, Details and Add to playlist actions.
- Android CI builds the debug app, compiles the instrumentation-test APK, runs unit tests and lint. Successful push builds upload a debug APK artifact for 14 days.

MediaStore discovery is limited to media Android has indexed and to the audio/video collections for which the user grants permission; this is not an unrestricted filesystem crawler. Background playback service, system media controls, audio focus management, drag-and-drop queue reordering, folder exclusions and lyrics-provider integration are not implemented yet. The queue can be manually reordered with its up/down controls. A successful build does not prove that local files or network streams play correctly on a real device. Format support depends on the stream, bundled native components and device.
## Documentation

- Design system: `docs/design-system/`
- Playback engine decision and acceptance criteria: `docs/architecture/adr-001-playback-engine.md`

## Development setup

Use Android Studio with JDK 17 and Android SDK Platform 36 installed. Open this repository as a Gradle project. From a terminal, run:

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest`
- `./gradlew lintDebug`

The app currently uses the provisional application namespace `dev.krndv.openmediaplayer`. Confirm a permanent namespace before the first production release.

## Licensing status

The application's own license has not yet been selected. LibVLC's engine license does not automatically determine the license of every bundled plugin or of the whole application. Before publishing binaries, inspect the exact artifact, transitive dependencies, native components, notices and applicable obligations for each distribution channel. See `docs/architecture/adr-001-playback-engine.md`.

Until a license is committed, do not assume that this repository grants permission to redistribute or reuse its code.
