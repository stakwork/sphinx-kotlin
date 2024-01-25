package chat.sphinx.feature_link_preview

import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.wrapper_chat.ChatHost
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import chat.sphinx.concept_link_preview.model.*
import chat.sphinx.feature_link_preview.util.getDescription
import chat.sphinx.feature_link_preview.util.getFavIconUrl
import chat.sphinx.feature_link_preview.util.getImageUrl
import chat.sphinx.feature_link_preview.util.getTitle
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
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

internal sealed interface LinkPreviewDataRetriever

internal data class HtmlPreviewDataRetriever(val url: HttpUrl): LinkPreviewDataRetriever {
    private val lock = Mutex()

    @Volatile
    private var previewData: HtmlPreviewData? = null

    suspend fun getHtmlPreview(
        dispatchers: CoroutineDispatchers,
        okHttpClient: OkHttpClient
    ): HtmlPreviewData? =
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
    ): HtmlPreviewData? {
        val request = Request.Builder().url(url).build()
        var response: Response?

        withContext(dispatchers.io) {
            response =
                try {
                    okHttpClient.newCall(request).execute()
                } catch (e: Exception) {
                    null
                }

            if (response?.isSuccessful == false) {
                response?.body?.closeQuietly()
            }
        }

        if (response == null || response?.isSuccessful == false) {
            return null
        }

        return response?.body?.source()?.inputStream()?.let { stream ->

            try {
                withContext(dispatchers.default) {

                    val document: Document = Jsoup.parse(
                        /* in */            stream,
                        /* charsetName */   null,
                        /* baseUri */       url.toString(),
                    )

                    HtmlPreviewData(
                        document.getTitle()?.toHtmlPreviewTitleOrNull(),
                        HtmlPreviewDomainHost(url.host),
                        document.getDescription()?.toPreviewDescriptionOrNull(),
                        document.getImageUrl()?.toPreviewImageUrlOrNull(),
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

internal class TribePreviewDataRetriever(val tribeJoinLink: TribeJoinLink): LinkPreviewDataRetriever{
    private val lock = Mutex()

    @Volatile
    private var previewData: TribePreviewData? = null

    suspend fun getTribePreview(networkQueryChat: NetworkQueryChat): TribePreviewData? =
        previewData ?: lock.withLock {
            previewData ?: retrievePreview(networkQueryChat)
                .also {
                    if (it != null) {
                        previewData = it
                    }
                }

        }

    private suspend fun retrievePreview(networkQueryChat: NetworkQueryChat): TribePreviewData? {

        var data: TribePreviewData? = null

        networkQueryChat.getTribeInfo(
            ChatHost(tribeJoinLink.tribeHost),
            LightningNodePubKey(tribeJoinLink.tribePubkey)
        ).collect { response ->
            @Exhaustive
            when (response) {
                is LoadResponse.Loading -> {}
                is chat.sphinx.kotlin_response.Response.Error -> {}
                is chat.sphinx.kotlin_response.Response.Success -> {

                    // Needs to get description and image

                    data = TribePreviewData(
                        TribePreviewName(response.value.name),
                        "".toPreviewDescriptionOrNull(),
                        "".toPreviewImageUrlOrNull(),
                    )
                }
            }
        }

        return data
    }
}
