# Family design principles

Status: v1 foundation

## Direction

The family direction is **expressive and modern**. Use Material 3 Expressive as the implementation baseline, Android dynamic color where supported, and a coordinated family fallback palette everywhere else.

This document sets shared defaults for current and future apps. It is not a mandate to reuse the same page layouts in every app.

## Principles

### 1. Expressive, not excessive

Use confident typography, clear hierarchy, distinctive but purposeful shapes, generous spacing, and motion that explains state changes. Do not add animation, color, cards, or decoration without a usability reason.

### 2. Personal, with a reliable fallback

On Android 12 (API 31) and newer, use Android dynamic color by default. On older versions, or when dynamic color is disabled, use the family light or dark fallback scheme. Both paths must be intentional and tested.

### 3. Shared identity, app-specific structure

Shared packages own semantic tokens and reusable patterns. Each app owns its navigation, information architecture, domain components, and workflows. An audio player, file browser, gallery, and utility should feel related without sharing inappropriate layouts.

### 4. Semantic tokens over literals

App screens should use MaterialTheme.colorScheme, MaterialTheme.typography, MaterialTheme.shapes, and FamilySpacing. Do not hardcode color or spacing values inside feature screens when a semantic token applies.

### 5. Compose-native foundations

Prefer official, stable Jetpack Compose and Material 3 APIs. Wrap or extend standard components only where the family has a real convention to preserve. Avoid reimplementing Material controls from scratch.

### 6. Accessibility and localization are baseline requirements

Build for font scaling, talkback labels and state, keyboard/focus navigation, minimum touch targets, sufficient contrast, RTL layouts, string resources, and reduced-motion needs from the start.

### 7. Private and calm by default

Do not add tracking, ads, sign-in, or network calls without an explicit product need. Request permissions contextually and only when a feature requires them.

## References

- Material Design 3: https://m3.material.io/
- Material 3 in Jetpack Compose: https://developer.android.com/develop/ui/compose/designsystems/material3
- Compose Material 3 release notes: https://developer.android.com/jetpack/androidx/releases/compose-material3

## Decisions to revisit

- Permanent family brand and app icon language.
- Whether a custom font is worthwhile, including license and language coverage.
- Final fallback palette after contrast validation and broader device testing.
- Extraction of the stable foundation into a separate shared library/repository.
