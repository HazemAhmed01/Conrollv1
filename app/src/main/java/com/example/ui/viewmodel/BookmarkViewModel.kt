package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.FirestoreFolder
import com.example.data.model.FirestoreVideoBookmark
import com.example.data.repository.FirestoreBookmarkRepository
import com.example.ui.auth.AuthManager
import com.example.util.VideoMetadata
import com.example.util.YouTubeUtil
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class FolderFilter {
    object All : FolderFilter()
    object Favorites : FolderFilter()
    object Uncategorized : FolderFilter()
    data class ByFolder(val folderId: String) : FolderFilter()
}

enum class SortOption {
    NEWEST,
    OLDEST,
    TITLE,
    TIMESTAMP
}

class BookmarkViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreBookmarkRepository(application)
    private val auth = Firebase.auth

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

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

    private val _editingBookmark = MutableStateFlow<FirestoreVideoBookmark?>(null)
    val editingBookmark: StateFlow<FirestoreVideoBookmark?> = _editingBookmark.asStateFlow()

    private val _prefilledUrl = MutableStateFlow<String?>(null)
    val prefilledUrl: StateFlow<String?> = _prefilledUrl.asStateFlow()

    private val _showFolderDialog = MutableStateFlow(false)
    val showFolderDialog: StateFlow<Boolean> = _showFolderDialog.asStateFlow()

    private val _editingFolder = MutableStateFlow<FirestoreFolder?>(null)
    val editingFolder: StateFlow<FirestoreFolder?> = _editingFolder.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                viewModelScope.launch {
                    try {
                        firestoreRepository.seedStarterFoldersIfEmpty()
                    } catch (e: Exception) {
                        // ignore or log
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val folders: StateFlow<List<FirestoreFolder>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            firestoreRepository.observeFolders().catch { e ->
                _userMessage.value = "خطأ في مزامنة المجلدات: ${e.localizedMessage}"
                emit(emptyList())
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookmarks: StateFlow<List<FirestoreVideoBookmark>> = _currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            combine(
                firestoreRepository.observeVideos().catch { e ->
                    _userMessage.value = "خطأ في مزامنة المقاطع: ${e.localizedMessage}"
                    emit(emptyList())
                },
                _filter,
                _searchQuery,
                _sortOption
            ) { allVideos, filter, query, sort ->
                var list = allVideos

                // Filter by folder / category
                list = when (filter) {
                    is FolderFilter.All -> list
                    is FolderFilter.Favorites -> list.filter { it.isFavorite }
                    is FolderFilter.Uncategorized -> list.filter { it.folderId.isNullOrBlank() }
                    is FolderFilter.ByFolder -> list.filter { it.folderId == filter.folderId }
                }

                // Filter by search query
                if (query.isNotBlank()) {
                    val q = query.trim().lowercase()
                    list = list.filter {
                        it.title.lowercase().contains(q) ||
                        it.channelName.lowercase().contains(q) ||
                        it.note.lowercase().contains(q)
                    }
                }

                // Sort
                when (sort) {
                    SortOption.NEWEST -> list.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    SortOption.OLDEST -> list.sortedBy { it.createdAt?.seconds ?: 0L }
                    SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
                    SortOption.TIMESTAMP -> list.sortedBy { it.startSeconds }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onAuthSuccess() {
        _currentUser.value = auth.currentUser
        _userMessage.value = "تم تسجيل الدخول بنجاح ومزامنة البيانات"
        viewModelScope.launch {
            try {
                firestoreRepository.seedStarterFoldersIfEmpty()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun signOut(context: Context, credentialManager: CredentialManager) {
        AuthManager.signOut(context, credentialManager, onSignOutComplete = {
            _currentUser.value = null
            _filter.value = FolderFilter.All
            _userMessage.value = "تم تسجيل الخروج"
        }, scope = viewModelScope)
    }

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

    fun openEditDialog(bookmark: FirestoreVideoBookmark) {
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

    fun openEditFolderDialog(folder: FirestoreFolder) {
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
                val metadata = YouTubeUtil.fetchMetadata(videoId)
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
        folderId: String?,
        note: String
    ) {
        viewModelScope.launch {
            try {
                val existing = _editingBookmark.value
                firestoreRepository.saveVideo(
                    youtubeVideoId = videoId,
                    originalUrl = originalUrl,
                    title = title,
                    channelName = channelName,
                    thumbnailUrl = thumbnailUrl,
                    startSeconds = startSeconds,
                    folderId = folderId,
                    note = note,
                    existingVideoId = existing?.id
                )
                _userMessage.value = if (existing != null) "تم تحديث الفيديو بنجاح في السحابة" else "تم حفظ الفيديو ومزامنته سحابياً"
            } catch (e: Exception) {
                _userMessage.value = "فشل حفظ الفيديو: ${e.localizedMessage}"
            } finally {
                dismissAddEditDialog()
            }
        }
    }

    fun toggleFavorite(bookmark: FirestoreVideoBookmark) {
        viewModelScope.launch {
            try {
                firestoreRepository.toggleFavorite(bookmark.id, !bookmark.isFavorite)
            } catch (e: Exception) {
                _userMessage.value = "فشل تعديل المفضلة: ${e.localizedMessage}"
            }
        }
    }

    fun deleteBookmark(bookmark: FirestoreVideoBookmark) {
        viewModelScope.launch {
            try {
                firestoreRepository.deleteVideo(bookmark.id)
                _userMessage.value = "تم حذف الفيديو من السحابة"
            } catch (e: Exception) {
                _userMessage.value = "فشل حذف الفيديو: ${e.localizedMessage}"
            }
        }
    }

    fun saveFolder(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            try {
                val existing = _editingFolder.value
                firestoreRepository.saveFolder(
                    name = name.trim(),
                    colorHex = colorHex,
                    iconName = iconName,
                    existingFolderId = existing?.id
                )
                _userMessage.value = if (existing != null) "تم تحديث المجلد في السحابة" else "تم إنشاء المجلد سحابياً"
            } catch (e: Exception) {
                _userMessage.value = "فشل حفظ المجلد: ${e.localizedMessage}"
            } finally {
                dismissFolderDialog()
            }
        }
    }

    fun deleteFolder(folder: FirestoreFolder) {
        viewModelScope.launch {
            try {
                firestoreRepository.deleteFolder(folder.id)
                if (_filter.value is FolderFilter.ByFolder && (_filter.value as FolderFilter.ByFolder).folderId == folder.id) {
                    _filter.value = FolderFilter.All
                }
                _userMessage.value = "تم حذف المجلد من السحابة"
            } catch (e: Exception) {
                _userMessage.value = "فشل حذف المجلد: ${e.localizedMessage}"
            }
        }
    }
}
