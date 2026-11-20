package com.sintesistudio.parser

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

class PdfTextExtractor(context: Context) {
    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    fun extract(input: InputStream, warnings: MutableList<String>): String {
        val builder = StringBuilder()
        PDDocument.load(input).use { doc ->
            val pages = doc.numberOfPages
            val stripper = PDFTextStripper().apply {
                sortByPosition = true
            }
            for (page in 1..pages) {
                try {
                    stripper.startPage = page
                    stripper.endPage = page
                    val pageText = stripper.getText(doc)
                    if (pageText.isNotBlank()) {
                        builder.append("--- Pagina $page ---\n")
                        builder.append(pageText.trim())
                        builder.append("\n\n")
                    }
                } catch (e: Exception) {
                    warnings.add("Pagina $page: estrazione parziale (${e.message})")
                }
            }
            if (builder.isEmpty()) {
                warnings.add("Nessun testo selezionabile trovato: il PDF potrebbe essere solo immagini (scansione).")
            }
        }
        return builder.toString()
    }
}
