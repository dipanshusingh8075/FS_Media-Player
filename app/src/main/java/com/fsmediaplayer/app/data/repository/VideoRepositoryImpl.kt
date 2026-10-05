package com.fsmediaplayer.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.fsmediaplayer.app.core.model.VideoMediaItem
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
class VideoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VideoRepository {

    override fun getAllVideos(): Flow<List<VideoMediaItem>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryMediaStoreVideos())
            }
        }

        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        // Initial query emission
        trySend(queryMediaStoreVideos())

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    override fun getVideoFolders(): Flow<Map<String, List<VideoMediaItem>>> {
        return getAllVideos().map { videos ->
            videos.groupBy { it.folderName }
        }.flowOn(Dispatchers.Default)
    }

    private fun queryMediaStoreVideos(): List<VideoMediaItem> {
        val videoItems = mutableListOf<VideoMediaItem>()

        val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            }
        }.toTypedArray()

        // Exclude zero-duration, negative duration, or corrupt 0-byte video entries
        val selection = "${MediaStore.Video.Media.DURATION} > 0 AND ${MediaStore.Video.Media.SIZE} > 0"
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                collectionUri,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

                val bucketNameColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Video_$id"
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dataPath = cursor.getString(dataColumn) ?: ""
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)

                    val resolution = if (width > 0 && height > 0) "${width}x${height}" else null

                    // Resolve folder name safely across Android 10-15 & legacy
                    val folderName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && bucketNameColumn != -1) {
                        cursor.getString(bucketNameColumn)?.takeIf { it.isNotBlank() }
                            ?: resolveFolderFromPath(dataPath)
                    } else {
                        resolveFolderFromPath(dataPath)
                    }

                    val contentUri = ContentUris.withAppendedId(collectionUri, id)

                    videoItems.add(
                        VideoMediaItem(
                            id = id,
                            contentUri = contentUri,
                            title = title,
                            durationMs = duration,
                            sizeBytes = size,
                            resolution = resolution,
                            folderName = folderName,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return videoItems
    }

    private fun resolveFolderFromPath(path: String): String {
        return try {
            File(path).parentFile?.name?.takeIf { it.isNotBlank() } ?: "Internal Storage"
        } catch (e: Exception) {
            "Internal Storage"
        }
    }
}
