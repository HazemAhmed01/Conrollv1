package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.repository.FirestoreBookmarkRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirestoreBookmarkRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun testAuthenticatedUserCanSaveAndObserveFolders() = runBlocking {
    val uid = signInTestUser("folders_${System.currentTimeMillis()}@example.com")
    val repo = FirestoreBookmarkRepository(firestore)

    repo.saveFolder(
      name = "تعليم",
      colorHex = "#1E88E5",
      iconName = "school"
    )

    val folders = repo.observeFolders().first { it.isNotEmpty() }
    assertEquals(1, folders.size)
    assertEquals("تعليم", folders[0].name)
    assertEquals(uid, folders[0].userId)
  }

  @Test
  fun testAuthenticatedUserCanSaveAndObserveVideos() = runBlocking {
    val uid = signInTestUser("videos_${System.currentTimeMillis()}@example.com")
    val repo = FirestoreBookmarkRepository(firestore)

    repo.saveVideo(
      youtubeVideoId = "dQw4w9WgXcQ",
      originalUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=45s",
      title = "Rick Astley Song",
      channelName = "Rick Astley",
      thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
      startSeconds = 45,
      folderId = null,
      note = "Favourite moment"
    )

    val videos = repo.observeVideos().first { it.isNotEmpty() }
    assertEquals(1, videos.size)
    assertEquals("dQw4w9WgXcQ", videos[0].youtubeVideoId)
    assertEquals(45, videos[0].startSeconds)
    assertEquals(uid, videos[0].userId)
  }

  @Test
  fun testUnauthenticatedUserCannotAccessRepository() = runBlocking {
    auth.signOut()
    val repo = FirestoreBookmarkRepository(firestore)

    try {
      repo.saveFolder("test", "#E53935", "folder")
      fail("Expected exception for unauthenticated call")
    } catch (e: IllegalStateException) {
      assertTrue(e.message?.contains("Google") == true)
    }
  }
}
