# OpenMediaPlayer — Product and Engineering Plan

**Status:** Planning baseline; implementation should begin only after the design and playback investigation gates are satisfied.

## Product goal
Build a polished, privacy-respecting Android app for playing media stored on the device. The app is local-first: library queries, playback history, queue, and preferences remain on-device. Network access is limited to explicitly requested local-network / URL playback or features added later by deliberate product decision.

## Reference review
Use these repositories for design research and interaction patterns only:
- PixelPlayer — music library, artwork-led now playing, Material 3 styling: https://github.com/PixelPlayerHQ/PixelPlayer
- Booming Music — expressive music player, album art, queue-oriented music workflows, settings: https://github.com/mardous/BoomingMusic
- mpvKt — full-screen video controls and player preferences: https://github.com/abdallahmehiz/mpvKt
- Next Player — local video browsing, grid/list navigation, settings and playback: https://github.com/anilbeesetti/nextplayer
- Namida — media library organization and artwork-driven customization: https://github.com/namidaco/namida
- Echo Music — music-library and player presentation: https://github.com/EchoMusicApp/Echo-Music
- LibreTube — visual hierarchy and interaction details only; do not implement online video discovery, subscriptions, downloaders or YouTube-specific behaviors: https://github.com/libre-tube/LibreTube
- Material 3 design guidance: https://m3.material.io/

Do not copy complete screens, source code, logos, or assets. Review each repository’s licence before reusing any source or asset. References are for inspiration; OpenMediaPlayer needs its own component system, visual identity, and interaction decisions.

## Product scope and navigation
Primary bottom destinations:
1. Library — overview, recently added, search, folder picker, refresh/rescan, permission/empty states.
2. Music — local audio tracks and available metadata/artwork, with sorting and search.
3. Videos — local videos with thumbnails where available, list/grid option, sorting and search.
4. Settings — appearance, library, playback and privacy/about sections. No decorative/inert settings.

Playback is a full-screen route/surface, not a permanent navigation destination. Tapping a media item opens it in the correct player. Browsing while audio plays exposes a compact now-playing bar that reopens the music player. Video playback gets an immersive surface with orientation-aware fullscreen behavior. File picker is a fallback to MediaStore browsing; network/URL playback stays secondary and explicit.

## Design principles
- Material 3 Expressive direction, but avoid alpha-only APIs without a documented reason.
- Edge-to-edge and system-bar behavior must be checked on Android 8 through current target API.
- One coherent type scale, spacing scale, shape system, icon style, and motion vocabulary.
- Artwork-led, but artwork is never required for usability: robust placeholders and video thumbnails are required.
- Meaningful list rows; avoid oversized decorative count cards dominating the library.
- Responsive layouts for narrow phones, large phones/tablets, and landscape video.
- Light, dark, and system theme; dynamic color where supported; readable contrast and font scaling.
- Every interactive-looking control must do what it advertises, have accessible descriptions, and provide disabled/loading/error feedback.

## Playback architecture and investigation
Keep UI independent of LibVLC through an app-owned engine boundary. First audit existing LibVLC adapter:
- Use monotonically increasing playback/session identifiers or otherwise guard native callbacks so stopped/error events for an old media item cannot overwrite the current item’s state.
- Publish the event snapshot itself, not a closure that reads mutable latest state and may collapse transitions.
- Define legal state transitions and distinguish idle, opening, playing, paused, stopping/stopped, ended, and error.
- Preserve diagnostic error details without exposing raw stack traces to users.
- Confirm content URI permissions and MediaStore access; test document-picker URIs and persisted permission behavior.
- Confirm VLCVideoLayout attach/detach ordering, lifecycle behavior, re-opening media, audio-only media, and screen rotation.
- Avoid claiming root cause until logs reproduce it. Capture structured app logs for media URI scheme/type, state transitions, and native errors while avoiding sensitive paths where unnecessary.
- Design a stable service/media-session owner before background playback, notification controls, headset controls, and process recreation are considered complete.

## Implementation phases and gates
### Phase 0 — Baseline
- Record current main commit, current CI status, dependencies, permissions, package ID, build reproducibility.
- Run existing tests; note which tests are not currently executed on an emulator.
- Do not change main directly.

### Phase 1 — Reference and interaction spec
- Inspect screenshots from PixelPlayer, Booming Music, mpvKt, Next Player, Namida, Echo Music, LibreTube, plus the user's earlier screenshot archive if available.
- Create a screen inventory and flow map; annotate which reference influenced each idea.
- Decide typography, color approach, corner shapes, spacing, navigation, empty/loading/error/permission states.
- Deliver design spec and acceptance checklist before implementing the redesign.

