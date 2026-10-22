# Accessibility and inclusive UI

Status: v1 baseline

Accessibility is part of the design system and must be considered in every feature review.

## Requirements

- Preserve Android font scaling; never lock content text to a fixed size in sp or clip it to force a single line unless truncation is genuinely appropriate.
- Provide descriptive labels for icon-only controls and state descriptions for toggles, playback controls, selection, and loading.
- Keep interactive targets comfortably touchable; use 48 dp as the default minimum target where practical.
- Check contrast for text and important non-text controls in both light and dark fallback themes. Dynamic palettes should use semantic Material color roles.
- Support TalkBack, switch access, keyboard/focus navigation, and sensible focus order.
- Use string resources for UI copy so translation and RTL support are possible.
- Do not communicate state through color alone.
- Respect system animation settings and avoid motion that blocks important actions.
- Test compact screens, landscape, larger fonts, and different navigation modes.

## Review checklist

- Can the main action be found and understood quickly?
- Are controls named and their state announced?
- Does the layout remain usable at large font sizes?
- Is content readable in both theme modes?
- Are selections and errors communicated through more than color?
- Can the core flow be completed without precise gestures?
