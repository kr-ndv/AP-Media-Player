# Playback Diagnostics — Local Media

**Status:** Initial playback-state diagnostics. This guide helps reproduce failures; it is not proof that every local file, codec, or device works.

## What changed in this diagnostic milestone

- Playback-state listeners receive the immutable snapshot that triggered the callback rather than reading whichever state happens to be current when the main-thread callback executes.
- The engine logs open/play/pause/stop/release commands and observed LibVLC state events under the `OpenMediaPlayerPlayback` tag.
- Logs record the source URI scheme and attempt number, not the full URI or local file path.
- A native `Stopped` event no longer independently sets app state. The app-owned `stop()` command reports `STOPPED`; this avoids a delayed native stop from an item being replaced from overwriting the replacement's state. This needs device/emulator regression testing.
- Android `content://` inputs (the normal shape of MediaStore and document-picker selections) are opened through `ContentResolver` and handed to LibVLC through an `fd://` media location; the corresponding descriptor remains open until the media is replaced or the engine is released. A pure unit test covers the mapping, while the actual provider/decoder path still needs device testing.
- The generic playback error copy is now local-file oriented.
- Unit tests cover the native-event-to-snapshot mapping, including that `Stopped` does not produce an app snapshot.

**This does not establish the root cause of the previously reported black video/stopped-state symptom.** The direct `content://` handoff was a credible source-code problem, and it has been changed to the descriptor-backed path, but real MediaStore/document-picker playback and LibVLC video output still need to be tested on an emulator and physical device.

## Reproduction checklist

Use two short local test files that are known to play in another player on the same device. Do not share personal media or full file paths in bug reports.

1. Install the debug APK built from the commit under test.
2. Launch OpenMediaPlayer and grant only the local media access the test requires, or open the file using the Android document picker.
3. Play one local audio file and listen for audible output; verify the state changes to Playing.
4. Pause and resume, then stop.
5. Play a local video and verify that decoded frames appear (a black canvas alone is not success).
6. While one item is active, quickly open a second item to exercise the replacement path.
7. Rotate the device during video playback, then return to the app and verify that the selected item and visible output remain coherent.
8. Repeat with one unsupported/corrupt file to verify that an error is visible and the app can recover by playing a different file.

Test audio and video separately. If audio works but video is black, capture that distinction explicitly.

## Capture a useful playback log

On a computer with Android platform tools installed:

```sh
adb logcat -c
adb logcat -v threadtime -s OpenMediaPlayerPlayback:D VLC:V LibVLC:V AndroidRuntime:E '*:S'
```

With the log running, reproduce one failure from a fresh app launch. Stop capture after the error appears. Include the tested app commit, Android version, device model, whether the item was opened from MediaStore or the document picker, media kind (audio/video), and whether sound/frames appeared.

The app's own log intentionally avoids printing the full content URI or file path. If a media item's exact codec/container is relevant, report only the media format or share a small non-personal test fixture whose redistribution is permitted.

## Known evidence limits

- Unit tests exercise the pure state mapper, not native LibVLC decoding.
- The current Android CI workflow compiles the instrumentation APK but does not run UI instrumentation on an emulator.
- A successful build cannot verify audio output, hardware decoding, rendering surfaces, audio focus, or activity recreation.
- Mark the issue fixed only after the reproduction checklist passes on an emulator and a physical device; retain device/API/format notes with the test result.
