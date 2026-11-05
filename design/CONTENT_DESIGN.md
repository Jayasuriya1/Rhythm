# Content Design and Product Language

## Voice
Calm, direct, supportive and factual. Never shame users for missed routines, incomplete tasks or gaps in health logs.

## Microcopy rules
- Prefer short, action-oriented labels: “Start focus”, “Log sleep”, “Save target”.
- Use sentence case; avoid unnecessary all-caps status labels.
- Explain what happens after an action.
- Prefer “Not recorded” over “Missing” or “Failed”.
- Distinguish “Skipped”, “Not recorded” and “Unavailable”.
- Avoid promising outcomes such as “stabilize your body clock” or “improve mental health”; describe the feature's purpose without medical claims.
- Use concrete permission explanations: what data, why, when and how to revoke access.
- Avoid jargon unless the screen is explicitly for advanced configuration.

## Empty states
Every empty state should explain the current state and offer one relevant next action. Avoid nested cards, duplicate CTAs, large decorative illustrations and motivational paragraphs.

## Errors
Use: what happened + what the user can do next. Preserve user input. Do not expose stack traces or technical identifiers.

## Health and medication language
- Clearly distinguish self-reported entries from connected measurements.
- Use neutral labels and units.
- Never infer diagnosis, episode state or treatment advice.
- Never suggest changing a prescribed dose.
- Do not imply that a single measurement explains a mood or sleep pattern.

## Localization
- Externalize user-facing strings to Android resources.
- Avoid concatenated sentence fragments; use formatted resource strings.
- Test longer translations and right-to-left layouts.
- Keep labels concise but meaningful.
- Never shrink text solely to force a translation to fit; allow wrapping or adapt layout.
