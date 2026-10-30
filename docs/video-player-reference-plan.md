# AP Media Player — Video Player Reference & Integration Plan

This document records the feature direction for the AP Media Player video player, using three public projects as technical references. It is a feature and architecture plan, not a claim that their source code has been copied.

## Reference projects

| Project | License observed in repository | Main strengths to study |
|---|---|---|
| [mpvKt](https://github.com/abdallahmehiz/mpvKt) | Apache-2.0 | Gesture handling, seek feedback, player controls, playback history, sleep timer, subtitle styling/delay, audio delay, decoder selection, PiP |
| [NextPlayer](https://github.com/anilbeesetti/nextplayer) | GPL-3.0 | Media3/ExoPlayer architecture, Compose player UI, supported-track selection, seek/zoom state, content scaling, external subtitles, PiP, background playback, network playback |
| [VLC](https://github.com/videolan/vlc) / libVLC | VLC app GPL-2.0-or-later; libVLC LGPL-2.1-or-later as described by upstream README | Broad media compatibility, playback engine behavior, audio/video/subtitle track capabilities, stream handling, chapters and advanced playback controls |

License note: upstream repository licenses and individual dependency/module notices must be checked at the exact revision before adapting source or distributing binaries. This plan favors independently implemented AP UI and behavior. Any copied/adapted code must retain required notices and comply with its source license. Linking a library and incorporating source are not interchangeable license questions; review the exact dependency and packaging model before release.

## Product direction

Build one coherent AP video player, not a merge of three applications. Keep the existing app architecture, local library, app navigation and Material 3 Expressive design. Reuse the current LibVLC engine initially because it is already integrated in AP. Evaluate Media3/NextLib and mpv/mpvKt only through a deliberate engine comparison and playback test matrix; do not add two engines to production simultaneously without a specific requirement.

## Feature scope

### Player foundation
- Dedicated immersive video surface, edge-to-edge controls and auto-hide behavior.
- Play/pause, previous/next when a playlist exists, seek bar, elapsed/remaining time and configurable skip intervals.
- Restore position per media item; remember last-played and resume prompt.
- Reliable orientation, system bars and video-surface attach/detach lifecycle.
- Graceful errors for unsupported, corrupt, unavailable or permission-revoked media.

### Gesture system (mpvKt + NextPlayer inspiration)
- Single tap toggles controls; double tap left/right seeks and center toggles play/pause.
- Horizontal swipe seeking with visual time delta and haptic feedback.
- Vertical swipe brightness on left and volume on right, configurable/swappable/disableable.
- Long-press temporary playback speed.
- Gesture lock and settings for sensitivity/seek interval.
- Ensure gestures do not steal controls, accessibility actions or system navigation gestures.

### Tracks and subtitles (NextPlayer + VLC + mpvKt inspiration)
- List audio tracks and select a supported track.
- List embedded subtitle tracks and select/off.
- Load external subtitle files through Storage Access Framework, with persisted URI permission where available.
- Subtitle delay/synchronization, size, style, text/background/outline and position preferences where engine support exists.
- Show available subtitle formats and useful unsupported-format feedback.
- Audio track delay/synchronization where the selected engine supports it.

### Picture, display and playback controls
- Picture-in-picture with lifecycle-safe entry/exit and correct actions.
- Display modes: fit, fill/crop, stretch and original aspect ratio.
- Pinch zoom and pan with reset; persist per-video setting only if UX remains predictable.
- Playback speed presets and fine adjustment.
- Screen rotation/orientation lock, screen-on while playing, brightness/volume controls.
- Chapter/seek navigation when metadata is available.
- A–B repeat, frame step, playback timer/sleep timer where engine APIs provide stable support.

### Library, queue and network (later video milestones)
- Video playlists/queue, shuffle, repeat, recently played and resume progress.
- External file/document picker support in addition to MediaStore.
- Network URI playback only after authentication, buffering, timeout and security requirements are designed.
- Thumbnail generation/caching off the main thread and responsive list/grid layouts.

## Engine decision and constraints

Current AP integration uses LibVLC 3.7.6. The video UI should depend on app-owned abstractions rather than LibVLC classes. If the feature comparison shows Media3/NextLib is materially better for Android-native track selection, Compose integration or PiP, build a small technical spike with the same local test files before deciding to migrate. Do not make the music player use the video engine.

For each engine candidate compare:
1. Local content:// and document-picker URI handling (including descriptor/permission lifetime).
2. Tested codec/container support on minSdk 26 and target devices, hardware/software decoder behavior.
3. Embedded/external subtitles, audio track selection, delay controls and chapters.
4. Seek accuracy, variable speed, frame stepping and playlist edge cases.
5. PiP, surface recreation, app background/foreground and activity recreation.
6. APK size, native ABI coverage, dependency license notices, F-Droid buildability and maintenance burden.

## Delivery phases and acceptance checks

1. **Video UI & lifecycle:** stable surface, immersive controls, seek/skip, orientation and error states. Verify with CI plus device playback before marking reliable.
2. **Track/subtitle support:** select audio/subtitle tracks, external subtitle picker, subtitle appearance and synchronization. Exercise missing/unsupported files.
3. **Gesture & display:** configurable tap/swipe/long-press, fit/fill/stretch/zoom/pan and speed.
4. **PiP & resume:** enter/exit PiP, restore playback position, orientation and UI after lifecycle transitions.
5. **Advanced playback & library:** chapters, A–B, sleep timer, history/queue and optional network sources.
6. **Release readiness:** device/API compatibility matrix, performance/battery and memory review, license/NOTICE audit, debug and release APK builds.

Physical device playback is not verified by CI. Maintain separate CI and device-test status in the roadmap.

## Working rule

Adopt useful capabilities from all three references, but implement them as one accessible, cohesive AP experience. Prefer app-owned abstractions, independent design and documented licenses over copying large upstream modules.
