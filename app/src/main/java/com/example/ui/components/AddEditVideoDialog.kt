package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Folder
import com.example.data.model.VideoBookmark
import com.example.ui.viewmodel.BookmarkViewModel
import com.example.util.YouTubeUtil

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditVideoDialog(
    viewModel: BookmarkViewModel,
    existingBookmark: VideoBookmark?,
    prefillUrl: String?,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onSave: (
        videoId: String,
        originalUrl: String,
        title: String,
        channelName: String,
        thumbnailUrl: String,
        startSeconds: Int,
        folderId: Long?,
        note: String
    ) -> Unit
) {
    val context = LocalContext.current

    var urlInput by remember {
        mutableStateOf(existingBookmark?.originalUrl ?: prefillUrl ?: "")
    }
    var videoId by remember {
        mutableStateOf(existingBookmark?.videoId ?: "")
    }
    var titleInput by remember {
        mutableStateOf(existingBookmark?.title ?: "")
    }
    var channelInput by remember {
        mutableStateOf(existingBookmark?.channelName ?: "")
    }
    var thumbnailUrl by remember {
        mutableStateOf(existingBookmark?.thumbnailUrl ?: "")
    }
    var noteInput by remember {
        mutableStateOf(existingBookmark?.note ?: "")
    }
    var selectedFolderId by remember {
        mutableStateOf(existingBookmark?.folderId)
    }

    // Time representation
    var totalSeconds by remember {
        mutableIntStateOf(existingBookmark?.startSeconds ?: 0)
    }

    var hoursText by remember {
        mutableStateOf(if (totalSeconds >= 3600) (totalSeconds / 3600).toString() else "0")
    }
    var minutesText by remember {
        mutableStateOf(((totalSeconds % 3600) / 60).toString())
    }
    var secondsText by remember {
        mutableStateOf((totalSeconds % 60).toString())
    }

    // Update total seconds helper
    fun updateFromFields(h: String, m: String, s: String) {
        val hVal = h.toIntOrNull() ?: 0
        val mVal = m.toIntOrNull() ?: 0
        val sVal = s.toIntOrNull() ?: 0
        totalSeconds = (hVal * 3600) + (mVal * 60) + sVal
    }

    fun setSecondsDirectly(sec: Int) {
        val safeSec = sec.coerceAtLeast(0)
        totalSeconds = safeSec
        hoursText = (safeSec / 3600).toString()
        minutesText = ((safeSec % 3600) / 60).toString()
        secondsText = (safeSec % 60).toString()
    }

    // Auto-parse URL when changed
    LaunchedEffect(urlInput) {
        if (urlInput.isNotBlank()) {
            val parsed = YouTubeUtil.parseUrl(urlInput)
            if (parsed.videoId != null) {
                videoId = parsed.videoId
                if (thumbnailUrl.isBlank()) {
                    thumbnailUrl = YouTubeUtil.getThumbnailUrl(parsed.videoId)
                }
                // If URL had embedded timestamp and user hasn't explicitly set one
                if (parsed.extractedSeconds > 0 && totalSeconds == 0 && existingBookmark == null) {
                    setSecondsDirectly(parsed.extractedSeconds)
                }
                // Fetch oEmbed title & metadata if title is empty
                if (titleInput.isBlank() || titleInput.startsWith("فيديو")) {
                    viewModel.fetchVideoInfo(parsed.videoId)
                }
            }
        }
    }

    // React to fetched metadata
    val fetchedMeta by viewModel.fetchedMetadata.collectAsStateWithLifecycle(null)
    val isFetching by viewModel.isFetchingMetadata.collectAsStateWithLifecycle(false)

    LaunchedEffect(fetchedMeta) {
        fetchedMeta?.let { meta ->
            if (titleInput.isBlank() || titleInput.startsWith("فيديو")) {
                titleInput = meta.title
            }
            if (channelInput.isBlank()) {
                channelInput = meta.channelName
            }
            if (thumbnailUrl.isBlank() || thumbnailUrl == YouTubeUtil.getThumbnailUrl(videoId)) {
                thumbnailUrl = meta.thumbnailUrl
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("add_edit_video_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingBookmark != null) "تعديل الفيديو والتوقيت" else "حفظ فيديو مع توقيت مخصص",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // URL Input Field + Paste Button
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("رابط فيديو يوتيوب") },
                    placeholder = { Text("https://youtu.be/... أو youtube.com/watch?v=...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_input_field"),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (urlInput.isNotEmpty()) {
                                IconButton(onClick = { urlInput = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح")
                                }
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        if (text.isNotBlank()) {
                                            urlInput = text
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("paste_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "لصق من الحافظة",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )

                // Video Preview Thumbnail & Title
                if (videoId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 96.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(thumbnailUrl.ifBlank { YouTubeUtil.getThumbnailUrl(videoId) })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (isFetching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.Center),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (titleInput.isNotBlank()) titleInput else "جاري جلب العنوان...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2
                                )
                                if (channelInput.isNotBlank()) {
                                    Text(
                                        text = channelInput,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.fetchVideoInfo(videoId) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "تحديث البيانات",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("عنوان الفيديو في التطبيق") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_input_field")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Timestamp Picker Section
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "توقيت البدء المخصص",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "سيبدأ تشغيل الفيديو في تطبيق يوتيوب عند هذه اللحظة بالذات:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Hours : Minutes : Seconds Fields
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hours
                            TimeNumberInput(
                                label = "ساعة",
                                value = hoursText,
                                onValueChange = {
                                    hoursText = it.filter { ch -> ch.isDigit() }.take(2)
                                    updateFromFields(hoursText, minutesText, secondsText)
                                },
                                testTag = "time_hours_input"
                            )

                            Text(
                                text = ":",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // Minutes
                            TimeNumberInput(
                                label = "دقيقة",
                                value = minutesText,
                                onValueChange = {
                                    minutesText = it.filter { ch -> ch.isDigit() }.take(2)
                                    updateFromFields(hoursText, minutesText, secondsText)
                                },
                                testTag = "time_minutes_input"
                            )

                            Text(
                                text = ":",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // Seconds
                            TimeNumberInput(
                                label = "ثانية",
                                value = secondsText,
                                onValueChange = {
                                    secondsText = it.filter { ch -> ch.isDigit() }.take(2)
                                    updateFromFields(hoursText, minutesText, secondsText)
                                },
                                testTag = "time_seconds_input"
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stepper Quick Buttons
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { setSecondsDirectly(totalSeconds + 15) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+15 ثانية", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { setSecondsDirectly(totalSeconds + 60) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+1 دقيقة", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { setSecondsDirectly(totalSeconds + 300) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+5 دقائق", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { setSecondsDirectly(0) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("البداية (00:00)", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Test playback right now button
                        if (videoId.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    YouTubeUtil.openVideoInYouTube(context, videoId, totalSeconds)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تجربة التشغيل في يوتيوب الآن (${YouTubeUtil.formatSeconds(totalSeconds)})")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Folder Selection
                Text(
                    text = "التنظيم في مجلد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // None / Uncategorized Chip
                    val isNoneSelected = selectedFolderId == null
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isNoneSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isNoneSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .clickable { selectedFolderId = null }
                            .testTag("folder_select_none")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.FolderSpecial, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("بدون مجلد", style = MaterialTheme.typography.labelMedium)
                            if (isNoneSelected) {
                                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Existing folders
                    folders.forEach { folder ->
                        val isSelected = selectedFolderId == folder.id
                        val color = try {
                            Color(android.graphics.Color.parseColor(folder.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
                            modifier = Modifier
                                .clickable { selectedFolderId = folder.id }
                                .testTag("folder_select_${folder.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Text(folder.name, style = MaterialTheme.typography.labelMedium)
                                if (isSelected) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notes / Description Input
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("ملاحظة سريعة أو سبب التوقيت (اختياري)") },
                    placeholder = { Text("مثال: بداية الشرح المهم أو الدقيقة الذهبية") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_input_field")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء")
                    }

                    val isSaveEnabled = videoId.isNotBlank() && titleInput.isNotBlank()
                    Button(
                        onClick = {
                            if (isSaveEnabled) {
                                onSave(
                                    videoId,
                                    urlInput.ifBlank { "https://www.youtube.com/watch?v=$videoId" },
                                    titleInput,
                                    channelInput,
                                    thumbnailUrl.ifBlank { YouTubeUtil.getThumbnailUrl(videoId) },
                                    totalSeconds,
                                    selectedFolderId,
                                    noteInput
                                )
                            }
                        },
                        enabled = isSaveEnabled,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_video_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (existingBookmark != null) "تحديث الفيديو" else "حفظ الفيديو",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeNumberInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag(testTag)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
