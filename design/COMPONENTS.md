# Component Standards

Audit current Compose components before replacing or duplicating them.

## Inventory to standardize
Navigation: AppTopBar, PrimaryNavigationBar, NavigationDestination, ContextualOverflowMenu.
Layout: ScreenContainer, SectionHeader, ContentGroup, InlineMetadata, EmptyState, LoadingState, ErrorState.
Actions: PrimaryButton, SecondaryButton, TextButton, IconButton, DestructiveButton.
Data: MetricSummary, ProgressIndicator, TimelineRow, HistoryListItem, StatusLabel, ChartContainer.
Input: CheckInControl, TimePickerField, DateSelector, QuantityStepper, SearchField, NotesField.
Feedback: SnackbarMessage, ConfirmationDialog, BottomSheet, PermissionRationale.

## Contracts
- Buttons use verb-first labels; one primary action per screen/dialog.
- Disabled controls remain legible.
- Cards group independent content; they are not default wrappers. Avoid nested cards.
- Section headers use clear titles; descriptions only when useful.
- Empty states explain what is absent and offer one relevant action without shame.
- Dialogs/sheets handle focused short tasks, not complex workflows.
- Charts include title, metric, unit, date range, accessible summary and meaningful empty state.
- Every component defines applicable default, pressed, focused, disabled, selected, loading and error states.

## Compose guidance
Prefer small stateless composables with explicit parameters. Hoist state to screen/ViewModel owners. Keep business logic out of presentation. Use semantic theme tokens, stable test tags and previews for themes, empty/populated and large-font states. Do not add another component library for problems Material 3 already solves.
