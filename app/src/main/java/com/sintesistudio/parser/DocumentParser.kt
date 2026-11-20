package com.sintesistudio.parser

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

class DocumentParser(private val context: Context) {
    private val pdfExtractor by lazy { PdfTextExtractor(context) }
    private val epubExtractor = EpubTextExtractor()

    fun parse(uri: Uri): ParseResult {
        val warnings = mutableListOf<String>()
        val (displayName, mime) = resolveMeta(uri)
        val format = detectFormat(displayName, mime)
        val fallbackTitle = displayName.substringBeforeLast('.').ifBlank { "Documento" }

        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: throw IllegalStateException("Impossibile aprire il file")

        val rawText: String
        val title: String

        when (format) {
            "pdf" -> {
                title = fallbackTitle
                rawText = pdfExtractor.extract(bytes.inputStream(), warnings)
            }
            "epub" -> {
                val (epubTitle, body) = epubExtractor.extract(bytes.inputStream(), warnings)
                title = epubTitle.ifBlank { fallbackTitle }
                rawText = body
            }
            "txt" -> {
                title = fallbackTitle
                rawText = decodeTextFile(bytes, warnings)
            }
            else -> throw IllegalArgumentException("Formato non supportato: $format")
        }

        val normalized = TextNormalizer.normalize(rawText, warnings)
        if (normalized.length < 200) {
            warnings.add("Testo estratto molto breve: verifica che il file non sia protetto o scansionato.")
        }

        return ParseResult(
            title = title,
            format = format,
            text = normalized,
            warnings = warnings
        )
    }

    private fun decodeTextFile(bytes: ByteArray, warnings: MutableList<String>): String {
        TextNormalizer.detectEncodingLabel(bytes)?.let { warnings.add(it) }
        val attempts = listOf(
            Charsets.UTF_8,
            java.nio.charset.Charset.forName("ISO-8859-1"),
            java.nio.charset.Charset.forName("windows-1252")
        )
        var best = ""
        var bestScore = -1
        for (cs in attempts) {
            runCatching {
                val s = String(bytes, cs)
                val score = s.count { it.isLetterOrDigit() }
                if (score > bestScore) {
                    bestScore = score
                    best = s
                }
            }
        }
        return best
    }

    private fun resolveMeta(uri: Uri): Pair<String, String?> {
        var name = "documento"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx) ?: name
            }
        }
        val mime = context.contentResolver.getType(uri)
        return name to mime
    }

    private fun detectFormat(name: String, mime: String?): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".pdf") || mime?.contains("pdf") == true -> "pdf"
            lower.endsWith(".epub") || lower.endsWith(".ipub") ||
                mime?.contains("epub") == true -> "epub"
            lower.endsWith(".txt") || mime?.startsWith("text/") == true -> "txt"
            else -> throw IllegalArgumentException(
                "Formato non supportato. Usa PDF, EPUB, iPUB o TXT."
            )
        }
    }
}

private fun ByteArray.inputStream() = java.io.ByteArrayInputStream(this)
