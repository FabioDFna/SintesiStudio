package com.sintesistudio.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val format: String,
    val fullText: String,
    val charCount: Int,
    val parseWarnings: String?,
    val summary: String?,
    val audioScript: String?,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
