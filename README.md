# SintesiStudio

A high-efficiency document synthesis app: PDF, EPUB, iPUB, and TXT, with chapter summaries, podcast-style scripts (TTS), and a Q&A mode over the full text stored in memory.

| Platform | How to use it |
|----------|---------------|
| **Android** | APK in `app/build/outputs/apk/debug/` |
| **iPhone** | Web version in [`web/`](web/) — see [web/README-IPHONE.md](web/README-IPHONE.md) |

## Features

- **Robust parsing**: PDF (PdfBox), EPUB/iPUB (epub4j), TXT with encoding fallback UTF-8 / ISO-8859-1 / Windows-1252
- **Broad-spectrum synthesis**: Gemini, automatic chunking for books of 300–500+ pages
- **Podcast TTS**: audio-friendly text plus built-in speech playback
- **Q&A**: answers based on the full text saved in the local database

## Requirements

- Android Studio Ladybug or newer, or JDK 17 + Android SDK
- Gemini API key: https://aistudio.google.com/apikey

## Build the APK

```powershell
cd "C:\Users\Utente New\Projects\SintesiStudio"

# First time: generate the wrapper (if Android Studio has not already)
# In Android Studio: File → Open → SintesiStudio → Build → Build APK(s)

# From the terminal (with the SDK configured):
.\gradlew.bat assembleDebug
```

Debug APK: `app\build\outputs\apk\debug\app-debug.apk`

Signed release APK:

```powershell
.\gradlew.bat assembleRelease
```

## Usage

1. Open the app → **Settings** → paste the Gemini API key → Save
2. Tap **+** and select a PDF / EPUB / TXT
3. Open the document → **Generate synthesis** (this can take several minutes for long books)
4. **Podcast TTS** tab → generate the script → ▶ to listen
5. **Q&A** tab → ask questions about chapters and concepts

## Notes

- Scanned PDFs (images only) do not contain selectable text: external OCR is required
- Synthesis uses the network (Gemini API); documents stay on the device
- For very large books, API cost and time increase with the number of chunks
