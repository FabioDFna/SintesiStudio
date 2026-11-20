package com.sintesistudio.parser

import io.documentnode.epub4j.domain.Resource
import io.documentnode.epub4j.domain.TOCReference
import io.documentnode.epub4j.epub.EpubReader
import java.io.InputStream
import java.nio.charset.Charset

class EpubTextExtractor {
    fun extract(input: InputStream, warnings: MutableList<String>): Pair<String, String> {
        val book = EpubReader().readEpub(input)
        val title = book.title?.takeIf { it.isNotBlank() } ?: "Documento EPUB"
        val builder = StringBuilder()
        val seen = mutableSetOf<String>()

        fun appendResource(resource: Resource, label: String) {
            val href = resource.href ?: label
            val key = href.lowercase()
            if (key in seen) return
            seen.add(key)
            val rawBytes = readResourceBytes(resource, warnings, href)
            val plain = htmlToPlain(decodeHtml(rawBytes, warnings, href))
            if (plain.length > 30) {
                builder.append("## $label\n\n")
                builder.append(plain)
                builder.append("\n\n")
            }
        }

        book.spine.spineReferences.forEachIndexed { index, ref ->
            val resource = ref.resource ?: return@forEachIndexed
            appendResource(resource, resource.href ?: "capitolo-$index")
        }

        if (builder.isEmpty()) {
            appendFromToc(book.tableOfContents.tocReferences, builder, warnings, seen)
        }

        if (builder.isEmpty()) {
            for (resource in book.resources.getAll()) {
                if (isTextResource(resource)) {
                    appendResource(resource, resource.href ?: "sezione")
                }
            }
        }

        if (builder.isEmpty()) {
            warnings.add(
                "EPUB: nessun capitolo testuale estratto. Il file potrebbe essere protetto (DRM) o solo immagini."
            )
        }

        return title to builder.toString()
    }

    private fun isTextResource(resource: Resource): Boolean {
        val mt = resource.mediaType?.name?.lowercase().orEmpty()
        val href = resource.href?.lowercase().orEmpty()
        return mt.contains("html") || mt.contains("xml") ||
            href.endsWith(".xhtml") || href.endsWith(".html") || href.endsWith(".htm")
    }

    private fun appendFromToc(
        refs: List<TOCReference>,
        builder: StringBuilder,
        warnings: MutableList<String>,
        seen: MutableSet<String>
    ) {
        for (toc in refs) {
            val res = toc.resource
            if (res != null) {
                val label = toc.title ?: res.href ?: "sezione"
                val key = (res.href ?: label).lowercase()
                if (key !in seen) {
                    seen.add(key)
                    val bytes = readResourceBytes(res, warnings, label)
                    val plain = htmlToPlain(decodeHtml(bytes, warnings, label))
                    if (plain.length > 30) {
                        builder.append("## $label\n\n$plain\n\n")
                    }
                }
            }
            if (toc.children.isNotEmpty()) {
                appendFromToc(toc.children, builder, warnings, seen)
            }
        }
    }

    private fun readResourceBytes(
        resource: Resource,
        warnings: MutableList<String>,
        label: String
    ): ByteArray {
        return try {
            resource.data ?: ByteArray(0)
        } catch (e: Exception) {
            warnings.add("$label: impossibile leggere risorsa (${e.message})")
            ByteArray(0)
        }
    }

    private fun decodeHtml(bytes: ByteArray, warnings: MutableList<String>, label: String): String {
        if (bytes.isEmpty()) return ""
        TextNormalizer.detectEncodingLabel(bytes)?.let { warnings.add("$label: $it") }
        val charsets = listOf(
            Charsets.UTF_8,
            Charset.forName("ISO-8859-1"),
            Charset.forName("windows-1252")
        )
        var best = ""
        var bestScore = -1
        for (cs in charsets) {
            runCatching {
                val decoded = String(bytes, cs)
                val score = decoded.count { it.isLetterOrDigit() || it.isWhitespace() }
                if (score > bestScore) {
                    bestScore = score
                    best = decoded
                }
            }
        }
        return best
    }

    private fun htmlToPlain(html: String): String {
        return html
            .replace(Regex("(?is)<script.*?>.*?</script>"), "")
            .replace(Regex("(?is)<style.*?>.*?</style>"), "")
            .replace(Regex("(?is)<br\\s*/?>"), "\n")
            .replace(Regex("(?is)</p>"), "\n")
            .replace(Regex("(?is)</h[1-6]>"), "\n\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&#\\d+;"), " ")
    }
}
