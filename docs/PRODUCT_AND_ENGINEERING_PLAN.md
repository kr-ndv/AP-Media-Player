# OpenMediaPlayer — Product and Engineering Plan

**Status:** Planning baseline; implementation should begin only after the design and playback investigation gates are satisfied.

## Product goal
Build a polished, privacy-respecting Android app for playing media stored on the device. The first-release product scope is strictly local media: library queries, playback, queue, playback history, and preferences remain on-device. Direct HTTP/RTSP streams, network shares, remote discovery, online catalogs, streaming feeds, tracking, sign-in, and other online services are out of scope. Do not request Internet access for the local-media MVP; any later network feature requires a separate explicit product decision and permission/security review.


## Planning artifacts

- [Baseline audit](BASELINE_AUDIT.md) — current stack, CI and test gaps, library/player risks, and observed screenshot mismatch.
- [Screen-by-screen design specification](DESIGN_SPEC.md) — navigation, individual screen contracts, states, adaptive behavior, reference findings, accessibility and acceptance gates.
- [Playback engine ADR](architecture/adr-001-playback-engine.md) — engine choice, local-only scope, and licensing decision gate.
- [Licensing and third-party distribution audit](LICENSING_AUDIT.md) — app license decision, LibVLC AAR/codec inventory, notices and release blockers.
- [Existing design-system principles](design-system/principles.md), [tokens](design-system/tokens.md), [component conventions](design-system/components.md), and [accessibility baseline](design-system/accessibility.md).


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

Playback is a full-screen route/surface, not a permanent navigation destination. Tapping a media item opens it in the correct player. Browsing while audio plays exposes a compact now-playing bar that reopens the music player. Video playback gets an immersive surface with orientation-aware fullscreen behavior. System file picking is the fallback for media not surfaced in MediaStore. Network/URL playback is out of scope for the local-media MVP.

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
- Build reproducible tests with small licensed/royalty-free local sample files: audio and video, common supported formats, malformed/unsupported input, short clip, larger file, MediaStore content URIs, and system document-picker URIs.
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
- MediaStore scans on IO dispatcher, permission-denied/partial-grant states, empty states and retry/rescan. On Android 14+ (API 34+), explicitly handle selected-video-only access via `READ_MEDIA_VISUAL_USER_SELECTED`, permission changes made in Settings, and reselection; distinguish partial access from an empty collection and re-check permissions on resume.
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
- Verify permissions are minimal and clearly explained. The local-media MVP should not make network requests or require INTERNET permission. Do not send analytics or library contents remotely.
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
2. Repository: no permissions, audio-only permission, full video permission, selected-video-only permission on API 34+, both, revoked/changed permission, empty MediaStore, disappearing file, malformed metadata and query error.
3. UI instrumentation: navigation destinations, search/filter, opening audio/video, back from player, mini-player visibility, settings persistence, permission rationale/retry, empty/error states.
4. Playback instrumentation/emulator: bundled open-license local audio/video samples; start, pause/resume, stop, seek, end event, reopen next file rapidly, rotate during video, go background/foreground for audio, invalid codec/URI error behavior.
5. Manual physical device: actual codecs/devices, decoder failures, audio focus/headphones, display cutouts, battery/resource behavior, real video surface and full-screen behavior.
6. Release validation: fresh install/update install, APK signature/install, app starts and plays local files offline, permissions align with user actions, no networking or Internet permission in the MVP, and CI artifacts match the commit.

## Delivery workflow
- Feature branch per milestone; small pull requests with scope and test results.
- PR review includes screen captures/video for UI work and logs/test evidence for playback work.
- CI must pass before merge; do not merge a feature solely because it compiles.
- After merge to main, verify the specific commit's CI run and APK artifact, then provide the artifact with build commit and known limitations.
- Report separately what is implemented, automated-tested, emulator-tested, and physically tested.


## UI engineering standards from the supplied Material Design 3 references

The following uploaded documents were reviewed as project guidance before implementation:
- `README.md` — skill overview and reference map.
- `SKILL.md` — required Material workflow, design rules, verification and handoff contract.
- `design-foundations.md` — hierarchy, semantic roles, typography, containment, component selection, loading and form states.
- `android(1).md` — Compose-specific APIs, dependency gates, edge-to-edge, navigation, performance and Android verification.
- `expressive.md` — selective expressive emphasis, stability rules, motion and migration strategy.
- `adaptive-accessibility(1).md` — window classes, adaptive layouts, accessibility and scenario matrix.
- `sources.md` — official source policy and version-status index.
- `web(1).md` — reviewed but not a direct implementation guide for this native Android Compose app; do not transfer web-specific component/API guidance to Android.

### Design decision order
Resolve each screen in this order: user goal and content priority; platform conventions and input model; information architecture/navigation; component semantics; theme roles and adaptive layout; brand expression; decorative detail only when it adds meaning. A polished visual theme does not compensate for unclear navigation or missing states.

