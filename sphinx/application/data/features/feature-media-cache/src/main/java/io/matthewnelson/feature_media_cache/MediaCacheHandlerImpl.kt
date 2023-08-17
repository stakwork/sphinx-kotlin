package io.matthewnelson.feature_media_cache

import chat.sphinx.wrapper_message_media.MediaType
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okio.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class MediaCacheHandlerImpl(
    private val applicationScope: CoroutineScope,
    cacheDir: File,
    dispatchers: CoroutineDispatchers,
): MediaCacheHandler(), CoroutineDispatchers by dispatchers {

    init {
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw IllegalStateException("Failed to create root MediaCache directory: ${cacheDir.path}")
            }
        } else {
            require(cacheDir.isDirectory) {
                "cacheDir must be a directory"
            }
        }
    }

    companion object {
        const val AUDIO_CACHE_DIR = "sphinx_audio_cache"
        const val IMAGE_CACHE_DIR = "sphinx_image_cache"
        const val VIDEO_CACHE_DIR = "sphinx_video_cache"
        const val PDF_CACHE_DIR = "sphinx_pdf_cache"
        const val PAID_TEXT_CACHE_DIR = "sphinx_paid_text_cache"
        const val GENERIC_FILES_CACHE_DIR = "sphinx_files_cache"

        const val DATE_FORMAT = "yyy_MM_dd_HH_mm_ss_SSS"

        const val AUD = "AUD"
        const val IMG = "IMG"
        const val VID = "VID"
        const val PDF = "PDF"
        const val TXT = "TXT"
        const val FILE = "FILE"

        private val cacheDirLock = Object()
    }

    private val audioCache: File by lazy {
        File(cacheDir, AUDIO_CACHE_DIR).also {
            it.mkdir()
        }
    }

    private val imageCache: File by lazy {
        File(cacheDir, IMAGE_CACHE_DIR).also {
            it.mkdir()
        }
    }

    private val videoCache: File by lazy {
        File(cacheDir, VIDEO_CACHE_DIR).also {
            it.mkdirs()
        }
    }

    private val pdfCache: File by lazy {
        File(cacheDir, PDF_CACHE_DIR).also {
            it.mkdir()
        }
    }

    private val paidTextCache: File by lazy {
        File(cacheDir, PAID_TEXT_CACHE_DIR).also {
            it.mkdirs()
        }
    }

    private val genericFilesCache: File by lazy {
        File(cacheDir, GENERIC_FILES_CACHE_DIR).also {
            it.mkdirs()
        }
    }

    override fun createFile(
        mediaType: MediaType,
        extension: String?
    ): File? {
        return when (mediaType) {
            is MediaType.Audio -> {
                mediaType.value.split("/").lastOrNull()?.let { fileType ->
                    when {
                        fileType.contains("m4a", ignoreCase = true) -> {
                            createAudioFile("m4a")
                        }
                        fileType.contains("mp3", ignoreCase = true) -> {
                            createAudioFile("mp3")
                        }
                        fileType.contains("mp4", ignoreCase = true) -> {
                            createAudioFile("mp4")
                        }
                        fileType.contains("mpeg", ignoreCase = true) -> {
                            createAudioFile("mpeg")
                        }
                        fileType.contains("wav", ignoreCase = true) -> {
                            createAudioFile("wav")
                        }
                        else -> {
                            null
                        }
                    }
                }
            }
            is MediaType.Image -> {
                mediaType.value.split("/").lastOrNull()?.let { fileType ->
                    when {
                        fileType.contains("jpeg", ignoreCase = true) || fileType.contains("jpg", ignoreCase = true) -> {
                            createImageFile("jpeg")
                        }
                        fileType.contains("png", ignoreCase = true) -> {
                            createImageFile("png")
                        }
                        else -> {
                            null
                        }
                    }
                }
            }
            is MediaType.Pdf -> {
                mediaType.value.split("/").lastOrNull()?.let { fileType ->
                    when {
                        fileType.contains("pdf", ignoreCase = true) -> {
                            createPdfFile("pdf")
                        }
                        else -> {
                            null
                        }
                    }
                }

            }
            is MediaType.Text -> {
                // TODO: Implement
                null
            }
            is MediaType.Video -> {
                // TODO: Auto generate file extension (if app doesn't support media we can load via )
                mediaType.value.split("/").lastOrNull()?.let { fileType ->
                    when {
                        fileType.contains("webm", ignoreCase = true) -> {
                            createVideoFile("webm")
                        }
                        fileType.contains("3gpp", ignoreCase = true) -> {
                            createVideoFile("3gp")
                        }
                        fileType.contains("x-matroska", ignoreCase = true) -> {
                            createVideoFile("mkv")
                        }
                        fileType.contains("mp4", ignoreCase = true) -> {
                            createVideoFile("mp4")
                        }
                        fileType.contains("mov", ignoreCase = true) -> {
                            createVideoFile("mov")
                        }
                        else -> {
                            null
                        }
                    }
                }
            }
            is MediaType.Unknown -> {
                createGenericFile(extension ?: "txt")
            }
        }
    }

    override fun createAudioFile(extension: String): File =
        createFileImpl(audioCache, AUD, extension)

    override fun createImageFile(extension: String): File =
        createFileImpl(imageCache, IMG, extension)

    override fun createVideoFile(extension: String): File =
        createFileImpl(videoCache, VID, extension)

    override fun createPdfFile(extension: String): File =
        createFileImpl(
            pdfCache, PDF, extension)

    override fun createPaidTextFile(extension: String): File =
        createFileImpl(paidTextCache, TXT, extension)

    private fun createGenericFile(extension: String): File =
        createFileImpl(genericFilesCache, FILE, extension)

    private fun createFileImpl(cacheDir: File, prefix: String, extension: String): File {
        if (!cacheDir.exists()) {
            synchronized(cacheDirLock) {
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
            }
        }

        val ext = extension.replace(".", "")
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.US)
        return File(cacheDir, "${prefix}_${sdf.format(Date())}.$ext")
    }

    // TODO: Implement file deletion on caller scope cancellation
    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun copyTo(from: File, to: File): File {
        copyToImpl(from.source(), to.sink().buffer()).join()
        return to
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun copyTo(from: InputStream, to: File): File {
        copyToImpl(from.source(), to.sink().buffer()).join()
        return to
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun copyToImpl(from: Source, to: BufferedSink): Job {
        return applicationScope.launch(io) {
            from.use {
                try {
                    to.writeAll(it)
                } catch (e: IOException) {
                } finally {

                    try {
                        to.close()
                    } catch (e: IOException) {}

                }
            }
        }
    }
}