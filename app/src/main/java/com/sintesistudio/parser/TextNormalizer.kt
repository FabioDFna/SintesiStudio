package com.sintesistudio.parser

object TextNormalizer {
    private val replacementChars = Regex("[\uFFFD\u0000-\u0008\u000B\u000C\u000E-\u001F]")

    fun normalize(raw: String, warnings: MutableList<String>): String {
        var text = raw.replace('\u00A0', ' ')
        val badCount = text.count { it == '\uFFFD' }
        if (badCount > 0) {
            warnings.add("Rilevati $badCount caratteri non decodificabili; il testo potrebbe essere incompleto.")
        }
        text = text.replace(replacementChars, " ")
        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        return text.trim()
    }

    fun detectEncodingLabel(bytes: ByteArray): String? {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return "UTF-8 (BOM)"
        }
        return null
    }
}
