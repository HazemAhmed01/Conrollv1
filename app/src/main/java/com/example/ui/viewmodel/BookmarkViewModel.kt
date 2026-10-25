package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Folder
import com.example.data.model.VideoBookmark
import com.example.data.repository.BookmarkRepository
import com.example.util.VideoMetadata
import com.example.util.YouTubeUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class FolderFilter {
    object All : FolderFilter()
    object Favorites : FolderFilter()
    object Uncategorized : FolderFilter()
    data class ByFolder(val folderId: Long) : FolderFilter()
}

enum class SortOption {
    NEWEST,
    OLDEST,
    TITLE,
    TIMESTAMP
}

data class UiState(
    val currentFilter: FolderFilter = FolderFilter.All,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.NEWEST,
    val isFetchingMetadata: Boolean = false,
    val lastFetchedMetadata: VideoMetadata? = null,
    val userMessage: String? = null
)

class BookmarkViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BookmarkRepository

    init {
        val database = AppDatabase.getInstance(application)
        repository = BookmarkRepository(
            folderDao = database.folderDao(),
            videoBookmarkDao = database.videoBookmarkDao()
        )
    }

    val folders: StateFlow<List<Folder>> = repository.folders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _filter = MutableStateFlow<FolderFilter>(FolderFilter.All)
    val filter: StateFlow<FolderFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isFetchingMetadata = MutableStateFlow(false)
    val isFetchingMetadata: StateFlow<Boolean> = _isFetchingMetadata.asStateFlow()

    private val _fetchedMetadata = MutableStateFlow<VideoMetadata?>(null)
    val fetchedMetadata: StateFlow<VideoMetadata?> = _fetchedMetadata.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Dialog & sheet controllers
    private val _showAddEditDialog = MutableStateFlow(false)
    val showAddEditDialog: StateFlow<Boolean> = _showAddEditDialog.asStateFlow()

    private val _editingBookmark = MutableStateFlow<VideoBookmark?>(null)
    val editingBookmark: StateFlow<VideoBookmark?> = _editingBookmark.asStateFlow()

    private val _prefilledUrl = MutableStateFlow<String?>(null)
    val prefilledUrl: StateFlow<String?> = _prefilledUrl.asStateFlow()

    private val _showFolderDialog = MutableStateFlow(false)
    val showFolderDialog: StateFlow<Boolean> = _showFolderDialog.asStateFlow()

    private val _editingFolder = MutableStateFlow<Folder?>(null)
    val editingFolder: StateFlow<Folder?> = _editingFolder.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookmarks: StateFlow<List<VideoBookmark>> = combine(
        _filter,
        _searchQuery,
        _sortOption
    ) { filter, query, sort ->
        Triple(filter, query, sort)
    }.flatMapLatest { (filter, query, sort) ->
        val baseFlow = if (query.isNotBlank()) {
            repository.searchBookmarks(query.trim())
        } else {
            when (filter) {
                is FolderFilter.All -> repository.allBookmarks
                is FolderFilter.Favorites -> repository.favoriteBookmarks
                is FolderFilter.Uncategorized -> repository.uncategorizedBookmarks
                is FolderFilter.ByFolder -> repository.getBookmarksByFolder(filter.folderId)
            }
        }

        combine(baseFlow) { bookmarkLists ->
            val list = bookmarkLists[0]
            when (sort) {
                SortOption.NEWEST -> list.sortedByDescending { it.createdAt }
                SortOption.OLDEST -> list.sortedBy { it.createdAt }
                SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
                SortOption.TIMESTAMP -> list.sortedBy { it.startSeconds }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: FolderFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sortOption: SortOption) {
        _sortOption.value = sortOption
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun openAddDialog(prefillUrl: String? = null) {
        _editingBookmark.value = null
        _prefilledUrl.value = prefillUrl
        _fetchedMetadata.value = null
        _showAddEditDialog.value = true
    }

    fun openEditDialog(bookmark: VideoBookmark) {
        _editingBookmark.value = bookmark
        _prefilledUrl.value = null
        _fetchedMetadata.value = null
        _showAddEditDialog.value = true
    }

    fun dismissAddEditDialog() {
        _showAddEditDialog.value = false
        _editingBookmark.value = null
        _prefilledUrl.value = null
        _fetchedMetadata.value = null
    }

    fun openCreateFolderDialog() {
        _editingFolder.value = null
        _showFolderDialog.value = true
    }

    fun openEditFolderDialog(folder: Folder) {
        _editingFolder.value = folder
        _showFolderDialog.value = true
    }

    fun dismissFolderDialog() {
        _showFolderDialog.value = false
        _editingFolder.value = null
    }

    fun fetchVideoInfo(videoId: String) {
        viewModelScope.launch {
            _isFetchingMetadata.value = true
            try {
                val metadata = repository.fetchMetadata(videoId)
                _fetchedMetadata.value = metadata
            } catch (e: Exception) {
                // Keep default
            } finally {
                _isFetchingMetadata.value = false
            }
        }
    }

    fun saveBookmark(
        videoId: String,
        originalUrl: String,
        title: String,
        channelName: String,
        thumbnailUrl: String,
        startSeconds: Int,
        folderId: Long?,
        note: String
    ) {
        viewModelScope.launch {
            val existing = _editingBookmark.value
            if (existing != null) {
                val updated = existing.copy(
                    videoId = videoId,
                    originalUrl = originalUrl,
                    title = title,
                    channelName = channelName,
                    thumbnailUrl = thumbnailUrl,
                    startSeconds = startSeconds,
                    folderId = folderId,
                    note = note
                )
                repository.updateBookmark(updated)
                _userMessage.value = "تم تحديث الفيديو بنجاح"
            } else {
                val newBookmark = VideoBookmark(
                    videoId = videoId,
                    originalUrl = originalUrl,
                    title = title,
                    channelName = channelName,
                    thumbnailUrl = thumbnailUrl,
                    startSeconds = startSeconds,
                    folderId = folderId,
                    note = note,
                    isFavorite = false
                )
                repository.insertBookmark(newBookmark)
                _userMessage.value = "تم حفظ الفيديو بنجاح"
            }
            dismissAddEditDialog()
        }
    }

    fun toggleFavorite(bookmark: VideoBookmark) {
        viewModelScope.launch {
            repository.toggleFavorite(bookmark.id, !bookmark.isFavorite)
        }
    }

    fun deleteBookmark(bookmark: VideoBookmark) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
            _userMessage.value = "تم حذف الفيديو"
        }
    }

    fun moveBookmarkToFolder(bookmark: VideoBookmark, newFolderId: Long?) {
        viewModelScope.launch {
            repository.updateBookmarkFolder(bookmark.id, newFolderId)
        }
    }

    fun saveFolder(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val existing = _editingFolder.value
            if (existing != null) {
                repository.updateFolder(
                    existing.copy(
                        name = name.trim(),
                        colorHex = colorHex,
                        iconName = iconName
                    )
                )
                _userMessage.value = "تم تحديث المجلد"
            } else {
                repository.insertFolder(
                    Folder(
                        name = name.trim(),
                        colorHex = colorHex,
                        iconName = iconName
                    )
                )
                _userMessage.value = "تم إنشاء المجلد بنجاح"
            }
            dismissFolderDialog()
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            if (_filter.value is FolderFilter.ByFolder && (_filter.value as FolderFilter.ByFolder).folderId == folder.id) {
                _filter.value = FolderFilter.All
            }
            _userMessage.value = "تم حذف المجلد"
        }
    }
}
