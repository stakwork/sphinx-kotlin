package chat.sphinx.feature_link_preview

import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal class LinkPreviewCache private constructor() {

    companion object {
        const val CACHE_SIZE = 10

        @Volatile
        private var instance: LinkPreviewCache? = null

        @JvmSynthetic
        internal fun getInstance(): LinkPreviewCache =
            instance ?: synchronized(this) {
                instance ?: LinkPreviewCache()
                    .also { instance = it }
            }
    }

    private var counter = 0
    private val list: MutableList<LinkPreviewDataRetriever> = ArrayList(CACHE_SIZE)
    private val lock = Mutex()

    suspend fun getHtmlPreviewDataRetriever(url: String): HtmlPreviewDataRetriever? {
        val httpUrl = url.toHttpUrlOrNull() ?: return null

        lock.withLock {
            for (item in list) {
                if (item is HtmlPreviewDataRetriever && item.url == httpUrl) {
                    return item
                }
            }

            return HtmlPreviewDataRetriever(httpUrl).also { retriever ->
                updateCache(retriever)
            }
        }
    }

    suspend fun getTribePreviewDataRetriever(tribeJoinLink: TribeJoinLink): TribePreviewDataRetriever? {
        if (tribeJoinLink.tribeHost.isEmpty()) {
            return null
        }

        lock.withLock {
            for (item in list) {
                if (item is TribePreviewDataRetriever && item.tribeJoinLink == tribeJoinLink) {
                    return item
                }
            }

            return TribePreviewDataRetriever(tribeJoinLink).also { retriever ->
                updateCache(retriever)
            }
        }
    }

    private fun updateCache(retriever: LinkPreviewDataRetriever) {
        list.add(counter, retriever)

        if (counter < CACHE_SIZE - 1 /* last index */) {
            counter++
        } else {
            counter = 0
        }
    }
}
