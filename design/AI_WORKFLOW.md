# AI-Assisted Design and Engineering Workflow

This is the operating contract for Gemini, Antigravity and other agents.

## Before every task
Read this folder's README, product vision, philosophy, design system and relevant screen specification. Inspect implementation and tests. Identify affected files, behavior, assumptions and open questions. Separate visual changes from behavior changes and refactoring.

## Design / audit mode
Inspect source and real screenshots when available. Describe current behavior and user problem. Propose hierarchy, interaction and components. Produce a screen spec or design proposal. Do not edit app source unless explicitly asked.

## Implementation mode
Implement only an approved specification. Preserve business logic and persistence unless behavior changes are explicitly in scope. Reuse tokens/components. Add previews/tests. Build and run relevant tests. Capture real screenshots where tools permit. Report deviations.

## Non-negotiable rules
- Never claim build/tests passed unless executed and observed.
- Never invent paths, APIs or behavior.
- Never silently remove features or change database schemas.
- Explain dependency purpose, license and maintenance cost before adding one.
- No analytics, trackers or remote services without explicit privacy approval.
- No proprietary dependency in F-Droid build without an approved compliant flavor and policy review.
- No fake production data, secrets, signing keys or personal health data in commits.
- Keep changes reviewable and reversible.
- Update design docs when decisions change.

## Required task report
Summarize user-visible changes, files changed, behavior preserved/changed, exact commands/results, devices/themes/screens inspected, accessibility considerations and limitations.

## Visual QA loop
Build; install; open exact screen; capture light/dark screenshots; compare hierarchy, spacing, typography, contrast and alignment; test interactions and TalkBack; fix the highest-impact issue; repeat.

## Git
Use a feature branch, focused commits and scoped pull requests. Never force-push shared branches.
