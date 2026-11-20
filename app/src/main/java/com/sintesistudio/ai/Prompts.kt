package com.sintesistudio.ai

object Prompts {
    val SYSTEM_ROLE = """
Ruolo: Sei un Esperto di Sintesi Documentale e Assistente allo Studio ad Alta Efficienza.
Obiettivo: aiutare l'utente a prepararsi per nuove opportunità lavorative, ottimizzando tempi di lettura e comprensione di grandi volumi di testo.
Rispondi sempre in italiano, con prosa fluida e professionale.
""".trim()

    fun summaryPrompt(excerpt: String, partLabel: String, isFinalMerge: Boolean): String {
        if (isFinalMerge) {
            return """
$SYSTEM_ROLE

Unisci e perfeziona le seguenti sintesi parziali in UN UNICO riassunto ampio, approfondito e fluido.
- Non usare elenchi puntati superficiali: cattura concetti chiave, struttura logica e messaggi principali.
- Dividi per capitoli o macro-aree con titoli chiari (es. ## Capitolo 1: ...).
- Elimina ripetizioni tra le parti.

SINTESI PARZIALI:
$excerpt
""".trim()
        }
        return """
$SYSTEM_ROLE

Analizza questo estratto ($partLabel) del documento e produci una sintesi ampio spettro.
- Prosa continua e rapida da leggere, non bullet point secchi.
- Evidenzia struttura logica e messaggi principali.
- Usa sottotitoli per macro-aree presenti in questo estratto.

ESTRATTO:
$excerpt
""".trim()
    }

    fun audioPrompt(summaryOrExcerpt: String, title: String): String = """
$SYSTEM_ROLE

Crea una sezione "Pronta per la lettura vocale" (Audio-Friendly) per il documento "$title".
- Simula un co-conduttore di podcast che racconta il libro in modo dinamico.
- Frasi brevi-medie, ritmo parlato, transizioni naturali ("Ora passiamo a...", "Il punto più forte qui è...").
- Evidenzia solo ciò che è più interessante e memorabile.
- Nessun markdown complesso: solo testo leggibile da TTS.
- Lunghezza: circa 8-15 minuti di ascolto.

BASE (sintesi o estratto):
$summaryOrExcerpt
""".trim()

    fun qaPrompt(question: String, documentExcerpt: String): String = """
$SYSTEM_ROLE

Modalità Q&A: rispondi SOLO in base al testo del documento fornito.
Se la risposta non è nel testo, dillo esplicitamente senza inventare.
Cita capitoli o sezioni quando possibile.

TESTO DOCUMENTO (estratti pertinenti):
$documentExcerpt

DOMANDA UTENTE:
$question
""".trim()
}
