package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VideoBookmark
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoBookmarkDao {
    @Query("SELECT * FROM video_bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<VideoBookmark>>

    @Query("SELECT * FROM video_bookmarks WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getBookmarksByFolder(folderId: Long): Flow<List<VideoBookmark>>

    @Query("SELECT * FROM video_bookmarks WHERE folderId IS NULL ORDER BY createdAt DESC")
    fun getUncategorizedBookmarks(): Flow<List<VideoBookmark>>

    @Query("SELECT * FROM video_bookmarks WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteBookmarks(): Flow<List<VideoBookmark>>

    @Query("""
        SELECT * FROM video_bookmarks 
        WHERE title LIKE '%' || :query || '%' 
           OR channelName LIKE '%' || :query || '%' 
           OR note LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchBookmarks(query: String): Flow<List<VideoBookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: VideoBookmark): Long

    @Update
    suspend fun updateBookmark(bookmark: VideoBookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: VideoBookmark)

    @Query("UPDATE video_bookmarks SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFav: Boolean)

    @Query("UPDATE video_bookmarks SET folderId = :folderId WHERE id = :id")
    suspend fun updateFolder(id: Long, folderId: Long?)

    @Query("SELECT COUNT(*) FROM video_bookmarks WHERE folderId = :folderId")
    suspend fun getCountInFolder(folderId: Long): Int
}
