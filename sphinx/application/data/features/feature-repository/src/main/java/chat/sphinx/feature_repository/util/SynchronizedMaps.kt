package chat.sphinx.feature_repository.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Wraps a mutable map such that anything
 * executed within the lambda holds a lock.
 * */
class SynchronizedSuspendMap<K, V>(initialCapacity: Int = 0) {
    private val map: MutableMap<K, V> = LinkedHashMap(initialCapacity)
    private val lock = Mutex()

    suspend fun<T> withLock(action: (MutableMap<K, V>) -> T): T =
        lock.withLock { action(map) }
}

/**
 * Wraps a mutable map such that anything
 * executed within the lambda holds a lock.
 * */
class SynchronizedMap<K, V>(initialCapacity: Int = 0) {
    private val map: MutableMap<K, V> = LinkedHashMap(initialCapacity)

    fun<T> withLock(action: (MutableMap<K, V>) -> T): T =
        synchronized(this) { action(map) }
}
