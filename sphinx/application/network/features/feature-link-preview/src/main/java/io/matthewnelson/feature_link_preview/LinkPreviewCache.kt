package io.matthewnelson.feature_link_preview

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

    suspend fun getHtmlPreviewDataRetriever(url: String): LinkPreviewDataRetriever? {
        val httpUrl = url.toHttpUrlOrNull() ?: return null

        lock.withLock {
            for (item in list) {
                if (item.url == httpUrl) {
                    return item
                }
            }

            return LinkPreviewDataRetriever(httpUrl).also { retriever ->
                list[counter] = retriever

                if (counter < CACHE_SIZE - 1 /* last index */) {
                    counter++
                } else {
                    counter = 0
                }
            }
        }
    }
}
