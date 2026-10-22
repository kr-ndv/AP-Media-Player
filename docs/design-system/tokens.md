# Design tokens

Status: v1 foundation

## Color

Use semantic Material 3 roles rather than custom colors in feature code. Important roles include primary, onPrimary, primaryContainer, onPrimaryContainer, secondary, tertiary, error, surface, onSurface, onSurfaceVariant, outline, and surface-container roles.

The current fallback palette is seed-inspired violet with neutral surfaces. It is an implementation baseline, not a final family-brand decision. Dynamic colors take precedence on Android 12 and newer while enabled.

The light and dark fallback schemes are defined in the theme package. Check foreground/background contrast for every new token pairing, especially text over primary, secondary, tertiary, and container roles. Do not assume that a visually attractive combination passes every accessibility contrast target.

## Typography

Start with the Material 3 typography scale from Compose. Prefer display styles for rare, high-emphasis moments; headlines for page hierarchy; title styles for cards and sections; body styles for readable content; and label styles for controls and metadata.

A custom typeface is deferred until licensing, file size, multilingual glyph coverage, and brand direction are understood. Do not shrink text to make a layout fit.

## Shapes

Family shape defaults:
- Small: 8 dp
- Medium: 12 dp
- Large: 20 dp
- Extra-large: 28 dp

Use a shape that supports the component's meaning. Avoid rounding every surface identically just to make the UI appear expressive.

## Spacing

Family spacing tokens:
- Extra small: 4 dp
- Small: 8 dp
- Medium: 12 dp
- Large: 16 dp
- Extra large: 24 dp
- Extra-extra large: 32 dp
- Page horizontal inset: 20 dp
- Section separation: 24 dp

Use these as defaults, not rigid laws. Window size, content density, and accessibility needs may justify local layout decisions; document meaningful exceptions.

## Density and motion

Use dp for layout and sp for text. Do not encode physical screen dimensions. Prefer Material motion and component behavior where available; custom motion must preserve clarity, avoid unnecessary delay, and respect reduced-motion settings.
