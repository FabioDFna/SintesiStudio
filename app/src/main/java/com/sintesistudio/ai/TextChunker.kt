package com.sintesistudio.ai

object TextChunker {
    private const val CHUNK_SIZE = 120_000
    private const val OVERLAP = 2_000

    fun chunk(text: String): List<String> {
        if (text.length <= CHUNK_SIZE) return listOf(text)
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = (start + CHUNK_SIZE).coerceAtMost(text.length)
            if (end < text.length) {
                val breakAt = text.lastIndexOf("\n\n", end).takeIf { it > start + CHUNK_SIZE / 2 }
                    ?: text.lastIndexOf('.', end).takeIf { it > start + CHUNK_SIZE / 2 }
                    ?: end
                end = breakAt
            }
            chunks.add(text.substring(start, end))
            if (end >= text.length) break
            start = (end - OVERLAP).coerceAtLeast(start + 1)
        }
        return chunks
    }

    fun excerptForQuestion(fullText: String, question: String, maxLen: Int = 80_000): String {
        val keywords = question.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 4 }
            .take(12)
        if (keywords.isEmpty()) {
            return fullText.take(maxLen)
        }
        val paragraphs = fullText.split(Regex("\n\n+"))
        val scored = paragraphs.map { p ->
            val lower = p.lowercase()
            val score = keywords.count { lower.contains(it) }
            p to score
        }
        val top = scored
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(40)
            .map { it.first }
        val selected = if (top.isEmpty()) fullText.take(maxLen) else top.joinToString("\n\n")
        return if (selected.length > maxLen) selected.take(maxLen) else selected
    }
}
