package io.matthewnelson.concept_media_cache

import java.io.File
import java.io.InputStream

abstract class MediaCacheHandler {
    abstract fun createImageFile(extension: String): File
    abstract fun createVideoFile(extension: String): File

    abstract suspend fun copyTo(from: File, to: File): File
    abstract suspend fun copyTo(from: InputStream, to: File): File
}
