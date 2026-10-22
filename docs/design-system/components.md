# Shared component conventions

Status: v1 foundation

## Ownership

Reusable components live in the core design package. Feature screens may compose these primitives but must not place app-specific business rules inside them.

## Initial shared patterns

- OpenMediaPlayerTheme: dynamic/fallback light and dark color schemes, typography, and shapes.
- FamilySpacing: consistent spacing tokens.
- FamilySectionHeader: reusable section title and optional description.
- SettingSwitchRow: a consistent title, supporting description, and switch layout.
- Material 3 primitives: buttons, cards, chips, dialogs, and surfaces should be used directly unless the family needs a stable wrapper.

## API guidelines

- Expose state as parameters; do not hide app state inside shared UI.
- Keep callbacks explicit and composables previewable.
- Accept Modifier as the final optional parameter.
- Use semantic MaterialTheme values, not literal colors.
- Use string resources for user-visible copy.
- Add semantics or content descriptions where interactive controls need them.
- Avoid wrappers that only rename an existing Material component without adding family-level behavior.
- Keep layouts responsive and allow text to wrap or scale.

## Adding a component

Before adding a shared component, check whether Material 3 already provides the behavior. If a wrapper is needed, document the convention it preserves and add a visual example to the design-system showcase.

A shared component is ready to reuse when it works in light/dark and dynamic/fallback themes, remains usable with enlarged text, exposes the correct semantics, and has a clear state/callback contract.
