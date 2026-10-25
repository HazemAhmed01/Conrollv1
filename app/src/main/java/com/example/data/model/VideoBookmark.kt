package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "video_bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class VideoBookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val originalUrl: String,
    val title: String,
    val channelName: String = "",
    val thumbnailUrl: String,
    val startSeconds: Int = 0,
    val folderId: Long? = null,
    val note: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
