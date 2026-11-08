# Repository instructions for AI coding agents

Before any task, read `design/AI_WORKFLOW.md` and `design/README.md`. The design folder is the canonical product and UI source of truth.

## Always
- Inspect existing code before editing; reuse architecture and components.
- For UI work, follow `design/DESIGN_SYSTEM.md`, `design/BRAND_ASSETS.md`, `design/COMPONENTS.md` and the relevant screen specification.
- Use the shared Kotlin Material 3 theme. Never hardcode screen-local hex/RGB colors or create another palette.
- Preserve the Ribbon R logo, app ID, Room schema, user data and existing behavior unless explicitly authorized.
- Keep the product calm, accessible, low-glare, responsive and privacy-first.
- Do not remove existing features or introduce dependencies/services/assets without justification and approval.
- Run relevant build, tests and lint; report exact results. Never claim unrun checks passed.
- Review real screenshots for applicable themes and device sizes.
- Keep changes focused, reversible and documented.

If requirements conflict or a requested change risks data or behavior, stop and explain before proceeding.
