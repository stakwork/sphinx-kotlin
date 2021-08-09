package io.matthewnelson.feature_html_preview

import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_html_preview.model.HtmlPreview
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

internal data class HtmlPreviewDataRetriever(val url: String) {
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

    private suspend fun retrievePreview(
        dispatchers: CoroutineDispatchers,
        okHttpClient: OkHttpClient
    ): HtmlPreview? =
        // TODO: Implement
        null
}