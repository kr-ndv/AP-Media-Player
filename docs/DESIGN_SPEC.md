# Screen-by-Screen Design Specification — OpenMediaPlayer

**Version:** 0.1 (design baseline)  
**Status:** Spec for review before implementation; not a screenshot/mockup and not proof of runtime behavior.  
**Product scope:** Local audio and video files only. Network streams, online feeds, remote shares, downloads, account features and YouTube-specific capabilities are excluded from the MVP.

## 1. Product experience in one sentence

OpenMediaPlayer should feel like a focused, local-first library app with artwork-rich music playback and an immersive, dependable video player—not a dashboard of cards with a generic embedded player.

## 2. Reference evidence and decisions

The supplied `Ref.zip` contains 46 screenshots from VLC, Echo Music and Next Player. The four supplied OpenMediaPlayer captures were also inspected. Repository metadata and README screenshot inventories were reviewed for PixelPlayer, Booming Music, mpvKt, Next Player, Namida, Echo Music and LibreTube.

| Reference capture | Observed pattern | Adapt for OpenMediaPlayer | Deliberately do not copy |
|---|---|---|---|
| PixelPlayer home — `assets/screenshot1.jpg` | An artwork-led “Your Mix” hero, floating now-playing bar and three-destination navigation | Compact now-playing bar, clear art and playback entry point | A generated-mix/large hero layout for a product with no recommendations or playlists yet |
| PixelPlayer now playing — `assets/screenshot2.jpg` | Dominant album artwork, strong track identity, visible timeline and clear transport controls; screen color is coordinated with artwork | Prominent artwork and hierarchy for title / artist / progress / play-pause | Automatically tinting every surface without contrast testing, or copying oversized control treatments |
| PixelPlayer library — `assets/screenshot3.jpg` | Songs/albums/artists peer views, real cover thumbnails, secondary per-track menu | Content-first music list, artwork, simple overflow for secondary actions | Exposing album/artist/genre tabs before library metadata and those browse modes are implemented |
| Booming Music expressive player — `assets/screenshots/Player_Expressive.jpg` | Large art, metadata, timeline and transport hierarchy with restrained secondary playback controls | Strong music-player information hierarchy and compact secondary controls | Its distinctive starburst-style play button or unsupported features such as lyrics/equalizer in the MVP |
| Namida dark collection screenshot — `namidaco/namida-assets: screenshots/readme/collection_dark_1.webp` | Collection sheet combines Home, rich current player and an equalizer screen; waveform and metadata are visually distinct | Clear separation between library/player/settings tasks; optional progress visualization only if real data exists | Replicating its feature breadth, YouTube section, or equalizer surface in this local-media MVP |
| mpvKt public screenshot preview — [repository README](https://github.com/abdallahmehiz/mpvKt) | File list, preferences and a separate video surface in the public screenshot preview; additional gestures/configuration are secondary | Local file selection and dedicated video playback surface; keep controls discoverable | URL entry and network playback, or deep decoder/subtitle settings before core playback is reliable |
| VLC video library — `Screenshot_2026-10-22-22-11-29-523_org.videolan.vlc.jpg` | Real video thumbnails, compact app bar, distinct Video/Playlist sections and useful bottom destinations | Real thumbnails, clear media-specific browse screens, visible search and simple item metadata | VLC's orange identity, every destination in one global nav, or settings-heavy chrome |
| VLC settings — `Screenshot_2026-10-22-22-11-56-406_org.videolan.vlc.png` | Grouped settings as readable rows with supporting text and only useful separators | Quiet preference rows, concise explanations, clear sections | Large advanced feature catalog that we have not implemented |
| Echo Music library — `Screenshot_2026-10-22-22-13-58-445_echo.music.iad1tya.jpg` | Strong artwork, meaningful content grouping, and a compact now-playing bar above navigation | Artwork-aware content, visible current playback, compact mini-player, content-first screen composition | Online recommendation feeds, microphone/discovery actions, download affordances and service-oriented home sections |
| Echo Music now playing — `Screenshot_2026-10-22-22-16-01-261_echo.music.iad1tya.jpg` | Large cover art, clear title/artist, progress/duration, primary transport controls and secondary playback actions | One obvious play/pause action, a usable timeline, track details, secondary queue/shuffle/repeat controls | A decorative oversized starburst or controls that suggest unavailable services |
| Next Player folder screen — `Screenshot_2026-10-22-22-18-10-014_dev.anilbeesetti.nextplayer.jpg` | Useful folder labels, locations/counts, view controls and a dedicated player affordance | Folder browsing, meaningful file/folder context, intentional list/grid choices | A permanent player navigation button if it competes with Settings |
| Next Player quick settings — `Screenshot_2026-10-22-22-18-13-928_dev.anilbeesetti.nextplayer.jpg` | A contextual sheet groups view mode, list/grid, sorting and metadata visibility | Put infrequent library layout/sort choices into a focused menu or sheet; keep common sort/search visible | Shipping a large collection of settings before the basic library workflow is sound |
| Next Player video track panes — `Screenshot_2026-10-22-22-20-19-700_dev.anilbeesetti.nextplayer.jpg` and `Screenshot_2026-10-22-22-20-23-391_dev.anilbeesetti.nextplayer.jpg` | A landscape video is paired with a contextual audio/subtitle pane | Use available wide space for useful controls/details; show track selection only if LibVLC APIs are implemented and tested | Showing track selectors as decoration or imitating the exact split proportions on a compact phone |
| User's current OpenMediaPlayer captures | Oversized purple library-count panel, separate media-count cards, repeated page headlines, icon-only media placeholders, a persistent Player destination, an always-visible black video rectangle, URL stream form and prototype notice | Preserve local-media messaging and the violet family fallback, but substantially improve hierarchy, content density, actual artwork/thumbnails, and player navigation | Repeating counts, giant marketing-style intro copy on every tab, empty video canvas when audio is selected, inert/unfinished controls and network UI in a local-only app |

**Reference boundary:** use interaction patterns and hierarchy only. Do not copy code, complete screens, logos, icons as branded assets, or screenshot artwork. Review licenses separately before reusing any source or asset. The supplied screenshot archive visually covers VLC, Echo Music and Next Player. We also inspected selected PixelPlayer screenshots, the Booming Music expressive player, and Namida’s linked dark collection screenshot. A public mpvKt screenshot preview was reviewed alongside its repository’s screenshot inventory. LibreTube’s screenshot references show useful compact navigation and media-action grouping, but its online feed, subscriptions, download, share and account/network behaviors are explicitly excluded. No repository’s full UI or feature set is being cloned.

## 3. Navigation architecture

### Primary destinations
1. **Library** — cross-media overview, recently added, search entry point, folder/system-picker access and refresh.
2. **Music** — local audio collection and track-focused browsing.
3. **Videos** — local video collection and thumbnail-focused browsing.
4. **Settings** — only persistent, implemented preferences plus privacy, version and licence information.

The expanded player is a **detail route**, not a primary bottom-navigation item. Remove the permanent Player tab. Music and video selection opens the corresponding playback UI. The system back action returns to the originating collection and restores useful scroll/selection state.

### Navigation by window
- Compact phone: Material navigation bar with the four primary destinations.
- Wider window: adapt to a supported Material navigation rail/suite where it helps; do not stretch a phone navigation bar across a tablet. Keep destination order and state consistent.
- Immersive video: hide top-level app navigation while video playback is foregrounded; provide an explicit, reachable back/close control.
- Avoid adding a new adaptive dependency until the supported API and version are checked. Do not read physical display metrics to infer layout class.

### Audio continuity
When audio is active and the full player is closed, show a compact **now-playing bar** above primary navigation. It shows cover/fallback art, track title, optional artist, play/pause, and an expand action. It appears only when there is a valid current audio item/session; it is never a permanent decorative card. Video does not appear in the audio mini-player.

## 4. Screen contracts

## 4.1 Library

**Primary task:** Find something on-device quickly or open a collection.

**Hierarchy**
1. Compact top app bar: `Library`; search and overflow/actions for refresh and file picker.
2. Lightweight media-type shortcuts (Music / Videos) with counts as secondary information, not oversized hero cards.
3. Recently added items, ordered by newest first, mixing audio and video only if the item type is clearly labeled.
4. Optional folder/system-picker entry in a simple row/section.
5. Now-playing bar only while audio is active.

**Media rows**
- Video: landscape thumbnail, title, duration and one useful secondary value (e.g. date or containing folder) where available.
- Audio: square cover art, title, artist/album when available, and duration.
- Missing metadata uses a clear filename/title and a neutral media-kind fallback. Never make a file unreadable because cover art is missing.
- The row itself opens the media. Avoid a duplicate row tap and play button that compete for the same action; place truly secondary actions in a menu.

**States**
- Loading: keep the shell and any previously trusted items visible; show a small inline scan indicator.
- Empty with permission: explain no indexed media was found; offer rescan and “Browse files”.
- Permission not granted: explain why media access is needed and provide a grant action; do not show an unexplained blank page.
- Partial access: display the collection that is accessible and explain the other collection needs permission when appropriate. On Android 14+ specifically, distinguish full video access from selected-video access via `READ_MEDIA_VISUAL_USER_SELECTED`; let the user manage/reselect video access from an explicit action. Do not treat selected-only access as an empty library, and recheck actual permissions on resume.
- Query/scan error: distinguish it from a genuinely empty library, give a retry action, and avoid silently treating repository exceptions as no media.
- Search active: clear query, retain relevant context, and display a specific no-results state.

**Avoid:** a marketing-style headline, multiple large metric cards, redundant total counts, an oversized purple container around a simple scan summary, and multiple nested card surfaces.

## 4.2 Music

**Primary task:** Find and play a local audio track.

**Hierarchy**
1. App bar title `Music`; search and a secondary view/sort action.
2. A compact content header/count if useful.
3. Track list with artwork thumbnails and meaningful metadata.
4. A mini-player only if audio is already active.

**MVP content:** Tracks from permitted local media indexed by MediaStore, with title, artist/album when available, duration, and stable URI. Search should match displayed metadata and filename/title; sort should have an intentional default (recently added initially) and a small useful set such as title and duration. Album/artist/grouped browsing can be added only after metadata quality and queue semantics are established; it should not block correct song playback.

**Interaction**
- Tap a row to play that track and establish a queue from the current collection/order.
- Now Playing opens as a full-screen audio route.
- Search and sort state should survive temporary navigation and ordinary configuration change.
- If permission is denied, explain how to grant it or use the system file picker. Do not fabricate tracks or placeholder cards.

**States:** scanning/loading, populated list, empty collection, no search results, permission denied/partial, scan error, and disappearing/unreadable file.

## 4.3 Videos

**Primary task:** Identify a local video visually and start playback.

**Hierarchy**
1. App bar title `Videos`; search, view mode and sort controls.
2. List or grid of real decoded/generated video thumbnails.
3. Title and duration; file size/date/folder are secondary, not all required on every row.
4. Search and sort state retained per destination.

**Grid/list:** list is the safe default for names and metadata; a grid option is valuable when thumbnails work. View choice belongs in a compact menu/sheet and should persist. Grid cards should not be wrapped in a second decorative card.

**Interaction**
- Tap thumbnail or row to open the dedicated video player.
- Preserve the selected item and list scroll state when returning.
- If a thumbnail fails, show a media-kind placeholder and keep the row playable.
- Unsupported/corrupt items should produce a useful playback error and allow Back to the list.

**States:** loading, empty, no results, permission denied, selected-video-only access/reselection (Android 14+), thumbnail failure, query error, and playback error.

## 4.4 Audio player — full-screen route

**Primary task:** Control listening without visual noise.

**Portrait hierarchy**
1. Top row with Back/Collapse and a clearly accessible queue/More action when implemented.
2. Large square artwork within the available height; use actual embedded/MediaStore album art where feasible, otherwise a restrained themed fallback.
3. Track title and artist/album metadata, wrapping under large font scaling.
4. Seek timeline with elapsed and total time; dragging seeks only when the engine supports it.
5. One dominant play/pause control, then previous/next.
6. Lower-emphasis shuffle/repeat and queue entry, with visible selected states only when those modes are implemented.

**Behavior**
- Audio media must not show a black video surface.
- Opening a row starts the selected item; the state and selected title must not be overwritten by stale events for previous media.
- The queue is the source of truth for previous/next and for mini-player identity.
- Play/pause, seeking, previous/next, queue, shuffle and repeat are enabled only when their behavior exists; no prototype buttons.
- Back from the player collapses to the previous browse screen while audio remains active. Activity recreation and background/foreground behavior must follow the service/session design, not the Compose screen lifetime.

**Required states:** no selected item, opening, playing, paused, seeking, ended, recoverable error, and audio output without artwork. State changes must be available to TalkBack.

**Background playback gate:** before the app claims continuous background playback, implement and test a proper playback owner/service and media session/system controls, audio focus, interruptions and notification behavior. Do not infer this from the current in-activity LibVLC engine.

## 4.5 Video player — immersive route

**Primary task:** Watch the selected file with reliable, discoverable controls.

**Phone portrait**
- Video canvas at the correct aspect ratio without stretching/cropping the picture by default.
- Controls are overlaid on the canvas or placed immediately below if more comfortable in portrait; timeline, time labels, play/pause, and skip actions must remain reachable.
- Title/track and secondary actions should be compact, not displace the actual picture.
- A fullscreen action enters immersive viewing; do not force orientation as a workaround.

**Landscape / wide window**
- Video canvas becomes dominant and may use the full window.
- Controls remain readable over variable footage with localized contrast treatment and a stable/opaque fallback.
- On a genuinely wide window, secondary content (e.g. queue or available track selection) may occupy a supporting pane if it improves the viewing task.
- Use supported LibVLC track/subtitle/decoder controls only after those behaviors have been implemented and tested.

**Controls**
- Back/close; play/pause; timeline with current and total time; seek forward/back if supported; fullscreen; overflow for implemented audio/subtitle/speed options.
- Controls can hide during playback only if tapping the video, keyboard, and accessibility actions can bring them back. Respect system reduced-motion preferences.
- Rotation/resizing keeps current media and playback position. Exiting immersive mode returns to the previous app state rather than stopping playback.
- A still/loading/failed video must never remain an unexplained black rectangle. Show an appropriate loading/error/empty-state overlay with recovery instructions.

## 4.6 Settings

**Primary task:** Change actual app behavior or understand local privacy.

**Sections and MVP rows**
- **Appearance:** System / Light / Dark; dynamic color where supported, using existing violet fallback scheme if disabled/unavailable.
- **Library:** rescan action, permission/access status and an entry to grant or browse selected media. Only offer folder inclusion/exclusion preferences if they are backed by real persistence and scanning behavior.
- **Playback:** only implemented options. Start with no speculative controls; add repeat/seek/rotation/decoder preferences only if the engine supports and persists them.
- **Privacy & About:** local-only statement, app version, open-source notices/licences, project/source link if available. The local media library is not uploaded.
- Use Settings-style rows with short supporting copy where it clarifies effect. Persist user choices and test reopening the app.

**Avoid:** a settings row that merely opens a prototype notice, toggles that do not affect behavior, or a long list of hypothetical planned features.

## 4.7 System file picker / selected file

**Primary task:** Open a user-chosen local file that is missing from MediaStore.

- Use Android's system picker and request only supported local audio/video types.
- Persist URI read permission only where the picker grants it and it is useful; gracefully handle providers where persistence is unavailable.
- Keep the selected URI, title and playback error associated with the attempted item.
- A file picker is a fallback, not a full-size panel permanently consuming the Player screen.
- Network URL entry is not included in this MVP.

## 4.8 Queue / play order

**Primary task:** Understand what plays next and change the order if reordering is implemented.

- Open from the audio player using a sheet on compact windows or a supporting pane on wider windows.
- Show current item and upcoming rows with artwork/title/artist.
- Clearly identify the active track semantically and visually.
- Previous/next follow the queue, not a hard-coded global list.
- If reordering is deferred, do not show drag handles; a read-only queue is acceptable until meaningful editing behavior and tests exist.

## 5. Shared visual system

### Reuse existing family design tokens
- Theme: existing `OpenMediaPlayerTheme`, semantic Material roles, dynamic color on supported Android versions, intentional violet/neutral fallback scheme.
- Spacing defaults: 4, 8, 12, 16, 24 and 32 dp; 20 dp compact page inset and roughly 24 dp section separation unless density/window needs justify a change.
- Shapes: existing small 8, medium 12, large 20, extra-large 28 dp. Do not apply extra-large rounding to every row/surface.
- Typography: existing Material 3 roles; use headline/title only for actual hierarchy, body for content, label for metadata. Avoid display-size text on operational library pages.
- Use official component defaults and stable public APIs first. Recheck Material 3 version and opt-ins before using expressive APIs. Do not upgrade to an alpha solely to gain decorative effects.

### Expression budget
- **Quiet:** page background, ordinary list rows, metadata and settings.
- **Clear:** selected destination, sort/view choice, permission/error/action state.
- **Expressive:** one primary action on the player and the transition into/out of playback; a small number of purposeful shape/motion accents.
- No default blur, glass, translucent cards or ornamental gradients. A localized media-control overlay is allowed only where video visibility needs it and contrast is checked against bright/dark footage.

### Containment
Start with alignment, whitespace and typography. Use one surface or divider where it makes ownership/state clear. Avoid cards inside cards and repeated outline/surface/divider/shadow around the same content. Rows in a homogeneous library should generally be unframed list items rather than independently elevated cards.

### Artwork and thumbnails
- Use real local metadata/artwork/thumbnails with a cache and cancellation-safe loading.
- A failure to render an image never blocks playback; always provide a stable placeholder.
- Do not extract colors from art in the first UI milestone unless performance and contrast can be validated; dynamic wallpaper color and the family fallback are enough initially.
- Avoid including media content, private absolute paths or embedded personal metadata in logs and tests.

## 6. Adaptive and accessibility contract

- Use current window size/adaptive APIs appropriate to installed dependencies; confirm the exact library/API before adding dependencies. Do not infer layout from physical screen dimensions.
- Support compact portrait, compact-height landscape, medium and expanded windows; adapt navigation and reflow/reveal supporting content when the window permits.
- No duplicated system inset padding. Check status/navigation bars, display cutouts, gesture areas and IME overlap.
- All important interactive targets at least 48 × 48 dp where practical; icon-only controls need localized/descriptive accessibility labels.
- Use string resources; support RTL layout and text wrapping. Preserve scalable font sizes; review 200% font-scale stress states if available.
- Expose playback state, toggles, active queue item, seek progress, permission explanations and errors to assistive technology. Do not communicate selection/state only by color or motion.
- Logical focus and keyboard/pointer operation where available; respect reduced-motion settings.
- Every async operation has an intentional loading, stale-content, empty, success or recoverable-error behavior.

## 7. State and interaction matrix

| Flow | Success path | Important failure/edge cases | Test gate |
|---|---|---|---|
| First launch | User sees Library; permission asked only when needed | Denied audio/video permission, selected-video access on Android 14+, access changed in Settings, permanently denied permission | UI permission tests; recheck on resume; no forced dead end |
| Scan/refresh | Scan indicator, then accurate media list | No files, revoked permission, query error, file disappears | Repository tests distinguish empty from failure; large-library performance test |
| Search | Results update and query can be cleared | No results, long filename, large font | UI instrumentation / screenshot |
| Open audio | Audio player opens selected row and begins playback | Invalid URI, old stop event arrives after new item, no artwork | Regression test and device playback evidence |
| Audio background | Audio remains playing with notification/system controls | Audio focus loss, headset disconnect, activity recreation, task removal | Service/media-session instrumentation + physical smoke test |
| Open video | Correct file renders; controls work | Decoder error, invalid file, no thumbnail, no frame produced | Device/emulator test with known open-license media |
| Rotate/resize video | Position and controls state remain coherent | rapid rotation, compact height, wide window, back during transition | Emulator matrix; capture portrait/landscape |
| Settings change | Preference remains after navigation/restart | unsupported dynamic color, corrupted/missing stored value | Unit + UI persistence test |
| Offline privacy | Local library and local playback work offline | Internet unavailable, no network permission, picker URI grant revoked | Offline smoke test and manifest review |

## 8. Implementation sequence and gates

1. **Phase 0 — Audit:** use `BASELINE_AUDIT.md`; establish screenshot/build provenance and validate test runner assumptions.
2. **Phase 1 — Design approval:** review this spec, capture a fresh screenshot baseline, finalize the exact artwork/thumbnail and navigation behavior, and inspect outstanding reference screenshots before implementation.
3. **Phase 2 — Playback correctness:** first reproduce audio/video failure with controlled local files, correlate LibVLC events to the selected media session, fix error visibility and surface/URI lifecycle, and build regression tests.
4. **Phase 3 — Shell and library UI:** implement four-destination navigation, library/music/video screens, permissions and states with actual artwork/thumbs and existing design tokens.
5. **Phase 4 — Dedicated audio player:** full player, queue source-of-truth, seek and transport controls, then service/media-session/background behavior.
6. **Phase 5 — Dedicated video player:** immersive/portrait behavior, timeline, rotation and lifecycle tests.
7. **Phase 6 — Settings, accessibility and polish:** persistent settings, privacy/about, accessible labels/states and adaptive review.
8. **Phase 7 — Release validation:** execute the automated/emulator/device matrix, licence review, release build and artifact validation.

Do not merge a phase just because it compiles. Each PR should include its test evidence and, for visual work, before/after captures at the relevant window sizes.

## 9. Non-goals for the MVP

- YouTube/streaming-client functions, online feed or recommendations.
- Direct network URL streams, remote shares, UPnP/DLNA discovery.
- Login, tracking, ads or remote upload of media metadata.
- Lyrics, AI playlists, tag editing, equalizer, scrobbling, casting and Android Auto.
- Placeholder controls for planned capabilities.
- A copied clone of any reference UI.

## 10. Open items to resolve before implementation
1. Confirm the supplied OpenMediaPlayer screenshots' originating commit / APK version, then capture a fresh set tied to a known commit.
2. Inspect the remaining individual mpvKt screenshots from the repository and a carefully selected LibreTube screen before locking any finer video-player controls; selected PixelPlayer, Booming Music and Namida screenshots have already been visually reviewed.
3. Verify official Material 3 and adaptive API availability for the installed BOM/version before selecting APIs or changing dependencies.
4. Define the exact audio service/media-session approach compatible with the chosen LibVLC wrapper.
5. Choose an open-license audio/video fixture set and record sample hashes/licenses, so playback tests are repeatable.
6. Complete LibVLC binary/codec/plugin license inventory and application licence decision before release.



## 11. Android selected-video access note

The current target SDK is 36. Android 14 introduced selected photos/videos access. The implementation must request and handle `READ_MEDIA_VISUAL_USER_SELECTED` alongside `READ_MEDIA_VIDEO` where appropriate on Android 14+, and provide a user-initiated reselection/manage-access path. This applies to the video collection, not the audio collection. Verify exact API behavior during implementation against the official page: https://developer.android.com/about/versions/14/changes/partial-photo-video-access
