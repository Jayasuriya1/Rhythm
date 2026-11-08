# Rhythm soundscape audio sourcing

Status: source candidates documented; **no third-party audio files are bundled yet**.

Rhythm currently synthesizes short procedural textures in `ConcentrationSoundscapeHelper`. Those are useful offline fallbacks, but they are not field recordings. Do not label them as authentic rain, birds, river, ocean, or fire recordings.

## Acceptance rules before bundling

1. Use only source pages that explicitly identify the individual recording as CC0 1.0 / public-domain dedication, or obtain written permission for redistribution.
2. Verify the licence on the original sound page at the time the binary is downloaded. A collection or remix licence does not automatically cover each source recording.
3. Keep a copy of the source URL, creator, source title, licence, download date, original filename, and any processing notes in the app's audio credits.
4. Prefer short, clean, loopable recordings. Trim silence, avoid abrupt transients, normalize conservatively, and audition loop boundaries.
5. Keep assets small (target roughly 1–3 MB per sound where quality permits). Do not add very large source WAVs directly to the APK.
6. Keep procedural synthesis as an offline fallback. Never download audio at runtime or make the focus timer depend on network access.

## Candidate source catalogue (not yet packaged)

| In-app sound | Candidate source | Creator | Licence evidence | Notes |
|---|---|---|---|---|
| Gentle rain | Freesound #640655, “Soft rain.WAV” | barkenov | Listed as CC0 on the source site's audio-credit page | Candidate; obtain original from Freesound and re-check its individual page before bundling. |
| Rain on window | Freesound #243781, “rain against window 2” | bastipictures | Listed as CC0 on the source site's audio-credit page | Candidate; re-check original page and inspect for loop points. |
| Wind / leaves | Freesound #527281, “Monologue Wind” | dlgebert | Listed as CC0 on the source site's audio-credit page | Candidate; re-check original page and listen for wind peaks. |
| Ocean | Freesound #578524, “Calm ocean waves” | SamsterBirdies | Listed as CC0 on the ocean source site's audio-credit page | Candidate; described as naturally loopable; re-check original page. |
| Birds / park ambience | OpenGameArt “Park ambiences” | Thimras | Page identifies the pack as CC0 | Three WAVs are very large (74–99 MB); do not bundle as-is. |

## Source and licence references

- Freesound licence guide: https://freesound.org/help/faq/
- Rain source catalogue and CC0 credits: https://rainsoundsforsleeping.com/licensing/
- Ocean source catalogue and CC0 credits: https://oceansoundsforsleeping.com/licensing/
- OpenGameArt park ambience pack: https://opengameart.org/content/park-ambiences
- CC0 1.0 legal text: https://creativecommons.org/publicdomain/zero/1.0/

The rain and ocean catalogue pages are useful discovery/credit references, not substitutes for checking the original Freesound sound page. This document is an acquisition checklist, not a claim that the files have already been downloaded, verified locally, or included in Rhythm.
