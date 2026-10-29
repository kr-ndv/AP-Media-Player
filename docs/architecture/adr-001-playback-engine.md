# Architecture Decision Record: Playback engine

- Status: provisional; validate with a device/emulator before treating as final.
- Date: 2026-10-22

## Context

OpenMediaPlayer's initial release is scoped to local audio and video files on the device, including MediaStore items and files the user selects through the system document picker. Network streams and remote shares are not part of the MVP. The playback implementation must remain replaceable so UI and media-library code do not become coupled to one vendor API.

## Decision

Prototype the official VideoLAN Android LibVLC artifact, using the latest stable VLC 3.x artifact confirmed by the upstream VideoLAN Android build configuration at the time of this decision. Keep VLC 4.0 EAP out of the initial prototype.

Place the engine behind a small app-owned `PlaybackEngine` interface. Do not expose LibVLC types to Compose screens or repositories. Add only the Internet permission initially; local media access should use MediaStore/Storage Access Framework and avoid broad storage permissions.

## Why this candidate

The official VideoLAN project documents LibVLC support for local media, a wide range of formats and protocols, and hardware decoding where supported. For the MVP, the engine is evaluated only on local media; the existence of remote protocol support is not a reason to surface networking UI or request Internet access.

Sources:
- https://github.com/videolan/vlc-android
- https://code.videolan.org/videolan/vlc-android/-/tags
- https://www.videolan.org/legal.html

## Licensing and distribution gate

Do not infer that the complete engine, every codec/plugin, and our application all share one license. The upstream project distinguishes the Android application (GPLv2-or-later; the Android app is described upstream as effectively GPLv3) from the LibVLC engine (LGPLv2-or-later), and individual plugins can have different terms.

Before a public binary release:
1. Pin the exact artifact and inspect its POM, notices and bundled native components.
2. Inventory transitive and native plugin licenses.
3. Confirm source-offer, notices, relinking/replacement and other applicable obligations for each distribution channel.
4. Choose and add the application's own license only after reviewing the resulting combination.

## Prototype acceptance criteria

- CI resolves and packages the selected artifact for supported Android ABIs.
- A real device/emulator can open and play local audio and video using both MediaStore content URIs and a system document-picker URI.
- Confirm local playback works with the app offline and does not require the Internet permission.
- Verify pause/resume, lifecycle/background behavior, audio focus, video surface handling and subtitles/tracks.
- Record tested Android versions, ABI, device, known limitations and licensing findings.

## Alternatives

- AndroidX Media3/ExoPlayer: smaller, Android-native path for common supported formats/protocols, but not a drop-in replacement for all VLC codec/protocol capabilities.
- A split engine strategy: use Media3 for common playback and LibVLC for fallback formats. This increases testing, UX and maintenance complexity and should only be considered if prototype measurements justify it.

## Consequences

The engine interface is the stable app-owned boundary; LibVLC remains an implementation detail. No feature should claim universal codec, stream, DRM or network compatibility. Hardware decoding and format support vary by device and by the exact native build.


## Product scope clarification — 2026-10-22

The product requirement is local media playback only for the first release. Earlier wording in this ADR about network streams, protocol browsing and a controlled HTTP acceptance test describes LibVLC capability, not the accepted MVP scope. The MVP must not include URL stream entry, network-share discovery, online feeds, or related networking permissions. Reopen this scope only through a separate product decision and a security/permission review.

This clarification does not change the need to validate the selected LibVLC artifact's license terms and local-file playback behavior before distribution.
