package io.matthewnelson.feature_link_preview

import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_link_preview.model.*
import io.matthewnelson.feature_link_preview.util.getDescription
import io.matthewnelson.feature_link_preview.util.getFavIconUrl
import io.matthewnelson.feature_link_preview.util.getImageUrl
import io.matthewnelson.feature_link_preview.util.getTitle
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal data class LinkPreviewDataRetriever(val url: HttpUrl) {
    private val lock = Mutex()

    @Volatile
    private var previewData: LinkPreviewData? = null

    suspend fun getHtmlPreview(
        dispatchers: CoroutineDispatchers,
        okHttpClient: OkHttpClient
    ): LinkPreviewData? =
        previewData ?: lock.withLock {
            previewData ?: retrievePreview(
                dispatchers = dispatchers,
                okHttpClient = okHttpClient,
            ).also {
                if (it != null) {
                    previewData = it
                }
            }
        }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun retrievePreview(
        dispatchers: CoroutineDispatchers,
        okHttpClient: OkHttpClient
    ): LinkPreviewData? {
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

        return response.body?.source()?.inputStream()?.let { stream ->

            try {
                withContext(dispatchers.default) {

                    val document: Document = Jsoup.parse(
                        /* in */            stream,
                        /* charsetName */   null,
                        /* baseUri */       url.toString(),
                    )

                    LinkPreviewData(
                        document.getTitle()?.toHtmlPreviewTitleOrNull(),
                        LinkPreviewDomainHost(url.host),
                        document.getDescription()?.toHtmlPreviewDescriptionOrNull(),
                        document.getImageUrl()?.toHtmlPreviewImageUrlOrNull(),
                        document.getFavIconUrl()?.toHtmlPreviewFavIconUrlOrNull(),
                    )
                }
            } catch (e: Exception) {
                null
            } finally {
                stream.closeQuietly()
            }
        }
    }
}
