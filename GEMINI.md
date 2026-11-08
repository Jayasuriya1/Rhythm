# Gemini / Google AI Studio project instructions

Follow `AGENTS.md` and the mandatory `design/AI_WORKFLOW.md` contract before making any change. Read the full design references listed there. Do not create a separate design system: the Kotlin theme package and `design/DESIGN_SYSTEM.md` are canonical. Use shared Material 3 semantic tokens, preserve the Ribbon R identity, and follow the documented visual/accessibility checks. Preserve existing features, persisted data, app ID and behavior unless explicitly approved.

**Mandatory for every feature, bug fix, refactor, dependency, manifest, resource or Gradle change:** before reporting completion, run the exact CI-equivalent commands in `design/AI_WORKFLOW.md` to assemble the debug APK, run Android lint and execute Robolectric unit tests. Fix failures introduced by the change, rerun the failed task, then rerun the complete gate set. Never hand off a knowingly broken build or claim success based only on code inspection.
