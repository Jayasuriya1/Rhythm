# Accessibility Standards

Accessibility is a release requirement.

## Touch and focus
- Aim for at least 48 × 48dp interactive targets.
- Ensure logical TalkBack traversal.
- Meaningful non-text controls need content descriptions; decorative items should not be focusable.
- Support keyboard/switch access where applicable.

## Text and color
- Support Android font scaling without clipping or overlap.
- Do not truncate medication names, alarm labels or important errors.
- Target WCAG 2.2 AA contrast: 4.5:1 normal text, 3:1 large text and essential UI components.
- Never use color as the only status indicator.

## Motion and charts
- Avoid flashing and unnecessary continuous motion; respect system animation settings.
- Charts need text summaries, units, labels and date ranges; do not distinguish series by color alone.
- Missing data is not zero.

## Test checklist
- [ ] TalkBack for all primary flows.
- [ ] Largest supported font and display size.
- [ ] Light, dark and OLED contrast.
- [ ] Touch target inspection.
- [ ] Narrow and landscape layouts where supported.
- [ ] Switch/keyboard access where relevant.
- [ ] Reduced animation.
- [ ] Permission and error announcements.

References: https://developer.android.com/guide/topics/ui/accessibility ; https://m3.material.io/foundations/accessible-design/overview