### Containment and visual restraint
- Begin with alignment, spacing and typography; add a visible container only when it communicates ownership, selection, interaction, or independent scrolling.
- Do not wrap every list row/section in a card, nest cards, or repeat a boundary using a surface, outline, divider and shadow together.
- Prefer Material surface roles and tonal elevation for ordinary hierarchy; reserve shadows for actual overlap/floating depth.
- No default glass, blur, glow, alpha-heavy translucent surfaces or decorative gradients. If video controls need a media overlay, keep it localized, maintain contrast against varied footage, and provide a solid fallback.
- Use the documented 1:3 nested-containment budget and two-level authored-containment limit as review heuristics from the supplied skill, not as official Material specification rules. Document any justified exception.

### Expressive intensity and dependency gate
- Define an intensity budget per screen: **quiet** for backgrounds/body/routine rows, **clear** for navigation and selected states, **expressive** for a small number of important controls, transitions or states.
- Use Material 3 Expressive only where it improves recognition, feedback or emotional tone; do not morph or animate every repeated control.
- Before using each API, confirm it exists in the installed Material3 version and whether it needs an experimental opt-in. Keep opt-ins narrow. Do not move this project from a stable release to an alpha dependency just to style a screen.
- Refresh the official release notes/API pages before any dependency decision. The uploaded source index is useful research but explicitly version-sensitive. Audit the existing `material-icons-extended` dependency against current official guidance; do not remove or replace it as an incidental change without checking call sites and an alternative icon strategy.

### Adaptive behavior contract
Design for the current app window, not a device label. The supplied reference describes width classes as compact (<600dp), medium (600–839dp), expanded (840–1199dp), large (1200–1599dp), and extra-large (1600dp+); validate the applicable API/version and use window-size/adaptive APIs instead of manually reading display metrics.
- Keep the same destinations and navigation state while adapting the navigation component (bottom navigation on compact windows; rail or another supported pattern when it materially improves wider windows).
- For wider windows, reflow or reveal useful content such as list-detail panes rather than stretching a phone layout or simply increasing padding.
- Handle compact-height landscape, resizable/multi-window layouts and video rotation without locking orientation as a layout shortcut.
- Preserve selection, entered text, scroll position where meaningful, playback state and sensible pane-local back behavior during resize or configuration changes.
- Avoid nested same-direction lazy scroll containers without an explicit size and interaction model; use stable keys and lazy collections for large libraries.

### Accessibility, system UI and interaction requirements
- Android touch targets must be at least 48dp by 48dp. Icon-only controls need meaningful content descriptions; decorative images/icons should not be announced. Expose selection, toggle, playback progress, loading, errors and actions semantically.
- Do not rely on color or motion alone to convey status. Verify real rendered contrast in light, dark, dynamic and fallback themes; preserve visible keyboard focus and a logical focus order.
- Keep scalable typography, test long labels/localized text and enlarged font settings, and ensure content reflows rather than clips.
- Consider touch as primary but verify keyboard/pointer behavior where available; respect Android/system reduced-motion or animation preferences for nonessential motion.
- Check edge-to-edge system bars, display cutouts, navigation gesture zones, IME overlap, and avoid applying the same inset twice. Keep focused URL/search fields and their actions visible when the keyboard opens.
- Every reachable workflow must have intentional loading, empty, partial, success, selected, disabled and recoverable-error states. Keep already-useful content visible during refresh; use skeletons only for meaningful waits and preserve its layout. Errors must offer a recovery path.

### Required visual verification
Compilation alone is not evidence of a successful visual implementation. For each UI milestone:
1. Write a concise UI contract specifying primary task/action, hierarchy, navigation, components, state variants, and adaptive transformations.
2. Build and lint the affected code.
3. Capture and inspect the actual screen or screenshot/preview at representative supported windows, including compact portrait and compact-height landscape; include wider classes affected by the change.
4. Review light/dark and dynamic/fallback color, realistic and long content, large font scale, empty/loading/error states, IME and system insets, touch targets, semantics and focus.
5. Report separately which previews/screenshots were inspected, which emulator/device scenarios were run, and what remains unverified. Never infer visual quality or accessibility from component names, dependency presence or compilation.

## Adjusted acceptance gates

### Design gate (before screen implementation)
- Screen inventory and navigation/return-flow diagram are reviewed.
- Each primary screen has a UI contract and states matrix.
- Reference screenshot annotations explain what is borrowed as a pattern and what is deliberately different.
- Typography, semantic color roles, shape/spacing rhythm, icon strategy, component emphasis and expressive intensity budget are documented.
- Compact and expanded-window behaviors plus video compact-height landscape are specified.
- No decorative containment, inert controls, or decorative hero copy is left in the design.

### UI gate (before feature merge)
- Layout is inspected at the required window classes and realistic content states, not only compiled.
- Every visible control performs its named action and exposes accessible state/name.
- No clipping, overlap, layout jump, duplicate inset, unintended nested scroll or focus loss is observed in tested scenarios.
- Current stable/experimental API choices, screen-size observations, accessibility checks and remaining risks are stated in the PR.

### Release handoff additions
Every delivery should state: Material library/version and experimental API usage; navigation and adaptive behavior implemented; accessibility and system UI checks; actual checks and observed screen sizes; test/device coverage; and unresolved risks. Separate code/build success from screenshot/preview inspection, emulator testing, and physical-device validation.
