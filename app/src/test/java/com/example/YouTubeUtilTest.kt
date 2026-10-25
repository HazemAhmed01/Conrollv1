package com.example

import com.example.util.YouTubeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class YouTubeUtilTest {

    @Test
    fun testParseStandardWatchUrl() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val parsed = YouTubeUtil.parseUrl(url)
        assertEquals("dQw4w9WgXcQ", parsed.videoId)
        assertEquals(0, parsed.extractedSeconds)
    }

    @Test
    fun testParseShortUrlWithTimestamp() {
        val url = "https://youtu.be/dQw4w9WgXcQ?t=85"
        val parsed = YouTubeUtil.parseUrl(url)
        assertEquals("dQw4w9WgXcQ", parsed.videoId)
        assertEquals(85, parsed.extractedSeconds)
    }

    @Test
    fun testParseShortsUrl() {
        val url = "https://www.youtube.com/shorts/AbCdEf12345"
        val parsed = YouTubeUtil.parseUrl(url)
        assertEquals("AbCdEf12345", parsed.videoId)
    }

    @Test
    fun testParseCompoundTimestamp() {
        assertEquals(90, YouTubeUtil.parseTimeStringToSeconds("1m30s"))
        assertEquals(3665, YouTubeUtil.parseTimeStringToSeconds("1h1m5s"))
        assertEquals(150, YouTubeUtil.parseTimeStringToSeconds("02:30"))
        assertEquals(3725, YouTubeUtil.parseTimeStringToSeconds("01:02:05"))
    }

    @Test
    fun testFormatSeconds() {
        assertEquals("00:45", YouTubeUtil.formatSeconds(45))
        assertEquals("02:30", YouTubeUtil.formatSeconds(150))
        assertEquals("01:02:05", YouTubeUtil.formatSeconds(3725))
    }
}
