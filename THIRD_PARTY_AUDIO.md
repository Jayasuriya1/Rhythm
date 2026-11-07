# Third-Party Audio & Soundscape Licences

Rhythm & Focus (`com.jayasuriya.rhythm`) is committed to open-source compliance, F-Droid publication guidelines, and ethical licensing.

## 1. Procedural Ambient Concentration Soundscapes

All ambient soundscapes in Rhythm & Focus are procedurally synthesised completely on-device in real-time or from verified royalty-free Creative Commons Zero (CC0) / Public Domain algorithms.

- **Offline Independence**: 100% offline. No proprietary codecs or remote server dependencies.
- **Audio Synthesis Engine**: Real-time 16-bit PCM multi-frequency wave generator using low-pass resonant filtering, Hann window crossfading, Poisson impulse distribution, and pink/brown noise integration.
- **Audio Latency & Looping**: Generated into static Hardware AudioTrack buffers with infinite sub-millisecond loop points and zero loop-boundary clicks.

### Soundscape Inventory

| Soundscape ID | Display Name | Acoustic Model / Origin | Licence | Attribution Required | Modification Allowed |
|---|---|---|---|---|---|
| `none` | Silence | N/A (Mute) | Public Domain / CC0 | No | Yes |
| `brown` | Deep Brown Noise | Leaky-integrator low-pass on white noise (1/f² power density) | CC0 1.0 Universal / Public Domain | No | Yes |
| `pink` | Pink Noise | Multi-stage Voss-McCartney 1/f noise filter | CC0 1.0 Universal / Public Domain | No | Yes |
| `rain` | Gentle Rain | Dual-stage pink noise floor with Poisson-distributed water droplet resonances | CC0 1.0 Universal / Public Domain | No | Yes |
| `rain_window` | Rain on Window | Mid-frequency high-pass rainfall with intermittent glass tap impulses | CC0 1.0 Universal / Public Domain | No | Yes |
| `forest_birds` | Forest Birds | Ambient pine wind floor with natural frequency-modulated bird call synthesis | CC0 1.0 Universal / Public Domain | No | Yes |
| `river` | Flowing River | Continuous broadband turbulent water flow with multi-pole resonant smoothing | CC0 1.0 Universal / Public Domain | No | Yes |
| `ocean` | Ocean Waves | Low-frequency brown noise with 3.5s sinusoidal swell envelope modulation | CC0 1.0 Universal / Public Domain | No | Yes |
| `fireplace` | Fireplace | Warm low-frequency rumble with stochastic ember pop/crackle impulses | CC0 1.0 Universal / Public Domain | No | Yes |
| `wind` | Wind Through Trees | Low-frequency bandpass-filtered noise with slow 0.35 Hz gust envelope | CC0 1.0 Universal / Public Domain | No | Yes |
| `crickets` | Night Crickets | Soft nighttime air floor with high-frequency 4.5 kHz rhythmic chirping bursts | CC0 1.0 Universal / Public Domain | No | Yes |

---

## 2. Local Music Player

The offline music player plays user-owned files stored locally on their device (MP3, M4A, FLAC, WAV, AAC, OGG).
- No proprietary cloud streaming service is bundled.
- Uses Android standard `MediaPlayer` and Storage Access Framework (`ACTION_OPEN_DOCUMENT`).
- All media remains in the user's private device storage or app-private storage.

---

## 3. External Streaming Integrations (Optional)

Any links to Spotify, YouTube, or Amazon Music strictly launch official Android intents targeting the user's installed official applications or standard web browsers.
- No copyrighted streams are scraped, downloaded, or redistributed.
- No advertisements are blocked or circumvented.
- Completely optional and decoupled from core offline app functionality.
