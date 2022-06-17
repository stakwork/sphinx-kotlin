package io.matthewnelson.concept_media_cache

import chat.sphinx.wrapper_message_media.MediaType
import java.io.File
import java.io.InputStream

abstract class MediaCacheHandler {
    abstract fun createFile(mediaType: MediaType, extension: String? = null): File?
    abstract fun createAudioFile(extension: String): File
    abstract fun createImageFile(extension: String): File
    abstract fun createVideoFile(extension: String): File
    abstract fun createPdfFile(extension: String) : File
    abstract fun createPaidTextFile(extension: String): File

    abstract suspend fun copyTo(from: File, to: File): File
    abstract suspend fun copyTo(from: InputStream, to: File): File
}
