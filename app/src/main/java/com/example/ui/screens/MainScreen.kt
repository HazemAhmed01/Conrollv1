package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FirestoreFolder
import com.example.data.model.FirestoreVideoBookmark
import com.example.ui.components.AddEditVideoDialog
import com.example.ui.components.FolderChipRow
import com.example.ui.components.FolderDialog
import com.example.ui.components.VideoCard
import com.example.ui.theme.YouTubeRed
import com.example.ui.viewmodel.BookmarkViewModel
import com.example.ui.viewmodel.FolderFilter
import com.example.ui.viewmodel.SortOption
import com.example.util.YouTubeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: BookmarkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val currentFilter by viewModel.filter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentSort by viewModel.sortOption.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val showAddEditDialog by viewModel.showAddEditDialog.collectAsStateWithLifecycle()
    val editingBookmark by viewModel.editingBookmark.collectAsStateWithLifecycle()
    val prefillUrl by viewModel.prefilledUrl.collectAsStateWithLifecycle()

    val showFolderDialog by viewModel.showFolderDialog.collectAsStateWithLifecycle()
    val editingFolder by viewModel.editingFolder.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var showUserMenu by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    var bookmarkToDelete by remember { mutableStateOf<FirestoreVideoBookmark?>(null) }
    var folderToDelete by remember { mutableStateOf<FirestoreFolder?>(null) }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(YouTubeRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Tubemark",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "مزامنة سحابية نشطة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Search button
                        IconButton(
                            onClick = { isSearchExpanded = !isSearchExpanded },
                            modifier = Modifier.testTag("search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = "بحث"
                            )
                        }

                        // Sort menu button
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "ترتيب"
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("الأحدث أولاً") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption.NEWEST)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("الأقدم أولاً") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption.OLDEST)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("أبجدياً (العنوان)") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption.TITLE)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("حسب التوقيت") },
                                    onClick = {
                                        viewModel.setSortOption(SortOption.TIMESTAMP)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }

                        // User profile / Account menu button
                        Box {
                            IconButton(
                                onClick = { showUserMenu = true },
                                modifier = Modifier.testTag("user_profile_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "الحساب",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            DropdownMenu(
                                expanded = showUserMenu,
                                onDismissRequest = { showUserMenu = false }
                            ) {
                                currentUser?.let { user ->
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                        Text(
                                            text = user.displayName ?: "مستخدم Tubemark",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = user.email ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    HorizontalDivider()
                                }

                                DropdownMenuItem(
                                    text = { Text("تسجيل الخروج", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showUserMenu = false
                                        viewModel.signOut(context, credentialManager)
                                    },
                                    modifier = Modifier.testTag("sign_out_menu_item")
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                if (isSearchExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("ابحث بالعنوان، اسم القناة أو الملاحظة...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                FolderChipRow(
                    folders = folders,
                    currentFilter = currentFilter,
                    totalVideosCount = bookmarks.size,
                    favoritesCount = bookmarks.count { it.isFavorite },
                    onSelectFilter = { viewModel.setFilter(it) },
                    onNewFolderClick = { viewModel.openCreateFolderDialog() },
                    onEditFolderClick = { viewModel.openEditFolderDialog(it) }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة فيديو", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_video_fab")
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (bookmarks.isEmpty()) {
                EmptyStateView(
                    isSearch = searchQuery.isNotBlank(),
                    onAddClick = { viewModel.openAddDialog() },
                    onClearSearch = { viewModel.setSearchQuery("") }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = bookmarks,
                        key = { it.id }
                    ) { bookmark ->
                        val folder = folders.find { it.id == bookmark.folderId }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 640.dp)
                                .align(Alignment.Center)
                        ) {
                            VideoCard(
                                bookmark = bookmark,
                                folder = folder,
                                onOpenInYouTube = {
                                    YouTubeUtil.openVideoInYouTube(context, it.youtubeVideoId, it.startSeconds)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onEdit = { viewModel.openEditDialog(it) },
                                onDelete = { bookmarkToDelete = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditVideoDialog(
            viewModel = viewModel,
            existingBookmark = editingBookmark,
            prefillUrl = prefillUrl,
            folders = folders,
            onDismiss = { viewModel.dismissAddEditDialog() },
            onSave = { vId, url, title, channel, thumb, sec, fId, note ->
                viewModel.saveBookmark(vId, url, title, channel, thumb, sec, fId, note)
            }
        )
    }

    if (showFolderDialog) {
        FolderDialog(
            existingFolder = editingFolder,
            onDismiss = { viewModel.dismissFolderDialog() },
            onSave = { name, colorHex, icon ->
                viewModel.saveFolder(name, colorHex, icon)
            },
            onDelete = { folder ->
                folderToDelete = folder
                viewModel.dismissFolderDialog()
            }
        )
    }

    bookmarkToDelete?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { bookmarkToDelete = null },
            title = { Text("حذف الفيديو؟") },
            text = { Text("هل أنت متأكد من حذف \"${bookmark.title}\" من سحابتك وقائمتك؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBookmark(bookmark)
                        bookmarkToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookmarkToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("حذف المجلد؟") },
            text = { Text("هل تريد حذف المجلد \"${folder.name}\" من السحابة؟ ستبقى مقاطعك محفوظة في تصنيف \"بدون مجلد\".") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFolder(folder)
                        folderToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("حذف المجلد")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun EmptyStateView(
    isSearch: Boolean,
    onAddClick: () -> Unit,
    onClearSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearch) Icons.Default.Search else Icons.Outlined.VideoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSearch) "لا توجد نتائج مطابقة لبحثك" else "لا توجد مقاطع محفوظة هنا بعد",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSearch) {
                "جرّب البحث بكلمة أخرى أو مسح البحث."
            } else {
                "احفظ أي فيديو من يوتيوب مع تحديد التوقيت المفضل بدقة، وستتم مزامنته تلقائياً بين جميع أجهزتك!"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isSearch) {
            TextButton(onClick = onClearSearch) {
                Text("مسح البحث")
            }
        } else {
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة أول فيديو")
            }
        }
    }
}
