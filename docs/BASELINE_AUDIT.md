# Baseline Audit — OpenMediaPlayer

**Audit date:** 2026-10-22  
**Purpose:** Record what the repository and supplied screenshots establish before design or playback implementation begins.  
**Scope:** Source/configuration review and screenshot review. This document does not claim a fresh local Android Studio build, emulator run, or physical-device playback test was performed in this audit.

## 1. Repository snapshot

- Repository: https://github.com/kr-ndv/OpenMediaPlayer
- Main HEAD at audit: `efbfea39a335a4798a2b24f716c2b51a66a083bb` (`docs: incorporate Material 3 design and accessibility review`).
- Latest source/UX commit before the planning documents: `2e6a8a3b91b67438061483c83e6bc576839d112a` (`Add local media discovery and expressive library UI`).
- The two commits between those revisions add planning documentation only; they do not fix or change app runtime behavior.
- Package / namespace / application ID: `dev.krndv.openmediaplayer`.
- Min SDK: 26; compile / target SDK: 36.
- Tooling recorded in the version catalog: AGP 9.4.0, Kotlin 2.4.10, Compose BOM 2025.10.00, Material 3 1.4.0, Activity Compose 1.13.0, LibVLC `libvlc-all:3.7.6`, JDK 17.
- The app is a single `:app` module with Kotlin and Jetpack Compose.

Version-sensitive dependencies should be rechecked against official AndroidX release notes before implementation. Do not upgrade versions as a side effect of redesign work.

## 2. Build and CI baseline

The CI run for the latest audit HEAD completed successfully:

