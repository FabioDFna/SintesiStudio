package com.sintesistudio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sintesistudio.data.dao.DocumentDao
import com.sintesistudio.data.entity.ChatMessageEntity
import com.sintesistudio.data.entity.DocumentEntity

@Database(
    entities = [DocumentEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "sintesi_studio.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
