package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.FirestoreFolder
import com.example.data.model.FirestoreVideoBookmark
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreBookmarkRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب Google للوصول إلى قاعدة البيانات السحابية")
    }

    /**
     * Realtime flow of folders for the signed-in user.
     */
    fun observeFolders(): Flow<List<FirestoreFolder>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/folders"
        val collection = db.collection("users").document(uid).collection("folders")

        val flow = collection.snapshots().map { snapshot ->
            snapshot.toObjects(FirestoreFolder::class.java)
        }.catch { error ->
            if (error is Exception) {
                handleFirestoreError(error, OperationType.LIST, path)
            }
            throw error
        }

        flow.collect { list ->
            emit(list)
        }
    }

    /**
     * Realtime flow of all video bookmarks for the signed-in user.
     */
    fun observeVideos(): Flow<List<FirestoreVideoBookmark>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/videos"
        val collection = db.collection("users").document(uid).collection("videos")

        val flow = collection.snapshots().map { snapshot ->
            snapshot.toObjects(FirestoreVideoBookmark::class.java)
        }.catch { error ->
            if (error is Exception) {
                handleFirestoreError(error, OperationType.LIST, path)
            }
            throw error
        }

        flow.collect { list ->
            emit(list)
        }
    }

    /**
     * Adds or updates a folder in Firestore.
     */
    suspend fun saveFolder(
        name: String,
        colorHex: String,
        iconName: String,
        existingFolderId: String? = null
    ) {
        val uid = requireUserId()
        val foldersColl = db.collection("users").document(uid).collection("folders")

        if (!existingFolderId.isNullOrBlank()) {
            val docRef = foldersColl.document(existingFolderId)
            val updates = mapOf(
                "name" to name.trim(),
                "colorHex" to colorHex,
                "iconName" to iconName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        } else {
            val docRef = foldersColl.document()
            val payload = mapOf(
                "userId" to uid,
                "name" to name.trim(),
                "colorHex" to colorHex,
                "iconName" to iconName,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }
    }

    /**
     * Deletes a folder from Firestore.
     */
    suspend fun deleteFolder(folderId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("folders").document(folderId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    /**
     * Adds or updates a video bookmark in Firestore.
     */
    suspend fun saveVideo(
        youtubeVideoId: String,
        originalUrl: String,
        title: String,
        channelName: String,
        thumbnailUrl: String,
        startSeconds: Int,
        folderId: String?,
        note: String,
        existingVideoId: String? = null
    ) {
        val uid = requireUserId()
        val videosColl = db.collection("users").document(uid).collection("videos")

        if (!existingVideoId.isNullOrBlank()) {
            val docRef = videosColl.document(existingVideoId)
            val updates = mutableMapOf<String, Any?>(
                "youtubeVideoId" to youtubeVideoId,
                "originalUrl" to originalUrl,
                "title" to title.trim(),
                "channelName" to channelName.trim(),
                "thumbnailUrl" to thumbnailUrl,
                "startSeconds" to startSeconds,
                "folderId" to folderId,
                "note" to note.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        } else {
            val docRef = videosColl.document()
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "youtubeVideoId" to youtubeVideoId,
                "originalUrl" to originalUrl,
                "title" to title.trim(),
                "channelName" to channelName.trim(),
                "thumbnailUrl" to thumbnailUrl,
                "startSeconds" to startSeconds,
                "folderId" to folderId,
                "note" to note.trim(),
                "isFavorite" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }
    }

    /**
     * Toggles favorite status in Firestore.
     */
    suspend fun toggleFavorite(videoId: String, isFavorite: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("videos").document(videoId)
        try {
            docRef.update(
                mapOf(
                    "isFavorite" to isFavorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Deletes a video bookmark from Firestore.
     */
    suspend fun deleteVideo(videoId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("videos").document(videoId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    /**
     * Seeds initial folders if a new user has zero folders.
     */
    suspend fun seedStarterFoldersIfEmpty() {
        val uid = requireUserId()
        val foldersColl = db.collection("users").document(uid).collection("folders")
        val snapshot = foldersColl.limit(1).get().await()
        if (snapshot.isEmpty) {
            saveFolder(name = "دورات وتعليم", colorHex = "#1E88E5", iconName = "school")
            saveFolder(name = "بودكاست ومقابلات", colorHex = "#FB8C00", iconName = "mic")
            saveFolder(name = "برمجة وتطوير", colorHex = "#43A047", iconName = "code")
        }
    }
}