- Run: https://github.com/kr-ndv/OpenMediaPlayer/actions/runs/39827642041
- Result: success.
- Steps: `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, and `lintDebug`.
- Debug artifact exists: `OpenMediaPlayer-debug-efbfea39a335a4798a2b24f716c2b51a66a083bb`; available through the run's Artifacts section for 14 days from upload.

**Important limitation:** the workflow builds the instrumentation-test APK but does not run `connectedDebugAndroidTest` or otherwise launch tests on an emulator. The repository tree contains one instrumentation test source file, `DesignSystemSmokeTest.kt`, and no `app/src/test` unit-test source files. Consequently, a green workflow proves the current code compiled, lint passed, the unit-test task completed, and the debug APK was packaged; it does not prove the Compose instrumentation tests ran, or that a local audio or video file plays.

The CI workflow does not currently build a release APK/AAB, run an emulator, exercise hardware decoding, or test playback on a physical device.

## 3. Current architecture

### App ownership
- `MainActivity` creates a `LibVlcPlaybackEngine` remembered in Compose and releases it when that Compose owner is disposed.
- `MediaLibraryScreen` owns the current top-level navigation state, MediaStore permission requests, scan/reload state, search, selected destination, and the requested media item passed to the player screen.
- `MediaRepository` queries Android MediaStore audio and video collections and maps rows to `LocalMediaItem`.
- `PlaybackEngine` is an app-owned interface; `LibVlcPlaybackEngine` owns LibVLC, MediaPlayer, native events, and video layout attach/detach. Keeping this boundary is a useful architectural choice.

### Library repository
- Audio and video are queried separately, then combined and sorted by date added.
- Queries run on an IO dispatcher from the current screen.
- Query errors are wrapped in `runCatching` and silently converted to an empty list. This can make a real query/permission/storage failure look like a genuinely empty library. Later work should report an explicit repository result/error state rather than silently lose the cause.
- The mapped media model includes URI, title, artist/album where available, MIME type, duration, size, date-added and kind. Current rows still need user-facing artwork/thumbnail loading and robust fallback behavior.

### Playback adapter — risks to investigate, not presumed root causes
- State updates are posted to the main handler, and each queued callback reads the mutable latest `currentState`, rather than delivering the snapshot that triggered that callback. Several native transitions can therefore be collapsed in the UI.
- Native events are not correlated with a playback-session/media identifier. Events from stopping/replacing a previous item may race with the new open sequence.
- `open()` stops current playback before installing the new media; the stop event may arrive asynchronously. The exact event sequence must be reproduced and logged before deciding on a fix.
- `EncounteredError` is currently published without a diagnostic message, limiting useful error reporting.
- The video layout attaches and detaches in the playback screen's composition lifecycle. Surface behavior during navigation, rapid replacement, rotation, and activity recreation must be tested.
- The current player engine is not owned by a dedicated playback service/media-session layer. Background audio, notification controls, headset actions, audio focus, and process/lifecycle behavior are not complete and must not be implied by the UI.

These points are source-code risks; none alone proves why playback failed on the user's device. Reproduction evidence is still required.

## 4. Permissions and product-scope mismatch

The current manifest declares:
- `INTERNET`
- `READ_MEDIA_AUDIO`
- `READ_MEDIA_VIDEO`
- `READ_EXTERNAL_STORAGE` capped at API 32.

It does not declare `READ_MEDIA_VISUAL_USER_SELECTED`. Official Android guidance says Android 14+ users can grant selected-photo/video access, and apps that maintain their own media library should account for this permission and reselection path. Because this app targets SDK 36 and queries MediaStore video rows, the video permission contract must distinguish full video access, selected-video access, denied access and access that was later changed in Settings. Recheck permissions on resume; do not interpret selected-only access as an empty library. This selected-visual-media permission is not an audio permission. Source: https://developer.android.com/about/versions/14/changes/partial-photo-video-access

The current player UI includes a direct stream URL field and `open stream` action, and resources describe network shares and UPnP/DLNA as planned. The stated product intent for the first release is now **local media only**. The next implementation should remove the network-stream experience and unused network placeholders, confirm no required local-media path needs Internet permission, and remove `INTERNET` if confirmed unnecessary. Do not silently replace this with network shares or an online feed.

Media access should be requested contextually; the UI must explain denied or partially granted access and offer the system file picker as a user-controlled fallback.

## 5. Current screen observations

The supplied OpenMediaPlayer captures show the current on-device app. They are useful product evidence, but their exact APK commit has not been tied to the screenshot files.

### Library / Music / Videos
- The home page starts with a very large headline and explanation, then a prominent purple library-count panel, separate Audio/Video metric cards, a large search field, and repeated media rows.
- Music and Videos repeat almost the same large count/intro structure rather than prioritizing content-specific metadata.
- Rows show generic audio/video icons, names, type, duration and size; they do not yet show real album art or video thumbnails.
- Repeated library totals and type counts consume a large amount of vertical space before the user reaches the media. Prefer a compact content-oriented header, real artwork/thumbnails, useful sorting and a less card-heavy list/grid.
- The screenshot header text (“Your media, all together.”) does not match the current resource text (`Your media, your way.`) or the instrumentation assertion. This indicates the supplied screenshot may not have been captured from the current HEAD. Capture a fresh baseline from a known commit before comparing before/after UI.

### Player
- The Player remains a permanent bottom-navigation destination.
- The black 16:9 video surface remains visible even without selected video media.
- The screen mixes play/pause and stop, a separate file-picker panel, a direct network-stream URL panel, a status panel, and a development warning in one long scrolling page.
- The shown “Playback stopped” state and blank surface do not establish that playback is functional. There is no visible timeline/seek control, track queue, audio-specific now-playing view, or dedicated immersive video route.
- For the local-media MVP, choosing an audio item should open an audio-focused player and choosing a video should open a video-focused player. The main navigation should not include Player as a permanent destination; use a compact now-playing bar for returning to active audio.

## 6. Existing design-system guidance to preserve

The repository already has:
- `docs/design-system/principles.md`: family style is expressive and modern, dynamic color on Android 12+, family fallback palette on earlier versions, app-specific layouts, semantic tokens, Compose-native foundations, privacy and accessibility.
- `docs/design-system/tokens.md`: neutral/violet fallback scheme, semantic Material roles, spacing defaults (4/8/12/16/24/32 dp), 20 dp page inset, 24 dp section separation, and family shapes (8/12/20/28 dp).
- `docs/design-system/components.md`: shared-component ownership and API guidance.
- `docs/design-system/accessibility.md`: font scaling, content/state descriptions, touch targets, contrast, TalkBack, focus, localization, reduced motion.
- `docs/architecture/adr-001-playback-engine.md`: a provisional LibVLC decision and a licensing/release gate.

The redesign should reuse these tokens and conventions, while adapting the hierarchy and components specifically for a local media player. Do not wholesale replace the theme or add decorative surfaces in the name of Expressive.

## 7. Current acceptance gaps

| Area | Baseline status | Required evidence to close |
|---|---|---|
| Debug build | CI green for audit HEAD | Keep CI green after each milestone |
| Instrumentation tests | Source compiles; not run by current workflow | Run on an emulator with deterministic test fixtures |
| Local audio playback | Not verified in this audit; user reported failure | Physical device and/or controlled emulator test with state/event logs |
| Local video rendering | Not verified in this audit; user reported failure | Confirm visible decoded frames, controls, seeking, lifecycle and rotation |
| Playback state correctness | Native-event race risk identified | Reproducible regression test for rapid stop/reopen and error transitions |
| Library scan errors | Query failures silently become an empty list | Explicit empty vs failure state and tests |
| Navigation | Current Player is a top-level destination; Settings is not in shown nav | Library / Music / Videos / Settings plus dedicated player route |
| Artwork / thumbnails | Not shown in supplied app captures | Actual cover/thumbnail retrieval, cache and fallback tests |
| Background playback | Not implemented according to UI notice | Service/media session, audio focus and notification/system controls tested |
| Local-only privacy | URL/network UI and Internet permission currently exist | Remove MVP network features; verify offline playback and no network dependency |
| Visual verification | User screenshots exist; APK commit provenance unclear | Commit-linked screenshots and size/theme state review |
| Release readiness | Debug APK artifact only | Release build, licensing review, signing/metadata and device validation |

## 8. Exit criteria for the planning phase

Before product implementation starts:
1. Screen-by-screen design spec and navigation/return-flow map are committed.
2. Reference screenshots are annotated with the patterns to adapt and features deliberately excluded.
3. A local-only scope statement is consistent across product plan, playback ADR and screen specification.
4. The playback reproduction matrix identifies test media, expected events, supported device/API matrix, and diagnostic capture approach.
5. Each planned UI screen has primary task, controls, states, adaptive behavior, accessibility requirements, and a test gate.
6. A baseline build/screenshots from a known commit are available for later comparison.

No runtime playback fix is claimed by this audit.


## 9. Official platform/dependency checks at audit time

Checked on 2026-10-22 against official Android Developers pages:

- Compose Material 3 lists **1.4.0 as the stable release** and **1.5.0-beta02 as the beta line** as of the latest update observed on 2026-10-21. The project currently pins Material 3 1.4.0, so the visual redesign should stay on this stable version unless a specific required API is absent and the trade-off is reviewed. Source: https://developer.android.com/jetpack/androidx/releases/compose-material3
- The same official release notes say the Material Icons library is no longer recommended for new use and is no longer updated; they recommend Material Symbols Vector Drawable XMLs instead. The current project depends on `material-icons-extended`. Audit actual icon call sites and APK/build impact, then migrate only through a deliberate PR with an icon inventory—do not mix a whole icon migration into an unrelated screen change. Source: https://developer.android.com/jetpack/androidx/releases/compose-material3 and https://developer.android.com/develop/ui/compose/graphics/images/material
- Android 16 (API 36) targeting apps cannot opt out of edge-to-edge, and large-screen orientation/aspect-ratio restrictions are ignored by default at smallest width >=600dp. The app already calls `enableEdgeToEdge()`; UI work must validate system-bar insets, resizing and landscape/wide-window layouts instead of forcing orientation. Sources: https://developer.android.com/about/versions/16/behavior-changes-16 and https://developer.android.com/design/ui/mobile/guides/layout-and-content/edge-to-edge

These checks inform planning only. No dependency versions or runtime code were changed by the audit.


## 10. Additional permission acceptance requirement

For the local-media MVP, test the following on Android 14+ and a current Android release:
- Full `READ_MEDIA_VIDEO` permission granted.
- User grants only selected-video access via `READ_MEDIA_VISUAL_USER_SELECTED`.
- User changes or revokes selected-video access in system Settings while the app is backgrounded.
- App returns to foreground, re-queries MediaStore, and reflects the actual selected set without treating it as a truly empty library.
- A user-initiated action clearly offers video access/reselection; the app does not unexpectedly re-prompt on startup.

Official source: https://developer.android.com/about/versions/14/changes/partial-photo-video-access
