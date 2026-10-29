# AP Media Player — UI design specification

## Design direction
Build a distinctive, friendly Android media experience using Material 3 Expressive foundations: strong but readable type hierarchy, large rounded surfaces, tonal layering, pill-shaped filters, clear playback focus, and responsive light/dark themes. The visual direction is informed by the user's references to Echo Music (artwork and music-library presentation), Next Player (video playback controls and utility), and VLC (format compatibility and playback features). Do not copy any reference app screen pixel-for-pixel.

## Brand and visual language
- Product name: AP Media Player.
- Positioning line: Music. Video. Yours.
- Use Android dynamic color when available, with the existing purple/lilac family palette as fallback.
- Prefer spacious content, 24dp-ish large card corners, compact song rows, and strong primary playback controls.
- Use high contrast, touch targets at least 48dp, descriptive content descriptions, and support system font scaling.
- Keep content local-first and communicate media permissions clearly.

## Main information architecture
Bottom navigation: Home, Music, Videos, Library/Player. Keep the current local-media scanning and playback pathways working.
- Home: expressive greeting/title, search, Audio and Video entry cards, recently added items, and a compact now-playing surface when media is active.
- Music: songs list with search and category/filter chips; rows show title, artist/metadata, media type, and a clear play affordance.
- Videos: visual rows/cards with duration and resolution metadata when available; video playback gets a dedicated dark, edge-to-edge stage.
- Player: media stage, track/title context, progress when supported by the engine, large play/pause control, stop, local file picker, and network URL entry.
- Empty/permission states: explain why the library is empty and provide a direct permission/rescan action.

## Player screen
- Use a dark, immersive 16:9 video stage with rounded corners in portrait; preserve landscape/edge-to-edge video behavior.
- Place the title and status close to the stage, then large play/pause and stop controls.
- Keep local file selection and network streaming in separate, clearly labeled cards.
- Errors and loading states must remain visible and actionable; do not hide engine diagnostics.

## Delivery strategy
1. Establish the visual specification before implementation.
2. Update library/home surfaces and player controls to align with the spec without replacing the playback engine contract.
3. Build and test via GitHub Actions, then validate on a physical device for audio and video. The concept board is a direction, not proof that every pictured feature is implemented.
