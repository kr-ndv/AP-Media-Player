# OpenMediaPlayer

An Android media player prototype built with Kotlin, Jetpack Compose and Material 3, with a privacy-respecting, local-first product direction.

**Current stage:** initial media-library UI and a LibVLC feasibility adapter. Playback has not yet been validated on a device, and the app is not ready for daily use.

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
- Initial Library, Network, Playlists and Settings navigation.
- Provisional LibVLC adapter with basic open/play/pause/stop/release operations.
- Android CI for build, lint, unit-test and instrumentation-test compilation.

The UI currently uses explanatory placeholders for media that is not connected yet. The playback adapter does not yet attach a video surface, publish playback state/events, manage audio focus, or prove local/network playback on a device.

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
