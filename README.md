# Lyrics Studio

A glassy, single-file lyrics generator for Android and the web — 28 languages, 14 genres, 10 moods, native script or Roman letters. Works fully offline with no account, no ads and no key. Optionally plug in your own AI key for free-form lyrics.

<p align="center">
  <img src="screenshots/01-home.jpg" width="240" alt="Lyrics Studio home screen">
  <img src="screenshots/02-result-hindi.jpg" width="240" alt="Generated Hindi lyrics">
  <img src="screenshots/03-result-tamil-roman.jpg" width="240" alt="Tamil lyrics in Roman letters">
</p>

## Download

| | |
|---|---|
| **Android APK** | [apk/LyricsStudio.apk](apk/LyricsStudio.apk) — 81 KB, Android 6.0+ |
| **Web app** | [Lyrics_Studio.html](Lyrics_Studio.html) — one file, open it in any browser |

The APK is signed with a self-signed certificate, so Android will ask you to allow installing from unknown sources the first time. It is not on the Play Store.

## What it does

You describe an idea, pick a language, genre, mood and length, and it writes an original song with a title, verses, a repeating chorus and a bridge. The chorus stays identical every time it comes back, like a real song. Regenerate gives you a different take on the same settings rather than the same lines again.

- **28 languages** — Hindi, English, Hinglish, Punjabi, Urdu, Bengali, Marathi, Gujarati, Tamil, Telugu, Kannada, Malayalam, Odia, Bhojpuri, Nepali, Spanish, French, German, Portuguese, Italian, Arabic, Turkish, Russian, Japanese, Korean, Chinese, Indonesian, Swahili
- **14 genres** and **10 moods**, both of which steer the word choice (Ghazal leans melancholic, Sufi leans spiritual, Hip-hop leans energetic, and so on)
- **3 lengths** — short (verse + chorus), standard (two verses, chorus, bridge) and long (three verses)
- **Native script or Roman letters** for every language, transliterated in-app
- **Your idea becomes the title**, and its key noun gets woven into the verse when the idea is written in the same script as the song
- **Copy, Share and Save .txt** — on Android these go through the native layer and land in `Downloads/Lyrics Studio/`
- **Recent lyrics** are kept on the device; tap one to bring it back

## Bring your own AI (optional)

Tap the gear icon, switch **Lyrics source** to *AI model*, and pick a provider:

- **Local AI** — talks to a model already running on your own device. No key, no account, no download here. It looks for an OpenAI-compatible server on `127.0.0.1` (Ollama on 11434, LocalAI on 8080, LM Studio on 1234, or 5000) and uses the first one that answers; you can also type the server address yourself. If it finds nothing, it falls back to the built-in engine. Small models (1B or under) run in 1-2 GB of RAM and stay entirely on the phone.
- **Gemini** or **OpenAI** — paste your own API key and the prompt goes straight to that provider.

Whatever fails, the built-in engine takes over, so the button never leaves you with nothing.

## How the built-in engine works

Lyrics Studio ships with 569 hand-written verse, chorus, bridge and opening lines across the 28 languages. Songs are not assembled from fixed stanzas: each section is composed line by line, drawing from every line of that language that fits the mood, and pairs from the same block are preferred so couplets still rhyme. Nothing repeats inside a song, and lines you have already seen are pushed to the back of the queue, so regenerating gives you a genuinely different song rather than the same verses reshuffled.

When the on-mood lines run out, the rest of that language's material comes in, so a long session keeps producing fresh lines instead of looping.

This is a curated generative engine, not a language model. Lines are original and rhyme within each section, but the vocabulary is finite — a few hundred lines per language. For unlimited, free-form verses, use the Local AI or key mode. The Roman transliteration is an approximation of the script, not a dictionary transcription.

## Build from source

The web app is the whole product; the Android app is a thin WebView shell around it.

```bash
# web: nothing to build, just open the file
open Lyrics_Studio.html

# android: see android/build.sh — needs a JDK 17 and Android SDK build-tools 34
cd android && ./build.sh
```

`android/build.sh` runs `aapt2` → `javac` → `d8` → `zipalign` → `apksigner` by hand, so Gradle is not required. It writes a signed APK to `apk/LyricsStudio.apk`.

## Privacy

No analytics, no accounts, no tracking. The only network request the app can ever make is the one you explicitly configure with your own API key. Everything else happens on the device.

## A note on the lyrics

Everything here is written by the app. Please don't pass generated lyrics off as another artist's work, and don't present them as human-written if that matters in your context.

## License

MIT — see [LICENSE](LICENSE).
