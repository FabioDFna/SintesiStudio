# SintesiStudio

App per sintesi documentale ad alta efficienza: PDF, EPUB, iPUB e TXT, con riassunti per capitoli, script stile podcast (TTS) e modalità Q&A su tutto il testo in memoria.

| Piattaforma | Come usarla |
|-------------|-------------|
| **Android** | APK in `app/build/outputs/apk/debug/` |
| **iPhone** | Versione web in [`web/`](web/) — vedi [web/README-IPHONE.md](web/README-IPHONE.md) |

## Funzionalità

- **Parsing robusto**: PDF (PdfBox), EPUB/iPUB (epub4j), TXT con fallback encoding UTF-8 / ISO-8859-1 / Windows-1252
- **Sintesi ampio spettro**: Gemini, chunking automatico per libri 300–500+ pagine
- **Podcast TTS**: testo Audio-Friendly + lettura vocale integrata
- **Q&A**: risposte basate sul testo completo salvato in database locale

## Requisiti

- Android Studio Ladybug o più recente, oppure JDK 17 + Android SDK
- API key Gemini: https://aistudio.google.com/apikey

## Build APK

```powershell
cd "C:\Users\Utente New\Projects\SintesiStudio"

# Prima volta: genera wrapper (se Android Studio non l'ha fatto)
# In Android Studio: File → Open → SintesiStudio → Build → Build APK(s)

# Da terminale (con SDK configurato):
.\gradlew.bat assembleDebug
```

APK debug: `app\build\outputs\apk\debug\app-debug.apk`

APK release firmato:

```powershell
.\gradlew.bat assembleRelease
```

## Uso

1. Apri l'app → **Impostazioni** → incolla la API key Gemini → Salva
2. Tocca **+** e seleziona PDF / EPUB / TXT
3. Apri il documento → **Genera sintesi** (può richiedere alcuni minuti per libri lunghi)
4. Tab **Podcast TTS** → genera script → ▶ per ascoltare
5. Tab **Q&A** → fai domande su capitoli e concetti

## Note

- PDF scansionati (solo immagini) non contengono testo selezionabile: serve OCR esterno
- La sintesi usa la rete (API Gemini); i documenti restano sul dispositivo
- Per libri molto grandi, il costo/tempo API aumenta con il numero di chunk
