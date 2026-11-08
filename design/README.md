# Rhythm Design & Product Quality Hub

This folder is the canonical source of truth for product direction, brand identity, design tokens, screen behavior, accessibility, AI contribution rules and release quality.

## Start here
1. [AI contribution contract](AI_WORKFLOW.md) — mandatory rules for every AI and human contributor.
2. [Product vision](PRODUCT_VISION.md)
3. [Design philosophy](DESIGN_PHILOSOPHY.md)
4. [Design system](DESIGN_SYSTEM.md) — runtime token locations and usage rules.
5. [Brand assets](BRAND_ASSETS.md)
6. [Component standards](COMPONENTS.md)
7. [Information architecture](INFORMATION_ARCHITECTURE.md)
8. [Screen specifications](screens/README.md)
9. [Accessibility](ACCESSIBILITY.md)
10. [Visual QA](VISUAL_QA.md)
11. [Quality gates](QUALITY_GATES.md)
12. [Current UX audit](audit/UX_AUDIT.md)
13. [Feature priority backlog](audit/FEATURE_PRIORITY.md)
14. [F-Droid readiness](F_DROID_AUDIT.md)

## Current design direction
Rhythm uses a calm, modern, editorial wellness aesthetic: deep teal, muted mint, restrained champagne and violet, purposeful spacing, readable typography and clear hierarchy. Default is OLED Dark, with Standard Dark and Light plus System following. All are first-class experiences.

## Non-negotiable operating rules
- Read the AI contribution contract before every code change.
- Use the Kotlin theme package as the only runtime source of truth for colors and typography.
- Never introduce screen-local palettes, unexplained hardcoded colors or competing brand marks.
- Preserve existing app behavior and persisted data during visual-only work.
- Validate with real rendered screenshots; source code or mockups alone do not prove visual quality.
- Keep assumptions and unverified work explicitly labeled.

## Foundation status
Central Material 3 theme and Ribbon R adaptive launcher artwork already exist. This contract makes their use mandatory and aligns tokens with brand identity. Screen-by-screen adoption and rendered QA remain ongoing; do not claim all screens are compliant.
