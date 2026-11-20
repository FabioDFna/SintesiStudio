package com.sintesistudio.data

import android.content.Context
import com.sintesistudio.ai.GeminiService
import com.sintesistudio.data.repository.DocumentRepository
import com.sintesistudio.parser.DocumentParser

class AppContainer(context: Context) {
    private val db = AppDatabase.create(context)
    val settingsStore = SettingsStore(context)
    val geminiService = GeminiService()
    val documentParser = DocumentParser(context)
    val documentRepository = DocumentRepository(
        dao = db.documentDao(),
        parser = documentParser,
        gemini = geminiService,
        settings = settingsStore
    )
}
