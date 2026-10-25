package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Folder
import com.example.data.model.VideoBookmark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Folder::class, VideoBookmark::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun videoBookmarkDao(): VideoBookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tubemark_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val folderDao = getInstance(context).folderDao()
                                val videoDao = getInstance(context).videoBookmarkDao()
                                
                                val f1 = folderDao.insertFolder(
                                    Folder(name = "دورات وتعليم", colorHex = "#1E88E5", iconName = "school")
                                )
                                val f2 = folderDao.insertFolder(
                                    Folder(name = "بودكاست ومقابلات", colorHex = "#FB8C00", iconName = "mic")
                                )
                                val f3 = folderDao.insertFolder(
                                    Folder(name = "برمجة وتطوير", colorHex = "#43A047", iconName = "code")
                                )

                                // Add a helpful sample bookmark demonstrating timestamp feature
                                videoDao.insertBookmark(
                                    VideoBookmark(
                                        videoId = "dQw4w9WgXcQ",
                                        originalUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=43s",
                                        title = "Rick Astley - Never Gonna Give You Up (Official Music Video)",
                                        channelName = "Rick Astley",
                                        thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
                                        startSeconds = 43,
                                        folderId = f2,
                                        note = "مقطع البداية الحماسي عند الثانية 43",
                                        isFavorite = true
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