### Phase 2 — Playback correctness spike
- Build reproducible tests with small licensed/royalty-free local sample files: audio and video, common supported formats, malformed/unsupported input, short clip, larger file, content URI and document picker.
- Fix event ordering, URI access, player and surface lifecycle, seeking and stop/reopen behavior.
- Establish truthful capability matrix for formats/containers/codecs based on actual tested combinations.
- Add unit tests for state transitions and instrumentation tests for user-visible playback controls.
- Gate: audio and video must both successfully start, pause/resume, seek where applicable, stop/end cleanly, and surface a recoverable error on unsupported input.

### Phase 3 — Design system and navigation
- Implement tokens and reusable components; responsive scaffold; primary nav Library/Music/Videos/Settings.
- Player routes outside primary navigation, mini-player lifecycle, back handling and state restoration.
- Settings persist real supported choices.
- Gate: navigation and accessibility tests pass; screenshot review on at least compact phone and large/landscape layout.

### Phase 4 — Library quality
- MediaStore scans on IO dispatcher, permission-denied/partial-grant states, empty states and retry/rescan.
- Metadata handling, artwork/video thumbnails with caching/fallbacks, search, sorting, list/grid as agreed, duplicate/unknown metadata behavior.
- Gate: correctness against a controlled media folder, performance check for large library, and no main-thread I/O.

### Phase 5 — Music player
- Artwork/title/artist/album; timeline and duration; previous/play/pause/next; queue; shuffle/repeat; background audio only with service and media session; notification/system controls; headset/audio focus behavior.
- Do not add lyrics, equalizer, cloud APIs or recommendations until foundational playback passes and product priority is revisited.
- Gate: playback remains coherent across navigation, foreground/background, audio interruptions, and process/lifecycle changes supported by scope.

### Phase 6 — Video player
- Full-screen layout and controls, seek gestures/buttons as usability testing supports, timeline, subtitle/audio track support only if feasible in selected engine, playback speed if supported, orientation, immersive mode, back/rotation restoration, resume position if planned.
- Gate: test portrait/landscape, rotation, repeated open/close, pause/resume and unsupported media.

### Phase 7 — Settings, privacy and polish
- Theme/system/dynamic color; library refresh/scan options; user-visible playback settings; privacy/about; reset options where appropriate.
- Verify permissions are minimal and clearly explained. Do not send analytics or library contents remotely.
- Verify open-source/licence obligations for LibVLC and all included assets/dependencies before release.
- Gate: persistence, accessibility, contrast, font scale, TalkBack labels and privacy review.

### Phase 8 — Verification and release candidate
- CI: assembleDebug, assembleRelease, unit tests, lint, dependency/license report, instrumentation tests on an emulator.
- Add deterministic playback sample tests; store no personal media or private paths in fixtures/logs.
- Manual device matrix: API 26 baseline; Android 13+ media permissions; current Android; one low-memory/older device if available; phones in portrait/landscape; headphones/audio focus; app background/foreground; empty and large libraries.
- Release checklist: version and changelog, screenshots, app icon, README, known limitations, license notices, signed release APK/AAB strategy, checksum, reproducible build instructions, GitHub release artifacts.
- Gate: no known blocker in core playback/navigation; CI green; emulator tests green; physical-device smoke test documented. APK build alone is not a release sign-off.

## Test plan
1. Unit: player-state transition reducer/session IDs; metadata formatting; sorting/search; preferences serialization.
2. Repository: no permissions, audio-only permission, video-only permission, both, revoked permission, empty MediaStore, disappearing file, malformed metadata and query error.
3. UI instrumentation: navigation destinations, search/filter, opening audio/video, back from player, mini-player visibility, settings persistence, permission rationale/retry, empty/error states.
4. Playback instrumentation/emulator: bundled open-license audio/video samples; start, pause/resume, stop, seek, end event, reopen next file rapidly, rotate during video, go background/foreground for audio, invalid codec/URI error behavior.
5. Manual physical device: actual codecs/devices, decoder failures, audio focus/headphones, display cutouts, battery/resource behavior, real video surface and full-screen behavior.
6. Release validation: fresh install/update install, APK signature/install, app starts offline, permissions align with user actions, no unexpected networking, and CI artifacts match commit.

## Delivery workflow
- Feature branch per milestone; small pull requests with scope and test results.
- PR review includes screen captures/video for UI work and logs/test evidence for playback work.
- CI must pass before merge; do not merge a feature solely because it compiles.
- After merge to main, verify the specific commit's CI run and APK artifact, then provide the artifact with build commit and known limitations.
- Report separately what is implemented, automated-tested, emulator-tested, and physically tested.
