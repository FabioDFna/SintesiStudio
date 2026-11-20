package com.sintesistudio.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    companion object {
        /** Modelli con quota free tier attiva (2.0-flash non è più disponibile gratis). */
        val FREE_TIER_MODELS = listOf(
            "gemini-1.5-flash",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash-lite"
        )
        const val DEFAULT_MODEL = "gemini-1.5-flash"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun generate(apiKey: String, prompt: String, preferredModel: String = DEFAULT_MODEL): String =
        withContext(Dispatchers.IO) {
            val models = buildModelChain(preferredModel)
            var lastError: Exception? = null

            for (model in models) {
                runCatching {
                    return@withContext generateWithModel(apiKey, prompt, model)
                }.onFailure { e ->
                    lastError = e as? Exception ?: Exception(e.message, e)
                    if (!isRetryableQuotaError(e.message)) throw e
                }
            }
            throw lastError ?: IllegalStateException("Nessun modello Gemini disponibile.")
        }

    private fun buildModelChain(preferred: String): List<String> {
        val chain = mutableListOf<String>()
        if (preferred.isNotBlank()) chain.add(preferred)
        for (m in FREE_TIER_MODELS) {
            if (!chain.contains(m)) chain.add(m)
        }
        return chain
    }

    private suspend fun generateWithModel(
        apiKey: String,
        prompt: String,
        model: String,
        allowRateLimitRetry: Boolean = true
    ): String {
        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("maxOutputTokens", 8192)
            })
        }

        val request = Request.Builder()
            .url(
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "$model:generateContent?key=$apiKey"
            )
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        return client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = runCatching {
                    JSONObject(raw).optJSONObject("error")?.optString("message")
                }.getOrNull()
                val friendly = friendlyError(msg, model)
                if (allowRateLimitRetry && isRetryableQuotaError(msg)) {
                    parseRetrySeconds(msg)?.let { seconds ->
                        delay((seconds * 1000).toLong().coerceAtMost(60_000))
                        return@use generateWithModel(apiKey, prompt, model, allowRateLimitRetry = false)
                    }
                }
                throw IllegalStateException(friendly)
            }
            parseText(raw)
        }
    }

    suspend fun buildFullSummary(
        apiKey: String,
        fullText: String,
        preferredModel: String = DEFAULT_MODEL
    ): String {
        val chunks = TextChunker.chunk(fullText)
        if (chunks.size == 1) {
            return generate(
                apiKey,
                Prompts.summaryPrompt(chunks[0], "documento completo", false),
                preferredModel
            )
        }
        val partials = chunks.mapIndexed { index, chunk ->
            generate(
                apiKey,
                Prompts.summaryPrompt(chunk, "parte ${index + 1} di ${chunks.size}", false),
                preferredModel
            )
        }
        val mergedInput = partials.mapIndexed { i, s -> "### Parte ${i + 1}\n$s" }.joinToString("\n\n")
        return generate(
            apiKey,
            Prompts.summaryPrompt(mergedInput, "finale", true),
            preferredModel
        )
    }

    suspend fun buildAudioScript(
        apiKey: String,
        title: String,
        summary: String,
        fullText: String,
        preferredModel: String = DEFAULT_MODEL
    ): String {
        val base = if (summary.isNotBlank()) summary else fullText.take(60_000)
        return generate(apiKey, Prompts.audioPrompt(base, title), preferredModel)
    }

    suspend fun answerQuestion(
        apiKey: String,
        question: String,
        fullText: String,
        preferredModel: String = DEFAULT_MODEL
    ): String {
        val excerpt = TextChunker.excerptForQuestion(fullText, question)
        return generate(apiKey, Prompts.qaPrompt(question, excerpt), preferredModel)
    }

    private fun parseText(responseJson: String): String {
        val root = JSONObject(responseJson)
        val candidates = root.optJSONArray("candidates")
            ?: throw IllegalStateException("Risposta vuota da Gemini.")
        val first = candidates.getJSONObject(0)
        val parts = first.getJSONObject("content").getJSONArray("parts")
        return buildString {
            for (i in 0 until parts.length()) {
                append(parts.getJSONObject(i).optString("text", ""))
            }
        }.trim().ifBlank { throw IllegalStateException("Nessun testo nella risposta.") }
    }

    private fun isRetryableQuotaError(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        val lower = message.lowercase()
        return lower.contains("quota") ||
            lower.contains("429") ||
            lower.contains("rate limit") ||
            lower.contains("resource exhausted") ||
            lower.contains("not found") ||
            lower.contains("retry in") ||
            lower.contains("limit: 0")
    }

    private fun parseRetrySeconds(message: String?): Double? {
        if (message == null) return null
        val regex = Regex("retry in ([\\d.]+)s", RegexOption.IGNORE_CASE)
        return regex.find(message)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun friendlyError(raw: String?, model: String): String {
        if (raw.isNullOrBlank()) return "Errore Gemini ($model)."
        if (isRetryableQuotaError(raw)) {
            return buildString {
                appendLine("Quota API Gemini esaurita (modello: $model).")
                appendLine()
                appendLine("Cosa puoi fare:")
                appendLine("• Impostazioni → scegli «Flash-Lite (gratuito)»")
                appendLine("• Attendi 1–2 minuti e riprova")
                appendLine("• Controlla i limiti su ai.google.dev/gemini-api/docs/rate-limits")
                appendLine("• In Google AI Studio collega la fatturazione per più quota")
                appendLine()
                appendLine("Nota: gemini-2.0-flash non ha più quota gratuita.")
            }.trim()
        }
        return raw.take(500)
    }
}
