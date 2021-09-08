package io.matthewnelson.feature_media_cache

import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.*
import okio.*
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class MediaCacheHandlerImpl(
    private val applicationScope: CoroutineScope,
    cacheDir: File,
    dispatchers: CoroutineDispatchers
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
        const val IMAGE_CACHE_DIR = "sphinx_image_cache"
        const val VIDEO_CACHE_DIR = "sphinx_video_cache"

        const val PAID_TEXT_CACHE_DIR = "sphinx_paid_text_cache"
        const val PAID_TEXT_FILE = "paid-message.txt"

        const val DATE_FORMAT = "yyy_MM_dd_HH_mm_ss_SSS"

        const val IMG = "IMG"
        const val VID = "VID"

        private val cacheDirLock = Object()
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

    private val paidTextCache: File by lazy {
        File(cacheDir, PAID_TEXT_CACHE_DIR).also {
            it.mkdirs()
        }
    }

    override fun createImageFile(extension: String): File =
        createFileImpl(imageCache, IMG, extension)

    override fun createVideoFile(extension: String): File =
        createFileImpl(videoCache, VID, extension)

    override fun createPaidTextFile(): File {
        if (!paidTextCache.exists()) {
            synchronized(cacheDirLock) {
                if (!paidTextCache.exists()) {
                    paidTextCache.mkdirs()
                }
            }
        }

        return File(paidTextCache, PAID_TEXT_FILE)
    }

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
                to.writeAll(it)
                try {
                    to.close()
                } catch (e: Exception) {}
            }
        }
    }
}