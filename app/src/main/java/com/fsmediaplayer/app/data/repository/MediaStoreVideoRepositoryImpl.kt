package com.fsmediaplayer.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.fsmediaplayer.app.core.model.VideoFolder
import com.fsmediaplayer.app.core.model.VideoItem
import com.fsmediaplayer.app.domain.repository.VideoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreVideoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VideoRepository {

    override fun getAllVideos(): Flow<List<VideoItem>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(fetchVideosInternal())
            }
        }

        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        // Initial fetch
        trySend(fetchVideosInternal())

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    override fun getVideoFolders(): Flow<List<VideoFolder>> {
        return getAllVideos().map { videos ->
            videos.groupBy { it.bucketId }
                .map { (bucketId, folderVideos) ->
                    val folderName = folderVideos.firstOrNull()?.bucketName.orEmpty().ifBlank { "Videos" }
                    val totalSize = folderVideos.sumOf { it.sizeBytes }
                    val firstThumbnailUri = folderVideos.firstOrNull()?.contentUri

                    VideoFolder(
                        id = bucketId,
                        name = folderName,
                        videoCount = folderVideos.size,
                        firstVideoUri = firstThumbnailUri,
                        totalSizeBytes = totalSize
                    )
                }
                .sortedByDescending { it.videoCount }
        }
    }

    override fun getVideosInFolder(bucketId: String): Flow<List<VideoItem>> {
        return getAllVideos().map { videos ->
            videos.filter { it.bucketId == bucketId }
        }
    }

    override fun getVideoById(id: Long): Flow<VideoItem?> {
        return getAllVideos().map { videos ->
            videos.firstOrNull { it.id == id }
        }
    }

    private fun fetchVideosInternal(): List<VideoItem> {
        val videoList = mutableListOf<VideoItem>()

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.BUCKET_ID)
                add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                collectionUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

                val bucketIdColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                } else -1

                val bucketNameColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Video_$id"
                    val path = cursor.getString(dataColumn) ?: ""
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: "video/*"
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)

                    val resolution = if (width > 0 && height > 0) "${width}x${height}" else null

                    // Bucket calculation for scoped storage & legacy
                    val bucketId: String
                    val bucketName: String

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && bucketIdColumn != -1) {
                        bucketId = cursor.getString(bucketIdColumn) ?: fallbackBucketId(path)
                        bucketName = cursor.getString(bucketNameColumn) ?: fallbackBucketName(path)
                    } else {
                        bucketId = fallbackBucketId(path)
                        bucketName = fallbackBucketName(path)
                    }

                    val contentUri = ContentUris.withAppendedId(collectionUri, id)

                    videoList.add(
                        VideoItem(
                            id = id,
                            contentUri = contentUri,
                            name = name,
                            path = path,
                            durationMs = duration,
                            sizeBytes = size,
                            resolution = resolution,
                            dateModified = dateModified,
                            bucketId = bucketId,
                            bucketName = bucketName,
                            mimeType = mimeType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return videoList
    }

    private fun fallbackBucketId(path: String): String {
        return try {
            File(path).parentFile?.absolutePath?.hashCode()?.toString() ?: "default_bucket"
        } catch (e: Exception) {
            "default_bucket"
        }
    }

    private fun fallbackBucketName(path: String): String {
        return try {
            File(path).parentFile?.name ?: "Videos"
        } catch (e: Exception) {
            "Videos"
        }
    }
}
