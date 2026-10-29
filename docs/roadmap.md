# AP Media Player development roadmap

This roadmap prioritizes a trustworthy playback foundation before expanding the feature set. A successful Gradle build is necessary but is not proof that audio or video playback works on a physical Android device.

## Milestone 1 — Playback reliability (current)

- [x] Keep all changes on a feature branch; do not commit to `main`.
- [x] Add privacy-conscious LibVLC event/action logging so device failures can be diagnosed.
- [x] Surface an actionable message when LibVLC reports a playback error.
- [ ] Build debug APK, run unit tests and lint in CI.
- [ ] Test a local audio file and a local video file from both MediaStore and Android's document picker on a physical device.
- [ ] Verify play, pause, stop, end-of-file, unsupported/corrupt media, and rapid file switching.
- [ ] Use device logs to identify and fix the underlying cause if playback still fails; do not treat logging as a playback fix.

**Acceptance gate:** at least one known-good local audio file and one known-good local video file play from start to finish on a physical Android device, with useful failure feedback for an invalid file.

## Milestone 2 — Playback controls and lifecycle

- Seeking and duration/position reporting.
- Correct audio focus, audio becoming noisy handling, and background audio playback.
- Playback service and notification controls; restore playback across screen/navigation changes.
- Picture-in-picture for video, lifecycle-safe video surface attachment, and subtitle/audio-track selection.

## Milestone 3 — Library and queue

- Reliable MediaStore refresh and permission-denied/empty-library states.
- Queue, next/previous, shuffle, repeat, and recently played.
- Metadata/artwork loading with responsive scrolling and explicit loading/error/empty states.

## Milestone 4 — Material 3 Expressive polish

- Consistent app-owned components, expressive shapes, typography, motion and dynamic/fallback color.
- Accessible touch targets, TalkBack labels, contrast, large-font support, and reduced-motion consideration.
- Keep playback controls responsive and avoid visual effects that compromise playback performance.

## Milestone 5 — Release readiness

- Automated unit/UI checks and repeatable debug/release builds.
- Device/API compatibility matrix, crash/log review, and performance/battery checks.
- License and native dependency/notice review before distributing binaries.
- Distribution packaging for GitHub Releases, F-Droid, and/or Google Play only after their respective requirements are checked.

## Working rules

1. Make changes on feature branches and use pull requests; never push roadmap work directly to `main`.
2. For each milestone, state what changed, which checks actually ran, and what remains unverified.
3. Do not claim device playback, CI success, or release readiness without evidence from that check.
