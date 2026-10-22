# OpenMediaPlayer

An open-source Android media player and the reference implementation for a shared design system across a future family of privacy-respecting Android apps.

**Current stage:** foundation and design-system prototype. Media playback is not implemented yet.

## Product direction

- Kotlin and Jetpack Compose.
- Material 3 Expressive, using stable AndroidX APIs.
- Android dynamic color on Android 12 and newer, with a coordinated fallback palette.
- System light/dark appearance by default.
- Local-first, privacy-respecting behavior.
- Future playback engine: LibVLC, pending a feasibility, dependency, and licensing review.
- Future distribution goals: GitHub Releases, F-Droid, and Google Play.

## Design foundation

The first implementation lives inside the app so its conventions can be validated against real screens before being extracted into a reusable family library.

Design-system documentation:
- docs/design-system/principles.md
- docs/design-system/tokens.md
- docs/design-system/components.md
- docs/design-system/accessibility.md

The initial app screen is a component and theme showcase, not the final media-player home screen.

## Development setup

Use Android Studio with JDK 17 and Android SDK Platform 36 installed. Open this repository as a Gradle project. From a terminal, run:

- ./gradlew assembleDebug
- ./gradlew testDebugUnitTest
- ./gradlew lintDebug

The app currently uses a provisional application namespace, dev.krndv.openmediaplayer. Choose and confirm a permanent namespace before the first production release.

## Licensing status

The project license has not yet been selected. It will be decided after reviewing the playback engine, transitive dependencies, bundled media components, and distribution requirements. Until a license is committed, do not assume that the repository grants permission to redistribute or reuse its code.
