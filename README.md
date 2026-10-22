# OpenMediaPlayer

An Android media player prototype built with Kotlin, Jetpack Compose and Material 3, with a privacy-respecting, local-first product direction.

**Current stage:** a working player-screen prototype with a LibVLC adapter and event-driven UI state. Native playback has not yet been validated on a device, and the app is not ready for daily use.

## Product direction

- Kotlin and Jetpack Compose.
- Material 3 Expressive styling using stable AndroidX APIs.
- Dynamic color on Android 12 and newer, with coordinated fallback colors.
- System light/dark appearance by default.
- Local-first behavior; use Android's media and document pickers rather than requesting broad storage access by default.
- Playback prototype: official VideoLAN LibVLC 3.x artifact, behind an app-owned `PlaybackEngine` interface.
- Future distribution goals: GitHub Releases, F-Droid and Google Play, subject to licensing and packaging review.

## Current implementation

- Material 3 design foundation and theme showcase components.
- Library, Player, Network, Playlists and Settings navigation.
- Player screen with a native video surface, Android document picker for local audio/video, and a direct-stream URL field.
- LibVLC event reporting for opening, playing, paused, stopped, ended and playback-error states; the UI no longer assumes an open request means playback succeeded.
- Android CI builds the debug app, compiles the instrumentation-test APK, runs unit tests and runs lint.

The media-library index, queues, background playback, audio focus, seeking and subtitle selection are not implemented yet. The player remains a prototype: a successful build does not prove that local files or network streams play on a real device. Format support depends on the stream, bundled native components and device.

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
