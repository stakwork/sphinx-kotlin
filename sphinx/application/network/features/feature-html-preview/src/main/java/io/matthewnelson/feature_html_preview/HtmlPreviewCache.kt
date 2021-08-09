package io.matthewnelson.feature_html_preview

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal class HtmlPreviewCache private constructor() {

    companion object {
        const val CACHE_SIZE = 10

        @Volatile
        private var instance: HtmlPreviewCache? = null

        @JvmSynthetic
        internal fun getInstance(): HtmlPreviewCache =
            instance ?: synchronized(this) {
                instance ?: HtmlPreviewCache()
                    .also { instance = it }
            }
    }

    private var counter = 0
    private val list: MutableList<HtmlPreviewDataRetriever> = ArrayList(CACHE_SIZE)
    private val lock = Mutex()

    suspend fun getHtmlPreviewDataRetriever(url: String): HtmlPreviewDataRetriever? {
        val httpUrl = url.toHttpUrlOrNull() ?: return null

        lock.withLock {
            for (item in list) {
                if (item.url == httpUrl) {
                    return item
                }
            }

            return HtmlPreviewDataRetriever(httpUrl).also { retriever ->
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
