package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreVideoBookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val youtubeVideoId: String = "",
    val originalUrl: String = "",
    val title: String = "",
    val channelName: String = "",
    val thumbnailUrl: String = "",
    val startSeconds: Int = 0,
    val folderId: String? = null,
    val note: String = "",
    val isFavorite: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
