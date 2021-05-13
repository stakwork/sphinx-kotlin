package chat.sphinx.feature_repository.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Mitigates creation of multiple flows for the same query such that
 * multiple collectors are accessing a single flow source.
 *
 * Used primarily for queries that do not include arguments, such
 * as `getAllChats()`.
 * */
internal class SingletonFlow<T> {
    private var flow: Flow<T>? = null

    private val lock = Mutex()

    suspend fun getOrInstantiate(
        action: suspend () -> Flow<T>
    ): Flow<T> =
        flow ?: lock.withLock {
            flow ?: action.invoke()
                .also { flow = it }
        }
}
