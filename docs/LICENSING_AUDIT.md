# Licensing and Third-Party Distribution Audit

**Audit date:** 2026-10-22  
**Status:** Preliminary engineering inventory, not legal advice. Release is blocked until the open items below are resolved.

## 1. Findings from the current repository

- A repository-tree scan did not find a root LICENSE, COPYING, NOTICE, or third-party notice file on the audited main revision.
- The app declares org.videolan.android:libvlc-all:3.7.6 in its version catalog.
- Maven Central metadata for libvlc-all:3.7.6 identifies the artifact as an Android AAR and reports GNU Lesser General Public License v2.1. This metadata is useful but is not, by itself, a complete inventory of licenses for every binary, plug-in, codec, font, icon, or asset contained in the AAR.
- The VideoLAN LibVLC page describes the LibVLC engine as LGPL 2.1. VideoLAN's Android repository separately states that the VLC for Android application is GPLv2-or-later (and characterizes the app as de facto GPLv3) while LibVLC itself is LGPLv2.1. OpenMediaPlayer uses the LibVLC artifact; it must not copy the full VLC Android application's source or assets under the assumption that its license is identical to the engine's.

## 2. Primary sources checked

- VideoLAN LibVLC overview and declared engine license: https://images.videolan.org/vlc/libvlc.html
- VideoLAN Android source repository licensing statement: https://github.com/videolan/vlc-android/blob/master/README.md
- Maven Central artifact metadata for the newer libvlc-all coordinates: https://central.sonatype.com/artifact/org.videolan.android/libvlc-all
- Metadata for the exact version currently used (3.7.6): https://mvnrepository.com/artifact/org.videolan.android/libvlc-all/3.7.6
- VideoLAN's redistribution guidance: https://images.videolan.org/legal.html
- LGPL 2.1 text: https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html

The upstream Android source repo is the authority for exact-version component notices/source packages. If repository and artifact metadata disagree, investigate the exact binary and its corresponding source/build configuration before distribution.

## 3. Required pre-release work

### A. Choose an explicit license for OpenMediaPlayer itself
The maintainers must choose and approve an application-source license. Do not add an arbitrary license because the project is described as open source. Record the selected license in a root LICENSE, update the README and contribution guidance, and make sure it accurately covers only code/assets that the project has the right to license.

Keep these as separate decisions:
1. OpenMediaPlayer-authored application code and original artwork.
2. Third-party source code incorporated or copied into the project.
3. Dynamically linked/prebuilt native libraries and their accompanying plug-ins/codecs.
4. Reference-project screenshots and other media (used only as private design research, not to be shipped).

### B. Inventory the exact LibVLC AAR used by this build
Before the release candidate is assembled:
- Download and inspect the exact org.videolan.android:libvlc-all:3.7.6 AAR.
- List native libraries, assets, embedded notices and available source/offer information.
- Match the AAR to its corresponding VideoLAN source tag/commit and build configuration.
- Identify whether optional plug-ins, codec libraries or modules carry GPL, LGPL, permissive, patent-notice or other obligations distinct from the top-level AAR POM.
- Check any redistributed FFmpeg or codec components by exact binary/build configuration; do not infer every plug-in's license solely from LibVLC's engine license.
- Preserve the licenses, copyright notices and source references required by the components actually shipped.

### C. Provide notices and source availability
- Add a third-party inventory and notices file (for example, THIRD_PARTY_NOTICES.md) with exact component/version/license/source links and any required notices.
- Include required LGPL notices and the exact corresponding source or a compliant written-offer/link arrangement as applicable to the distributed build; preserve modifications if any.
- For any binary/codec whose terms require source disclosure or specific redistribution conditions, meet those requirements before publication.
- Verify that the delivered APK's bundled native libraries match the audited component inventory. A list of Gradle coordinates alone is not sufficient proof for a native AAR.

### D. Audit non-code assets and reference material
- Original app icon and design assets must have a clear author/license.
- Only use icons/fonts/artwork under terms compatible with the chosen distribution.
- Do not ship screenshots or artwork from PixelPlayer, Booming Music, mpvKt, Next Player, Namida, Echo Music, VLC or LibreTube as OpenMediaPlayer assets simply because they were supplied as UI references.
- Do not use VideoLAN's name, logo or VLC cone in OpenMediaPlayer branding without separately confirming relevant trademark permission/branding guidance.

### E. Validate the actual release package
- Generate a dependency/native-library license report, but manually check the LibVLC AAR contents and report.
- Inspect the final merged APK/AAB for native libraries, assets and license files.
- Ensure README/release notes identify third-party components and source repositories accurately.
- Keep the exact artifact version, source commit, checksums, notices and source links together in release records.
- Have the chosen distribution channel's current F-Droid / Play Store requirements reviewed separately; the same engineering build does not automatically meet every store's policy.

## 4. Current release blocker list

| Item | Status |
|---|---|
| App's own source license chosen | Open decision |
| Root LICENSE and README attribution | Missing / not verified |
| Exact LibVLC AAR component inventory | Not yet inspected |
| Native plug-in/codec license inventory | Not yet inspected |
| Third-party notices and corresponding source information | Not yet prepared |
| License/notice audit of fonts, icons and shipped assets | Not yet complete |
| Final APK/AAB content reconciled with notices | Not yet possible before release build |

## 5. Acceptance gate

Do not publish an APK/AAB or claim the project is distribution-ready until the maintainers have selected the app license, identified the exact third-party binaries and their terms, completed the required notice/source package, and checked the final distributable. This document is a tracking checklist; it is not legal advice or a determination that the current binary is compliant.
