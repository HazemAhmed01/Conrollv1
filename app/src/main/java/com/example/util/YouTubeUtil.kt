package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ParsedYouTubeInfo(
    val videoId: String?,
    val extractedSeconds: Int = 0
)

data class VideoMetadata(
    val title: String,
    val channelName: String,
    val thumbnailUrl: String
)

object YouTubeUtil {
    private const val TAG = "YouTubeUtil"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Extracts YouTube Video ID and optional timestamp from a given URL or text.
     */
    fun parseUrl(input: String): ParsedYouTubeInfo {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ParsedYouTubeInfo(null, 0)

        var videoId: String? = null
        var seconds = 0

        // Regex patterns for various YouTube URL forms
        val patterns = listOf(
            Pattern.compile("(?:v=|/v/|youtu\\.be/|/embed/|/shorts/|/live/|/e/|watch\\?feature=player_embedded&v=)([^#&?/\n\r]+)"),
            Pattern.compile("^[a-zA-Z0-9_-]{11}$") // Direct 11-char ID
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(trimmed)
            if (matcher.find()) {
                val candidate = if (matcher.groupCount() >= 1) matcher.group(1) else matcher.group()
                if (candidate != null && candidate.length == 11) {
                    videoId = candidate
                    break
                }
            }
        }

        // Parse timestamp parameter if present (t=..., time_continue=..., start=...)
        seconds = extractTimestampSeconds(trimmed)

        return ParsedYouTubeInfo(videoId, seconds)
    }

    /**
     * Extracts start time in seconds from URL parameters like ?t=1m30s, &t=90, ?start=45
     */
    fun extractTimestampSeconds(url: String): Int {
        val timeRegex = Pattern.compile("[?&](?:t|time_continue|start)=([0-9a-zA-Z]+)")
        val matcher = timeRegex.matcher(url)
        if (matcher.find()) {
            val timeValue = matcher.group(1) ?: return 0
            return parseTimeStringToSeconds(timeValue)
        }
        return 0
    }

    /**
     * Parses time strings such as:
     * - "90" or "90s" -> 90
     * - "1m30s" or "1m 30s" -> 90
     * - "1h2m3s" -> 3723
     * - "02:30" -> 150
     * - "01:02:05" -> 3725
     */
    fun parseTimeStringToSeconds(input: String): Int {
        val clean = input.trim().lowercase()
        if (clean.isEmpty()) return 0

        // Case 1: Standard colons hh:mm:ss or mm:ss
        if (clean.contains(":")) {
            val parts = clean.split(":").mapNotNull { it.trim().toIntOrNull() }
            return when (parts.size) {
                2 -> (parts[0] * 60) + parts[1]
                3 -> (parts[0] * 3600) + (parts[1] * 60) + parts[2]
                else -> 0
            }
        }

        // Case 2: Compound like "1h20m15s" or "1h 20m"
        var total = 0
        var foundAny = false

        val hMatcher = Pattern.compile("(\\d+)\\s*h").matcher(clean)
        if (hMatcher.find()) {
            total += (hMatcher.group(1)?.toIntOrNull() ?: 0) * 3600
            foundAny = true
        }

        val mMatcher = Pattern.compile("(\\d+)\\s*m").matcher(clean)
        if (mMatcher.find()) {
            total += (mMatcher.group(1)?.toIntOrNull() ?: 0) * 60
            foundAny = true
        }

        val sMatcher = Pattern.compile("(\\d+)\\s*s").matcher(clean)
        if (sMatcher.find()) {
            total += sMatcher.group(1)?.toIntOrNull() ?: 0
            foundAny = true
        }

        if (foundAny) return total

        // Case 3: Plain digits
        return clean.filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    /**
     * Formats seconds into "mm:ss" or "hh:mm:ss".
     */
    fun formatSeconds(totalSeconds: Int): String {
        if (totalSeconds <= 0) return "00:00"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats seconds into friendly text like "1h 23m 45s" or "02:15 دقيقة"
     */
    fun formatSecondsReadable(totalSeconds: Int): String {
        if (totalSeconds <= 0) return "البداية (00:00)"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("${hours} س")
        if (minutes > 0) parts.add("${minutes} د")
        if (seconds > 0 || parts.isEmpty()) parts.add("${seconds} ث")

        return parts.joinToString(" ")
    }

    /**
     * Default thumbnail URL for a video ID.
     */
    fun getThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }

    /**
     * Fetches video metadata (title, author, thumbnail) via YouTube oEmbed API.
     */
    suspend fun fetchMetadata(videoId: String): VideoMetadata = withContext(Dispatchers.IO) {
        val oEmbedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
        try {
            val request = Request.Builder()
                .url(oEmbedUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val title = json.optString("title", "فيديو يوتيوب")
                    val author = json.optString("author_name", "")
                    val thumb = json.optString("thumbnail_url", getThumbnailUrl(videoId))
                    return@withContext VideoMetadata(
                        title = title,
                        channelName = author,
                        thumbnailUrl = thumb
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch oEmbed metadata for $videoId: ${e.message}")
        }

        // Fallback
        VideoMetadata(
            title = "فيديو ($videoId)",
            channelName = "",
            thumbnailUrl = getThumbnailUrl(videoId)
        )
    }

    /**
     * Opens YouTube app with exact timestamp.
     * Uses Intent with fallback to standard web browser.
     */
    fun openVideoInYouTube(context: Context, videoId: String, startSeconds: Int) {
        val timeParam = if (startSeconds > 0) "&t=${startSeconds}s" else ""
        val webUrl = "https://www.youtube.com/watch?v=$videoId$timeParam"

        try {
            // First attempt: try opening directly via YouTube application package
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            // Fallback: Open in any available video player / browser
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Cannot launch video: ${ex.message}")
            }
        }
    }
}
