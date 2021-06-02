package chat.sphinx.concept_paging

import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

abstract class PageSourceWrapper<Key: Any, Value: Any, Original: Any>(
    initialSource: PagingSource<Key, Original>
) {
    @Volatile
    private var currentSource: PagingSource<Key, Original> = initialSource

    protected abstract val config: PagingConfig

    protected open val initialKey: Key?
        get() = null

    @OptIn(ExperimentalPagingApi::class)
    protected open fun getRemoteMediator(): RemoteMediator<Key, Original>? = null

    protected abstract fun createNewPagerSource(): PagingSource<Key, Original>
    protected abstract suspend fun mapOriginal(original: Original): Value

    @OptIn(ExperimentalPagingApi::class)
    val pagingDataFlow: Flow<PagingData<Value>> by lazy {
        Pager(config, initialKey, getRemoteMediator()) {
            if (currentSource.invalid) {
                createNewPagerSource()
                    .also { currentSource = it }
            } else {
                currentSource
            }
        }.flow.map { pagingData ->
            pagingData.map {
                mapOriginal(it)
            }
        }
    }

    val invalid: Boolean
        get() = currentSource.invalid

    fun invalidate(): Unit =
        currentSource.invalidate()

    val jumpingSupported: Boolean
        get() = currentSource.jumpingSupported

    val keyReuseSupported: Boolean
        get() = currentSource.keyReuseSupported

    fun registerInvalidatedCallback(onInvalidatedCallback: () -> Unit) {
        currentSource.registerInvalidatedCallback(onInvalidatedCallback)
    }

    fun unregisterInvalidatedCallback(onInvalidatedCallback: () -> Unit) {
        currentSource.unregisterInvalidatedCallback(onInvalidatedCallback)
    }
}
