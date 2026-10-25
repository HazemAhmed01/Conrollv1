package com.example.data.repository

import com.example.data.local.FolderDao
import com.example.data.local.VideoBookmarkDao
import com.example.data.model.Folder
import com.example.data.model.VideoBookmark
import com.example.util.VideoMetadata
import com.example.util.YouTubeUtil
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(
    private val folderDao: FolderDao,
    private val videoBookmarkDao: VideoBookmarkDao
) {
    val folders: Flow<List<Folder>> = folderDao.getAllFolders()
    val allBookmarks: Flow<List<VideoBookmark>> = videoBookmarkDao.getAllBookmarks()
    val uncategorizedBookmarks: Flow<List<VideoBookmark>> = videoBookmarkDao.getUncategorizedBookmarks()
    val favoriteBookmarks: Flow<List<VideoBookmark>> = videoBookmarkDao.getFavoriteBookmarks()

    fun getBookmarksByFolder(folderId: Long): Flow<List<VideoBookmark>> {
        return videoBookmarkDao.getBookmarksByFolder(folderId)
    }

    fun searchBookmarks(query: String): Flow<List<VideoBookmark>> {
        return videoBookmarkDao.searchBookmarks(query)
    }

    suspend fun insertBookmark(bookmark: VideoBookmark): Long {
        return videoBookmarkDao.insertBookmark(bookmark)
    }

    suspend fun updateBookmark(bookmark: VideoBookmark) {
        videoBookmarkDao.updateBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: VideoBookmark) {
        videoBookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun toggleFavorite(id: Long, isFav: Boolean) {
        videoBookmarkDao.updateFavorite(id, isFav)
    }

    suspend fun updateBookmarkFolder(id: Long, folderId: Long?) {
        videoBookmarkDao.updateFolder(id, folderId)
    }

    suspend fun insertFolder(folder: Folder): Long {
        return folderDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: Folder) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: Folder) {
        folderDao.deleteFolder(folder)
    }

    suspend fun fetchMetadata(videoId: String): VideoMetadata {
        return YouTubeUtil.fetchMetadata(videoId)
    }
}
