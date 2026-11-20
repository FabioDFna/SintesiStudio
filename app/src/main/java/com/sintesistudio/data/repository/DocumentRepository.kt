package com.sintesistudio.data.repository

import android.net.Uri
import com.sintesistudio.ai.GeminiService
import com.sintesistudio.data.SettingsStore
import com.sintesistudio.data.dao.DocumentDao
import com.sintesistudio.data.entity.ChatMessageEntity
import com.sintesistudio.data.entity.DocumentEntity
import com.sintesistudio.parser.DocumentParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DocumentRepository(
    private val dao: DocumentDao,
    private val parser: DocumentParser,
    private val gemini: GeminiService,
    private val settings: SettingsStore
) {
    fun observeDocuments(): Flow<List<DocumentEntity>> = dao.observeAll()

    fun observeMessages(documentId: Long): Flow<List<ChatMessageEntity>> =
        dao.observeMessages(documentId)

    suspend fun importDocument(uri: Uri): Long {
        val parsed = parser.parse(uri)
        val warnings = parsed.warnings.takeIf { it.isNotEmpty() }?.joinToString("\n")
        val entity = DocumentEntity(
            title = parsed.title,
            format = parsed.format,
            fullText = parsed.text,
            charCount = parsed.text.length,
            parseWarnings = warnings,
            summary = null,
            audioScript = null
        )
        return dao.insert(entity)
    }

    suspend fun getDocument(id: Long): DocumentEntity? = dao.getById(id)

    suspend fun deleteDocument(id: Long) = dao.delete(id)

    private suspend fun requireApiKey(): String {
        val key = settings.geminiApiKey.first()?.takeIf { it.isNotBlank() }
        return key ?: throw IllegalStateException(
            "Inserisci la API key Gemini in Impostazioni (aistudio.google.com/apikey)."
        )
    }

    private suspend fun preferredModel(): String = settings.geminiModel.first()

    suspend fun generateSummary(documentId: Long): DocumentEntity {
        val doc = dao.getById(documentId) ?: throw IllegalStateException("Documento non trovato")
        val apiKey = requireApiKey()
        val summary = gemini.buildFullSummary(apiKey, doc.fullText, preferredModel())
        val updated = doc.copy(summary = summary)
        dao.update(updated)
        return updated
    }

    suspend fun generateAudioScript(documentId: Long): DocumentEntity {
        val doc = dao.getById(documentId) ?: throw IllegalStateException("Documento non trovato")
        val apiKey = requireApiKey()
        val script = gemini.buildAudioScript(
            apiKey = apiKey,
            title = doc.title,
            summary = doc.summary.orEmpty(),
            fullText = doc.fullText,
            preferredModel = preferredModel()
        )
        val updated = doc.copy(audioScript = script)
        dao.update(updated)
        return updated
    }

    suspend fun ask(documentId: Long, question: String): String {
        val doc = dao.getById(documentId) ?: throw IllegalStateException("Documento non trovato")
        val apiKey = requireApiKey()
        dao.insertMessage(ChatMessageEntity(documentId = documentId, role = "user", content = question))
        val answer = gemini.answerQuestion(apiKey, question, doc.fullText, preferredModel())
        dao.insertMessage(ChatMessageEntity(documentId = documentId, role = "assistant", content = answer))
        return answer
    }
}
