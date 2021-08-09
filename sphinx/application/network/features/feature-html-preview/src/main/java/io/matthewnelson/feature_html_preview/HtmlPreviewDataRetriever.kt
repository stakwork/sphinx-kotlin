package io.matthewnelson.feature_html_preview

import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_html_preview.model.HtmlPreview
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly

internal data class HtmlPreviewDataRetriever(val url: HttpUrl) {
    private val lock = Mutex()

    @Volatile
    private var preview: HtmlPreview? = null

    suspend fun getHtmlPreview(
        dispatchers: CoroutineDispatchers,
        okHttpClient: OkHttpClient
    ): HtmlPreview? =
        preview ?: lock.withLock {
            preview ?: retrievePreview(
                dispatchers = dispatchers,
                okHttpClient = okHttpClient,
            ).also {
                if (it != null) {
                    preview = it
                }
            }
        }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun retrievePreview(
        dispatchers: CoroutineDispatchers,
        okHttpClient: OkHttpClient
    ): HtmlPreview? {
        val request = Request.Builder().url(url).build()

        val response: Response = withContext(dispatchers.io) {
            try {
                okHttpClient.newCall(request).execute()
            } catch (e: Exception) {
                null
            }
        } ?: return null

        if (!response.isSuccessful) {
            response.body?.closeQuietly()
            return null
        }

        return response.body?.let { body ->
            // TODO: Implement
            HtmlPreview()
        }
    }
}
